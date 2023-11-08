package app.k9mail.feature.onboarding.permissions.domain.usecase

import app.k9mail.core.android.permissions.Permission
import app.k9mail.core.android.permissions.PermissionChecker
import app.k9mail.core.android.permissions.PermissionState
import app.k9mail.feature.onboarding.permissions.domain.PermissionsDomainContract.UseCase

/**
 * Checks if a [Permission] has been granted to the app.
 */
class CheckPermission(
    private val permissionChecker: PermissionChecker,
) : UseCase.CheckPermission {

    override fun invoke(permission: Permission): PermissionState {
        return permissionChecker.checkPermission(permission)
    }
}
