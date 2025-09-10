package com.fsck.k9.ui.messagelist.item

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import com.fsck.k9.ui.messagelist.MessageListItem
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

/**
 * A composable view holder for message list items.
 */
class ComposableMessageViewHolder(
    private val composeView: ComposeView,
    private val themeProvider: FeatureThemeProvider,
    private val onClick: (MessageListItem) -> Unit,
    private val onLongClick: (MessageListItem) -> Unit,
    private val onAvatarClick: (MessageListItem) -> Unit,
    private val onFavouriteClick: (MessageListItem) -> Unit,
) : MessageListViewHolder(composeView) {

    var uniqueId: Long = -1L

    fun bind(item: MessageListItem, isActive: Boolean, isSelected: Boolean) {
        uniqueId = item.uniqueId

        composeView.setContent {
            themeProvider.WithTheme {
                MessageItemContent(
                    item = item,
                    isActive = isActive,
                    isSelected = isSelected,
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(item) },
                    onAvatarClick = { onAvatarClick(item) },
                    onFavouriteClick = { onFavouriteClick(item) },
                )
            }
        }
    }

    companion object {

        fun create(
            context: Context,
            themeProvider: FeatureThemeProvider,
            onClick: (MessageListItem) -> Unit,
            onLongClick: (MessageListItem) -> Unit,
            onFavouriteClick: (MessageListItem) -> Unit,
            onAvatarClick: (MessageListItem) -> Unit,
        ): ComposableMessageViewHolder {
            val composeView = ComposeView(context)

            val holder = ComposableMessageViewHolder(
                composeView = composeView,
                themeProvider = themeProvider,
                onClick = onClick,
                onLongClick = onLongClick,
                onAvatarClick = onAvatarClick,
                onFavouriteClick = onFavouriteClick,
            )

            composeView.tag = holder

            return holder
        }
    }
}
