package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SpecialFoldersFormStateMapperKtTest {

    @Test
    fun `should map folders to form state and assign selected folders`() = runTest {
        val specialOptions = SpecialFolderOptions(
            archiveSpecialFolderOptions = createFolderList(
                SpecialFolderOption.Special(
                    remoteFolder = createRemoteFolder("archive1"),
                    isAutomatic = true,
                ),
            ),
            draftsSpecialFolderOptions = createFolderList(
                SpecialFolderOption.Special(
                    remoteFolder = createRemoteFolder("drafts1"),
                    isAutomatic = true,
                ),
            ),
            sentSpecialFolderOptions = createFolderList(
                SpecialFolderOption.Special(
                    remoteFolder = createRemoteFolder("sent1"),
                    isAutomatic = true,
                ),
            ),
            spamSpecialFolderOptions = createFolderList(
                SpecialFolderOption.Special(
                    remoteFolder = createRemoteFolder("spam1"),
                    isAutomatic = true,
                ),
            ),
            trashSpecialFolderOptions = createFolderList(
                SpecialFolderOption.Special(
                    remoteFolder = createRemoteFolder("trash1"),
                    isAutomatic = true,
                ),
            ),
        )

        val result = specialOptions.toFormState()

        assertThat(result).isEqualTo(
            SpecialFoldersContract.FormState(
                archiveSpecialFolderOptions = specialOptions.archiveSpecialFolderOptions,
                draftsSpecialFolderOptions = specialOptions.draftsSpecialFolderOptions,
                sentSpecialFolderOptions = specialOptions.sentSpecialFolderOptions,
                spamSpecialFolderOptions = specialOptions.spamSpecialFolderOptions,
                trashSpecialFolderOptions = specialOptions.trashSpecialFolderOptions,

                selectedArchiveSpecialFolderOption = specialOptions.archiveSpecialFolderOptions.first(),
                selectedDraftsSpecialFolderOption = specialOptions.draftsSpecialFolderOptions.first(),
                selectedSentSpecialFolderOption = specialOptions.sentSpecialFolderOptions.first(),
                selectedSpamSpecialFolderOption = specialOptions.spamSpecialFolderOptions.first(),
                selectedTrashSpecialFolderOption = specialOptions.trashSpecialFolderOptions.first(),
            ),
        )
    }

    @Test
    fun `should map folders to form state and not assign selected folders when there is none automatic`() {
        val specialFolderOptions = SpecialFolderOptions(
            archiveSpecialFolderOptions = createFolderList(SpecialFolderOption.None(isAutomatic = true)),
            draftsSpecialFolderOptions = createFolderList(SpecialFolderOption.None(isAutomatic = true)),
            sentSpecialFolderOptions = createFolderList(SpecialFolderOption.None(isAutomatic = true)),
            spamSpecialFolderOptions = createFolderList(SpecialFolderOption.None(isAutomatic = true)),
            trashSpecialFolderOptions = createFolderList(SpecialFolderOption.None(isAutomatic = true)),
        )

        val result = specialFolderOptions.toFormState()

        assertThat(result).isEqualTo(
            SpecialFoldersContract.FormState(
                archiveSpecialFolderOptions = specialFolderOptions.archiveSpecialFolderOptions,
                draftsSpecialFolderOptions = specialFolderOptions.draftsSpecialFolderOptions,
                sentSpecialFolderOptions = specialFolderOptions.sentSpecialFolderOptions,
                spamSpecialFolderOptions = specialFolderOptions.spamSpecialFolderOptions,
                trashSpecialFolderOptions = specialFolderOptions.trashSpecialFolderOptions,

                selectedArchiveSpecialFolderOption = specialFolderOptions.archiveSpecialFolderOptions.first(),
                selectedDraftsSpecialFolderOption = specialFolderOptions.draftsSpecialFolderOptions.first(),
                selectedSentSpecialFolderOption = specialFolderOptions.sentSpecialFolderOptions.first(),
                selectedSpamSpecialFolderOption = specialFolderOptions.spamSpecialFolderOptions.first(),
                selectedTrashSpecialFolderOption = specialFolderOptions.trashSpecialFolderOptions.first(),
            ),
        )
    }

    private companion object {
        fun createRemoteFolder(name: String): RemoteFolder {
            return RemoteFolder(
                serverId = FolderServerId(name),
                displayName = name,
                type = FolderType.REGULAR,
            )
        }

        fun createFolderList(automaticSpecialFolderOption: SpecialFolderOption): List<SpecialFolderOption> {
            return listOf(
                automaticSpecialFolderOption,
                SpecialFolderOption.None(),
                SpecialFolderOption.Regular(createRemoteFolder("regular1")),
            )
        }
    }
}
