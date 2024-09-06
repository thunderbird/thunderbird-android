package app.k9mail.feature.migration.qrcode.ui

import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import app.k9mail.core.ui.compose.testing.mvi.MviTurbines
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Effect
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.UiPermissionState
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class QrCodeScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `user grants camera permission`() = runTest {
        with(QrCodeScannerScreenRobot(testScope = this)) {
            startScreen()
            userGrantsCameraPermission()

            ensureThatAllEventsAreConsumed()
        }
    }

    @Test
    fun `user denies camera permission`() = runTest {
        with(QrCodeScannerScreenRobot(testScope = this)) {
            startScreen()
            userDeniesCameraPermission()

            ensureThatAllEventsAreConsumed()
        }
    }

    @Test
    fun `user grants camera permission via android app settings`() = runTest {
        with(QrCodeScannerScreenRobot(testScope = this)) {
            startScreen()
            systemDeniesCameraPermission()

            userClicksGoToSettings()
            userReturnsFromAppInfoScreen()
            systemGrantsCameraPermission()

            ensureThatAllEventsAreConsumed()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class QrCodeScannerScreenRobot(
    private val testScope: TestScope,
) {
    private val viewModel = QrCodeScannerViewModel()
    private lateinit var turbines: MviTurbines<State, Effect>

    private val initialState = State()

    suspend fun startScreen() {
        turbines = testScope.turbinesWithInitialStateCheck(viewModel, initialState)

        viewModel.event(Event.StartScreen)

        assertThat(turbines.awaitEffectItem()).isEqualTo(Effect.RequestCameraPermission)
    }

    suspend fun userGrantsCameraPermission() {
        grantCameraPermission()
    }

    suspend fun systemGrantsCameraPermission() {
        grantCameraPermission()
    }

    private suspend fun grantCameraPermission() {
        viewModel.event(Event.CameraPermissionResult(success = true))

        assertThat(turbines.awaitStateItem()).isEqualTo(State(cameraPermissionState = UiPermissionState.Granted))
    }

    suspend fun userDeniesCameraPermission() {
        denyCameraPermission()
    }

    suspend fun systemDeniesCameraPermission() {
        denyCameraPermission()
    }

    private suspend fun denyCameraPermission() {
        viewModel.event(Event.CameraPermissionResult(success = false))

        assertThat(turbines.awaitStateItem()).isEqualTo(State(cameraPermissionState = UiPermissionState.Denied))
    }

    suspend fun userClicksGoToSettings() {
        viewModel.event(Event.GoToSettingsClicked)

        assertThat(turbines.awaitEffectItem()).isEqualTo(Effect.GoToAppInfoScreen)
        assertThat(turbines.awaitStateItem()).isEqualTo(State(cameraPermissionState = UiPermissionState.Waiting))
    }

    suspend fun userReturnsFromAppInfoScreen() {
        viewModel.event(Event.ReturnedFromAppInfoScreen)

        assertThat(turbines.awaitStateItem()).isEqualTo(State(cameraPermissionState = UiPermissionState.Unknown))
        assertThat(turbines.awaitEffectItem()).isEqualTo(Effect.RequestCameraPermission)
    }

    fun ensureThatAllEventsAreConsumed() {
        turbines.effectTurbine.ensureAllEventsConsumed()
        turbines.stateTurbine.ensureAllEventsConsumed()
    }
}
