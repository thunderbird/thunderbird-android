package app.k9mail.feature.account.edit.domain

import android.util.Base64
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.nio.charset.StandardCharsets.UTF_8
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MicrosoftAccountIdentityVerifierTest {
    private val testSubject = DefaultMicrosoftAccountIdentityVerifier()

    @Test
    fun `same tenant and object id is accepted even when Microsoft email alias changed`() {
        val currentIdToken = idToken(tenantId = "tenant", objectId = "object", email = "old@example.com")
        val newIdToken = idToken(tenantId = "tenant", objectId = "object", email = "new@example.com")

        val result = testSubject.matchesIdTokens(
            accountEmail = "old@example.com",
            currentIdToken = currentIdToken,
            newIdToken = newIdToken,
            clientId = CLIENT_ID,
        )

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `different object id is rejected even when email matches`() {
        val currentIdToken = idToken(tenantId = "tenant", objectId = "original", email = EMAIL_ADDRESS)
        val newIdToken = idToken(tenantId = "tenant", objectId = "different", email = EMAIL_ADDRESS)

        val result = testSubject.matchesIdTokens(
            accountEmail = EMAIL_ADDRESS,
            currentIdToken = currentIdToken,
            newIdToken = newIdToken,
            clientId = CLIENT_ID,
        )

        assertThat(result).isEqualTo(false)
    }

    @Test
    fun `partial saved identity still requires available tenant and email claims to match`() {
        val currentIdToken = idToken(tenantId = "tenant", objectId = null, email = EMAIL_ADDRESS)
        val newIdToken = idToken(tenantId = "another-tenant", objectId = null, email = EMAIL_ADDRESS)

        val result = testSubject.matchesIdTokens(
            accountEmail = EMAIL_ADDRESS,
            currentIdToken = currentIdToken,
            newIdToken = newIdToken,
            clientId = CLIENT_ID,
        )

        assertThat(result).isEqualTo(false)
    }

    @Test
    fun `email fallback requires an exact case insensitive account email claim`() {
        val matchingIdToken = idToken(tenantId = null, objectId = null, email = EMAIL_ADDRESS.uppercase())
        val mismatchingIdToken = idToken(tenantId = null, objectId = null, email = "other@example.com")

        assertThat(
            testSubject.matchesIdTokens(
                accountEmail = EMAIL_ADDRESS,
                currentIdToken = null,
                newIdToken = matchingIdToken,
                clientId = CLIENT_ID,
            ),
        ).isEqualTo(true)
        assertThat(
            testSubject.matchesIdTokens(
                accountEmail = EMAIL_ADDRESS,
                currentIdToken = null,
                newIdToken = mismatchingIdToken,
                clientId = CLIENT_ID,
            ),
        ).isEqualTo(false)
    }

    @Test
    fun `token with a different client audience is rejected`() {
        val mismatchedAudienceToken = idToken(
            tenantId = "tenant",
            objectId = "object",
            email = EMAIL_ADDRESS,
            audience = "another-client",
        )

        val result = testSubject.matchesIdTokens(
            accountEmail = EMAIL_ADDRESS,
            currentIdToken = null,
            newIdToken = mismatchedAudienceToken,
            clientId = CLIENT_ID,
        )

        assertThat(result).isEqualTo(false)
    }

    private fun idToken(
        tenantId: String?,
        objectId: String?,
        email: String,
        audience: String = CLIENT_ID,
    ): String {
        val issuerTenantId = tenantId ?: "tenant"
        val claims = JSONObject()
            .put("iss", "https://login.microsoftonline.com/$issuerTenantId/v2.0")
            .put("aud", audience)
            .put("email", email)
        tenantId?.let { claims.put("tid", it) }
        objectId?.let { claims.put("oid", it) }

        val encodedClaims = Base64.encodeToString(
            claims.toString().toByteArray(UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        return "header.$encodedClaims.signature"
    }

    private companion object {
        const val CLIENT_ID = "microsoft-client-id"
        const val EMAIL_ADDRESS = "mailbox@example.com"
    }
}
