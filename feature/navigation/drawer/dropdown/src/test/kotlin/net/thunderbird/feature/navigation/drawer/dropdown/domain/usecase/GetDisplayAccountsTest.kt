package net.thunderbird.feature.navigation.drawer.dropdown.domain.usecase

import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.message.controller.MessageCounts
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.avatar.Avatar
import net.thunderbird.feature.account.storage.mapper.AvatarDataMapper
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.navigation.drawer.dropdown.data.FakeMessageCountsProvider
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeFeatureFlagProvider
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream

internal class GetDisplayAccountsTest {

    @Test
    fun `should use filtered unified inbox counts for unified account`() = runTest {
        // Arrange
        val includedAccount = createAccount("00000000-0000-0000-0000-000000000001")
        val excludedAccount = createAccount("00000000-0000-0000-0000-000000000002")
        val testSubject = GetDisplayAccounts(
            accountManager = FakeLegacyAccountDtoManager(accounts = listOf(includedAccount, excludedAccount)),
            messageCountsProvider = FakeMessageCountsProvider(
                messageCounts = MessageCounts(unread = 1, starred = 2),
                accountMessageCounts = mapOf(
                    includedAccount.uuid to MessageCounts(unread = 1, starred = 2),
                    excludedAccount.uuid to MessageCounts(unread = 5, starred = 7),
                ),
            ),
            messageListRepository = FakeMessageListRepository(),
            notificationStream = FakeInAppNotificationStream(),
            featureFlagProvider = FakeFeatureFlagProvider(),
            avatarMapper = FakeAvatarDataMapper,
            unifiedFolderRepository = FakeUnifiedFolderRepository(
                flowOf(
                    UnifiedDisplayFolder(
                        id = "unified_inbox",
                        unifiedType = UnifiedDisplayFolderType.INBOX,
                        unreadMessageCount = 1,
                        starredMessageCount = 2,
                    ),
                ),
            ),
        )

        // Act
        val result = testSubject(showUnifiedAccount = true).first()

        // Assert
        assertThat(result.first()).isEqualTo(
            UnifiedDisplayAccount(
                unreadMessageCount = 1,
                starredMessageCount = 2,
                hasError = false,
            ),
        )
    }

    private fun createAccount(uuid: String): LegacyAccountDto {
        return LegacyAccountDto(uuid).apply {
            identities = mutableListOf(Identity(email = "$uuid@example.com"))
        }
    }

    private class FakeMessageListRepository : MessageListRepository {
        override fun addListener(listener: MessageListChangedListener) = Unit

        override fun addListener(accountUuid: String, listener: MessageListChangedListener) = Unit

        override fun removeListener(listener: MessageListChangedListener) = Unit

        override fun notifyMessageListChanged(accountUuid: String) = Unit

        override fun <T> getMessages(
            accountUuid: String,
            selection: String,
            selectionArgs: Array<String>,
            sortOrder: String,
            messageMapper: MessageMapper<T>,
        ): List<T> = emptyList()

        override fun <T> getThreadedMessages(
            accountUuid: String,
            selection: String,
            selectionArgs: Array<String>,
            sortOrder: String,
            messageMapper: MessageMapper<T>,
        ): List<T> = emptyList()

        override fun <T> getThread(
            accountUuid: String,
            threadId: Long,
            sortOrder: String,
            messageMapper: MessageMapper<T>,
        ): List<T> = emptyList()
    }

    private class FakeInAppNotificationStream : InAppNotificationStream {
        override val notifications: StateFlow<Set<InAppNotification>> = MutableStateFlow(emptySet())
    }

    private object FakeAvatarDataMapper : AvatarDataMapper {
        override fun toDomain(dto: AvatarDto): Avatar = Avatar.Monogram("?")

        override fun toDto(domain: Avatar): AvatarDto {
            return AvatarDto(
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "?",
                avatarImageUri = null,
                avatarIconName = null,
            )
        }
    }
}
