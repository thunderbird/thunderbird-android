package app.k9mail.core.android.permissions

/**
 * Checks if a [Permission] has been granted to the app.
 */
interface PermissionChecker {
    fun checkPermission(permission: Permission): PermissionState
}
