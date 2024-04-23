package com.fsck.k9.preferences

import com.fsck.k9.mail.AuthType

internal data class Imported(
    val contentVersion: Int,
    val globalSettings: ImportedSettings?,
    val accounts: Map<String, ImportedAccount>?,
)

internal data class ImportedSettings(
    val settings: Map<String, String> = emptyMap(),
)

internal data class ImportedAccount(
    val uuid: String,
    val name: String?,
    val incoming: ImportedServer?,
    val outgoing: ImportedServer?,
    val settings: ImportedSettings?,
    val identities: List<ImportedIdentity>?,
    val folders: List<ImportedFolder>?,
)

internal data class ImportedServer(
    val type: String?,
    val host: String?,
    val port: String?,
    val connectionSecurity: String?,
    val authenticationType: AuthType?,
    val username: String?,
    val password: String?,
    val clientCertificateAlias: String?,
    val extras: ImportedSettings?,
)

internal data class ImportedIdentity(
    val name: String?,
    val email: String?,
    val description: String?,
    val settings: ImportedSettings?,
)

internal data class ImportedFolder(
    val name: String?,
    val settings: ImportedSettings?,
)
