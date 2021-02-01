package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.ServerSettings

/**
 * Extract IMAP-specific server settings from [ServerSettings]
 */
object ImapStoreSettings {
    private const val AUTODETECT_NAMESPACE_KEY = "autoDetectNamespace"
    private const val PATH_PREFIX_KEY = "pathPrefix"

    @JvmStatic
    val ServerSettings.autoDetectNamespace: Boolean
        get() = extra[AUTODETECT_NAMESPACE_KEY]?.toBoolean() ?: true

    @JvmStatic
    val ServerSettings.pathPrefix: String?
        get() = extra[PATH_PREFIX_KEY]

    @JvmStatic
    fun createExtra(autoDetectNamespace: Boolean, pathPrefix: String?): Map<String, String?> {
        return mapOf(
            AUTODETECT_NAMESPACE_KEY to autoDetectNamespace.toString(),
            PATH_PREFIX_KEY to pathPrefix
        )
    }
}
