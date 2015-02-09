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
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.crypto.OpenPgpApiHelper;
import com.fsck.k9.helper.IdentityHelper;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.DecryptStreamParser;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.OpenPgpResultAnnotation;
import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;


public class MessageCryptoHelper {
    private static final int REQUEST_CODE_CRYPTO = 1000;
    private static final int INVALID_OPENPGP_RESULT_CODE = -1;


    private final Context context;
    private final Activity activity;
    private final MessageCryptoCallback callback;
    private final Account account;
    private LocalMessage message;

    private Deque<Part> partsToDecryptOrVerify;
    private OpenPgpApi openPgpApi;
    private Part currentlyDecryptingOrVerifyingPart;
    private Intent currentCryptoResult;

    private MessageCryptoAnnotations messageAnnotations;


    public MessageCryptoHelper(Activity activity, Account account, MessageCryptoCallback callback) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.callback = callback;
        this.account = account;

        this.messageAnnotations = new MessageCryptoAnnotations();
    }

    public void decryptOrVerifyMessagePartsIfNecessary(LocalMessage message) {
        this.message = message;

        if (!isCryptoProviderConfigured()) {
            returnResultToFragment();
            return;
        }

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);
        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message);
        List<Part> inlineParts = MessageDecryptVerifier.findPgpInlineParts(message);
        if (!encryptedParts.isEmpty() || !signedParts.isEmpty() || !inlineParts.isEmpty()) {
            partsToDecryptOrVerify = new ArrayDeque<Part>();
            partsToDecryptOrVerify.addAll(encryptedParts);
            partsToDecryptOrVerify.addAll(signedParts);
            partsToDecryptOrVerify.addAll(inlineParts);
            decryptOrVerifyNextPart();
        } else {
            returnResultToFragment();
        }
    }

    private boolean isCryptoProviderConfigured() {
        return !TextUtils.isEmpty(account.getCryptoApp());
    }

    private void decryptOrVerifyNextPart() {
        if (partsToDecryptOrVerify.isEmpty()) {
            returnResultToFragment();
            return;
        }

        Part part = partsToDecryptOrVerify.peekFirst();
        if ("text/plain".equalsIgnoreCase(part.getMimeType())) {
            startDecryptingOrVerifyingPart(part);
        } else if (MessageDecryptVerifier.isPgpMimePart(part)) {
            Multipart multipart = (Multipart) part.getBody();
            if (multipart == null) {
                throw new RuntimeException("Downloading missing parts before decryption isn't supported yet");
            }

            startDecryptingOrVerifyingPart(part);
        } else {
            partsToDecryptOrVerify.removeFirst();
            decryptOrVerifyNextPart();
        }
    }

    private void startDecryptingOrVerifyingPart(Part part) {
        if (!isBoundToCryptoProviderService()) {
            connectToCryptoProviderService();
        } else {
            decryptOrVerifyPart(part);
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
                    public void onBound(IOpenPgpService service) {
                        openPgpApi = new OpenPgpApi(context, service);

                        decryptOrVerifyNextPart();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(K9.LOG_TAG, "Couldn't connect to OpenPgpService", e);
                    }
                }).bindToService();
    }

    private void decryptOrVerifyPart(Part part) {
        currentlyDecryptingOrVerifyingPart = part;
        decryptVerify(new Intent());
    }

    private void decryptVerify(Intent intent) {
        intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, message);
        String accountName = OpenPgpApiHelper.buildAccountName(identity);
        intent.putExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME, accountName);

        try {
            if (MessageDecryptVerifier.isPgpMimeSignedPart(currentlyDecryptingOrVerifyingPart)) {
                callAsyncDetachedVerify(intent);
            } else if (MessageDecryptVerifier.isPgpInlinePart(currentlyDecryptingOrVerifyingPart)) {
                callAsyncInlineOperation(intent);
            } else {
                callAsyncDecrypt(intent);
            }
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

                onCryptoConverge(decryptedPart);
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

        byte[] signatureData = MessageDecryptVerifier.getSignatureData(currentlyDecryptingOrVerifyingPart);
        intent.putExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE, signatureData);

        openPgpApi.executeApiAsync(intent, pipedInputStream, null, new IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                currentCryptoResult = result;
                onCryptoConverge(null);
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
                    Multipart multipartSignedMultipart = (Multipart) currentlyDecryptingOrVerifyingPart.getBody();
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
                    if (MessageDecryptVerifier.isPgpMimePart(currentlyDecryptingOrVerifyingPart)) {
                        Multipart multipartEncryptedMultipart =
                                (Multipart) currentlyDecryptingOrVerifyingPart.getBody();
                        BodyPart encryptionPayloadPart = multipartEncryptedMultipart.getBodyPart(1);
                        Body encryptionPayloadBody = encryptionPayloadPart.getBody();
                        encryptionPayloadBody.writeTo(out);
                    } else if (MessageDecryptVerifier.isPgpInlinePart(currentlyDecryptingOrVerifyingPart)) {
                        String text = MessageExtractor.getTextFromPart(currentlyDecryptingOrVerifyingPart);
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
                onCryptoConverge(decryptedPart);
            }
        }.execute();
        return decryptedOutputStream;
    }

    private void onCryptoConverge(MimeBodyPart outputPart) {
        try {
            if (currentCryptoResult == null) {
                Log.e(K9.LOG_TAG, "Internal error: we should have a result here!");
                return;
            }

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
                    PendingIntent pendingIntent = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    if (pendingIntent == null) {
                        throw new AssertionError("Expecting PendingIntent on USER_INTERACTION_REQUIRED!");
                    }

                    try {
                        activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                                REQUEST_CODE_CRYPTO, null, 0, 0, 0);
                    } catch (SendIntentException e) {
                        Log.e(K9.LOG_TAG, "Internal error on starting pendingintent!", e);
                    }
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    OpenPgpError error = currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_ERROR);

                    if (K9.DEBUG) {
                        Log.w(K9.LOG_TAG, "OpenPGP API error: " + error.getMessage());
                    }

                    onCryptoFailed(error);
                    break;
                }
                case OpenPgpApi.RESULT_CODE_SUCCESS: {
                    OpenPgpResultAnnotation resultAnnotation = new OpenPgpResultAnnotation();

                    resultAnnotation.setOutputData(outputPart);

                    // TODO if the data /was/ encrypted, we should set it here!
                    // this is not easy to determine for inline data though
                    resultAnnotation.setWasEncrypted(false);

                    OpenPgpSignatureResult signatureResult =
                            currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
                    resultAnnotation.setSignatureResult(signatureResult);

                    PendingIntent pendingIntent =
                            currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    resultAnnotation.setPendingIntent(pendingIntent);

                    onCryptoSuccess(resultAnnotation);
                    break;
                }
            }
        } finally {
            currentCryptoResult = null;
        }
    }

    public void handleCryptoResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_CRYPTO) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            decryptOrVerifyNextPart();
        } else {
            // FIXME: don't pass null
            onCryptoFailed(null);
        }
    }

    private void onCryptoSuccess(OpenPgpResultAnnotation resultAnnotation) {
        addOpenPgpResultPartToMessage(resultAnnotation);
        onCryptoFinished();
    }

    private void addOpenPgpResultPartToMessage(OpenPgpResultAnnotation resultAnnotation) {
        messageAnnotations.put(currentlyDecryptingOrVerifyingPart, resultAnnotation);
    }

    private void onCryptoFailed(OpenPgpError error) {
        OpenPgpResultAnnotation errorPart = new OpenPgpResultAnnotation();
        errorPart.setError(error);
        addOpenPgpResultPartToMessage(errorPart);
        onCryptoFinished();
    }

    private void onCryptoFinished() {
        partsToDecryptOrVerify.removeFirst();
        decryptOrVerifyNextPart();
    }

    private void returnResultToFragment() {
        callback.onCryptoOperationsFinished(messageAnnotations);
    }
}
