package app.k9mail.feature.account.edit.domain

import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import org.json.JSONObject

fun interface GmailAccountIdentityVerifier {
    suspend fun matches(accountEmail: String?, authorizationState: AuthorizationState): Boolean
}

class DefaultGmailAccountIdentityVerifier : GmailAccountIdentityVerifier {
    override suspend fun matches(accountEmail: String?, authorizationState: AuthorizationState): Boolean =
        withContext(Dispatchers.IO) {
            val expectedEmail = accountEmail?.normalizeEmail() ?: return@withContext false
            val accessToken = authorizationState.value
                ?.let { runCatching { AuthState.jsonDeserialize(it).accessToken }.getOrNull() }
                ?.takeIf(String::isNotBlank)
                ?: return@withContext false

            val connection = URL(GMAIL_PROFILE_URL).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECTION_TIMEOUT_MILLIS
                connection.readTimeout = CONNECTION_TIMEOUT_MILLIS
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext false
                }

                val profile = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                profile.optString("emailAddress").normalizeEmail() == expectedEmail
            } catch (_: Exception) {
                false
            } finally {
                connection.disconnect()
            }
        }

    private fun String.normalizeEmail(): String = trim().lowercase(Locale.ROOT)

    private companion object {
        const val GMAIL_PROFILE_URL = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
        const val CONNECTION_TIMEOUT_MILLIS = 15_000
    }
}
