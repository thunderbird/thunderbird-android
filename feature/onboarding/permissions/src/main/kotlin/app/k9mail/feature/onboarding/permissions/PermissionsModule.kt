package app.k9mail.feature.onboarding.permissions

import app.k9mail.core.android.permissions.corePermissionsAndroidModule
import app.k9mail.feature.onboarding.permissions.domain.PermissionsDomainContract.UseCase
import app.k9mail.feature.onboarding.permissions.domain.usecase.CheckPermission
import app.k9mail.feature.onboarding.permissions.ui.PermissionsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureOnboardingPermissionsModule: Module = module {
    includes(corePermissionsAndroidModule)

    factory<UseCase.CheckPermission> { CheckPermission(permissionChecker = get()) }

    viewModel {
        PermissionsViewModel(
            checkPermission = get(),
        )
    }
}
