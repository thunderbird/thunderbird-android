package com.fsck.k9.mail.store


import com.fsck.k9.mail.NetworkType

interface StoreConfig {
    val subscribedFoldersOnly: Boolean

    val inboxFolder: String?
    val outboxFolder: String?
    val draftsFolder: String?

    val maximumAutoDownloadMessageSize: Int

    val allowRemoteSearch: Boolean
    val remoteSearchFullText: Boolean

    val pushPollOnConnect: Boolean

    val displayCount: Int

    val idleRefreshMinutes: Int
    fun useCompression(networkType: NetworkType): Boolean

    fun shouldHideHostname(): Boolean
}
