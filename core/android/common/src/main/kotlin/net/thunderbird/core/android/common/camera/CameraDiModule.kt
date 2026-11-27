package net.thunderbird.core.android.common.camera

import net.thunderbird.core.android.common.camera.io.CaptureImageFileWriter
import org.koin.dsl.module

internal val cameraDiModule = module {
    single { CaptureImageFileWriter(context = get()) }
    single<CameraCaptureHandler> {
        CameraCaptureHandler(
            captureImageFileWriter = get(),
        )
    }
}
