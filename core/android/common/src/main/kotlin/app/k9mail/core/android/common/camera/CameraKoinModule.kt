package app.k9mail.core.android.common.camera

import app.k9mail.core.android.common.camera.io.CaptureImageFileWriter
import org.koin.dsl.module

internal val cameraModule = module {
    single { CaptureImageFileWriter(context = get()) }
    single<CameraCaptureHandler> {
        CameraCaptureHandler(
            captureImageFileWriter = get(),
        )
    }
}
