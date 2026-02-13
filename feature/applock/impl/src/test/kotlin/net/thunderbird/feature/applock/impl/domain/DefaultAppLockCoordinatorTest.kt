package net.thunderbird.feature.applock.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.applock.api.AppLockAuthenticator
import net.thunderbird.feature.applock.api.AppLockConfig
import net.thunderbird.feature.applock.api.AppLockError
import net.thunderbird.feature.applock.api.AppLockResult
import net.thunderbird.feature.applock.api.AppLockState
import net.thunderbird.feature.applock.api.UnavailableReason
import net.thunderbird.feature.applock.api.isUnlocked
import org.junit.Test

class DefaultAppLockCoordinatorTest {

    @Test
    fun `should require auth on cold start when enabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should return Unavailable on cold start when enabled but auth unavailable`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
            biometricAvailable = false,
            unavailableReason = UnavailableReason.NOT_ENROLLED,
        )

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Unavailable(UnavailableReason.NOT_ENROLLED))
    }

    @Test
    fun `should do nothing on foreground when feature is disabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )

        testSubject.onAppForegrounded()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)
    }

    @Test
    fun `should transition to Unavailable on foreground when auth is unavailable`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
            biometricAvailable = false,
            unavailableReason = UnavailableReason.NO_HARDWARE,
        )

        testSubject.onAppForegrounded()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Unavailable(UnavailableReason.NO_HARDWARE))
    }

    @Test
    fun `should keep Locked state on foreground when pull model requires ensureUnlocked`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.onAppForegrounded()

        // In pull model, onAppForegrounded does NOT auto-transition to Unlocking
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should transition Locked to Unlocking when ensureUnlocked called`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        val result = testSubject.ensureUnlocked()

        assertThat(result).isTrue()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()
    }

    @Test
    fun `should return false when ensureUnlocked called and already Unlocking`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        // Second call should return false (already unlocking)
        val result = testSubject.ensureUnlocked()

        assertThat(result).isFalse()
    }

    @Test
    fun `should return true when ensureUnlocked called and already Unlocked`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.success())

        val result = testSubject.ensureUnlocked()

        assertThat(result).isTrue()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()
    }

    @Test
    fun `should transition Failed to Unlocking when ensureUnlocked called`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.failure(AppLockError.Failed))
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Failed(AppLockError.Failed))

        val result = testSubject.ensureUnlocked()

        assertThat(result).isTrue()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()
    }

    @Test
    fun `should lock on foreground when timeout exceeded since background`() = runTest {
        var now = 100_000L
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true, timeoutMillis = 60_000L),
            clock = { now },
        )

        // Unlock
        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.success())
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()

        // Go to background and advance time past timeout
        testSubject.onAppBackgrounded()
        now += 120_000L

        testSubject.onAppForegrounded()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should stay Unlocked on foreground when timeout not exceeded since background`() = runTest {
        var now = 100_000L
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true, timeoutMillis = 60_000L),
            clock = { now },
        )

        // Unlock
        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.success())
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()

        // Go to background but don't advance time past timeout
        testSubject.onAppBackgrounded()
        now += 30_000L

        testSubject.onAppForegrounded()

        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()
    }

    @Test
    fun `should lock immediately on foreground when timeout is zero`() = runTest {
        var now = 100_000L
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true, timeoutMillis = 0L),
            clock = { now },
        )

        // Unlock
        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.success())
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()

        // Go to background and advance time minimally
        testSubject.onAppBackgrounded()
        now += 1L

        testSubject.onAppForegrounded()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should cancel Unlocking state when backgrounded`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        testSubject.onAppBackgrounded()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should lock when screen off and Unlocked`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.success())
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()

        testSubject.onScreenOff()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should lock when screen off and Unlocking`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        testSubject.onScreenOff()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should do nothing when screen off and Disabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )

        testSubject.onScreenOff()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)
    }

    @Test
    fun `should transition to Locked when lockNow called`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.success())
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()

        testSubject.lockNow()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should transition to Locked when settings changed to enabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )

        testSubject.onSettingsChanged(AppLockConfig(isEnabled = true))

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should update state when authentication succeeds`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        val result = testSubject.authenticate(FakeAuthenticator.success())

        assertThat(result).isEqualTo(Outcome.Success(Unit))
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()
    }

    @Test
    fun `should update state when authentication fails`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        val result = testSubject.authenticate(FakeAuthenticator.failure(AppLockError.Failed))

        assertThat(result).isEqualTo(Outcome.Failure(AppLockError.Failed))
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Failed(AppLockError.Failed))
    }

    @Test
    fun `should return error when authenticate called and not in Unlocking state`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        // Don't call ensureUnlocked - state is Locked, not Unlocking
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)

        val result = testSubject.authenticate(FakeAuthenticator.success())

        assertThat(result).isEqualTo(Outcome.Failure(AppLockError.UnableToStart("Not in Unlocking state")))
    }

    @Test
    fun `should transition to Locked when authentication interrupted`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        val result = testSubject.authenticate(FakeAuthenticator.failure(AppLockError.Interrupted))

        assertThat(result).isEqualTo(Outcome.Failure(AppLockError.Interrupted))
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should return true for isEnabled when feature is enabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        assertThat(testSubject.isEnabled).isTrue()
    }

    @Test
    fun `should return false for isEnabled when feature is disabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )

        assertThat(testSubject.isEnabled).isFalse()
    }

    @Test
    fun `should disable lock when settings changed to disabled`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)

        testSubject.onSettingsChanged(AppLockConfig(isEnabled = false))

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)
    }

    @Test
    fun `should allow successful authentication when ensureUnlocked called after failure`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.failure(AppLockError.Failed))
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Failed(AppLockError.Failed))

        // ensureUnlocked transitions Failed -> Unlocking
        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        val result = testSubject.authenticate(FakeAuthenticator.success())

        assertThat(result).isEqualTo(Outcome.Success(Unit))
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()
    }

    @Test
    fun `should return false when ensureUnlocked called and Unavailable`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
            biometricAvailable = false,
            unavailableReason = UnavailableReason.NOT_ENROLLED,
        )
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unavailable>()

        val result = testSubject.ensureUnlocked()

        assertThat(result).isFalse()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unavailable>()
    }

    @Test
    fun `should return false for isUnlocked when state is Unavailable`() = runTest {
        val state = AppLockState.Unavailable(UnavailableReason.NOT_ENROLLED)

        assertThat(state.isUnlocked()).isFalse()
    }

    @Test
    fun `should reject concurrent authenticate calls`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()

        // First call - use a suspending authenticator
        val suspendingAuthenticator = SuspendingAuthenticator()
        val firstJob = launch {
            testSubject.authenticate(suspendingAuthenticator)
        }

        // Wait for first call to start
        suspendingAuthenticator.awaitStarted()

        // Second concurrent call should be rejected
        val result = testSubject.authenticate(FakeAuthenticator.success())

        assertThat(result).isEqualTo(
            Outcome.Failure(AppLockError.UnableToStart("Authentication already in progress")),
        )

        // Complete first call
        suspendingAuthenticator.complete(Outcome.Success(Unit))
        firstJob.join()
    }

    @Test
    fun `should transition Unavailable to Locked when refreshAvailability finds auth available`() = runTest {
        val availability = FakeAppLockAvailability(available = false, reason = UnavailableReason.NOT_ENROLLED)
        val testSubject = createTestSubjectWithAvailability(
            config = AppLockConfig(isEnabled = true),
            availability = availability,
        )

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Unavailable(UnavailableReason.NOT_ENROLLED))

        // User sets up authentication in device settings
        availability.setAvailable(true)
        testSubject.refreshAvailability()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should do nothing when refreshAvailability called and not in Unavailable state`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)

        testSubject.refreshAvailability()

        // State should remain Locked
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should transition to Disabled when refreshAvailability called and lock is disabled`() = runTest {
        val configRepository = FakeAppLockConfigRepository(AppLockConfig(isEnabled = true))
        val availability = FakeAppLockAvailability(available = false, reason = UnavailableReason.NOT_ENROLLED)
        val testSubject = DefaultAppLockCoordinator(
            configRepository = configRepository,
            availability = availability,
            mainThreadCheck = {},
        )

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Unavailable(UnavailableReason.NOT_ENROLLED))

        // User disabled app lock while in unavailable state
        configRepository.setConfig(AppLockConfig(isEnabled = false))
        testSubject.refreshAvailability()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)
    }

    @Test
    fun `should authenticate and enable when requestEnable succeeds`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)

        val result = testSubject.requestEnable(FakeAuthenticator.success())

        assertThat(result).isEqualTo(Outcome.Success(Unit))
        assertThat(testSubject.config.isEnabled).isTrue()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocked>()
    }

    @Test
    fun `should not enable when requestEnable authentication fails`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)

        val result = testSubject.requestEnable(FakeAuthenticator.failure(AppLockError.Canceled))

        assertThat(result).isEqualTo(Outcome.Failure(AppLockError.Canceled))
        assertThat(testSubject.config.isEnabled).isFalse()
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)
    }

    @Test
    fun `should reject requestEnable when auth unavailable`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
            biometricAvailable = false,
        )

        val result = testSubject.requestEnable(FakeAuthenticator.success())

        assertThat(result).isEqualTo(Outcome.Failure(AppLockError.NotAvailable))
        assertThat(testSubject.config.isEnabled).isFalse()
    }

    @Test
    fun `should reject concurrent requestEnable calls`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = false),
        )

        val suspendingAuthenticator = SuspendingAuthenticator()
        val firstJob = launch {
            testSubject.requestEnable(suspendingAuthenticator)
        }

        suspendingAuthenticator.awaitStarted()

        val result = testSubject.requestEnable(FakeAuthenticator.success())

        assertThat(result).isEqualTo(
            Outcome.Failure(AppLockError.UnableToStart("Authentication already in progress")),
        )

        suspendingAuthenticator.complete(Outcome.Success(Unit))
        firstJob.join()
    }

    @Test
    fun `should transition Failed to Locked when backgrounded`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        testSubject.ensureUnlocked()
        testSubject.authenticate(FakeAuthenticator.failure(AppLockError.Failed))
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Failed(AppLockError.Failed))

        testSubject.onAppBackgrounded()

        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should require re-auth when backgrounded during credential activity`() = runTest {
        val testSubject = createTestSubject(
            config = AppLockConfig(isEnabled = true),
        )

        // Start unlock flow
        testSubject.ensureUnlocked()
        assertThat(testSubject.state.value).isInstanceOf<AppLockState.Unlocking>()

        // Simulate: auth is in progress (e.g., credential activity launched)
        val suspendingAuthenticator = SuspendingAuthenticator()
        val authJob = launch {
            testSubject.authenticate(suspendingAuthenticator)
        }
        suspendingAuthenticator.awaitStarted()

        // App goes to background (e.g., user switches away) — coordinator resets to Locked
        testSubject.onAppBackgrounded()
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)

        // The credential activity completes successfully, but coordinator already moved to Locked
        suspendingAuthenticator.complete(Outcome.Success(Unit))
        authJob.join()

        // State remains Locked — the successful auth result is discarded because the coordinator
        // was no longer in Unlocking state. This is intentional: backgrounding invalidates any
        // in-flight authentication, requiring the user to re-authenticate on next foreground.
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Locked)
    }

    @Test
    fun `should reject enabling when settings changed and auth unavailable`() = runTest {
        val availability = FakeAppLockAvailability(available = true)
        val testSubject = createTestSubjectWithAvailability(
            config = AppLockConfig(isEnabled = false),
            availability = availability,
        )
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)

        // Make auth unavailable then try to enable
        availability.setAvailable(false)
        testSubject.onSettingsChanged(AppLockConfig(isEnabled = true))

        // State should remain Disabled - enabling was rejected
        assertThat(testSubject.state.value).isEqualTo(AppLockState.Disabled)
        // Config should not be persisted
        assertThat(testSubject.config.isEnabled).isFalse()
    }

    private class SuspendingAuthenticator : AppLockAuthenticator {
        private val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        private val result = kotlinx.coroutines.CompletableDeferred<AppLockResult>()

        suspend fun awaitStarted() = started.await()
        fun complete(value: AppLockResult) = result.complete(value)

        override suspend fun authenticate(): AppLockResult {
            started.complete(Unit)
            return result.await()
        }
    }

    private fun createTestSubject(
        config: AppLockConfig,
        biometricAvailable: Boolean = true,
        unavailableReason: UnavailableReason = UnavailableReason.NO_HARDWARE,
        clock: () -> Long = { System.currentTimeMillis() },
    ): DefaultAppLockCoordinator {
        val configRepository = FakeAppLockConfigRepository(config)
        val availability = FakeAppLockAvailability(available = biometricAvailable, reason = unavailableReason)

        return DefaultAppLockCoordinator(
            configRepository = configRepository,
            availability = availability,
            clock = clock,
            mainThreadCheck = {},
        )
    }

    private class FakeAppLockConfigRepository(
        private var config: AppLockConfig,
    ) : AppLockConfigRepository {
        override fun getConfig(): AppLockConfig = config

        override fun setConfig(config: AppLockConfig) {
            this.config = config
        }
    }

    private class FakeAppLockAvailability(
        private var available: Boolean,
        private val reason: UnavailableReason = UnavailableReason.NO_HARDWARE,
    ) : AppLockAvailability {
        fun setAvailable(available: Boolean) {
            this.available = available
        }

        override fun isAuthenticationAvailable(): Boolean = available
        override fun getUnavailableReason(): UnavailableReason = reason
    }

    private fun createTestSubjectWithAvailability(
        config: AppLockConfig,
        availability: FakeAppLockAvailability,
        clock: () -> Long = { System.currentTimeMillis() },
    ): DefaultAppLockCoordinator {
        val configRepository = FakeAppLockConfigRepository(config)

        return DefaultAppLockCoordinator(
            configRepository = configRepository,
            availability = availability,
            clock = clock,
            mainThreadCheck = {},
        )
    }
}
