package app.k9mail.feature.onboarding.permissions.domain.usecase

import app.k9mail.core.android.permissions.PermissionsModelChecker
import app.k9mail.feature.onboarding.permissions.domain.PermissionsDomainContract

class HasRuntimePermissions(
    private val permissionsModelChecker: PermissionsModelChecker,
) : PermissionsDomainContract.UseCase.HasRuntimePermissions {
    override fun invoke(): Boolean {
        return permissionsModelChecker.hasRuntimePermissions()
    }
}
