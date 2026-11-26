package com.fsck.k9.ui.messagelist

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import org.junit.Test

private const val THREAD_ROOT = 42L
private const val FOLDER_ID = 1L

class MessageListLoaderTest {

    @Test
    fun `mergeActiveMessage should replace the thread entry with the active message item`() {
        // Arrange
        val account = createLegacyAccount()
        val threadEntry = createMessageListItem(account = account, messageUid = "newest", threadRoot = THREAD_ROOT)
        val activeMessageItem = createMessageListItem(account = account, messageUid = "older", threadRoot = THREAD_ROOT)
        val activeMessage = MessageReference(account.id, FOLDER_ID, "older")
        val messageList = listOf(threadEntry, createMessageListItem(account = account, messageUid = "other"))

        // Act
        val result = messageList.mergeActiveMessage(activeMessage, activeMessageItem)

        // Assert
        assertThat(result).isEqualTo(
            listOf(
                activeMessageItem.copy(threadCount = threadEntry.threadCount),
                createMessageListItem(account = account, messageUid = "other"),
            ),
        )
    }

    @Test
    fun `mergeActiveMessage should return the list unchanged when the active message is already present`() {
        // Arrange
        val account = createLegacyAccount()
        val activeMessageItem = createMessageListItem(account = account, messageUid = "older", threadRoot = THREAD_ROOT)
        val activeMessage = MessageReference(account.id, FOLDER_ID, "older")
        val messageList = listOf(
            createMessageListItem(account = account, messageUid = "newest", threadRoot = THREAD_ROOT),
            activeMessageItem,
        )

        // Act
        val result = messageList.mergeActiveMessage(activeMessage, activeMessageItem)

        // Assert
        assertThat(result).isSameAs(messageList)
    }

    @Test
    fun `mergeActiveMessage should return the list unchanged when the thread is not part of the list`() {
        // Arrange
        val account = createLegacyAccount()
        val activeMessageItem = createMessageListItem(account = account, messageUid = "older", threadRoot = 99L)
        val activeMessage = MessageReference(account.id, FOLDER_ID, "older")
        val messageList = listOf(
            createMessageListItem(account = account, messageUid = "newest", threadRoot = THREAD_ROOT),
        )

        // Act
        val result = messageList.mergeActiveMessage(activeMessage, activeMessageItem)

        // Assert
        assertThat(result).isSameAs(messageList)
    }

    private fun createLegacyAccount(): LegacyAccount {
        return LegacyAccount(
            id = ACCOUNT_ID,
            name = "irrelevant",
            email = "irrelevant@example.com",
            profile = createProfile(),
            incomingServerSettings = createServerSettings(),
            outgoingServerSettings = createServerSettings(),
            identities = listOf(
                Identity(
                    description = null,
                    name = "irrelevant",
                    email = "irrelevant@example.com",
                ),
            ),
        )
    }

    private fun createProfile(): ProfileDto {
        return ProfileDto(
            id = ACCOUNT_ID,
            name = "irrelevant",
            color = 0xFF00FF,
            avatar = AvatarDto(
                accountId = ACCOUNT_ID,
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "ab",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        )
    }

    private fun createServerSettings(): ServerSettings {
        return ServerSettings(
            type = "imap",
            host = "irrelevant",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "irrelevant",
            password = "irrelevant",
            clientCertificateAlias = null,
        )
    }

    private fun createMessageListItem(
        account: LegacyAccount,
        messageUid: String,
        threadRoot: Long = 0L,
    ): MessageListItem {
        return MessageListItem(
            account = account,
            subject = "irrelevant",
            threadCount = 3,
            messageDate = 0L,
            internalDate = 0L,
            displayName = "irrelevant",
            displayAddress = Address.parse("irrelevant@domain.example").first(),
            displayMessageDateTime = "12:34",
            previewText = "irrelevant",
            isMessageEncrypted = false,
            isRead = false,
            isStarred = false,
            isAnswered = false,
            isForwarded = false,
            hasAttachments = false,
            uniqueId = messageUid.hashCode().toLong(),
            folderId = FOLDER_ID,
            messageUid = messageUid,
            databaseId = 0L,
            threadRoot = threadRoot,
            contactColor = -1,
        )
    }
}
