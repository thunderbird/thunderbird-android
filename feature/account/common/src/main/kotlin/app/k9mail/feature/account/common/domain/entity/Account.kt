package app.k9mail.feature.account.common.domain.entity

import com.fsck.k9.mail.ServerSettings
import net.thunderbird.feature.account.AccountId

data class Account(
    val id: AccountId,
    val emailAddress: String,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
    val authorizationState: String?,
    val specialFolderSettings: SpecialFolderSettings?,
    val options: AccountOptions,
)
