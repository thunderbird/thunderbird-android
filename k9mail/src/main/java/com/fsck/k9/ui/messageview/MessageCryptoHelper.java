package com.fsck.k9.ui.messageview;


import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.crypto.MessageDecryptVerifier;
import com.fsck.k9.crypto.OpenPgpApiHelper;
import com.fsck.k9.helper.IdentityHelper;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.DecryptStreamParser;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.OpenPgpResultBodyPart;
import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;


public class MessageCryptoHelper {

    private MessageViewFragment fragment;
    private Account account;
    private LocalMessage message;

    private Deque<Part> partsToDecryptOrVerify;
    private OpenPgpApi openPgpApi;
    private Part currentlyDecrypringOrVerifyingPart;
    private Intent currentCryptoResult;

    private static final int INVALID_OPENPGP_RESULT_CODE = -1;

    MessageCryptoHelper(MessageViewFragment fragment, Account account) {
        this.fragment = fragment;
        this.account = account;
    }

    void decryptOrVerifyMessagePartsIfNecessary(LocalMessage message) {
        this.message = message;

        List<Part> encryptedParts = MessageDecryptVerifier.findEncryptedParts(message);
        List<Part> signedParts = MessageDecryptVerifier.findSignedParts(message);
        if (!encryptedParts.isEmpty() || !signedParts.isEmpty()) {
            partsToDecryptOrVerify = new ArrayDeque<Part>();
            partsToDecryptOrVerify.addAll(encryptedParts);
            partsToDecryptOrVerify.addAll(signedParts);
            decryptOrVerifyNextPartOrStartExtractingTextAndAttachments();
        } else {
            returnResultToFragment();
        }
    }

    private void decryptOrVerifyNextPartOrStartExtractingTextAndAttachments() {
        if (!partsToDecryptOrVerify.isEmpty()) {

            Part part = partsToDecryptOrVerify.peekFirst();
            if (MessageDecryptVerifier.isPgpMimePart(part)) {
                startDecryptingOrVerifyingPart(part);
            } else {
                partsToDecryptOrVerify.removeFirst();
                decryptOrVerifyNextPartOrStartExtractingTextAndAttachments();
            }

            return;
        }

        returnResultToFragment();

    }

    private void startDecryptingOrVerifyingPart(Part part) {
        Multipart multipart = (Multipart) part.getBody();
        if (multipart == null) {
            throw new RuntimeException("Downloading missing parts before decryption isn't supported yet");
        }

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
        new OpenPgpServiceConnection(fragment.getContext(), openPgpProvider,
                new OnBound() {
                    @Override
                    public void onBound(IOpenPgpService service) {
                        openPgpApi = new OpenPgpApi(fragment.getContext(), service);

                        decryptOrVerifyNextPartOrStartExtractingTextAndAttachments();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(K9.LOG_TAG, "Couldn't connect to OpenPgpService", e);
                    }
                }).bindToService();
    }

    private void decryptOrVerifyPart(Part part) {
        currentlyDecrypringOrVerifyingPart = part;
        decryptVerify(new Intent());
    }

    private void decryptVerify(Intent intent) {
        intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, message);
        String accountName = OpenPgpApiHelper.buildAccountName(identity);
        intent.putExtra(OpenPgpApi.EXTRA_ACCOUNT_NAME, accountName);

