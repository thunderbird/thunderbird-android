package net.thunderbird.feature.account.settings.impl.ui.api

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.core.outcome.handle
import net.thunderbird.core.common.mail.NativeApiMailServerSettings
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.oauth.OAuthConfigurationProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.core.validation.input.StringInputField
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UpdateGeneralSettingCommand
import net.thunderbird.feature.account.settings.impl.domain.AccountSettingsDomainContract.UseCase

private const val TAG = "ApiSettingsViewModel"

internal class ApiSettingsViewModel(
    private val accountId: AccountId,
    private val getAccountProfile: UseCase.GetAccountProfile,
    private val getLegacyAccount: UseCase.GetLegacyAccount,
    private val updateGeneralSettings: UseCase.UpdateGeneralSettings,
    private val updateApiClientId: UseCase.UpdateApiClientId,
    private val oauthConfigurationProvider: OAuthConfigurationProvider,
    private val validateAccountName: UseCase.ValidateAccountName,
    private val logger: Logger,
    initialState: ApiSettingsContract.State = ApiSettingsContract.State(),
) : BaseViewModel<ApiSettingsContract.State, ApiSettingsContract.Event, ApiSettingsContract.Effect>(initialState),
    ApiSettingsContract.ViewModel {

    private val clientIdUpdateMutex = Mutex()

    init {
        observeAccountProfile()
        loadLegacyAccount()
    }

    override fun event(event: ApiSettingsContract.Event) {
        when (event) {
            is ApiSettingsContract.Event.OnNameChange -> updateName(event.name)
            is ApiSettingsContract.Event.OnClientIdChange -> updateClientId(event.clientId)
            ApiSettingsContract.Event.OnReauthorizeClick -> reauthorize()
            ApiSettingsContract.Event.OnBackPressed -> navigateBack()
        }
    }

    private fun observeAccountProfile() {
        getAccountProfile(accountId)
            .onEach { outcome ->
                outcome.handle(
                    onSuccess = { profile ->
                        updateState { current ->
                            current.copy(
                                name = StringInputField(value = profile.name, isValid = true),
                            )
                        }
                    },
                    onFailure = { error -> logError(error) },
                )
            }
            .launchIn(viewModelScope)
    }

    private fun loadLegacyAccount() {
        viewModelScope.launch {
            getLegacyAccount(accountId).handle(
                onSuccess = { account ->
                    val incomingSettings = account.incomingServerSettings
                    val incomingType = incomingSettings.type
                    val requiresClientId =
                        incomingType == Protocols.GMAIL_API || Protocols.isGraphProtocol(incomingType)
                    val storedClientId = incomingSettings.extra[NativeApiMailServerSettings.EXTRA_OAUTH_CLIENT_ID]
                        .orEmpty()
                        .takeIf(String::isNotBlank)
                    val defaultClientId = oauthConfigurationProvider.getConfiguration(incomingSettings.host)?.clientId
                        .orEmpty()
                    val clientId = storedClientId ?: defaultClientId

                    updateState { current ->
                        current.copy(
                            emailId = account.email,
                            clientId = clientId,
                            requiresClientId = requiresClientId,
                            isApiAccount = Protocols.isNativeApiProtocol(incomingType),
                            isGraphAccount = Protocols.isGraphProtocol(incomingType),
                            isLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    updateState { it.copy(isLoading = false) }
                    logError(error)
                },
            )
        }
    }

    private fun updateName(name: String) {
        val validation = validateAccountName(name)
        updateState { current ->
            current.copy(
                name = current.name
                    .updateValue(name)
                    .updateFromValidationOutcome(validation),
            )
        }
        if (validation is Outcome.Success) {
            viewModelScope.launch {
                updateGeneralSettings(
                    accountId = accountId,
                    command = UpdateGeneralSettingCommand.UpdateName(name),
                ).handle(
                    onSuccess = {},
                    onFailure = { error -> logError(error) },
                )
            }
        }
    }

    private fun updateClientId(clientId: String) {
        val normalizedClientId = clientId.trim()
        updateState { it.copy(clientId = normalizedClientId) }
        if (state.value.requiresClientId && normalizedClientId.isNotBlank()) {
            persistClientId(normalizedClientId)
        }
    }

    private fun persistClientId(clientId: String) {
        viewModelScope.launch {
            saveClientId(clientId)
        }
    }

    private suspend fun saveClientId(clientId: String): Boolean = clientIdUpdateMutex.withLock {
        var isSaved = false
        updateApiClientId(accountId, clientId).handle(
            onSuccess = { isSaved = true },
            onFailure = { error -> logError(error) },
        )
        isSaved
    }

    private fun navigateBack() {
        val currentState = state.value
        if (currentState.requiresClientId && currentState.clientId.isNotBlank()) {
            viewModelScope.launch {
                saveClientId(currentState.clientId)
                emitEffect(ApiSettingsContract.Effect.NavigateBack)
            }
        } else {
            emitEffect(ApiSettingsContract.Effect.NavigateBack)
        }
    }

    private fun reauthorize() {
        val currentState = state.value
        if (!currentState.isApiAccount || (currentState.requiresClientId && currentState.clientId.isBlank())) return

        if (currentState.requiresClientId) {
            viewModelScope.launch {
                if (saveClientId(currentState.clientId)) {
                    emitEffect(ApiSettingsContract.Effect.ReauthorizeAccount)
                }
            }
        } else {
            emitEffect(ApiSettingsContract.Effect.ReauthorizeAccount)
        }
    }

    private fun logError(error: AccountSettingsDomainContract.AccountSettingError) {
        when (error) {
            is AccountSettingsDomainContract.AccountSettingError.NotFound -> logger.error(tag = TAG) { error.message }

            is AccountSettingsDomainContract.AccountSettingError.StorageError -> logger.error(tag = TAG) {
                error.message
            }

            is AccountSettingsDomainContract.AccountSettingError.UnsupportedFormat -> logger.error(tag = TAG) {
                error.message
            }
        }
    }
}
