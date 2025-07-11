package net.thunderbird.core.android.common.resources

import net.thunderbird.core.common.resources.PluralsResourceManager
import net.thunderbird.core.common.resources.ResourceManager
import net.thunderbird.core.common.resources.StringsResourceManager
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

internal val resourcesAndroidModule: Module = module {
    single { AndroidResourceManager(context = androidApplication()) }
    single<ResourceManager> { get<AndroidResourceManager>() }
    single<StringsResourceManager> { get<AndroidResourceManager>() }
    single<PluralsResourceManager> { get<AndroidResourceManager>() }
}
