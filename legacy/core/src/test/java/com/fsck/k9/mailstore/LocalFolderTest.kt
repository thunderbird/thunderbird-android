package com.fsck.k9.mailstore

import android.net.Uri
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.provider.AttachmentProvider
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.kotlin.whenever
import org.robolectric.RuntimeEnvironment

class LocalFolderTest : K9RobolectricTest() {
    private val localStoreProvider: LocalStoreProvider by inject()
    private val messageStoreManager: MessageStoreManager by inject()
    private val saveMessageDataCreator: SaveMessageDataCreator by inject()

    private val accountId = AccountIdFactory.create()

    @Before
    fun setUp() {
        val accountManager: LegacyAccountManager by inject()
        whenever(accountManager.findById(accountId)).thenReturn(FakeLegacyAccount.create(id = accountId))
        // Core.restoreNotifications() runs in the background at app start and creates NotificationChannelManager,
        // whose init collects observeAll(). The unstubbed mock would return null.
        whenever(accountManager.observeAll()).thenReturn(flowOf(emptyList()))
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.getApplication().cacheDir)
        AttachmentProvider.CONTENT_URI = Uri.parse("content://test.attachmentprovider")
    }

    @Test
    fun `message_rfc822 attachment should show the size from Content-Disposition`() = runTest {
        // Arrange
        saveRemoteMessage(createMessageWithAttachedMessage("attachment; filename=\"attached.eml\"; size=16893"))

        // Act
        val attachmentViewInfo = loadAttachedMessageInfo()

        // Assert
        assertThat(attachmentViewInfo.size).isEqualTo(16893L)
    }

    @Test
    fun `message_rfc822 attachment without size parameter should have unknown size`() = runTest {
        // Arrange
        saveRemoteMessage(createMessageWithAttachedMessage("attachment; filename=\"attached.eml\""))

        // Act
        val attachmentViewInfo = loadAttachedMessageInfo()

        // Assert
        assertThat(attachmentViewInfo.size).isEqualTo(AttachmentViewInfo.UNKNOWN_SIZE)
    }

    private fun loadAttachedMessageInfo(): AttachmentViewInfo {
        val localStore = localStoreProvider.getInstance(accountId)
        val testSubject = localStore.getFolder(FOLDER_SERVER_ID)
        val message = testSubject.getMessage(MESSAGE_SERVER_ID)
        assertThat(message).isNotNull()

        testSubject.fetch(listOf(message), FetchProfile().apply { add(FetchProfile.Item.BODY) }, null)

        val attachmentPart = (message.body as Multipart).getBodyPart(1)
        assertThat(attachmentPart.mimeType).isEqualTo("message/rfc822")
        return localStore.attachmentInfoExtractor.extractAttachmentInfo(attachmentPart)
    }

    private suspend fun saveRemoteMessage(rawMessage: String) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val backendStorage = K9BackendStorage(
            messageStore,
            createFolderSettingsProvider(),
            saveMessageDataCreator,
            emptyList(),
        )
        backendStorage.updateFolders {
            createFolders(listOf(FolderInfo(FOLDER_SERVER_ID, FOLDER_NAME, FolderType.INBOX)))
        }
        val backendFolder = K9BackendFolder(messageStore, saveMessageDataCreator, FOLDER_SERVER_ID)

        val message = MimeMessage.parseMimeMessage(rawMessage.byteInputStream(), false).apply {
            uid = MESSAGE_SERVER_ID
        }
        backendFolder.saveMessage(message, MessageDownloadState.FULL)
    }

    companion object {
        private const val FOLDER_SERVER_ID = "testFolder"
        private const val FOLDER_NAME = "Test Folder"
        private const val MESSAGE_SERVER_ID = "msg001"

        private fun createMessageWithAttachedMessage(contentDisposition: String): String = """
            From: bob@domain.example
            To: alice@domain.example
            Subject: Fwd: Attached
            MIME-Version: 1.0
            Content-Type: multipart/mixed; boundary="outer"

            --outer
            Content-Type: text/plain

            See attachment
            --outer
            Content-Type: message/rfc822; name="attached.eml"
            Content-Disposition: $contentDisposition

            From: alice@domain.example
            Subject: Attached
            Content-Type: text/plain

            Text of the attached message
            --outer--
        """
            .trimIndent().replace("\n", "\r\n")
    }
}
