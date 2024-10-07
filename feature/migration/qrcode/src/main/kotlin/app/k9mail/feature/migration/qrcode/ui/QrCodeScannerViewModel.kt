package app.k9mail.feature.migration.qrcode.ui

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData
import app.k9mail.feature.migration.qrcode.domain.entity.AccountData.Account
import app.k9mail.feature.migration.qrcode.domain.usecase.QrCodeImageAnalysisProvider
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Effect
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.UiPermissionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

private typealias PayloadCallback = (String) -> Unit
private typealias CameraUseCaseProviderFactory = (PayloadCallback) -> UseCase.CameraUseCasesProvider

@Suppress("TooManyFunctions")
internal class QrCodeScannerViewModel(
    private val qrCodePayloadReader: UseCase.QrCodePayloadReader,
    private val qrCodeSettingsWriter: UseCase.QrCodeSettingsWriter,
    createCameraUseCaseProvider: CameraUseCaseProviderFactory = { callback -> QrCodeImageAnalysisProvider(callback) },
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    initialState: State = State(),
) : BaseViewModel<State, Event, Effect>(initialState), QrCodeScannerContract.ViewModel {
    private val supportedPayloadHashes = mutableSetOf<ByteString>()
    private val unsupportedPayloadHashes = ArrayDeque<ByteString>()
    private val accountDataList = mutableListOf<AccountData>()

    override val cameraUseCasesProvider: UseCase.CameraUseCasesProvider =
        createCameraUseCaseProvider(::handleQrCodeScanned)

    override fun event(event: Event) {
        when (event) {
            Event.StartScreen -> handleOneTimeEvent(event, ::handleStartScreen)
            is Event.CameraPermissionResult -> handleCameraPermissionResult(event.success)
            Event.GoToSettingsClicked -> handleGoToSettingsClicked()
            Event.ReturnedFromAppInfoScreen -> handleReturnedFromAndroidSettings()
            Event.DoneClicked -> handleDoneClicked()
        }
    }

    private fun handleStartScreen() {
        requestCameraPermission()
    }

    private fun handleCameraPermissionResult(success: Boolean) {
        updateState {
            it.copy(cameraPermissionState = if (success) UiPermissionState.Granted else UiPermissionState.Denied)
        }
    }

    private fun handleGoToSettingsClicked() {
        emitEffect(Effect.GoToAppInfoScreen)

        viewModelScope.launch {
            // Delay updating the UI to make sure Android's app settings screen is active and our activity is paused.
            // We want to prevent QrCodeScannerContent triggering the permission dialog again before the user had a
            // chance to enter Android's app settings screen.
            delay(APP_SETTINGS_DELAY)

            updateState {
                it.copy(cameraPermissionState = UiPermissionState.Waiting)
            }
        }
    }

    private fun handleReturnedFromAndroidSettings() {
        updateState {
            it.copy(cameraPermissionState = UiPermissionState.Unknown)
        }

        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        emitEffect(Effect.RequestCameraPermission)
    }

    private fun handleQrCodeScanned(payload: String) {
        val payloadHash = payload.sha1
        if (payloadHash in supportedPayloadHashes || payloadHash in unsupportedPayloadHashes) {
            // This QR code has already been scanned. Skip further processing.
            return
        }

        val accountData = qrCodePayloadReader.read(payload)
        if (accountData != null) {
            handleSupportedPayload(accountData)
            supportedPayloadHashes.add(payloadHash)
        } else {
            if (unsupportedPayloadHashes.size > MAX_NUMBER_OF_UNSUPPORTED_PAYLOADS) {
                unsupportedPayloadHashes.removeFirst()
            }
            unsupportedPayloadHashes.add(payloadHash)
        }
    }

    private fun handleSupportedPayload(accountData: AccountData) {
        val currentState = state.value
        if (accountData.sequenceEnd == currentState.totalCount) {
            accountDataList.add(accountData)

            updateState {
                it.copy(scannedCount = accountDataList.size)
            }
        } else {
            // Total QR code count doesn't match previous value. The user has probably started over.

            supportedPayloadHashes.clear()
            accountDataList.clear()
            accountDataList.add(accountData)

            updateState {
                it.copy(scannedCount = 1, totalCount = accountData.sequenceEnd)
            }
        }

        if (accountDataList.size == accountData.sequenceEnd) {
            startAccountImport()
        }
    }

    private fun handleDoneClicked() {
        startAccountImport()
    }

    private fun startAccountImport() {
        val accounts = mergeAccounts(accountDataList)
        if (accounts.isEmpty()) {
            emitEffect(Effect.Cancel)
            return
        }

        viewModelScope.launch {
            val contentUri = withContext(backgroundDispatcher) {
                qrCodeSettingsWriter.write(accounts)
            }

            emitEffect(Effect.ReturnResult(contentUri))
        }
    }

    private fun mergeAccounts(accountDataList: List<AccountData>): List<Account> {
        return accountDataList.flatMap { it.accounts }
    }

    private val String.sha1: ByteString
        get() = encodeUtf8().sha1()
}

private const val APP_SETTINGS_DELAY = 100L
private const val MAX_NUMBER_OF_UNSUPPORTED_PAYLOADS = 5
