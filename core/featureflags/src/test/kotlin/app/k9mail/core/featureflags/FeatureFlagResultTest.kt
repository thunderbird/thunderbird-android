package app.k9mail.core.featureflags

import app.k9mail.core.featureflag.FeatureFlagResult
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
}
