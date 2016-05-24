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
import android.content.IntentSender.SendIntentException;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
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
import com.fsck.k9.mailstore.DecryptStreamParser;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageHelper;
import org.apache.commons.io.IOUtils;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpSinkResultCallback;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSink;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;


public class MessageCryptoHelper {
    private static final int REQUEST_CODE_CRYPTO = 1000;
    private static final int INVALID_OPENPGP_RESULT_CODE = -1;
    private static final MimeBodyPart NO_REPLACEMENT_PART = null;


    private final Context context;
    private final Activity activity;
    private final MessageCryptoCallback callback;
    private final Account account;

    private Deque<CryptoPart> partsToDecryptOrVerify = new ArrayDeque<>();
    private OpenPgpApi openPgpApi;
    private CryptoPart currentCryptoPart;
    private Intent currentCryptoResult;

    private MessageCryptoAnnotations messageAnnotations;
    private Intent userInteractionResultIntent;
    private LocalMessage currentMessage;
    private boolean secondPassStarted;


    public MessageCryptoHelper(Activity activity, Account account, MessageCryptoCallback callback) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.callback = callback;
        this.account = account;
    }

    public void decryptOrVerifyMessagePartsIfNecessary(LocalMessage message) {
        if (!account.isOpenPgpProviderConfigured()) {
            returnResultToFragment();
            return;
        }

        this.messageAnnotations = new MessageCryptoAnnotations();
        this.currentMessage = message;

        runFirstPass();
    }

    private void runFirstPass() {
        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(currentMessage);
        processFoundEncryptedParts(encryptedParts, MessageHelper.createEmptyPart());

        decryptOrVerifyNextPart();
    }

    private void runSecondPass() {
        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(currentMessage, messageAnnotations);
        processFoundSignedParts(signedParts);
        List<Part> inlineParts = MessageDecryptVerifier.findPgpInlineParts(currentMessage);
        addFoundInlinePgpParts(inlineParts);

        decryptOrVerifyNextPart();
    }

    private void processFoundEncryptedParts(List<Part> foundParts, MimeBodyPart replacementPart) {
        for (Part part : foundParts) {
            if (!MessageHelper.isCompletePartAvailable(part)) {
                addErrorAnnotation(part, CryptoError.ENCRYPTED_BUT_INCOMPLETE, replacementPart);
                continue;
            }
            if (MessageDecryptVerifier.isPgpMimeEncryptedOrSignedPart(part)) {
                CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_ENCRYPTED, part);
                partsToDecryptOrVerify.add(cryptoPart);
                continue;
            }
            addErrorAnnotation(part, CryptoError.ENCRYPTED_BUT_UNSUPPORTED, replacementPart);
        }
    }

    private void processFoundSignedParts(List<Part> foundParts) {
        for (Part part : foundParts) {
            if (!MessageHelper.isCompletePartAvailable(part)) {
                MimeBodyPart replacementPart = getMultipartSignedContentPartIfAvailable(part);
                addErrorAnnotation(part, CryptoError.SIGNED_BUT_INCOMPLETE, replacementPart);
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
            CryptoPart cryptoPart = new CryptoPart(CryptoPartType.PGP_INLINE, part);
            partsToDecryptOrVerify.add(cryptoPart);
        }
    }

    private void decryptOrVerifyNextPart() {
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
        String openPgpProvider = account.getOpenPgpProvider();
        new OpenPgpServiceConnection(context, openPgpProvider,
                new OnBound() {
                    @Override
                    public void onBound(IOpenPgpService2 service) {
                        openPgpApi = new OpenPgpApi(context, service);

                        decryptOrVerifyNextPart();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(K9.LOG_TAG, "Couldn't connect to OpenPgpService", e);
                    }
                }).bindToService();
    }

    private void decryptOrVerifyPart(CryptoPart cryptoPart) {
        currentCryptoPart = cryptoPart;
        Intent decryptIntent = userInteractionResultIntent;
        userInteractionResultIntent = null;
        if (decryptIntent == null) {
            decryptIntent = new Intent();
        }
        decryptVerify(decryptIntent);
    }

    private void decryptVerify(Intent intent) {
        intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

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

        openPgpApi.executeApiAsync(intent, dataSource, dataSink, new IOpenPgpSinkResultCallback<MimeBodyPart>() {
            @Override
            public void onProgress(int current, int max) {
                Log.d(K9.LOG_TAG, "received progress status: " + current + " / " + max);
                callback.onCryptoHelperProgress(current, max);
            }

            @Override
            public void onReturn(Intent result, MimeBodyPart bodyPart) {
                currentCryptoResult = result;
                onCryptoOperationReturned(bodyPart);
            }
        });
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

        openPgpApi.executeApiAsync(intent, dataSource, openPgpDataSink, new IOpenPgpSinkResultCallback<MimeBodyPart>() {
            @Override
            public void onReturn(Intent result, MimeBodyPart decryptedPart) {
                currentCryptoResult = result;
                onCryptoOperationReturned(decryptedPart);
            }

            @Override
            public void onProgress(int current, int max) {
                Log.d(K9.LOG_TAG, "got progress: " + current + " / " + max);
            }
        });
    }

    private void callAsyncDetachedVerify(Intent intent) throws IOException, MessagingException {
        OpenPgpDataSource dataSource = getDataSourceForSignedData();

        byte[] signatureData = MessageDecryptVerifier.getSignatureData(currentCryptoPart.part);
        intent.putExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE, signatureData);

        openPgpApi.executeApiAsync(intent, dataSource, new IOpenPgpSinkResultCallback<Void>() {
            @Override
            public void onReturn(Intent result, Void dummy) {
                currentCryptoResult = result;
                onCryptoOperationReturned(null);
            }

            @Override
            public void onProgress(int current, int max) {
                Log.d(K9.LOG_TAG, "got progress: " + current + " / " + max);
            }
        });
    }

    private OpenPgpDataSource getDataSourceForSignedData() throws IOException {
        return new OpenPgpDataSource() {
            @Override
            public void writeTo(OutputStream os) throws IOException {
                try {
                    Multipart multipartSignedMultipart = (Multipart) currentCryptoPart.part.getBody();
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
            public Long getTotalDataSize() {
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
                    return DecryptStreamParser.parse(context, is);
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

        try {
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_CODE_CRYPTO, null, 0, 0, 0);
        } catch (SendIntentException e) {
            Log.e(K9.LOG_TAG, "Internal error on starting pendingintent!", e);
        }
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

        onCryptoSuccess(resultAnnotation);
    }

    public void handleCryptoResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_CRYPTO) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            userInteractionResultIntent = data;
            decryptOrVerifyNextPart();
        } else {
            onCryptoOperationCanceled();
        }
    }

    private void onCryptoSuccess(CryptoResultAnnotation resultAnnotation) {
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
        CryptoResultAnnotation errorPart = CryptoResultAnnotation.createOpenPgpErrorAnnotation(error);
        addCryptoResultAnnotationToMessage(errorPart);
        onCryptoFinished();
    }

    private void addCryptoResultAnnotationToMessage(CryptoResultAnnotation resultAnnotation) {
        Part part = currentCryptoPart.part;
        messageAnnotations.put(part, resultAnnotation);

        propagateEncapsulatedSignedPart(resultAnnotation, part);
    }

    private void onCryptoFinished() {
        partsToDecryptOrVerify.removeFirst();
        decryptOrVerifyNextPart();
    }

    private void runSecondPassOrReturnResultToFragment() {
        if (secondPassStarted) {
            callback.onCryptoOperationsFinished(messageAnnotations);
            return;
        }
        secondPassStarted = true;
        runSecondPass();
    }

    private void returnResultToFragment() {
        callback.onCryptoOperationsFinished(messageAnnotations);
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
}
