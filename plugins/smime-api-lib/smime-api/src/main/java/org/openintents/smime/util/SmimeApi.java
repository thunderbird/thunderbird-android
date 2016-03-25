/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.smime.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.openintents.smime.SmimeService;
import org.openintents.smime.SmimeError;

public class SmimeApi {

    public static final String TAG = "SMIME API";

    public static final String SERVICE_INTENT_2 = "org.openintents.smime.SmimeService";

    /**
     * see CHANGELOG.md
     */
    public static final int API_VERSION = 1;

    /**
     * General extras
     * --------------
     *
     * required extras:
     * int           EXTRA_API_VERSION           (always required)
     *
     * returned extras:
     * int           RESULT_CODE                 (RESULT_CODE_ERROR, RESULT_CODE_SUCCESS or RESULT_CODE_USER_INTERACTION_REQUIRED)
     * SmimeError  RESULT_ERROR                (if RESULT_CODE == RESULT_CODE_ERROR)
     * PendingIntent RESULT_INTENT               (if RESULT_CODE == RESULT_CODE_USER_INTERACTION_REQUIRED)
     */

    public static final String ACTION_CHECK_PERMISSION = "org.openintents.smime.action.CHECK_PERMISSION";

    /**
     * DEPRECATED
     * Same as ACTION_CLEARTEXT_SIGN
     * <p/>
     * optional extras:
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (DEPRECATED: this makes no sense here)
     * char[]        EXTRA_PASSPHRASE            (key passphrase)
     */
    public static final String ACTION_SIGN = "org.openintents.smime.action.SIGN";

    /**
     * Sign text resulting in a cleartext signature
     * Some magic pre-processing of the text is done to convert it to a format usable for
     * cleartext signatures per RFC 4880 before the text is actually signed:
     * - end cleartext with newline
     * - remove whitespaces on line endings
     * <p/>
     * required extras:
     * long          EXTRA_SIGN_KEY_ID           (key id of signing key)
     * <p/>
     * optional extras:
     * char[]        EXTRA_PASSPHRASE            (key passphrase)
     */
    public static final String ACTION_CLEARTEXT_SIGN = "org.openintents.smime.action.CLEARTEXT_SIGN";

    /**
     * Sign text or binary data resulting in a detached signature.
     * No OutputStream necessary for ACTION_DETACHED_SIGN (No magic pre-processing like in ACTION_CLEARTEXT_SIGN)!
     * The detached signature is returned separately in RESULT_DETACHED_SIGNATURE.
     * <p/>
     * required extras:
     * long          EXTRA_SIGN_KEY_ID           (key id of signing key)
     * <p/>
     * optional extras:
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (request ascii armor for detached signature)
     * char[]        EXTRA_PASSPHRASE            (key passphrase)
     * <p/>
     * returned extras:
     * byte[]        RESULT_DETACHED_SIGNATURE
     */
    public static final String ACTION_DETACHED_SIGN = "org.openintents.smime.action.DETACHED_SIGN";

    /**
     * Encrypt
     * <p/>
     * required extras:
     * String[]      EXTRA_USER_IDS              (=emails of recipients, if more than one key has a user_id, a PendingIntent is returned via RESULT_INTENT)
     * or
     * long[]        EXTRA_KEY_IDS
     * <p/>
     * optional extras:
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (request ascii armor for output)
     * char[]        EXTRA_PASSPHRASE            (key passphrase)
     * String        EXTRA_ORIGINAL_FILENAME     (original filename to be encrypted as metadata)
     * boolean       EXTRA_ENABLE_COMPRESSION    (enable ZLIB compression, default ist true)
     */
    public static final String ACTION_ENCRYPT = "org.openintents.smime.action.ENCRYPT";

    /**
     * Sign and encrypt
     * <p/>
     * required extras:
     * String[]      EXTRA_USER_IDS              (=emails of recipients, if more than one key has a user_id, a PendingIntent is returned via RESULT_INTENT)
     * or
     * long[]        EXTRA_KEY_IDS
     * <p/>
     * optional extras:
     * long          EXTRA_SIGN_KEY_ID           (key id of signing key)
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (request ascii armor for output)
     * char[]        EXTRA_PASSPHRASE            (key passphrase)
     * String        EXTRA_ORIGINAL_FILENAME     (original filename to be encrypted as metadata)
     * boolean       EXTRA_ENABLE_COMPRESSION    (enable ZLIB compression, default ist true)
     */
    public static final String ACTION_SIGN_AND_ENCRYPT = "org.openintents.smime.action.SIGN_AND_ENCRYPT";

