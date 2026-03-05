package net.thunderbird.feature.applock.api

import net.thunderbird.core.outcome.Outcome

/**
 * Type alias for the result of an authentication operation.
 *
 * Returns [Outcome.Success] with [Unit] on successful authentication,
 * or [Outcome.Failure] with an [AppLockError] on failure.
 */
typealias AppLockResult = Outcome<Unit, AppLockError>
