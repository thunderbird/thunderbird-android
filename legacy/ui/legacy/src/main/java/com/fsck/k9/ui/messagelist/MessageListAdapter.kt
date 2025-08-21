package com.fsck.k9.ui.messagelist

import android.annotation.SuppressLint
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter
import com.fsck.k9.ui.messagelist.item.BannerInlineListInAppNotificationViewHolder
import com.fsck.k9.ui.messagelist.item.FooterViewHolder
import com.fsck.k9.ui.messagelist.item.MessageListViewHolder
import com.fsck.k9.ui.messagelist.item.MessageViewHolder
import com.fsck.k9.ui.messagelist.item.MessageViewHolderColors
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult

private const val FOOTER_ID = 1L

private const val TYPE_MESSAGE = 0
private const val TYPE_FOOTER = 1
private const val TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST = 2

@Suppress("LongParameterList")
class MessageListAdapter internal constructor(
    private val theme: Theme,
    private val res: Resources,
    private val layoutInflater: LayoutInflater,
    private val contactsPictureLoader: ContactPictureLoader,
    private val listItemListener: MessageListItemActionListener,
    private val appearance: MessageListAppearance,
    private val relativeDateTimeFormatter: RelativeDateTimeFormatter,
    private val featureFlagProvider: FeatureFlagProvider,
) : RecyclerView.Adapter<MessageListViewHolder>() {

    val colors: MessageViewHolderColors = MessageViewHolderColors.resolveColors(theme)

    var messages: List<MessageListItem> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            val oldMessageList = field

            field = value
            accountUuids = value.map { it.account.uuid }.toSet()
            messagesMap = value.associateBy { it.uniqueId }

            if (selected.isNotEmpty()) {
                val uniqueIds = messagesMap.keys
                selected = selected.intersect(uniqueIds)
            }

            val diffResult = DiffUtil.calculateDiff(
                MessageListDiffCallback(oldMessageList = oldMessageList, newMessageList = value),
            )
            diffResult.dispatchUpdatesTo(this)
        }

    private var messagesMap = emptyMap<Long, MessageListItem>()
    private var accountUuids = emptySet<String>()

    var activeMessage: MessageReference? = null
        set(value) {
            if (value == field) return

            val oldPosition = getPosition(field)
            val newPosition = getPosition(value)

            field = value

            oldPosition?.let { position -> notifyItemChanged(position) }
            newPosition?.let { position -> notifyItemChanged(position) }
        }

    var selected: Set<Long> = emptySet()
        private set(value) {
            if (value == field) return

            // Selection removed
            field.asSequence()
                .filter { uniqueId -> uniqueId !in value }
                .mapNotNull { uniqueId -> messagesMap[uniqueId] }
                .mapNotNull { messageListItem -> getPosition(messageListItem) }
                .forEach { position ->
                    notifyItemChanged(position)
                }

            // Selection added
            value.asSequence()
                .filter { uniqueId -> uniqueId !in field }
                .mapNotNull { uniqueId -> messagesMap[uniqueId] }
                .mapNotNull { messageListItem -> getPosition(messageListItem) }
                .forEach { position ->
                    notifyItemChanged(position)
                }

            field = value
            selectedCount = calculateSelectionCount()
        }

    val selectedMessages: List<MessageListItem>
        get() = selected.map { messagesMap[it]!! }

    val isAllSelected: Boolean
        get() = selected.isNotEmpty() && selected.size == messages.size

    var selectedCount: Int = 0
        private set

    var footerText: String? = null
        set(value) {
            if (field == value) return

            val hadFooterText = field != null
            val previousFooterPosition = footerPosition
            field = value

            if (hadFooterText) {
                if (value == null) {
                    notifyItemRemoved(previousFooterPosition)
                } else {
                    notifyItemChanged(footerPosition)
                }
            } else {
                notifyItemInserted(footerPosition)
            }
        }

    private val hasFooter: Boolean
        get() = footerText != null

    private val lastMessagePosition: Int
        get() = messages.lastIndex

    private val footerPosition: Int
        get() = if (hasFooter) lastMessagePosition + 1 else NO_POSITION

    private val messageClickedListener = OnClickListener { view: View ->
        val messageListItem = getItemFromView(view) ?: return@OnClickListener
        listItemListener.onMessageClicked(messageListItem)
    }

    private val messageLongClickedListener = OnLongClickListener { view: View ->
        getItemFromView(view)?.let { messageListItem ->
            listItemListener.onToggleMessageSelection(messageListItem)
        }
        true
    }

    private val footerClickListener = OnClickListener {
        listItemListener.onFooterClicked()
    }

    private val starClickListener = OnClickListener { view: View ->
        val parentView = view.parent as View
        val messageListItem = getItemFromView(parentView) ?: return@OnClickListener
        listItemListener.onToggleMessageFlag(messageListItem)
    }

    private val contactPictureContainerClickListener = OnClickListener { view: View ->
        val parentView = view.parent as View
        val messageListItem = getItemFromView(parentView) ?: return@OnClickListener
        listItemListener.onToggleMessageSelection(messageListItem)
    }

    private val isInAppNotificationEnabled: Boolean
        get() = featureFlagProvider.provide(FeatureFlagKey.DisplayInAppNotifications) == FeatureFlagResult.Enabled

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = messages.size + if (hasFooter) 1 else 0

    override fun getItemId(position: Int): Long {
        return if (position <= lastMessagePosition) {
            messages[position].uniqueId
        } else {
            FOOTER_ID
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 && isInAppNotificationEnabled -> TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST
            position <= lastMessagePosition -> TYPE_MESSAGE
            else -> TYPE_FOOTER
        }
    }

    private fun getItem(position: Int): MessageListItem = messages[position]

    fun getItemById(uniqueId: Long): MessageListItem? {
        return messagesMap[uniqueId]
    }

    fun getItem(messageReference: MessageReference): MessageListItem? {
        return messages.firstOrNull {
            it.account.uuid == messageReference.accountUuid &&
                it.folderId == messageReference.folderId &&
                it.messageUid == messageReference.uid
        }
    }

    fun getPosition(messageListItem: MessageListItem): Int? {
        return messages.indexOf(messageListItem).takeIf { it != -1 }
    }

    private fun getPosition(messageReference: MessageReference?): Int? {
        if (messageReference == null) return null

        return messages.indexOfFirst {
            messageReference.equals(it.account.uuid, it.folderId, it.messageUid)
        }.takeIf { it != -1 }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageListViewHolder {
        return when (viewType) {
            TYPE_MESSAGE -> createMessageViewHolder(parent)
            TYPE_FOOTER -> FooterViewHolder.create(layoutInflater, parent, footerClickListener)
            TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST if isInAppNotificationEnabled ->
                BannerInlineListInAppNotificationViewHolder(
                    view = ComposeView(context = parent.context),
                    eventFilter = { event ->
                        val accountUuid = event.notification.accountUuid
                        accountUuid != null && accountUuid in accountUuids
                    },
                )

            else -> error("Unsupported type: $viewType")
        }
    }

    private fun createMessageViewHolder(parent: ViewGroup?): MessageViewHolder =
        MessageViewHolder.create(
            layoutInflater = layoutInflater,
            parent = parent,
            appearance = appearance,
            res = res,
            contactsPictureLoader = contactsPictureLoader,
            relativeDateTimeFormatter = relativeDateTimeFormatter,
            colors = colors,
            theme = theme,
            onClickListener = messageClickedListener,
            onLongClickListener = messageLongClickedListener,
            contactPictureContainerClickListener = contactPictureContainerClickListener,
            starClickListener = starClickListener,
        )

    override fun onBindViewHolder(holder: MessageListViewHolder, position: Int) {
        when (val viewType = getItemViewType(position)) {
            TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST if isInAppNotificationEnabled ->
                (holder as BannerInlineListInAppNotificationViewHolder).bind()

            TYPE_MESSAGE -> {
                val messageListItem = getItem(position)
                val messageViewHolder = holder as MessageViewHolder
                messageViewHolder.bind(
                    messageListItem = messageListItem,
                    isActive = isActiveMessage(messageListItem),
                    isSelected = isSelected(messageListItem),
                )
            }

            TYPE_FOOTER -> {
                val footerViewHolder = holder as FooterViewHolder
                footerViewHolder.bind(footerText)
            }

            else -> {
                error("Unsupported type: $viewType")
            }
        }
    }

    private fun isActiveMessage(item: MessageListItem): Boolean {
        val activeMessage = this.activeMessage ?: return false

        return item.account.uuid == activeMessage.accountUuid &&
            item.folderId == activeMessage.folderId &&
            item.messageUid == activeMessage.uid
    }

    fun isSelected(item: MessageListItem): Boolean {
        return item.uniqueId in selected
    }

    fun toggleSelection(item: MessageListItem) {
        if (messagesMap[item.uniqueId] == null) {
            // MessageListItem is no longer in the list
            return
        }

        if (item.uniqueId in selected) {
            deselectMessage(item)
        } else {
            selectMessage(item)
        }
    }

    fun selectMessage(item: MessageListItem) {
        selected = selected + item.uniqueId
    }

    fun deselectMessage(item: MessageListItem) {
        selected = selected - item.uniqueId
    }

    fun selectAll() {
        val uniqueIds = messagesMap.keys.toSet()
        selected = uniqueIds
    }

    fun clearSelected() {
        selected = emptySet()
    }

    fun restoreSelected(selectedIds: Set<Long>) {
        if (selectedIds.isEmpty()) {
            clearSelected()
        } else {
            val uniqueIds = messagesMap.keys
            selected = selectedIds.intersect(uniqueIds)
        }
    }

    private fun calculateSelectionCount(): Int {
        if (selected.isEmpty()) {
            return 0
        }

        if (!appearance.showingThreadedList) {
            return selected.size
        }

        return messages
            .asSequence()
            .filter { it.uniqueId in selected }
            .sumOf { it.threadCount.coerceAtLeast(1) }
    }

    private fun getItemFromView(view: View): MessageListItem? {
        val messageViewHolder = view.tag as MessageViewHolder
        return getItemById(messageViewHolder.uniqueId)
    }
}

private class MessageListDiffCallback(
    private val oldMessageList: List<MessageListItem>,
    private val newMessageList: List<MessageListItem>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldMessageList.size

    override fun getNewListSize(): Int = newMessageList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldMessageList[oldItemPosition].uniqueId == newMessageList[newItemPosition].uniqueId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldMessageList[oldItemPosition] == newMessageList[newItemPosition]
    }
}

interface MessageListItemActionListener {
    fun onMessageClicked(messageListItem: MessageListItem)
    fun onToggleMessageSelection(item: MessageListItem)
    fun onToggleMessageFlag(item: MessageListItem)
    fun onFooterClicked()
}
