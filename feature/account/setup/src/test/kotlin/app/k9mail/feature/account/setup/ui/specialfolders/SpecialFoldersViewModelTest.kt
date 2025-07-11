package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.core.ui.compose.testing.mvi.assertThatAndEffectTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.assertThatAndStateTurbineConsumed
import app.k9mail.core.ui.compose.testing.mvi.runMviTest
import app.k9mail.core.ui.compose.testing.mvi.turbinesWithInitialStateCheck
import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Effect
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormEvent
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormState
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.State
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.folders.FolderFetcherException
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder
import kotlinx.coroutines.delay
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class SpecialFoldersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should load folders, validate and save successfully when LoadSpecialFolders event received and setup valid`() =
        runMviTest {
            val accountStateRepository = InMemoryAccountStateRepository()
            val initialState = State(
                isLoading = true,
            )
            val testSubject = createTestSubject(
                formUiModel = FakeSpecialFoldersFormUiModel(),
                validateSpecialFolderOptions = { ValidationResult.Success },
                accountStateRepository = accountStateRepository,
                initialState = initialState,
            )
            val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

            testSubject.event(Event.LoadSpecialFolderOptions)

            val validatedState = initialState.copy(
                isLoading = false,
                isSuccess = true,
                formState = FormState(
                    archiveSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.archiveSpecialFolderOptions,
                    draftsSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.draftsSpecialFolderOptions,
                    sentSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.sentSpecialFolderOptions,
                    spamSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.spamSpecialFolderOptions,
                    trashSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.trashSpecialFolderOptions,

                    selectedArchiveSpecialFolderOption = SPECIAL_FOLDER_ARCHIVE.copy(isAutomatic = true),
                    selectedDraftsSpecialFolderOption = SPECIAL_FOLDER_DRAFTS.copy(isAutomatic = true),
                    selectedSentSpecialFolderOption = SPECIAL_FOLDER_SENT.copy(isAutomatic = true),
                    selectedSpamSpecialFolderOption = SPECIAL_FOLDER_SPAM.copy(isAutomatic = true),
                    selectedTrashSpecialFolderOption = SPECIAL_FOLDER_TRASH.copy(isAutomatic = true),
                ),
            )

            turbines.assertThatAndStateTurbineConsumed {
                isEqualTo(validatedState)
            }

            turbines.assertThatAndEffectTurbineConsumed {
                isEqualTo(Effect.NavigateNext(false))
            }

            assertThat(accountStateRepository.getState()).isEqualTo(
                AccountState(
                    specialFolderSettings = SpecialFolderSettings(
                        archiveSpecialFolderOption = SPECIAL_FOLDER_ARCHIVE.copy(isAutomatic = true),
                        draftsSpecialFolderOption = SPECIAL_FOLDER_DRAFTS.copy(isAutomatic = true),
                        sentSpecialFolderOption = SPECIAL_FOLDER_SENT.copy(isAutomatic = true),
                        spamSpecialFolderOption = SPECIAL_FOLDER_SPAM.copy(isAutomatic = true),
                        trashSpecialFolderOption = SPECIAL_FOLDER_TRASH.copy(isAutomatic = true),
                    ),
                ),
            )
        }

    @Test
    fun `should load folders and validate unsuccessful when LoadSpecialFolders event received`() = runMviTest {
        val accountStateRepository = InMemoryAccountStateRepository()
        val initialState = State(
            isLoading = true,
        )
        val testSubject = createTestSubject(
            formUiModel = FakeSpecialFoldersFormUiModel(),
            validateSpecialFolderOptions = { ValidationResult.Failure(TestValidationError) },
            accountStateRepository = accountStateRepository,
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.LoadSpecialFolderOptions)

        val unvalidatedState = initialState.copy(
            isManualSetup = true,
            isLoading = false,
            isSuccess = false,
            formState = FormState(
                archiveSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.archiveSpecialFolderOptions,
                draftsSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.draftsSpecialFolderOptions,
                sentSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.sentSpecialFolderOptions,
                spamSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.spamSpecialFolderOptions,
                trashSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.trashSpecialFolderOptions,

                selectedArchiveSpecialFolderOption = SPECIAL_FOLDER_ARCHIVE.copy(isAutomatic = true),
                selectedDraftsSpecialFolderOption = SPECIAL_FOLDER_DRAFTS.copy(isAutomatic = true),
                selectedSentSpecialFolderOption = SPECIAL_FOLDER_SENT.copy(isAutomatic = true),
                selectedSpamSpecialFolderOption = SPECIAL_FOLDER_SPAM.copy(isAutomatic = true),
                selectedTrashSpecialFolderOption = SPECIAL_FOLDER_TRASH.copy(isAutomatic = true),
            ),
        )

        turbines.assertThatAndStateTurbineConsumed {
            isEqualTo(unvalidatedState)
        }

        turbines.effectTurbine.ensureAllEventsConsumed()

        assertThat(accountStateRepository.getState()).isEqualTo(AccountState())
    }

    @Test
    fun `should change to error state when LoadSpecialFolders fails with loading folder failure`() = runMviTest {
        val initialState = State(
            isLoading = true,
        )
        val testSubject = createTestSubject(
            formUiModel = FakeSpecialFoldersFormUiModel(),
            getSpecialFolderOptions = {
                throw FolderFetcherException(IllegalStateException(), messageFromServer = "Failed to load folders")
            },
            initialState = initialState,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.LoadSpecialFolderOptions)

        turbines.assertThatAndStateTurbineConsumed {
            isEqualTo(
                State(
                    isLoading = false,
                    isSuccess = false,
                    error = SpecialFoldersContract.Failure.LoadFoldersFailed(
                        "Failed to load folders",
                    ),
                ),
            )
        }
    }

    @Test
    fun `should delegate form events to form view model`() = runMviTest {
        val formUiModel = FakeSpecialFoldersFormUiModel()
        val testSubject = createTestSubject(
            formUiModel = formUiModel,
        )

        testSubject.event(FormEvent.ArchiveFolderChanged(SPECIAL_FOLDER_ARCHIVE))
        testSubject.event(FormEvent.DraftsFolderChanged(SPECIAL_FOLDER_DRAFTS))
        testSubject.event(FormEvent.SentFolderChanged(SPECIAL_FOLDER_SENT))
        testSubject.event(FormEvent.SpamFolderChanged(SPECIAL_FOLDER_SPAM))
        testSubject.event(FormEvent.TrashFolderChanged(SPECIAL_FOLDER_TRASH))

        assertThat(formUiModel.events).containsExactly(
            FormEvent.ArchiveFolderChanged(SPECIAL_FOLDER_ARCHIVE),
            FormEvent.DraftsFolderChanged(SPECIAL_FOLDER_DRAFTS),
            FormEvent.SentFolderChanged(SPECIAL_FOLDER_SENT),
            FormEvent.SpamFolderChanged(SPECIAL_FOLDER_SPAM),
            FormEvent.TrashFolderChanged(SPECIAL_FOLDER_TRASH),
        )
    }

    @Test
    fun `should save form data and emit NavigateNext effect when OnNextClicked event received`() = runMviTest {
        val initialState = State(isManualSetup = true)
        val accountStateRepository = InMemoryAccountStateRepository()
        val testSubject = createTestSubject(
            initialState = initialState,
            accountStateRepository = accountStateRepository,
        )
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnNextClicked)

        assertThat(turbines.awaitStateItem()).isEqualTo(initialState.copy(isLoading = false))

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.NavigateNext(true))
        }

        assertThat(accountStateRepository.getState()).isEqualTo(
            AccountState(
                specialFolderSettings = SpecialFolderSettings(
                    archiveSpecialFolderOption = SpecialFolderOption.None(isAutomatic = true),
                    draftsSpecialFolderOption = SpecialFolderOption.None(isAutomatic = true),
                    sentSpecialFolderOption = SpecialFolderOption.None(isAutomatic = true),
                    spamSpecialFolderOption = SpecialFolderOption.None(isAutomatic = true),
                    trashSpecialFolderOption = SpecialFolderOption.None(isAutomatic = true),
                ),
            ),
        )
    }

    @Test
    fun `should emit NavigateBack effect when OnBackClicked event received`() = runMviTest {
        val testSubject = createTestSubject()
        val turbines = turbinesWithInitialStateCheck(testSubject, State())

        testSubject.event(Event.OnBackClicked)

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.NavigateBack)
        }
    }

    @Test
    fun `should show form when OnRetryClicked event received`() = runMviTest {
        val initialState = State(error = SpecialFoldersContract.Failure.LoadFoldersFailed("irrelevant"))
        val testSubject = createTestSubject(initialState = initialState)
        val turbines = turbinesWithInitialStateCheck(testSubject, initialState)

        testSubject.event(Event.OnRetryClicked)

        assertThat(turbines.awaitStateItem()).isEqualTo(
            initialState.copy(
                isLoading = true,
                error = null,
            ),
        )

        // Turbine misses the intermediate state because we're using UnconfinedTestDispatcher and StateFlow.
        // Here we need to make sure the coroutine used to load the special folder options has completed.
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(turbines.awaitStateItem()).isEqualTo(
            State(
                isLoading = false,
                isSuccess = true,
                formState = FormState(
                    archiveSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.archiveSpecialFolderOptions,
                    draftsSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.draftsSpecialFolderOptions,
                    sentSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.sentSpecialFolderOptions,
                    spamSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.spamSpecialFolderOptions,
                    trashSpecialFolderOptions = SPECIAL_FOLDER_OPTIONS.trashSpecialFolderOptions,

                    selectedArchiveSpecialFolderOption = SPECIAL_FOLDER_ARCHIVE.copy(isAutomatic = true),
                    selectedDraftsSpecialFolderOption = SPECIAL_FOLDER_DRAFTS.copy(isAutomatic = true),
                    selectedSentSpecialFolderOption = SPECIAL_FOLDER_SENT.copy(isAutomatic = true),
                    selectedSpamSpecialFolderOption = SPECIAL_FOLDER_SPAM.copy(isAutomatic = true),
                    selectedTrashSpecialFolderOption = SPECIAL_FOLDER_TRASH.copy(isAutomatic = true),
                ),
            ),
        )

        turbines.assertThatAndEffectTurbineConsumed {
            isEqualTo(Effect.NavigateNext(false))
        }
    }

    private object TestValidationError : ValidationError

    private companion object {
        fun createTestSubject(
            formUiModel: SpecialFoldersContract.FormUiModel = FakeSpecialFoldersFormUiModel(),
            getSpecialFolderOptions: () -> SpecialFolderOptions = { SPECIAL_FOLDER_OPTIONS },
            validateSpecialFolderOptions: (SpecialFolderOptions) -> ValidationResult = { ValidationResult.Success },
            accountStateRepository: AccountDomainContract.AccountStateRepository = InMemoryAccountStateRepository(),
            initialState: State = State(),
        ) = SpecialFoldersViewModel(
            formUiModel = formUiModel,
            getSpecialFolderOptions = {
                delay(50)
                getSpecialFolderOptions()
            },
            validateSpecialFolderOptions = validateSpecialFolderOptions,
            accountStateRepository = accountStateRepository,
            initialState = initialState,
        )

        val REMOTE_FOLDER = RemoteFolder(FolderServerId("archive"), "archive", FolderType.ARCHIVE)

        val SPECIAL_FOLDER_ARCHIVE = SpecialFolderOption.Special(
            isAutomatic = false,
            remoteFolder = REMOTE_FOLDER.copy(displayName = "Archive"),
        )
        val SPECIAL_FOLDER_DRAFTS = SpecialFolderOption.Special(
            isAutomatic = false,
            remoteFolder = REMOTE_FOLDER.copy(displayName = "Drafts"),
        )
        val SPECIAL_FOLDER_SENT = SpecialFolderOption.Special(
            isAutomatic = false,
            remoteFolder = REMOTE_FOLDER.copy(displayName = "Sent"),
        )
        val SPECIAL_FOLDER_SPAM = SpecialFolderOption.Special(
            isAutomatic = false,
            remoteFolder = REMOTE_FOLDER.copy(displayName = "Spam"),
        )
        val SPECIAL_FOLDER_TRASH = SpecialFolderOption.Special(
            isAutomatic = false,
            remoteFolder = REMOTE_FOLDER.copy(displayName = "Trash"),
        )

        val SPECIAL_FOLDER_OPTIONS = SpecialFolderOptions(
            archiveSpecialFolderOptions = listOf(
                SPECIAL_FOLDER_ARCHIVE.copy(isAutomatic = true),
                SpecialFolderOption.None(),
                SPECIAL_FOLDER_ARCHIVE,
                SpecialFolderOption.Regular(REMOTE_FOLDER),
            ),
            draftsSpecialFolderOptions = listOf(
                SPECIAL_FOLDER_DRAFTS.copy(isAutomatic = true),
                SpecialFolderOption.None(),
                SPECIAL_FOLDER_DRAFTS,
                SpecialFolderOption.Regular(REMOTE_FOLDER),
            ),
            sentSpecialFolderOptions = listOf(
                SPECIAL_FOLDER_SENT.copy(isAutomatic = true),
                SpecialFolderOption.None(),
                SPECIAL_FOLDER_SENT,
                SpecialFolderOption.Regular(REMOTE_FOLDER),
            ),
            spamSpecialFolderOptions = listOf(
                SPECIAL_FOLDER_SPAM.copy(isAutomatic = true),
                SpecialFolderOption.None(),
                SPECIAL_FOLDER_SPAM,
                SpecialFolderOption.Regular(REMOTE_FOLDER),
            ),
            trashSpecialFolderOptions = listOf(
                SPECIAL_FOLDER_TRASH.copy(isAutomatic = true),
                SpecialFolderOption.None(),
                SPECIAL_FOLDER_TRASH,
                SpecialFolderOption.Regular(REMOTE_FOLDER),
            ),
        )
    }
}
