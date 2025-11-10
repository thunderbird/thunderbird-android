package net.thunderbird.feature.notification.api.ui.style

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlin.test.assertFails

@Suppress("MaxLineLength")
class InAppNotificationStyleTest {
    @Test
    fun `inAppNotificationStyle dsl should create a banner inline in-app notification style`() {
        // Arrange
        val expectedStyle = InAppNotificationStyle.BannerInlineNotification

        // Act
        val inAppStyle = inAppNotificationStyle { bannerInline() }

        // Assert
        assertThat(inAppStyle).isEqualTo(expectedStyle)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a banner global in-app notification style`() {
        // Arrange
        val expectedStyle = InAppNotificationStyle.BannerGlobalNotification(priority = 0)

        // Act
        val inAppStyle = inAppNotificationStyle { bannerGlobal() }

        // Assert
        assertThat(inAppStyle).isEqualTo(expectedStyle)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a snackbar in-app notification style`() {
        // Arrange
        val expectedStyle = InAppNotificationStyle.SnackbarNotification()

        // Act
        val inAppStyle = inAppNotificationStyle { snackbar() }

        // Assert
        assertThat(inAppStyle).isEqualTo(expectedStyle)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a snackbar with 30 seconds duration in-app notification style`() {
        // Arrange
        val duration = SnackbarDuration.Short
        val expectedStyle = InAppNotificationStyle.SnackbarNotification(duration)

        // Act
        val inAppStyle = inAppNotificationStyle { snackbar(duration = duration) }

        // Assert
        assertThat(inAppStyle).isEqualTo(expectedStyle)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a dialog in-app notification style`() {
        // Arrange
        val expectedStyle = InAppNotificationStyle.DialogNotification

        // Act
        val inAppStyle = inAppNotificationStyle { dialog() }

        // Assert
        assertThat(inAppStyle).isEqualTo(expectedStyle)
    }

    @Test
    fun `inAppNotificationStyle dsl should not create multiple styles in-app notification style`() {
        // Arrange
        val firstStyle = InAppNotificationStyle.BannerInlineNotification::class.simpleName
        val secondStyle = InAppNotificationStyle.BannerGlobalNotification::class.simpleName
        val expectedErrorMessage = "An in-app notification can only have one type of style. " +
            "Current style is $firstStyle, trying to set $secondStyle."

        // Act
        val actual = assertFails {
            inAppNotificationStyle {
                bannerInline()
                bannerGlobal()
                snackbar()
                dialog()
            }
        }

        // Assert
        assertThat(actual)
            .isInstanceOf<IllegalStateException>()
            .hasMessage(expectedErrorMessage)
    }

    @Test
    fun `inAppNotificationStyle dsl should throw IllegalStateException when bannerInline style is added multiple times`() {
        // Arrange
        val styleName = InAppNotificationStyle.BannerInlineNotification::class.simpleName
        val expectedErrorMessage = "An in-app notification can only have one type of style. " +
            "Current style is $styleName, trying to set $styleName."

        // Act
        val actual = assertFails {
            inAppNotificationStyle {
                bannerInline()
                bannerInline()
                bannerInline()
            }
        }

        // Assert
        assertThat(actual)
            .isInstanceOf<IllegalStateException>()
            .hasMessage(expectedErrorMessage)
    }

    @Test
    fun `inAppNotificationStyle dsl should throw IllegalStateException when bannerGlobal style is added multiple times`() {
        // Arrange
        val styleName = InAppNotificationStyle.BannerGlobalNotification::class.simpleName
        val expectedErrorMessage = "An in-app notification can only have one type of style. " +
            "Current style is $styleName, trying to set $styleName."

        // Act
        val actual = assertFails {
            inAppNotificationStyle {
                bannerGlobal()
                bannerGlobal()
                bannerGlobal()
            }
        }

        // Assert
        assertThat(actual)
            .isInstanceOf<IllegalStateException>()
            .hasMessage(expectedErrorMessage)
    }

    @Test
    fun `inAppNotificationStyle dsl should throw IllegalStateException when snackbar style is added multiple times`() {
        // Arrange
        val styleName = InAppNotificationStyle.SnackbarNotification::class.simpleName
        val expectedErrorMessage = "An in-app notification can only have one type of style. " +
            "Current style is $styleName, trying to set $styleName."

        // Act
        val actual = assertFails {
            inAppNotificationStyle {
                snackbar()
                snackbar(duration = SnackbarDuration.Short)
                snackbar(duration = SnackbarDuration.Long)
            }
        }

        // Assert
        assertThat(actual)
            .isInstanceOf<IllegalStateException>()
            .hasMessage(expectedErrorMessage)
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
            .hasMessage("You must pick one in-app notification style.")
    }
}
