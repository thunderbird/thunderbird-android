package app.k9mail.core.featureflags

import app.k9mail.core.featureflag.FeatureFlagResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class FeatureFlagResultTest {

    @Test
    fun `should only call onEnabled when enabled`() {
        val testSubject = FeatureFlagResult.Enabled

        var result = ""

        testSubject.onEnabled {
            result = "enabled"
        }.onDisabled {
            result = "disabled"
        }.onUnavailable {
            result = "unavailable"
        }

        assertThat(result).isEqualTo("enabled")
    }

    @Test
    fun `should only call onDisabled when disabled`() {
        val testSubject = FeatureFlagResult.Disabled

        var result = ""

        testSubject.onEnabled {
            result = "enabled"
        }.onDisabled {
            result = "disabled"
        }.onUnavailable {
            result = "unavailable"
        }

        assertThat(result).isEqualTo("disabled")
    }

    @Test
    fun `should only call onUnavailable when unavailable`() {
        val testSubject = FeatureFlagResult.Unavailable

        var result = ""

        testSubject.onEnabled {
            result = "enabled"
        }.onDisabled {
            result = "disabled"
        }.onUnavailable {
            result = "unavailable"
        }

        assertThat(result).isEqualTo("unavailable")
    }
}
