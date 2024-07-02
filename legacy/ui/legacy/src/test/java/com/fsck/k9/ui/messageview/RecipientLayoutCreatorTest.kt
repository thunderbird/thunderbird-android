package com.fsck.k9.ui.messageview

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import app.k9mail.core.android.testing.RobolectricTest
import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import org.junit.Test

private const val COLOR = 0xFF0000

class RecipientLayoutCreatorTest : RobolectricTest() {
    private val textMeasure = object : TextMeasure {
        override fun measureRecipientNames(text: CharSequence): Int {
            return measureText(text)
        }

        override fun measureRecipientCount(text: CharSequence): Int {
            return measureText(text)
        }

        private fun measureText(text: CharSequence): Int {
            return text.length
        }
    }

    private val recipientLayoutCreator = RecipientLayoutCreator(
        textMeasure = textMeasure,
        maxNumberOfRecipientNames = 5,
        recipientsFormat = "to %s",
        additionalRecipientSpacing = 1,
        additionalRecipientsPrefix = "+",
    )

    @Test(expected = IllegalArgumentException::class)
    fun `empty recipient list should throw`() {
        recipientLayoutCreator.createRecipientLayout(
            recipientNames = emptyList(),
            totalNumberOfRecipients = 0,
            availableWidth = 100,
        )
    }

    @Test
    fun `single recipient name that fits available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("me"),
            totalNumberOfRecipients = 1,
            availableWidth = 10,
        )

        assertThat(result.recipientList).isEqualToSpanned("to me")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `single colored recipient name`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf(coloredString("Alice")),
            totalNumberOfRecipients = 1,
            availableWidth = 10,
        )

        assertThat(result.recipientList).isEqualToSpanned("to <color>Alice</color>")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `single recipient name that doesn't fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("me"),
            totalNumberOfRecipients = 1,
            availableWidth = 1,
        )

        assertThat(result.recipientList).isEqualToSpanned("to me")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `two recipient names where first one doesn't fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf(coloredString("Alice"), coloredString("Bob")),
            totalNumberOfRecipients = 2,
            availableWidth = 5,
        )

        assertThat(result.recipientList).isEqualToSpanned("to <color>Alice</color>")
        assertThat(result.additionalRecipients).isEqualTo("+1")
    }

    @Test
    fun `two recipient names where both fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", coloredString("Bob")),
            totalNumberOfRecipients = 2,
            availableWidth = 13,
        )

        assertThat(result.recipientList).isEqualToSpanned("to Alice, <color>Bob</color>")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `three recipient names where only first one fits available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", coloredString("Bob"), "Charly"),
            totalNumberOfRecipients = 3,
            availableWidth = 13,
        )

        assertThat(result.recipientList).isEqualToSpanned("to Alice")
        assertThat(result.additionalRecipients).isEqualTo("+2")
    }

    @Test
    fun `three recipient names where only first two fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", coloredString("Bob"), "Charly"),
            totalNumberOfRecipients = 3,
            availableWidth = 16,
        )

        assertThat(result.recipientList).isEqualToSpanned("to Alice, <color>Bob</color>")
        assertThat(result.additionalRecipients).isEqualTo("+1")
    }

    @Test
    fun `three recipient names where all fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", "Bob", "Charly"),
            totalNumberOfRecipients = 3,
            availableWidth = 100,
        )

        assertThat(result.recipientList).isEqualToSpanned("to Alice, Bob, Charly")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `five recipient names and additional recipients where only two fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf(coloredString("One"), coloredString("Two"), "Three", "Four", "Five"),
            totalNumberOfRecipients = 10,
            availableWidth = 20,
        )

        assertThat(result.recipientList).isEqualToSpanned("to <color>One</color>, <color>Two</color>")
        assertThat(result.additionalRecipients).isEqualTo("+8")
    }

    @Test
    fun `five recipient names and additional recipients where all five fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("One", "Two", "Three", "Four", coloredString("Five")),
            totalNumberOfRecipients = 10,
            availableWidth = 100,
        )

        assertThat(result.recipientList).isEqualToSpanned("to One, Two, Three, Four, <color>Five</color>")
        assertThat(result.additionalRecipients).isEqualTo("+5")
    }

    private fun coloredString(text: String): Spannable {
        return SpannableString(text).apply {
            setSpan(ForegroundColorSpan(COLOR), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun Assert<CharSequence>.isEqualToSpanned(expected: String) = given { charSequence ->
        assertThat(charSequence).isInstanceOf<Spanned>()
        val spanned = charSequence as Spanned

        val spans = spanned.getSpans(0, spanned.length, Any::class.java)
            .toList()
            .sortedByDescending { spanned.getSpanStart(it) }

        val tagString = buildString {
            append(spanned)

            for (span in spans) {
                assertThat(span).isInstanceOf<ForegroundColorSpan>().given { colorSpan ->
                    assertThat(colorSpan.foregroundColor).isEqualTo(COLOR)
                    insert(spanned.getSpanEnd(colorSpan), "</color>")
                    insert(spanned.getSpanStart(colorSpan), "<color>")
                }
            }
        }

        assertThat(tagString).isEqualTo(expected)
    }
}
