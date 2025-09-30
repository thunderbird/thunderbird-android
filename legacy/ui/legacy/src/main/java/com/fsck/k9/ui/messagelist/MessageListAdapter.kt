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
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter
import com.fsck.k9.ui.messagelist.MessageListFeatureFlags.UseComposeForMessageListItems
import com.fsck.k9.ui.messagelist.item.BannerInlineListInAppNotificationViewHolder
import com.fsck.k9.ui.messagelist.item.ComposableMessageViewHolder
import com.fsck.k9.ui.messagelist.item.FooterViewHolder
import com.fsck.k9.ui.messagelist.item.MessageListViewHolder
import com.fsck.k9.ui.messagelist.item.MessageViewHolder
import com.fsck.k9.ui.messagelist.item.MessageViewHolderColors
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.notification.api.receiver.InAppNotificationEvent
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

private const val FOOTER_ID = 1L
private const val IN_APP_NOTIFICATION_BANNER_INLINE_LIST_ID = -1L

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
    private val themeProvider: FeatureThemeProvider,
    private val featureFlagProvider: FeatureFlagProvider,
    private val contactRepository: ContactRepository,
    private val avatarMonogramCreator: AvatarMonogramCreator,
) : RecyclerView.Adapter<MessageListViewHolder>() {

    val colors: MessageViewHolderColors = MessageViewHolderColors.resolveColors(theme)

    var viewItems: List<MessageListViewItem> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            val oldMessageList = field

            field = value
            val messages = value.filterMessageListItem()
            accountUuids = messages.map { it.account.uuid }.toSet()
            messagesMap = messages.associateBy { it.uniqueId }

            if (selected.isNotEmpty()) {
                val uniqueIds = messagesMap.keys
                selected = selected.intersect(uniqueIds)
            }

            val diffResult = DiffUtil.calculateDiff(
                MessageListDiffCallback(oldMessageList = oldMessageList, newMessageList = value),
            )
            diffResult.dispatchUpdatesTo(this)
        }

    private val messages get() = viewItems.filterMessageListItem()
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

    override fun getItemCount(): Int = viewItems.size

    override fun getItemId(position: Int): Long {
        return viewItems[position].viewId
    }

    override fun getItemViewType(position: Int): Int {
        return viewItems[position].viewType
    }

    private fun getItem(position: Int): MessageListItem = (viewItems[position] as MessageListViewItem.Message).item

    fun getItemById(uniqueId: Long): MessageListItem? {
        return messagesMap[uniqueId]
    }

    fun getItem(messageReference: MessageReference): MessageListItem? {
        return viewItems
            .filterMessageListItem()
            .firstOrNull {
                it.account.uuid == messageReference.accountUuid &&
                    it.folderId == messageReference.folderId &&
                    it.messageUid == messageReference.uid
            }
    }

    fun getPosition(messageListItem: MessageListItem): Int? {
        return viewItems
            .map { (it as? MessageListViewItem.Message)?.item }
            .indexOf(messageListItem).takeIf { it != -1 }
    }

    private fun getPosition(messageReference: MessageReference?): Int? {
        if (messageReference == null) return null

        return viewItems
            .map { (it as? MessageListViewItem.Message)?.item }
            .indexOfFirst {
                it != null &&
                    messageReference.equals(it.account.uuid, it.folderId, it.messageUid)
            }
            .takeIf { it != -1 }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageListViewHolder {
        return when (viewType) {
            TYPE_MESSAGE -> {
                val result = featureFlagProvider.provide(UseComposeForMessageListItems)
                if (result.isEnabled()) {
                    createComposableMessageViewHolder(parent)
                } else {
                    createMessageViewHolder(parent)
                }
            }

            TYPE_FOOTER -> FooterViewHolder.create(layoutInflater, parent, footerClickListener)

            TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST if isInAppNotificationEnabled ->
                BannerInlineListInAppNotificationViewHolder(
                    view = ComposeView(context = parent.context),
                    eventFilter = listItemListener::filterInAppNotificationEvents,
                )

            else -> error("Unsupported type: $viewType")
        }
    }

    private fun createMessageViewHolder(parent: ViewGroup): MessageViewHolder =
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

    private fun createComposableMessageViewHolder(parent: ViewGroup): MessageListViewHolder =
        ComposableMessageViewHolder.create(
            context = parent.context,
            themeProvider = themeProvider,
            contactRepository = contactRepository,
            avatarMonogramCreator = avatarMonogramCreator,
            onClick = { listItemListener.onMessageClicked(it) },
            onLongClick = { listItemListener.onToggleMessageSelection(it) },
            onFavouriteClick = { listItemListener.onToggleMessageFlag(it) },
            onAvatarClick = { listItemListener.onToggleMessageSelection(it) },
            appearance = appearance,
        )

    override fun onBindViewHolder(holder: MessageListViewHolder, position: Int) {
        when (val viewType = getItemViewType(position)) {
            TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST if isInAppNotificationEnabled ->
                (holder as BannerInlineListInAppNotificationViewHolder).bind()

            TYPE_MESSAGE -> {
                val messageListItem = getItem(position)
                val result = featureFlagProvider.provide(UseComposeForMessageListItems)
                if (result.isEnabled()) {
                    val messageViewHolder = holder as ComposableMessageViewHolder
                    messageViewHolder.bind(
                        item = messageListItem,
                        isActive = isActiveMessage(messageListItem),
                        isSelected = isSelected(messageListItem),
                    )
                } else {
                    val messageViewHolder = holder as MessageViewHolder
                    messageViewHolder.bind(
                        messageListItem = messageListItem,
                        isActive = isActiveMessage(messageListItem),
                        isSelected = isSelected(messageListItem),
                    )
                }
            }

            TYPE_FOOTER -> {
                val footerViewHolder = holder as FooterViewHolder
                val footer = viewItems[position] as MessageListViewItem.Footer
                footerViewHolder.bind(footer.text)
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
        return when {
            selected.isEmpty() -> 0
            !appearance.showingThreadedList -> selected.size
            else ->
                viewItems
                    .asSequence()
                    .filterIsInstance<MessageListViewItem.Message>()
                    .map { it.item }
                    .filter { it.uniqueId in selected }
                    .sumOf { it.threadCount.coerceAtLeast(1) }
        }
    }

    private fun getItemFromView(view: View): MessageListItem? {
        if (featureFlagProvider.provide(UseComposeForMessageListItems).isEnabled()) {
            val messageViewHolder = view.tag as ComposableMessageViewHolder
            return getItemById(messageViewHolder.uniqueId)
        } else {
            val messageViewHolder = view.tag as MessageViewHolder
            return getItemById(messageViewHolder.uniqueId)
        }
    }

    private fun List<MessageListViewItem>.filterMessageListItem(): List<MessageListItem> =
        filterIsInstance<MessageListViewItem.Message>()
            .map { it.item }
}

private class MessageListDiffCallback(
    private val oldMessageList: List<MessageListViewItem>,
    private val newMessageList: List<MessageListViewItem>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldMessageList.size

    override fun getNewListSize(): Int = newMessageList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldMessageList[oldItemPosition]
        val newItem = newMessageList[newItemPosition]
        return when (oldItem) {
            is MessageListViewItem.InAppNotificationBannerList
            if newItem is MessageListViewItem.InAppNotificationBannerList -> true

            is MessageListViewItem.Message
            if newItem is MessageListViewItem.Message -> oldItem.item.uniqueId == newItem.item.uniqueId

            is MessageListViewItem.Footer if newItem is MessageListViewItem.Footer -> true
            else -> false
        }
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
    fun filterInAppNotificationEvents(event: InAppNotificationEvent): Boolean
}

sealed interface MessageListViewItem {
    val viewId: Long
    val viewType: Int

    data object InAppNotificationBannerList : MessageListViewItem {
        override val viewId: Long = IN_APP_NOTIFICATION_BANNER_INLINE_LIST_ID
        override val viewType: Int = TYPE_IN_APP_NOTIFICATION_BANNER_INLINE_LIST
    }

    data class Message(val item: MessageListItem) : MessageListViewItem {
        override val viewId: Long get() = item.uniqueId
        override val viewType: Int = TYPE_MESSAGE
    }

    data class Footer(val text: String) : MessageListViewItem {
        override val viewId: Long = FOOTER_ID
        override val viewType: Int = TYPE_FOOTER
    }
}
