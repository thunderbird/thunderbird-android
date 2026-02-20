package net.thunderbird.feature.applock.impl.domain

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.applock.api.AppLockAuthenticator
import net.thunderbird.feature.applock.api.AppLockError
import net.thunderbird.feature.applock.api.AppLockResult

/**
 * Fake implementation of [AppLockAuthenticator] for testing.
 */
internal class FakeAuthenticator(
    private val result: AppLockResult = Outcome.Success(Unit),
) : AppLockAuthenticator {
    var authenticateCallCount = 0
        private set

    override suspend fun authenticate(): AppLockResult {
        authenticateCallCount++
        return result
    }

    companion object {
        fun success(): FakeAuthenticator = FakeAuthenticator(Outcome.Success(Unit))
        fun failure(error: AppLockError): FakeAuthenticator = FakeAuthenticator(Outcome.Failure(error))
    }
}
