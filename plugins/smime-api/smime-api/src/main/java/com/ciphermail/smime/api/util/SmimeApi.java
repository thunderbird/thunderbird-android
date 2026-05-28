package com.ciphermail.smime.api.util;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import com.ciphermail.smime.api.ISmimeService;
import com.ciphermail.smime.api.SmimeError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side helper for the S/MIME companion service API.
 *
 * Usage pattern (mirrors OpenPgpApi):
 *
 *   SmimeApi api = new SmimeApi(context, boundService);
 *
 *   Intent request = new Intent(SmimeApi.ACTION_DECRYPT_VERIFY);
 *   request.putExtra(SmimeApi.EXTRA_API_VERSION, SmimeApi.API_VERSION);
 *
 *   api.executeApiAsync(request, inputStream, outputStream, result -> {
 *       int code = result.getIntExtra(SmimeApi.RESULT_CODE, SmimeApi.RESULT_CODE_ERROR);
 *       // handle code
 *   });
 */
public class SmimeApi {

    // -------------------------------------------------------------------------
    // Service binding intent action — used to bind to CipherMail's service
    // -------------------------------------------------------------------------
    public static final String SERVICE_INTENT = "com.ciphermail.smime.api.ISmimeService";

    public static final int API_VERSION = 1;

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Check whether the caller has permission to use the API.
     * Returns RESULT_CODE_SUCCESS or RESULT_CODE_USER_INTERACTION_REQUIRED (first-run consent).
     * No input stream needed.
     */
    public static final String ACTION_CHECK_PERMISSION =
            "com.ciphermail.smime.api.action.CHECK_PERMISSION";

    /**
     * Decrypt and/or verify an S/MIME message part.
     *
     * Input stream:  raw bytes of the MIME part (application/pkcs7-mime or multipart/signed).
     * Output stream: decrypted/verified MIME content.
     *
     * Returned extras: RESULT_DECRYPTION, RESULT_SIGNATURE (both Parcelable).
     */
    public static final String ACTION_DECRYPT_VERIFY =
            "com.ciphermail.smime.api.action.DECRYPT_VERIFY";

    /**
     * Sign and/or encrypt an outgoing MIME message.
     *
     * Required extras: EXTRA_USER_IDS (recipient email addresses).
     * Optional extras: EXTRA_SIGN (default true), EXTRA_ENCRYPT (default true),
     *                  EXTRA_FROM (sender address; selects the signing identity).
     *
     * Input stream:  raw MIME bytes of the message to protect.
     * Output stream: S/MIME wrapped MIME bytes ready for transport.
     */
    public static final String ACTION_SIGN_AND_ENCRYPT =
            "com.ciphermail.smime.api.action.SIGN_AND_ENCRYPT";

    /**
     * Query certificate availability for a list of email addresses.
     * Used by the compose screen to determine per-recipient lock icon state.
     *
     * Required extras: EXTRA_USER_IDS.
     * No input stream needed; no output stream produced.
     *
     * Returned extras: RESULT_CERTIFICATES (SmimeCertificateInfo[]).
     */
    public static final String ACTION_GET_CERTIFICATES =
            "com.ciphermail.smime.api.action.GET_CERTIFICATES";

    /**
     * Import a certificate from a byte stream (DER or PEM encoded).
     *
     * Input stream:  certificate bytes.
     * No output stream produced.
     */
    public static final String ACTION_IMPORT_CERTIFICATE =
            "com.ciphermail.smime.api.action.IMPORT_CERTIFICATE";

    // -------------------------------------------------------------------------
    // Request extras
    // -------------------------------------------------------------------------

    /** int — always required. Must equal API_VERSION. */
    public static final String EXTRA_API_VERSION = "api_version";

    /** String[] — recipient email addresses (for SIGN_AND_ENCRYPT and GET_CERTIFICATES). */
    public static final String EXTRA_USER_IDS = "user_ids";

    /** boolean — whether to sign the outgoing message (default true). */
    public static final String EXTRA_SIGN = "sign";

    /** boolean — whether to encrypt the outgoing message (default true). */
    public static final String EXTRA_ENCRYPT = "encrypt";

    /**
     * String — sender (From) email address (for SIGN_AND_ENCRYPT). Selects which
     * personal certificate is used to sign the message and to encrypt it to self.
     * Optional; when absent the service falls back to its configured default identity.
     */
    public static final String EXTRA_FROM = "from";

    // -------------------------------------------------------------------------
    // Result codes
    // -------------------------------------------------------------------------

