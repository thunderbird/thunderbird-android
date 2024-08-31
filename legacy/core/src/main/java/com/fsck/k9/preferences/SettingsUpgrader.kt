package com.fsck.k9.preferences

/**
 * Used for a nontrivial settings upgrade.
 *
 * @see Settings.upgrade
 */
internal fun interface SettingsUpgrader {
    /**
     * Upgrade the provided settings.
     *
     * @param settings The settings to upgrade. This map is modified and contains the upgraded settings when this
     *   method returns.
     */
    fun upgrade(settings: MutableMap<String, Any?>)
}
