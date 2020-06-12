package com.fsck.k9.widget.unread

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.AppRobolectricTest
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.mailstore.FolderRepositoryManager
import com.fsck.k9.mailstore.FolderType
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.ui.folders.FolderNameFormatterFactory
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.robolectric.RuntimeEnvironment

class UnreadWidgetDataProviderTest : AppRobolectricTest() {
    val context: Context = RuntimeEnvironment.application
    val account = createAccount()
    val preferences = createPreferences()
    val messagingController = createMessagingController()
    val defaultFolderStrategy = createDefaultFolderStrategy()
    val folderRepositoryManager = createFolderRepositoryManager()
    val folderNameFormatterFactory = createFolderNameFormatterFactory()
    val provider = UnreadWidgetDataProvider(context, preferences, messagingController, defaultFolderStrategy,
        folderRepositoryManager, folderNameFormatterFactory)

    @Test
    fun unifiedInbox() {
        val configuration = UnreadWidgetConfiguration(
                appWidgetId = 1, accountUuid = SearchAccount.UNIFIED_INBOX, folderId = null)

        val widgetData = provider.loadUnreadWidgetData(configuration)

        with(widgetData!!) {
            assertThat(title).isEqualTo("Unified Inbox")
            assertThat(unreadCount).isEqualTo(SEARCH_ACCOUNT_UNREAD_COUNT)
        }
    }

    @Test
    fun regularAccount() {
        val configuration = UnreadWidgetConfiguration(
                appWidgetId = 3, accountUuid = ACCOUNT_UUID, folderId = null)

        val widgetData = provider.loadUnreadWidgetData(configuration)

        with(widgetData!!) {
            assertThat(title).isEqualTo(ACCOUNT_DESCRIPTION)
            assertThat(unreadCount).isEqualTo(ACCOUNT_UNREAD_COUNT)
        }
    }

    @Test
    fun folder() {
        val configuration = UnreadWidgetConfiguration(appWidgetId = 4, accountUuid = ACCOUNT_UUID, folderId = FOLDER_ID)

        val widgetData = provider.loadUnreadWidgetData(configuration)

        with(widgetData!!) {
            assertThat(title).isEqualTo("$ACCOUNT_DESCRIPTION - $LOCALIZED_FOLDER_NAME")
            assertThat(unreadCount).isEqualTo(FOLDER_UNREAD_COUNT)
        }
    }

    @Test
    fun nonExistentAccount_shouldReturnNull() {
        val configuration = UnreadWidgetConfiguration(appWidgetId = 3, accountUuid = "invalid", folderId = null)

        val widgetData = provider.loadUnreadWidgetData(configuration)

        assertThat(widgetData).isNull()
    }

    fun createAccount(): Account = mock {
        on { uuid } doReturn ACCOUNT_UUID
        on { description } doReturn ACCOUNT_DESCRIPTION
    }

    fun createPreferences(): Preferences = mock {
        on { getAccount(ACCOUNT_UUID) } doReturn account
    }

    fun createMessagingController(): MessagingController = mock {
        on { getUnreadMessageCount(any<SearchAccount>()) } doReturn SEARCH_ACCOUNT_UNREAD_COUNT
        on { getUnreadMessageCount(account) } doReturn ACCOUNT_UNREAD_COUNT
        on { getFolderUnreadMessageCount(eq(account), eq(FOLDER_ID)) } doReturn FOLDER_UNREAD_COUNT
    }

    fun createDefaultFolderStrategy(): DefaultFolderProvider = mock {
        on { getDefaultFolder(account) } doReturn FOLDER_ID
    }

    fun createFolderRepositoryManager(): FolderRepositoryManager {
        val folderRepository = createFolderRepository()
        return mock {
            on { getFolderRepository(account) } doReturn folderRepository
        }
    }

    fun createFolderRepository(): FolderRepository {
        return mock {
            on { getFolder(FOLDER_ID) } doReturn FOLDER
        }
    }

    private fun createFolderNameFormatterFactory(): FolderNameFormatterFactory {
        val folderNameFormatter = createFolderNameFormatter()
        return mock {
            on { create(any()) } doReturn folderNameFormatter
        }
    }

    private fun createFolderNameFormatter(): FolderNameFormatter = mock {
        on { displayName(FOLDER) } doReturn LOCALIZED_FOLDER_NAME
    }

    companion object {
        const val ACCOUNT_UUID = "00000000-0000-0000-0000-000000000000"
        const val ACCOUNT_DESCRIPTION = "Test account"
        const val FOLDER_ID = 23L
        const val SEARCH_ACCOUNT_UNREAD_COUNT = 1
        const val ACCOUNT_UNREAD_COUNT = 2
        const val FOLDER_UNREAD_COUNT = 3
        const val LOCALIZED_FOLDER_NAME = "Posteingang"
        val FOLDER = Folder(
            id = FOLDER_ID,
            name = "INBOX",
            type = FolderType.INBOX,
            isLocalOnly = false
        )
    }
}
