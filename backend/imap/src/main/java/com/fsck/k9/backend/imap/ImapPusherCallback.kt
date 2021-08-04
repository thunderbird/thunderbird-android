package com.fsck.k9.backend.imap

interface ImapPusherCallback {
    fun onPushEvent(folderServerId: String)
    fun onPushError(folderServerId: String, exception: Exception)
    fun onPushNotSupported()
}
