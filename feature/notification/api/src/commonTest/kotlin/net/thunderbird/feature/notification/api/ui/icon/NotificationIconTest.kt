package net.thunderbird.feature.notification.api.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlin.test.assertFails

class NotificationIconTest {
    @Test
    fun `NotificationIcon should throw IllegalStateException when both system and inApp icons are null`() {
        // Arrange
        val systemNotificationIcon: SystemNotificationIcon? = null
        val inAppNotificationIcon: ImageVector? = null

        // Act
        val exception = assertFails {
            NotificationIcon(systemNotificationIcon, inAppNotificationIcon)
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage(
                "Both systemNotificationIcon and inAppNotificationIcon are null. " +
                    "You must specify at least one type of icon.",
            )
    }
}
