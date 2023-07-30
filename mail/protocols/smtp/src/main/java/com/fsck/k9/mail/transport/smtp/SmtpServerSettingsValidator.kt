package com.fsck.k9.mail.transport.smtp

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidator
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException

class SmtpServerSettingsValidator(
    private val trustedSocketFactory: TrustedSocketFactory,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory?,
) : ServerSettingsValidator {

    @Suppress("TooGenericExceptionCaught")
    override fun checkServerSettings(
        serverSettings: ServerSettings,
        authStateStorage: AuthStateStorage?,
    ): ServerSettingsValidationResult {
        val oAuth2TokenProvider = createOAuth2TokenProviderOrNull(authStateStorage)
        val smtpTransport = SmtpTransport(serverSettings, trustedSocketFactory, oAuth2TokenProvider)

        return try {
            smtpTransport.checkSettings()

            ServerSettingsValidationResult.Success
        } catch (e: AuthenticationFailedException) {
            ServerSettingsValidationResult.AuthenticationError(e.messageFromServer)
        } catch (e: CertificateValidationException) {
            ServerSettingsValidationResult.CertificateError(e.certChain.toList())
        } catch (e: NegativeSmtpReplyException) {
            ServerSettingsValidationResult.ServerError(e.replyText)
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

    private fun createOAuth2TokenProviderOrNull(authStateStorage: AuthStateStorage?): OAuth2TokenProvider? {
        return authStateStorage?.let {
            oAuth2TokenProviderFactory?.create(it)
        }
    }
}
