package app.k9mail.core.android.permissions

import org.koin.core.module.Module
import org.koin.dsl.module

val corePermissionsAndroidModule: Module = module {
    factory<PermissionChecker> { AndroidPermissionChecker(context = get()) }
    factory<PermissionsModelChecker> { AndroidPermissionsModelChecker() }
}
