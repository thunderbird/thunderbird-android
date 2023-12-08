package com.fsck.k9.mail.server

import com.fsck.k9.mail.ServerSettings
import java.io.IOException
import java.security.cert.X509Certificate

/**
 * Result type for [ServerSettingsValidator].
 */
sealed interface ServerSettingsValidationResult {
    /**
     * The given [ServerSettings] were successfully used to connect to the server and log in.
     */
    object Success : ServerSettingsValidationResult

    /**
     * A network error occurred while checking the server settings.
     */
    data class NetworkError(val exception: IOException) : ServerSettingsValidationResult

    /**
     * A certificate error occurred while checking the server settings.
     */
    data class CertificateError(val certificateChain: List<X509Certificate>) : ServerSettingsValidationResult

    /**
     * There's a problem with the client certificate.
     */
    sealed interface ClientCertificateError : ServerSettingsValidationResult {
        /**
         * The client certificate couldn't be retrieved.
         */
        data object ClientCertificateRetrievalFailure : ClientCertificateError

        /**
         * The client certificate (or another one in the chain) has expired.
         */
        data object ClientCertificateExpired : ClientCertificateError
    }

    /**
     * Authentication failed while checking the server settings.
     */
    data class AuthenticationError(val serverMessage: String?) : ServerSettingsValidationResult

    /**
     * The server returned an error while checking the server settings.
     */
    data class ServerError(val serverMessage: String?) : ServerSettingsValidationResult

    /**
     * The server is missing a capability that is required by the current server settings.
     */
    data class MissingServerCapabilityError(val capabilityName: String) : ServerSettingsValidationResult

    /**
     * An unknown error occurred while checking the server settings.
     */
    data class UnknownError(val exception: Exception) : ServerSettingsValidationResult
}
