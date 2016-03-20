/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp.example;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class OpenPgpApiActivity extends Activity {
    private EditText mMessage;
    private EditText mCiphertext;
    private EditText mDetachedSignature;
    private EditText mEncryptUserIds;
    private EditText mGetKeyEdit;

    private OpenPgpServiceConnection mServiceConnection;

    private long mSignKeyId;

    public static final int REQUEST_CODE_CLEARTEXT_SIGN = 9910;
    public static final int REQUEST_CODE_ENCRYPT = 9911;
    public static final int REQUEST_CODE_SIGN_AND_ENCRYPT = 9912;
    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY = 9913;
    public static final int REQUEST_CODE_GET_KEY = 9914;
    public static final int REQUEST_CODE_GET_KEY_IDS = 9915;
    public static final int REQUEST_CODE_DETACHED_SIGN = 9916;
    public static final int REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED = 9917;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openpgp_provider);

        mMessage = (EditText) findViewById(R.id.crypto_provider_demo_message);
        mCiphertext = (EditText) findViewById(R.id.crypto_provider_demo_ciphertext);
        mDetachedSignature = (EditText) findViewById(R.id.crypto_provider_demo_detached_signature);
        mEncryptUserIds = (EditText) findViewById(R.id.crypto_provider_demo_encrypt_user_id);
        Button cleartextSign = (Button) findViewById(R.id.crypto_provider_demo_cleartext_sign);
        Button detachedSign = (Button) findViewById(R.id.crypto_provider_demo_detached_sign);
        Button encrypt = (Button) findViewById(R.id.crypto_provider_demo_encrypt);
        Button signAndEncrypt = (Button) findViewById(R.id.crypto_provider_demo_sign_and_encrypt);
        Button decryptAndVerify = (Button) findViewById(R.id.crypto_provider_demo_decrypt_and_verify);
        Button verifyDetachedSignature = (Button) findViewById(R.id.crypto_provider_demo_verify_detached_signature);
        mGetKeyEdit = (EditText) findViewById(R.id.crypto_provider_demo_get_key_edit);
        EditText getKeyIdsEdit = (EditText) findViewById(R.id.crypto_provider_demo_get_key_ids_edit);
        Button getKey = (Button) findViewById(R.id.crypto_provider_demo_get_key);
        Button getKeyIds = (Button) findViewById(R.id.crypto_provider_demo_get_key_ids);

        cleartextSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleartextSign(new Intent());
            }
        });
        detachedSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detachedSign(new Intent());
            }
        });
        encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encrypt(new Intent());
            }
        });
        signAndEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signAndEncrypt(new Intent());
            }
        });
        decryptAndVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAndVerify(new Intent());
            }
        });
        verifyDetachedSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAndVerifyDetached(new Intent());
            }
        });
        getKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getKey(new Intent());
            }
        });
        getKeyIds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getKeyIds(new Intent());
            }
        });

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String providerPackageName = settings.getString("openpgp_provider_list", "");
        mSignKeyId = settings.getLong("openpgp_key", 0);
        if (TextUtils.isEmpty(providerPackageName)) {
            Toast.makeText(this, "No OpenPGP app selected!", Toast.LENGTH_LONG).show();
            finish();
        } else if (mSignKeyId == 0) {
            Toast.makeText(this, "No key selected!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            // bind to service
            mServiceConnection = new OpenPgpServiceConnection(
                    OpenPgpApiActivity.this.getApplicationContext(),
                    providerPackageName,
                    new OpenPgpServiceConnection.OnBound() {
                        @Override
                        public void onBound(IOpenPgpService2 service) {
                            Log.d(OpenPgpApi.TAG, "onBound!");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(OpenPgpApi.TAG, "exception when binding!", e);
                        }
                    }
            );
            mServiceConnection.bindToService();
        }
    }

    private void handleError(final OpenPgpError error) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(OpenPgpApiActivity.this,
                        "onError id:" + error.getErrorId() + "\n\n" + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(Constants.TAG, "onError getErrorId:" + error.getErrorId());
                Log.e(Constants.TAG, "onError getMessage:" + error.getMessage());
            }
        });
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(OpenPgpApiActivity.this,
                        message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Takes input from message or ciphertext EditText and turns it into a ByteArrayInputStream
     */
    private InputStream getInputstream(boolean ciphertext) {
        InputStream is = null;
        try {
            String inputStr;
            if (ciphertext) {
                inputStr = mCiphertext.getText().toString();
            } else {
                inputStr = mMessage.getText().toString();
            }
            is = new ByteArrayInputStream(inputStr.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
        }

        return is;
    }

    private class MyCallback implements OpenPgpApi.IOpenPgpCallback {
        boolean returnToCiphertextField;
        ByteArrayOutputStream os;
        int requestCode;

        private MyCallback(boolean returnToCiphertextField, ByteArrayOutputStream os, int requestCode) {
            this.returnToCiphertextField = returnToCiphertextField;
            this.os = os;
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                case OpenPgpApi.RESULT_CODE_SUCCESS: {
                    showToast("RESULT_CODE_SUCCESS");

                    // encrypt/decrypt/sign/verify
                    if (os != null) {
                        try {
                            Log.d(OpenPgpApi.TAG, "result: " + os.toByteArray().length
                                    + " str=" + os.toString("UTF-8"));

                            if (returnToCiphertextField) {
                                mCiphertext.setText(os.toString("UTF-8"));
                            } else {
                                mMessage.setText(os.toString("UTF-8"));
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e(Constants.TAG, "UnsupportedEncodingException", e);
                        }
                    }

                    switch (requestCode) {
                        case REQUEST_CODE_DECRYPT_AND_VERIFY:
                        case REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED: {
                            // RESULT_SIGNATURE and RESULT_DECRYPTION are never null!

                            OpenPgpSignatureResult signatureResult
                                    = result.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE);
                            showToast(signatureResult.toString());
                            OpenPgpDecryptionResult decryptionResult
                                    = result.getParcelableExtra(OpenPgpApi.RESULT_DECRYPTION);
                            showToast(decryptionResult.toString());

                            break;
                        }
                        case REQUEST_CODE_DETACHED_SIGN: {
                            byte[] detachedSig
                                    = result.getByteArrayExtra(OpenPgpApi.RESULT_DETACHED_SIGNATURE);
                            Log.d(OpenPgpApi.TAG, "RESULT_DETACHED_SIGNATURE: " + detachedSig.length
                                    + " str=" + new String(detachedSig));
                            mDetachedSignature.setText(new String(detachedSig));

                            break;
                        }
                        case REQUEST_CODE_GET_KEY_IDS: {
                            long[] keyIds = result.getLongArrayExtra(OpenPgpApi.RESULT_KEY_IDS);
                            String out = "keyIds: ";
                            for (long keyId : keyIds) {
                                out += OpenPgpUtils.convertKeyIdToHex(keyId) + ", ";
                            }
                            showToast(out);

                            break;
                        }
                        default: {

                        }
                    }

                    break;
                }
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    showToast("RESULT_CODE_USER_INTERACTION_REQUIRED");

                    PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                    try {
                        OpenPgpApiActivity.this.startIntentSenderFromChild(
                                OpenPgpApiActivity.this, pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(Constants.TAG, "SendIntentException", e);
                    }
                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    showToast("RESULT_CODE_ERROR");

                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    handleError(error);
                    break;
                }
            }
        }
    }

    public void cleartextSign(Intent data) {
        data.setAction(OpenPgpApi.ACTION_CLEARTEXT_SIGN);
        data.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, mSignKeyId);

        InputStream is = getInputstream(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(true, os, REQUEST_CODE_CLEARTEXT_SIGN));
    }

    public void detachedSign(Intent data) {
        data.setAction(OpenPgpApi.ACTION_DETACHED_SIGN);
        data.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, mSignKeyId);

        InputStream is = getInputstream(false);
        // no output stream needed, detached signature is returned as RESULT_DETACHED_SIGNATURE

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, null, new MyCallback(true, null, REQUEST_CODE_DETACHED_SIGN));
    }

    public void encrypt(Intent data) {
        data.setAction(OpenPgpApi.ACTION_ENCRYPT);
        if (!TextUtils.isEmpty(mEncryptUserIds.getText().toString())) {
            data.putExtra(OpenPgpApi.EXTRA_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        }
        data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(true, os, REQUEST_CODE_ENCRYPT));
    }

    public void signAndEncrypt(Intent data) {
        data.setAction(OpenPgpApi.ACTION_SIGN_AND_ENCRYPT);
        data.putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, mSignKeyId);
        if (!TextUtils.isEmpty(mEncryptUserIds.getText().toString())) {
            data.putExtra(OpenPgpApi.EXTRA_USER_IDS, mEncryptUserIds.getText().toString().split(","));
        }
        data.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);

        InputStream is = getInputstream(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(true, os, REQUEST_CODE_SIGN_AND_ENCRYPT));
    }

    public void decryptAndVerify(Intent data) {
        data.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

        InputStream is = getInputstream(true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, os, new MyCallback(false, os, REQUEST_CODE_DECRYPT_AND_VERIFY));
    }

    public void decryptAndVerifyDetached(Intent data) {
        data.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);
        data.putExtra(OpenPgpApi.EXTRA_DETACHED_SIGNATURE, mDetachedSignature.getText().toString().getBytes());

        // use from text from mMessage
        InputStream is = getInputstream(false);

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, is, null, new MyCallback(false, null, REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED));
    }

    public void getKey(Intent data) {
        data.setAction(OpenPgpApi.ACTION_GET_KEY);
        data.putExtra(OpenPgpApi.EXTRA_KEY_ID, Long.decode(mGetKeyEdit.getText().toString()));

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_GET_KEY));
    }

    public void getKeyIds(Intent data) {
        data.setAction(OpenPgpApi.ACTION_GET_KEY_IDS);

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_GET_KEY_IDS));
    }

    public void getAnyKeyIds(Intent data) {
        data.setAction(OpenPgpApi.ACTION_GET_KEY_IDS);
//        data.putExtra(OpenPgpApi.EXTRA_USER_IDS, mGetKeyIdsEdit.getText().toString().split(","));

        OpenPgpApi api = new OpenPgpApi(this, mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(false, null, REQUEST_CODE_GET_KEY_IDS));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Constants.TAG, "onActivityResult resultCode: " + resultCode);

        // try again after user interaction
        if (resultCode == RESULT_OK) {
            /*
             * The data originally given to one of the methods above, is again
             * returned here to be used when calling the method again after user
             * interaction. The Intent now also contains results from the user
             * interaction, for example selected key ids.
             */
            switch (requestCode) {
                case REQUEST_CODE_CLEARTEXT_SIGN: {
                    cleartextSign(data);
                    break;
                }
                case REQUEST_CODE_DETACHED_SIGN: {
                    detachedSign(data);
                    break;
                }
                case REQUEST_CODE_ENCRYPT: {
                    encrypt(data);
                    break;
                }
                case REQUEST_CODE_SIGN_AND_ENCRYPT: {
                    signAndEncrypt(data);
                    break;
                }
                case REQUEST_CODE_DECRYPT_AND_VERIFY: {
                    decryptAndVerify(data);
                    break;
                }
                case REQUEST_CODE_DECRYPT_AND_VERIFY_DETACHED: {
                    decryptAndVerifyDetached(data);
                    break;
                }
                case REQUEST_CODE_GET_KEY: {
                    getKey(data);
                    break;
                }
                case REQUEST_CODE_GET_KEY_IDS: {
                    getKeyIds(data);
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null) {
            mServiceConnection.unbindFromService();
        }
    }

}
