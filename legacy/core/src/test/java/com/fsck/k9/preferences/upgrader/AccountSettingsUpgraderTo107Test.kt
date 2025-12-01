package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AccountSettingsUpgraderTo107Test {

    @Test
    fun `should set automaticCheckIntervalMinutes when missing`() {
        val settings = mutableMapOf<String, Any?>()

        AccountSettingsUpgraderTo107().upgrade(settings)

        assertThat(settings[AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY]).isEqualTo(NEW_CHECK_INTERVAL_MINUTES)
    }

    @Test
    fun `should change automaticCheckIntervalMinutes from old default to new default value`() {
        val settings = mutableMapOf<String, Any?>(
            AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY to OLD_DEFAULT_CHECK_INTERVAL_MINUTES,
        )

        AccountSettingsUpgraderTo107().upgrade(settings)

        assertThat(settings[AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY]).isEqualTo(NEW_CHECK_INTERVAL_MINUTES)
    }

    @Test
    fun `should keep custom automaticCheckIntervalMinutes for all allowed values`() {
        val customValues = listOf(-1, 15, 30, 120, 180, 360, 720, 1440)

        customValues.forEach { value ->
            val settings = mutableMapOf<String, Any?>(AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY to value)

            AccountSettingsUpgraderTo107().upgrade(settings)

            assertThat(settings[AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY]).isEqualTo(value)
        }
    }

    private companion object {
        const val AUTOMATIC_CHECK_INTERVAL_MINUTES_KEY = "automaticCheckIntervalMinutes"
        const val OLD_DEFAULT_CHECK_INTERVAL_MINUTES = 60
        const val NEW_CHECK_INTERVAL_MINUTES = 15
    }
}
