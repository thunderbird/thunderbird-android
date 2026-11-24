package app.k9mail.feature.account.common.domain.entity

import com.fsck.k9.mail.ServerSettings
import net.thunderbird.feature.account.AccountId

data class AccountState(
    val id: AccountId? = null,
    val emailAddress: String? = null,
    val incomingServerSettings: ServerSettings? = null,
    val outgoingServerSettings: ServerSettings? = null,
    val authorizationState: AuthorizationState? = null,
    val specialFolderSettings: SpecialFolderSettings? = null,
    val displayOptions: AccountDisplayOptions? = null,
    val syncOptions: AccountSyncOptions? = null,
)
