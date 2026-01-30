package app.k9mail.feature.onboarding.permissions.domain

import app.k9mail.core.android.permissions.Permission
import app.k9mail.core.android.permissions.PermissionState

interface PermissionsDomainContract {

    interface UseCase {

        fun interface CheckPermission {
            operator fun invoke(permission: Permission): PermissionState
        }
    }
}
