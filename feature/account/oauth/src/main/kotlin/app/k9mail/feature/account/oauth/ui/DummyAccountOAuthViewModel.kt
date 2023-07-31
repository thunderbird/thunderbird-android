package app.k9mail.feature.account.oauth.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel

// Only used by @DevicePreviews functions
class DummyAccountOAuthViewModel :
    BaseViewModel<AccountOAuthContract.State, AccountOAuthContract.Event, AccountOAuthContract.Effect>(
        AccountOAuthContract.State(),
    ),
    AccountOAuthContract.ViewModel {

    override fun initState(state: AccountOAuthContract.State) = Unit
    override fun event(event: AccountOAuthContract.Event) = Unit
}
