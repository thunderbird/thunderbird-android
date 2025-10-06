package net.thunderbird.feature.notification.api.ui.style

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import kotlin.test.assertFails
import net.thunderbird.feature.notification.api.ui.style.builder.MAX_LINES

@Suppress("MaxLineLength")
class SystemNotificationStyleTest {
    @Test
    fun `systemNotificationStyle dsl should create inbox system notification style`() {
        // Arrange
        val title = "The title"
        val summary = "The summary"
        val expected = SystemNotificationStyle.InboxStyle(
            bigContentTitle = title,
            summary = summary,
            lines = listOf(),
        )

        // Act
        val systemStyle = systemNotificationStyle {
            inbox {
                title(title)
                summary(summary)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isInstanceOf<SystemNotificationStyle.InboxStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `systemNotificationStyle dsl should create inbox system notification style with multiple lines`() {
        // Arrange
        val title = "The title"
        val summary = "The summary"
        val contentLines = List(size = 5) {
            "line $it"
        }
        val expected = SystemNotificationStyle.InboxStyle(
            bigContentTitle = title,
            summary = summary,
            lines = contentLines,
        )

        // Act
        val systemStyle = systemNotificationStyle {
            inbox {
                title(title)
                summary(summary)
                for (line in contentLines) {
                    line(line)
                }
            }
        }

        // Assert
        assertThat(systemStyle)
            .isInstanceOf<SystemNotificationStyle.InboxStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `systemNotificationStyle dsl should create big text system notification style`() {
        // Arrange
        val bigText = "The ${"big ".repeat(n = 1000)}text"

        // Act
        val systemStyle = systemNotificationStyle {
            bigText(bigText)
        }

        // Assert
        assertThat(systemStyle)
            .isInstanceOf<SystemNotificationStyle.BigTextStyle>()
            .prop("text") { it.text }
            .isEqualTo(bigText)
    }

    @Test
    fun `systemNotificationStyle dsl should throw IllegalStateException when inbox system notification is missing title`() {
        // Arrange & Act
        val exception = assertFails {
            systemNotificationStyle {
                inbox {
                    summary("summary")
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("The inbox notification's title is required")
    }

    @Test
    fun `systemNotificationStyle dsl should throw IllegalStateException when inbox system notification is missing summary`() {
        // Arrange & Act
        val exception = assertFails {
            systemNotificationStyle {
                inbox {
                    title("title")
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("The inbox  notification's summary is required")
    }

    @Suppress("VisibleForTests")
    @Test
    fun `systemNotificationStyle dsl should throw IllegalArgumentException when inbox system notification adds more then 5 lines`() {
        // Arrange
        val lines = List(size = MAX_LINES + 1) { "line $it" }

        // Act
        val exception = assertFails {
            systemNotificationStyle {
                inbox {
                    title("title")
                    summary("summary")
                    lines(lines = lines.toTypedArray())
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalArgumentException>()
            .hasMessage("The maximum number of lines for a inbox notification is $MAX_LINES")
    }

    @Test
    fun `systemNotificationStyle dsl should throw IllegalStateException when system notification style set both big text and inbox styles`() {
        // Arrange
        val bigText = "The ${"big ".repeat(n = 1000)}text"

        // Act
        val exception = assertFails {
            systemNotificationStyle {
                bigText(bigText)
                inbox {
                    title("title")
                    summary("summary")
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("A system notification can either have a BigText or InboxStyle, not both.")
    }

    @Test
    fun `systemNotificationStyle dsl should throw IllegalStateException when system notification style is called without any style configuration`() {
        // Arrange & Act
        val exception = assertFails {
            systemNotificationStyle {
                // intentionally empty.
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("You must configure at least one of the following styles: bigText or inbox.")
    }
}
