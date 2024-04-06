package com.fsck.k9.preferences

import com.fsck.k9.mail.AuthType

internal class Imported {
    @JvmField var contentVersion: Int = 0

    @JvmField var globalSettings: ImportedSettings? = null

    @JvmField var accounts: Map<String, ImportedAccount>? = null
}

internal class ImportedSettings {
    @JvmField var settings = mutableMapOf<String, String>()
}

internal class ImportedAccount {
    @JvmField var uuid: String? = null

    @JvmField var name: String? = null

    @JvmField var incoming: ImportedServer? = null

    @JvmField var outgoing: ImportedServer? = null

    @JvmField var settings: ImportedSettings? = null

    @JvmField var identities: List<ImportedIdentity>? = null

    @JvmField var folders: List<ImportedFolder>? = null
}

internal class ImportedServer {
    @JvmField var type: String? = null

    @JvmField var host: String? = null

    @JvmField var port: String? = null

    @JvmField var connectionSecurity: String? = null

    @JvmField var authenticationType: AuthType? = null

    @JvmField var username: String? = null

    @JvmField var password: String? = null

    @JvmField var clientCertificateAlias: String? = null

    @JvmField var extras: ImportedSettings? = null
}

internal class ImportedIdentity {
    @JvmField var name: String? = null

    @JvmField var email: String? = null

    @JvmField var description: String? = null

    @JvmField var settings: ImportedSettings? = null
}

internal class ImportedFolder {
    @JvmField var name: String? = null

    @JvmField var settings: ImportedSettings? = null
}
