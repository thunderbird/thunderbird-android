package com.fsck.k9.fragment


import android.content.Context
import android.content.res.Resources
import android.database.Cursor
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
import android.widget.CursorAdapter
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.fsck.k9.FontSizes
import com.fsck.k9.Preferences
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.ContactBadge
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.MessageListAppearance
import kotlin.math.max

class MessageListAdapter internal constructor(
        context: Context,
        theme: Resources.Theme,
        private val res: Resources,
        private val layoutInflater: LayoutInflater,
        private val messageHelper: MessageHelper,
        private val contactsPictureLoader: ContactPictureLoader,
        private val preferences: Preferences,
        private val listItemListener: MessageListItemActionListener,
        private val appearance: MessageListAppearance
) : CursorAdapter(context, null, 0) {

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

    var activeMessage: MessageReference? = null

    var uniqueIdColumn: Int = 0

    var selected: Set<Long> = emptySet()

    private inline val subjectViewFontSize: Int
        get() = if (appearance.senderAboveSubject) {
            appearance.fontSizes.messageListSender
        } else {
            appearance.fontSizes.messageListSubject
        }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
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

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val itemExtractor = getExtractor(view, cursor)
        val holder = view.tag as MessageViewHolder

        val displayName = itemExtractor.displayName
        val displayDate = DateUtils.getRelativeTimeSpanString(context, itemExtractor.date)
        val threadCount = if (appearance.showingThreadedList) itemExtractor.threadCount else 0
        val subject = itemExtractor.subject(threadCount)
        val read = itemExtractor.read
        val answered = itemExtractor.answered
        val forwarded = itemExtractor.forwarded
        val maybeBoldTypeface = if (read) Typeface.NORMAL else Typeface.BOLD
        val uniqueId = cursor.getLong(uniqueIdColumn)
        val selected = selected.contains(uniqueId)

        if (appearance.showAccountChip) {
            val accountChipDrawable = holder.chip.drawable.mutate()
            DrawableCompat.setTint(accountChipDrawable, itemExtractor.chipColor)
            holder.chip.setImageDrawable(accountChipDrawable)
        }

        holder.flagged.isChecked = appearance.stars && itemExtractor.flagged

        holder.position = cursor.position
        if (holder.contactBadge.isVisible) {
            val counterpartyAddress = itemExtractor.counterPartyAddresses
            updateContactBadge(holder.contactBadge, counterpartyAddress)
        }
        setBackgroundColor(view, selected, read)
        if (itemExtractor.isActiveMessage(activeMessage)) {
            view.setBackgroundColor(activeItemBackgroundColor)
        }

        holder.threadCount.isVisible = threadCount > 1
        if (holder.threadCount.isVisible) {
            holder.threadCount.text = "%d".format(threadCount)
        }

        val beforePreviewText = if (appearance.senderAboveSubject) subject else displayName
        val sigil = itemExtractor.sigil
        val messageStringBuilder = SpannableStringBuilder(sigil)
                .append(beforePreviewText)
        if (appearance.previewLines > 0) {
            val preview = itemExtractor.preview
            messageStringBuilder.append(" ").append(preview)
        }
        holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE)

        formatPreviewText(holder.preview, beforePreviewText, sigil, read)

        holder.subject.typeface = Typeface.create(holder.subject.typeface, maybeBoldTypeface)
        if (appearance.senderAboveSubject) {
            holder.subject.text = displayName
        } else {
            holder.subject.text = subject
        }

        holder.date.text = displayDate
        holder.attachment.isVisible = itemExtractor.hasAttachments

        holder.status.isVisible = answered || forwarded
        if (holder.status.isVisible) {
            holder.status.setImageDrawable(getStatusDrawable(forwarded, answered))
        }
    }

    private fun getExtractor(view: View, cursor: Cursor): MessageListItemExtractor {
        if (view.getTag(EXTRACTOR) == null
                || (view.getTag(EXTRACTOR) as MessageListItemExtractor).cursor != cursor) {
            val extractor = MessageListItemExtractor(preferences, cursor, messageHelper, res)
            view.setTag(EXTRACTOR, extractor)
            return extractor
        }
        return view.getTag(EXTRACTOR) as MessageListItemExtractor
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

    private fun getStatusDrawable(forwarded: Boolean, answered: Boolean): Drawable? {
        if (forwarded && answered) {
            return forwardedAnsweredIcon
        } else if (answered) {
            return answeredIcon
        } else if (forwarded) {
            return forwardedIcon
        }
        return null
    }

    private fun setBackgroundColor(view: View, selected: Boolean, read: Boolean) {
        if (selected || appearance.backGroundAsReadIndicator) {
            val color: Int
            if (selected) {
                color = selectedItemBackgroundColor
            } else if (read) {
                color = readItemBackgroundColor
            } else {
                color = unreadItemBackgroundColor
            }

            view.setBackgroundColor(color)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    companion object {
        private val EXTRACTOR = R.id.message_list_item_extractor
    }
}

interface MessageListItemActionListener {
    fun toggleMessageFlagWithAdapterPosition(position: Int)
}
