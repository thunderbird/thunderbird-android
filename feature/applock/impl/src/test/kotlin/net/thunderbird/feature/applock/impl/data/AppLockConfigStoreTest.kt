package net.thunderbird.feature.applock.impl.data

import android.os.Build
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.feature.applock.api.AppLockConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class AppLockConfigStoreTest {

    private val testSubject = AppLockConfigStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `should return default config when nothing is stored`() {
        val config = testSubject.getConfig()

        assertThat(config.isEnabled).isFalse()
        assertThat(config.timeoutMillis).isEqualTo(AppLockConfig.DEFAULT_TIMEOUT_MILLIS)
    }

    @Test
    fun `should persist and retrieve enabled state`() {
        testSubject.setConfig(AppLockConfig(isEnabled = true))

        val config = testSubject.getConfig()

        assertThat(config.isEnabled).isTrue()
    }

    @Test
    fun `should persist and retrieve timeout`() {
        testSubject.setConfig(AppLockConfig(timeoutMillis = 60_000L))

        val config = testSubject.getConfig()

        assertThat(config.timeoutMillis).isEqualTo(60_000L)
    }

    @Test
    fun `should persist and retrieve full config`() {
        val expected = AppLockConfig(isEnabled = true, timeoutMillis = 120_000L)

        testSubject.setConfig(expected)

        assertThat(testSubject.getConfig()).isEqualTo(expected)
    }

    @Test
    fun `should overwrite previous config`() {
        testSubject.setConfig(AppLockConfig(isEnabled = true, timeoutMillis = 60_000L))
        testSubject.setConfig(AppLockConfig(isEnabled = false, timeoutMillis = 30_000L))

        val config = testSubject.getConfig()

        assertThat(config.isEnabled).isFalse()
        assertThat(config.timeoutMillis).isEqualTo(30_000L)
    }
}
