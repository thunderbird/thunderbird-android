package app.k9mail.feature.account.oauth.domain.usecase

import android.content.Intent
import app.k9mail.core.common.oauth.OAuthConfiguration
import app.k9mail.feature.account.oauth.domain.DomainContract
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationIntentResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import org.junit.Test

class GetOAuthRequestIntentTest {

    @Test
    fun `should return NotSupported when hostname has no oauth configuration`() = runTest {
        val testSubject = GetOAuthRequestIntent(
            repository = FakeAuthorizationRepository(),
            configurationProvider = { null },
        )
        val hostname = "hostname"
        val emailAddress = "emailAddress"

        val result = testSubject.execute(hostname, emailAddress)

        assertThat(result).isEqualTo(AuthorizationIntentResult.NotSupported)
    }

    @Test
    fun `should return Success when repository has intent`() = runTest {
        val intent = Intent()
        val repository = FakeAuthorizationRepository(intent)
        val testSubject = GetOAuthRequestIntent(
            repository = repository,
            configurationProvider = { oAuthConfiguration },
        )
        val hostname = "hostname"
        val emailAddress = "emailAddress"

        val result = testSubject.execute(hostname, emailAddress)

        assertThat(result).isEqualTo(
            AuthorizationIntentResult.Success(
                intent = intent,
            ),
        )
        assertThat(repository.recordedConfiguration).isEqualTo(oAuthConfiguration)
        assertThat(repository.recordedEmailAddress).isEqualTo(emailAddress)
    }

    private companion object {
        val oAuthConfiguration = OAuthConfiguration(
            clientId = "clientId",
            scopes = listOf("scope", "scope2"),
            authorizationEndpoint = "auth.example.com",
            tokenEndpoint = "token.example.com",
            redirectUri = "redirect.example.com",
        )
    }

    private class FakeAuthorizationRepository(
        private val intent: Intent = Intent(),
    ) : DomainContract.AuthorizationRepository {

        var recordedConfiguration: OAuthConfiguration? = null
        var recordedEmailAddress: String? = null
        override fun getAuthorizationRequestIntent(
            configuration: OAuthConfiguration,
            emailAddress: String,
        ): AuthorizationIntentResult {
            recordedConfiguration = configuration
            recordedEmailAddress = emailAddress

            return AuthorizationIntentResult.Success(intent)
        }

        override suspend fun getAuthorizationResponse(intent: Intent): AuthorizationResponse? {
            TODO("Not yet implemented")
        }

        override suspend fun getAuthorizationException(intent: Intent): AuthorizationException? {
            TODO("Not yet implemented")
        }

        override suspend fun getExchangeToken(
            authorizationState: AuthorizationState,
            response: AuthorizationResponse,
        ): AuthorizationResult {
            TODO("Not yet implemented")
        }
    }
}
