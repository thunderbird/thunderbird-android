package app.k9mail.core.android.permissions

/**
 * Checks what permission model the system is using.
 */
interface PermissionsModelChecker {
    fun hasRuntimePermissions(): Boolean
}
