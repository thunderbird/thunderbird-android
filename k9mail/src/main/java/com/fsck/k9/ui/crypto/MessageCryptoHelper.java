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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.SizeAware;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;
import com.fsck.k9.mailstore.LocalMessage;
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
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpSinkResultCallback;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSink;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;
import org.openintents.openpgp.util.OpenPgpUtils;


public class MessageCryptoHelper {
    private static final int INVALID_OPENPGP_RESULT_CODE = -1;
    private static final MimeBodyPart NO_REPLACEMENT_PART = null;
    public static final int REQUEST_CODE_USER_INTERACTION = 124;
    public static final int PROGRESS_SIZE_THRESHOLD = 4096;


    private final Context context;
    private final String openPgpProviderPackage;
    private final Object callbackLock = new Object();

    @Nullable
    private MessageCryptoCallback callback;

    private LocalMessage currentMessage;
    private OpenPgpDecryptionResult cachedDecryptionResult;
    private MessageCryptoAnnotations queuedResult;
    private PendingIntent queuedPendingIntent;


    private MessageCryptoAnnotations messageAnnotations;
    private Deque<CryptoPart> partsToDecryptOrVerify = new ArrayDeque<>();
    private CryptoPart currentCryptoPart;
    private Intent currentCryptoResult;
    private Intent userInteractionResultIntent;
    private boolean secondPassStarted;
    private CancelableBackgroundOperation cancelableBackgroundOperation;
    private boolean isCancelled;

    private OpenPgpApi openPgpApi;
    private OpenPgpServiceConnection openPgpServiceConnection;


    public MessageCryptoHelper(Context context, String openPgpProviderPackage) {
        this.context = context.getApplicationContext();
        this.openPgpProviderPackage = openPgpProviderPackage;

        if (openPgpProviderPackage == null || Account.NO_OPENPGP_PROVIDER.equals(openPgpProviderPackage)) {
            throw new IllegalStateException("MessageCryptoHelper must only be called with a openpgp provider!");
        }
    }

    public void asyncStartOrResumeProcessingMessage(LocalMessage message, MessageCryptoCallback callback,
            OpenPgpDecryptionResult cachedDecryptionResult) {
        if (this.currentMessage != null) {
            reattachCallback(message, callback);
            return;
        }

        this.messageAnnotations = new MessageCryptoAnnotations();
        this.currentMessage = message;
        this.cachedDecryptionResult = cachedDecryptionResult;
        this.callback = callback;

        runFirstPass();
    }

    private void runFirstPass() {
        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(currentMessage);
        processFoundEncryptedParts(encryptedParts);

        decryptOrVerifyNextPart();
    }

    private void runSecondPass() {
        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(currentMessage, messageAnnotations);
        processFoundSignedParts(signedParts);

        List<Part> inlineParts = MessageDecryptVerifier.findPgpInlineParts(currentMessage);
        addFoundInlinePgpParts(inlineParts);

        decryptOrVerifyNextPart();
    }

