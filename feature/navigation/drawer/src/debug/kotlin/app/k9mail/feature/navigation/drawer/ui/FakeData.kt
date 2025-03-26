package app.k9mail.feature.navigation.drawer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolderType
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.account.LegacyAccount
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal object FakeData {

    const val ACCOUNT_UUID = "uuid"
    const val DISPLAY_NAME = "Account Name"
    const val EMAIL_ADDRESS = "test@example.com"

    const val LONG_TEXT = "loremipsumdolorsitametconsetetursadipscingelitr" +
        "seddiamnonumyeirmodtemporinviduntutlaboreetdoloremagnaaliquyameratseddiamvoluptua"

    val ACCOUNT = LegacyAccount(
        uuid = ACCOUNT_UUID,
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

    val DISPLAY_ACCOUNT = DisplayAccount(
        id = ACCOUNT_UUID,
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

    val DISPLAY_FOLDER = DisplayAccountFolder(
        accountId = ACCOUNT_UUID,
        folder = FOLDER,
        isInTopGroup = false,
        unreadMessageCount = 14,
        starredMessageCount = 5,
    )

    val UNIFIED_FOLDER = DisplayUnifiedFolder(
        id = "unified_inbox",
        unifiedType = DisplayUnifiedFolderType.INBOX,
        unreadMessageCount = 123,
        starredMessageCount = 567,
    )

    fun createAccountList(): PersistentList<DisplayAccount> {
        return persistentListOf(
            DisplayAccount(
                id = "account1",
                name = "job@example.com",
                email = "job@example.com",
                color = Color.Green.toArgb(),
                unreadMessageCount = 2,
                starredMessageCount = 0,
            ),
            DisplayAccount(
                id = "account2",
                name = "Jodie Doe",
                email = "jodie@example.com",
                color = Color.Red.toArgb(),
                unreadMessageCount = 12,
                starredMessageCount = 0,
            ),
            DisplayAccount(
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
