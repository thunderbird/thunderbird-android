package app.k9mail.core.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Checks if a [Permission] has been granted to the app.
 */
class AndroidPermissionChecker(
    private val context: Context,
) : PermissionChecker {

    override fun checkPermission(permission: Permission): PermissionState {
        return when (permission) {
            Permission.Contacts -> {
                checkSelfPermission(Manifest.permission.READ_CONTACTS)
            }
            Permission.Notifications -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionState.GrantedImplicitly
                }
            }
        }
    }

    private fun checkSelfPermission(permission: String): PermissionState {
        return if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }
}
