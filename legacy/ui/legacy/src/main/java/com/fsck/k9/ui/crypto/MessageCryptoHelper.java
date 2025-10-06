package com.fsck.k9.ui.crypto;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.IntentCompat;
import com.fsck.k9.autocrypt.AutocryptOperations;
import com.fsck.k9.crypto.MessageCryptoStructureDetector;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.SizeAware;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;
import com.fsck.k9.mailstore.MessageCryptoAnnotations;
import com.fsck.k9.mailstore.MessageHelper;
import com.fsck.k9.mailstore.MimePartStreamParser;
import com.fsck.k9.mailstore.util.FileFactory;
import com.fsck.k9.provider.DecryptedFileProvider;
import org.apache.commons.io.IOUtils;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.CancelableBackgroundOperation;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpSinkResultCallback;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSink;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;
import net.thunderbird.core.logging.legacy.Log;


public class MessageCryptoHelper {
    private static final int INVALID_OPENPGP_RESULT_CODE = -1;
    private static final MimeBodyPart NO_REPLACEMENT_PART = null;
    private static final int REQUEST_CODE_USER_INTERACTION = 124;


    private final Context context;
    private final String openPgpProvider;
    private final AutocryptOperations autocryptOperations;
    private final Object callbackLock = new Object();
    private final Deque<CryptoPart> partsToProcess = new ArrayDeque<>();

    @Nullable
    private MessageCryptoCallback callback;

    private Message currentMessage;
    private OpenPgpDecryptionResult cachedDecryptionResult;
    private MessageCryptoAnnotations queuedResult;
    private PendingIntent queuedPendingIntent;


    private MessageCryptoAnnotations messageAnnotations;
    private CryptoPart currentCryptoPart;
    private Intent currentCryptoResult;
    private Intent userInteractionResultIntent;
    private State state;
    private CancelableBackgroundOperation cancelableBackgroundOperation;
    private boolean isCancelled;
    private boolean processSignedOnly;

    private OpenPgpApi openPgpApi;
    private OpenPgpServiceConnection openPgpServiceConnection;
    private OpenPgpApiFactory openPgpApiFactory;


    public MessageCryptoHelper(Context context, OpenPgpApiFactory openPgpApiFactory,
            AutocryptOperations autocryptOperations, @NonNull String openPgpProvider) {
        this.context = context.getApplicationContext();

        this.autocryptOperations = autocryptOperations;
        this.openPgpApiFactory = openPgpApiFactory;
        this.openPgpProvider = openPgpProvider;
    }

    public boolean isConfiguredForOpenPgpProvider(String openPgpProvider) {
        return this.openPgpProvider.equals(openPgpProvider);
    }

    public void asyncStartOrResumeProcessingMessage(Message message, MessageCryptoCallback callback,
            OpenPgpDecryptionResult cachedDecryptionResult, boolean processSignedOnly) {
        if (this.currentMessage != null) {
            reattachCallback(message, callback);
            return;
        }

        this.messageAnnotations = new MessageCryptoAnnotations();
        this.state = State.START;
        this.currentMessage = message;
        this.cachedDecryptionResult = cachedDecryptionResult;
        this.callback = callback;
        this.processSignedOnly = processSignedOnly;

        nextStep();
    }

    public void resumeCryptoOperationIfNecessary() {
        if (queuedPendingIntent != null) {
            deliverResult();
        }
    }

