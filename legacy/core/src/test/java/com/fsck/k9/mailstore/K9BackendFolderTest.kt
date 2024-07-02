package com.fsck.k9.mailstore

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.fsck.k9.Account
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MimeMessageHelper
import com.fsck.k9.mail.internet.TextBody
import org.junit.After
import org.junit.Test
import org.koin.core.component.inject

class K9BackendFolderTest : K9RobolectricTest() {
    val preferences: Preferences by inject()
    val localStoreProvider: LocalStoreProvider by inject()
    val messageStoreManager: MessageStoreManager by inject()
    val saveMessageDataCreator: SaveMessageDataCreator by inject()

    val account: Account = createAccount()
    val backendFolder = createBackendFolder()
    val database: LockableDatabase = localStoreProvider.getInstance(account).database

    @After
    fun tearDown() {
        preferences.deleteAccount(account)
    }

    @Test
    fun getMessageFlags() {
        val flags = setOf(Flag.SEEN, Flag.DRAFT, Flag.X_DOWNLOADED_FULL)
        createMessageInBackendFolder(MESSAGE_SERVER_ID, flags)

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags).isEqualTo(flags)
    }

    @Test
    fun getMessageFlags_withFlagsColumnSetToNull_shouldBeTreatedAsEmpty() {
        createMessageInBackendFolder(MESSAGE_SERVER_ID)
        setFlagsColumnToNull()

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags.isEmpty()).isTrue()
    }

    @Test
    fun getMessageFlags_withFlagsColumnSetToNull_shouldReadSpecialColumnFlags() {
        val flags = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)
        createMessageInBackendFolder(MESSAGE_SERVER_ID, flags)
        setFlagsColumnToNull()

        val messageFlags = backendFolder.getMessageFlags(MESSAGE_SERVER_ID)

        assertThat(messageFlags).isEqualTo(flags)
    }

    @Test
    fun saveCompleteMessage_withoutServerId_shouldThrow() {
        val message = createMessage(messageServerId = null)

        assertFailure {
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Message requires a server ID to be set")
    }

    @Test
    fun savePartialMessage_withoutServerId_shouldThrow() {
        val message = createMessage(messageServerId = null)

        assertFailure {
            backendFolder.saveMessage(message, MessageDownloadState.PARTIAL)
        }.isInstanceOf<IllegalStateException>()
            .hasMessage("Message requires a server ID to be set")
    }

    fun createAccount(): Account {
        // FIXME: This is a hack to get Preferences into a state where it's safe to call newAccount()
        preferences.clearAccounts()

        return preferences.newAccount()
    }

    fun createBackendFolder(): BackendFolder {
        val messageStore = messageStoreManager.getMessageStore(account)
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

    fun createMessageInBackendFolder(messageServerId: String, flags: Set<Flag> = emptySet()) {
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
