package app.k9mail.feature.account.common.domain.entity

import assertk.assertThat
import assertk.assertions.isNull
import org.junit.Test

class AuthorizationStateTest {

    @Test
    fun `should default to null state`() {
        val authorizationState = AuthorizationState()

        assertThat(authorizationState.value).isNull()
    }
}
