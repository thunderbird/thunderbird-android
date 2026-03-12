package net.thunderbird.feature.mail.message.list.internal.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.organism.ActiveMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.MessageItemDefaults
import net.thunderbird.feature.mail.message.list.ui.component.organism.NewMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.ReadMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.UnreadMessageItem
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
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
    when (message.state) {
        else if message.isActive -> ActiveMessageListItem(
            message = message,
            showAccountIndicator = showAccountIndicator,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.ACTIVE_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )

        MessageItemUi.State.New -> NewMessageListItem(
            message = message,
            showAccountIndicator = showAccountIndicator,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.NEW_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )

        MessageItemUi.State.Read -> ReadMessageListItem(
            message = message,
            showAccountIndicator = showAccountIndicator,
            preferences = preferences,
            contentPadding = contentPadding,
            modifier = modifier.testTagAsResourceId(MessageListItemDefaults.READ_MESSAGE_LIST_TEST_TAG),
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteClick = onFavouriteClick,
        )

        MessageItemUi.State.Unread -> UnreadMessageListItem(
            message = message,
            showAccountIndicator = showAccountIndicator,
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
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    ActiveMessageItem(
        sender = styledSenderOrSubject(
            useSender = true,
            senders = message.senders,
            subject = AnnotatedString(message.subject),
        ),
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = message.senders.avatar ?: return@ActiveMessageItem,
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
        threadCount = message.threadCount,
        hasAttachments = message.hasAttachments,
        swapSenderWithSubject = !preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun NewMessageListItem(
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    NewMessageItem(
        sender = styledSenderOrSubject(
            useSender = true,
            senders = message.senders,
            subject = AnnotatedString(message.subject),
        ),
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = message.senders.avatar ?: return@NewMessageItem,
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
        threadCount = message.threadCount,
        hasAttachments = message.hasAttachments,
        swapSenderWithSubject = preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun ReadMessageListItem(
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    ReadMessageItem(
        sender = styledSenderOrSubject(
            useSender = true,
            senders = message.senders,
            subject = AnnotatedString(message.subject),
        ),
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = message.senders.avatar ?: return@ReadMessageItem,
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
        threadCount = message.threadCount,
        hasAttachments = message.hasAttachments,
        swapSenderWithSubject = preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun UnreadMessageListItem(
    message: MessageItemUi,
    showAccountIndicator: Boolean,
    preferences: MessageListPreferences,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
) {
    UnreadMessageItem(
        sender = styledSenderOrSubject(
            useSender = true,
            senders = message.senders,
            subject = AnnotatedString(message.subject),
        ),
        subject = message.subject,
        preview = message.excerpt,
        receivedAt = message.formattedReceivedAt,
        showAccountIndicator = showAccountIndicator,
        accountIndicatorColor = message.account.color,
        avatar = {
            MessageItemAvatar(
                avatar = message.senders.avatar ?: return@UnreadMessageItem,
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
        threadCount = message.threadCount,
        hasAttachments = message.hasAttachments,
        swapSenderWithSubject = preferences.senderAboveSubject,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
private fun styledSenderOrSubject(
    useSender: Boolean,
    senders: ComposedAddressUi,
    subject: AnnotatedString,
    prefix: AnnotatedString? = null,
    forceRegularFontWeight: Boolean = false,
): AnnotatedString = buildAnnotatedString {
    prefix?.let { append(it) }
    val text = if (useSender) senders.displayName else subject
    append(text)
    when {
        forceRegularFontWeight -> {
            addStyle(SpanStyle(fontWeight = FontWeight.Normal), 0, text.length)
        }

        useSender -> {
            senders.displayNameStyles.forEach { style ->
                when (style) {
                    ComposedAddressStyle.AllBold -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start = 0,
                        end = text.length,
                    )

                    is ComposedAddressStyle.Bold -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        style.start,
                        style.end ?: text.length,
                    )

                    is ComposedAddressStyle.Regular -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Normal),
                        style.start,
                        style.end ?: text.length,
                    )
                }
            }
        }
    }
}

internal object MessageListItemDefaults {
    const val ACTIVE_MESSAGE_LIST_TEST_TAG = "ActiveMessageListItem_Root"
    const val NEW_MESSAGE_LIST_TEST_TAG = "NewMessageListItem_Root"
    const val READ_MESSAGE_LIST_TEST_TAG = "ReadMessageListItem_Root"
    const val UNREAD_MESSAGE_LIST_TEST_TAG = "UnreadMessageListItem_Root"
}
