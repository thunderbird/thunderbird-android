package net.thunderbird.feature.applock.impl.domain

import androidx.biometric.BiometricPrompt
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.thunderbird.feature.applock.api.AppLockError
import org.junit.Test

class MapErrorCodeTest {

    @Test
    fun `should map ERROR_HW_NOT_PRESENT to NotAvailable`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_HW_NOT_PRESENT, "")

        assertThat(result).isEqualTo(AppLockError.NotAvailable)
    }

    @Test
    fun `should map ERROR_HW_UNAVAILABLE to NotAvailable`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_HW_UNAVAILABLE, "")

        assertThat(result).isEqualTo(AppLockError.NotAvailable)
    }

    @Test
    fun `should map ERROR_NO_BIOMETRICS to NotEnrolled`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_NO_BIOMETRICS, "")

        assertThat(result).isEqualTo(AppLockError.NotEnrolled)
    }

    @Test
    fun `should map ERROR_NO_DEVICE_CREDENTIAL to NotEnrolled`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, "")

        assertThat(result).isEqualTo(AppLockError.NotEnrolled)
    }

    @Test
    fun `should map ERROR_USER_CANCELED to Canceled`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_USER_CANCELED, "")

        assertThat(result).isEqualTo(AppLockError.Canceled)
    }

    @Test
    fun `should map ERROR_NEGATIVE_BUTTON to Canceled`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_NEGATIVE_BUTTON, "")

        assertThat(result).isEqualTo(AppLockError.Canceled)
    }

    @Test
    fun `should map ERROR_CANCELED to Interrupted`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_CANCELED, "")

        assertThat(result).isEqualTo(AppLockError.Interrupted)
    }

    @Test
    fun `should map ERROR_LOCKOUT to temporary Lockout`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_LOCKOUT, "")

        assertThat(result).isEqualTo(AppLockError.Lockout(durationSeconds = 0))
    }

    @Test
    fun `should map ERROR_LOCKOUT_PERMANENT to permanent Lockout`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_LOCKOUT_PERMANENT, "")

        assertThat(result).isEqualTo(AppLockError.Lockout(durationSeconds = -1))
    }

    @Test
    fun `should map ERROR_TIMEOUT to Failed`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_TIMEOUT, "")

        assertThat(result).isEqualTo(AppLockError.Failed)
    }

    @Test
    fun `should map ERROR_UNABLE_TO_PROCESS to Failed`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, "")

        assertThat(result).isEqualTo(AppLockError.Failed)
    }

    @Test
    fun `should map ERROR_NO_SPACE to Failed`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_NO_SPACE, "")

        assertThat(result).isEqualTo(AppLockError.Failed)
    }

    @Test
    fun `should map ERROR_VENDOR to Failed`() {
        val result = mapErrorCode(BiometricPrompt.ERROR_VENDOR, "")

        assertThat(result).isEqualTo(AppLockError.Failed)
    }

    @Test
    fun `should map unknown error code to UnableToStart with error string`() {
        val result = mapErrorCode(9999, "Some unknown error")

        assertThat(result).isInstanceOf<AppLockError.UnableToStart>()
        assertThat((result as AppLockError.UnableToStart).message).isEqualTo("Some unknown error")
    }
}
