package com.fsck.k9.network

import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import timber.log.Timber
import android.net.ConnectivityManager as SystemConnectivityManager

@RequiresApi(Build.VERSION_CODES.M)
internal class ConnectivityManagerApi23(
    private val systemConnectivityManager: SystemConnectivityManager,
) : ConnectivityManagerBase() {
    private var isRunning = false
    private var lastActiveNetwork: Network? = null
    private var wasConnected: Boolean? = null

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.v("Network available: $network")
            notifyIfActiveNetworkOrConnectivityHasChanged()
        }

        override fun onLost(network: Network) {
            Timber.v("Network lost: $network")
            notifyIfActiveNetworkOrConnectivityHasChanged()
        }

        private fun notifyIfActiveNetworkOrConnectivityHasChanged() {
            val activeNetwork = systemConnectivityManager.activeNetwork
            val isConnected = isNetworkAvailable()

            synchronized(this@ConnectivityManagerApi23) {
                if (activeNetwork != lastActiveNetwork || isConnected != wasConnected) {
                    lastActiveNetwork = activeNetwork
                    wasConnected = isConnected
                    if (isConnected) {
                        notifyOnConnectivityChanged()
                    } else {
                        notifyOnConnectivityLost()
                    }
                }
            }
        }
    }

    @Synchronized
    override fun start() {
        if (!isRunning) {
            isRunning = true

            val networkRequest = NetworkRequest.Builder().build()
            systemConnectivityManager.registerNetworkCallback(networkRequest, networkCallback)
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
        val activeNetwork = systemConnectivityManager.activeNetwork ?: return false
        val networkCapabilities = systemConnectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
