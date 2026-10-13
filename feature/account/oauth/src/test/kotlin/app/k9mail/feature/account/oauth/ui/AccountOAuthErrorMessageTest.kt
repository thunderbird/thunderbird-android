package app.k9mail.feature.account.oauth.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.openid.appauth.AuthorizationException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountOAuthErrorMessageTest {
    @Test
    fun `OAuth error code is shown when description is missing`() {
        val exception = AuthorizationException(
            AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR,
            1,
            "access_denied",
            null,
            null,
            null,
        )

        assertThat(exception.toOAuthErrorMessage()).isEqualTo("access_denied")
    }

    @Test
    fun `AppAuth type and code are shown when both error and description are missing`() {
        val exception = AuthorizationException(
            AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR,
            1,
            null,
            null,
            null,
            null,
        )

        assertThat(exception.toOAuthErrorMessage()).isEqualTo("OAuth error (type 1, code 1)")
    }

    @Test
    fun `ordinary exception message remains visible`() {
        assertThat(IllegalStateException("response could not be parsed").toOAuthErrorMessage())
            .isEqualTo("response could not be parsed")
    }

    @Test
    fun `ordinary exception without message uses its type instead of null`() {
        assertThat(IllegalStateException().toOAuthErrorMessage()).isEqualTo("IllegalStateException")
    }
}
