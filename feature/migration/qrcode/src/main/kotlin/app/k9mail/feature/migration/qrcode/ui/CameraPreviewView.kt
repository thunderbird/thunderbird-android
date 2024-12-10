package app.k9mail.feature.migration.qrcode.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.feature.migration.qrcode.domain.QrCodeDomainContract.UseCase.CameraUseCasesProvider
import android.graphics.Color as AndroidColor

/**
 * Displays a camera preview and includes the provided CameraX [UseCase]s.
 */
@Composable
internal fun CameraPreviewView(
    cameraUseCasesProvider: CameraUseCasesProvider,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        TextTitleLarge(
            text = "Camera preview",
            textAlign = TextAlign.Center,
            modifier = modifier.background(Color.DarkGray),
        )
        return
    }

    val lensFacing = CameraSelector.LENS_FACING_BACK

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)

        val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview = Preview.Builder().build()
        val cameraUseCases = cameraUseCasesProvider.getUseCases()

        cameraProvider.unbindAll()

        @Suppress("SpreadOperator")
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, *cameraUseCases.toTypedArray())

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
