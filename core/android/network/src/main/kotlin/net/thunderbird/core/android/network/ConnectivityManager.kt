package net.thunderbird.core.android.network

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
