package net.thunderbird.core.android.network

import android.os.Build
import android.net.ConnectivityManager as AndroidConnectivityManager

/**
 * Provides a compatibility layer for [ConnectivityManager] based on the device's API level.
 *
 * @param connectivityManager The system's [android.net.ConnectivityManager] instance.
 * @return An instance of [ConnectivityManager] appropriate for the device's API level.
 */
internal fun connectivityManagerCompat(
    connectivityManager: AndroidConnectivityManager,
): ConnectivityManager {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> ConnectivityManagerApi24(connectivityManager)
        else -> ConnectivityManagerApi23(connectivityManager)
    }
}
