package app.k9mail.feature.account.oauth.domain.usecase

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class CheckIsGoogleSignInTest {

    private val testSubject = CheckIsGoogleSignIn()

    @Test
    fun `should return true when hostname ends with a google domain`() {
        val hostnames = listOf(
            "mail.gmail.com",
            "mail.googlemail.com",
            "mail.google.com",
        )

        for (hostname in hostnames) {
            assertThat(testSubject.execute(hostname)).isTrue()
        }
    }

    @Test
    fun `should return false when hostname does not end with a google domain`() {
        val hostnames = listOf(
            "mail.example.com",
            "mail.example.org",
            "mail.example.net",
        )

        for (hostname in hostnames) {
            assertThat(testSubject.execute(hostname)).isFalse()
        }
    }
}
