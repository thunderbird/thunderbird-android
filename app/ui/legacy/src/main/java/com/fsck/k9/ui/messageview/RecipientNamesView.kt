package com.fsck.k9.ui.messageview

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

private const val MAX_NUMBER_OF_RECIPIENT_NAMES = 5

/**
 * View that displays the names of recipients of a message.
 *
 * Up to [MAX_NUMBER_OF_RECIPIENT_NAMES] names of recipients are displayed, followed by the number of recipients that
 * weren't displayed.
 *
 * Examples:
 * - to me, Alice, Bob, Charly +3
 * - to Camila Hyphenated-Nam… +5
 *
 * This custom layout uses [RecipientLayoutCreator] to figure out how many recipient names can be displayed without
 * being truncated. If not even one recipient name can be displayed without being truncated, we first measure the space
 * needed for number of additional recipients, then use the rest to display the first recipient and ellipsize the end.
 */
class RecipientNamesView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    val maxNumberOfRecipientNames: Int = MAX_NUMBER_OF_RECIPIENT_NAMES

    private val recipientLayoutCreator: RecipientLayoutCreator

    private val recipientNameTextView: MaterialTextView
    private val recipientCountTextView: MaterialTextView
    private val additionRecipientSpacing: Int

    init {
        LayoutInflater.from(context).inflate(R.layout.recipient_names, this, true)
        recipientNameTextView = findViewById(R.id.recipient_names)
        recipientCountTextView = findViewById(R.id.recipient_count)
        additionRecipientSpacing = (recipientCountTextView.layoutParams as MarginLayoutParams).marginStart
    }

    private var recipientNames: List<CharSequence> = emptyList()
    private var numberOfRecipients: Int = 0

    private val textMeasure = object : TextMeasure {
        override fun measureRecipientNames(text: CharSequence): Int {
            return measureWidth(recipientNameTextView, text)
        }

        override fun measureRecipientCount(text: CharSequence): Int {
            return measureWidth(recipientCountTextView, text)
        }

        private fun measureWidth(textView: MaterialTextView, text: CharSequence): Int {
            textView.text = text

            val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            val heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            textView.measure(widthMeasureSpec, heightMeasureSpec)

            return textView.measuredWidth
        }
    }

    init {
        recipientLayoutCreator = RecipientLayoutCreator(
            textMeasure = textMeasure,
            maxNumberOfRecipientNames = MAX_NUMBER_OF_RECIPIENT_NAMES,
            recipientsFormat = context.getString(R.string.message_view_recipients_format),
            additionalRecipientSpacing = additionRecipientSpacing,
            additionalRecipientsPrefix = context.getString(R.string.message_view_additional_recipient_prefix),
        )

        if (isInEditMode) {
            recipientNames = listOf(
                "Grace Hopper",
                "Katherine Johnson",
                "Margaret Hamilton",
                "Adele Goldberg",
                "Steve Shirley",
            )
            numberOfRecipients = 8
        }
    }

    fun setTextSize(textSize: Int) {
        recipientNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
        recipientCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
    }

    fun setRecipients(recipientNames: List<CharSequence>, numberOfRecipients: Int) {
        if (recipientNames != this.recipientNames && numberOfRecipients != this.numberOfRecipients) {
            this.recipientNames = recipientNames
            this.numberOfRecipients = numberOfRecipients
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        require(MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            "Width of RecipientNamesView needs to be constrained"
        }

        recipientNameTextView.measure(widthMeasureSpec, heightMeasureSpec)
        recipientCountTextView.measure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = maxOf(recipientNameTextView.measuredHeight, recipientCountTextView.measuredHeight)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (numberOfRecipients == 0) {
            // There's nothing to display
            return
        }

        val availableWidth = width

        val recipientLayoutData = recipientLayoutCreator.createRecipientLayout(
            recipientNames,
            numberOfRecipients,
            availableWidth,
        )

        recipientNameTextView.text = recipientLayoutData.recipientList
        val additionalRecipientsVisible = recipientLayoutData.additionalRecipients != null
        val remainingWidth: Int
        if (additionalRecipientsVisible) {
            recipientCountTextView.isGone = false
            recipientCountTextView.text = recipientLayoutData.additionalRecipients

            recipientCountTextView.measure(
                MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST),
            )

            remainingWidth = availableWidth - additionRecipientSpacing - recipientCountTextView.measuredWidth
        } else {
            recipientCountTextView.isGone = true
            remainingWidth = availableWidth
        }

        recipientNameTextView.measure(
            MeasureSpec.makeMeasureSpec(remainingWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST),
        )

        if (layoutDirection == LAYOUT_DIRECTION_LTR) {
            val recipientNameRight = recipientNameTextView.measuredWidth
            recipientNameTextView.layout(
                0,
                0,
                recipientNameRight,
                recipientNameTextView.measuredHeight,
            )
            val recipientCountLeft = recipientNameRight + additionRecipientSpacing
            recipientCountTextView.layout(
                recipientCountLeft,
                0,
                recipientCountLeft + recipientCountTextView.measuredWidth,
                recipientCountTextView.measuredHeight,
            )
        } else {
            val recipientNameLeft = width - recipientNameTextView.measuredWidth
            recipientNameTextView.layout(
                recipientNameLeft,
                0,
                right,
                recipientNameTextView.measuredHeight,
            )
            val recipientCountRight = recipientNameLeft - additionRecipientSpacing
            recipientCountTextView.layout(
                recipientCountRight - recipientCountTextView.measuredWidth,
                0,
                recipientCountRight,
                0 + recipientCountTextView.measuredHeight,
            )
        }
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(0, 0)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}
