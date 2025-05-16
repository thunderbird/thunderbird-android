package app.k9mail.core.android.common

import app.k9mail.core.android.common.camera.cameraModule
import app.k9mail.core.android.common.contact.contactModule
import net.thunderbird.core.common.coreCommonModule
import org.koin.core.module.Module
import org.koin.dsl.module

val coreCommonAndroidModule: Module = module {
    includes(coreCommonModule)

    includes(contactModule)

    includes(cameraModule)
}
