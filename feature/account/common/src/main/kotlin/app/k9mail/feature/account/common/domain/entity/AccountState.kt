package app.k9mail.feature.account.common.domain.entity

import com.fsck.k9.mail.ServerSettings

data class AccountState(
    val uuid: String? = null,
    val emailAddress: String? = null,
    val incomingServerSettings: ServerSettings? = null,
    val outgoingServerSettings: ServerSettings? = null,
    val authorizationState: AuthorizationState? = null,
    val specialFolderSettings: SpecialFolderSettings? = null,
    val displayOptions: AccountDisplayOptions? = null,
    val syncOptions: AccountSyncOptions? = null,
)
