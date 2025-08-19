package net.thunderbird.feature.notification.api.ui.style

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("MaxLineLength")
class InAppNotificationStyleTest {
    @Test
    fun `inAppNotificationStyle dsl should create a banner inline in-app notification style`() {
        // Arrange
        val expectedStyles = arrayOf<InAppNotificationStyle>(InAppNotificationStyle.BannerInlineNotification)

        // Act
        val inAppStyles = inAppNotificationStyles {
            bannerInline()
        }

        // Assert
        assertThat(inAppStyles).containsExactly(elements = expectedStyles)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a banner global in-app notification style`() {
        // Arrange
        val expectedStyles = arrayOf<InAppNotificationStyle>(
            InAppNotificationStyle.BannerGlobalNotification(priority = 0),
        )

        // Act
        val inAppStyles = inAppNotificationStyles {
            bannerGlobal()
        }

        // Assert
        assertThat(inAppStyles).containsExactly(elements = expectedStyles)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a snackbar in-app notification style`() {
        // Arrange
        val expectedStyles = arrayOf<InAppNotificationStyle>(InAppNotificationStyle.SnackbarNotification())

        // Act
        val inAppStyles = inAppNotificationStyles {
            snackbar()
        }

        // Assert
        assertThat(inAppStyles).containsExactly(elements = expectedStyles)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a snackbar with 30 seconds duration in-app notification style`() {
        // Arrange
        val duration = 30.seconds
        val expectedStyles = arrayOf<InAppNotificationStyle>(InAppNotificationStyle.SnackbarNotification(duration))

        // Act
        val inAppStyles = inAppNotificationStyles {
            snackbar(duration = duration)
        }

        // Assert
        assertThat(inAppStyles).containsExactly(elements = expectedStyles)
    }

    @Test
    fun `inAppNotificationStyle dsl should create a dialog in-app notification style`() {
        // Arrange
        val expectedStyles = arrayOf<InAppNotificationStyle>(InAppNotificationStyle.DialogNotification)

        // Act
        val inAppStyles = inAppNotificationStyles {
            dialog()
        }

        // Assert
        assertThat(inAppStyles).containsExactly(elements = expectedStyles)
    }

    @Test
    fun `inAppNotificationStyle dsl should create multiple styles in-app notification style`() {
        // Arrange
        val expectedStyles = arrayOf(
            InAppNotificationStyle.BannerInlineNotification,
            InAppNotificationStyle.BannerGlobalNotification(priority = 0),
            InAppNotificationStyle.SnackbarNotification(),
            InAppNotificationStyle.DialogNotification,
        )

        // Act
        val inAppStyles = inAppNotificationStyles {
            bannerInline()
            bannerGlobal()
            snackbar()
            dialog()
        }

        // Assert
        assertThat(inAppStyles).containsExactly(elements = expectedStyles)
    }

    @Test
    fun `inAppNotificationStyle dsl should throw IllegalStateException when bannerInline style is added multiple times`() {
        // Arrange
        val expectedErrorMessage =
            "An in-app notification can only have at most one type of ${
                InAppNotificationStyle.BannerInlineNotification::class.simpleName
            } style"

        // Act
        val actual = assertFails {
            inAppNotificationStyles {
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
        val expectedErrorMessage =
            "An in-app notification can only have at most one type of ${
                InAppNotificationStyle.BannerGlobalNotification::class.simpleName
            } style"

        // Act
        val actual = assertFails {
            inAppNotificationStyles {
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
        val expectedErrorMessage =
            "An in-app notification can only have at most one type of ${
                InAppNotificationStyle.SnackbarNotification::class.simpleName
            } style"

        // Act
        val actual = assertFails {
            inAppNotificationStyles {
                snackbar()
                snackbar(duration = 1.minutes)
                snackbar(duration = 1.hours)
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
            inAppNotificationStyles {
                // intentionally empty.
            }
        }

        // Assert
        assertThat(exception)
            .isInstanceOf<IllegalStateException>()
            .hasMessage("You must add at least one in-app notification style.")
    }
}
