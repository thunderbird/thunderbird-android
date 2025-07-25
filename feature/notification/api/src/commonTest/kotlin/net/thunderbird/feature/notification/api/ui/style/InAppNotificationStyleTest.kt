package net.thunderbird.feature.notification.api.ui.style

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlin.test.assertFails
import net.thunderbird.feature.notification.api.NotificationSeverity

@Suppress("MaxLineLength")
class InAppNotificationStyleTest {
    @Test
    fun `inAppNotificationStyle dsl should create a fatal in-app notification style when NotificationSeverity Fatal is provided`() {
        // Arrange
        val expected = InAppNotificationStyle.Fatal

        // Act
        val inAppStyle = inAppNotificationStyle {
            severity(NotificationSeverity.Fatal)
        }

        // Assert
        assertThat(inAppStyle)
            .isInstanceOf<InAppNotificationStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a critical in-app notification style when NotificationSeverity Critical is provided`() {
        // Arrange
        val expected = InAppNotificationStyle.Critical

        // Act
        val inAppStyle = inAppNotificationStyle {
            severity(NotificationSeverity.Critical)
        }

        // Assert
        assertThat(inAppStyle)
            .isInstanceOf<InAppNotificationStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a temporary in-app notification style when NotificationSeverity Temporary is provided`() {
        // Arrange
        val expected = InAppNotificationStyle.Temporary

        // Act
        val inAppStyle = inAppNotificationStyle {
            severity(NotificationSeverity.Temporary)
        }

        // Assert
        assertThat(inAppStyle)
            .isInstanceOf<InAppNotificationStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a warning in-app notification style when NotificationSeverity Warning is provided`() {
        // Arrange
        val expected = InAppNotificationStyle.Warning

        // Act
        val inAppStyle = inAppNotificationStyle {
            severity(NotificationSeverity.Warning)
        }

        // Assert
        assertThat(inAppStyle)
            .isInstanceOf<InAppNotificationStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a information in-app notification style when NotificationSeverity Information is provided`() {
        // Arrange
        val expected = InAppNotificationStyle.Information

        // Act
        val inAppStyle = inAppNotificationStyle {
            severity(NotificationSeverity.Information)
        }

        // Assert
        assertThat(inAppStyle)
            .isInstanceOf<InAppNotificationStyle>()
            .isEqualTo(expected)
    }

    @Test
    fun `inAppNotificationStyle dsl should throw IllegalArgumentException when severity method is called multiple times within inAppNotification dsl`() {
        // Arrange & Act
        val exception = assertFails {
            inAppNotificationStyle {
                severity(severity = NotificationSeverity.Fatal)
                severity(severity = NotificationSeverity.Critical)
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalArgumentException>()
            .hasMessage("In-App Notifications must have only one severity.")
    }

    @Test
    fun `inAppNotificationStyle dsl should throw IllegalStateException when in-app notification style is called without any style configuration`() {
        // Arrange & Act
        val exception = assertFails {
            inAppNotificationStyle {
                // intentionally empty.
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("You must add severity of the in-app notification.")
    }
}
