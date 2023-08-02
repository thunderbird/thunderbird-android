package app.k9mail.feature.account.setup.ui.preview

import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import com.fsck.k9.mail.ServerSettings

class PreviewAccountSetupStateRepository : DomainContract.AccountSetupStateRepository {
    override fun getState(): AccountSetupState = AccountSetupState()

    override fun save(accountSetupState: AccountSetupState) = Unit

    override fun saveEmailAddress(emailAddress: String) = Unit

    override fun saveIncomingServerSettings(serverSettings: ServerSettings) = Unit

    override fun saveOutgoingServerSettings(serverSettings: ServerSettings) = Unit

    override fun saveAuthorizationState(authorizationState: AuthorizationState) = Unit

    override fun saveOptions(options: AccountOptions) = Unit

    override fun clear() = Unit
}
