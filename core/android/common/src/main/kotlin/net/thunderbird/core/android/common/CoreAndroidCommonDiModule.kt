package net.thunderbird.core.android.common

import net.thunderbird.core.android.common.camera.cameraDiModule
import net.thunderbird.core.android.common.contact.contactDiModule
import net.thunderbird.core.android.common.resources.resourcesAndroidDiModule
import net.thunderbird.core.common.coreCommonDiModule
import org.koin.core.module.Module
import org.koin.dsl.module

val coreAndroidCommonDiModule: Module = module {
    includes(resourcesAndroidDiModule)

    includes(coreCommonDiModule)

    includes(contactDiModule)

    includes(cameraDiModule)
}
