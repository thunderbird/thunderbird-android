package net.thunderbird.feature.mail.message.list.domain.usecase

import assertk.all
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import kotlin.random.Random
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.BackgroundSync
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.feature.mail.message.list.fakes.FakeAccount
import net.thunderbird.feature.mail.message.list.fakes.FakeAccountManager

@OptIn(ExperimentalUuidApi::class)
@Suppress("MaxLineLength")
class BuildSwipeActionsTest {
    private val defaultGeneralSettings
        get() = GeneralSettings(
            backgroundSync = BackgroundSync.NEVER,
            showRecentChanges = false,
            appTheme = AppTheme.FOLLOW_SYSTEM,
            messageViewTheme = SubTheme.USE_GLOBAL,
            messageComposeTheme = SubTheme.USE_GLOBAL,
            fixedMessageViewTheme = false,
            isShowUnifiedInbox = false,
            isShowStarredCount = false,
            isShowMessageListStars = false,
            isShowAnimations = false,
            isShowCorrespondentNames = false,
            shouldShowSetupArchiveFolderDialog = false,
            isMessageListSenderAboveSubject = false,
        )

    @Test
    fun `invoke should return empty map when empty account uuids is provided`() {
        // Arrange
        val uuids = setOf<String>()
        val testSubject = createTestSubject(
            accountsUuids = List(size = 10) { Uuid.random().toHexString() },
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).isEmpty()
    }

    @Test
    fun `invoke should return map with SwipeActions(ToggleRead, ToggleRead) when no user preference is stored`() {
        // Arrange
        val uuid = Uuid.random().toHexString()
        val uuids = setOf(uuid)
        val testSubject = createTestSubject(
            accountsUuids = uuids.toList(),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 1)
            containsOnly(
                uuid to SwipeActions(
                    leftAction = SwipeAction.ToggleRead,
                    rightAction = SwipeAction.ToggleRead,
                ),
            )
        }
    }

    @Test
    fun `invoke should return map with multiple keys when multiple accounts`() {
        // Arrange
        val accountsSize = 10
        val uuids = List(size = accountsSize) { Uuid.random().toHexString() }
        val testSubject = createTestSubject(
            accountsUuids = uuids.toList(),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids.toSet(),
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).all {
            hasSize(accountsSize)
            containsOnly(
                elements = uuids
                    .associateWith {
                        SwipeActions(
                            leftAction = SwipeAction.ToggleRead,
                            rightAction = SwipeAction.ToggleRead,
                        )
                    }
                    .map { it.key to it.value }
                    .toTypedArray(),
            )
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(None, ToggleRead) when left action is stored as None but right is not`() {
        // Arrange
        val uuid = Uuid.random().toHexString()
        val uuids = setOf(uuid)
        val testSubject = createTestSubject(
            accountsUuids = uuids.toList(),
            storageValues = mapOf(
                SwipeActions.KEY_SWIPE_ACTION_LEFT to SwipeAction.None.name,
            ),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 1)
            containsOnly(
                uuid to SwipeActions(
                    leftAction = SwipeAction.None,
                    rightAction = SwipeAction.ToggleRead,
                ),
            )
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(ToggleRead, Delete) when left action is not stored but right is stored as Delete`() {
        // Arrange
        val uuid = Uuid.random().toHexString()
        val uuids = setOf(uuid)
        val testSubject = createTestSubject(
            accountsUuids = uuids.toList(),
            storageValues = mapOf(
                SwipeActions.KEY_SWIPE_ACTION_RIGHT to SwipeAction.Delete.name,
            ),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 1)
            containsOnly(
                uuid to SwipeActions(
                    leftAction = SwipeAction.ToggleRead,
                    rightAction = SwipeAction.Delete,
                ),
            )
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(Archive, Archive) when both stored actions are Archive, account isn't pop3 and has archive folder`() {
        // Arrange
        val uuid = Uuid.random().toHexString()
        val uuids = setOf(uuid)
        val testSubject = createTestSubject(
            accountsUuids = uuids.toList(),
            storageValues = mapOf(
                SwipeActions.KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive.name,
                SwipeActions.KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive.name,
            ),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { true },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 1)
            containsOnly(
                uuid to SwipeActions(
                    leftAction = SwipeAction.Archive,
                    rightAction = SwipeAction.Archive,
                ),
            )
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(ArchiveDisabled, ArchiveDisabled) when both stored actions are Archive, account is pop3`() {
        // Arrange
        val uuid = Uuid.random().toHexString()
        val uuids = setOf(uuid)
        val testSubject = createTestSubject(
            accountsUuids = uuids.toList(),
            storageValues = mapOf(
                SwipeActions.KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive.name,
                SwipeActions.KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive.name,
            ),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { true },
            hasArchiveFolder = { true },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 1)
            containsOnly(
                uuid to SwipeActions(
                    leftAction = SwipeAction.ArchiveDisabled,
                    rightAction = SwipeAction.ArchiveDisabled,
                ),
            )
        }
    }

    @Test
    fun `invoke should return map with SwipeActions(ArchiveSetupArchiveFolder, ArchiveSetupArchiveFolder) when both stored actions are Archive, account isn't pop3, has not archive folder and shouldShowSetupArchiveFolderDialog is true`() {
        // Arrange
        val uuid = Uuid.random().toHexString()
        val uuids = setOf(uuid)
        val testSubject = createTestSubject(
            initialGeneralSettings = defaultGeneralSettings.copy(shouldShowSetupArchiveFolderDialog = true),
            accountsUuids = uuids.toList(),
            storageValues = mapOf(
                SwipeActions.KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive.name,
                SwipeActions.KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive.name,
            ),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 1)
            containsOnly(
                uuid to SwipeActions(
                    leftAction = SwipeAction.ArchiveSetupArchiveFolder,
                    rightAction = SwipeAction.ArchiveSetupArchiveFolder,
                ),
            )
        }
    }

    @Test
    fun `invoke should return map with different SwipeAction Archive when multiple accounts that includes pop3 accounts or accounts without archive folder`() {
        // Arrange
        val uuidPop3 = "pop3-account"
        val uuidWithoutArchiveFolder = "no-archive-folder-account"
        val uuidWithArchiveFolder = "archive-folder-account"
        val uuids = setOf(
            uuidPop3,
            uuidWithoutArchiveFolder,
            uuidWithArchiveFolder,
        )
        val testSubject = createTestSubject(
            initialGeneralSettings = defaultGeneralSettings.copy(shouldShowSetupArchiveFolderDialog = true),
            accountsUuids = uuids.toList(),
            storageValues = mapOf(
                SwipeActions.KEY_SWIPE_ACTION_LEFT to SwipeAction.Archive.name,
                SwipeActions.KEY_SWIPE_ACTION_RIGHT to SwipeAction.Archive.name,
            ),
        )

        // Act
        val actions = testSubject(
            accountUuids = uuids,
            isIncomingServerPop3 = { it.uuid == uuidPop3 },
            hasArchiveFolder = { it.uuid == uuidWithArchiveFolder },
        )

        // Assert
        assertThat(actions).all {
            hasSize(size = 3)
            containsOnly(
                uuidPop3 to SwipeActions(
                    leftAction = SwipeAction.ArchiveDisabled,
                    rightAction = SwipeAction.ArchiveDisabled,
                ),
                uuidWithoutArchiveFolder to SwipeActions(
                    leftAction = SwipeAction.ArchiveSetupArchiveFolder,
                    rightAction = SwipeAction.ArchiveSetupArchiveFolder,
                ),
                uuidWithArchiveFolder to SwipeActions(
                    leftAction = SwipeAction.Archive,
                    rightAction = SwipeAction.Archive,
                ),
            )
        }
    }

    @Test
    fun `invoke should return empty map when account uuid doesn't exists in AccountManager`() {
        // Arrange
        val uuids = List(size = Random.nextInt(from = 1, until = 100)) { Uuid.random().toHexString() }
        val accountManagerUuids =
            List(size = Random.nextInt(from = 1, until = 100)) { Uuid.random().toHexString() } - uuids
        val testSubject = createTestSubject(accountsUuids = accountManagerUuids)

        // Act
        val actions = testSubject(
            accountUuids = uuids.toSet(),
            isIncomingServerPop3 = { false },
            hasArchiveFolder = { false },
        )

        // Assert
        assertThat(actions).isEmpty()
    }

    private fun createTestSubject(
        initialGeneralSettings: GeneralSettings = defaultGeneralSettings,
        accountsUuids: List<String>,
        storageValues: Map<String, String> = mapOf(),
    ): BuildSwipeActions = BuildSwipeActions(
        generalSettingsManager = FakeGeneralSettingsManager(initialGeneralSettings),
        accountManager = FakeAccountManager(accounts = accountsUuids.map { FakeAccount(uuid = it) }),
        storage = FakeStorage(storageValues),
    )
}

