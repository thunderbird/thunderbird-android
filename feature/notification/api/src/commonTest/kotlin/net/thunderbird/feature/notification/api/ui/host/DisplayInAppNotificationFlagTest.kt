package net.thunderbird.feature.notification.api.ui.host

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

@Suppress("MaxLineLength")
class DisplayInAppNotificationFlagTest {

    @Test
    fun `GIVEN None flag WHEN checked against any other flag THEN result is None`() {
        // Arrange
        val none = DisplayInAppNotificationFlag.None

        // Act & Assert
        assertThat(none and DisplayInAppNotificationFlag.BannerGlobalNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.None)
        assertThat(none and DisplayInAppNotificationFlag.BannerInlineNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.None)
        assertThat(none and DisplayInAppNotificationFlag.SnackbarNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.None)
        assertThat(none and DisplayInAppNotificationFlag.AllNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.None)
    }

    @Test
    fun `GIVEN individual notification flags WHEN compared THEN they are distinct`() {
        // Arrange
        val bannerGlobal = DisplayInAppNotificationFlag.BannerGlobalNotifications
        val bannerInline = DisplayInAppNotificationFlag.BannerInlineNotifications
        val snackbar = DisplayInAppNotificationFlag.SnackbarNotifications

        // Act & Assert
        // Assert that no flag contains bits from another
        assertThat(bannerGlobal and bannerInline).isEqualTo(DisplayInAppNotificationFlag.None)
        assertThat(bannerGlobal and snackbar).isEqualTo(DisplayInAppNotificationFlag.None)
        assertThat(bannerInline and snackbar).isEqualTo(DisplayInAppNotificationFlag.None)

        // Assert that a flag AND itself is itself
        assertThat(bannerGlobal and bannerGlobal).isEqualTo(bannerGlobal)
    }

    @Test
    fun `GIVEN AllNotifications flag WHEN checked THEN it contains all individual flags`() {
        // Arrange
        val all = DisplayInAppNotificationFlag.AllNotifications

        // Act & Assert
        assertThat(all and DisplayInAppNotificationFlag.BannerGlobalNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.BannerGlobalNotifications)
        assertThat(all and DisplayInAppNotificationFlag.BannerInlineNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.BannerInlineNotifications)
        assertThat(all and DisplayInAppNotificationFlag.SnackbarNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.SnackbarNotifications)
    }

    // endregion

    @Test
    fun `GIVEN two flags WHEN combined with 'or' THEN result contains both flags`() {
        // Arrange
        val bannerGlobal = DisplayInAppNotificationFlag.BannerGlobalNotifications
        val snackbar = DisplayInAppNotificationFlag.SnackbarNotifications

        // Act
        val combination = bannerGlobal or snackbar

        // Assert
        // Verify the combined flag contains both original flags
        assertThat(combination and bannerGlobal).isEqualTo(bannerGlobal)
        assertThat(combination and snackbar).isEqualTo(snackbar)

        // Verify it does NOT contain other flags
        assertThat(combination and DisplayInAppNotificationFlag.BannerInlineNotifications)
            .isEqualTo(DisplayInAppNotificationFlag.None)
    }

    @Test
    fun `GIVEN multiple flags WHEN combined with 'or' THEN result contains all flags`() {
        // Arrange
        val bannerGlobal = DisplayInAppNotificationFlag.BannerGlobalNotifications
        val bannerInline = DisplayInAppNotificationFlag.BannerInlineNotifications
        val snackbar = DisplayInAppNotificationFlag.SnackbarNotifications

        // Act
        val combination = bannerGlobal or bannerInline or snackbar

        // Assert
        assertThat(combination and bannerGlobal).isEqualTo(bannerGlobal)
        assertThat(combination and bannerInline).isEqualTo(bannerInline)
        assertThat(combination and snackbar).isEqualTo(snackbar)
    }

    @Test
    fun `GIVEN any flag WHEN combined with 'or' None THEN result is the original flag`() {
        // Arrange
        val bannerGlobal = DisplayInAppNotificationFlag.BannerGlobalNotifications
        val none = DisplayInAppNotificationFlag.None

        // Act & Assert
        assertThat(bannerGlobal or none).isEqualTo(bannerGlobal)
        assertThat(none or bannerGlobal).isEqualTo(bannerGlobal)
        assertThat(none or none).isEqualTo(none)
    }
}
