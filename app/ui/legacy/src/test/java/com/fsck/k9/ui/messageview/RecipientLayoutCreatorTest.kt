package com.fsck.k9.ui.messageview

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.RobolectricTest
import org.junit.Test

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
        recipientsPrefix = "to ",
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

        assertThat(result.recipientNames.toString()).isEqualTo("to me")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `single recipient name that doesn't fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("me"),
            totalNumberOfRecipients = 1,
            availableWidth = 1,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to me")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `two recipient names where first one doesn't fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", "Bob"),
            totalNumberOfRecipients = 2,
            availableWidth = 5,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to Alice")
        assertThat(result.additionalRecipients).isEqualTo("+1")
    }

    @Test
    fun `two recipient names where both fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", "Bob"),
            totalNumberOfRecipients = 2,
            availableWidth = 13,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to Alice, Bob")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `three recipient names where only first one fits available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", "Bob", "Charly"),
            totalNumberOfRecipients = 3,
            availableWidth = 13,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to Alice")
        assertThat(result.additionalRecipients).isEqualTo("+2")
    }

    @Test
    fun `three recipient names where only first two fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", "Bob", "Charly"),
            totalNumberOfRecipients = 3,
            availableWidth = 16,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to Alice, Bob")
        assertThat(result.additionalRecipients).isEqualTo("+1")
    }

    @Test
    fun `three recipient names where all fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("Alice", "Bob", "Charly"),
            totalNumberOfRecipients = 3,
            availableWidth = 100,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to Alice, Bob, Charly")
        assertThat(result.additionalRecipients).isNull()
    }

    @Test
    fun `five recipient names and additional recipients where only two fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("One", "Two", "Three", "Four", "Five"),
            totalNumberOfRecipients = 10,
            availableWidth = 20,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to One, Two")
        assertThat(result.additionalRecipients).isEqualTo("+8")
    }

    @Test
    fun `five recipient names and additional recipients where all five fit available width`() {
        val result = recipientLayoutCreator.createRecipientLayout(
            recipientNames = listOf("One", "Two", "Three", "Four", "Five"),
            totalNumberOfRecipients = 10,
            availableWidth = 100,
        )

        assertThat(result.recipientNames.toString()).isEqualTo("to One, Two, Three, Four, Five")
        assertThat(result.additionalRecipients).isEqualTo("+5")
    }
}
