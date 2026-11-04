package net.thunderbird.feature.notification.api.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector
import assertk.assertFailure
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class NotificationIconTest {
    @Test
    fun `NotificationIcon should throw IllegalStateException when both system and inApp icons are null`() {
        // Arrange
        val systemNotificationIcon: SystemNotificationIcon? = null
        val inAppNotificationIcon: ImageVector? = null

        // Act
        val exception = assertFailure {
            NotificationIcon(systemNotificationIcon, inAppNotificationIcon)
        }

        // Assert
        exception
            .isInstanceOf<IllegalStateException>()
            .hasMessage(
                "Both systemNotificationIcon and inAppNotificationIcon are null. " +
                    "You must specify at least one type of icon.",
            )
    }
}
