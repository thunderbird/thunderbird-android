package net.thunderbird.core.android.common.resources

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

internal val resourcesAndroidModule: Module = module {
    single { AndroidResourceManager(context = androidApplication()) }
    single<ResourceManager> { get<AndroidResourceManager>() }
    single<StringsResourceManager> { get<AndroidResourceManager>() }
    single<PluralsResourceManager> { get<AndroidResourceManager>() }
}