    private void processFoundEncryptedParts(List<Part> foundParts) {
        for (Part part : foundParts) {
            if (!MessageHelper.isCompletePartAvailable(part)) {
                addErrorAnnotation(part, CryptoError.OPENPGP_ENCRYPTED_BUT_INCOMPLETE, MessageHelper.createEmptyPart());
                continue;
            }
            if (MessageDecryptVerifier.isPgpMimeEncryptedOrSignedPart(part)) {
                CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_ENCRYPTED, part);
                partsToDecryptOrVerify.add(cryptoPart);
                continue;
            }
            addErrorAnnotation(part, CryptoError.ENCRYPTED_BUT_UNSUPPORTED, MessageHelper.createEmptyPart());
        }
    }

    private void processFoundSignedParts(List<Part> foundParts) {
        for (Part part : foundParts) {
            if (!MessageHelper.isCompletePartAvailable(part)) {
                MimeBodyPart replacementPart = getMultipartSignedContentPartIfAvailable(part);
                addErrorAnnotation(part, CryptoError.OPENPGP_SIGNED_BUT_INCOMPLETE, replacementPart);
                continue;
            }
            if (MessageDecryptVerifier.isPgpMimeEncryptedOrSignedPart(part)) {
                CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_SIGNED, part);
                partsToDecryptOrVerify.add(cryptoPart);
                continue;
            }
            MimeBodyPart replacementPart = getMultipartSignedContentPartIfAvailable(part);
            addErrorAnnotation(part, CryptoError.SIGNED_BUT_UNSUPPORTED, replacementPart);
        }
    }

    private void addErrorAnnotation(Part part, CryptoError error, MimeBodyPart replacementPart) {
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createErrorAnnotation(error, replacementPart);
        messageAnnotations.put(part, annotation);
    }

    private void addFoundInlinePgpParts(List<Part> foundParts) {
        for (Part part : foundParts) {
            if (!currentMessage.getFlags().contains(Flag.X_DOWNLOADED_FULL)) {
                if (MessageDecryptVerifier.isPartPgpInlineEncrypted(part)) {
                    addErrorAnnotation(part, CryptoError.OPENPGP_ENCRYPTED_BUT_INCOMPLETE, NO_REPLACEMENT_PART);
                } else {
                    MimeBodyPart replacementPart = extractClearsignedTextReplacementPart(part);
                    addErrorAnnotation(part, CryptoError.OPENPGP_SIGNED_BUT_INCOMPLETE, replacementPart);
                }
                continue;
            }

            CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_INLINE, part);
            partsToDecryptOrVerify.add(cryptoPart);
        }
    }

    private void decryptOrVerifyNextPart() {
        if (isCancelled) {
            return;
        }

        if (partsToDecryptOrVerify.isEmpty()) {
            runSecondPassOrReturnResultToFragment();
            return;
        }

        CryptoPart cryptoPart = partsToDecryptOrVerify.peekFirst();
        startDecryptingOrVerifyingPart(cryptoPart);
    }

    private void startDecryptingOrVerifyingPart(CryptoPart cryptoPart) {
        if (!isBoundToCryptoProviderService()) {
            connectToCryptoProviderService();
        } else {
            decryptOrVerifyPart(cryptoPart);
        }
    }

    private boolean isBoundToCryptoProviderService() {
        return openPgpApi != null;
    }

    private void connectToCryptoProviderService() {
        openPgpServiceConnection = new OpenPgpServiceConnection(context, openPgpProviderPackage,
                new OnBound() {
                    @Override
                    public void onBound(IOpenPgpService2 service) {
                        openPgpApi = new OpenPgpApi(context, service);

                        decryptOrVerifyNextPart();
                    }

                    @Override
                    public void onError(Exception e) {
                        // TODO actually handle (hand to ui, offer retry?)
                        Log.e(K9.LOG_TAG, "Couldn't connect to OpenPgpService", e);
                    }
                });
        openPgpServiceConnection.bindToService();
    }

    private void decryptOrVerifyPart(CryptoPart cryptoPart) {
        currentCryptoPart = cryptoPart;
        Intent decryptIntent = userInteractionResultIntent;
        userInteractionResultIntent = null;
        if (decryptIntent == null) {
            decryptIntent = getDecryptionIntent();
        }
        decryptVerify(decryptIntent);
    }

    @NonNull
    private Intent getDecryptionIntent() {
        Intent decryptIntent = new Intent(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        Address[] from = currentMessage.getFrom();
        if (from.length > 0) {
            decryptIntent.putExtra(OpenPgpApi.EXTRA_SENDER_ADDRESS, from[0].getAddress());
        }

        decryptIntent.putExtra(OpenPgpApi.EXTRA_DECRYPTION_RESULT, cachedDecryptionResult);

        return decryptIntent;
    }

    private void decryptVerify(Intent intent) {
        try {
            CryptoPartType cryptoPartType = currentCryptoPart.type;
            switch (cryptoPartType) {
                case PGP_SIGNED: {
                    callAsyncDetachedVerify(intent);
                    return;
                }
                case PGP_ENCRYPTED: {
                    callAsyncDecrypt(intent);
                    return;
                }
                case PGP_INLINE: {
                    callAsyncInlineOperation(intent);
                    return;
                }
            }

            throw new IllegalStateException("Unknown crypto part type: " + cryptoPartType);
        } catch (IOException e) {
            Log.e(K9.LOG_TAG, "IOException", e);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "MessagingException", e);
        }
    }

    private void callAsyncInlineOperation(Intent intent) throws IOException {
        OpenPgpDataSource dataSource = getDataSourceForEncryptedOrInlineData();
        OpenPgpDataSink<MimeBodyPart> dataSink = getDataSinkForDecryptedInlineData();

        cancelableBackgroundOperation = openPgpApi.executeApiAsync(intent, dataSource, dataSink,
                new IOpenPgpSinkResultCallback<MimeBodyPart>() {
            @Override
            public void onProgress(int current, int max) {
                Log.d(K9.LOG_TAG, "received progress status: " + current + " / " + max);
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
                    Log.e(K9.LOG_TAG, "MessagingException", e);
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
                Log.d(K9.LOG_TAG, "received progress status: " + current + " / " + max);
                callbackProgress(current, max);
            }
        });
    }

    private void callAsyncDetachedVerify(Intent intent) throws IOException, MessagingException {
        OpenPgpDataSource dataSource = getDataSourceForSignedData(currentCryptoPart.part);

        byte[] signatureData = MessageDecryptVerifier.getSignatureData(currentCryptoPart.part);
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
                Log.d(K9.LOG_TAG, "received progress status: " + current + " / " + max);
                callbackProgress(current, max);
            }
        });
    }

    private OpenPgpDataSource getDataSourceForSignedData(final Part signedPart) throws IOException {
        return new OpenPgpDataSource() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                try {
                    Multipart multipartSignedMultipart = (Multipart) signedPart.getBody();
                    BodyPart signatureBodyPart = multipartSignedMultipart.getBodyPart(0);
                    Log.d(K9.LOG_TAG, "signed data type: " + signatureBodyPart.getMimeType());
                    signatureBodyPart.writeTo(os);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Exception while writing message to crypto provider", e);
                }
            }
        };
    }

    private OpenPgpDataSource getDataSourceForEncryptedOrInlineData() throws IOException {
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
                    long bodySize = ((SizeAware) body).getSize();
                    if (bodySize > PROGRESS_SIZE_THRESHOLD) {
                        return bodySize;
                    }
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
                    Log.e(K9.LOG_TAG, "MessagingException while writing message to crypto provider", e);
                }
            }
        };
    }

    private OpenPgpDataSink<MimeBodyPart> getDataSinkForDecryptedData() throws IOException {
        return new OpenPgpDataSink<MimeBodyPart>() {
            @Override
            @WorkerThread
            public MimeBodyPart processData(InputStream is) throws IOException {
                try {
                    FileFactory fileFactory =
                            DecryptedFileProvider.getFileFactory(context);
                    return MimePartStreamParser.parse(fileFactory, is);
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "Something went wrong while parsing the decrypted MIME part", e);
                    //TODO: pass error to main thread and display error message to user
                    return null;
                }
            }
        };
    }

    private void onCryptoOperationReturned(MimeBodyPart decryptedPart) {
        if (currentCryptoResult == null) {
            Log.e(K9.LOG_TAG, "Internal error: we should have a result here!");
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
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "OpenPGP API decryptVerify result code: " + resultCode);
        }

        switch (resultCode) {
            case INVALID_OPENPGP_RESULT_CODE: {
                Log.e(K9.LOG_TAG, "Internal error: no result code!");
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
        PendingIntent pendingIntent = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
        if (pendingIntent == null) {
            throw new AssertionError("Expecting PendingIntent on USER_INTERACTION_REQUIRED!");
        }

        callbackPendingIntent(pendingIntent);
    }

    private void handleCryptoOperationError() {
        OpenPgpError error = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
        if (K9.DEBUG) {
            Log.w(K9.LOG_TAG, "OpenPGP API error: " + error.getMessage());
        }

        onCryptoOperationFailed(error);
    }

    private void handleCryptoOperationSuccess(MimeBodyPart outputPart) {
        OpenPgpDecryptionResult decryptionResult =
                currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_DECRYPTION);
        OpenPgpSignatureResult signatureResult =
                currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
        PendingIntent pendingIntent = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

        CryptoResultAnnotation resultAnnotation = CryptoResultAnnotation.createOpenPgpResultAnnotation(
                decryptionResult, signatureResult, pendingIntent, outputPart);

        onCryptoOperationSuccess(resultAnnotation);
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
            decryptOrVerifyNextPart();
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
        CryptoResultAnnotation errorPart = CryptoResultAnnotation.createOpenPgpCanceledAnnotation();
        addCryptoResultAnnotationToMessage(errorPart);
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
        currentCryptoPart = null;
        partsToDecryptOrVerify.removeFirst();
        decryptOrVerifyNextPart();
    }

    private void runSecondPassOrReturnResultToFragment() {
        if (secondPassStarted) {
            callbackReturnResult();
            return;
        }
        secondPassStarted = true;
        runSecondPass();
    }

    private void cleanupAfterProcessingFinished() {
        partsToDecryptOrVerify = null;
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

    private void reattachCallback(LocalMessage message, MessageCryptoCallback callback) {
        if (!message.equals(currentMessage)) {
            throw new AssertionError("Callback may only be reattached for the same message!");
        }
        synchronized (callbackLock) {
            if (queuedResult != null) {
                Log.d(K9.LOG_TAG, "Returning cached result to reattached callback");
            }
            this.callback = callback;
            deliverResult();
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
            Log.d(K9.LOG_TAG, "Keeping crypto helper result in queue for later delivery");
            return;
        }
        if (queuedResult != null) {
            callback.onCryptoOperationsFinished(queuedResult);
        } else if (queuedPendingIntent != null) {
            callback.startPendingIntentForCryptoHelper(
                    queuedPendingIntent.getIntentSender(), REQUEST_CODE_USER_INTERACTION, null, 0, 0, 0);
            queuedPendingIntent = null;
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
        PGP_SIGNED
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

    private static MimeBodyPart extractClearsignedTextReplacementPart(Part part) {
        try {
            String clearsignedText = MessageExtractor.getTextFromPart(part);
            String replacementText = OpenPgpUtils.extractClearsignedMessage(clearsignedText);
            if (replacementText == null) {
                Log.e(K9.LOG_TAG, "failed to extract clearsigned text for replacement part");
                return NO_REPLACEMENT_PART;
            }
            return new MimeBodyPart(new TextBody(replacementText), "text/plain");
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "failed to create clearsigned text replacement part", e);
            return NO_REPLACEMENT_PART;
        }
    }

}
