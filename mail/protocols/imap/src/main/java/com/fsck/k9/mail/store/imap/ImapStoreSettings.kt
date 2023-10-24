package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.ServerSettings

/**
 * Extract IMAP-specific server settings from [ServerSettings]
 */
object ImapStoreSettings {
    private const val AUTODETECT_NAMESPACE_KEY = "autoDetectNamespace"
    private const val PATH_PREFIX_KEY = "pathPrefix"
    private const val SEND_CLIENT_ID = "sendClientId"
    private const val USE_COMPRESSION = "useCompression"

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
    val ServerSettings.isSendClientId: Boolean
        get() = extra[SEND_CLIENT_ID]?.toBoolean() ?: true

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
        sendClientId: Boolean,
    ): Map<String, String?> {
        return mapOf(
            AUTODETECT_NAMESPACE_KEY to autoDetectNamespace.toString(),
            PATH_PREFIX_KEY to pathPrefix,
            USE_COMPRESSION to useCompression.toString(),
            SEND_CLIENT_ID to sendClientId.toString(),
        )
    }
}
