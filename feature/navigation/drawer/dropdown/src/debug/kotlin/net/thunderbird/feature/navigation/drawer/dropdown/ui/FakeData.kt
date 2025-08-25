package net.thunderbird.feature.navigation.drawer.dropdown.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType

internal object FakeData {

    const val DISPLAY_NAME = "Account Name"
    const val EMAIL_ADDRESS = "test@example.com"

    val ACCOUNT = LegacyAccount(
        uuid = ACCOUNT_ID_RAW,
    ).apply {
        identities = ArrayList()

        val identity = Identity(
            signatureUse = false,
            signature = "",
            description = "",
        )
        identities.add(identity)

        name = DISPLAY_NAME
        email = EMAIL_ADDRESS
    }

    val UNIFIED_DISPLAY_ACCOUNT = UnifiedDisplayAccount(
        unreadMessageCount = 224,
        starredMessageCount = 42,
    )

    val MAIL_DISPLAY_ACCOUNT = MailDisplayAccount(
        id = ACCOUNT_ID_RAW,
        name = DISPLAY_NAME,
        email = EMAIL_ADDRESS,
        color = Color.Red.toArgb(),
        unreadMessageCount = 0,
        starredMessageCount = 0,
    )

    val FOLDER = Folder(
        id = 1,
        name = "Folder Name",
        type = FolderType.REGULAR,
        isLocalOnly = false,
    )

    val DISPLAY_FOLDER = MailDisplayFolder(
        accountId = ACCOUNT_ID_RAW,
        folder = FOLDER,
        isInTopGroup = false,
        unreadMessageCount = 14,
        starredMessageCount = 5,
    )

    val DISPLAY_TREE_FOLDER = DisplayTreeFolder(
        displayFolder = null,
        displayName = null,
        totalUnreadCount = 14,
        totalStarredCount = 5,
        children = persistentListOf(
            DisplayTreeFolder(
                displayFolder = DISPLAY_FOLDER,
                displayName = DISPLAY_FOLDER.folder.name,
                totalUnreadCount = 14,
                totalStarredCount = 5,
                children = persistentListOf(),
            ),
        ),
    )

    val EMPTY_DISPLAY_TREE_FOLDER = DisplayTreeFolder(
        displayFolder = null,
        displayName = null,
        totalUnreadCount = 0,
        totalStarredCount = 0,
        children = persistentListOf(),
    )

    val UNIFIED_FOLDER = UnifiedDisplayFolder(
        id = "unified_inbox",
        unifiedType = UnifiedDisplayFolderType.INBOX,
        unreadMessageCount = 123,
        starredMessageCount = 567,
    )

    val DISPLAY_TREE_FOLDER_WITH_UNIFIED_FOLDER = DisplayTreeFolder(
        displayFolder = null,
        displayName = null,
        totalUnreadCount = 14,
        totalStarredCount = 5,
        children = persistentListOf(
            DisplayTreeFolder(
                displayFolder = UNIFIED_FOLDER,
                displayName = null,
                totalUnreadCount = 7,
                totalStarredCount = 2,
                children = persistentListOf(),
            ),
            DisplayTreeFolder(
                displayFolder = DISPLAY_FOLDER,
                displayName = DISPLAY_FOLDER.folder.name,
                totalUnreadCount = 7,
                totalStarredCount = 3,
                children = persistentListOf(),
            ),
        ),
    )

    val DISPLAY_TREE_FOLDER_WITH_NESTED_FOLDERS = DisplayTreeFolder(
        displayFolder = null,
        displayName = null,
        totalUnreadCount = 14,
        totalStarredCount = 5,
        children = persistentListOf(
            DisplayTreeFolder(
                displayFolder = DISPLAY_FOLDER,
                displayName = DISPLAY_FOLDER.folder.name,
                totalUnreadCount = 7,
                totalStarredCount = 3,
                children = persistentListOf(
                    DisplayTreeFolder(
                        displayFolder = null,
                        displayName = null,
                        totalUnreadCount = 7,
                        totalStarredCount = 3,
                        children = persistentListOf(),
                    ),
                ),
            ),
        ),
    )

    fun createAccountList(): PersistentList<MailDisplayAccount> {
        return persistentListOf(
            MailDisplayAccount(
                id = "account1",
                name = "job@example.com",
                email = "job@example.com",
                color = Color.Green.toArgb(),
                unreadMessageCount = 2,
                starredMessageCount = 0,
            ),
            MailDisplayAccount(
                id = "account2",
                name = "Jodie Doe",
                email = "jodie@example.com",
                color = Color.Red.toArgb(),
                unreadMessageCount = 12,
                starredMessageCount = 0,
            ),
            MailDisplayAccount(
                id = "account3",
                name = "John Doe",
                email = "john@example.com",
                color = Color.Cyan.toArgb(),
                unreadMessageCount = 0,
                starredMessageCount = 0,
            ),
        )
    }

    fun createDisplayFolderList(hasUnifiedFolder: Boolean): PersistentList<DisplayFolder> {
        val folders = mutableListOf<DisplayFolder>()

        if (hasUnifiedFolder) {
            folders.add(UNIFIED_FOLDER)
        }

        folders.addAll(
            listOf(
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 2, name = "Inbox", type = FolderType.INBOX),
                    unreadMessageCount = 12,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 3, name = "Outbox", type = FolderType.OUTBOX),
                    unreadMessageCount = 0,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 4, name = "Drafts", type = FolderType.DRAFTS),
                    unreadMessageCount = 0,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 5, name = "Sent", type = FolderType.SENT),
                    unreadMessageCount = 0,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 6, name = "Spam", type = FolderType.SPAM),
                    unreadMessageCount = 5,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 7, name = "Trash", type = FolderType.TRASH),
                    unreadMessageCount = 0,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 8, name = "Archive", type = FolderType.ARCHIVE),
                    unreadMessageCount = 0,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 9, name = "Work", type = FolderType.REGULAR),
                    unreadMessageCount = 3,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 10, name = "Personal", type = FolderType.REGULAR),
                    unreadMessageCount = 4,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 11, name = "Important", type = FolderType.REGULAR),
                    unreadMessageCount = 0,
                ),
                DISPLAY_FOLDER.copy(
                    folder = FOLDER.copy(id = 12, name = "Later", type = FolderType.REGULAR),
                    unreadMessageCount = 0,
                ),
            ),
        )

        return folders.toPersistentList()
    }
}
