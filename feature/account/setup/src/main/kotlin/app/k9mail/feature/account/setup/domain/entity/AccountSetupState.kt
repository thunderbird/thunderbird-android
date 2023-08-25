package app.k9mail.feature.account.setup.domain.entity

import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings

data class AccountSetupState(
    val emailAddress: String? = null,
    val incomingServerSettings: ServerSettings? = null,
    val outgoingServerSettings: ServerSettings? = null,
    val authorizationState: AuthorizationState? = null,
    val options: AccountOptions? = null,
)
