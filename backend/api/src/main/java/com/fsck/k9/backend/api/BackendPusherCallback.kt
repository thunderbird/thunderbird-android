package com.fsck.k9.backend.api

interface BackendPusherCallback {
    fun onPushEvent(folderServerId: String)
    fun onPushError(exception: Exception)
    fun onPushNotSupported()
}
