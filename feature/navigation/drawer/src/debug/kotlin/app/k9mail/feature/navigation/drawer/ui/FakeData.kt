package app.k9mail.feature.navigation.drawer.ui

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.legacy.account.Account
import app.k9mail.legacy.account.Identity

internal object FakeData {

    const val ACCOUNT_UUID = "uuid"
    const val DISPLAY_NAME = "Account Name"
    const val EMAIL_ADDRESS = "test@example.com"

    const val LONG_TEXT = "loremipsumdolorsitametconsetetursadipscingelitr" +
        "seddiamnonumyeirmodtemporinviduntutlaboreetdoloremagnaaliquyameratseddiamvoluptua"

    val ACCOUNT = Account(
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
        account = ACCOUNT,
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
        accountUuid = ACCOUNT_UUID,
        folder = FOLDER,
        isInTopGroup = false,
        unreadMessageCount = 14,
        starredMessageCount = 5,
    )
}
