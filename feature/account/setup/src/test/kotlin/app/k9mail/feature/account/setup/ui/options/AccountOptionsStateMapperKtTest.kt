package app.k9mail.feature.account.setup.ui.options

import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.input.StringInputField
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class AccountOptionsStateMapperKtTest {

    @Test
    fun `should map state to account options`() {
        val state = AccountOptionsContract.State(
            accountName = StringInputField("accountName"),
            displayName = StringInputField("displayName"),
            emailSignature = StringInputField("emailSignature"),
            checkFrequency = EmailCheckFrequency.EVERY_2_HOURS,
            messageDisplayCount = EmailDisplayCount.MESSAGES_100,
            showNotification = true,
        )

        val result = state.toAccountOptions()

        assertThat(result).isEqualTo(
            AccountOptions(
                accountName = "accountName",
                displayName = "displayName",
                emailSignature = "emailSignature",
                checkFrequencyInMinutes = 120,
                messageDisplayCount = 100,
                showNotification = true,
            ),
        )
    }

    @Test
    fun `empty signature should map to null`() {
        val state = AccountOptionsContract.State(emailSignature = StringInputField(""))

        val result = state.toAccountOptions()

        assertThat(result.emailSignature).isNull()
    }
}
