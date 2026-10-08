package com.fsck.k9.mailstore

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.fsck.k9.FakeLegacyAccount
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.TextBody
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.Before
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.kotlin.whenever

class K9BackendFolderTest : K9RobolectricTest() {
    val localStoreProvider: LocalStoreProvider by inject()
    val messageStoreManager: MessageStoreManager by inject()
    val saveMessageDataCreator: SaveMessageDataCreator by inject()

    val accountId = AccountIdFactory.create()
    private lateinit var backendFolder: BackendFolder
    private lateinit var database: LockableDatabase

    @Before
    fun setUp() {
        val accountManager: LegacyAccountManager by inject()
        whenever(accountManager.findById(accountId)).thenReturn(FakeLegacyAccount.create(id = accountId))
        backendFolder = createBackendFolder()
        database = localStoreProvider.getInstance(accountId).database
    }

    @Test
    fun getMessageFlags() = runTest {
        val flags = setOf(Flag.SEEN, Flag.DRAFT, Flag.X_DOWNLOADED_FULL)
        createMessageInBackendFolder(MESSAGE_SERVER_ID, flags)

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags).isEqualTo(flags)
    }

    @Test
    fun getMessageFlags_withFlagsColumnSetToNull_shouldBeTreatedAsEmpty() = runTest {
        createMessageInBackendFolder(MESSAGE_SERVER_ID)
        setFlagsColumnToNull()

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags.isEmpty()).isTrue()
    }

    @Test
    fun getMessageFlags_withFlagsColumnSetToNull_shouldReadSpecialColumnFlags() = runTest {
        val flags = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)
        createMessageInBackendFolder(MESSAGE_SERVER_ID, flags)
        setFlagsColumnToNull()

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags).isEqualTo(flags)
    }

    @Test
    fun saveCompleteMessage_withoutServerId_shouldThrow() = runTest {
        val message = createMessage(messageServerId = null)

        assertFailure {
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Message requires a server ID to be set")
    }

    @Test
    fun savePartialMessage_withoutServerId_shouldThrow() = runTest {
        val message = createMessage(messageServerId = null)

        assertFailure {
            backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Message requires a server ID to be set")
    }

    fun createBackendFolder(): BackendFolder {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        val backendStorage = K9BackendStorage(
            messageStore,
            createFolderSettingsProvider(),
            saveMessageDataCreator,
            emptyList(),
        )
        backendStorage.updateFolders {
            createFolders(listOf(FolderInfo(FOLDER_SERVER_ID, FOLDER_NAME, FOLDER_TYPE)))
        }

        val folderServerIds = backendStorage.getFolderServerIds()
        assertThat(folderServerIds).contains(FOLDER_SERVER_ID)

        return K9BackendFolder(messageStore, saveMessageDataCreator, FOLDER_SERVER_ID)
    }

    suspend fun createMessageInBackendFolder(messageServerId: String, flags: Set<Flag> = emptySet()) {
        val message = createMessage(messageServerId, flags)
        backendFolder.saveMessage(message, MessageDownloadState.FULL)

        val messageServerIds = backendFolder.getMessageServerIds()
        assertThat(messageServerIds).contains(messageServerId)
    }

    private fun createMessage(messageServerId: String?, flags: Set<Flag> = emptySet()): Message {
        return MimeMessage().apply {
            subject = "Test message"
            setFrom(Address("alice@domain.example"))
            setHeader("To", "bob@domain.example")
            MimeMessageHelper.setBody(this, TextBody("Hello Bob!"))

            uid = messageServerId
            setFlags(flags, true)
        }
    }

    private fun setFlagsColumnToNull() {
        dbOperation { db ->
            val numberOfUpdatedRows = db.update(
                "messages",
                contentValuesOf("flags" to null),
                "uid = ?",
                arrayOf(MESSAGE_SERVER_ID),
            )
            assertThat(numberOfUpdatedRows).isEqualTo(1)
        }
    }

    private fun dbOperation(action: (SQLiteDatabase) -> Unit) = database.execute(false, action)

    companion object {
        const val FOLDER_SERVER_ID = "testFolder"
        const val FOLDER_NAME = "Test Folder"
        val FOLDER_TYPE = FolderType.INBOX
        const val MESSAGE_SERVER_ID = "msg001"
    }
}
