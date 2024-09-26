package app.k9mail.feature.migration.qrcode.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Effect
import app.k9mail.feature.migration.qrcode.ui.QrCodeScannerContract.Event
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
internal fun QrCodeScannerScreen(
    viewModel: QrCodeScannerContract.ViewModel = koinViewModel<QrCodeScannerViewModel>(),
) {
    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { success ->
        viewModel.event(Event.CameraPermissionResult(success))
    }

    val context = LocalContext.current

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.RequestCameraPermission -> cameraPermissionLauncher.requestCameraPermission()
            Effect.GoToAppInfoScreen -> context.goToAppInfoScreen()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.StartScreen)
    }

    QrCodeScannerContent(
        cameraUseCasesProvider = viewModel.cameraUseCasesProvider,
        state = state.value,
        onEvent = dispatch,
    )
}

private fun Context.goToAppInfoScreen() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }

    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Timber.e(e, "Error opening Android's app settings")
    }
}

private fun ManagedActivityResultLauncher<String, Boolean>.requestCameraPermission() {
    launch(Manifest.permission.CAMERA)
}
