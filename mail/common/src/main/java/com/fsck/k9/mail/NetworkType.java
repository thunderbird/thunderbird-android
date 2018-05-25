package com.fsck.k9.mail;

import android.net.ConnectivityManager;

/**
 * Enum for some of
 * https://developer.android.com/reference/android/net/ConnectivityManager.html#TYPE_MOBILE etc.
 */
public enum NetworkType {

    WIFI,
    MOBILE,
    OTHER;

    public static NetworkType fromConnectivityManagerType(int type){
        switch (type) {
            case ConnectivityManager.TYPE_MOBILE:
                return MOBILE;
            case ConnectivityManager.TYPE_WIFI:
                return WIFI;
            default:
                return OTHER;
        }
    }
}
