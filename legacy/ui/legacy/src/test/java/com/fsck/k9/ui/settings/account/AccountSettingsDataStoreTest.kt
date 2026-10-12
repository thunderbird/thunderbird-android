package com.fsck.k9.ui.settings.account

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.account.FakeLegacyAccountManager
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.ShowPictures
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class AccountSettingsDataStoreTest {
    private val account = FakeLegacyAccount.create(name = "Old name")
    private val accountManager = FakeLegacyAccountManager(listOf(account))

    private var storedSubscribedFoldersOnlyOnRefresh: Boolean? = null
    private val messagingController = mock<MessagingController> {
        on { refreshFolderList(account.id) } doAnswer {
            storedSubscribedFoldersOnlyOnRefresh = accountManager.findById(account.id)?.isSubscribedFoldersOnly
        }
    }

    private val testSubject = AccountSettingsDataStore(
        accountManager = accountManager,
        executorService = DirectExecutorService(),
        account = account,
        jobManager = mock(),
        notificationChannelManager = mock(),
        notificationController = mock(),
        messagingController = messagingController,
    )

    @Test
    fun `renaming account updates name and profile name before saving`() {
        // Act
        testSubject.putString("account_description", "New name")

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::name).isEqualTo("New name")
            prop("profileName") { it.profile.name }.isEqualTo("New name")
        }
        assertThat(testSubject.getString("account_description", "")).isEqualTo("New name")
    }

    @Test
    fun `saving a setting should keep account changes made elsewhere after the screen was opened`() {
        // Arrange
        val storedAccount = requireNotNull(accountManager.findById(account.id))
        accountManager.updateSync(
            storedAccount.copy(
                oAuthState = "refreshed-oauth-state",
                openPgpProvider = "org.sufficientlysecure.keychain",
                inboxFolderId = INBOX_FOLDER_ID,
                messagesNotificationChannelVersion = 3,
            ),
        )

        // Act
        testSubject.putBoolean("always_show_cc_bcc", true)

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::isAlwaysShowCcBcc).isTrue()
            prop(LegacyAccount::oAuthState).isEqualTo("refreshed-oauth-state")
            prop(LegacyAccount::openPgpProvider).isEqualTo("org.sufficientlysecure.keychain")
            prop(LegacyAccount::inboxFolderId).isEqualTo(INBOX_FOLDER_ID)
            prop(LegacyAccount::messagesNotificationChannelVersion).isEqualTo(3)
        }
    }

    @Test
    fun `saving several settings should keep all of them`() {
        // Act
        testSubject.putString("show_pictures_enum", ShowPictures.ALWAYS.name)
        testSubject.putString("account_display_count", "100")
        testSubject.putBoolean("strip_signature", true)

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::showPictures).isEqualTo(ShowPictures.ALWAYS)
            prop(LegacyAccount::displayCount).isEqualTo(100)
            prop(LegacyAccount::isStripSignature).isTrue()
        }
    }

    @Test
    fun `changing subscribed folders only should refresh the folder list after saving`() {
        // Act
        testSubject.putBoolean("subscribed_folders_only", true)

        // Assert
        assertThat(storedSubscribedFoldersOnlyOnRefresh).isNotNull().isTrue()
    }

    private companion object {
        const val INBOX_FOLDER_ID = 42L
    }
}
