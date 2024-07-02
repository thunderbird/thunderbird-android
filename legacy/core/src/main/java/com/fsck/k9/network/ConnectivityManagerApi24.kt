package com.fsck.k9.network

import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber
import android.net.ConnectivityManager as SystemConnectivityManager

@RequiresApi(Build.VERSION_CODES.N)
internal class ConnectivityManagerApi24(
    private val systemConnectivityManager: SystemConnectivityManager,
) : ConnectivityManagerBase() {
    private var isRunning = false
    private var isNetworkAvailable: Boolean? = null

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.v("Network available: $network")
            synchronized(this@ConnectivityManagerApi24) {
                isNetworkAvailable = true
                notifyOnConnectivityChanged()
            }
        }

        override fun onLost(network: Network) {
            Timber.v("Network lost: $network")
            synchronized(this@ConnectivityManagerApi24) {
                isNetworkAvailable = false
                notifyOnConnectivityLost()
            }
        }
    }

    @Synchronized
    override fun start() {
        if (!isRunning) {
            isRunning = true

            systemConnectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    @Synchronized
    override fun stop() {
        if (isRunning) {
            isRunning = false

            systemConnectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    override fun isNetworkAvailable(): Boolean {
        return synchronized(this) { isNetworkAvailable } ?: isNetworkAvailableSynchronous()
    }

    // Sometimes this will return 'true' even though networkCallback has already received onLost().
    // That's why isNetworkAvailable() prefers the state derived from the callbacks over this method.
    private fun isNetworkAvailableSynchronous(): Boolean {
        val activeNetwork = systemConnectivityManager.activeNetwork ?: return false
        val networkCapabilities = systemConnectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
