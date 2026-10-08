package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.doesNotContainKey
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.preference.AnimationPreference

class GeneralSettingsUpgraderTo111Test {

    private val upgrader = GeneralSettingsUpgraderTo111()

    @Test
    fun `should convert animations=true to ON`() {
        val settings = mutableMapOf<String, Any?>(ANIMATIONS_KEY to true)

        upgrader.upgrade(settings)

        assertThat(settings[ANIMATIONS_KEY]).isEqualTo(AnimationPreference.ON)
    }

    @Test
    fun `should convert animations=false to OFF`() {
        val settings = mutableMapOf<String, Any?>(ANIMATIONS_KEY to false)

        upgrader.upgrade(settings)

        assertThat(settings[ANIMATIONS_KEY]).isEqualTo(AnimationPreference.OFF)
    }

    @Test
    fun `should not insert animations key when missing`() {
        val settings = mutableMapOf<String, Any?>()

        upgrader.upgrade(settings)

        assertThat(settings).doesNotContainKey(ANIMATIONS_KEY)
    }

    @Test
    fun `should leave existing AnimationPreference value unchanged`() {
        val settings = mutableMapOf<String, Any?>(ANIMATIONS_KEY to AnimationPreference.FOLLOW_SYSTEM)

        upgrader.upgrade(settings)

        assertThat(settings[ANIMATIONS_KEY]).isEqualTo(AnimationPreference.FOLLOW_SYSTEM)
    }

    private companion object {
        const val ANIMATIONS_KEY = "animations"
    }
}
