package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SyncOptionsStateMapperKtTest {

    @Test
    fun `should map state to account options`() {
        val state = SyncOptionsContract.State(
            checkFrequency = EmailCheckFrequency.EVERY_2_HOURS,
            messageDisplayCount = EmailDisplayCount.MESSAGES_100,
            showNotification = true,
        )

        val result = state.toAccountSyncOptions()

        assertThat(result).isEqualTo(
            AccountSyncOptions(
                checkFrequencyInMinutes = 120,
                messageDisplayCount = 100,
                showNotification = true,
            ),
        )
    }
}
