package app.k9mail.feature.account.server.validation.ui.fake

import app.k9mail.feature.account.oauth.ui.AccountOAuthContract
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

/**
 * Only for previewing the UI.
 */
class FakeAccountOAuthViewModel :
    BaseViewModel<AccountOAuthContract.State, AccountOAuthContract.Event, AccountOAuthContract.Effect>(
        AccountOAuthContract.State(),
    ),
    AccountOAuthContract.ViewModel {

    override fun initState(state: AccountOAuthContract.State) = Unit
    override fun event(event: AccountOAuthContract.Event) = Unit
}
