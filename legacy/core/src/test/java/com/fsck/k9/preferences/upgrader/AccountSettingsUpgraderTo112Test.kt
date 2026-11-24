package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.core.android.account.MessageFormat

class AccountSettingsUpgraderTo112Test {

    @Test
    fun `should migrate messageFormatAuto true to messageFormat AUTO and remove messageFormatAuto`() {
        val settings = mutableMapOf<String, Any?>(
            "messageFormatAuto" to true,
            "messageFormat" to "TEXT",
        )

        AccountSettingsUpgraderTo112().upgrade(settings)

        assertThat(settings["messageFormat"]).isEqualTo(MessageFormat.AUTO.name)
        assertThat(settings["messageFormatAuto"]).isNull()
    }

    @Test
    fun `should leave messageFormat unchanged when messageFormatAuto is false`() {
        val settings = mutableMapOf<String, Any?>(
            "messageFormatAuto" to false,
            "messageFormat" to "HTML",
        )

        AccountSettingsUpgraderTo112().upgrade(settings)

        assertThat(settings["messageFormat"]).isEqualTo("HTML")
        assertThat(settings["messageFormatAuto"]).isNull()
    }

    @Test
    fun `should leave messageFormat unchanged when messageFormatAuto is missing`() {
        val settings = mutableMapOf<String, Any?>(
            "messageFormat" to "TEXT",
        )

        AccountSettingsUpgraderTo112().upgrade(settings)

        assertThat(settings["messageFormat"]).isEqualTo("TEXT")
        assertThat(settings["messageFormatAuto"]).isNull()
    }
}
