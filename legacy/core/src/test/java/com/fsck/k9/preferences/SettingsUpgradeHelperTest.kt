package com.fsck.k9.preferences

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.fail
import com.fsck.k9.preferences.Settings.BooleanSetting
import com.fsck.k9.preferences.Settings.StringSetting
import kotlin.test.Test
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SettingsUpgradeHelperTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `upgrade() with new setting being added`() {
        val version = 1
        val upgraders = emptyMap<Int, SettingsUpgrader>()
        val settingsDescriptions = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
            ),
            "two" to versions(
                2 to StringSetting("default"),
            ),
        )
        val settings = mapOf<String, Any>(
            "one" to false,
        )

        val result = SettingsUpgradeHelper.upgrade(
            version,
            upgraders,
            settingsDescriptions,
            settings,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )

        assertThat(result).isEqualTo(
            mapOf(
                "one" to false,
                "two" to "default",
            ),
        )
    }

    @Test
    fun `upgrade() with setting being removed`() {
        val version = 1
        val upgraders = emptyMap<Int, SettingsUpgrader>()
        val settingsDescriptions = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
                2 to null,
            ),
            "two" to versions(
                2 to StringSetting("default"),
            ),
        )
        val settings = mapOf<String, Any>(
            "one" to false,
        )

        val result = SettingsUpgradeHelper.upgrade(
            version,
            upgraders,
            settingsDescriptions,
            settings,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )

        assertThat(result).isEqualTo(
            mapOf(
                "two" to "default",
            ),
        )
    }

    @Test
    fun `upgrade() with custom upgrader renaming a setting`() {
        val version = 1
        val upgraders = mapOf(
            2 to SettingsUpgrader { settings ->
                settings["two"] = settings["one"]
                setOf("one")
            },
        )
        val settingsDescriptions = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
                2 to null,
            ),
            "two" to versions(
                2 to BooleanSetting(true),
            ),
        )
        val settings = mutableMapOf<String, Any>(
            "one" to false,
        )

        val result = SettingsUpgradeHelper.upgrade(
            version,
            upgraders,
            settingsDescriptions,
            settings,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )

        assertThat(result).isEqualTo(
            mapOf(
                "two" to false,
            ),
        )
    }

    @Test
    fun `upgrade() with settings already at latest version`() {
        var upgraderCalled = false
        val version = Settings.VERSION
        val upgraders = mapOf(
            Settings.VERSION to SettingsUpgrader {
                upgraderCalled = true
            },
        )
        val settingsDescriptions = mapOf(
            "setting" to versions(
                1 to BooleanSetting(true),
            ),
        )
        val settings = mapOf<String, Any>(
            "setting" to false,
        )

        val result = SettingsUpgradeHelper.upgrade(
            version,
            upgraders,
            settingsDescriptions,
            settings,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )

        assertThat(result).isEqualTo(
            mapOf(
                "setting" to false,
            ),
        )
        assertThat(upgraderCalled).isFalse()
    }

    @Test
    fun `upgrade() with first version of setting being null should throw`() {
        val version = 1
        val upgraders = emptyMap<Int, SettingsUpgrader>()
        val settingsDescriptions = mapOf(
            "setting" to versions(
                2 to null,
            ),
        )
        val settings = mapOf<String, Any>(
            "settings" to "1",
        )

        assertFailure {
            SettingsUpgradeHelper.upgrade(
                version,
                upgraders,
                settingsDescriptions,
                settings,
                mock { on { getConfig() } doReturn GeneralSettings() },
            )
        }.isInstanceOf<AssertionError>()
            .hasMessage("First version of a setting must be non-null!")
    }

    @Test
    fun `upgradeToVersion() to intermediate version`() {
        val version = 1
        val upgraders = emptyMap<Int, SettingsUpgrader>()
        val settingsDescriptions = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
            ),
            "two" to versions(
                2 to StringSetting("default"),
            ),
            "three" to versions(
                3 to BooleanSetting(false),
            ),
        )
        val settings = mapOf<String, Any>(
            "one" to false,
        )

        val result = SettingsUpgradeHelper.upgradeToVersion(
            targetVersion = 2,
            version,
            upgraders,
            settingsDescriptions,
            settings,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )

        assertThat(result).isEqualTo(
            mapOf(
                "one" to false,
                "two" to "default",
            ),
        )
    }

    @Test
    fun `upgradeToVersion() with custom upgrader`() {
        val version = 1
        val upgraders = mapOf(
            2 to SettingsUpgrader { settings ->
                settings["two"] = "custom"
            },
            3 to SettingsUpgrader {
                fail("SettingsUpgrader for version 3 should not be invoked")
            },
        )
        val settingsDescriptions = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
            ),
            "two" to versions(
                2 to StringSetting("default"),
            ),
            "three" to versions(
                3 to BooleanSetting(false),
            ),
        )
        val settings = mapOf<String, Any>(
            "one" to false,
        )

        val result = SettingsUpgradeHelper.upgradeToVersion(
            targetVersion = 2,
            version,
            upgraders,
            settingsDescriptions,
            settings,
            mock { on { getConfig() } doReturn GeneralSettings() },
        )

        assertThat(result).isEqualTo(
            mapOf(
                "one" to false,
                "two" to "custom",
            ),
        )
    }
}
