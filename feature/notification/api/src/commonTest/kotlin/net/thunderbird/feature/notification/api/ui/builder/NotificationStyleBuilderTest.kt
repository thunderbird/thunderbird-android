package net.thunderbird.feature.notification.api.ui.builder

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import kotlin.test.assertFails
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.NotificationStyle

@Suppress("MaxLineLength")
class NotificationStyleBuilderTest {
    @Test
    fun `notificationStyle dsl should create inbox system notification style`() {
        // Arrange
        val title = "The title"
        val summary = "The summary"
        val expected = NotificationStyle.System.InboxStyle(
            bigContentTitle = title,
            summary = summary,
            lines = listOf(),
        )

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            systemStyle {
                inbox {
                    title(title)
                    summary(summary)
                }
            }
        }

        // Assert
        assertThat(systemStyle)
            .isInstanceOf<NotificationStyle.System.InboxStyle>()
            .isEqualTo(expected)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(NotificationStyle.InApp.Undefined)
    }

    @Test
    fun `notificationStyle dsl should create inbox system notification style with multiple lines`() {
        // Arrange
        val title = "The title"
        val summary = "The summary"
        val contentLines = List(size = 5) {
            "line $it"
        }
        val expected = NotificationStyle.System.InboxStyle(
            bigContentTitle = title,
            summary = summary,
            lines = contentLines,
        )

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            systemStyle {
                inbox {
                    title(title)
                    summary(summary)
                    for (line in contentLines) {
                        line(line)
                    }
                }
            }
        }

        // Assert
        assertThat(systemStyle)
            .isInstanceOf<NotificationStyle.System.InboxStyle>()
            .isEqualTo(expected)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(NotificationStyle.InApp.Undefined)
    }

    @Test
    fun `notificationStyle dsl should create big text system notification style`() {
        // Arrange
        val bigText = "The ${"big ".repeat(n = 1000)}text"

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            systemStyle {
                bigText(bigText)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isInstanceOf<NotificationStyle.System.BigTextStyle>()
            .prop("text") { it.text }
            .isEqualTo(bigText)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(NotificationStyle.InApp.Undefined)
    }

    @Test
    fun `notificationStyle dsl should throw IllegalStateException when inbox system notification is missing title`() {
        // Arrange & Act
        val exception = assertFails {
            notificationStyle {
                systemStyle {
                    inbox {
                        summary("summary")
                    }
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("The inbox notification's title is required")
    }

    @Test
    fun `notificationStyle dsl should throw IllegalStateException when inbox system notification is missing summary`() {
        // Arrange & Act
        val exception = assertFails {
            notificationStyle {
                systemStyle {
                    inbox {
                        title("title")
                    }
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("The inbox  notification's summary is required")
    }

    @Test
    fun `notificationStyle dsl should throw IllegalArgumentException when inbox system notification adds more then 5 lines`() {
        // Arrange
        val lines = List(size = MAX_LINES + 1) { "line $it" }

        // Act
        val exception = assertFails {
            notificationStyle {
                systemStyle {
                    inbox {
                        title("title")
                        summary("summary")
                        lines(lines = lines.toTypedArray())
                    }
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalArgumentException>()
            .hasMessage("The maximum number of lines for a inbox notification is $MAX_LINES")
    }

    @Test
    fun `notificationStyle dsl should throw IllegalStateException when system notification style set both big text and inbox styles`() {
        // Arrange
        val bigText = "The ${"big ".repeat(n = 1000)}text"

        // Act
        val exception = assertFails {
            notificationStyle {
                systemStyle {
                    bigText(bigText)
                    inbox {
                        title("title")
                        summary("summary")
                    }
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("A system notification can either have a BigText or InboxStyle, not both.")
    }

    @Test
    fun `notificationStyle dsl should throw IllegalStateException when system notification style is called without any style configuration`() {
        // Arrange & Act
        val exception = assertFails {
            notificationStyle {
                systemStyle {
                    // intentionally empty.
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("You must configure at least one of the following styles: bigText or inbox.")
    }

    @Test
    fun `notificationStyle dsl should create a fatal in-app notification style when NotificationSeverity Fatal is provided`() {
        // Arrange
        val expected = NotificationStyle.InApp.Fatal

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            inAppStyle {
                severity(NotificationSeverity.Fatal)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isEqualTo(NotificationStyle.System.Undefined)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(expected)
    }

    @Test
    fun `notificationStyle dsl should create a critical in-app notification style when NotificationSeverity Critical is provided`() {
        // Arrange
        val expected = NotificationStyle.InApp.Critical

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            inAppStyle {
                severity(NotificationSeverity.Critical)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isEqualTo(NotificationStyle.System.Undefined)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(expected)
    }

    @Test
    fun `notificationStyle dsl should create a temporary in-app notification style when NotificationSeverity Temporary is provided`() {
        // Arrange
        val expected = NotificationStyle.InApp.Temporary

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            inAppStyle {
                severity(NotificationSeverity.Temporary)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isEqualTo(NotificationStyle.System.Undefined)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(expected)
    }

    @Test
    fun `notificationStyle dsl should create a warning in-app notification style when NotificationSeverity Warning is provided`() {
        // Arrange
        val expected = NotificationStyle.InApp.Warning

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            inAppStyle {
                severity(NotificationSeverity.Warning)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isEqualTo(NotificationStyle.System.Undefined)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(expected)
    }

    @Test
    fun `notificationStyle dsl should create a information in-app notification style when NotificationSeverity Information is provided`() {
        // Arrange
        val expected = NotificationStyle.InApp.Information

        // Act
        val (systemStyle, inAppStyle) = notificationStyle {
            inAppStyle {
                severity(NotificationSeverity.Information)
            }
        }

        // Assert
        assertThat(systemStyle)
            .isEqualTo(NotificationStyle.System.Undefined)

        assertThat(inAppStyle)
            .isInstanceOf<NotificationStyle.InApp>()
            .isEqualTo(expected)
    }

    @Test
    fun `notificationStyle dsl should throw IllegalArgumentException when severity method is called multiple times within inAppNotification dsl`() {
        // Arrange & Act
        val exception = assertFails {
            notificationStyle {
                inAppStyle {
                    severity(severity = NotificationSeverity.Fatal)
                    severity(severity = NotificationSeverity.Critical)
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalArgumentException>()
            .hasMessage("In-App Notifications must have only one severity.")
    }

    @Test
    fun `notificationStyle dsl should throw IllegalStateException when in-app notification style is called without any style configuration`() {
        // Arrange & Act
        val exception = assertFails {
            notificationStyle {
                inAppStyle {
                    // intentionally empty.
                }
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("You must add severity of the in-app notification.")
    }
}
