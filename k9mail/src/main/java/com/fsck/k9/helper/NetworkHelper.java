package com.fsck.k9.helper;


import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.fsck.k9.Globals;
import com.fsck.k9.mail.NetworkType;


/**
 * Created by daquexian on 17-4-7.
 */

public class NetworkHelper {
    public static boolean isActiveNetworkMeteredCompat() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) Globals.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            return connectivityManager.isActiveNetworkMetered();
        } else {
            NetworkType networkType =
                    NetworkType.fromConnectivityManagerType(connectivityManager.getActiveNetworkInfo().getType());

            return networkType != NetworkType.WIFI;
        }
    }
}
