package app.k9mail.feature.account.oauth.domain.usecase

import android.content.Intent
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.oauth.domain.FakeAuthorizationRepository
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinishOAuthSignInTest {

    @Test
    fun `should return failure when intent has encoded exception`() = runTest {
        val intent = Intent()
        val exception = AuthorizationException(
            AuthorizationException.TYPE_GENERAL_ERROR,
            1,
            "error",
            "error_description",
            null,
            null,
        )
        val repository = FakeAuthorizationRepository(
            answerGetAuthorizationException = exception,
        )
        val testSubject = FinishOAuthSignIn(
            repository = repository,
        )

        val result = testSubject.execute(intent)

        assertThat(result).isEqualTo(AuthorizationResult.Failure(exception))
        assertThat(repository.recordedGetAuthorizationExceptionIntent).isEqualTo(intent)
    }

    @Test
    fun `should return canceled when intent has no response and no exception`() = runTest {
        val intent = Intent()
        val repository = FakeAuthorizationRepository(
            answerGetAuthorizationResponse = null,
            answerGetAuthorizationException = null,
        )
        val testSubject = FinishOAuthSignIn(
            repository = repository,
        )

        val result = testSubject.execute(intent)

        assertThat(result).isEqualTo(AuthorizationResult.Canceled)
        assertThat(repository.recordedGetAuthorizationResponseIntent).isEqualTo(intent)
        assertThat(repository.recordedGetAuthorizationExceptionIntent).isEqualTo(intent)
    }

    @Test
    fun `should return success when intent has response`() = runTest {
        val authorizationState = AuthorizationState()
        val intent = Intent()
        val response = mock<AuthorizationResponse>()
        val repository = FakeAuthorizationRepository(
            answerGetAuthorizationResponse = response,
            answerGetAuthorizationException = null,
            answerGetExchangeToken = AuthorizationResult.Success(authorizationState),
        )
        val testSubject = FinishOAuthSignIn(
            repository = repository,
        )

        val result = testSubject.execute(intent)

        assertThat(result).isEqualTo(AuthorizationResult.Success(authorizationState))
        assertThat(repository.recordedGetAuthorizationResponseIntent).isEqualTo(intent)
        assertThat(repository.recordedGetAuthorizationExceptionIntent).isEqualTo(intent)
        assertThat(repository.recordedGetExchangeTokenResponse).isEqualTo(response)
    }
}
