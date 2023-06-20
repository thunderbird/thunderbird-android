package app.k9mail.core.common.oauth

import assertk.assertFailure
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import org.junit.Test

class OAuthProviderSettingsTest {

    @Test
    fun `should succeed with all arguments set`() {
        OAuthProviderSettings(
            applicationId = "test",
            clientIds = ALL_CLIENT_IDS,
            redirectUriIds = ALL_REDIRECT_URI_IDS,
        )
    }

    @Test
    fun `should fail with empty application id`() {
        assertFailure {
            OAuthProviderSettings(
                applicationId = "",
                clientIds = emptyMap(),
                redirectUriIds = emptyMap(),
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Application id must be set")
    }

    @Test
    fun `should fail with blank application id`() {
        assertFailure {
            OAuthProviderSettings(
                applicationId = "  ",
                clientIds = emptyMap(),
                redirectUriIds = emptyMap(),
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Application id must be set")
    }

    @Test
    fun `should fail for empty clientIds`() {
        assertFailure {
            OAuthProviderSettings(
                applicationId = "test",
                clientIds = emptyMap(),
                redirectUriIds = emptyMap(),
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Client ids must be set")
    }

    @Test
    fun `should fail with incomplete clientIds`() {
        assertFailure {
            OAuthProviderSettings(
                applicationId = "test",
                clientIds = (OAuthProvider.values().toList() - OAuthProvider.GMAIL).associateWith { "test" },
                redirectUriIds = emptyMap(),
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Client id for GMAIL must be set")
    }

    @Test
    fun `should fail for empty redirectUriIds`() {
        assertFailure {
            OAuthProviderSettings(
                applicationId = "test",
                clientIds = ALL_CLIENT_IDS,
                redirectUriIds = emptyMap(),
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Redirect URI ids must be set")
    }

    @Test
    fun `should fail for empty microsoft redirectUriId`() {
        assertFailure {
            OAuthProviderSettings(
                applicationId = "test",
                clientIds = ALL_CLIENT_IDS,
                redirectUriIds = mapOf(OAuthProvider.GMAIL to "test"),
            )
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Microsoft redirect URI id must be set")
    }

    companion object {
        private val ALL_CLIENT_IDS = OAuthProvider.values().associateWith { "test" }
        private val ALL_REDIRECT_URI_IDS = OAuthProvider.values().associateWith { "test" }
    }
}
