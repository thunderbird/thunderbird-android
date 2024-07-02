package com.fsck.k9.network

import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import timber.log.Timber
import android.net.ConnectivityManager as SystemConnectivityManager

@Suppress("DEPRECATION")
internal class ConnectivityManagerApi21(
    private val systemConnectivityManager: SystemConnectivityManager,
) : ConnectivityManagerBase() {
    private var isRunning = false
    private var lastNetworkType: Int? = null
    private var wasConnected: Boolean? = null

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.v("Network available: $network")
            notifyIfConnectivityHasChanged()
        }

        override fun onLost(network: Network) {
            Timber.v("Network lost: $network")
            notifyIfConnectivityHasChanged()
        }

        private fun notifyIfConnectivityHasChanged() {
            val networkType = systemConnectivityManager.activeNetworkInfo?.type
            val isConnected = isNetworkAvailable()

            synchronized(this@ConnectivityManagerApi21) {
                if (networkType != lastNetworkType || isConnected != wasConnected) {
                    lastNetworkType = networkType
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

    override fun isNetworkAvailable(): Boolean = systemConnectivityManager.activeNetworkInfo?.isConnected == true
}
