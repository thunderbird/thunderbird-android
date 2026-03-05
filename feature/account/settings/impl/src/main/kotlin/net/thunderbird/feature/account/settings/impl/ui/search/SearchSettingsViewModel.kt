package net.thunderbird.feature.account.settings.impl.ui.search

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_REMOTE_SEARCH_NUM_RESULTS
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.handle
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.R
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.AccountSettingError
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

private const val TAG = "SearchSettingsViewModel"

internal class SearchSettingsViewModel(
    private val accountId: AccountId,
    private val getAccountName: UseCase.GetAccountName,
    private val updateSearchSettings: UseCase.UpdateSearchSettings,
    private val getLegacyAccount: UseCase.GetLegacyAccount,
    private val logger: Logger,
    private val resources: StringsResourceManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    initialState: SearchSettingsContract.State = SearchSettingsContract.State(
        serverSearchLimit = SelectOption(DEFAULT_REMOTE_SEARCH_NUM_RESULTS.toString()) {
            resources.stringResource(R.string.account_settings_remote_search_num_results_entries_25)
        },
    ),
) : BaseViewModel<SearchSettingsContract.State, SearchSettingsContract.Event, SearchSettingsContract.Effect>(
    initialState,
),
    SearchSettingsContract.ViewModel {
    override fun event(event: SearchSettingsContract.Event) {
        when (event) {
            is SearchSettingsContract.Event.OnBackPressed -> emitEffect(SearchSettingsContract.Effect.NavigateBack)
            is SearchSettingsContract.Event.OnServerSearchLimitChange -> {
                viewModelScope.launch(dispatcher) {
                    updateSearchSettings(
                        accountId = accountId,
                        command = AccountSettingsDomainContract.UpdateSearchSettingsCommand.UpdateServerSearchLimit(
                            event.serverSearchLimit,
                        ),
                    ).handle(
                        onSuccess = {
                            val selectOption = selectOptionForServerSearchLimit(event.serverSearchLimit)
                            updateState { state -> state.copy(serverSearchLimit = selectOption) }
                        },
                        onFailure = { handleError(it) },
                    )
                }
            }
        }
    }

    init {
        observeAccountName()
        observeSearchSettings()
    }

    private fun observeAccountName() {
        getAccountName(accountId)
            .onEach { outcome ->
                outcome.handle(
                    onSuccess = { updateState { state -> state.copy(subtitle = it) } },
                    onFailure = { handleError(it) },
                )
            }.launchIn(viewModelScope)
    }

    private fun observeSearchSettings() {
        viewModelScope.launch(dispatcher) {
            getLegacyAccount(accountId).handle(
                onSuccess = {
                    val selectOption = selectOptionForServerSearchLimit(it.remoteSearchNumResults)
                    updateState { state -> state.copy(serverSearchLimit = selectOption) }
                },
                onFailure = {
                    handleError(it)
                },
            )
        }
    }

    private fun selectOptionForServerSearchLimit(limit: Int): SelectOption {
        val labelRes = when (limit) {
            ServerSearchLimit.ALL.count -> R.string.account_settings_remote_search_num_results_entries_all
            ServerSearchLimit.TEN.count -> R.string.account_settings_remote_search_num_results_entries_10
            ServerSearchLimit.TWENTY_FIVE.count -> R.string.account_settings_remote_search_num_results_entries_25
            ServerSearchLimit.FIFTY.count -> R.string.account_settings_remote_search_num_results_entries_50
            ServerSearchLimit.HUNDRED.count -> R.string.account_settings_remote_search_num_results_entries_100
            ServerSearchLimit.TWO_HUNDRED_FIFTY.count -> R.string.account_settings_remote_search_num_results_entries_250
            ServerSearchLimit.FIVE_HUNDRED.count -> R.string.account_settings_remote_search_num_results_entries_500
            ServerSearchLimit.THOUSAND.count -> R.string.account_settings_remote_search_num_results_entries_1000
            else -> error("Invalid serverSearchLimit value: $limit")
        }

        return SelectOption(limit.toString()) {
            resources.stringResource(labelRes)
        }
    }

    private fun handleError(error: AccountSettingError) {
        when (error) {
            is AccountSettingError.NotFound -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.StorageError -> logger.error(tag = TAG, message = { error.message })
            is AccountSettingError.UnsupportedFormat -> logger.error(tag = TAG, message = { error.message })
        }
    }
}