private class FakeGeneralSettingsManager(
    initialGeneralSettings: GeneralSettings,
) : GeneralSettingsManager {
    private val generalSettings = MutableStateFlow(initialGeneralSettings)
    override fun getSettings(): GeneralSettings = generalSettings.value

    override fun getSettingsFlow(): Flow<GeneralSettings> = generalSettings

    override fun setShowRecentChanges(showRecentChanges: Boolean) = error("not implemented")

    override fun setAppTheme(appTheme: AppTheme) = error("not implemented")

    override fun setMessageViewTheme(subTheme: SubTheme) = error("not implemented")

    override fun setMessageComposeTheme(subTheme: SubTheme) = error("not implemented")

    override fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean) = error("not implemented")

    override fun setIsShowUnifiedInbox(isShowUnifiedInbox: Boolean) = error("not implemented")

    override fun setIsShowStarredCount(isShowStarredCount: Boolean) = error("not implemented")

    override fun setIsShowMessageListStars(isShowMessageListStars: Boolean) = error("not implemented")

    override fun setIsShowAnimations(isShowAnimations: Boolean) = error("not implemented")

    override fun setIsShowCorrespondentNames(isShowCorrespondentNames: Boolean) = error("not implemented")

    override fun setSetupArchiveShouldNotShowAgain(shouldShowSetupArchiveFolderDialog: Boolean) {
        generalSettings.update { it.copy(shouldShowSetupArchiveFolderDialog = shouldShowSetupArchiveFolderDialog) }
    }

    override fun setIsMessageListSenderAboveSubject(isMessageListSenderAboveSubject: Boolean) = error("not implemented")
}

private class FakeStorage(
    private val values: Map<String, String>,
) : Storage {
    override fun isEmpty(): Boolean = error("not implemented")

    override fun contains(key: String): Boolean = error("not implemented")

    override fun getAll(): Map<String, String> = error("not implemented")

    override fun getBoolean(key: String, defValue: Boolean): Boolean = error("not implemented")

    override fun getInt(key: String, defValue: Int): Int = error("not implemented")

    override fun getLong(key: String, defValue: Long): Long = error("not implemented")

    override fun getString(key: String): String = error("not implemented")

    override fun getStringOrDefault(key: String, defValue: String): String = error("not implemented")

    override fun getStringOrNull(key: String): String? = values[key]
}