    private void findPartsForMultipartEncryptionPass() {
        List<Part> encryptedParts = MessageCryptoStructureDetector.findMultipartEncryptedParts(currentMessage);
        for (Part part : encryptedParts) {
            if (!MessageHelper.isCompletePartAvailable(part)) {
                addErrorAnnotation(part, CryptoError.OPENPGP_ENCRYPTED_BUT_INCOMPLETE, MessageHelper.createEmptyPart());
                continue;
            }
            if (MessageCryptoStructureDetector.isMultipartEncryptedOpenPgpProtocol(part)) {
                CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_ENCRYPTED, part);
                partsToProcess.add(cryptoPart);
                continue;
            }
            addErrorAnnotation(part, CryptoError.ENCRYPTED_BUT_UNSUPPORTED, MessageHelper.createEmptyPart());
        }
    }

    private void findPartsForMultipartSignaturePass() {
        List<Part> signedParts = MessageCryptoStructureDetector
                .findMultipartSignedParts(currentMessage, messageAnnotations);
        for (Part part : signedParts) {
            if (!processSignedOnly) {
                boolean isEncapsulatedSignature =
                        messageAnnotations.findKeyForAnnotationWithReplacementPart(part) != null;
                if (!isEncapsulatedSignature) {
                    continue;
                }
            }
            if (!MessageHelper.isCompletePartAvailable(part)) {
                MimeBodyPart replacementPart = getMultipartSignedContentPartIfAvailable(part);
                addErrorAnnotation(part, CryptoError.OPENPGP_SIGNED_BUT_INCOMPLETE, replacementPart);
                continue;
            }
            if (MessageCryptoStructureDetector.isMultipartSignedOpenPgpProtocol(part)) {
                CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_SIGNED, part);
                partsToProcess.add(cryptoPart);
                continue;
            }
            MimeBodyPart replacementPart = getMultipartSignedContentPartIfAvailable(part);
            addErrorAnnotation(part, CryptoError.SIGNED_BUT_UNSUPPORTED, replacementPart);
        }
    }

    private void findPartsForPgpInlinePass() {
        List<Part> inlineParts = MessageCryptoStructureDetector.findPgpInlineParts(currentMessage);
        for (Part part : inlineParts) {
            if (!processSignedOnly && !MessageCryptoStructureDetector.isPartPgpInlineEncrypted(part)) {
                continue;
            }

            if (!currentMessage.getFlags().contains(Flag.X_DOWNLOADED_FULL)) {
                if (MessageCryptoStructureDetector.isPartPgpInlineEncrypted(part)) {
                    addErrorAnnotation(part, CryptoError.OPENPGP_ENCRYPTED_BUT_INCOMPLETE, NO_REPLACEMENT_PART);
                } else {
                    addErrorAnnotation(part, CryptoError.OPENPGP_SIGNED_BUT_INCOMPLETE, NO_REPLACEMENT_PART);
                }
                continue;
            }

            CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_INLINE, part);
            partsToProcess.add(cryptoPart);
        }
    }

    private void findPartsForAutocryptPass() {
        boolean otherCryptoPerformed = !messageAnnotations.isEmpty();
        if (otherCryptoPerformed) {
            return;
        }

        if (autocryptOperations.hasAutocryptHeader(currentMessage)) {
            CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PLAIN_AUTOCRYPT, currentMessage);
            partsToProcess.add(cryptoPart);
        }
    }

    private void addErrorAnnotation(Part part, CryptoError error, MimeBodyPart replacementPart) {
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createErrorAnnotation(error, replacementPart);
        messageAnnotations.put(part, annotation);
    }

    private void nextStep() {
        if (isCancelled) {
            return;
        }

        while (state != State.FINISHED && partsToProcess.isEmpty()) {
            findPartsForNextPass();
        }

        if (state == State.FINISHED) {
            callbackReturnResult();
            return;
        }

        if (!isBoundToCryptoProviderService()) {
            connectToCryptoProviderService();
            return;
        }

        currentCryptoPart = partsToProcess.peekFirst();
        if (currentCryptoPart.type == CryptoPartType.PLAIN_AUTOCRYPT) {
            processAutocryptHeaderForCurrentPart();
        } else {
            decryptOrVerifyCurrentPart();
        }
    }

    private boolean isBoundToCryptoProviderService() {
        return openPgpApi != null;
    }

    private void connectToCryptoProviderService() {
        openPgpServiceConnection = new OpenPgpServiceConnection(context, openPgpProvider,
                new OnBound() {

                    @Override
                    public void onBound(IOpenPgpService2 service) {
                        openPgpApi = openPgpApiFactory.createOpenPgpApi(context, service);

                        nextStep();
                    }

                    @Override
                    public void onError(Exception e) {
                        // TODO actually handle (hand to ui, offer retry?)
                        Log.e(e, "Couldn't connect to OpenPgpService");
                    }
                });
        openPgpServiceConnection.bindToService();
    }

    private void decryptOrVerifyCurrentPart() {
        Intent apiIntent = userInteractionResultIntent;
        userInteractionResultIntent = null;
        if (apiIntent == null) {
            apiIntent = getDecryptVerifyIntent();
        }
        decryptVerify(apiIntent);
    }

    @NonNull
    private Intent getDecryptVerifyIntent() {
        Intent decryptIntent = new Intent(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        Address[] from = currentMessage.getFrom();
        if (from.length > 0) {
            decryptIntent.putExtra(OpenPgpApi.EXTRA_SENDER_ADDRESS, from[0].getAddress());
            // we add this here independently of the autocrypt peer update, to allow picking up signing keys as gossip
            decryptIntent.putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID, from[0].getAddress());
        }
        autocryptOperations.addAutocryptPeerUpdateToIntentIfPresent(currentMessage, decryptIntent);

        decryptIntent.putExtra(OpenPgpApi.EXTRA_SUPPORT_OVERRIDE_CRYPTO_WARNING, true);
        decryptIntent.putExtra(OpenPgpApi.EXTRA_DECRYPTION_RESULT, cachedDecryptionResult);

        return decryptIntent;
    }

    private void decryptVerify(Intent apiIntent) {
        try {
            CryptoPartType cryptoPartType = currentCryptoPart.type;
            switch (cryptoPartType) {
                case PGP_SIGNED: {
                    callAsyncDetachedVerify(apiIntent);
                    return;
                }
                case PGP_ENCRYPTED: {
                    callAsyncDecrypt(apiIntent);
                    return;
                }
                case PGP_INLINE: {
                    callAsyncInlineOperation(apiIntent);
                    return;
                }
                case PLAIN_AUTOCRYPT:
                    throw new IllegalStateException("This part type must have been handled previously!");
            }

            throw new IllegalStateException("Unknown crypto part type: " + cryptoPartType);
        } catch (IOException e) {
            Log.e(e, "IOException");
        } catch (MessagingException e) {
            Log.e(e, "MessagingException");
        }
    }

    private void processAutocryptHeaderForCurrentPart() {
        Intent intent = new Intent(OpenPgpApi.ACTION_UPDATE_AUTOCRYPT_PEER);
        boolean hasInlineKeyData = autocryptOperations.addAutocryptPeerUpdateToIntentIfPresent(
                (Message) currentCryptoPart.part, intent);
        if (hasInlineKeyData) {
            Log.d("Passing autocrypt data from plain mail to OpenPGP API");
            // We don't care about the result here, so we just call this fire-and-forget wait to minimize delay
            openPgpApi.executeApiAsync(intent, null, null, new IOpenPgpCallback() {
                @Override
                public void onReturn(Intent result) {
                    Log.d("Autocrypt update OK!");
                }
            });
        }
        onCryptoFinished();
    }

    private void callAsyncInlineOperation(Intent intent) throws IOException {
        OpenPgpDataSource dataSource = getDataSourceForEncryptedOrInlineData();
        OpenPgpDataSink<MimeBodyPart> dataSink = getDataSinkForDecryptedInlineData();

        cancelableBackgroundOperation = openPgpApi.executeApiAsync(intent, dataSource, dataSink,
                new IOpenPgpSinkResultCallback<MimeBodyPart>() {
            @Override
            public void onProgress(int current, int max) {
                Log.d("received progress status: %d / %d", current, max);
                callbackProgress(current, max);
            }

            @Override
            public void onReturn(Intent result, MimeBodyPart bodyPart) {
                cancelableBackgroundOperation = null;
                currentCryptoResult = result;
                onCryptoOperationReturned(bodyPart);
            }
        });
    }

    public void cancelIfRunning() {
        detachCallback();
        isCancelled = true;
        if (cancelableBackgroundOperation != null) {
            cancelableBackgroundOperation.cancelOperation();
        }
    }

    private OpenPgpDataSink<MimeBodyPart> getDataSinkForDecryptedInlineData() {
        return new OpenPgpDataSink<MimeBodyPart>() {
            @Override
            public MimeBodyPart processData(InputStream is) throws IOException {
                try {
                    ByteArrayOutputStream decryptedByteOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(is, decryptedByteOutputStream);
                    TextBody body = new TextBody(new String(decryptedByteOutputStream.toByteArray()));
                    return new MimeBodyPart(body, "text/plain");
                } catch (MessagingException e) {
                    Log.e(e, "MessagingException");
                }

                return null;
            }
        };
    }

    private void callAsyncDecrypt(Intent intent) throws IOException {
        OpenPgpDataSource dataSource = getDataSourceForEncryptedOrInlineData();
        OpenPgpDataSink<MimeBodyPart> openPgpDataSink = getDataSinkForDecryptedData();

        cancelableBackgroundOperation = openPgpApi.executeApiAsync(intent, dataSource, openPgpDataSink,
                new IOpenPgpSinkResultCallback<MimeBodyPart>() {
            @Override
            public void onReturn(Intent result, MimeBodyPart decryptedPart) {
                cancelableBackgroundOperation = null;
                currentCryptoResult = result;
                onCryptoOperationReturned(decryptedPart);
            }

            @Override
            public void onProgress(int current, int max) {
                Log.d("received progress status: %d / %d", current, max);
                callbackProgress(current, max);
            }
        });
    }

    private void callAsyncDetachedVerify(Intent intent) throws IOException, MessagingException {
        OpenPgpDataSource dataSource = getDataSourceForSignedData(currentCryptoPart.part);

        byte[] signatureData = MessageCryptoStructureDetector.getSignatureData(currentCryptoPart.part);
        intent.putExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE, signatureData);

        openPgpApi.executeApiAsync(intent, dataSource, new IOpenPgpSinkResultCallback<Void>() {
            @Override
            public void onReturn(Intent result, Void dummy) {
                cancelableBackgroundOperation = null;
                currentCryptoResult = result;
                onCryptoOperationReturned(null);
            }

            @Override
            public void onProgress(int current, int max) {
                Log.d("received progress status: %d / %d", current, max);
                callbackProgress(current, max);
            }
        });
    }

    private OpenPgpDataSource getDataSourceForSignedData(final Part signedPart) {
        return new OpenPgpDataSource() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                try {
                    Multipart multipartSignedMultipart = (Multipart) signedPart.getBody();
                    BodyPart signatureBodyPart = multipartSignedMultipart.getBodyPart(0);
                    Log.d("signed data type: %s", signatureBodyPart.getMimeType());
                    signatureBodyPart.writeTo(os);
                } catch (MessagingException e) {
                    Log.e(e, "Exception while writing message to crypto provider");
                }
            }
        };
    }

    private OpenPgpDataSource getDataSourceForEncryptedOrInlineData() {
        return new OpenPgpApi.OpenPgpDataSource() {
            @Override
            public Long getSizeForProgress() {
                Part part = currentCryptoPart.part;
                CryptoPartType cryptoPartType = currentCryptoPart.type;
                Body body;
                if (cryptoPartType == CryptoPartType.PGP_ENCRYPTED) {
                    Multipart multipartEncryptedMultipart = (Multipart) part.getBody();
                    BodyPart encryptionPayloadPart = multipartEncryptedMultipart.getBodyPart(1);
                    body = encryptionPayloadPart.getBody();
                } else if (cryptoPartType == CryptoPartType.PGP_INLINE) {
                    body = part.getBody();
                } else {
                    throw new IllegalStateException("part to stream must be encrypted or inline!");
                }
                if (body instanceof SizeAware) {
                    return ((SizeAware) body).getSize();
                }
                return null;
            }

            @Override
            @WorkerThread
            public void writeTo(OutputStream os) throws IOException {
                try {
                    Part part = currentCryptoPart.part;
                    CryptoPartType cryptoPartType = currentCryptoPart.type;
                    if (cryptoPartType == CryptoPartType.PGP_ENCRYPTED) {
                        Multipart multipartEncryptedMultipart = (Multipart) part.getBody();
                        BodyPart encryptionPayloadPart = multipartEncryptedMultipart.getBodyPart(1);
                        Body encryptionPayloadBody = encryptionPayloadPart.getBody();
                        encryptionPayloadBody.writeTo(os);
                    } else if (cryptoPartType == CryptoPartType.PGP_INLINE) {
                        String text = MessageExtractor.getTextFromPart(part);
                        os.write(text.getBytes());
                    } else {
                        throw new IllegalStateException("part to stream must be encrypted or inline!");
                    }
                } catch (MessagingException e) {
                    Log.e(e, "MessagingException while writing message to crypto provider");
                }
            }
        };
    }

    private OpenPgpDataSink<MimeBodyPart> getDataSinkForDecryptedData() {
        return new OpenPgpDataSink<MimeBodyPart>() {
            @Override
            @WorkerThread
            public MimeBodyPart processData(InputStream is) throws IOException {
                try {
                    FileFactory fileFactory =
                            DecryptedFileProvider.getFileFactory(context);
                    return MimePartStreamParser.parse(fileFactory, is);
                } catch (MessagingException e) {
                    Log.e(e, "Something went wrong while parsing the decrypted MIME part");
                    //TODO: pass error to main thread and display error message to user
                    return null;
                }
            }
        };
    }

    private void onCryptoOperationReturned(MimeBodyPart decryptedPart) {
        if (currentCryptoResult == null) {
            Log.e("Internal error: we should have a result here!");
            return;
        }

        try {
            handleCryptoOperationResult(decryptedPart);
        } finally {
            currentCryptoResult = null;
        }
    }

    private void handleCryptoOperationResult(MimeBodyPart outputPart) {
        int resultCode = currentCryptoResult.getIntExtra(OpenPgpApi.RESULT_CODE, INVALID_OPENPGP_RESULT_CODE);
        Log.d("OpenPGP API decryptVerify result code: %d", resultCode);

        switch (resultCode) {
            case INVALID_OPENPGP_RESULT_CODE: {
                Log.e("Internal error: no result code!");
                break;
            }
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                handleUserInteractionRequest();
                break;
            }
            case OpenPgpApi.RESULT_CODE_ERROR: {
                handleCryptoOperationError();
                break;
            }
            case OpenPgpApi.RESULT_CODE_SUCCESS: {
                handleCryptoOperationSuccess(outputPart);
                break;
            }
        }
    }

    private void handleUserInteractionRequest() {
        PendingIntent pendingIntent = IntentCompat.getParcelableExtra(
            currentCryptoResult,
            OpenPgpApi.RESULT_INTENT,
            PendingIntent.class
        );
        if (pendingIntent == null) {
            throw new AssertionError("Expecting PendingIntent on USER_INTERACTION_REQUIRED!");
        }

        callbackPendingIntent(pendingIntent);
    }

    private void handleCryptoOperationError() {
        OpenPgpError error = IntentCompat.getParcelableExtra(
            currentCryptoResult,
            OpenPgpApi.RESULT_ERROR,
            OpenPgpError.class
        );
        Log.w("OpenPGP API error: %s", error.getMessage());

        onCryptoOperationFailed(error);
    }

    private void handleCryptoOperationSuccess(MimeBodyPart outputPart) {
        OpenPgpDecryptionResult decryptionResult = IntentCompat.getParcelableExtra(
            currentCryptoResult,
            OpenPgpApi.RESULT_DECRYPTION,
            OpenPgpDecryptionResult.class
        );
        OpenPgpSignatureResult signatureResult = IntentCompat.getParcelableExtra(
            currentCryptoResult,
            OpenPgpApi.RESULT_SIGNATURE,
            OpenPgpSignatureResult.class
        );
        if (decryptionResult.getResult() == OpenPgpDecryptionResult.RESULT_ENCRYPTED) {
            parseAutocryptGossipHeadersFromDecryptedPart(outputPart);
        }
        PendingIntent pendingIntent = IntentCompat.getParcelableExtra(
            currentCryptoResult,
            OpenPgpApi.RESULT_INTENT,
            PendingIntent.class
        );
        PendingIntent insecureWarningPendingIntent = IntentCompat.getParcelableExtra(
            currentCryptoResult,
            OpenPgpApi.RESULT_INSECURE_DETAIL_INTENT,
            PendingIntent.class
        );
        boolean overrideCryptoWarning = currentCryptoResult.getBooleanExtra(
                OpenPgpApi.RESULT_OVERRIDE_CRYPTO_WARNING, false);

        CryptoResultAnnotation resultAnnotation = CryptoResultAnnotation.createOpenPgpResultAnnotation(decryptionResult,
                signatureResult, pendingIntent, insecureWarningPendingIntent, outputPart, overrideCryptoWarning);

        onCryptoOperationSuccess(resultAnnotation);
    }

    private void parseAutocryptGossipHeadersFromDecryptedPart(MimeBodyPart outputPart) {
        if (!autocryptOperations.hasAutocryptGossipHeader(outputPart)) {
            return;
        }

        Intent intent = new Intent(OpenPgpApi.ACTION_UPDATE_AUTOCRYPT_PEER);
        boolean hasInlineKeyData = autocryptOperations.addAutocryptGossipUpdateToIntentIfPresent(
                currentMessage, outputPart, intent);
        if (hasInlineKeyData) {
            Log.d("Passing autocrypt data from plain mail to OpenPGP API");
            // We don't care about the result here, so we just call this fire-and-forget wait to minimize delay
            openPgpApi.executeApiAsync(intent, null, null, new IOpenPgpCallback() {
                @Override
                public void onReturn(Intent result) {
                    Log.d("Autocrypt update OK!");
                }
            });
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (isCancelled) {
            return;
        }

        if (requestCode != REQUEST_CODE_USER_INTERACTION) {
            throw new IllegalStateException("got an activity result that wasn't meant for us. this is a bug!");
        }
        if (resultCode == Activity.RESULT_OK) {
            userInteractionResultIntent = data;
            nextStep();
        } else {
            onCryptoOperationCanceled();
        }
    }

    private void onCryptoOperationSuccess(CryptoResultAnnotation resultAnnotation) {
        addCryptoResultAnnotationToMessage(resultAnnotation);
        onCryptoFinished();
    }

    private void propagateEncapsulatedSignedPart(CryptoResultAnnotation resultAnnotation, Part part) {
        Part encapsulatingPart = messageAnnotations.findKeyForAnnotationWithReplacementPart(part);
        CryptoResultAnnotation encapsulatingPartAnnotation = messageAnnotations.get(encapsulatingPart);

        if (encapsulatingPart != null && resultAnnotation.hasSignatureResult()) {
            CryptoResultAnnotation replacementAnnotation =
                    encapsulatingPartAnnotation.withEncapsulatedResult(resultAnnotation);
            messageAnnotations.put(encapsulatingPart, replacementAnnotation);
        }
    }

    private void onCryptoOperationCanceled() {
        // there are weird states that get us here when we're not actually processing any part. just skip in that case
        // see https://github.com/thunderbird/thunderbird-android/issues/1878
        if (currentCryptoPart != null) {
            CryptoResultAnnotation errorPart = CryptoResultAnnotation.createOpenPgpCanceledAnnotation();
            addCryptoResultAnnotationToMessage(errorPart);
        }
        onCryptoFinished();
    }

    private void onCryptoOperationFailed(OpenPgpError error) {
        CryptoResultAnnotation annotation;
        if (currentCryptoPart.type == CryptoPartType.PGP_SIGNED) {
            MimeBodyPart replacementPart = getMultipartSignedContentPartIfAvailable(currentCryptoPart.part);
            annotation = CryptoResultAnnotation.createOpenPgpSignatureErrorAnnotation(error, replacementPart);
        } else {
            annotation = CryptoResultAnnotation.createOpenPgpEncryptionErrorAnnotation(error);
        }
        addCryptoResultAnnotationToMessage(annotation);
        onCryptoFinished();
    }

    private void addCryptoResultAnnotationToMessage(CryptoResultAnnotation resultAnnotation) {
        Part part = currentCryptoPart.part;
        messageAnnotations.put(part, resultAnnotation);

        propagateEncapsulatedSignedPart(resultAnnotation, part);
    }

    private void onCryptoFinished() {
        boolean currentPartIsFirstInQueue = partsToProcess.peekFirst() == currentCryptoPart;
        if (!currentPartIsFirstInQueue) {
            throw new IllegalStateException(
                    "Trying to remove part from queue that is not the currently processed one!");
        }
        if (currentCryptoPart != null) {
            partsToProcess.removeFirst();
            currentCryptoPart = null;
        } else {
            Log.e(new Throwable(), "Got to onCryptoFinished() with no part in processing!");
        }
        nextStep();
    }

    private void findPartsForNextPass() {
        switch (state) {
            case START: {
                state = State.ENCRYPTION;

                findPartsForMultipartEncryptionPass();
                return;
            }

            case ENCRYPTION: {
                state = State.SIGNATURES_AND_INLINE;

                findPartsForMultipartSignaturePass();
                findPartsForPgpInlinePass();
                return;
            }

            case SIGNATURES_AND_INLINE: {
                state = State.AUTOCRYPT;

                findPartsForAutocryptPass();
                return;
            }

            case AUTOCRYPT: {
                state = State.FINISHED;
                return;
            }

            default: {
                throw new IllegalStateException("unhandled state");
            }
        }
    }

    private void cleanupAfterProcessingFinished() {
        partsToProcess.clear();
        openPgpApi = null;
        if (openPgpServiceConnection != null) {
            openPgpServiceConnection.unbindFromService();
        }
        openPgpServiceConnection = null;
    }

    public void detachCallback() {
        synchronized (callbackLock) {
            callback = null;
        }
    }

    private void reattachCallback(Message message, MessageCryptoCallback callback) {
        if (!message.equals(currentMessage)) {
            throw new AssertionError("Callback may only be reattached for the same message!");
        }
        synchronized (callbackLock) {
            this.callback = callback;

            boolean hasCachedResult = queuedResult != null || queuedPendingIntent != null;
            if (hasCachedResult) {
                Log.d("Returning cached result or pending intent to reattached callback");
                deliverResult();
            }
        }
    }

    private void callbackPendingIntent(PendingIntent pendingIntent) {
        synchronized (callbackLock) {
            queuedPendingIntent = pendingIntent;
            deliverResult();
        }
    }

    private void callbackReturnResult() {
        synchronized (callbackLock) {
            cleanupAfterProcessingFinished();

            queuedResult = messageAnnotations;
            messageAnnotations = null;

            deliverResult();
        }
    }

    private void callbackProgress(int current, int max) {
        synchronized (callbackLock) {
            if (callback != null) {
                callback.onCryptoHelperProgress(current, max);
            }
        }
    }

    // This method must only be called inside a synchronized(callbackLock) block!
    private void deliverResult() {
        if (isCancelled) {
            return;
        }

        if (callback == null) {
            Log.d("Keeping crypto helper result in queue for later delivery");
            return;
        }
        if (queuedResult != null) {
            callback.onCryptoOperationsFinished(queuedResult);
        } else if (queuedPendingIntent != null) {
            boolean pendingIntentHandled = callback.startPendingIntentForCryptoHelper(
                    queuedPendingIntent.getIntentSender(), REQUEST_CODE_USER_INTERACTION);
            if (pendingIntentHandled) {
                queuedPendingIntent = null;
            }
        } else {
            throw new IllegalStateException("deliverResult() called with no result!");
        }
    }

    private static class CryptoPart {
        public final CryptoPartType type;
        public final Part part;

        CryptoPart(CryptoPartType type, Part part) {
            this.type = type;
            this.part = part;
        }
    }

    private enum CryptoPartType {
        PGP_INLINE,
        PGP_ENCRYPTED,
        PGP_SIGNED,
        PLAIN_AUTOCRYPT
    }

    @Nullable
    private static MimeBodyPart getMultipartSignedContentPartIfAvailable(Part part) {
        MimeBodyPart replacementPart = NO_REPLACEMENT_PART;
        Body body = part.getBody();
        if (body instanceof MimeMultipart) {
            MimeMultipart multipart = ((MimeMultipart) part.getBody());
            if (multipart.getCount() >= 1) {
                replacementPart = (MimeBodyPart) multipart.getBodyPart(0);
            }
        }
        return replacementPart;
    }

    private enum State {
        START, ENCRYPTION, SIGNATURES_AND_INLINE, AUTOCRYPT, FINISHED
    }
}
