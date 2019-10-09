package com.fsck.k9.fragment

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.fsck.k9.FontSizes
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.ContactBadge
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import kotlin.math.max

class MessageListAdapter internal constructor(
    private val context: Context,
    theme: Resources.Theme,
    private val res: Resources,
    private val layoutInflater: LayoutInflater,
    private val contactsPictureLoader: ContactPictureLoader,
    private val listItemListener: MessageListItemActionListener,
    private val appearance: MessageListAppearance
) : BaseAdapter() {

    private val forwardedIcon: Drawable
    private val answeredIcon: Drawable
    private val forwardedAnsweredIcon: Drawable
    private val previewTextColor: Int
    private val activeItemBackgroundColor: Int
    private val selectedItemBackgroundColor: Int
    private val readItemBackgroundColor: Int
    private val unreadItemBackgroundColor: Int

    init {

        val attributes = intArrayOf(
                R.attr.messageListAnswered,
                R.attr.messageListForwarded,
                R.attr.messageListAnsweredForwarded,
                R.attr.messageListPreviewTextColor,
                R.attr.messageListActiveItemBackgroundColor,
                R.attr.messageListSelectedBackgroundColor,
                R.attr.messageListReadItemBackgroundColor,
                R.attr.messageListUnreadItemBackgroundColor
        )

        val array = theme.obtainStyledAttributes(attributes)

        answeredIcon = res.getDrawable(array.getResourceId(0, R.drawable.ic_messagelist_answered_dark))
        forwardedIcon = res.getDrawable(array.getResourceId(1, R.drawable.ic_messagelist_forwarded_dark))
        forwardedAnsweredIcon = res.getDrawable(array.getResourceId(2, R.drawable.ic_messagelist_answered_forwarded_dark))
        previewTextColor = array.getColor(3, Color.BLACK)
        activeItemBackgroundColor = array.getColor(4, Color.BLACK)
        selectedItemBackgroundColor = array.getColor(5, Color.BLACK)
        readItemBackgroundColor = array.getColor(6, Color.BLACK)
        unreadItemBackgroundColor = array.getColor(7, Color.BLACK)

        array.recycle()
    }

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

    private fun recipientSigil(toMe: Boolean, ccMe: Boolean): String {
        return if (toMe) {
            res.getString(R.string.messagelist_sent_to_me_sigil)
        } else if (ccMe) {
            res.getString(R.string.messagelist_sent_cc_me_sigil)
        } else {
            ""
        }
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

        val holder = MessageViewHolder(view, listItemListener)

        holder.contactBadge.isVisible = appearance.showContactPicture
        holder.chip.isVisible = appearance.showAccountChip

        appearance.fontSizes.setViewTextSize(holder.subject, subjectViewFontSize)

        appearance.fontSizes.setViewTextSize(holder.date, appearance.fontSizes.messageListDate)

        // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
        holder.preview.setLines(max(appearance.previewLines, 1))
        appearance.fontSizes.setViewTextSize(holder.preview, appearance.fontSizes.messageListPreview)
        appearance.fontSizes.setViewTextSize(holder.threadCount, appearance.fontSizes.messageListSubject) // thread count is next to subject

        holder.flagged.isVisible = appearance.stars
        holder.flagged.setOnClickListener(holder)

        view.tag = holder

        return view
    }

    private fun bindView(view: View, context: Context, message: MessageListItem) {
        val isSelected = selected.contains(message.uniqueId)
        val isActive = isActiveMessage(message)

        val holder = view.tag as MessageViewHolder

        with(message) {
            val maybeBoldTypeface = if (isRead) Typeface.NORMAL else Typeface.BOLD
            val displayDate = DateUtils.getRelativeTimeSpanString(context, messageDate)
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
            if (holder.contactBadge.isVisible) {
                updateContactBadge(holder.contactBadge, counterPartyAddress)
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
            holder.attachment.visibility = if (hasAttachments) View.VISIBLE else View.GONE

            val statusHolder = buildStatusHolder(isForwarded, isAnswered)
            if (statusHolder != null) {
                holder.status.setImageDrawable(statusHolder)
                holder.status.visibility = View.VISIBLE
            } else {
                holder.status.visibility = View.GONE
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

    private fun updateContactBadge(contactBadge: ContactBadge, counterpartyAddress: Address?) {
        if (counterpartyAddress != null) {
            contactBadge.setContact(counterpartyAddress)
            /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but ContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */
            contactBadge.setPadding(0, 0, 0, 0)
            contactsPictureLoader.setContactPicture(contactBadge, counterpartyAddress)
        } else {
            contactBadge.assignContactUri(null)
            contactBadge.setImageResource(R.drawable.ic_contact_picture)
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
            holder.threadCount.setText(String.format("%d", threadCount))
            holder.threadCount.visibility = View.VISIBLE
        } else {
            holder.threadCount.visibility = View.GONE
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
            item.folderServerId == activeMessage.folderServerId &&
            item.messageUid == activeMessage.uid
    }
}

interface MessageListItemActionListener {
    fun toggleMessageFlagWithAdapterPosition(position: Int)
}
