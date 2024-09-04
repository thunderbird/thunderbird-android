package com.fsck.k9.preferences

internal typealias CombinedSettingsUpgraderFactory = () -> CombinedSettingsUpgrader

internal object CombinedSettingsUpgraders {
    val UPGRADERS = emptyMap<Int, CombinedSettingsUpgraderFactory>()
}
