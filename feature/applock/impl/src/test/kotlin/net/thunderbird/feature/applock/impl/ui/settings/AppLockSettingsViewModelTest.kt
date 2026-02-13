package net.thunderbird.feature.applock.impl.ui.settings

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import net.thunderbird.feature.applock.api.AppLockConfig
import net.thunderbird.feature.applock.impl.domain.FakeAppLockCoordinator
import net.thunderbird.feature.applock.impl.ui.settings.AppLockSettingsContract.Effect
import net.thunderbird.feature.applock.impl.ui.settings.AppLockSettingsContract.Event
import org.junit.Rule
import org.junit.Test

class AppLockSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should initialize state from coordinator config`() {
        val coordinator = FakeAppLockCoordinator().apply {
            onSettingsChanged(AppLockConfig(isEnabled = true, timeoutMillis = 180_000L))
        }

        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)

        assertThat(testSubject.state.value.isEnabled).isTrue()
        assertThat(testSubject.state.value.timeoutMinutes).isEqualTo(3)
        assertThat(testSubject.state.value.isAuthenticationAvailable).isTrue()
    }

    @Test
    fun `should emit RequestAuthentication effect when enabling`() = runTest {
        val coordinator = FakeAppLockCoordinator()
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)

        testSubject.effect.test {
            testSubject.event(Event.OnEnableChanged(true))

            assertThat(awaitItem()).isInstanceOf<Effect.RequestAuthentication>()
        }
    }

    @Test
    fun `should update state and coordinator when disabling`() {
        val coordinator = FakeAppLockCoordinator().apply {
            onSettingsChanged(AppLockConfig(isEnabled = true))
        }
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)

        testSubject.event(Event.OnEnableChanged(false))

        assertThat(testSubject.state.value.isEnabled).isFalse()
        assertThat(coordinator.config.isEnabled).isFalse()
    }

    @Test
    fun `should update state on successful authentication`() = runTest {
        val coordinator = FakeAppLockCoordinator()
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)
        val fakeAuthenticator = { Outcome.success(Unit) }

        testSubject.event(Event.OnAuthenticatorReady(fakeAuthenticator))

        assertThat(testSubject.state.value.isEnabled).isTrue()
    }

    @Test
    fun `should not update state on failed authentication`() = runTest {
        val coordinator = FakeAppLockCoordinator()
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)
        val fakeAuthenticator = { Outcome.failure(net.thunderbird.feature.applock.api.AppLockError.Failed) }

        testSubject.event(Event.OnAuthenticatorReady(fakeAuthenticator))

        assertThat(testSubject.state.value.isEnabled).isFalse()
    }

    @Test
    fun `should update state and coordinator when timeout changed`() {
        val coordinator = FakeAppLockCoordinator()
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)

        testSubject.event(Event.OnTimeoutChanged(5))

        assertThat(testSubject.state.value.timeoutMinutes).isEqualTo(5)
        assertThat(coordinator.config.timeoutMillis).isEqualTo(300_000L)
    }

    @Test
    fun `should refresh availability on resume`() {
        val coordinator = FakeAppLockCoordinator().apply {
            isAuthenticationAvailable = false
        }
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)

        assertThat(testSubject.state.value.isAuthenticationAvailable).isFalse()

        coordinator.isAuthenticationAvailable = true
        testSubject.event(Event.OnResume)

        assertThat(testSubject.state.value.isAuthenticationAvailable).isTrue()
    }

    @Test
    fun `should emit NavigateBack effect when back pressed`() = runTest {
        val coordinator = FakeAppLockCoordinator()
        val testSubject = AppLockSettingsViewModel(coordinator = coordinator)

        testSubject.effect.test {
            testSubject.event(Event.OnBackPressed)

            assertThat(awaitItem()).isInstanceOf<Effect.NavigateBack>()
        }
    }
}
