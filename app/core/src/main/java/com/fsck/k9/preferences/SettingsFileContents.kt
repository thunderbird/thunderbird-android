package com.fsck.k9.preferences

import com.fsck.k9.mail.AuthType

internal data class Imported(
    @JvmField val contentVersion: Int,
    @JvmField val globalSettings: ImportedSettings?,
    @JvmField val accounts: Map<String, ImportedAccount>?,
)

internal data class ImportedSettings(
    @JvmField val settings: Map<String, String> = emptyMap(),
)

internal data class ImportedAccount(
    @JvmField val uuid: String,
    @JvmField val name: String?,
    @JvmField val incoming: ImportedServer?,
    @JvmField val outgoing: ImportedServer?,
    @JvmField val settings: ImportedSettings?,
    @JvmField val identities: List<ImportedIdentity>?,
    @JvmField val folders: List<ImportedFolder>?,
)

internal data class ImportedServer(
    @JvmField val type: String?,
    @JvmField val host: String?,
    @JvmField val port: String?,
    @JvmField val connectionSecurity: String?,
    @JvmField val authenticationType: AuthType?,
    @JvmField val username: String?,
    @JvmField val password: String?,
    @JvmField val clientCertificateAlias: String?,
    @JvmField val extras: ImportedSettings?,
)

internal data class ImportedIdentity(
    @JvmField val name: String?,
    @JvmField val email: String?,
    @JvmField val description: String?,
    @JvmField val settings: ImportedSettings?,
)

internal data class ImportedFolder(
    @JvmField val name: String?,
    @JvmField val settings: ImportedSettings?,
)
