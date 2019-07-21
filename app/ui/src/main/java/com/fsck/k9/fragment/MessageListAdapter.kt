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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.ui.R
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.DatabasePreviewType
import com.fsck.k9.ui.ContactBadge

import com.fsck.k9.fragment.MLFProjectionInfo.ANSWERED_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.CC_LIST_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.DATE_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.FLAGGED_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.FOLDER_SERVER_ID_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.FORWARDED_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_TYPE_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.READ_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.SUBJECT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.THREAD_COUNT_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.TO_LIST_COLUMN
import com.fsck.k9.fragment.MLFProjectionInfo.UID_COLUMN
import com.fsck.k9.helper.MessageHelper

import kotlin.math.max

class MessageListAdapter internal constructor(
        theme: Resources.Theme,
        private val res: Resources,
        private val fragment: MessageListFragment,
        private val layoutInflater: LayoutInflater,
        private val messageHelper: MessageHelper
) : CursorAdapter(fragment.activity, null, 0) {
    private val mForwardedIcon: Drawable
    private val mAnsweredIcon: Drawable
    private val mForwardedAnsweredIcon: Drawable
    private val previewTextColor: Int
    private val activeItemBackgroundColor: Int
    private val selectedItemBackgroundColor: Int
    private val readItemBackgroundColor: Int
    private val unreadItemBackgroundColor: Int
    private val fontSizes = K9.fontSizes

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

        mAnsweredIcon = res.getDrawable(array.getResourceId(0, R.drawable.ic_messagelist_answered_dark))
        mForwardedIcon = res.getDrawable(array.getResourceId(1, R.drawable.ic_messagelist_forwarded_dark))
        mForwardedAnsweredIcon = res.getDrawable(array.getResourceId(2, R.drawable.ic_messagelist_answered_forwarded_dark))
        previewTextColor = array.getColor(3, Color.BLACK)
        activeItemBackgroundColor = array.getColor(4, Color.BLACK)
        selectedItemBackgroundColor = array.getColor(5, Color.BLACK)
        readItemBackgroundColor = array.getColor(6, Color.BLACK)
        unreadItemBackgroundColor = array.getColor(7, Color.BLACK)

        array.recycle()
    }

    private inline val previewLines: Int
        get() = K9.messageListPreviewLines

    private val senderAboveSubject: Boolean
        get() = K9.isMessageListSenderAboveSubject


    private fun recipientSigil(toMe: Boolean, ccMe: Boolean): String {
        return if (toMe) {
            res.getString(R.string.messagelist_sent_to_me_sigil)
        } else if (ccMe) {
            res.getString(R.string.messagelist_sent_cc_me_sigil)
        } else {
            ""
        }
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val view = layoutInflater.inflate(R.layout.message_list_item, parent, false)

        val holder = MessageViewHolder(fragment)
        holder.date = view.findViewById(R.id.date)
        holder.chip = view.findViewById(R.id.chip)
        holder.attachment = view.findViewById(R.id.attachment)
        holder.status = view.findViewById(R.id.status)
        holder.preview = view.findViewById(R.id.preview)
        holder.flagged = view.findViewById(R.id.star)

        val contactBadge = view.findViewById<ContactBadge>(R.id.contact_badge)
        if (fragment.contactsPictureLoader != null) {
            holder.contactBadge = contactBadge
        } else {
            contactBadge.visibility = View.GONE
        }

        if (senderAboveSubject) {
            holder.from = view.findViewById(R.id.subject)
            fontSizes.setViewTextSize(holder.from, fontSizes.messageListSender)

        } else {
            holder.subject = view.findViewById(R.id.subject)
            fontSizes.setViewTextSize(holder.subject, fontSizes.messageListSubject)

        }

        fontSizes.setViewTextSize(holder.date, fontSizes.messageListDate)


        // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
        holder.preview.setLines(max(previewLines, 1))
        fontSizes.setViewTextSize(holder.preview, fontSizes.messageListPreview)
        holder.threadCount = view.findViewById(R.id.thread_count)
        fontSizes.setViewTextSize(holder.threadCount, fontSizes.messageListSubject) // thread count is next to subject
        view.findViewById<View>(R.id.selected_checkbox_wrapper).visibility = if (fragment.checkboxes) View.VISIBLE else View.GONE

        holder.flagged.visibility = if (fragment.stars) View.VISIBLE else View.GONE
        holder.flagged.setOnClickListener(holder)


        holder.selected = view.findViewById(R.id.selected_checkbox)
        holder.selected.setOnClickListener(holder)


        view.tag = holder

        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val account = fragment.getAccountFromCursor(cursor)

        val fromList = cursor.getString(SENDER_LIST_COLUMN)
        val toList = cursor.getString(TO_LIST_COLUMN)
        val ccList = cursor.getString(CC_LIST_COLUMN)
        val fromAddrs = Address.unpack(fromList)
        val toAddrs = Address.unpack(toList)
        val ccAddrs = Address.unpack(ccList)

        val fromMe = messageHelper.toMe(account, fromAddrs)
        val toMe = messageHelper.toMe(account, toAddrs)
        val ccMe = messageHelper.toMe(account, ccAddrs)

        val displayName = messageHelper.getDisplayName(account, fromAddrs, toAddrs)
        val displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(DATE_COLUMN))

        val counterpartyAddress = fetchCounterPartyAddress(fromMe, toAddrs, ccAddrs, fromAddrs)

        val threadCount = if (fragment.showingThreadedList) cursor.getInt(THREAD_COUNT_COLUMN) else 0

        val subject = MlfUtils.buildSubject(cursor.getString(SUBJECT_COLUMN),
                res.getString(R.string.general_no_subject), threadCount)

        val read = cursor.getInt(READ_COLUMN) == 1
        val flagged = cursor.getInt(FLAGGED_COLUMN) == 1
        val answered = cursor.getInt(ANSWERED_COLUMN) == 1
        val forwarded = cursor.getInt(FORWARDED_COLUMN) == 1

        val hasAttachments = cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0

        val holder = view.tag as MessageViewHolder

        val maybeBoldTypeface = if (read) Typeface.NORMAL else Typeface.BOLD

        val uniqueId = cursor.getLong(fragment.uniqueIdColumn)
        val selected = fragment.selected.contains(uniqueId)

        holder.chip.setBackgroundColor(account.chipColor)
        if (fragment.checkboxes) {
            holder.selected.isChecked = selected
        }
        if (fragment.stars) {
            holder.flagged.isChecked = flagged
        }
        holder.position = cursor.position
        if (holder.contactBadge != null) {
            updateContactBadge(holder, counterpartyAddress)
        }
        setBackgroundColor(view, selected, read)
        if (fragment.activeMessage != null) {
            changeBackgroundColorIfActiveMessage(cursor, account, view)
        }
        updateWithThreadCount(holder, threadCount)
        val beforePreviewText = if (senderAboveSubject) subject else displayName
        val sigil = recipientSigil(toMe, ccMe)
        val messageStringBuilder = SpannableStringBuilder(sigil)
                .append(beforePreviewText)
        if (previewLines > 0) {
            val preview = getPreview(cursor)
            messageStringBuilder.append(" ").append(preview)
        }
        holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE)

        formatPreviewText(holder.preview, beforePreviewText, sigil)

        if (holder.from != null) {
            holder.from.typeface = Typeface.create(holder.from.typeface, maybeBoldTypeface)
            if (senderAboveSubject) {
                holder.from.text = displayName
            } else {
                holder.from.text = SpannableStringBuilder(sigil).append(displayName)
            }
        }
        if (holder.subject != null) {
            holder.subject.typeface = Typeface.create(holder.subject.typeface, maybeBoldTypeface)
            holder.subject.text = subject
        }
        holder.date.text = displayDate
        holder.attachment.visibility = if (hasAttachments) View.VISIBLE else View.GONE

        val statusHolder = buildStatusHolder(forwarded, answered)
        if (statusHolder != null) {
            holder.status.setImageDrawable(statusHolder)
            holder.status.visibility = View.VISIBLE
        } else {
            holder.status.visibility = View.GONE
        }
    }

    private fun formatPreviewText(
            preview: TextView,
            beforePreviewText: CharSequence,
            sigil: String
    ) {
        val previewText = preview.text as Spannable
        previewText.setSpan(buildSenderSpan(), 0, beforePreviewText.length + sigil.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set span (color) for preview message
        previewText.setSpan(
                ForegroundColorSpan(previewTextColor),
                beforePreviewText.length + sigil.length,
                previewText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Create a span section for the sender, and assign the correct font size and weight
     */
    private fun buildSenderSpan(): AbsoluteSizeSpan {
        val fontSize = if (senderAboveSubject)
            fontSizes.messageListSubject
        else
            fontSizes.messageListSender
        return AbsoluteSizeSpan(fontSize, true)
    }

    private fun fetchCounterPartyAddress(
            fromMe: Boolean,
            toAddrs: Array<Address>,
            ccAddrs: Array<Address>,
            fromAddrs: Array<Address>
    ): Address? {
        if (fromMe) {
            if (toAddrs.size > 0) {
                return toAddrs[0]
            } else if (ccAddrs.size > 0) {
                return ccAddrs[0]
            }
        } else if (fromAddrs.size > 0) {
            return fromAddrs[0]
        }
        return null
    }

    private fun updateContactBadge(holder: MessageViewHolder, counterpartyAddress: Address?) {
        if (counterpartyAddress != null) {
            holder.contactBadge.setContact(counterpartyAddress)
            /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but ContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */
            holder.contactBadge.setPadding(0, 0, 0, 0)
            fragment.contactsPictureLoader.setContactPicture(holder.contactBadge, counterpartyAddress)
        } else {
            holder.contactBadge.assignContactUri(null)
            holder.contactBadge.setImageResource(R.drawable.ic_contact_picture)
        }
    }

    private fun changeBackgroundColorIfActiveMessage(cursor: Cursor, account: Account, view: View) {
        val uid = cursor.getString(UID_COLUMN)
        val folderServerId = cursor.getString(FOLDER_SERVER_ID_COLUMN)

        if (account.uuid == fragment.activeMessage.accountUuid &&
                folderServerId == fragment.activeMessage.folderServerId &&
                uid == fragment.activeMessage.uid) {
            view.setBackgroundColor(activeItemBackgroundColor)
        }
    }

    private fun buildStatusHolder(forwarded: Boolean, answered: Boolean): Drawable? {
        if (forwarded && answered) {
            return mForwardedAnsweredIcon
        } else if (answered) {
            return mAnsweredIcon
        } else if (forwarded) {
            return mForwardedIcon
        }
        return null
    }

    private fun setBackgroundColor(view: View, selected: Boolean, read: Boolean) {
        if (selected || K9.isUseBackgroundAsUnreadIndicator) {
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

    private fun updateWithThreadCount(holder: MessageViewHolder, threadCount: Int) {
        if (threadCount > 1) {
            holder.threadCount.setText(String.format("%d", threadCount))
            holder.threadCount.visibility = View.VISIBLE
        } else {
            holder.threadCount.visibility = View.GONE
        }
    }

    private fun getPreview(cursor: Cursor): String {
        val previewTypeString = cursor.getString(PREVIEW_TYPE_COLUMN)
        val previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString)

        when (previewType) {
            DatabasePreviewType.NONE, DatabasePreviewType.ERROR -> {
                return ""
            }
            DatabasePreviewType.ENCRYPTED -> {
                return res.getString(R.string.preview_encrypted)
            }
            DatabasePreviewType.TEXT -> {
                return cursor.getString(PREVIEW_COLUMN)
            }
        }

        throw AssertionError("Unknown preview type: $previewType")
    }
}
