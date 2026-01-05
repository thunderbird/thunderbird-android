package com.fsck.k9.ui.messagelist.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.k9mail.core.android.common.contact.Contact
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import com.fsck.k9.FontSizes
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.mail.EmailAddress
import net.thunderbird.core.preference.DateFormatMode
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

@Composable
@PreviewLightDark
internal fun MessageItemContentPreview() {
    PreviewWithThemesLightDark {
        MessageItemContent(
            item = fakeMessageListItem,
            isActive = true,
            isSelected = false,
            contactRepository = fakeContactRepository,
            avatarMonogramCreator = fakeAvatarMonogramCreator,
            onClick = {},
            onLongClick = {},
            onAvatarClick = {},
            onFavouriteClick = {},
            appearance = fakeMessageListAppearance,
        )
    }
}

private val accountId = AccountIdFactory.create()

private val serverSettings = ServerSettings(
    type = "imap",
    host = "imap.example.com",
    port = 993,
    connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
    authenticationType = AuthType.PLAIN,
    username = "username",
    password = "password",
    clientCertificateAlias = null,
)
private val fakeMessageListItem = MessageListItem(
    account = LegacyAccount(
        id = accountId,
        name = "Name",
        email = "test@example.com",
        profile = ProfileDto(
            id = accountId,
            name = "Name",
            color = 0xFF0000FF.toInt(),
            avatar = AvatarDto(
                avatarType = AvatarTypeDto.MONOGRAM,
                avatarMonogram = "AB",
                avatarImageUri = null,
                avatarIconName = null,
            ),
        ),
        incomingServerSettings = serverSettings,
        outgoingServerSettings = serverSettings,
        identities = listOf(Identity()),
    ),
    subject = "Subject",
    threadCount = 0,
    messageDate = 1234456789L,
    internalDate = 1234456789L,
    displayName = "Sender Name",
    displayAddress = null,
    previewText = "This is the preview text.",
    isMessageEncrypted = false,
    isRead = false,
    isStarred = false,
    isAnswered = false,
    isForwarded = false,
    hasAttachments = false,
    uniqueId = 42L,
    folderId = 123L,
    messageUid = "654321",
    databaseId = 1L,
    threadRoot = 1L,
)

private val fakeMessageListAppearance = MessageListAppearance(
    fontSizes = FontSizes(),
    previewLines = 2,
    stars = true,
    senderAboveSubject = false,
    showContactPicture = true,
    showingThreadedList = false,
    backGroundAsReadIndicator = false,
    showAccountIndicator = true,
    density = UiDensity.Default,
    dateFormatMode = DateFormatMode.ADAPTIVE,
)

private val fakeContactRepository = object : ContactRepository {
    override fun getContactFor(emailAddress: EmailAddress): Contact? {
        error("Not implemented")
    }

    override fun hasContactFor(emailAddress: EmailAddress): Boolean {
        error("Not implemented")
    }

    override fun hasAnyContactFor(emailAddresses: List<EmailAddress>): Boolean {
        error("Not implemented")
    }

    override fun getPhotoUri(emailAddress: String) = null
}

private val fakeAvatarMonogramCreator = object : AvatarMonogramCreator {
    override fun create(name: String?, email: String?) = "SE"
}
