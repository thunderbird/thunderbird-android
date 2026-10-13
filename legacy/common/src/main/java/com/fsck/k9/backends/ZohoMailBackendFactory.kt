package com.fsck.k9.backends

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.zohomail.ZohoMailAuthorizationRequiredBackend
import com.fsck.k9.backend.zohomail.ZohoMailBackend
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory
import java.net.URI
import java.util.Locale
import net.openid.appauth.AuthState
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId

interface ZohoMailBackendFactory : BackendFactory

class DefaultZohoMailBackendFactory(
    private val accountManager: LegacyAccountManager,
    private val backendStorageFactory: BackendStorageFactory,
    private val oAuth2TokenProviderFactory: OAuth2TokenProviderFactory,
    private val logger: Logger,
) : ZohoMailBackendFactory {
    override fun createBackend(accountId: AccountId): Backend {
        val account = accountManager.getById(accountId)
            ?: error("Account not found: $accountId")
        val incomingSettings = account.incomingServerSettings
        require(incomingSettings.type == Protocols.ZOHO_MAIL_API) {
            "Expected a Zoho Mail REST account, got ${incomingSettings.type}"
        }
        require(incomingSettings.authenticationType == AuthType.XOAUTH2) {
            "Zoho Mail REST accounts require OAuth2 authentication"
        }

        val mailboxAddress = incomingSettings.username.trim()
        require(mailboxAddress.isNotBlank()) { "Zoho Mail account is missing its mailbox identity" }

        val authStateStorage = AccountAuthStateStorage(accountManager, accountId)
        val serializedAuthState = authStateStorage.getAuthorizationState()
        val authState = serializedAuthState?.let { serialized ->
            try {
                AuthState.jsonDeserialize(serialized)
            } catch (_: Exception) {
                null
            }
        }
        val verifiedMailApiOrigin = authState
            ?.takeIf { it.accessToken != null || it.refreshToken != null }
            ?.let { state ->
                try {
                    resolveVerifiedMailApiOrigin(state)
                } catch (_: Exception) {
                    null
                }
            }
        if (authState == null || verifiedMailApiOrigin == null) {
            logger.warn(TAG) { "Zoho Mail OAuth authorization is incomplete; sign-in is required" }
            return ZohoMailAuthorizationRequiredBackend
        }
        val storage = backendStorageFactory.createBackendStorage(accountId)
        storage.setExtraString(ZohoMailServerSettings.EXTRA_API_ORIGIN, verifiedMailApiOrigin)

        val tokenProvider = oAuth2TokenProviderFactory.create(authStateStorage)
        val incomingOnly = incomingSettings.extra[ZohoMailServerSettings.EXTRA_INCOMING_ONLY].toBoolean()
        return ZohoMailBackend(storage, tokenProvider, logger, verifiedMailApiOrigin, mailboxAddress, incomingOnly)
    }

    private fun resolveVerifiedMailApiOrigin(authState: AuthState): String {
        val authorizationParameters = authState.lastAuthorizationResponse?.additionalParameters.orEmpty()
        val tokenParameters = authState.lastTokenResponse?.additionalParameters.orEmpty()
        val location = authorizationParameters["location"]?.lowercase(Locale.ROOT)?.trim()
        val origins = buildList {
            REGIONAL_MAIL_API_ORIGINS[location]?.let(::add)
            mapOfficialHostToMailOrigin(tokenParameters["api_domain"])?.let(::add)
            mapOfficialHostToMailOrigin(authorizationParameters["accounts-server"])?.let(::add)
        }.distinct()

        require(origins.size <= 1) { "Zoho OAuth regional response fields did not agree on one Mail API origin" }
        return origins.singleOrNull()
            ?: error("Zoho OAuth response did not contain a recognized regional Mail API location")
    }

    private fun mapOfficialHostToMailOrigin(value: String?): String? {
        return value?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { runCatching { URI(it).host?.lowercase(Locale.ROOT) }.getOrNull() }
            ?.let { OFFICIAL_HOST_ORIGINS[it] }
    }

    private companion object {
        const val TAG = "ZohoMailBackendFactory"
        val REGIONAL_MAIL_API_ORIGINS = mapOf(
            "us" to "https://mail.zoho.com",
            "eu" to "https://mail.zoho.eu",
            "in" to "https://mail.zoho.in",
            "au" to "https://mail.zoho.com.au",
            "jp" to "https://mail.zoho.jp",
            "ca" to "https://mail.zohocloud.ca",
            "cn" to "https://mail.zoho.com.cn",
            "ae" to "https://mail.zoho.ae",
            "sa" to "https://mail.zoho.sa",
        )
        val OFFICIAL_HOST_ORIGINS = mapOf(
            "mail.zoho.com" to "https://mail.zoho.com",
            "www.zohoapis.com" to "https://mail.zoho.com",
            "zohoapis.com" to "https://mail.zoho.com",
            "accounts.zoho.com" to "https://mail.zoho.com",
            "mail.zoho.eu" to "https://mail.zoho.eu",
            "www.zohoapis.eu" to "https://mail.zoho.eu",
            "zohoapis.eu" to "https://mail.zoho.eu",
            "accounts.zoho.eu" to "https://mail.zoho.eu",
            "mail.zoho.in" to "https://mail.zoho.in",
            "www.zohoapis.in" to "https://mail.zoho.in",
            "zohoapis.in" to "https://mail.zoho.in",
            "accounts.zoho.in" to "https://mail.zoho.in",
            "mail.zoho.com.au" to "https://mail.zoho.com.au",
            "www.zohoapis.com.au" to "https://mail.zoho.com.au",
            "accounts.zoho.com.au" to "https://mail.zoho.com.au",
            "mail.zoho.jp" to "https://mail.zoho.jp",
            "www.zohoapis.jp" to "https://mail.zoho.jp",
            "accounts.zoho.jp" to "https://mail.zoho.jp",
            "mail.zohocloud.ca" to "https://mail.zohocloud.ca",
            "www.zohoapis.ca" to "https://mail.zohocloud.ca",
            "accounts.zohocloud.ca" to "https://mail.zohocloud.ca",
            "mail.zoho.com.cn" to "https://mail.zoho.com.cn",
            "www.zohoapis.com.cn" to "https://mail.zoho.com.cn",
            "accounts.zoho.com.cn" to "https://mail.zoho.com.cn",
            "mail.zoho.ae" to "https://mail.zoho.ae",
            "www.zohoapis.ae" to "https://mail.zoho.ae",
            "accounts.zoho.ae" to "https://mail.zoho.ae",
            "mail.zoho.sa" to "https://mail.zoho.sa",
            "www.zohoapis.sa" to "https://mail.zoho.sa",
            "accounts.zoho.sa" to "https://mail.zoho.sa",
        )
    }
}