    /** int extra key carrying the result code. */
    public static final String RESULT_CODE = "result_code";

    public static final int RESULT_CODE_ERROR = 0;
    public static final int RESULT_CODE_SUCCESS = 1;
    /** Service needs user interaction (e.g. keystore unlock). Launch RESULT_INTENT. */
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    // -------------------------------------------------------------------------
    // Result extras
    // -------------------------------------------------------------------------

    /** PendingIntent — present when RESULT_CODE == RESULT_CODE_USER_INTERACTION_REQUIRED. */
    public static final String RESULT_INTENT = "intent";

    /** SmimeError Parcelable — present when RESULT_CODE == RESULT_CODE_ERROR. */
    public static final String RESULT_ERROR = "error";

    /** SmimeDecryptionResult Parcelable — present after ACTION_DECRYPT_VERIFY. */
    public static final String RESULT_DECRYPTION = "decryption_result";

    /** SmimeSignatureResult Parcelable — present after ACTION_DECRYPT_VERIFY. */
    public static final String RESULT_SIGNATURE = "signature_result";

    /** SmimeCertificateInfo[] Parcelable array — present after ACTION_GET_CERTIFICATES. */
    public static final String RESULT_CERTIFICATES = "certificates";

    // -------------------------------------------------------------------------
    // Async execution
    // -------------------------------------------------------------------------

    public interface SmimeCallback {
        void onReturn(Intent result);
    }

    private final ISmimeService mService;
    private static final AtomicInteger sPipeIdCounter = new AtomicInteger(0);

    public SmimeApi(ISmimeService service) {
        this.mService = service;
    }

    /**
     * Execute an API call asynchronously, streaming data through pipes.
     *
     * @param data         Request Intent with action and extras.
     * @param inputStream  Source bytes (the MIME message to process), or null.
     * @param outputStream Destination for processed bytes, or null.
     * @param callback     Called on the calling thread with the result Intent.
     */
    public void executeApiAsync(final Intent data,
                                final InputStream inputStream,
                                final OutputStream outputStream,
                                final SmimeCallback callback) {
        new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... params) {
                return executeApi(data, inputStream, outputStream);
            }

            @Override
            protected void onPostExecute(Intent result) {
                callback.onReturn(result);
            }
        }.execute();
    }

    /**
     * Execute an API call synchronously. Must not be called on the main thread.
     */
    public Intent executeApi(Intent data, InputStream inputStream, OutputStream outputStream) {
        ParcelFileDescriptor inputFd = null;
        ParcelFileDescriptor outputFd = null;
        Thread inputThread = null;
        Thread outputThread = null;

        try {
            final int pipeId = sPipeIdCounter.incrementAndGet();

            // Wire up the output pipe (service writes → we read)
            if (outputStream != null) {
                outputFd = mService.createOutputPipe(pipeId);
                final ParcelFileDescriptor readEnd = outputFd;
                outputThread = new Thread(() -> {
                    try (ParcelFileDescriptor.AutoCloseInputStream in =
                                 new ParcelFileDescriptor.AutoCloseInputStream(readEnd)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            outputStream.write(buf, 0, n);
                        }
                    } catch (IOException ignored) {}
                });
                outputThread.start();
            }

            // Wire up the input pipe (we write → service reads)
            if (inputStream != null) {
                ParcelFileDescriptor[] inputPipe = ParcelFileDescriptor.createPipe();
                final ParcelFileDescriptor writeEnd = inputPipe[1];
                inputFd = inputPipe[0];
                inputThread = new Thread(() -> {
                    try (ParcelFileDescriptor.AutoCloseOutputStream out =
                                 new ParcelFileDescriptor.AutoCloseOutputStream(writeEnd)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = inputStream.read(buf)) != -1) {
                            out.write(buf, 0, n);
                        }
                    } catch (IOException ignored) {}
                });
                inputThread.start();
            }

            Intent result = mService.execute(data, inputFd, pipeId);

            if (outputThread != null) outputThread.join();
            if (inputThread != null) inputThread.join();

            return result;

        } catch (Exception e) {
            Intent error = new Intent();
            error.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            error.putExtra(RESULT_ERROR, new SmimeError(SmimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return error;
        } finally {
            closeQuietly(inputFd);
            closeQuietly(outputFd);
        }
    }

    private static void closeQuietly(ParcelFileDescriptor fd) {
        if (fd != null) {
            try { fd.close(); } catch (IOException ignored) {}
        }
    }
}
