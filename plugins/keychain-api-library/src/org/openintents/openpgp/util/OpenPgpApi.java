/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp.util;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;

import java.io.InputStream;
import java.io.OutputStream;

public class OpenPgpApi {

    //TODO: fix this documentation
    /**
     * General extras
     * --------------
     *
     * Intent extras:
     * int          api_version (required)
     * boolean      ascii_armor (request ascii armor for ouput)
     *
     * returned Bundle:
     * int          result_code (0, 1, or 2 (see OpenPgpApi))
     * OpenPgpError error       (if result_code == 0)
     * Intent       intent      (if result_code == 2)
     */

    /**
     * Sign only
     *
     * optional params:
     * String       passphrase  (for key passphrase)
     */

    /**
     * Encrypt
     *
     * Intent extras:
     * long[]       key_ids
     * or
     * String[]     user_ids    (= emails of recipients) (if more than one key has this user_id, a PendingIntent is returned)
     *
     * optional extras:
     * String       passphrase  (for key passphrase)
     */

    /**
     * Sign and encrypt
     *
     * Intent extras:
     * same as in encrypt()
     */

    /**
     * Decrypts and verifies given input bytes. This methods handles encrypted-only, signed-and-encrypted,
     * and also signed-only input.
     *
     * returned Bundle:
     * OpenPgpSignatureResult   signature_result
     */

    /**
     * Retrieves key ids based on given user ids (=emails)
     *
     * Intent extras:
     * String[]     user_ids
     *
     * returned Bundle:
     * long[]       key_ids
     */

    public static final String TAG = "OpenPgp API";

    public static final int API_VERSION = 2;
    public static final String SERVICE_INTENT = "org.openintents.openpgp.IOpenPgpService";

    public static final String ACTION_SIGN = "org.openintents.openpgp.action.SIGN";
    public static final String ACTION_ENCRYPT = "org.openintents.openpgp.action.ENCRYPT";
    public static final String ACTION_SIGN_AND_ENCTYPT = "org.openintents.openpgp.action.SIGN_AND_ENCRYPT";
    public static final String ACTION_DECRYPT_VERIFY = "org.openintents.openpgp.action.DECRYPT_VERIFY";
    public static final String ACTION_DOWNLOAD_KEYS = "org.openintents.openpgp.action.DOWNLOAD_KEYS";
    public static final String ACTION_GET_KEY_IDS = "org.openintents.openpgp.action.GET_KEY_IDS";

    /* Bundle params */
    public static final String EXTRA_API_VERSION = "api_version";

    // SIGN, ENCRYPT, SIGN_ENCRYPT, DECRYPT_VERIFY
    // request ASCII Armor for output
    // OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
    public static final String EXTRA_REQUEST_ASCII_ARMOR = "ascii_armor";

    // ENCRYPT, SIGN_ENCRYPT
    public static final String EXTRA_USER_IDS = "user_ids";
    public static final String EXTRA_KEY_IDS = "key_ids";
    // optional parameter:
    public static final String EXTRA_PASSPHRASE = "passphrase";

    /* Service Bundle returns */
    public static final String RESULT_CODE = "result_code";
    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_ERRORS = "error";
    public static final String RESULT_INTENT = "intent";

    // get actual error object from RESULT_ERRORS
    public static final int RESULT_CODE_ERROR = 0;
    // success!
    public static final int RESULT_CODE_SUCCESS = 1;
    // executeServiceMethod intent and do it again with intent
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;


    IOpenPgpService mService;
    Context mContext;

    public OpenPgpApi(Context context, IOpenPgpService service) {
        this.mContext = context;
        this.mService = service;
    }

    public interface IOpenPgpCallback {
        void onReturn(final Intent result);
    }

    private class OpenPgpAsyncTask extends AsyncTask<Void, Integer, Intent> {
        Intent data;
        InputStream is;
        OutputStream os;
        IOpenPgpCallback callback;

        private OpenPgpAsyncTask(Intent data, InputStream is, OutputStream os, IOpenPgpCallback callback) {
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

    public void executeApiAsync(Intent data, InputStream is, OutputStream os, IOpenPgpCallback callback) {
        OpenPgpAsyncTask task = new OpenPgpAsyncTask(data, is, os, callback);

        // don't serialize async tasks!
        // http://commonsware.com/blog/2012/04/20/asynctask-threading-regression-confirmed.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        } else {
            task.execute((Void[]) null);
        }
    }

    public Intent executeApi(Intent data, InputStream is, OutputStream os) {
        try {
            data.putExtra(EXTRA_API_VERSION, OpenPgpApi.API_VERSION);

            Intent result = null;

            if (ACTION_GET_KEY_IDS.equals(data.getAction())) {
                result = mService.execute(data, null, null);
                return result;
            } else {
                // send the input and output pfds
                ParcelFileDescriptor input = ParcelFileDescriptorUtil.pipeFrom(is,
                        new ParcelFileDescriptorUtil.IThreadListener() {

                            @Override
                            public void onThreadFinished(Thread thread) {
                                Log.d(OpenPgpApi.TAG, "Copy to service finished");
                            }
                        });
                ParcelFileDescriptor output = ParcelFileDescriptorUtil.pipeTo(os,
                        new ParcelFileDescriptorUtil.IThreadListener() {

                            @Override
                            public void onThreadFinished(Thread thread) {
                                Log.d(OpenPgpApi.TAG, "Service finished writing!");
                            }
                        });

                // blocks until result is ready
                result = mService.execute(data, input, output);
                // close() is required to halt the TransferThread
                output.close();

                // set class loader to current context to allow unparcelling
                // of OpenPgpError and OpenPgpSignatureResult
                // http://stackoverflow.com/a/3806769
                result.setExtrasClassLoader(mContext.getClassLoader());

                return result;
            }
        } catch (Exception e) {
            Log.e(OpenPgpApi.TAG, "Exception", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        }
    }

}
