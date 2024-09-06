package app.k9mail.feature.migration.qrcode.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
import app.k9mail.feature.migration.qrcode.BuildConfig
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Effect
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.State
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.UiPermissionState
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QrCodeScannerScreenKtTest {
    init {
        // Running this test class in the release configuration fails with the following error message:
        // Unable to resolve activity for Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER]
        // cmp=app.k9mail.feature.migration.qrcode/androidx.activity.ComponentActivity } -- see
        // https://github.com/robolectric/robolectric/pull/4736 for details

        // So we make sure this test class is only run in the debug configuration.
        assumeTrue(BuildConfig.DEBUG)
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = FakeQrCodeScannerViewModel()

    @Test
    fun `starting screen should emit StartScreen event`() = runTest {
        setContentWithTheme {
            QrCodeScannerScreen(viewModel)
        }

        assertThat(viewModel.events).containsExactly(Event.StartScreen)
    }

    @Test
    fun `RequestCameraPermission effect should request CAMERA permission`() = runTest {
        lateinit var context: Context
        setContentWithTheme {
            context = LocalContext.current
            QrCodeScannerScreen(viewModel)
        }

        viewModel.effect(Effect.RequestCameraPermission)

        val shadowActivity = Shadow.extract<ShadowActivity>(context)
        assertThat(shadowActivity.lastRequestedPermission?.requestedPermissions?.toList())
            .isNotNull()
            .containsExactly(Manifest.permission.CAMERA)
    }

    @Test
    fun `GoToAppInfoScreen effect should launch intent`() = runTest {
        lateinit var context: Context
        setContentWithTheme {
            context = LocalContext.current
            QrCodeScannerScreen(viewModel)
        }

        viewModel.effect(Effect.GoToAppInfoScreen)

        val shadowActivity = Shadow.extract<ShadowActivity>(context)
        assertThat(shadowActivity.nextStartedActivity?.action)
            .isNotNull()
            .isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    }

    @Test
    fun `UiPermissionState_Granted should show QrCodeScannerView`() = runTest {
        setContentWithTheme {
            QrCodeScannerScreen(viewModel)
        }

        viewModel.applyState(State(cameraPermissionState = UiPermissionState.Granted))

        composeTestRule.onNodeWithTag("QrCodeScannerView").assertExists()
    }

    @Test
    fun `UiPermissionState_Denied should show PermissionDeniedContent`() = runTest {
        setContentWithTheme {
            QrCodeScannerScreen(viewModel)
        }

        viewModel.applyState(State(cameraPermissionState = UiPermissionState.Denied))

        composeTestRule.onNodeWithTag("PermissionDeniedContent").assertExists()
    }

    @Test
    fun `pressing 'go to settings' button should send GoToSettingsClicked event`() = runTest {
        setContentWithTheme {
            QrCodeScannerScreen(viewModel)
        }
        viewModel.events.clear()
        viewModel.applyState(State(cameraPermissionState = UiPermissionState.Denied))

        composeTestRule.onNodeWithTag("GoToSettingsButton").performClick()

        assertThat(viewModel.events).containsExactly(Event.GoToSettingsClicked)
    }

    private fun setContentWithTheme(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            K9MailTheme2 {
                content()
            }
        }
    }
}
