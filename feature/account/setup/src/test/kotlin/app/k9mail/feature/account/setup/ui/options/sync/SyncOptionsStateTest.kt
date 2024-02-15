package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SyncOptionsStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                checkFrequency = EmailCheckFrequency.DEFAULT,
                messageDisplayCount = EmailDisplayCount.DEFAULT,
                showNotification = true,
            ),
        )
    }
}
