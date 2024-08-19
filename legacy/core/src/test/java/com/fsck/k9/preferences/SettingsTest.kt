package com.fsck.k9.preferences

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import com.fsck.k9.preferences.Settings.BooleanSetting
import com.fsck.k9.preferences.Settings.SettingsDescription
import com.fsck.k9.preferences.Settings.SettingsUpgrader
import com.fsck.k9.preferences.Settings.StringSetting
import java.util.TreeMap
import kotlin.test.Test

class SettingsTest {
    @Test
    fun `upgrade() with new setting being added`() {
        val version = 1
        val upgraders = emptyMap<Int, SettingsUpgrader>()
        val settings = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
            ),
            "two" to versions(
                2 to StringSetting("default"),
            ),
        )
        val validatedSettings = mutableMapOf<String, Any>(
            "one" to false,
        )

        Settings.upgrade(version, upgraders, settings, validatedSettings)

        assertThat(validatedSettings).isEqualTo(
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
        val settings = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
                2 to null,
            ),
            "two" to versions(
                2 to StringSetting("default"),
            ),
        )
        val validatedSettings = mutableMapOf<String, Any>(
            "one" to false,
        )

        Settings.upgrade(version, upgraders, settings, validatedSettings)

        assertThat(validatedSettings).isEqualTo(
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
        val settings = mapOf(
            "one" to versions(
                1 to BooleanSetting(true),
                2 to null,
            ),
            "two" to versions(
                2 to BooleanSetting(true),
            ),
        )
        val validatedSettings = mutableMapOf<String, Any>(
            "one" to false,
        )

        Settings.upgrade(version, upgraders, settings, validatedSettings)

        assertThat(validatedSettings).isEqualTo(
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
        val settings = mapOf(
            "setting" to versions(
                1 to BooleanSetting(true),
            ),
        )
        val validatedSettings = mutableMapOf<String, Any>(
            "setting" to false,
        )

        Settings.upgrade(version, upgraders, settings, validatedSettings)

        assertThat(validatedSettings).isEqualTo(
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
        val settings = mapOf(
            "setting" to versions(
                2 to null,
            ),
        )
        val validatedSettings = mutableMapOf<String, Any>(
            "settings" to "1",
        )

        assertFailure {
            Settings.upgrade(version, upgraders, settings, validatedSettings)
        }.isInstanceOf<AssertionError>()
            .hasMessage("First version of a setting must be non-null!")
    }

    private fun versions(
        vararg pairs: Pair<Int, SettingsDescription<*>?>,
    ): TreeMap<Int, SettingsDescription<*>?> {
        return pairs.toMap(TreeMap<Int, SettingsDescription<*>?>())
    }
}
