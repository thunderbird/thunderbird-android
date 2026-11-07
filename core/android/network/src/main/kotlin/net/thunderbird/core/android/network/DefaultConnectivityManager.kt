package net.thunderbird.core.android.network

import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import net.thunderbird.components.core.logging.Logger
import android.net.ConnectivityManager as SystemConnectivityManager

private const val TAG = "DefaultConnectivityManager"

internal class DefaultConnectivityManager(
    private val systemConnectivityManager: SystemConnectivityManager,
    private val logger: Logger,
) : ConnectivityManagerBase() {
    private var isRunning = false
    private var isNetworkAvailable: Boolean? = null

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            logger.verbose(TAG) { "Network available: $network" }
            synchronized(this@DefaultConnectivityManager) {
                isNetworkAvailable = true
                notifyOnConnectivityChanged()
            }
        }

        override fun onLost(network: Network) {
            logger.verbose(TAG) { "Network lost: $network" }
            synchronized(this@DefaultConnectivityManager) {
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
