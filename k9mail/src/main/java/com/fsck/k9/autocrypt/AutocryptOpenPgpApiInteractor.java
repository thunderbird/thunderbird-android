package com.fsck.k9.autocrypt;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.content.Intent;

import org.openintents.openpgp.util.OpenPgpApi;


public class AutocryptOpenPgpApiInteractor {
    public static AutocryptOpenPgpApiInteractor getInstance() {
        return new AutocryptOpenPgpApiInteractor();
    }

    private AutocryptOpenPgpApiInteractor() { }

    public byte[] getKeyMaterialForKeyId(OpenPgpApi openPgpApi, long keyId, String minimizeForUserId) {
        Intent retrieveKeyIntent = new Intent(OpenPgpApi.ACTION_GET_KEY);
        retrieveKeyIntent.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId);
        return getKeyMaterialFromApi(openPgpApi, retrieveKeyIntent, minimizeForUserId);
    }

    public byte[] getKeyMaterialForUserId(OpenPgpApi openPgpApi, String userId) {
        Intent retrieveKeyIntent = new Intent(OpenPgpApi.ACTION_GET_KEY);
        retrieveKeyIntent.putExtra(OpenPgpApi.EXTRA_USER_ID, userId);
        return getKeyMaterialFromApi(openPgpApi, retrieveKeyIntent, userId);
    }

    private byte[] getKeyMaterialFromApi(OpenPgpApi openPgpApi, Intent retrieveKeyIntent, String userId) {
        retrieveKeyIntent.putExtra(OpenPgpApi.EXTRA_MINIMIZE, true);
        retrieveKeyIntent.putExtra(OpenPgpApi.EXTRA_MINIMIZE_USER_ID, userId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Intent result = openPgpApi.executeApi(retrieveKeyIntent, (InputStream) null, baos);

        if (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR) ==
                OpenPgpApi.RESULT_CODE_SUCCESS) {
            return baos.toByteArray();
        } else{
            return null;
        }
    }
}
