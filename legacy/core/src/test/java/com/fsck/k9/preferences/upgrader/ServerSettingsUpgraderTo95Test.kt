package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ServerSettingsUpgraderTo95Test {
    private val upgrader = ServerSettingsUpgraderTo95()

    @Test
    fun `server settings with empty username should be migrated to use authenticationType = NONE`() {
        val mutableSettings = mutableMapOf<String, Any?>(
            "authenticationType" to "PLAIN",
            "username" to "",
            "password" to "",
        )

        upgrader.upgrade(mutableSettings)

        assertThat(mutableSettings).isEqualTo(
            mapOf<String, Any?>(
                "authenticationType" to "NONE",
                "username" to "",
                "password" to null,
            ),
        )
    }

    @Test
    fun `server settings with username and no password should not be modified`() {
        val settings = mapOf<String, Any?>(
            "authenticationType" to "PLAIN",
            "username" to "user",
            "password" to null,
        )
        val mutableSettings = settings.toMutableMap()

        upgrader.upgrade(mutableSettings)

        assertThat(mutableSettings).isEqualTo(settings)
    }
}
