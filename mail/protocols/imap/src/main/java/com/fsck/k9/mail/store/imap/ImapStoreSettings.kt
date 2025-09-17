package com.fsck.k9.mail.store.imap

import androidx.annotation.VisibleForTesting
import com.fsck.k9.mail.ServerSettings

/**
 * Extract IMAP-specific server settings from [ServerSettings]
 */
object ImapStoreSettings {
    @VisibleForTesting
    const val AUTODETECT_NAMESPACE_KEY = "autoDetectNamespace"

    @VisibleForTesting
    const val PATH_PREFIX_KEY = "pathPrefix"

    @VisibleForTesting
    const val SEND_CLIENT_INFO = "sendClientInfo"

    @VisibleForTesting
    const val USE_COMPRESSION = "useCompression"

    @JvmStatic
    val ServerSettings.autoDetectNamespace: Boolean
        get() = extra[AUTODETECT_NAMESPACE_KEY]?.toBoolean() ?: true

    @JvmStatic
    val ServerSettings.pathPrefix: String?
        get() = extra[PATH_PREFIX_KEY]

    @JvmStatic
    val ServerSettings.isUseCompression: Boolean
        get() = extra[USE_COMPRESSION]?.toBoolean() ?: true

    @JvmStatic
    val ServerSettings.isSendClientInfo: Boolean
        get() = extra[SEND_CLIENT_INFO]?.toBoolean() ?: true

    // Note: These extras are currently held in the instance referenced by Account.incomingServerSettings
    @JvmStatic
    fun createExtra(autoDetectNamespace: Boolean, pathPrefix: String?): Map<String, String?> {
        return mapOf(
            AUTODETECT_NAMESPACE_KEY to autoDetectNamespace.toString(),
            PATH_PREFIX_KEY to pathPrefix,
        )
    }

    // Note: These extras are required when creating an ImapStore instance.
    fun createExtra(
        autoDetectNamespace: Boolean,
        pathPrefix: String?,
        useCompression: Boolean,
        sendClientInfo: Boolean,
    ): Map<String, String?> {
        return mapOf(
            AUTODETECT_NAMESPACE_KEY to autoDetectNamespace.toString(),
            PATH_PREFIX_KEY to pathPrefix,
            USE_COMPRESSION to useCompression.toString(),
            SEND_CLIENT_INFO to sendClientInfo.toString(),
        )
    }
}
