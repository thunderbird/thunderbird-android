package net.thunderbird.feature.thundermail.internal.common.domain

import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import java.util.Base64
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import net.thunderbird.core.outcome.Outcome
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateAccountStateUseCaseTest {

    @Test
    fun `invoke should fail with AuthorizationStateMissing when authorization state has no value`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)

        // Act
        val result = testSubject(
            authorizationState = AuthorizationState(value = null),
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<CreateAccountStateUseCase.Failure>>()
            .prop(Outcome.Failure<CreateAccountStateUseCase.Failure>::error)
            .isSameInstanceAs(CreateAccountStateUseCase.Failure.AuthorizationStateMissing)
    }

    @Test
    fun `invoke should fail with InvalidAuthorizationState when value is malformed JSON`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)

        // Act
        val result = testSubject(
            authorizationState = AuthorizationState(value = "not-valid-json"),
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<CreateAccountStateUseCase.Failure>>()
            .prop(Outcome.Failure<CreateAccountStateUseCase.Failure>::error)
            .isInstanceOf<CreateAccountStateUseCase.Failure.InvalidAuthorizationState>()
    }

    @Test
    fun `invoke should fail with IdTokenMissing when auth state has no id token`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = AuthorizationState(value = AuthState().jsonSerializeString())

        // Act
        val result = testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<CreateAccountStateUseCase.Failure>>()
            .prop(Outcome.Failure<CreateAccountStateUseCase.Failure>::error)
            .isSameInstanceAs(CreateAccountStateUseCase.Failure.IdTokenMissing)
    }

    @Test
    fun `invoke should fail with MissingEmail when id token has no preferred_username or email`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(preferredUsername = null, email = null),
        )

        // Act
        val result = testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(result).isInstanceOf<Outcome.Failure<CreateAccountStateUseCase.Failure>>()
            .prop(Outcome.Failure<CreateAccountStateUseCase.Failure>::error)
            .isInstanceOf<CreateAccountStateUseCase.Failure.MissingEmail>()
    }

    @Test
    fun `invoke should succeed using preferred_username for email address`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(
                preferredUsername = "user@thundermail.test",
                email = "fallback@thundermail.test",
                name = "Test User",
            ),
        )

        // Act
        val result = testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
        assertThat(repository.getState().emailAddress).isEqualTo("user@thundermail.test")
    }

    @Test
    fun `invoke should fall back to email claim when preferred_username absent`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(
                preferredUsername = null,
                email = "email-only@thundermail.test",
                name = "Test User",
            ),
        )

        // Act
        val result = testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(result).isInstanceOf<Outcome.Success<Unit>>()
        assertThat(repository.getState().emailAddress).isEqualTo("email-only@thundermail.test")
    }

    @Test
    fun `invoke should persist server settings and authorization state on success`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(preferredUsername = "user@thundermail.test"),
        )

        // Act
        testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        val state = repository.getState()
        assertThat(state.incomingServerSettings).isEqualTo(INCOMING_SERVER_SETTINGS)
        assertThat(state.outgoingServerSettings).isEqualTo(OUTGOING_SERVER_SETTINGS)
        assertThat(state.authorizationState).isEqualTo(authorizationState)
    }

    @Test
    fun `invoke should set display options with name claim when present`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(
                preferredUsername = "user@thundermail.test",
                name = "Ada Lovelace",
                givenName = "Ignored",
            ),
        )

        // Act
        testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(repository.getState().displayOptions).isNotNull().isEqualTo(
            AccountDisplayOptions(
                accountName = "user@thundermail.test",
                displayName = "Ada Lovelace",
                emailSignature = null,
            ),
        )
    }

    @Test
    fun `invoke should fall back to given_name for displayName when name absent`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(
                preferredUsername = "user@thundermail.test",
                name = null,
                givenName = "Ada",
            ),
        )

        // Act
        testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(repository.getState().displayOptions)
            .isNotNull()
            .prop(AccountDisplayOptions::displayName)
            .isEqualTo("Ada")
    }

    @Test
    fun `invoke should leave display options null when name and given_name absent`() {
        // Arrange
        val repository = InMemoryAccountStateRepository()
        val testSubject = CreateAccountStateUseCase(repository)
        val authorizationState = authorizationStateWith(
            buildIdTokenClaims(
                preferredUsername = "user@thundermail.test",
                name = null,
                givenName = null,
            ),
        )

        // Act
        testSubject(
            authorizationState = authorizationState,
            incomingServerSettings = INCOMING_SERVER_SETTINGS,
            outgoingServerSettings = OUTGOING_SERVER_SETTINGS,
        )

        // Assert
        assertThat(repository.getState().displayOptions).isNull()
    }

    private fun authorizationStateWith(idToken: String): AuthorizationState {
        val configuration = AuthorizationServiceConfiguration(
            android.net.Uri.parse("https://example.com/authorize"),
            android.net.Uri.parse("https://example.com/token"),
        )
        val tokenRequest = TokenRequest.Builder(configuration, "test-client")
            .setGrantType(TokenRequest.GRANT_TYPE_PASSWORD)
            .build()
        val tokenResponse = TokenResponse.Builder(tokenRequest)
            .setAccessToken("access-token")
            .setIdToken(idToken)
            .build()
        val authState = AuthState().apply { update(tokenResponse, null) }
        return AuthorizationState(value = authState.jsonSerializeString())
    }

    private fun buildIdTokenClaims(
        preferredUsername: String? = null,
        email: String? = null,
        name: String? = null,
        givenName: String? = null,
    ): String {
        val header = JSONObject().apply {
            put("alg", "none")
            put("typ", "JWT")
        }
        val claims = JSONObject().apply {
            put("iss", "https://example.com")
            put("sub", "subject")
            put("aud", "test-client")
            put("exp", 9_999_999_999L)
            put("iat", 1_000_000_000L)
            preferredUsername?.let { put("preferred_username", it) }
            email?.let { put("email", it) }
            name?.let { put("name", it) }
            givenName?.let { put("given_name", it) }
        }
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val headerSegment = encoder.encodeToString(header.toString().toByteArray())
        val payloadSegment = encoder.encodeToString(claims.toString().toByteArray())
        return "$headerSegment.$payloadSegment."
    }

    private companion object {
        val INCOMING_SERVER_SETTINGS = ServerSettings(
            type = "imap",
            host = "imap.thundermail.test",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = "user@thundermail.test",
            password = null,
            clientCertificateAlias = null,
        )

        val OUTGOING_SERVER_SETTINGS = ServerSettings(
            type = "smtp",
            host = "smtp.thundermail.test",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.XOAUTH2,
            username = "user@thundermail.test",
            password = null,
            clientCertificateAlias = null,
        )
    }
}
