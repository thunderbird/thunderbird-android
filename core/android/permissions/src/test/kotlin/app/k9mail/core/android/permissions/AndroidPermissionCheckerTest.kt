package app.k9mail.core.android.permissions

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
class AndroidPermissionCheckerTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val shadowApplication = Shadows.shadowOf(application)

    private val permissionChecker = AndroidPermissionChecker(application)

    @Test
    fun `granted READ_CONTACTS permission`() {
        shadowApplication.grantPermissions(Manifest.permission.READ_CONTACTS)

        val result = permissionChecker.checkPermission(Permission.Contacts)

        assertThat(result).isEqualTo(PermissionState.Granted)
    }

    @Test
    fun `denied READ_CONTACTS permission`() {
        shadowApplication.denyPermissions(Manifest.permission.READ_CONTACTS)

        val result = permissionChecker.checkPermission(Permission.Contacts)

        assertThat(result).isEqualTo(PermissionState.Denied)
    }

    @Test
    fun `granted POST_NOTIFICATIONS permission`() {
        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val result = permissionChecker.checkPermission(Permission.Notifications)

        assertThat(result).isEqualTo(PermissionState.Granted)
    }

    @Test
    fun `denied POST_NOTIFICATIONS permission`() {
        shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val result = permissionChecker.checkPermission(Permission.Notifications)

        assertThat(result).isEqualTo(PermissionState.Denied)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S_V2, maxSdk = Build.VERSION_CODES.S_V2)
    fun `POST_NOTIFICATIONS permission not available`() {
        val result = permissionChecker.checkPermission(Permission.Notifications)

        assertThat(result).isEqualTo(PermissionState.GrantedImplicitly)
    }
}
