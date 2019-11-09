package com.fsck.k9.ui.messagelist

import android.content.res.Resources
import android.database.Cursor
import com.fsck.k9.Preferences
import com.fsck.k9.fragment.MessageListItemExtractor
import com.fsck.k9.helper.MessageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CursorToMessageListItems(
        private val preferences: Preferences,
        private val helper: MessageHelper,
        private val resources: Resources,
        private val uniqueColumnId: Int
) {

    private fun messageListItemExtractor(cursor: Cursor) = MessageListItemExtractor(
            preferences,
            cursor,
            helper,
            resources
    )

    fun convert(cursor: Cursor, onItemsReady: ConvertedMessagesReady) {
        val extractor = messageListItemExtractor(cursor)
        GlobalScope.launch(Dispatchers.Main) {
            val items = async {
                val items = arrayListOf<MessageListItem>()
                while (cursor.moveToNext()) {
                    items += extractor.asItem
                }
                items
            }
            onItemsReady.onItemsReady(items.await())
        }
    }

    private inline val MessageListItemExtractor.asItem: MessageListItem
        get() = MessageListItem(
                this.id,
                this.uid,
                this.folderServerId.orEmpty(),
                this.displayName.toString(),
                this.subject(this.threadCount),
                this.date,
                this.sigil,
                this.threadCount,
                this.threadRootId,
                this.read,
                this.answered,
                this.forwarded,
                this.chipColor,
                this.flagged,
                this.hasAttachments,
                this.preview,
                this.counterPartyAddresses,
                this.account,
                this.selectionIdentifier(uniqueColumnId)

        )
}

interface ConvertedMessagesReady {
    fun onItemsReady(list: List<MessageListItem>)
}