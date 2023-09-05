package app.k9mail.feature.account.setup.ui.options

import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class AccountOptionsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                accountName = StringInputField(),
                displayName = StringInputField(),
                emailSignature = StringInputField(),
                checkFrequency = EmailCheckFrequency.DEFAULT,
                messageDisplayCount = EmailDisplayCount.DEFAULT,
                showNotification = false,
            ),
        )
    }
}
