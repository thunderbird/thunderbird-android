package com.fsck.k9.preferences;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fsck.k9.K9;
import com.fsck.k9.preferences.Settings.SettingsDescription;
import timber.log.Timber;


class SettingsUpgradeHelper {
    /**
     * Upgrade settings using the settings structure and/or special upgrade code.
     *
     * @param version
     *         The content version of the settings in {@code settings}.
     * @param upgraders
     *         A map of {@link SettingsUpgrader}s for nontrivial settings upgrades.
     * @param settingsDescriptions
     *         The structure describing the different settings, possibly containing multiple versions.
     * @param settings
     *         The validated settings as returned by {@link Settings#validate(int, Map, Map, boolean)}.
     *
     * @return The upgraded settings.
     */
    public static Map<String, Object> upgrade(int version, Map<Integer, SettingsUpgrader> upgraders,
        Map<String, TreeMap<Integer, SettingsDescription<?>>> settingsDescriptions, Map<String, Object> settings) {

        Map<String, Object> upgradedSettings = new HashMap<>(settings);
        for (int toVersion = version + 1; toVersion <= Settings.VERSION; toVersion++) {
            if (upgraders.containsKey(toVersion)) {
                SettingsUpgrader upgrader = upgraders.get(toVersion);
                upgrader.upgrade(upgradedSettings);
            }

            upgradeSettingsGeneric(settingsDescriptions, upgradedSettings, toVersion);
        }

        return upgradedSettings;
    }

    private static void upgradeSettingsGeneric(
        Map<String, TreeMap<Integer, SettingsDescription<?>>> settingsDescriptions,
        Map<String, Object> mutableSettings, int toVersion) {
        for (Entry<String, TreeMap<Integer, SettingsDescription<?>>> versions : settingsDescriptions.entrySet()) {
            String settingName = versions.getKey();
            TreeMap<Integer, SettingsDescription<?>> versionedSettingsDescriptions = versions.getValue();

            boolean isNewlyAddedSetting = versionedSettingsDescriptions.firstKey() == toVersion;
            if (isNewlyAddedSetting) {
                boolean wasHandledByCustomUpgrader = mutableSettings.containsKey(settingName);
                if (wasHandledByCustomUpgrader) {
                    continue;
                }

                SettingsDescription<?> settingDescription = versionedSettingsDescriptions.get(toVersion);
                if (settingDescription == null) {
                    throw new AssertionError("First version of a setting must be non-null!");
                }
                upgradeSettingInsertDefault(mutableSettings, settingName, settingDescription);
            }

            Integer highestVersion = versionedSettingsDescriptions.lastKey();
            boolean isRemovedSetting = highestVersion == toVersion &&
                versionedSettingsDescriptions.get(highestVersion) == null;

            if (isRemovedSetting) {
                mutableSettings.remove(settingName);
                Timber.v("Removed setting \"%s\"", settingName);
            }
        }
    }

    private static <T> void upgradeSettingInsertDefault(Map<String, Object> mutableSettings,
        String settingName, SettingsDescription<T> settingDescription) {
        T defaultValue = settingDescription.getDefaultValue();
        mutableSettings.put(settingName, defaultValue);

        if (K9.isDebugLoggingEnabled()) {
            String prettyValue = settingDescription.toPrettyString(defaultValue);
            Timber.v("Added new setting \"%s\" with default value \"%s\"", settingName, prettyValue);
        }
    }
}
