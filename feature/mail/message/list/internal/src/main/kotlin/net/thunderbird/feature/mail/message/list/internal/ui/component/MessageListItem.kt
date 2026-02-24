package net.thunderbird.feature.mail.message.list.internal.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.organism.message.ActiveMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.MessageItemDefaults
import net.thunderbird.core.ui.compose.designsystem.organism.message.NewMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.ReadMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.UnreadMessageItem
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.EmailIdentity
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

@Composable
internal fun MessageListItem(
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    preferences: MessageListPreferences,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    val contentPadding = calculateContentPadding(preferences)
    val mostRecentSender = remember(message.senders) { message.senders.first() }
    val senders = rememberSendersText(message, mostRecentSender, preferences)
    when (message.state) {
        else if message.isActive -> ActiveMessageListItem(
            senders = senders,
            message = message,
            showAccountIndicator = showAccountIndicator,
            mostRecentSender = mostRecentSender,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.ACTIVE_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )

        MessageItemUi.State.New -> NewMessageListItem(
            senders = senders,
            message = message,
            showAccountIndicator = showAccountIndicator,
            mostRecentSender = mostRecentSender,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.NEW_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )

        MessageItemUi.State.Read -> ReadMessageListItem(
            senders = senders,
            message = message,
            showAccountIndicator = showAccountIndicator,
            mostRecentSender = mostRecentSender,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.READ_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )

        MessageItemUi.State.Unread -> UnreadMessageListItem(
            senders = senders,
            message = message,
            showAccountIndicator = showAccountIndicator,
            mostRecentSender = mostRecentSender,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )
    }
}

@Composable
private fun calculateContentPadding(preferences: MessageListPreferences): PaddingValues = when (preferences.density) {
    UiDensity.Compact -> MessageItemDefaults.compactContentPadding
    UiDensity.Relaxed -> MessageItemDefaults.relaxedContentPadding
    else -> MessageItemDefaults.defaultContentPadding
}

@Composable
private fun ActiveMessageListItem(
    senders: AnnotatedString,
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    mostRecentSender: EmailIdentity,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    ActiveMessageItem(
        sender = senders,
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = mostRecentSender.avatar,
                showMessageAvatar = preferences.showMessageAvatar,
                onAvatarClick = onAvatarClick,
            )
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onAvatarClick,
        onFavouriteChange = { onFavouriteClick() },
        favourite = message.starred,
        selected = message.selected,
        maxPreviewLines = preferences.excerptLines,
        threadCount = message.conversations.size,
        hasAttachments = message.attachments.isNotEmpty(),
        swapSenderWithSubject = !preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun NewMessageListItem(
    senders: AnnotatedString,
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    mostRecentSender: EmailIdentity,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    NewMessageItem(
        sender = senders,
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = mostRecentSender.avatar,
                showMessageAvatar = preferences.showMessageAvatar,
                onAvatarClick = onAvatarClick,
            )
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onAvatarClick,
        onFavouriteChange = { onFavouriteClick() },
        favourite = message.starred,
        selected = message.selected,
        maxPreviewLines = preferences.excerptLines,
        threadCount = message.conversations.size,
        hasAttachments = message.attachments.isNotEmpty(),
        swapSenderWithSubject = preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun ReadMessageListItem(
    senders: AnnotatedString,
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    mostRecentSender: EmailIdentity,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    ReadMessageItem(
        sender = senders,
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = mostRecentSender.avatar,
                showMessageAvatar = preferences.showMessageAvatar,
                onAvatarClick = onAvatarClick,
            )
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onAvatarClick,
        onFavouriteChange = { onFavouriteClick() },
        favourite = message.starred,
        selected = message.selected,
        maxPreviewLines = preferences.excerptLines,
        threadCount = message.conversations.size,
        hasAttachments = message.attachments.isNotEmpty(),
        swapSenderWithSubject = preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun UnreadMessageListItem(
    senders: AnnotatedString,
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    mostRecentSender: EmailIdentity,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    UnreadMessageItem(
        sender = senders,
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = mostRecentSender.avatar,
                showMessageAvatar = preferences.showMessageAvatar,
                onAvatarClick = onAvatarClick,
            )
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onLeadingClick = onAvatarClick,
        onFavouriteChange = { onFavouriteClick() },
        favourite = message.starred,
        selected = message.selected,
        maxPreviewLines = preferences.excerptLines,
        threadCount = message.conversations.size,
        hasAttachments = message.attachments.isNotEmpty(),
        swapSenderWithSubject = preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun rememberSendersText(
    message: MessageItemUi,
    mostRecentSender: EmailIdentity,
    preferences: MessageListPreferences,
): AnnotatedString = remember(message.senders, message.state, preferences.showCorrespondentNames) {
    buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontWeight = when (message.state) {
                    MessageItemUi.State.New, MessageItemUi.State.Unread -> FontWeight.Bold
                    else -> FontWeight.Normal
                },
            ),
        ) {
            append(mostRecentSender.senderText(preferences.showCorrespondentNames))
            if (message.senders.size > 1) append(", ")
        }
        message.senders.drop(1)
            .joinTo(this, separator = ", ") {
                it.senderText(preferences.showCorrespondentNames)
            }
    }
}

private fun EmailIdentity.senderText(showSendersName: Boolean) = if (showSendersName) {
    name
} else {
    email
}

internal object MessageListItemDefaults {
    const val ACTIVE_MESSAGE_LIST_TEST_TAG = "ActiveMessageListItem_Root"
    const val NEW_MESSAGE_LIST_TEST_TAG = "NewMessageListItem_Root"
    const val READ_MESSAGE_LIST_TEST_TAG = "ReadMessageListItem_Root"
    const val UNREAD_MESSAGE_LIST_TEST_TAG = "UnreadMessageListItem_Root"
}
