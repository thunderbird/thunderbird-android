package com.fsck.k9.ui.messageview

import android.text.SpannableStringBuilder

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
 * actually rendering the text to ellipsize [RecipientLayoutData.recipientNames], but not
 * [RecipientLayoutData.additionalRecipients].
 */
internal class RecipientLayoutCreator(
    private val textMeasure: TextMeasure,
    private val maxNumberOfRecipientNames: Int,
    private val recipientsPrefix: String,
    private val additionalRecipientSpacing: Int,
    private val additionalRecipientsPrefix: String,
) {
    fun createRecipientLayout(
        recipientNames: List<CharSequence>,
        totalNumberOfRecipients: Int,
        availableWidth: Int,
    ): RecipientLayoutData {
        require(recipientNames.isNotEmpty())

        val displayRecipientsBuilder = SpannableStringBuilder()

        if (recipientNames.size == 1) {
            displayRecipientsBuilder.append(recipientsPrefix)
            displayRecipientsBuilder.append(recipientNames.first())

            return RecipientLayoutData(
                recipientNames = displayRecipientsBuilder,
                additionalRecipients = null,
            )
        }

        val additionalRecipientsBuilder = StringBuilder(additionalRecipientsPrefix + 10)

        val maxRecipientNames = recipientNames.size.coerceAtMost(maxNumberOfRecipientNames)
        for (numberOfDisplayRecipients in maxRecipientNames downTo 2) {
            displayRecipientsBuilder.clear()
            displayRecipientsBuilder.append(recipientsPrefix)

            recipientNames.asSequence()
                .take(numberOfDisplayRecipients)
                .joinTo(displayRecipientsBuilder, separator = LIST_SEPARATOR)

            additionalRecipientsBuilder.setLength(0)
            val numberOfAdditionalRecipients = totalNumberOfRecipients - numberOfDisplayRecipients
            if (numberOfAdditionalRecipients > 0) {
                additionalRecipientsBuilder.append(additionalRecipientsPrefix)
                additionalRecipientsBuilder.append(numberOfAdditionalRecipients)
            }

            if (doesTextFitAvailableWidth(displayRecipientsBuilder, additionalRecipientsBuilder, availableWidth)) {
                return RecipientLayoutData(
                    recipientNames = displayRecipientsBuilder,
                    additionalRecipients = additionalRecipientsBuilder.toStringOrNull(),
                )
            }
        }

        displayRecipientsBuilder.clear()
        displayRecipientsBuilder.append(recipientsPrefix)
        displayRecipientsBuilder.append(recipientNames.first())

        return RecipientLayoutData(
            recipientNames = displayRecipientsBuilder,
            additionalRecipients = "$additionalRecipientsPrefix${totalNumberOfRecipients - 1}",
        )
    }

    private fun doesTextFitAvailableWidth(
        displayRecipients: CharSequence,
        additionalRecipients: CharSequence,
        availableWidth: Int,
    ): Boolean {
        val recipientNamesWidth = textMeasure.measureRecipientNames(displayRecipients)
        if (recipientNamesWidth > availableWidth) {
            return false
        } else if (additionalRecipients.isEmpty()) {
            return true
        }

        val totalWidth = recipientNamesWidth + additionalRecipientSpacing +
            textMeasure.measureRecipientCount(additionalRecipients)

        return totalWidth <= availableWidth
    }
}

private fun StringBuilder.toStringOrNull(): String? {
    return if (isEmpty()) null else toString()
}

internal data class RecipientLayoutData(
    val recipientNames: CharSequence,
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
