package com.fsck.k9.fragment

import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.fsck.k9.FontSizes
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import com.fsck.k9.ui.resolveColorAttribute
import com.fsck.k9.ui.resolveDrawableAttribute
import kotlin.math.max

class MessageListAdapter internal constructor(
    private val context: Context,
    theme: Theme,
    private val res: Resources,
    private val layoutInflater: LayoutInflater,
    private val contactsPictureLoader: ContactPictureLoader,
    private val listItemListener: MessageListItemActionListener,
    private val appearance: MessageListAppearance,
    private val relativeDateTimeFormatter: RelativeDateTimeFormatter
) : BaseAdapter() {

    private val forwardedIcon: Drawable = theme.resolveDrawableAttribute(R.attr.messageListForwarded)
    private val answeredIcon: Drawable = theme.resolveDrawableAttribute(R.attr.messageListAnswered)
    private val forwardedAnsweredIcon: Drawable = theme.resolveDrawableAttribute(R.attr.messageListAnsweredForwarded)
    private val previewTextColor: Int = theme.resolveColorAttribute(R.attr.messageListPreviewTextColor)
    private val activeItemBackgroundColor: Int = theme.resolveColorAttribute(R.attr.messageListActiveItemBackgroundColor)
    private val selectedItemBackgroundColor: Int = theme.resolveColorAttribute(R.attr.messageListSelectedBackgroundColor)
    private val readItemBackgroundColor: Int = theme.resolveColorAttribute(R.attr.messageListReadItemBackgroundColor)
    private val unreadItemBackgroundColor: Int = theme.resolveColorAttribute(R.attr.messageListUnreadItemBackgroundColor)

    var messages: List<MessageListItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var activeMessage: MessageReference? = null

    var selected: Set<Long> = emptySet()

    private inline val subjectViewFontSize: Int
        get() = if (appearance.senderAboveSubject) {
            appearance.fontSizes.messageListSender
        } else {
            appearance.fontSizes.messageListSubject
        }

    private val flagClickListener = OnClickListener { view: View ->
        val messageViewHolder = view.tag as MessageViewHolder
        val messageListItem = getItem(messageViewHolder.position)
        listItemListener.onToggleMessageFlag(messageListItem)
    }

    private val contactPictureClickListener = OnClickListener { view: View ->
        val parentView = view.parent.parent as View
        val messageViewHolder = parentView.tag as MessageViewHolder
        val messageListItem = getItem(messageViewHolder.position)
        listItemListener.onToggleMessageSelection(messageListItem)
    }

    private fun recipientSigil(toMe: Boolean, ccMe: Boolean) = when {
        toMe -> res.getString(R.string.messagelist_sent_to_me_sigil)
        ccMe -> res.getString(R.string.messagelist_sent_cc_me_sigil)
        else -> ""
    }

    override fun hasStableIds(): Boolean = true

    override fun getCount(): Int = messages.size

    override fun getItemId(position: Int): Long = messages[position].uniqueId

    override fun getItem(position: Int): MessageListItem = messages[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val message = getItem(position)
        val view: View = convertView ?: newView(parent)
        bindView(view, context, message)

        return view
    }

    private fun newView(parent: ViewGroup?): View {
        val view = layoutInflater.inflate(R.layout.message_list_item, parent, false)

        val holder = MessageViewHolder(view)

        view.findViewById<View>(R.id.contact_picture_container).isVisible = appearance.showContactPicture
        holder.contactPicture.setOnClickListener(contactPictureClickListener)

        holder.chip.isVisible = appearance.showAccountChip

        appearance.fontSizes.setViewTextSize(holder.subject, subjectViewFontSize)

        appearance.fontSizes.setViewTextSize(holder.date, appearance.fontSizes.messageListDate)

        // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
        holder.preview.setLines(max(appearance.previewLines, 1))
        appearance.fontSizes.setViewTextSize(holder.preview, appearance.fontSizes.messageListPreview)
        appearance.fontSizes.setViewTextSize(holder.threadCount, appearance.fontSizes.messageListSubject) // thread count is next to subject

        holder.flagged.isVisible = appearance.stars
        holder.flagged.tag = holder
        holder.flagged.setOnClickListener(flagClickListener)

        view.tag = holder

        return view
    }

    private fun bindView(view: View, context: Context, message: MessageListItem) {
        val isSelected = selected.contains(message.uniqueId)
        val isActive = isActiveMessage(message)

        val holder = view.tag as MessageViewHolder

        if (appearance.showContactPicture) {
            if (isSelected) {
                holder.contactPicture.isVisible = false
                holder.selected.isVisible = true
            } else {
                holder.selected.isVisible = false
                holder.contactPicture.isVisible = true
            }
        }

        with(message) {
            val maybeBoldTypeface = if (isRead) Typeface.NORMAL else Typeface.BOLD
            val displayDate = relativeDateTimeFormatter.formatDate(messageDate)
            val displayThreadCount = if (appearance.showingThreadedList) threadCount else 0
            val subject = MlfUtils.buildSubject(subject, res.getString(R.string.general_no_subject), displayThreadCount)

            if (appearance.showAccountChip) {
                val accountChipDrawable = holder.chip.drawable.mutate()
                DrawableCompat.setTint(accountChipDrawable, account.chipColor)
                holder.chip.setImageDrawable(accountChipDrawable)
            }

            if (appearance.stars) {
                holder.flagged.isChecked = isStarred
            }
            holder.position = position
            if (appearance.showContactPicture && holder.contactPicture.isVisible) {
                setContactPicture(holder.contactPicture, counterPartyAddress)
            }
            setBackgroundColor(view, isSelected, isRead, isActive)
            updateWithThreadCount(holder, displayThreadCount)
            val beforePreviewText = if (appearance.senderAboveSubject) subject else displayName
            val sigil = recipientSigil(toMe, ccMe)
            val messageStringBuilder = SpannableStringBuilder(sigil)
                    .append(beforePreviewText)
            if (appearance.previewLines > 0) {
                val preview = getPreview(isMessageEncrypted, previewText)
                messageStringBuilder.append(" ").append(preview)
            }
            holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE)

            formatPreviewText(holder.preview, beforePreviewText, sigil, isRead)

            holder.subject.typeface = Typeface.create(holder.subject.typeface, maybeBoldTypeface)
            if (appearance.senderAboveSubject) {
                holder.subject.text = displayName
            } else {
                holder.subject.text = subject
            }

            holder.date.text = displayDate
            holder.attachment.isVisible = hasAttachments

            val statusHolder = buildStatusHolder(isForwarded, isAnswered)
            if (statusHolder != null) {
                holder.status.setImageDrawable(statusHolder)
                holder.status.isVisible = true
            } else {
                holder.status.isVisible = false
            }
        }
    }

    private fun formatPreviewText(
        preview: TextView,
        beforePreviewText: CharSequence,
        sigil: String,
        messageRead: Boolean
    ) {
        val previewText = preview.text as Spannable

        val beforePreviewLength = beforePreviewText.length + sigil.length
        addBeforePreviewSpan(previewText, beforePreviewLength, messageRead)

        // Set span (color) for preview message
        previewText.setSpan(
                ForegroundColorSpan(previewTextColor),
                beforePreviewLength,
                previewText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun addBeforePreviewSpan(text: Spannable, length: Int, messageRead: Boolean) {
        val fontSize = if (appearance.senderAboveSubject) {
            appearance.fontSizes.messageListSubject
        } else {
            appearance.fontSizes.messageListSender
        }

        if (fontSize != FontSizes.FONT_DEFAULT) {
            val span = AbsoluteSizeSpan(fontSize, true)
            text.setSpan(span, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (!messageRead) {
            val span = StyleSpan(Typeface.BOLD)
            text.setSpan(span, 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun setContactPicture(contactPictureView: ImageView, counterpartyAddress: Address?) {
        if (counterpartyAddress != null) {
            contactsPictureLoader.setContactPicture(contactPictureView, counterpartyAddress)
        } else {
            contactPictureView.setImageResource(R.drawable.ic_contact_picture)
        }
    }

    private fun buildStatusHolder(forwarded: Boolean, answered: Boolean): Drawable? {
        if (forwarded && answered) {
            return forwardedAnsweredIcon
        } else if (answered) {
            return answeredIcon
        } else if (forwarded) {
            return forwardedIcon
        }
        return null
    }

    private fun setBackgroundColor(view: View, selected: Boolean, read: Boolean, active: Boolean) {
        val backGroundAsReadIndicator = appearance.backGroundAsReadIndicator
        val backgroundColor = when {
            active -> activeItemBackgroundColor
            selected -> selectedItemBackgroundColor
            backGroundAsReadIndicator && read -> readItemBackgroundColor
            backGroundAsReadIndicator && !read -> unreadItemBackgroundColor
            else -> Color.TRANSPARENT
        }

        view.setBackgroundColor(backgroundColor)
    }

    private fun updateWithThreadCount(holder: MessageViewHolder, threadCount: Int) {
        if (threadCount > 1) {
            holder.threadCount.text = String.format("%d", threadCount)
            holder.threadCount.isVisible = true
        } else {
            holder.threadCount.isVisible = false
        }
    }

    private fun getPreview(isMessageEncrypted: Boolean, previewText: String): String {
        return if (isMessageEncrypted) {
            res.getString(R.string.preview_encrypted)
        } else {
            previewText
        }
    }

    private fun isActiveMessage(item: MessageListItem): Boolean {
        val activeMessage = this.activeMessage ?: return false

        return item.account.uuid == activeMessage.accountUuid &&
            item.folderId == activeMessage.folderId &&
            item.messageUid == activeMessage.uid
    }
}

interface MessageListItemActionListener {
    fun onToggleMessageSelection(item: MessageListItem)
    fun onToggleMessageFlag(item: MessageListItem)
}
