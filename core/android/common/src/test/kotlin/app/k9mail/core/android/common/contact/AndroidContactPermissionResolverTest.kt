package app.k9mail.core.android.common.contact

import android.Manifest
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class AndroidContactPermissionResolverTest {
    private val application = RuntimeEnvironment.getApplication()
    private val testSubject = AndroidContactPermissionResolver(context = application)

    @Test
    fun `hasPermission() with contact permission`() {
        grantContactPermission()

        val result = testSubject.hasContactPermission()

        assertThat(result).isTrue()
    }

    @Test
    fun `hasPermission() without contact permission`() {
        denyContactPermission()

        val result = testSubject.hasContactPermission()

        assertThat(result).isFalse()
    }

    private fun grantContactPermission() {
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.READ_CONTACTS)
    }

    private fun denyContactPermission() {
        Shadows.shadowOf(application).denyPermissions(Manifest.permission.READ_CONTACTS)
    }
}
