package com.fsck.k9.mail.store.pop3

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException

class Pop3ServerSettingsValidator(
    private val trustedSocketFactory: TrustedSocketFactory,
) : ServerSettingsValidator {

    @Suppress("TooGenericExceptionCaught")
    override fun checkServerSettings(
        serverSettings: ServerSettings,
        authStateStorage: AuthStateStorage?,
    ): ServerSettingsValidationResult {
        val store = Pop3Store(serverSettings, trustedSocketFactory)

        return try {
            store.checkSettings()

            ServerSettingsValidationResult.Success
        } catch (e: AuthenticationFailedException) {
            ServerSettingsValidationResult.AuthenticationError(e.messageFromServer)
        } catch (e: CertificateValidationException) {
            ServerSettingsValidationResult.CertificateError(e.certChain.toList())
        } catch (e: Pop3ErrorResponse) {
            ServerSettingsValidationResult.ServerError(e.responseText)
        } catch (e: MessagingException) {
            val cause = e.cause
            if (cause is IOException) {
                ServerSettingsValidationResult.NetworkError(cause)
            } else {
                ServerSettingsValidationResult.UnknownError(e)
            }
        } catch (e: IOException) {
            ServerSettingsValidationResult.NetworkError(e)
        } catch (e: Exception) {
            ServerSettingsValidationResult.UnknownError(e)
        }
    }
}
