package app.k9mail.feature.account.edit.ui.server.settings.save

import assertk.assertThat
import assertk.assertions.isFalse
import org.junit.Test

class SaveOutgoingServerSettingsViewModelTest {

    @Test
    fun `should set is incoming to true`() {
        val testSubject = SaveOutgoingServerSettingsViewModel(
            accountUuid = "accountUuid",
            saveServerSettings = { _, _ -> },
        )

        assertThat(testSubject.isIncoming).isFalse()
    }
}
