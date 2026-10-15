package com.fsck.k9.controller

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.single
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.api.Backend
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.components.core.logging.testing.TestLogger
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.fake.FakeLegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.notification.testing.fake.FakeNotificationManager
import net.thunderbird.legacy.core.StubLocalDeleteOperationDecider
import net.thunderbird.legacy.core.mailstore.folder.FakeLocalMessageUidPrefixProvider
import net.thunderbird.legacy.core.mailstore.folder.FakeOutboxFolderManager
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment

class MessagingControllerRefreshFolderListTest : K9RobolectricTest() {
    private val account = FakeLegacyAccount.create()
    private val accountManager = FakeLegacyAccountManager(listOf(account))

    private var onRefreshFolderList: () -> String? = { null }
    private val backend = mock<Backend> {
        on { refreshFolderList() } doAnswer { onRefreshFolderList() }
    }
    private val backendManager = BackendManager(
        backendFactories = mapOf(
            Protocols.IMAP to object : BackendFactory {
                override fun createBackend(accountId: AccountId): Backend = backend
            },
        ),
        accountManager = accountManager,
    )

    private val testSubject = MessagingController(
        RuntimeEnvironment.getApplication(),
        mock(),
        mock(),
        mock(),
        backendManager,
        mock(),
        mock(),
        mock(),
        StubLocalDeleteOperationDecider(),
        FakeLocalMessageUidPrefixProvider(),
        emptyList(),
        { FeatureFlagResult.Disabled },
        TestLogger(),
        FakeNotificationManager(
            emitOnSend = { error("Unexpected notification") },
            emitOnDismissNotification = { error("Unexpected notification dismissal") },
            emitOnDismissId = { error("Unexpected notification dismissal") },
        ),
        FakeOutboxFolderManager(OUTBOX_FOLDER_ID),
        accountManager,
    )

    @After
    fun tearDown() {
        testSubject.stop()
    }

    @Test
    fun `refreshFolderListSynchronous should keep account changes made during the refresh`() {
        // Arrange
        onRefreshFolderList = {
            // Simulates SpecialFolderUpdater saving special folders while the folder list is refreshed
            accountManager.replace(account.copy(inboxFolderId = INBOX_FOLDER_ID, trashFolderId = TRASH_FOLDER_ID))
            NEW_FOLDER_PATH_DELIMITER
        }

        // Act
        testSubject.refreshFolderListSynchronous(account.id)

        // Assert
        assertThat(accountManager.savedAccounts).single().all {
            prop(LegacyAccount::inboxFolderId).isEqualTo(INBOX_FOLDER_ID)
            prop(LegacyAccount::trashFolderId).isEqualTo(TRASH_FOLDER_ID)
            prop(LegacyAccount::folderPathDelimiter).isEqualTo(NEW_FOLDER_PATH_DELIMITER)
        }
    }

    private companion object {
        const val OUTBOX_FOLDER_ID = 1L
        const val INBOX_FOLDER_ID = 42L
        const val TRASH_FOLDER_ID = 43L
        const val NEW_FOLDER_PATH_DELIMITER = "."
    }
}
