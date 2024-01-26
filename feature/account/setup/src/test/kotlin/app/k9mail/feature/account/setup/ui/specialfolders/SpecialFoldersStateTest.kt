package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormState
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SpecialFoldersStateTest {

    @Test
    fun `should set default values`() {
        val state = State()

        assertThat(state).isEqualTo(
            State(
                formState = FormState(),
                isManualSetup = false,
                isSuccess = false,
                error = null,
                isLoading = true,
            ),
        )
    }

    @Test
    fun `should set default form values`() {
        val formState = FormState()

        assertThat(formState).isEqualTo(
            FormState(
                archiveSpecialFolderOptions = emptyList(),
                draftsSpecialFolderOptions = emptyList(),
                sentSpecialFolderOptions = emptyList(),
                spamSpecialFolderOptions = emptyList(),
                trashSpecialFolderOptions = emptyList(),

                selectedArchiveSpecialFolderOption = SpecialFolderOption.None(true),
                selectedDraftsSpecialFolderOption = SpecialFolderOption.None(true),
                selectedSentSpecialFolderOption = SpecialFolderOption.None(true),
                selectedSpamSpecialFolderOption = SpecialFolderOption.None(true),
                selectedTrashSpecialFolderOption = SpecialFolderOption.None(true),
            ),
        )
    }
}
