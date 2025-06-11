package net.thunderbird.core.common

import android.content.Context
import net.thunderbird.core.common.provider.AndroidContextProvider
import net.thunderbird.core.common.provider.ContextProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformCoreCommonModule: Module = module {
    single<ContextProvider<Context>> {
        AndroidContextProvider(context = androidApplication())
    }
}
