package app.k9mail.feature.account.common.domain

import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import com.fsck.k9.mail.ServerSettings

interface AccountDomainContract {

    interface AccountStateRepository {
        fun getState(): AccountState

        fun setState(accountState: AccountState)

        fun setEmailAddress(emailAddress: String)

        fun setIncomingServerSettings(serverSettings: ServerSettings)

        fun setOutgoingServerSettings(serverSettings: ServerSettings)

        fun setAuthorizationState(authorizationState: AuthorizationState)

        fun setSpecialFolderSettings(specialFolderSettings: SpecialFolderSettings)

        fun setOptions(options: AccountOptions)

        fun clear()
    }
}
