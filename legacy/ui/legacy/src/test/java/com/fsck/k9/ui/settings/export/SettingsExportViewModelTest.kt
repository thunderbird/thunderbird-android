package com.fsck.k9.ui.settings.export

import android.net.Uri
import android.os.Bundle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fsck.k9.preferences.SettingsExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccountManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsExportViewModelTest {

    private val accountManager = mock<LegacyAccountManager>()
    private val settingsExporter = mock<SettingsExporter>()
    private val logger = mock<Logger>()

    private val contentUri = mock<Uri>()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: SettingsExportViewModel

    private val fakeRealtimeMeasurer = RealtimeMeasurer { block ->
        block()
        0L
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(accountManager.findAll()).thenReturn(emptyList())
        whenever(settingsExporter.generateDatedExportFileName())
            .thenReturn("settings-export.k9s")

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsExportViewModel(
        accountManager = accountManager,
        settingsExporter = settingsExporter,
        logger = logger,
        realtimeMeasurer = fakeRealtimeMeasurer,
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `initial state loads general settings`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.state.value

        assertThat(state.settingsList).isEqualTo(
            listOf(SettingsListItem.GeneralSettings()),
        )
        assertThat(state.isSettingsListEnabled).isTrue()
        assertThat(state.exportButtonState).isEqualTo(ButtonState.ENABLED)
        assertThat(state.isShareButtonVisible).isFalse()
        assertThat(state.isProgressVisible).isFalse()
        assertThat(state.statusText).isEqualTo(StatusText.HIDDEN)
    }

    @Test
    fun `selection change updates general settings selection`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            val generalSettings = viewModel.state.value.settingsList
                .filterIsInstance<SettingsListItem.GeneralSettings>()
                .single()

            viewModel.event(
                SettingsExportContract.Event.OnSettingsItemSelectionChange(
                    itemId = generalSettings.stableId,
                    isSelected = false,
                ),
            )

            val updatedItem = viewModel.state.value.settingsList
                .filterIsInstance<SettingsListItem.GeneralSettings>()
                .single()

            assertThat(updatedItem.selected).isFalse()
        }

    @Test
    fun `selection change with unknown item id does not update list`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            val originalList = viewModel.state.value.settingsList

            viewModel.event(
                SettingsExportContract.Event.OnSettingsItemSelectionChange(
                    itemId = "unknown-item",
                    isSelected = true,
                ),
            )

            assertThat(viewModel.state.value.settingsList)
                .isEqualTo(originalList)
        }

    @Test
    fun `export click disables button and emits pick document effect`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.event(SettingsExportContract.Event.OnExportClick)

                assertThat(awaitItem()).isEqualTo(
                    SettingsExportContract.Effect.PickDocument(
                        fileNameSuggestion = "settings-export.k9s",
                        mimeType = "application/octet-stream",
                    ),
                )

                cancelAndIgnoreRemainingEvents()
            }

            assertThat(viewModel.state.value.exportButtonState)
                .isEqualTo(ButtonState.DISABLED)
        }

    @Test
    fun `export excludes general settings when deselected`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            val generalSettings = viewModel.state.value.settingsList
                .filterIsInstance<SettingsListItem.GeneralSettings>()
                .single()

            viewModel.event(
                SettingsExportContract.Event.OnSettingsItemSelectionChange(
                    itemId = generalSettings.stableId,
                    isSelected = false,
                ),
            )

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            advanceUntilIdle()

            verify(settingsExporter).exportToUri(
                includeGlobals = false,
                accountUuids = emptySet(),
                uri = contentUri,
            )
        }

    @Test
    fun `document pick cancellation restores initial state`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.event(SettingsExportContract.Event.OnExportClick)
            viewModel.event(SettingsExportContract.Event.OnDocumentPickCanceled)

            val state = viewModel.state.value

            assertThat(state.exportButtonState).isEqualTo(ButtonState.ENABLED)
            assertThat(state.isSettingsListEnabled).isTrue()
            assertThat(state.isProgressVisible).isFalse()
            assertThat(state.isShareButtonVisible).isFalse()
        }

    @Test
    fun `document picked immediately shows progress state`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            val state = viewModel.state.value

            assertThat(state.isProgressVisible).isTrue()
            assertThat(state.isSettingsListEnabled).isFalse()
            assertThat(state.isShareButtonVisible).isFalse()
            assertThat(state.exportButtonState)
                .isEqualTo(ButtonState.INVISIBLE)
            assertThat(state.statusText).isEqualTo(StatusText.PROGRESS)
        }

    @Test
    fun `successful export updates state and calls exporter`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            advanceUntilIdle()

            val state = viewModel.state.value

            assertThat(state.isProgressVisible).isFalse()
            assertThat(state.isSettingsListEnabled).isTrue()
            assertThat(state.isShareButtonVisible).isTrue()
            assertThat(state.exportButtonState).isEqualTo(ButtonState.GONE)
            assertThat(state.statusText).isEqualTo(StatusText.EXPORT_SUCCESS)

            verify(settingsExporter).exportToUri(
                includeGlobals = true,
                accountUuids = emptySet(),
                uri = contentUri,
            )
        }

    @Test
    fun `export failure updates state and logs exception`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            whenever(
                settingsExporter.exportToUri(
                    any(),
                    any(),
                    any(),
                ),
            ).thenThrow(IllegalStateException("Export failed"))

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            advanceUntilIdle()

            val state = viewModel.state.value

            assertThat(state.isProgressVisible).isFalse()
            assertThat(state.isSettingsListEnabled).isTrue()
            assertThat(state.isShareButtonVisible).isFalse()
            assertThat(state.exportButtonState).isEqualTo(ButtonState.GONE)
            assertThat(state.statusText).isEqualTo(StatusText.EXPORT_FAILURE)

            verify(settingsExporter).exportToUri(
                includeGlobals = true,
                accountUuids = emptySet(),
                uri = contentUri,
            )
        }

    @Test
    fun `share click emits no effect before document is picked`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.event(SettingsExportContract.Event.OnShareClick)

                expectNoEvents()
            }
        }

    @Test
    fun `share click emits share effect after export`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.event(SettingsExportContract.Event.OnShareClick)

                assertThat(awaitItem()).isEqualTo(
                    SettingsExportContract.Effect.ShareDocument(
                        contentUri = contentUri,
                        mimeType = "application/octet-stream",
                    ),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `share click emits no effect after export failure`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            whenever(
                settingsExporter.exportToUri(
                    any(),
                    any(),
                    any(),
                ),
            ).thenThrow(IllegalStateException("Export failed"))

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.event(SettingsExportContract.Event.OnShareClick)

                expectNoEvents()
            }
        }

    @Test
    fun `save and restore preserves progress state`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            val savedState = createSavedStateBundle()
            viewModel.saveInstanceState(savedState)

            val restoredViewModel = createViewModel()
            advanceUntilIdle()

            restoredViewModel.initializeFromSavedState(savedState)

            val restoredState = restoredViewModel.state.value

            assertThat(restoredState.isProgressVisible).isTrue()
            assertThat(restoredState.isSettingsListEnabled).isFalse()
            assertThat(restoredState.isShareButtonVisible).isFalse()
            assertThat(restoredState.exportButtonState)
                .isEqualTo(ButtonState.INVISIBLE)
            assertThat(restoredState.statusText).isEqualTo(StatusText.PROGRESS)
        }

    @Test
    fun `restored content uri is available for sharing`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            viewModel.event(
                SettingsExportContract.Event.OnDocumentPicked(contentUri),
            )

            val savedState = createSavedStateBundle()
            viewModel.saveInstanceState(savedState)

            val restoredViewModel = createViewModel()
            advanceUntilIdle()

            restoredViewModel.initializeFromSavedState(savedState)

            restoredViewModel.effect.test {
                restoredViewModel.event(
                    SettingsExportContract.Event.OnShareClick,
                )

                assertThat(awaitItem()).isEqualTo(
                    SettingsExportContract.Effect.ShareDocument(
                        contentUri = contentUri,
                        mimeType = "application/octet-stream",
                    ),
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createSavedStateBundle(): Bundle {
        val values = mutableMapOf<String, Any?>()

        return mock {
            on { putBoolean(any(), any()) }.thenAnswer { invocation ->
                values[invocation.getArgument<String>(0)] =
                    invocation.getArgument<Boolean>(1)
                null
            }

            on { putString(any(), any()) }.thenAnswer { invocation ->
                values[invocation.getArgument<String>(0)] =
                    invocation.getArgument<String?>(1)
                null
            }

            on { putParcelable(any<String>(), any<Uri>()) }.thenAnswer { invocation ->
                values[invocation.getArgument<String>(0)] =
                    invocation.getArgument<Uri>(1)
                null
            }

            on { getBoolean(any()) }.thenAnswer { invocation ->
                values[invocation.getArgument<String>(0)] as? Boolean ?: false
            }

            on { getString(any(), any()) }.thenAnswer { invocation ->
                values[invocation.getArgument<String>(0)] as? String
                    ?: invocation.getArgument<String>(1)
            }

            on { getParcelable<Uri>(any()) }.thenAnswer { invocation ->
                values[invocation.getArgument<String>(0)] as? Uri
            }
        }
    }
}