    /**
     * Decrypts and verifies given input stream. This methods handles encrypted-only, signed-and-encrypted,
     * and also signed-only input.
     * OutputStream is optional, e.g., for verifying detached signatures!
     * <p/>
     * If SmimeSignatureResult.getResult() == SmimeSignatureResult.RESULT_KEY_MISSING
     * in addition a PendingIntent is returned via RESULT_INTENT to download missing keys.
     * On all other status, in addition a PendingIntent is returned via RESULT_INTENT to open
     * the key view in OpenKeychain.
     * <p/>
     * optional extras:
     * byte[]        EXTRA_DETACHED_SIGNATURE    (detached signature)
     * <p/>
     * returned extras:
     * SmimeSignatureResult   RESULT_SIGNATURE
     * SmimeDecryptionResult  RESULT_DECRYPTION
     * SmimeDecryptMetadata   RESULT_METADATA
     * String                   RESULT_CHARSET   (charset which was specified in the headers of ascii armored input, if any)
     */
    public static final String ACTION_DECRYPT_VERIFY = "org.openintents.smime.action.DECRYPT_VERIFY";

    /**
     * Decrypts the header of an encrypted file to retrieve metadata such as original filename.
     * <p/>
     * This does not decrypt the actual content of the file.
     * <p/>
     * returned extras:
     * SmimeDecryptMetadata   RESULT_METADATA
     * String                   RESULT_CHARSET   (charset which was specified in the headers of ascii armored input, if any)
     */
    public static final String ACTION_DECRYPT_METADATA = "org.openintents.smime.action.DECRYPT_METADATA";

    /**
     * Select key id for signing
     * <p/>
     * optional extras:
     * String      EXTRA_USER_ID
     * <p/>
     * returned extras:
     * long        EXTRA_SIGN_KEY_ID
     */
    public static final String ACTION_GET_SIGN_KEY_ID = "org.openintents.smime.action.GET_SIGN_KEY_ID";

    /**
     * Get key ids based on given user ids (=emails)
     * <p/>
     * required extras:
     * String[]      EXTRA_USER_IDS
     * <p/>
     * returned extras:
     * long[]        RESULT_KEY_IDS
     */
    public static final String ACTION_GET_KEY_IDS = "org.openintents.smime.action.GET_KEY_IDS";

    /**
     * This action returns RESULT_CODE_SUCCESS if the Smime Provider already has the key
     * corresponding to the given key id in its database.
     * <p/>
     * It returns RESULT_CODE_USER_INTERACTION_REQUIRED if the Provider does not have the key.
     * The PendingIntent from RESULT_INTENT can be used to retrieve those from a keyserver.
     * <p/>
     * If an Output stream has been defined the whole public key is returned.
     * required extras:
     * long        EXTRA_KEY_ID
     * <p/>
     * optional extras:
     * String      EXTRA_REQUEST_ASCII_ARMOR (request that the returned key is encoded in ASCII Armor)
     *
     */
    public static final String ACTION_GET_KEY = "org.openintents.smime.action.GET_KEY";

    /* Intent extras */
    public static final String EXTRA_API_VERSION = "api_version";

    // DEPRECATED!!!
    public static final String EXTRA_ACCOUNT_NAME = "account_name";

    // ACTION_DETACHED_SIGN, ENCRYPT, SIGN_AND_ENCRYPT, DECRYPT_VERIFY
    // request ASCII Armor for output
    // Smime Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
    public static final String EXTRA_REQUEST_ASCII_ARMOR = "ascii_armor";

    // ACTION_DETACHED_SIGN
    public static final String RESULT_DETACHED_SIGNATURE = "detached_signature";
    public static final String RESULT_SIGNATURE_MICALG = "signature_micalg";

    // ENCRYPT, SIGN_AND_ENCRYPT
    public static final String EXTRA_USER_IDS = "user_ids";
    public static final String EXTRA_KEY_IDS = "key_ids";
    public static final String EXTRA_SIGN_KEY_ID = "sign_key_id";
    // optional extras:
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_ORIGINAL_FILENAME = "original_filename";
    public static final String EXTRA_ENABLE_COMPRESSION = "enable_compression";
    public static final String EXTRA_ENCRYPT_OPPORTUNISTIC = "opportunistic";

    // GET_SIGN_KEY_ID
    public static final String EXTRA_USER_ID = "user_id";

    // GET_KEY
    public static final String EXTRA_KEY_ID = "key_id";
    public static final String RESULT_KEY_IDS = "key_ids";

    /* Service Intent returns */
    public static final String RESULT_CODE = "result_code";

    // get actual error object from RESULT_ERROR
    public static final int RESULT_CODE_ERROR = 0;
    // success!
    public static final int RESULT_CODE_SUCCESS = 1;
    // get PendingIntent from RESULT_INTENT, start PendingIntent with startIntentSenderForResult,
    // and execute service method again in onActivityResult
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    public static final String RESULT_ERROR = "error";
    public static final String RESULT_INTENT = "intent";

