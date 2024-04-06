package com.fsck.k9.preferences

data class ImportResults(
    @JvmField val globalSettings: Boolean,
    @JvmField val importedAccounts: List<AccountDescriptionPair>,
    @JvmField val erroneousAccounts: List<AccountDescription>,
)

data class AccountDescriptionPair(
    @JvmField val original: AccountDescription,
    @JvmField val imported: AccountDescription,
    @JvmField val overwritten: Boolean,
    @JvmField val authorizationNeeded: Boolean,
    @JvmField val incomingPasswordNeeded: Boolean,
    @JvmField val outgoingPasswordNeeded: Boolean,
    @JvmField val incomingServerName: String,
    @JvmField val outgoingServerName: String,
)

data class AccountDescription(
    @JvmField val name: String,
    @JvmField val uuid: String,
)
