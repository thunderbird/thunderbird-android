package com.fsck.k9.network

import android.annotation.SuppressLint
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.net.ConnectivityManager as SystemConnectivityManager

@SuppressLint("MissingPermission")
class ConnectivityManager(private val systemConnectivityManager: SystemConnectivityManager) {
    private var isRunning = false
    private val listeners = mutableSetOf<ConnectivityChangeListener>()
    private var isNetworkAvailable: Boolean? = null

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(this@ConnectivityManager) {
                isNetworkAvailable = true
                notifyListeners()
            }
        }

        override fun onLost(network: Network) {
            synchronized(this@ConnectivityManager) {
                isNetworkAvailable = false
                notifyListeners()
            }
        }
    }

    @Synchronized
    fun start() {
        if (!isRunning) {
            isRunning = true

            val networkRequest = NetworkRequest.Builder().build()
            systemConnectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
    }

    @Synchronized
    fun stop() {
        if (isRunning) {
            isRunning = false

            systemConnectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    @Synchronized
    fun addListener(listener: ConnectivityChangeListener) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: ConnectivityChangeListener) {
        listeners.remove(listener)
    }

    @Synchronized
    fun notifyListeners() {
        for (listener in listeners) {
            listener.onConnectivityChanged()
        }
    }

    fun isNetworkAvailable(): Boolean {
        return synchronized(this) { isNetworkAvailable } ?: isNetworkConnected()
    }

    @Suppress("DEPRECATION")
    private fun isNetworkConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = systemConnectivityManager.activeNetwork
            if (activeNetwork != null) {
                val networkCapabilities = systemConnectivityManager.getNetworkCapabilities(activeNetwork)
                networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            } else {
                false
            }
        } else {
            systemConnectivityManager.activeNetworkInfo?.isConnected == true
        }
    }
}

fun interface ConnectivityChangeListener {
    fun onConnectivityChanged()
}
