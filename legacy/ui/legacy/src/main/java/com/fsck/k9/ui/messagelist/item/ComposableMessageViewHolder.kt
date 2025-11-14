package com.fsck.k9.ui.messagelist.item

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import app.k9mail.core.android.common.contact.ContactRepository
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator

/**
 * A composable view holder for message list items.
 */
@Suppress("LongParameterList")
class ComposableMessageViewHolder(
    private val composeView: ComposeView,
    private val themeProvider: FeatureThemeProvider,
    private val onClick: (MessageListItem) -> Unit,
    private val onLongClick: (MessageListItem) -> Unit,
    private val onAvatarClick: (MessageListItem) -> Unit,
    private val onFavouriteClick: (MessageListItem) -> Unit,
    private val appearance: MessageListAppearance,
    private val contactRepository: ContactRepository,
    private val avatarMonogramCreator: AvatarMonogramCreator,
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
                    contactRepository = contactRepository,
                    avatarMonogramCreator = avatarMonogramCreator,
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(item) },
                    onAvatarClick = { onAvatarClick(item) },
                    onFavouriteClick = { onFavouriteClick(item) },
                    appearance = appearance,
                )
            }
        }
    }

    companion object {
        @Suppress("LongParameterList")
        fun create(
            context: Context,
            themeProvider: FeatureThemeProvider,
            contactRepository: ContactRepository,
            avatarMonogramCreator: AvatarMonogramCreator,
            onClick: (MessageListItem) -> Unit,
            onLongClick: (MessageListItem) -> Unit,
            onFavouriteClick: (MessageListItem) -> Unit,
            onAvatarClick: (MessageListItem) -> Unit,
            appearance: MessageListAppearance,
        ): ComposableMessageViewHolder {
            val composeView = ComposeView(context)

            val holder = ComposableMessageViewHolder(
                composeView = composeView,
                themeProvider = themeProvider,
                contactRepository = contactRepository,
                avatarMonogramCreator = avatarMonogramCreator,
                onClick = onClick,
                onLongClick = onLongClick,
                onAvatarClick = onAvatarClick,
                onFavouriteClick = onFavouriteClick,
                appearance = appearance,
            )

            composeView.tag = holder

            return holder
        }
    }
}
