package com.fsck.k9.ui.messagelist.item

import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.FontSizes
import com.fsck.k9.contacts.ContactPictureLoader
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.ui.R
import com.fsck.k9.ui.helper.RelativeDateTimeFormatter
import com.fsck.k9.ui.messagelist.MessageListAppearance
import com.fsck.k9.ui.messagelist.MessageListItem
import com.google.android.material.textview.MaterialTextView
import java.util.Locale
import kotlin.math.max
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity

@Suppress("TooManyFunctions")
class MessageViewHolder(
    view: View,
    private val appearance: MessageListAppearance,
    private val theme: Resources.Theme,
    private val res: Resources,
    private val contactsPictureLoader: ContactPictureLoader,
    private val relativeDateTimeFormatter: RelativeDateTimeFormatter,
    private val colors: MessageViewHolderColors,
) : MessageListViewHolder(view) {

    var uniqueId: Long = -1L

    val selectedView: View = view.findViewById(R.id.selected)
    val contactPictureView: ImageView = view.findViewById(R.id.contact_picture)
    val contactPictureClickArea: View = view.findViewById(R.id.contact_picture_click_area)
    val subjectView: MaterialTextView = view.findViewById(R.id.subject)
    val previewView: MaterialTextView = view.findViewById(R.id.preview)
    val dateView: MaterialTextView = view.findViewById(R.id.date)
    val chipView: ImageView = view.findViewById(R.id.account_color_chip)
    val threadCountView: MaterialTextView = view.findViewById(R.id.thread_count)
    val starView: ImageView = view.findViewById(R.id.star)
    val starClickAreaView: View = view.findViewById(R.id.star_click_area)
    val attachmentView: ImageView = view.findViewById(R.id.attachment)
    val statusView: ImageView = view.findViewById(R.id.status)

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun bind(messageListItem: MessageListItem, isActive: Boolean, isSelected: Boolean) {
        if (appearance.showContactPicture) {
            contactPictureClickArea.isSelected = isSelected
            if (isSelected) {
                contactPictureView.isVisible = false
                selectedView.isVisible = true
            } else {
                selectedView.isVisible = false
                contactPictureView.isVisible = true
            }
            contactPictureClickArea.contentDescription = if (isSelected) {
                res.getString(R.string.swipe_action_deselect)
            } else {
                res.getString(R.string.swipe_action_select)
            }
        }

        uniqueId = messageListItem.uniqueId

        with(messageListItem) {
            val foregroundColor = selectForegroundColor(isSelected, isRead, isActive)
            val maybeBoldTypeface = if (isRead) Typeface.NORMAL else Typeface.BOLD
            val displayDate = relativeDateTimeFormatter.formatDate(messageDate, appearance.dateFormatMode)
            val displayThreadCount = if (appearance.showingThreadedList) threadCount else 0
            val subject = buildSubject(
                subject = subject,
                noSubjectText = res.getString(R.string.general_no_subject),
                threadCount = displayThreadCount,
            )

            if (appearance.showAccountIndicator) {
                val accountIndicatorDrawable = chipView.drawable.mutate()
                DrawableCompat.setTint(accountIndicatorDrawable, account.profile.color)
                chipView.setImageDrawable(accountIndicatorDrawable)
            }

            if (appearance.stars) {
                starView.isSelected = isStarred
                if (isStarred) {
                    starView.clearColorFilter()
                } else {
                    starView.setColorFilter(foregroundColor)
                }
                starClickAreaView.contentDescription = if (isStarred) {
                    res.getString(R.string.unflag_action)
                } else {
                    res.getString(R.string.flag_action)
                }
            }

            if (appearance.showContactPicture && contactPictureView.isVisible) {
                setContactPicture(contactPictureView, displayAddress)
            }
            itemView.setBackgroundColor(selectBackgroundColor(isSelected, isRead, isActive))
            updateWithThreadCount(displayThreadCount)
            val beforePreviewText = if (appearance.senderAboveSubject) subject else displayName
            val messageStringBuilder = SpannableStringBuilder(beforePreviewText)
            if (appearance.previewLines > 0) {
                val preview = getPreview(isMessageEncrypted, previewText)
                if (preview.isNotEmpty()) {
                    messageStringBuilder.append(" – ").append(preview)
                }
            }
            previewView.setTextColor(foregroundColor)
            previewView.setText(messageStringBuilder, TextView.BufferType.SPANNABLE)

            formatPreviewText(previewView, beforePreviewText, isRead, isActive, isSelected)

            subjectView.typeface = Typeface.create(subjectView.typeface, maybeBoldTypeface)
            subjectView.setTextColor(foregroundColor)

            val firstLineText = if (appearance.senderAboveSubject) displayName else subject
            subjectView.text = firstLineText

            subjectView.contentDescription = if (isRead) {
                null
            } else {
                res.getString(R.string.message_list_content_description_unread_prefix, firstLineText)
            }

            dateView.typeface = Typeface.create(dateView.typeface, maybeBoldTypeface)
            dateView.setTextColor(foregroundColor)
            dateView.text = displayDate
            attachmentView.isVisible = hasAttachments
            attachmentView.setColorFilter(foregroundColor)

            val statusHolder = buildStatusHolder(isForwarded, isAnswered)
            if (statusHolder != null) {
                statusView.setImageDrawable(statusHolder)
                statusView.isVisible = true
            } else {
                statusView.isVisible = false
            }
        }
    }

    private fun buildSubject(
        subject: String?,
        noSubjectText: String,
        threadCount: Int,
    ): String = if (subject.isNullOrEmpty()) {
        noSubjectText
    } else if (threadCount > 1) {
        // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
        Utility.stripSubject(subject)
    } else {
        subject
    }

    private fun getPreview(isMessageEncrypted: Boolean, previewText: String): String {
        return if (isMessageEncrypted) {
            res.getString(R.string.preview_encrypted)
        } else {
            previewText
        }
    }

    private fun setContactPicture(contactPictureView: ImageView, displayAddress: Address?) {
        if (displayAddress != null) {
            contactsPictureLoader.setContactPicture(contactPictureView, displayAddress)
        } else {
            contactPictureView.setImageResource(Icons.Outlined.Check)
        }
    }

    private fun updateWithThreadCount(count: Int) {
        if (count > 1) {
            threadCountView.text = String.format(Locale.US, "%d", count)
            threadCountView.isVisible = true
        } else {
            threadCountView.isVisible = false
        }
    }

    private fun formatPreviewText(
        preview: MaterialTextView,
        beforePreviewText: CharSequence,
        messageRead: Boolean,
        active: Boolean,
        selected: Boolean,
    ) {
        val previewText = preview.text as Spannable
        val textColor = selectPreviewTextColor(active, selected)

        val beforePreviewLength = beforePreviewText.length
        addBeforePreviewSpan(previewText, beforePreviewLength, messageRead)

        previewText.setSpan(
            ForegroundColorSpan(textColor),
            beforePreviewLength,
            previewText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
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

    private fun selectForegroundColor(selected: Boolean, read: Boolean, active: Boolean): Int {
        return when {
            selected -> colors.selected
            active -> colors.active
            read -> colors.read
            !read -> colors.unread
            else -> colors.regular
        }
    }

    private fun selectBackgroundColor(selected: Boolean, read: Boolean, active: Boolean): Int {
        val backGroundAsReadIndicator = appearance.backGroundAsReadIndicator
        return when {
            selected -> colors.selectedBackground
            active -> colors.activeBackground
            backGroundAsReadIndicator && read -> colors.readBackground
            backGroundAsReadIndicator && !read -> colors.unreadBackground
            else -> colors.regularBackground
        }
    }

    private fun selectPreviewTextColor(active: Boolean, selected: Boolean): Int {
        return when {
            selected -> colors.previewSelectedText
            active -> colors.previewActiveText
            else -> colors.previewText
        }
    }

    private fun buildStatusHolder(forwarded: Boolean, answered: Boolean): Drawable? {
        return if (forwarded && answered) {
            ResourcesCompat.getDrawable(res, Icons.Outlined.CompareArrows, theme)!!
        } else if (answered) {
            ResourcesCompat.getDrawable(res, Icons.Outlined.Reply, theme)!!
        } else if (forwarded) {
            ResourcesCompat.getDrawable(res, Icons.Outlined.Forward, theme)!!
        } else {
            null
        }
    }

    companion object {

        @Suppress("LongParameterList")
        fun create(
            layoutInflater: LayoutInflater,
            parent: ViewGroup?,
            appearance: MessageListAppearance,
            theme: Resources.Theme,
            res: Resources,
            contactsPictureLoader: ContactPictureLoader,
            relativeDateTimeFormatter: RelativeDateTimeFormatter,
            colors: MessageViewHolderColors,
            onClickListener: View.OnClickListener,
            onLongClickListener: View.OnLongClickListener,
            contactPictureContainerClickListener: View.OnClickListener,
            starClickListener: View.OnClickListener,
        ): MessageViewHolder {
            val view = layoutInflater.inflate(R.layout.message_list_item, parent, false)
            view.setOnClickListener(onClickListener)
            view.setOnLongClickListener(onLongClickListener)

            val holder = MessageViewHolder(
                view = view,
                appearance = appearance,
                theme = theme,
                res = res,
                contactsPictureLoader = contactsPictureLoader,
                relativeDateTimeFormatter = relativeDateTimeFormatter,
                colors = colors,
            )

            applyFontSizes(holder, appearance.fontSizes, appearance.senderAboveSubject)
            applyDensityValue(holder, appearance.density, res)

            if (appearance.showContactPicture) {
                holder.contactPictureClickArea.setOnClickListener(contactPictureContainerClickListener)
            } else {
                holder.contactPictureClickArea.isVisible = false
                holder.selectedView.isVisible = false
                holder.contactPictureView.isVisible = false
            }

            holder.chipView.isVisible = appearance.showAccountIndicator

            // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
            holder.previewView.maxLines = max(appearance.previewLines, 1)
            appearance.fontSizes.setViewTextSize(holder.previewView, appearance.fontSizes.messageListPreview)
            appearance.fontSizes.setViewTextSize(
                holder.threadCountView,
                appearance.fontSizes.messageListSubject,
            ) // thread count is next to subject

            holder.starView.isVisible = appearance.stars
            holder.starClickAreaView.isVisible = appearance.stars
            holder.starClickAreaView.setOnClickListener(starClickListener)

            view.tag = holder

            return holder
        }

        private fun applyFontSizes(holder: MessageViewHolder, fontSizes: FontSizes, senderAboveSubject: Boolean) {
            if (senderAboveSubject) {
                fontSizes.setViewTextSize(holder.subjectView, fontSizes.messageListSender)
            } else {
                fontSizes.setViewTextSize(holder.subjectView, fontSizes.messageListSubject)
            }

            fontSizes.setViewTextSize(holder.dateView, fontSizes.messageListDate)
        }

        private fun applyDensityValue(holder: MessageViewHolder, density: UiDensity, res: Resources) {
            val verticalPadding: Int
            val textViewMarginTop: Int
            val lineSpacingMultiplier: Float
            when (density) {
                UiDensity.Compact -> {
                    verticalPadding = res.getDimensionPixelSize(R.dimen.messageListCompactVerticalPadding)
                    textViewMarginTop = res.getDimensionPixelSize(R.dimen.messageListCompactTextViewMargin)
                    lineSpacingMultiplier = res.getFloatCompat(R.dimen.messageListCompactLineSpacingMultiplier)
                }

                UiDensity.Default -> {
                    verticalPadding = res.getDimensionPixelSize(R.dimen.messageListDefaultVerticalPadding)
                    textViewMarginTop = res.getDimensionPixelSize(R.dimen.messageListDefaultTextViewMargin)
                    lineSpacingMultiplier = res.getFloatCompat(R.dimen.messageListDefaultLineSpacingMultiplier)
                }

                UiDensity.Relaxed -> {
                    verticalPadding = res.getDimensionPixelSize(R.dimen.messageListRelaxedVerticalPadding)
                    textViewMarginTop = res.getDimensionPixelSize(R.dimen.messageListRelaxedTextViewMargin)
                    lineSpacingMultiplier = res.getFloatCompat(R.dimen.messageListRelaxedLineSpacingMultiplier)
                }
            }

            holder.itemView.findViewById<Guideline>(R.id.top_guideline).setGuidelineBegin(verticalPadding)
            holder.itemView.findViewById<Guideline>(R.id.bottom_guideline).setGuidelineEnd(verticalPadding)
            holder.previewView.apply {
                setMarginTop(textViewMarginTop)
                setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            }
        }

        private fun View.setMarginTop(margin: Int) {
            (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = margin
        }

        private fun Resources.getFloatCompat(@DimenRes resId: Int) = ResourcesCompat.getFloat(this, resId)
    }
}
