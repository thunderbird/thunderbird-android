package net.thunderbird.feature.account.server.settings.ui.common

import net.thunderbird.core.outcome.Outcome

/**
 * A functional interface for authenticating a user.
 */
fun interface Authenticator {

    /**
     * Authenticates the user.
     *
     * @return An [Outcome] representing the result of the authentication process.
     */
    suspend fun authenticate(): Outcome<Unit, AuthenticationError>
}

/**
 *  Authentication errors that can occur during the authentication process.
 */
sealed interface AuthenticationError {
    /**
     * The user has not set up any authentication methods (e.g. screen lock, biometrics).
     */
    data object NotAvailable : AuthenticationError

    /**
     * The authentication failed.
     */
    data object Failed : AuthenticationError

    /**
     * An unknown error occurred, and authentication could not be started.
     */
    data object UnableToStart : AuthenticationError
}
