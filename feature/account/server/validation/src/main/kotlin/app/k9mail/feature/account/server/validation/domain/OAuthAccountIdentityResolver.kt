package app.k9mail.feature.account.server.validation.domain

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.mail.ServerSettings
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.common.mail.ZohoMailServerSettings
import org.json.JSONArray
import org.json.JSONObject

data class OAuthAccountIdentity(
    val emailAddress: String? = null,
    val displayName: String? = null,
)

fun interface OAuthAccountIdentityResolver {
    suspend fun resolve(serverSettings: ServerSettings, authorizationState: AuthorizationState): OAuthAccountIdentity?
}

class DefaultOAuthAccountIdentityResolver : OAuthAccountIdentityResolver {
    override suspend fun resolve(
        serverSettings: ServerSettings,
        authorizationState: AuthorizationState,
    ): OAuthAccountIdentity? = withContext(Dispatchers.IO) {
        val authState = authorizationState.value
            ?.let { runCatching { AuthState.jsonDeserialize(it) }.getOrNull() }
            ?: return@withContext null
        val accessToken = authState.accessToken?.takeIf(String::isNotBlank) ?: return@withContext null

        runCatching {
            when {
                serverSettings.type == Protocols.GMAIL_API -> resolveGoogleIdentity(accessToken)

                Protocols.isGraphProtocol(serverSettings.type) -> resolveMicrosoftIdentity(accessToken)

                serverSettings.type == Protocols.ZOHO_MAIL_API -> resolveZohoIdentity(
                    authState = authState,
                    accessToken = accessToken,
                    expectedEmail = serverSettings.username,
                )

                else -> null
            }
        }.getOrNull()
    }

    private fun resolveGoogleIdentity(accessToken: String): OAuthAccountIdentity? {
        val userInfo = getJson(GOOGLE_USER_INFO_URL, "Bearer $accessToken")
        val gmailProfile = getJson(GMAIL_PROFILE_URL, "Bearer $accessToken")
        return googleIdentity(userInfo, gmailProfile)
    }

    private fun resolveMicrosoftIdentity(accessToken: String): OAuthAccountIdentity? {
        val userInfo = getJson(MICROSOFT_USER_INFO_URL, "Bearer $accessToken")
        return OAuthAccountIdentity(
            emailAddress = userInfo.optString("email").validatedEmailOrNull(),
            displayName = userInfo.optString("name").trim().takeIf(String::isNotEmpty),
        ).takeIf { it.emailAddress != null || it.displayName != null }
    }

    private fun resolveZohoIdentity(
        authState: AuthState,
        accessToken: String,
        expectedEmail: String,
    ): OAuthAccountIdentity? {
        val mailOrigin = authState.lastAuthorizationResponse
            ?.additionalParameters
            ?.get(ZOHO_ACCOUNTS_SERVER_PARAMETER)
            ?.toZohoAccountsServerHost()
            ?.let(ZOHO_ACCOUNTS_TO_MAIL_ORIGINS::get)
        val accounts = mailOrigin?.let { origin ->
            getJson("$origin/api/accounts", "Zoho-oauthtoken $accessToken").let { root ->
                root.optJSONArray("data")
                    ?: root.optJSONObject("data")?.optJSONArray("accounts")
                    ?: root.optJSONArray("accounts")
            }
        }
        val account = accounts?.firstObjectOrNull { it.matchesMailbox(expectedEmail.normalizeEmail()) }

        return account?.let {
            val email = it.primaryEmail()?.validatedEmailOrNull()
            val senderName = it.optString("displayName").trim()
                .ifEmpty { it.optString("accountDisplayName").trim() }
                .ifEmpty { it.sendMailDisplayName(email).orEmpty() }
            OAuthAccountIdentity(
                emailAddress = email,
                displayName = senderName.takeIf(String::isNotEmpty),
            )
        }?.takeIf { it.emailAddress != null || it.displayName != null }
    }

