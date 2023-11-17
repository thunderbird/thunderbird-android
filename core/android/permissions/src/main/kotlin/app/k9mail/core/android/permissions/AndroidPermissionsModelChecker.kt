package app.k9mail.core.android.permissions

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Checks if the Android version the app is running on supports runtime permissions.
 */
internal class AndroidPermissionsModelChecker : PermissionsModelChecker {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
    override fun hasRuntimePermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}
