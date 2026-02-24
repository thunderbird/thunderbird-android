package com.fsck.k9.ui.messagelist

import android.os.Bundle
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentActivity
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.MessagingControllerRegistry
import com.fsck.k9.controller.MessagingControllerWrapper
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.ui.R
import com.fsck.k9.ui.changelog.RecentChangesViewModel
import com.fsck.k9.ui.messagelist.debug.AuthDebugActions
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.network.ConnectivityManager
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings
import net.thunderbird.core.testing.TestClock
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.dialog.SetupArchiveFolderDialogFragmentFactory
import net.thunderbird.feature.notification.api.ui.dialog.ErrorNotificationsDialogFragmentFactory
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.serialization.LocalMessageSearchSerializer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(qualifiers = "w360dp-h640dp")
class BaseMessageListFragmentTest : RobolectricTest() {

    private val generalSettingsManager = mock<GeneralSettingsManager> {
        val generalSettings = GeneralSettings(
            platformConfigProvider = mock(),
            display = DisplaySettings(
                inboxSettings = DisplayInboxSettings(
                    isShowComposeButtonOnMessageList = true,
                ),
            ),
        )
        on { getConfig() } doReturn generalSettings
        on { getConfigFlow() } doReturn MutableStateFlow(generalSettings)
        on { getSettingsFlow() } doReturn MutableStateFlow(generalSettings)
    }
    private val featureFlagProvider = mock<FeatureFlagProvider> {
        on { provide(any()) } doReturn FeatureFlagResult.Disabled
    }
    private val messageListViewModel = mock<MessageListViewModel> {
        on { getMessageListLiveData() } doReturn mock()
    }
    private val recentChangesViewModel = mock<RecentChangesViewModel> {
        on { shouldShowRecentChangesHint } doReturn mock()
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private val testModule = module {
        single { mock<MessagingControllerWrapper>() }
        single { mock<MessagingControllerRegistry>() }
        single { generalSettingsManager }
        single { mock<ConnectivityManager>() }
        single { mock<LocalStoreProvider>() }
        single { mock<OutboxFolderManager>() }
        single { mock<LegacyAccountManager>() }
        single { featureFlagProvider }
        viewModel { messageListViewModel }
        single { mock<FeatureThemeProvider>() }
        single { TestLogger() }
        single<Clock> { TestClock() }
        single { mock<SetupArchiveFolderDialogFragmentFactory>() }
        single { mock<DomainContract.UseCase.BuildSwipeActions>() }
        single { mock<SortTypeToastProvider>() }
        single { mock<ContactRepository>() }
        single { mock<AvatarMonogramCreator>() }
        single { mock<AuthDebugActions>() }
        single { mock<ErrorNotificationsDialogFragmentFactory>() }
        single { mock<com.fsck.k9.contacts.ContactPictureLoader>() }
        viewModel { recentChangesViewModel }
    }

    @Before
    fun setup() {
        startKoin {
            androidContext(RuntimeEnvironment.getApplication())
            modules(testModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun onSaveInstanceState_beforeInitialization_shouldNotCrash() {
        val search = LocalMessageSearch()
        val args = Bundle().apply {
            putByteArray("searchObject", LocalMessageSearchSerializer.serialize(search))
            putBoolean("isThreadedDisplay", false)
            putBoolean("showingThreadedList", false)
        }

        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        activity.setTheme(R.style.Theme_Legacy_Test)
        val fragment = TestBaseMessageListFragment().apply {
            arguments = args
        }

        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "test_fragment")
            .commitNow()

        // This is where it crashed before:
        // outState.putLongArray(STATE_SELECTED_MESSAGES, adapter.selected.toLongArray())

        val outState = Bundle()
        fragment.onSaveInstanceState(outState)
    }

    class TestBaseMessageListFragment : BaseMessageListFragment() {
        override val logTag: String = "TestBaseMessageListFragment"
        override val swipeActions = MutableStateFlow<Map<AccountId, SwipeActions>>(emptyMap()).asStateFlow()
    }

    class TestActivity : FragmentActivity(), BaseMessageListFragment.MessageListFragmentListener {
        override fun startSearch(
            query: String,
            account: LegacyAccount?,
            folderId: Long?,
        ): Boolean = true

        override fun setMessageListProgressEnabled(enable: Boolean) = Unit

        override fun setMessageListProgress(level: Int) = Unit

        override fun showThread(
            account: LegacyAccount,
            threadRootId: Long,
        ) = Unit

        override fun openMessage(messageReference: MessageReference) = Unit

        override fun setMessageListTitle(title: String, subtitle: String?) = Unit

        override fun onCompose(account: LegacyAccount?) = Unit

        override fun startSupportActionMode(callback: ActionMode.Callback): ActionMode? = null

        override fun goBack() = Unit
    }
}
