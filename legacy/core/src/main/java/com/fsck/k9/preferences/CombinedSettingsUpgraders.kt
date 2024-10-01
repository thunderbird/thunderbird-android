package com.fsck.k9.preferences

import com.fsck.k9.preferences.upgrader.CombinedSettingsUpgraderTo96
import com.fsck.k9.preferences.upgrader.CombinedSettingsUpgraderTo98
import com.fsck.k9.preferences.upgrader.CombinedSettingsUpgraderTo99

internal typealias CombinedSettingsUpgraderFactory = () -> CombinedSettingsUpgrader

@Suppress("MagicNumber")
internal object CombinedSettingsUpgraders {
    val UPGRADERS = mapOf<Int, CombinedSettingsUpgraderFactory>(
        96 to ::CombinedSettingsUpgraderTo96,
        98 to ::CombinedSettingsUpgraderTo98,
        99 to ::CombinedSettingsUpgraderTo99,
    )
}
