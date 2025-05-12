package app.k9mail.core.android.common

import app.k9mail.core.android.common.camera.cameraModule
import app.k9mail.core.android.common.contact.contactModule
import net.thunderbird.core.android.common.resources.resourcesAndroidModule
import net.thunderbird.core.common.coreCommonModule
import org.koin.core.module.Module
import org.koin.dsl.module

val coreCommonAndroidModule: Module = module {
    includes(resourcesAndroidModule)

    includes(coreCommonModule)

    includes(contactModule)

    includes(cameraModule)
}
