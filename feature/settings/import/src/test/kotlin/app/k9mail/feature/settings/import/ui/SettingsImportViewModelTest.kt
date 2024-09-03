package app.k9mail.feature.settings.import.ui

import android.app.Application
import android.content.ContentResolver
import androidx.core.net.toUri
import app.k9mail.feature.settings.import.SettingsImportExternalContract.AccountActivator
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.preferences.AccountDescription
import com.fsck.k9.preferences.AccountDescriptionPair
import com.fsck.k9.preferences.ImportContents
import com.fsck.k9.preferences.ImportResults
import com.fsck.k9.preferences.SettingsImportExportException
import com.fsck.k9.preferences.SettingsImporter
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsImportViewModelTest {
    private val testScope = TestScope()
    private val contentResolver = mock<ContentResolver>()
    private val settingsImporter = mock<SettingsImporter>()
    private val accountActivator = mock<AccountActivator>()
    private val importAppFetcher = mock<ImportAppFetcher>()
    private val viewModel = SettingsImportViewModel(
        contentResolver = contentResolver,
        settingsImporter = settingsImporter,
        accountActivator = accountActivator,
        importAppFetcher = importAppFetcher,
        backgroundDispatcher = Dispatchers.Unconfined,
        viewModelScope = testScope,
    )

    @Test
    fun `pickAppButton should only be enabled after app from which we can import has been found`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn true
        }
        val uiModel = viewModel.getUiModel().value!!

        assertThat(uiModel.isPickAppButtonVisible).isTrue()
        assertThat(uiModel.isPickAppButtonEnabled).isFalse()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        assertThat(uiModel.isPickAppButtonVisible).isTrue()
        assertThat(uiModel.isPickAppButtonEnabled).isTrue()
    }

    @Test
    fun `pickAppButton should remain disabled when no app from which we can import has been found`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn false
        }
        val uiModelLiveData = viewModel.getUiModel()

        assertThat(uiModelLiveData.value!!.isPickAppButtonVisible).isTrue()
        assertThat(uiModelLiveData.value!!.isPickAppButtonEnabled).isFalse()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        assertThat(uiModelLiveData.value!!.isPickAppButtonVisible).isTrue()
        assertThat(uiModelLiveData.value!!.isPickAppButtonEnabled).isFalse()
    }

    @Test
    fun `clicking pickAppButton should disable buttons and emit PickApp event`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn true
        }
        val uiModelLiveData = viewModel.getUiModel()
        val actionEventsLiveData = viewModel.getActionEvents()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        viewModel.onPickAppButtonClicked()

        assertThat(uiModelLiveData.value!!.isPickDocumentButtonEnabled).isFalse()
        assertThat(uiModelLiveData.value!!.isPickAppButtonEnabled).isFalse()
        assertThat(actionEventsLiveData.value).isEqualTo(Action.PickApp)
    }

    @Test
    fun `canceling picking an app should enable buttons`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn true
        }
        val uiModelLiveData = viewModel.getUiModel()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        viewModel.onPickAppButtonClicked()
        viewModel.onAppPickCanceled()

        assertThat(uiModelLiveData.value!!.isPickDocumentButtonEnabled).isTrue()
        assertThat(uiModelLiveData.value!!.isPickAppButtonEnabled).isTrue()
    }

    @Test
    fun `picking an app should show loading progress, then settings list`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn true
        }
        val uiModelLiveData = viewModel.getUiModel()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        viewModel.onPickAppButtonClicked()
        viewModel.onAppPicked("net.thunderbird.android")

        assertThat(uiModelLiveData.value!!.isLoadingProgressVisible).isTrue()
        assertThat(uiModelLiveData.value!!.isPickDocumentButtonVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isPickAppButtonVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isSettingsListVisible).isFalse()

        val inputStream = "".byteInputStream()
        contentResolver.stub {
            on { openInputStream("content://net.thunderbird.android.settings/".toUri()) } doReturn inputStream
        }
        settingsImporter.stub {
            on { getImportStreamContents(inputStream) } doReturn ImportContents(
                globalSettings = false,
                accounts = listOf(
                    AccountDescription(name = "Account 1", uuid = "uuid-1"),
                    AccountDescription(name = "Account 2", uuid = "uuid-2"),
                ),
            )
        }

        // Read settings data from content provider
        testScope.testScheduler.advanceUntilIdle()

        assertThat(uiModelLiveData.value!!.isLoadingProgressVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isPickDocumentButtonVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isPickAppButtonVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isSettingsListVisible).isTrue()
        assertThat(uiModelLiveData.value!!.settingsList).all {
            hasSize(2)
            index(0).isInstanceOf<SettingsListItem.Account>()
                .prop(SettingsListItem.Account::displayName).isEqualTo("Account 1")
            index(1).isInstanceOf<SettingsListItem.Account>()
                .prop(SettingsListItem.Account::displayName).isEqualTo("Account 2")
        }
    }

    @Test
    fun `failure to import settings from an app should show error message`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn true
        }
        val uiModelLiveData = viewModel.getUiModel()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        viewModel.onPickAppButtonClicked()
        viewModel.onAppPicked("net.thunderbird.android")

        val inputStream = "".byteInputStream()
        contentResolver.stub {
            on { openInputStream("content://net.thunderbird.android.settings/".toUri()) } doReturn inputStream
        }
        settingsImporter.stub {
            on { getImportStreamContents(inputStream) } doThrow SettingsImportExportException()
        }

        // Read settings data from content provider
        testScope.testScheduler.advanceUntilIdle()

        assertThat(uiModelLiveData.value!!.isLoadingProgressVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isPickDocumentButtonVisible).isTrue()
        assertThat(uiModelLiveData.value!!.isPickAppButtonVisible).isTrue()
        assertThat(uiModelLiveData.value!!.isSettingsListVisible).isFalse()
        assertThat(uiModelLiveData.value!!.statusText).isEqualTo(StatusText.IMPORT_READ_FAILURE)
    }

    @Test
    fun `successfully importing settings from an app should show success message`() {
        importAppFetcher.stub {
            on { isAtLeastOneAppInstalled() } doReturn true
        }
        val uiModelLiveData = viewModel.getUiModel()

        // Check for apps we can import from
        testScope.testScheduler.advanceUntilIdle()

        viewModel.onPickAppButtonClicked()
        viewModel.onAppPicked("net.thunderbird.android")

        val inputStream = "".byteInputStream()
        contentResolver.stub {
            on { openInputStream("content://net.thunderbird.android.settings/".toUri()) } doReturn inputStream
        }
        val accountOne = AccountDescription(name = "Account 1", uuid = "uuid-1")
        val accountTwo = AccountDescription(name = "Account 2", uuid = "uuid-2")
        settingsImporter.stub {
            on { getImportStreamContents(inputStream) } doReturn ImportContents(
                globalSettings = false,
                accounts = listOf(
                    accountOne,
                    accountTwo,
                ),
            )
        }

        // Read settings data from content provider
        testScope.testScheduler.advanceUntilIdle()

        // Deselect second account
        viewModel.onSettingsListItemClicked(1)

        viewModel.onImportButtonClicked()

        assertThat(uiModelLiveData.value!!.isImportProgressVisible).isTrue()
        assertThat(uiModelLiveData.value!!.isSettingsListEnabled).isFalse()
        assertThat(uiModelLiveData.value!!.importButton).isEqualTo(ButtonState.INVISIBLE)
        assertThat(uiModelLiveData.value!!.statusText).isEqualTo(StatusText.IMPORTING_PROGRESS)

        settingsImporter.stub {
            on { importSettings(inputStream, false, listOf("uuid-1")) } doReturn ImportResults(
                globalSettings = false,
                importedAccounts = listOf(
                    AccountDescriptionPair(
                        original = accountOne,
                        imported = accountOne,
                        authorizationNeeded = false,
                        incomingPasswordNeeded = false,
                        outgoingPasswordNeeded = false,
                        incomingServerName = "irrelevant",
                        outgoingServerName = "irrelevant",
                    ),
                ),
                erroneousAccounts = emptyList(),
            )
        }

        // Import settings
        testScope.testScheduler.advanceUntilIdle()

        assertThat(uiModelLiveData.value!!.isImportProgressVisible).isFalse()
        assertThat(uiModelLiveData.value!!.isSettingsListEnabled).isTrue()
        assertThat(uiModelLiveData.value!!.importButton).isEqualTo(ButtonState.GONE)
        assertThat(uiModelLiveData.value!!.closeButton).isEqualTo(ButtonState.ENABLED)
        assertThat(uiModelLiveData.value!!.closeButtonLabel).isEqualTo(CloseButtonLabel.OK)
        assertThat(uiModelLiveData.value!!.statusText).isEqualTo(StatusText.IMPORT_SUCCESS)
    }
}