        try {
            if (MessageDecryptVerifier.isPgpMimeSignedPart(currentlyDecrypringOrVerifyingPart)) {
                callAsyncDetachedVerify(intent);
            } else {
                callAsyncDecrypt(intent);
            }
        } catch (IOException e) {
            Log.e(K9.LOG_TAG, "IOException", e);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "MessagingException", e);
        }
    }

    private void callAsyncDecrypt(Intent intent) throws IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        PipedInputStream pipedInputStream = getPipedInputStreamForEncryptedData();
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

        byte[] signatureData = MessageDecryptVerifier.getSignatureData(currentlyDecrypringOrVerifyingPart);
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
                    Multipart multipartSignedMultipart = (Multipart) currentlyDecrypringOrVerifyingPart.getBody();
                    BodyPart signatureBodyPart = multipartSignedMultipart.getBodyPart(0);
                    Log.d(K9.LOG_TAG, "signed data type: " + signatureBodyPart.getMimeType());
                    signatureBodyPart.writeTo(out);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Exception while writing message to crypto provider", e);
                }
            }
        }).start();

        return pipedInputStream;
    }

    private PipedInputStream getPipedInputStreamForEncryptedData() throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();

        final PipedOutputStream out = new PipedOutputStream(pipedInputStream);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Multipart multipartEncryptedMultipart = (Multipart) currentlyDecrypringOrVerifyingPart.getBody();
                    BodyPart encryptionPayloadPart = multipartEncryptedMultipart.getBodyPart(1);
                    Body encryptionPayloadBody = encryptionPayloadPart.getBody();
                    encryptionPayloadBody.writeTo(out);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Exception while writing message to crypto provider", e);
                }
            }
        }).start();

        return pipedInputStream;
    }

    private PipedOutputStream getPipedOutputStreamForDecryptedData(final CountDownLatch latch) throws IOException {
        PipedOutputStream decryptedOutputStream = new PipedOutputStream();
        final PipedInputStream decryptedInputStream = new PipedInputStream(decryptedOutputStream);
        new AsyncTask<Void, Void, OpenPgpResultBodyPart>() {
            @Override
            protected OpenPgpResultBodyPart doInBackground(Void... params) {
                OpenPgpResultBodyPart decryptedPart = null;
                try {
                    decryptedPart = DecryptStreamParser.parse(decryptedInputStream);

                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(K9.LOG_TAG, "we were interrupted while waiting for onReturn!", e);
                } catch (Exception e) {
                    Log.e(K9.LOG_TAG, "Something went wrong while parsing the decrypted MIME part", e);
                    //TODO: pass error to main thread and display error message to user
                }
                return decryptedPart;
            }

            @Override
            protected void onPostExecute(OpenPgpResultBodyPart decryptedPart) {
                onCryptoConverge(decryptedPart);
            }
        }.execute();
        return decryptedOutputStream;
    }

    private void onCryptoConverge(OpenPgpResultBodyPart openPgpResultBodyPart) {
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
                        fragment.getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                                MessageList.REQUEST_CODE_CRYPTO, null, 0, 0, 0);
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
                    if (openPgpResultBodyPart == null) {
                        openPgpResultBodyPart = new OpenPgpResultBodyPart(false);
                    }
                    OpenPgpSignatureResult signatureResult =
                            currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
                    openPgpResultBodyPart.setSignatureResult(signatureResult);

                    PendingIntent pendingIntent =
                            currentCryptoResult.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    openPgpResultBodyPart.setPendingIntent(pendingIntent);

                    onCryptoSuccess(openPgpResultBodyPart);
                    break;
                }
            }
        } catch (MessagingException e) {
            // catching the empty OpenPgpResultBodyPart constructor above - this can't actually happen
            Log.e(K9.LOG_TAG, "This shouldn't happen", e);
        } finally {
            currentCryptoResult = null;
        }
    }

    public void handleCryptoResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            decryptOrVerifyNextPartOrStartExtractingTextAndAttachments();
        } else {
            // FIXME: don't pass null
            onCryptoFailed(null);
        }
    }

    private void onCryptoSuccess(OpenPgpResultBodyPart decryptedPart) {
        addOpenPgpResultPartToMessage(decryptedPart);
        onCryptoFinished();
    }

    private void addOpenPgpResultPartToMessage(OpenPgpResultBodyPart decryptedPart) {
        Multipart multipart = (Multipart) currentlyDecrypringOrVerifyingPart.getBody();
        multipart.addBodyPart(decryptedPart);
    }

    private void onCryptoFailed(OpenPgpError error) {
        try {
            OpenPgpResultBodyPart errorPart = new OpenPgpResultBodyPart(false);
            errorPart.setError(error);
            addOpenPgpResultPartToMessage(errorPart);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "This shouldn't happen", e);
        }
        onCryptoFinished();
    }

    private void onCryptoFinished() {
        partsToDecryptOrVerify.removeFirst();
        decryptOrVerifyNextPartOrStartExtractingTextAndAttachments();
    }

    private void returnResultToFragment() {
        fragment.startExtractingTextAndAttachments(message);
    }

}
