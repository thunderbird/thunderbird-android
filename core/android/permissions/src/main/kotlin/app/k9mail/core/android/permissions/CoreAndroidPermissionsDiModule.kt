package app.k9mail.core.android.permissions

import org.koin.core.module.Module
import org.koin.dsl.module

val coreAndroidPermissionsDiModule: Module = module {
    factory<PermissionChecker> { AndroidPermissionChecker(context = get()) }
}
