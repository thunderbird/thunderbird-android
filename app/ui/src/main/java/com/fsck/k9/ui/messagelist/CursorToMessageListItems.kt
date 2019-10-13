package com.fsck.k9.ui.messagelist

import android.content.res.Resources
import android.database.Cursor
import com.fsck.k9.Preferences
import com.fsck.k9.fragment.MessageListItemExtractor
import com.fsck.k9.helper.MessageHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class CursorToMessageListItems(
        private val preferences: Preferences,
        private val helper: MessageHelper,
        private val resources: Resources
) {

    private fun messageListItemExtractor(cursor: Cursor) = MessageListItemExtractor(
            preferences,
            cursor,
            helper,
            resources
    )

    fun convert(cursor: Cursor, onItemsReady: (List<MessageListItem>) -> Unit) {
        val extractor = messageListItemExtractor(cursor)
        GlobalScope.launch {
            async {
                val items = arrayListOf<MessageListItem>()
                while (cursor.moveToNext()) {
                    items += extractor.asItem
                }
                onItemsReady(items)
            }
        }
    }

    private inline val MessageListItemExtractor.asItem: MessageListItem
        get() = MessageListItem(
                this.displayName.toString(),
                this.subject(this.threadCount),
                this.date,
                this.sigil,
                this.threadCount,
                this.read,
                this.answered,
                this.forwarded,
                false,
                this.chipColor,
                this.flagged,
                this.hasAttachments,
                this.preview
        )

}