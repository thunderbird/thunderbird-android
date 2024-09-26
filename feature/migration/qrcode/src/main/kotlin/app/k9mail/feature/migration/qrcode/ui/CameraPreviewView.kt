package app.k9mail.feature.migration.qrcode.ui

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
internal fun CameraPreviewView(
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        TextTitleLarge(
            text = "Camera preview",
            textAlign = TextAlign.Center,
            modifier = modifier.background(Color.Cyan),
        )
        return
    }

    val lensFacing = CameraSelector.LENS_FACING_BACK

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = getProcessCameraProvider(context)

        val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview = Preview.Builder().build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview)

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

/**
 * Suspending function to retrieve a [ProcessCameraProvider].
 */
private suspend fun getProcessCameraProvider(context: Context): ProcessCameraProvider {
    return suspendCoroutine { continuation ->
        val mainExecutor = ContextCompat.getMainExecutor(context)

        ProcessCameraProvider.getInstance(context).also { processCameraProvider ->
            val listener = Runnable {
                continuation.resume(processCameraProvider.get())
            }

            processCameraProvider.addListener(listener, mainExecutor)
        }
    }
}
