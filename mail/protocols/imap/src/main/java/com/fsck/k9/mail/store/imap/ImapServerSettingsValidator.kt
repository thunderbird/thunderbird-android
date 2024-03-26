package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.ClientCertificateError.CertificateExpired
import com.fsck.k9.mail.ClientCertificateError.RetrievalFailure
import com.fsck.k9.mail.ClientCertificateException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.MissingCapabilityException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import com.fsck.k9.mail.server.ServerSettingsValidationResult.ClientCertificateError
import com.fsck.k9.mail.server.ServerSettingsValidator
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.IOException

class ImapServerSettingsValidator(
    private val trustedSocketFactory: TrustedSocketFactory,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory?,
    private val clientIdAppName: String,
    private val clientIdAppVersion: String,
) : ServerSettingsValidator {

    @Suppress("TooGenericExceptionCaught")
    override fun checkServerSettings(
        serverSettings: ServerSettings,
        authStateStorage: AuthStateStorage?,
    ): ServerSettingsValidationResult {
        val config = object : ImapStoreConfig {
            override val logLabel = "check"
            override fun isSubscribedFoldersOnly() = false
            override fun isExpungeImmediately() = false
            override fun clientId() = ImapClientId(appName = clientIdAppName, appVersion = clientIdAppVersion)
        }
        val oAuth2TokenProvider = createOAuth2TokenProviderOrNull(authStateStorage)
        val store = RealImapStore(serverSettings, config, trustedSocketFactory, oAuth2TokenProvider)

        return try {
            store.checkSettings()

            ServerSettingsValidationResult.Success
        } catch (e: AuthenticationFailedException) {
            ServerSettingsValidationResult.AuthenticationError(e.messageFromServer)
        } catch (e: CertificateValidationException) {
            ServerSettingsValidationResult.CertificateError(e.certificateChain)
        } catch (e: NegativeImapResponseException) {
            ServerSettingsValidationResult.ServerError(e.responseText)
        } catch (e: MissingCapabilityException) {
            ServerSettingsValidationResult.MissingServerCapabilityError(e.capabilityName)
        } catch (e: ClientCertificateException) {
            when (e.error) {
                RetrievalFailure -> ClientCertificateError.ClientCertificateRetrievalFailure
                CertificateExpired -> ClientCertificateError.ClientCertificateExpired
            }
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
