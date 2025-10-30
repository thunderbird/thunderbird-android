package net.thunderbird.feature.account.server.settings.ui.common

import net.thunderbird.core.outcome.Outcome

/**
 * A functional interface for authenticating a user.
 */
fun interface Authenticator {
    suspend fun authenticate(): Outcome<Unit, String>
}
