package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.SettingsDescription
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.GeneralSettingsManager

internal object SettingsUpgradeHelper {
    /**
     * Upgrade settings using the settings structure and/or special upgrade code.
     *
     * @param version
     *   The content version of the settings in `settings`.
     * @param upgraders
     *   A map of [SettingsUpgrader]s for nontrivial settings upgrades.
     * @param settingsDescriptions
     *   The structure describing the different settings, possibly containing multiple versions.
     * @param settings
     *   The validated settings as returned by [Settings.validate].
     *
     * @return The upgraded settings.
     */
    @JvmStatic
    fun upgrade(
        version: Int,
        upgraders: Map<Int, SettingsUpgrader>,
        settingsDescriptions: SettingsDescriptions,
        settings: Map<String, Any?>,
        generalSettingsManager: GeneralSettingsManager,
    ): Map<String, Any?> {
        return upgradeToVersion(
            Settings.VERSION,
            version,
            upgraders,
            settingsDescriptions,
            settings,
            generalSettingsManager,
        )
    }

    fun upgradeToVersion(
        targetVersion: Int,
        version: Int,
        upgraders: Map<Int, SettingsUpgrader>,
        settingsDescriptions: SettingsDescriptions,
        settings: Map<String, Any?>,
        generalSettingsManager: GeneralSettingsManager,
    ): Map<String, Any?> {
        val upgradedSettings = settings.toMutableMap()

        for (toVersion in version + 1..targetVersion) {
            upgraders[toVersion]?.upgrade(upgradedSettings)

            upgradeSettingsGeneric(settingsDescriptions, upgradedSettings, toVersion, generalSettingsManager)
        }

        return upgradedSettings
    }

    private fun upgradeSettingsGeneric(
        settingsDescriptions: SettingsDescriptions,
        mutableSettings: MutableMap<String, Any?>,
        toVersion: Int,
        generalSettingsManager: GeneralSettingsManager,
    ) {
        for ((settingName, versionedSettingsDescriptions) in settingsDescriptions) {
            val isNewlyAddedSetting = versionedSettingsDescriptions.firstKey() == toVersion
            if (isNewlyAddedSetting) {
                val wasHandledByCustomUpgrader = mutableSettings.containsKey(settingName)
                if (wasHandledByCustomUpgrader) {
                    continue
                }

                val settingDescription = versionedSettingsDescriptions[toVersion]
                    ?: throw AssertionError("First version of a setting must be non-null!")

                upgradeSettingInsertDefault(mutableSettings, settingName, settingDescription, generalSettingsManager)
            }

            val highestVersion = versionedSettingsDescriptions.lastKey()
            val isRemovedSetting = highestVersion == toVersion &&
                versionedSettingsDescriptions[highestVersion] == null

            if (isRemovedSetting) {
                mutableSettings.remove(settingName)
                Log.v("Removed setting '%s'", settingName)
            }
        }
    }

    private fun <T> upgradeSettingInsertDefault(
        mutableSettings: MutableMap<String, Any?>,
        settingName: String,
        settingDescription: SettingsDescription<T>,
        generalSettingsManager: GeneralSettingsManager,
    ) {
        val defaultValue = settingDescription.getDefaultValue()
        mutableSettings[settingName] = defaultValue

        if (generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled) {
            val prettyValue = settingDescription.toPrettyString(defaultValue)
            Log.v("Added new setting '%s' with default value '%s'", settingName, prettyValue)
        }
    }
}
