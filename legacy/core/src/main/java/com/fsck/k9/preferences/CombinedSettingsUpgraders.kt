package com.fsck.k9.preferences

import com.fsck.k9.preferences.upgrader.CombinedSettingsUpgraderTo96

internal typealias CombinedSettingsUpgraderFactory = () -> CombinedSettingsUpgrader

@Suppress("MagicNumber")
internal object CombinedSettingsUpgraders {
    val UPGRADERS = mapOf<Int, CombinedSettingsUpgraderFactory>(
        96 to ::CombinedSettingsUpgraderTo96,
    )
}
