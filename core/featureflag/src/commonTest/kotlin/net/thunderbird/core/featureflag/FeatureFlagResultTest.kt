package net.thunderbird.core.featureflag

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class FeatureFlagResultTest {

    @Test
    fun `should only call onEnabled when enabled`() {
        val testSubject = FeatureFlagResult.Enabled

        var resultEnabled = ""
        var resultDisabled = ""
        var resultUnavailable = ""

        testSubject.onEnabled {
            resultEnabled = "enabled"
        }.onDisabled {
            resultDisabled = "disabled"
        }.onUnavailable {
            resultUnavailable = "unavailable"
        }

        assertThat(resultEnabled).isEqualTo("enabled")
        assertThat(resultDisabled).isEqualTo("")
        assertThat(resultUnavailable).isEqualTo("")
    }

    @Test
    fun `should only call onDisabled when disabled`() {
        val testSubject = FeatureFlagResult.Disabled

        var resultEnabled = ""
        var resultDisabled = ""
        var resultUnavailable = ""

        testSubject.onEnabled {
            resultEnabled = "enabled"
        }.onDisabled {
            resultDisabled = "disabled"
        }.onUnavailable {
            resultUnavailable = "unavailable"
        }

        assertThat(resultEnabled).isEqualTo("")
        assertThat(resultDisabled).isEqualTo("disabled")
        assertThat(resultUnavailable).isEqualTo("")
    }

    @Test
    fun `should only call onUnavailable when unavailable`() {
        val testSubject = FeatureFlagResult.Unavailable

        var resultEnabled = ""
        var resultDisabled = ""
        var resultUnavailable = ""

        testSubject.onEnabled {
            resultEnabled = "enabled"
        }.onDisabled {
            resultDisabled = "disabled"
        }.onUnavailable {
            resultUnavailable = "unavailable"
        }

        assertThat(resultEnabled).isEqualTo("")
        assertThat(resultDisabled).isEqualTo("")
        assertThat(resultUnavailable).isEqualTo("unavailable")
    }

    @Test
    fun `should call onDisabledOrUnavailable when disabled`() {
        val testSubject = FeatureFlagResult.Disabled

        var resultEnabled = ""
        var resultDisabled = ""
        var resultUnavailable = ""
        var resultDisabledOrUnavailable = ""

        testSubject.onEnabled {
            resultEnabled = "enabled"
        }.onDisabled {
            resultDisabled = "disabled"
        }.onUnavailable {
            resultUnavailable = "unavailable"
        }.onDisabledOrUnavailable {
            resultDisabledOrUnavailable = "disabled or unavailable"
        }

        assertThat(resultEnabled).isEqualTo("")
        assertThat(resultDisabled).isEqualTo("disabled")
        assertThat(resultUnavailable).isEqualTo("")
        assertThat(resultDisabledOrUnavailable).isEqualTo("disabled or unavailable")
    }

    @Test
    fun `should call onDisabledOrUnavailable when unavailable`() {
        val testSubject = FeatureFlagResult.Unavailable

        var resultEnabled = ""
        var resultDisabled = ""
        var resultUnavailable = ""
        var resultDisabledOrUnavailable = ""

        testSubject.onEnabled {
            resultEnabled = "enabled"
        }.onDisabled {
            resultDisabled = "disabled"
        }.onUnavailable {
            resultUnavailable = "unavailable"
        }.onDisabledOrUnavailable {
            resultDisabledOrUnavailable = "disabled or unavailable"
        }

        assertThat(resultEnabled).isEqualTo("")
        assertThat(resultDisabled).isEqualTo("")
        assertThat(resultUnavailable).isEqualTo("unavailable")
        assertThat(resultDisabledOrUnavailable).isEqualTo("disabled or unavailable")
    }

    @Test
    fun `whenEnabledOrNot should return correct value based on state`() {
        val enabledResult = FeatureFlagResult.Enabled.whenEnabledOrNot(
            onEnabled = { "Feature is ON" },
            onDisabledOrUnavailable = { "Feature is OFF" },
        )
        assertThat(enabledResult).isEqualTo("Feature is ON")

        val disabledResult = FeatureFlagResult.Disabled.whenEnabledOrNot(
            onEnabled = { "Feature is ON" },
            onDisabledOrUnavailable = { "Feature is OFF" },
        )
        assertThat(disabledResult).isEqualTo("Feature is OFF")

        val unavailableResult = FeatureFlagResult.Unavailable.whenEnabledOrNot(
            onEnabled = { "Feature is ON" },
            onDisabledOrUnavailable = { "Feature is OFF" },
        )
        assertThat(unavailableResult).isEqualTo("Feature is OFF")
    }

    @Test
    fun `isEnabled, isDisabled, isUnavailable, isDisabledOrUnavailable should return correct values`() {
        assertThat(FeatureFlagResult.Enabled.isEnabled()).isEqualTo(true)
        assertThat(FeatureFlagResult.Enabled.isDisabled()).isEqualTo(false)
        assertThat(FeatureFlagResult.Enabled.isUnavailable()).isEqualTo(false)
        assertThat(FeatureFlagResult.Enabled.isDisabledOrUnavailable()).isEqualTo(false)

        assertThat(FeatureFlagResult.Disabled.isEnabled()).isEqualTo(false)
        assertThat(FeatureFlagResult.Disabled.isDisabled()).isEqualTo(true)
        assertThat(FeatureFlagResult.Disabled.isUnavailable()).isEqualTo(false)
        assertThat(FeatureFlagResult.Disabled.isDisabledOrUnavailable()).isEqualTo(true)

        assertThat(FeatureFlagResult.Unavailable.isEnabled()).isEqualTo(false)
        assertThat(FeatureFlagResult.Unavailable.isDisabled()).isEqualTo(false)
        assertThat(FeatureFlagResult.Unavailable.isUnavailable()).isEqualTo(true)
        assertThat(FeatureFlagResult.Unavailable.isDisabledOrUnavailable()).isEqualTo(true)
    }
}
