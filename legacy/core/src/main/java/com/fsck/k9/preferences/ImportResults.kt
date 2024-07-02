package com.fsck.k9.preferences

data class ImportResults(
    val globalSettings: Boolean,
    val importedAccounts: List<AccountDescriptionPair>,
    val erroneousAccounts: List<AccountDescription>,
)

data class AccountDescriptionPair(
    val original: AccountDescription,
    val imported: AccountDescription,
    val authorizationNeeded: Boolean,
    val incomingPasswordNeeded: Boolean,
    val outgoingPasswordNeeded: Boolean,
    val incomingServerName: String,
    val outgoingServerName: String,
)

data class AccountDescription(
    val name: String,
    val uuid: String,
)
