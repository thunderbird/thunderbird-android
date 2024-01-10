package app.k9mail.feature.account.common.domain.entity

import assertk.all
import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.prop
import org.junit.Test

class AccountStateTest {

    @Test
    fun `should default to null state`() {
        val accountState = AccountState()

        assertThat(accountState).all {
            prop(AccountState::emailAddress).isNull()
            prop(AccountState::incomingServerSettings).isNull()
            prop(AccountState::outgoingServerSettings).isNull()
            prop(AccountState::authorizationState).isNull()
            prop(AccountState::specialFolderSettings).isNull()
            prop(AccountState::displayOptions).isNull()
            prop(AccountState::syncOptions).isNull()
        }
    }
}
