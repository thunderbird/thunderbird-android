package net.thunderbird.feature.mail.message.list.internal.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.organism.NewMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.ReadMessageItem
import net.thunderbird.feature.mail.message.list.ui.component.organism.UnreadMessageItem
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
    when (message.state) {
        MessageItemUi.State.New -> NewMessageItem(
            state = message,
            preferences = preferences,
            accountIndicator = if (showAccountIndicator) {
                MessageItemAccountIndicator(color = message.account.color)
            } else {
                null
            },
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteChange = { onFavouriteClick() },
            modifier = modifier.testTag(MessageListItemDefaults.NEW_MESSAGE_LIST_TEST_TAG),
        )

        MessageItemUi.State.Read -> ReadMessageItem(
            state = message,
            preferences = preferences,
            accountIndicator = if (showAccountIndicator) {
                MessageItemAccountIndicator(color = message.account.color)
            } else {
                null
            },
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteChange = { onFavouriteClick() },
            modifier = modifier.testTag(MessageListItemDefaults.READ_MESSAGE_LIST_TEST_TAG),
        )

        MessageItemUi.State.Unread -> UnreadMessageItem(
            state = message,
            preferences = preferences,
            accountIndicator = if (showAccountIndicator) {
                MessageItemAccountIndicator(color = message.account.color)
            } else {
                null
            },
            onClick = onClick,
            onLongClick = onLongClick,
            onAvatarClick = onAvatarClick,
            onFavouriteChange = { onFavouriteClick() },
            modifier = modifier.testTag(MessageListItemDefaults.UNREAD_MESSAGE_LIST_TEST_TAG),
        )
    }
}

internal object MessageListItemDefaults {
    const val NEW_MESSAGE_LIST_TEST_TAG = "NewMessageListItem_Root"
    const val READ_MESSAGE_LIST_TEST_TAG = "ReadMessageListItem_Root"
    const val UNREAD_MESSAGE_LIST_TEST_TAG = "UnreadMessageListItem_Root"
}
