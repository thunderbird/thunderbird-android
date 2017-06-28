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

    public byte[] getKeyMaterialFromApi(OpenPgpApi openPgpApi, long keyId, String minimizeForUserId) {
        Intent retreiveKeyIntent = new Intent(OpenPgpApi.ACTION_GET_KEY);
        retreiveKeyIntent.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId);
        retreiveKeyIntent.putExtra(OpenPgpApi.EXTRA_MINIMIZE, true);
        retreiveKeyIntent.putExtra(OpenPgpApi.EXTRA_MINIMIZE_USER_ID, minimizeForUserId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Intent result = openPgpApi.executeApi(retreiveKeyIntent, (InputStream) null, baos);

        if (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR) ==
                OpenPgpApi.RESULT_CODE_SUCCESS) {
            return baos.toByteArray();
        } else{
            return null;
        }
    }
}
