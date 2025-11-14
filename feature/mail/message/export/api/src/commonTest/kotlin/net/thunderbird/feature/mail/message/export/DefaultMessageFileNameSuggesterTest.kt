package net.thunderbird.feature.mail.message.export

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.datetime.LocalDateTime

class DefaultMessageFileNameSuggesterTest {

    private val testSubject = DefaultMessageFileNameSuggester()

    @Test
    fun `suggestFileName should format date and time, normalize subject, and append extension`() {
        // Arrange
        val dt = LocalDateTime(2025, 10, 15, 14, 1)
        val subject = "Meeting Reminder: Project Kickoff @ 10am!"
        val extension = "eml"

        // Act
        val result = testSubject.suggestFileName(subject = subject, sentDateTime = dt, extension = extension)

        // Assert
        assertThat(result)
            .isEqualTo("2025-10-15_14-01_meeting-reminder-project-kickoff-10am.eml")
    }

    @Test
    fun `suggestFileName should normalize mixed case and non-alphanumeric characters`() {
        // Arrange
        val dt = LocalDateTime(2023, 1, 2, 3, 4)
        val subject = "  HeLLo___World!!  -@-  ###Thunderbird!!!  "
        val extension = "eml"

        // Act
        val result = testSubject.suggestFileName(subject = subject, sentDateTime = dt, extension = extension)

        // Assert
        assertThat(result).isEqualTo("2023-01-02_03-04_hello-world-thunderbird.eml")
    }

    @Test
    fun `suggestFileName should fall back to message when subject is blank or symbols only`() {
        // Arrange
        val dt = LocalDateTime(1999, 12, 31, 23, 59)

        // Act
        val resultBlank = testSubject.suggestFileName(subject = "\t \n  ", sentDateTime = dt, extension = "eml")
        val resultSymbols = testSubject.suggestFileName(subject = "*** ### !!!", sentDateTime = dt, extension = "log")

        // Assert
        assertThat(resultBlank).isEqualTo("1999-12-31_23-59_message.eml")
        assertThat(resultSymbols).isEqualTo("1999-12-31_23-59_message.log")
    }
}
