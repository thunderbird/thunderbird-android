package com.fsck.k9.ui.crypto;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;
import com.fsck.k9.mailstore.DecryptStreamParser;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.MessageHelper;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
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
    private LocalMessage message;

    private Deque<CryptoPart> partsToDecryptOrVerify = new ArrayDeque<>();
    private OpenPgpApi openPgpApi;
    private CryptoPart currentCryptoPart;
    private Intent currentCryptoResult;

    private MessageCryptoAnnotations messageAnnotations;
    private Intent userInteractionResultIntent;


    public MessageCryptoHelper(Activity activity, Account account, MessageCryptoCallback callback) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.callback = callback;
        this.account = account;

        this.messageAnnotations = new MessageCryptoAnnotations();
    }

    public void decryptOrVerifyMessagePartsIfNecessary(LocalMessage message) {
        this.message = message;

        if (!account.isOpenPgpProviderConfigured()) {
            returnResultToFragment();
            return;
        }

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);
        processFoundParts(encryptedParts, CryptoPartType.ENCRYPTED, CryptoError.ENCRYPTED_BUT_INCOMPLETE,
                MessageHelper.createEmptyPart());

        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message);
        processFoundParts(signedParts, CryptoPartType.SIGNED, CryptoError.SIGNED_BUT_INCOMPLETE, NO_REPLACEMENT_PART);

        List<Part> inlineParts = MessageDecryptVerifier.findPgpInlineParts(message);
        addFoundInlinePgpParts(inlineParts);

        decryptOrVerifyNextPart();
    }

    private void processFoundParts(List<Part> foundParts, CryptoPartType cryptoPartType, CryptoError errorIfIncomplete,
            MimeBodyPart replacementPart) {
        for (Part part : foundParts) {
            if (MessageHelper.isCompletePartAvailable(part)) {
                CryptoPart cryptoPart = new CryptoPart(cryptoPartType, part);
                partsToDecryptOrVerify.add(cryptoPart);
            } else {
                addErrorAnnotation(part, errorIfIncomplete, replacementPart);
            }
        }
    }

    private void addErrorAnnotation(Part part, CryptoError error, MimeBodyPart replacementPart) {
        CryptoResultAnnotation annotation = CryptoResultAnnotation.createErrorAnnotation(error, replacementPart);
        messageAnnotations.put(part, annotation);
    }

    private void addFoundInlinePgpParts(List<Part> foundParts) {
        for (Part part : foundParts) {
            CryptoPart cryptoPart = new CryptoPart(CryptoPartType.INLINE_PGP, part);
            partsToDecryptOrVerify.add(cryptoPart);
        }
    }

    private void decryptOrVerifyNextPart() {
        if (partsToDecryptOrVerify.isEmpty()) {
            returnResultToFragment();
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
                case SIGNED: {
                    callAsyncDetachedVerify(intent);
                    return;
                }
                case ENCRYPTED: {
                    callAsyncDecrypt(intent);
                    return;
                }
                case INLINE_PGP: {
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
        PipedInputStream pipedInputStream = getPipedInputStreamForEncryptedOrInlineData();
        final ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

        openPgpApi.executeApiAsync(intent, pipedInputStream, decryptedOutputStream, new IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;

                MimeBodyPart decryptedPart = null;
                try {
                    TextBody body = new TextBody(new String(decryptedOutputStream.toByteArray()));
                    decryptedPart = new MimeBodyPart(body, "text/plain");
                } catch (MessagingException e) {
                    Log.e(K9.LOG_TAG, "MessagingException", e);
                }

                onCryptoOperationReturned(decryptedPart);
            }
        });
    }

    private void callAsyncDecrypt(Intent intent) throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        PipedInputStream pipedInputStream = getPipedInputStreamForEncryptedOrInlineData();
        PipedOutputStream decryptedOutputStream = getPipedOutputStreamForDecryptedData(latch);

        openPgpApi.executeApiAsync(intent, pipedInputStream, decryptedOutputStream, new IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                latch.countDown();
            }
        });
    }

    private void callAsyncDetachedVerify(Intent intent) throws IOException, MessagingException {
        PipedInputStream pipedInputStream = getPipedInputStreamForSignedData();

        byte[] signatureData = MessageDecryptVerifier.getSignatureData(currentCryptoPart.part);
        intent.putExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE, signatureData);

        openPgpApi.executeApiAsync(intent, pipedInputStream, null, new IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                onCryptoOperationReturned(null);
            }
        });
    }

    private PipedInputStream getPipedInputStreamForSignedData() throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();

        final PipedOutputStream out = new PipedOutputStream(pipedInputStream);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Multipart multipartSignedMultipart = (Multipart) currentCryptoPart.part.getBody();
                    BodyPart signatureBodyPart = multipartSignedMultipart.getBodyPart(0);
                    Log.d(K9.LOG_TAG, "signed data type: " + signatureBodyPart.getMimeType());
                    signatureBodyPart.writeTo(out);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Exception while writing message to crypto provider", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // don't care
                    }
                }
            }
        }).start();

        return pipedInputStream;
    }

    private PipedInputStream getPipedInputStreamForEncryptedOrInlineData() throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();

        final PipedOutputStream out = new PipedOutputStream(pipedInputStream);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Part part = currentCryptoPart.part;
                    CryptoPartType cryptoPartType = currentCryptoPart.type;
                    if (cryptoPartType == CryptoPartType.ENCRYPTED) {
                        Multipart multipartEncryptedMultipart = (Multipart) part.getBody();
                        BodyPart encryptionPayloadPart = multipartEncryptedMultipart.getBodyPart(1);
                        Body encryptionPayloadBody = encryptionPayloadPart.getBody();
                        encryptionPayloadBody.writeTo(out);
                    } else if (cryptoPartType == CryptoPartType.INLINE_PGP) {
                        String text = MessageExtractor.getTextFromPart(part);
                        out.write(text.getBytes());
                    } else {
                        Log.wtf(K9.LOG_TAG, "No suitable data to stream found!");
                    }
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Exception while writing message to crypto provider", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // don't care
                    }
                }
            }
        }).start();

        return pipedInputStream;
    }

    private PipedOutputStream getPipedOutputStreamForDecryptedData(final CountDownLatch latch) throws IOException {
        PipedOutputStream decryptedOutputStream = new PipedOutputStream();
        final PipedInputStream decryptedInputStream = new PipedInputStream(decryptedOutputStream);
        new AsyncTask<Void, Void, MimeBodyPart>() {
            @Override
            protected MimeBodyPart doInBackground(Void... params) {
                MimeBodyPart decryptedPart = null;
                try {
                    decryptedPart = DecryptStreamParser.parse(context, decryptedInputStream);

                    latch.await();
                } catch (InterruptedException e) {
                    Log.w(K9.LOG_TAG, "we were interrupted while waiting for onReturn!", e);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Something went wrong while parsing the decrypted MIME part", e);
                    //TODO: pass error to main thread and display error message to user
                }
                return decryptedPart;
            }

            @Override
            protected void onPostExecute(MimeBodyPart decryptedPart) {
                onCryptoOperationReturned(decryptedPart);
            }
        }.execute();
        return decryptedOutputStream;
    }

    private void onCryptoOperationReturned(MimeBodyPart outputPart) {
        if (currentCryptoResult == null) {
            Log.e(K9.LOG_TAG, "Internal error: we should have a result here!");
            return;
        }

        try {
            handleCryptoOperationResult(outputPart);
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

        onCryptoFailed(error);
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
            onCryptoFailed(new OpenPgpError(OpenPgpError.CLIENT_SIDE_ERROR, context.getString(R.string.openpgp_canceled_by_user)));
        }
    }

    private void onCryptoSuccess(CryptoResultAnnotation resultAnnotation) {
        addCryptoResultAnnotationToMessage(resultAnnotation);
        onCryptoFinished();
    }

    private void onCryptoFailed(OpenPgpError error) {
        CryptoResultAnnotation errorPart = CryptoResultAnnotation.createOpenPgpErrorAnnotation(error);
        addCryptoResultAnnotationToMessage(errorPart);
        onCryptoFinished();
    }

    private void addCryptoResultAnnotationToMessage(CryptoResultAnnotation resultAnnotation) {
        Part part = currentCryptoPart.part;
        messageAnnotations.put(part, resultAnnotation);
    }

    private void onCryptoFinished() {
        partsToDecryptOrVerify.removeFirst();
        decryptOrVerifyNextPart();
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
        INLINE_PGP,
        ENCRYPTED,
        SIGNED
    }
}
