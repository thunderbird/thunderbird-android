package app.k9mail.feature.account.edit.domain

import android.util.Base64
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.MicrosoftAccountIdentityVerifier
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale
import net.openid.appauth.AuthState
import org.json.JSONArray
import org.json.JSONObject

class DefaultMicrosoftAccountIdentityVerifier : MicrosoftAccountIdentityVerifier {
    override fun matches(
        accountEmail: String?,
        currentAuthorizationState: AuthorizationState?,
        newAuthorizationState: AuthorizationState,
        clientId: String,
    ): Boolean {
        val currentIdToken = currentAuthorizationState?.value?.let(::readIdToken)
        val newIdToken = newAuthorizationState.value?.let(::readIdToken)
        return matchesIdTokens(accountEmail, currentIdToken, newIdToken, clientId)
    }

    internal fun matchesIdTokens(
        accountEmail: String?,
        currentIdToken: String?,
        newIdToken: String?,
        clientId: String,
    ): Boolean {
        val newClaims = newIdToken?.toMicrosoftClaims(clientId) ?: return false
        val currentClaims = currentIdToken?.toMicrosoftClaims(clientId)

        return when {
            currentClaims?.hasStableObjectIdentity == true ->
                newClaims.tenantId == currentClaims.tenantId &&
                    newClaims.objectId == currentClaims.objectId

            currentClaims != null -> {
                val tenantMatches = currentClaims.tenantId == null || newClaims.tenantId == currentClaims.tenantId
                val objectMatches = currentClaims.objectId == null || newClaims.objectId == currentClaims.objectId
                val emailMatches = currentClaims.emailClaims.any(newClaims.emailClaims::contains)
                tenantMatches && objectMatches && emailMatches
            }

            else -> accountEmail?.normalizeEmail()?.let(newClaims.emailClaims::contains) == true
        }
    }

    private fun readIdToken(serializedAuthorizationState: String): String? =
        runCatching { AuthState.jsonDeserialize(serializedAuthorizationState).idToken }.getOrNull()

    internal fun String.toMicrosoftClaims(clientId: String): MicrosoftClaims? {
        val tokenParts = split('.')
        if (tokenParts.size != JWT_PARTS_COUNT) return null

        return runCatching {
            val payload = String(
                Base64.decode(tokenParts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                UTF_8,
            )
            val claims = JSONObject(payload)
            val issuer = claims.optString("iss")
            val tenantId = claims.optString("tid").takeIf(String::isNotBlank)
            val isMicrosoftIssuer = issuer.startsWith("https://login.microsoftonline.com/") ||
                issuer.startsWith("https://sts.windows.net/")
            val issuerMatchesTenant = tenantId == null || issuer.contains("/$tenantId/", ignoreCase = true)

            if (clientId !in claims.audiences() || !isMicrosoftIssuer || !issuerMatchesTenant) {
                null
            } else {
                MicrosoftClaims(
                    tenantId = tenantId,
                    objectId = claims.optString("oid").takeIf(String::isNotBlank),
                    emailClaims = setOfNotNull(
                        claims.optString("email").normalizeEmailOrNull(),
                        claims.optString("preferred_username").normalizeEmailOrNull(),
                        claims.optString("upn").normalizeEmailOrNull(),
                    ),
                )
            }
        }.getOrNull()
    }

    private fun JSONObject.audiences(): Set<String> = when (val audience = opt("aud")) {
        is String -> setOf(audience)

        is JSONArray -> (0 until audience.length())
            .mapNotNull { index -> audience.optString(index).takeIf(String::isNotBlank) }
            .toSet()

        else -> emptySet()
    }

    private fun String.normalizeEmail(): String = trim().lowercase(Locale.ROOT)

    private fun String.normalizeEmailOrNull(): String? = normalizeEmail().takeIf(String::isNotBlank)

    private companion object {
        const val JWT_PARTS_COUNT = 3
    }
}

internal data class MicrosoftClaims(
    val tenantId: String?,
    val objectId: String?,
    val emailClaims: Set<String>,
) {
    val hasStableObjectIdentity: Boolean
        get() = tenantId != null && objectId != null
}
