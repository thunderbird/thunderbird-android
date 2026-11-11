package app.k9mail.feature.onboarding.permissions

import app.k9mail.core.android.permissions.coreAndroidPermissionsDiModule
import app.k9mail.feature.onboarding.permissions.domain.PermissionsDomainContract.UseCase
import app.k9mail.feature.onboarding.permissions.domain.usecase.CheckPermission
import app.k9mail.feature.onboarding.permissions.ui.PermissionsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureOnboardingPermissionsDiModule: Module = module {
    includes(coreAndroidPermissionsDiModule)

    factory<UseCase.CheckPermission> { CheckPermission(permissionChecker = get()) }

    viewModel {
        PermissionsViewModel(
            checkPermission = get(),
        )
    }
}
