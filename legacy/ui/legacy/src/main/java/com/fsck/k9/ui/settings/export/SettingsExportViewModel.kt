package com.fsck.k9.ui.settings.export

import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewModelScope
import com.fsck.k9.preferences.SettingsExporter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

private typealias AccountUuid = String
private typealias AccountNumber = Int

private const val TAG = "SettingsExportViewModel"

class SettingsExportViewModel(
    val accountManager: LegacyAccountManager,
    val settingsExporter: SettingsExporter,
    private val logger: Logger,
    private val realtimeMeasurer: RealtimeMeasurer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<SettingsExportContract.State, SettingsExportContract.Event, SettingsExportContract.Effect>(
    initialState = SettingsExportContract.State(),
) {
    private var contentUri: Uri? = null
    private var accountsMap: Map<AccountNumber, AccountUuid> = emptyMap()

    init {
        loadSettingsExportList()
    }

    @Suppress("LongMethod")
    override fun event(event: SettingsExportContract.Event) {
        when (event) {
            is SettingsExportContract.Event.OnSettingsItemSelectionChange -> {
                updateState { state ->
                    state.copy(
                        settingsList = state.settingsList.map { item ->
                            if (item.stableId != event.itemId) {
                                item
                            } else {
                                when (item) {
                                    is SettingsListItem.GeneralSettings ->
                                        item.copy(selected = event.isSelected)

                                    is SettingsListItem.Account ->
                                        item.copy(selected = event.isSelected)
                                }
                            }
                        },
                    )
                }
            }

            is SettingsExportContract.Event.OnExportClick -> {
                disableExportButton()
                startExportSettings()
            }

            is SettingsExportContract.Event.OnShareClick -> {
                contentUri?.let {
                    emitEffect(SettingsExportContract.Effect.ShareDocument(it, SETTINGS_MIME_TYPE))
                }
            }

            is SettingsExportContract.Event.OnDocumentPicked -> {
                contentUri = event.contentUri
                updateState { state ->
                    state.copy(
                        isProgressVisible = true,
                        exportButtonState = ButtonState.INVISIBLE,
                        isShareButtonVisible = false,
                        isSettingsListEnabled = false,
                        statusText = StatusText.PROGRESS,
                    )
                }

                val includeGeneralSettings = state.value.settingsList
                    .find { it is SettingsListItem.GeneralSettings }?.selected ?: false
                val selectedAccounts = state.value.settingsList
                    .filterIsInstance<SettingsListItem.Account>().filter { it.selected }
                    .map {
                        accountsMap[it.accountNumber] ?: error("Unknown account number: ${it.accountNumber}")
                    }.toSet()

                viewModelScope.launch {
                    try {
                        val elapsed = realtimeMeasurer.measure {
                            withContext(ioDispatcher) {
                                contentUri?.let {
                                    settingsExporter.exportToUri(
                                        includeGeneralSettings,
                                        selectedAccounts,
                                        it,
                                    )
                                }
                            }
                        }

                        if (elapsed < MIN_PROGRESS_DURATION) {
                            delay((MIN_PROGRESS_DURATION - elapsed).milliseconds)
                        }

                        updateState { state ->
                            state.copy(
                                exportButtonState = ButtonState.GONE,
                                isProgressVisible = false,
                                isShareButtonVisible = true,
                                isSettingsListEnabled = true,
                                statusText = StatusText.EXPORT_SUCCESS,
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(TAG, e) { "Error getting widget configuration for widget ID" }
                        updateState { state ->
                            state.copy(
                                exportButtonState = ButtonState.GONE,
                                isProgressVisible = false,
                                isShareButtonVisible = false,
                                isSettingsListEnabled = true,
                                statusText = StatusText.EXPORT_FAILURE,
                            )
                        }
                    }
                }
            }

            is SettingsExportContract.Event.OnDocumentPickCanceled -> {
                enableExportButton()
            }
        }
    }

    private fun startExportSettings() {
        val exportFileName = settingsExporter.generateDatedExportFileName()
        emitEffect(SettingsExportContract.Effect.PickDocument(exportFileName, SETTINGS_MIME_TYPE))
    }

    private fun enableExportButton() {
        updateState { state ->
            state.copy(
                exportButtonState = ButtonState.ENABLED,
                isShareButtonVisible = false,
                isProgressVisible = false,
                isSettingsListEnabled = true,
            )
        }
    }

    private fun disableExportButton() {
        updateState { state ->
            state.copy(
                exportButtonState = ButtonState.DISABLED,
                isShareButtonVisible = false,
                isProgressVisible = false,
            )
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SETTINGS_LIST_ENABLED, state.value.isSettingsListEnabled)
        outState.putString(STATE_EXPORT_BUTTON, state.value.exportButtonState.name)
        outState.putBoolean(STATE_SHARE_BUTTON_VISIBLE, state.value.isShareButtonVisible)
        outState.putBoolean(STATE_PROGRESS_VISIBLE, state.value.isProgressVisible)
        outState.putString(STATE_STATUS_TEXT, state.value.statusText.name)
        outState.putParcelable(STATE_CONTENT_URI, contentUri)
    }

    fun initializeFromSavedState(savedInstanceState: Bundle) {
        updateState { state ->
            state.copy(
                isSettingsListEnabled = savedInstanceState.getBoolean(STATE_SETTINGS_LIST_ENABLED),
                exportButtonState = ButtonState.valueOf(
                    savedInstanceState.getString(STATE_EXPORT_BUTTON, ButtonState.DISABLED.name),
                ),
                isShareButtonVisible = savedInstanceState.getBoolean(STATE_SHARE_BUTTON_VISIBLE),
                isProgressVisible = savedInstanceState.getBoolean(STATE_PROGRESS_VISIBLE),
                statusText = StatusText.valueOf(
                    savedInstanceState.getString(
                        STATE_STATUS_TEXT,
                        StatusText.HIDDEN.name,
                    ),
                ),
            )
        }
        contentUri = BundleCompat.getParcelable(
            savedInstanceState,
            STATE_CONTENT_URI, Uri::class.java,
        )
    }

    private fun loadSettingsExportList() {
        viewModelScope.launch {
            val accounts = withContext(ioDispatcher) {
                accountManager.findAll()
            }

            accountsMap = accounts.map { it.accountNumber to it.id.toString() }.toMap()

            val accountListItems = accounts.map { account ->
                SettingsListItem.Account(
                    accountNumber = account.accountNumber,
                    displayName = account.profile.name,
                    email = account.email,
                )
            }

            updateState {
                it.copy(
                    settingsList = listOf(
                        SettingsListItem.GeneralSettings(),
                    ) + accountListItems,
                )
            }
        }
    }

    companion object {
        private const val MIN_PROGRESS_DURATION = 1000L
        private const val SETTINGS_MIME_TYPE = "application/octet-stream"

        private const val STATE_SETTINGS_LIST_ENABLED = "settingsListEnabled"
        private const val STATE_EXPORT_BUTTON = "exportButton"
        private const val STATE_SHARE_BUTTON_VISIBLE = "shareButtonVisible"
        private const val STATE_PROGRESS_VISIBLE = "progressVisible"
        private const val STATE_STATUS_TEXT = "statusText"
        private const val STATE_CONTENT_URI = "contentUri"
    }
}