    // DECRYPT_VERIFY
    public static final String EXTRA_DETACHED_SIGNATURE = "detached_signature";
    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_DECRYPTION = "decryption";
    public static final String RESULT_METADATA = "metadata";
    // This will be the charset which was specified in the headers of ascii armored input, if any
    public static final String RESULT_CHARSET = "charset";

    // INTERNAL, should not be used
    public static final String EXTRA_CALL_UUID1 = "call_uuid1";
    public static final String EXTRA_CALL_UUID2 = "call_uuid2";

    SmimeService mService;
    Context mContext;
    final AtomicInteger mPipeIdGen = new AtomicInteger();

    public SmimeApi(Context context, SmimeService service) {
        this.mContext = context;
        this.mService = service;
    }

    public interface ISmimeCallback {
        void onReturn(final Intent result);
    }

    private class SmimeAsyncTask extends AsyncTask<Void, Integer, Intent> {
        Intent data;
        InputStream is;
        OutputStream os;
        ISmimeCallback callback;

        private SmimeAsyncTask(Intent data, InputStream is, OutputStream os, ISmimeCallback callback) {
            this.data = data;
            this.is = is;
            this.os = os;
            this.callback = callback;
        }

        @Override
        protected Intent doInBackground(Void... unused) {
            return executeApi(data, is, os);
        }

        protected void onPostExecute(Intent result) {
            callback.onReturn(result);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void executeApiAsync(Intent data, InputStream is, OutputStream os, ISmimeCallback callback) {
        SmimeAsyncTask task = new SmimeAsyncTask(data, is, os, callback);

        // don't serialize async tasks!
        // http://commonsware.com/blog/2012/04/20/asynctask-threading-regression-confirmed.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        } else {
            task.execute((Void[]) null);
        }
    }

    /**
     * InputStream and OutputStreams are always closed after operating on them!
     */
    public Intent executeApi(Intent data, InputStream is, OutputStream os) {
        ParcelFileDescriptor input = null;
        try {
            if (is != null) {
                input = ParcelFileDescriptorUtil.pipeFrom(is);
            }

            return executeApi(data, input, os);
        } catch (Exception e) {
            Log.e(SmimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERROR,
                    new SmimeError(SmimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        }
    }

    public interface SmimeDataSource {
        void writeTo(OutputStream os) throws IOException;
    }

    /**
     * InputStream and OutputStreams are always closed after operating on them!
     */
    public Intent executeApi(Intent data, SmimeDataSource dataSource, OutputStream os) {
        ParcelFileDescriptor input = null;
        try {
            if (dataSource != null) {
                input = ParcelFileDescriptorUtil.asyncPipeFromDataSource(dataSource);
            }

            return executeApi(data, input, os);
        } catch (Exception e) {
            Log.e(SmimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERROR,
                    new SmimeError(SmimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        }
    }

    /**
     * InputStream and OutputStreams are always closed after operating on them!
     */
    private Intent executeApi(Intent data, ParcelFileDescriptor input, OutputStream os) {
        ParcelFileDescriptor output = null;
        try {
            // always send version from client
            data.putExtra(EXTRA_API_VERSION, SmimeApi.API_VERSION);

            Intent result;

            Thread pumpThread =null;
            int outputPipeId = 0;

            if (os != null) {
                outputPipeId = mPipeIdGen.incrementAndGet();
                output = mService.createOutputPipe(outputPipeId);
                pumpThread = ParcelFileDescriptorUtil.pipeTo(os, output);
            }

            // blocks until result is ready
            result = mService.execute(data, input, outputPipeId);

            // set class loader to current context to allow unparcelling
            // of SmimeError and SmimeSignatureResult
            // http://stackoverflow.com/a/3806769
            result.setExtrasClassLoader(mContext.getClassLoader());

            //wait for ALL data being pumped from remote side
            if (pumpThread != null) {
                pumpThread.join();
            }

            return result;
        } catch (Exception e) {
            Log.e(SmimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERROR,
                    new SmimeError(SmimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        } finally {
            // close() is required to halt the TransferThread
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(SmimeApi.TAG, "IOException when closing ParcelFileDescriptor!", e);
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(SmimeApi.TAG, "IOException when closing ParcelFileDescriptor!", e);
                }
            }
        }
    }


    public interface PermissionPingCallback {
        void onSmimePermissionCheckResult(Intent result);
    }

    public void checkPermissionPing(final PermissionPingCallback permissionPingCallback) {
        Intent intent = new Intent(SmimeApi.ACTION_CHECK_PERMISSION);
        executeApiAsync(intent, null, null, new ISmimeCallback() {
            @Override
            public void onReturn(Intent result) {
                permissionPingCallback.onSmimePermissionCheckResult(result);
            }
        });
    }
}