    private fun getJson(url: String, authorization: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECTION_TIMEOUT_MILLIS
            connection.readTimeout = CONNECTION_TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", authorization)
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == HttpURLConnection.HTTP_OK)
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.matchesMailbox(expectedEmail: String): Boolean {
        if (expectedEmail.isBlank()) return false
        val candidateEmails = buildList {
            listOf("primaryEmailAddress", "mailboxAddress", "incomingUserName").forEach { key ->
                optString(key).takeIf(String::isNotBlank)?.let(::add)
            }
            val addresses = optJSONArray("emailAddress")
            if (addresses != null) {
                for (index in 0 until addresses.length()) {
                    addresses.optJSONObject(index)?.optString("mailId")
                        ?.takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            }
        }
        return candidateEmails.any { it.normalizeEmail() == expectedEmail }
    }

    private fun JSONObject.primaryEmail(): String? =
        optString("primaryEmailAddress").takeIf(String::isNotBlank)
            ?: optString("mailboxAddress").takeIf(String::isNotBlank)
            ?: optString("incomingUserName").takeIf(String::isNotBlank)
            ?: optJSONArray("emailAddress")?.firstObjectOrNull { it.optBoolean("isPrimary") }
                ?.optString("mailId")?.takeIf(String::isNotBlank)
            ?: optJSONArray("emailAddress")?.firstObjectOrNull { true }
                ?.optString("mailId")?.takeIf(String::isNotBlank)

    private fun JSONObject.sendMailDisplayName(email: String?): String? =
        optJSONArray("sendMailDetails")?.let { sendMailDetails ->
            (0 until sendMailDetails.length()).asSequence()
                .mapNotNull(sendMailDetails::optJSONObject)
                .firstNotNullOfOrNull { detail ->
                    val fromAddress = detail.optString("fromAddress").normalizeEmail()
                    detail.optString("displayName").trim().takeIf { displayName ->
                        displayName.isNotEmpty() && (email == null || fromAddress == email.normalizeEmail())
                    }
                }
        }

    private fun JSONArray.firstObjectOrNull(predicate: (JSONObject) -> Boolean): JSONObject? {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            if (predicate(item)) return item
        }
        return null
    }

    private fun String.toZohoAccountsServerHost(): String? {
        val value = trim()
        if (value.isEmpty()) return null

        val host = runCatching { URI(value).host }.getOrNull()
            ?: value.substringBefore('/').substringBefore(':')
        return host.takeIf(String::isNotBlank)?.lowercase(Locale.ROOT)?.removeSuffix(".")
    }

    private fun String.validatedEmailOrNull(): String? {
        val email = trim()
        val separator = email.indexOf('@')
        return email.takeIf {
            separator > 0 && separator == email.lastIndexOf('@') &&
                separator < email.lastIndex && !email.any(Char::isWhitespace)
        }
    }

    private companion object {
        const val GOOGLE_USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo"
        const val GMAIL_PROFILE_URL = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
        const val MICROSOFT_USER_INFO_URL = "https://graph.microsoft.com/oidc/userinfo"
        const val ZOHO_ACCOUNTS_SERVER_PARAMETER = "accounts-server"
        const val CONNECTION_TIMEOUT_MILLIS = 10_000

        val ZOHO_ACCOUNTS_TO_MAIL_ORIGINS = mapOf(
            "accounts.zoho.com" to "https://mail.zoho.com",
            "accounts.zoho.eu" to "https://mail.zoho.eu",
            "accounts.zoho.in" to "https://mail.zoho.in",
            "accounts.zoho.com.au" to "https://mail.zoho.com.au",
            "accounts.zoho.jp" to "https://mail.zoho.jp",
            "accounts.zohocloud.ca" to "https://mail.zohocloud.ca",
            "accounts.zoho.com.cn" to "https://mail.zoho.com.cn",
            "accounts.zoho.ae" to "https://mail.zoho.ae",
            "accounts.zoho.sa" to "https://mail.zoho.sa",
        )
    }
}

private fun String.normalizeEmail(): String = trim().lowercase(Locale.ROOT)

internal fun googleIdentity(userInfo: JSONObject, gmailProfile: JSONObject): OAuthAccountIdentity? {
    val identity = OAuthAccountIdentity(
        emailAddress = gmailProfile.optString("emailAddress").validatedEmailOrNull(),
        displayName = userInfo.optString("name").trim().takeIf(String::isNotEmpty),
    )
    return identity.takeIf { it.emailAddress != null || it.displayName != null }
}

private fun String.validatedEmailOrNull(): String? {
    val email = trim()
    val separator = email.indexOf('@')
    return email.takeIf {
        separator > 0 && separator == email.lastIndexOf('@') &&
            separator < email.lastIndex && !email.any(Char::isWhitespace)
    }
}
