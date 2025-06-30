package net.thunderbird.core.android.network

import android.os.Build
import android.net.ConnectivityManager as SystemConnectivityManager

interface ConnectivityManager {
    fun start()
    fun stop()
    fun isNetworkAvailable(): Boolean
    fun addListener(listener: ConnectivityChangeListener)
    fun removeListener(listener: ConnectivityChangeListener)
}

interface ConnectivityChangeListener {
    fun onConnectivityChanged()
    fun onConnectivityLost()
}

internal fun ConnectivityManager(systemConnectivityManager: SystemConnectivityManager): ConnectivityManager {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> ConnectivityManagerApi24(systemConnectivityManager)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> ConnectivityManagerApi23(systemConnectivityManager)
        else -> ConnectivityManagerApi21(systemConnectivityManager)
    }
}
