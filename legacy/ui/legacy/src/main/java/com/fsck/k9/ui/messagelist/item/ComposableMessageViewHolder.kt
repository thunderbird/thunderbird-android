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
                )
            }
        }
    }

    companion object {

        fun create(
            context: Context,
            themeProvider: FeatureThemeProvider,
        ): ComposableMessageViewHolder {
            val composeView = ComposeView(context)

            val holder = ComposableMessageViewHolder(
                composeView = composeView,
                themeProvider = themeProvider,
            )

            composeView.tag = holder

            return holder
        }
    }
}
