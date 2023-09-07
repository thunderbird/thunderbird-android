package app.k9mail.feature.account.common.domain

import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings

interface AccountDomainContract {

    interface AccountStateRepository {
        fun getState(): AccountState

        fun save(accountState: AccountState)

        fun saveEmailAddress(emailAddress: String)

        fun saveIncomingServerSettings(serverSettings: ServerSettings)

        fun saveOutgoingServerSettings(serverSettings: ServerSettings)

        fun saveAuthorizationState(authorizationState: AuthorizationState)

        fun saveOptions(options: AccountOptions)

        fun clear()
    }
}
