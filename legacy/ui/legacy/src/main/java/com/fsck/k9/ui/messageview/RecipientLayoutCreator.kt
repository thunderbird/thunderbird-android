package com.fsck.k9.ui.messageview

import android.text.SpannableStringBuilder
import androidx.core.text.toSpanned

private const val LIST_SEPARATOR = ", "

/**
 * Calculates how many recipient names can be displayed given the available width.
 *
 * We display up to [maxNumberOfRecipientNames] recipient names, then the number of additional recipients.
 *
 * Example:
 *   to me, Alice, Bob, Charly, Dora +11
 *
 * If there's not enough room to display the first recipient name, we return it anyway and expect the component that is
 * actually rendering the text to ellipsize [RecipientLayoutData.recipientList], but not
 * [RecipientLayoutData.additionalRecipients].
 */
internal class RecipientLayoutCreator(
    private val textMeasure: TextMeasure,
    private val maxNumberOfRecipientNames: Int,
    recipientsFormat: String,
    private val additionalRecipientSpacing: Int,
    private val additionalRecipientsPrefix: String,
) {
    private val recipientsPrefix: String
    private val recipientsSuffix: String

    init {
        require(recipientsFormat.contains("%s")) { "recipientFormat must contain '%s'" }
        recipientsPrefix = recipientsFormat.substringBefore("%s")
        recipientsSuffix = recipientsFormat.substringAfter("%s")
    }

    fun createRecipientLayout(
        recipientNames: List<CharSequence>,
        totalNumberOfRecipients: Int,
        availableWidth: Int,
    ): RecipientLayoutData {
        require(recipientNames.isNotEmpty())

        if (recipientNames.size == 1) {
            return RecipientLayoutData(
                recipientList = createRecipientList(recipientNames),
                additionalRecipients = null,
            )
        }

        val additionalRecipientsBuilder = StringBuilder(additionalRecipientsPrefix + 10)

        val maxRecipientNames = recipientNames.size.coerceAtMost(maxNumberOfRecipientNames)
        for (numberOfDisplayRecipients in maxRecipientNames downTo 2) {
            val recipientList = createRecipientList(recipientNames.take(numberOfDisplayRecipients))

            additionalRecipientsBuilder.setLength(0)
            val numberOfAdditionalRecipients = totalNumberOfRecipients - numberOfDisplayRecipients
            if (numberOfAdditionalRecipients > 0) {
                additionalRecipientsBuilder.append(additionalRecipientsPrefix)
                additionalRecipientsBuilder.append(numberOfAdditionalRecipients)
            }

            if (doesTextFitAvailableWidth(recipientList, additionalRecipientsBuilder, availableWidth)) {
                return RecipientLayoutData(
                    recipientList = recipientList,
                    additionalRecipients = additionalRecipientsBuilder.toStringOrNull(),
                )
            }
        }

        return RecipientLayoutData(
            recipientList = createRecipientList(recipientNames.take(1)),
            additionalRecipients = "$additionalRecipientsPrefix${totalNumberOfRecipients - 1}",
        )
    }

    private fun doesTextFitAvailableWidth(
        recipientList: CharSequence,
        additionalRecipients: CharSequence,
        availableWidth: Int,
    ): Boolean {
        val recipientListWidth = textMeasure.measureRecipientNames(recipientList)
        if (recipientListWidth > availableWidth) {
            return false
        }

        return if (additionalRecipients.isEmpty()) {
            true
        } else {
            val totalWidth = recipientListWidth + additionalRecipientSpacing +
                textMeasure.measureRecipientCount(additionalRecipients)

            totalWidth <= availableWidth
        }
    }

    private fun createRecipientList(recipientNames: List<CharSequence>): CharSequence {
        return recipientNames.joinTo(
            buffer = SpannableStringBuilder(),
            separator = LIST_SEPARATOR,
            prefix = recipientsPrefix,
            postfix = recipientsSuffix,
        ).toSpanned()
    }
}

private fun StringBuilder.toStringOrNull(): String? {
    return if (isEmpty()) null else toString()
}

internal data class RecipientLayoutData(
    val recipientList: CharSequence,
    val additionalRecipients: String?,
)

internal interface TextMeasure {
    /**
     * Measure the width of the supplied recipient names when rendered.
     */
    fun measureRecipientNames(text: CharSequence): Int

    /**
     * Measure the width of the supplied recipient count when rendered.
     */
    fun measureRecipientCount(text: CharSequence): Int
}
