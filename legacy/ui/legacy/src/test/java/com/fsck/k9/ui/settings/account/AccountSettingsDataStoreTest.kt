package com.fsck.k9.ui.settings.account

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.job.K9JobManager
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class AccountSettingsDataStoreTest {
    private val account = FakeLegacyAccount.create(name = "Old name")
    private val accountManager = FailingSaveAccountManager(listOf(account))

    private var storedSubscribedFoldersOnlyOnRefresh: Boolean? = null
    private val messagingController = mock<MessagingController> {
        on { refreshFolderList(account.id) } doAnswer {
            storedSubscribedFoldersOnlyOnRefresh = accountManager.findById(account.id)?.isSubscribedFoldersOnly
        }
    }

    private var scheduledAccount: LegacyAccount? = null
    private val jobManager = mock<K9JobManager> {
        on { scheduleMailSync(any()) } doAnswer { scheduledAccount = it.getArgument(0) }
    }

    private val testSubject = AccountSettingsDataStore(
        accountManager = accountManager,
        executorService = DirectExecutorService(),
        account = account,
        jobManager = jobManager,
        notificationChannelManager = mock(),
        notificationController = mock(),
        messagingController = messagingController,
        logger = TestLogger(),
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
        accountManager.updateSync(account.id) {
            it.copy(
                oAuthState = "refreshed-oauth-state",
                openPgpProvider = "org.sufficientlysecure.keychain",
                inboxFolderId = INBOX_FOLDER_ID,
                messagesNotificationChannelVersion = 3,
            )
        }

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

    @Test
    fun `changing the check frequency should reschedule mail sync with the saved account`() {
        // Arrange
        accountManager.updateSync(account.id) { it.copy(lastSyncTime = LAST_SYNC_TIME) }

        // Act
        testSubject.putString("account_check_frequency", "30")

        // Assert
        assertThat(scheduledAccount).isNotNull().all {
            prop(LegacyAccount::automaticCheckIntervalMinutes).isEqualTo(30)
            prop(LegacyAccount::lastSyncTime).isEqualTo(LAST_SYNC_TIME)
        }
    }

    @Test
    fun `a failed save should keep the change and its follow-up action for the next save`() {
        // Arrange
        accountManager.failingSaveCount = 1
        testSubject.putBoolean("subscribed_folders_only", true)
        assertThat(accountManager.findById(account.id)).isNotNull()
            .prop(LegacyAccount::isSubscribedFoldersOnly).isFalse()
        assertThat(storedSubscribedFoldersOnlyOnRefresh).isNull()

        // Act
        testSubject.putBoolean("always_show_cc_bcc", true)

        // Assert
        assertThat(accountManager.findById(account.id)).isNotNull().all {
            prop(LegacyAccount::isSubscribedFoldersOnly).isTrue()
            prop(LegacyAccount::isAlwaysShowCcBcc).isTrue()
        }
        assertThat(storedSubscribedFoldersOnlyOnRefresh).isNotNull().isTrue()
    }

    private companion object {
        const val LAST_SYNC_TIME = 1_700_000_000_000L
        const val INBOX_FOLDER_ID = 42L
    }

    /** Fake account manager whose next [failingSaveCount] saves fail, like a storage commit that doesn't succeed. */
    private class FailingSaveAccountManager(accounts: List<LegacyAccount>) : FakeLegacyAccountManager(accounts) {
        var failingSaveCount = 0

        override fun updateSync(accountId: AccountId, transform: (LegacyAccount) -> LegacyAccount): LegacyAccount? {
            if (failingSaveCount > 0) {
                failingSaveCount--
                error("Simulated save failure")
            }
            return super.updateSync(accountId, transform)
        }
    }
}
