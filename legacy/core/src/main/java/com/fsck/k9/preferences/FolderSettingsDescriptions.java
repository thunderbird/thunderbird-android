package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.fsck.k9.preferences.legacy.FolderClass;
import com.fsck.k9.preferences.Settings.BooleanSetting;
import com.fsck.k9.preferences.Settings.EnumSetting;
import com.fsck.k9.preferences.Settings.SettingsDescription;
import com.fsck.k9.preferences.Settings.V;


class FolderSettingsDescriptions {
    static final Map<String, TreeMap<Integer, SettingsDescription<?>>> SETTINGS;
    static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription<?>>> s = new LinkedHashMap<>();

        /*
         * When adding new settings here, be sure to increment {@link Settings.VERSION}
         * and use that for whatever you add here.
         */

        s.put("displayMode", Settings.versions(
                new V(1, new EnumSetting<>(FolderClass.class, FolderClass.NO_CLASS)),
                new V(100, null)
        ));
        s.put("notifyMode", Settings.versions(
                new V(34, new EnumSetting<>(FolderClass.class, FolderClass.INHERITED)),
                new V(96, null)
        ));
        s.put("syncMode", Settings.versions(
                new V(1, new EnumSetting<>(FolderClass.class, FolderClass.INHERITED)),
                new V(99, null)
        ));
        s.put("pushMode", Settings.versions(
                new V(1, new EnumSetting<>(FolderClass.class, FolderClass.INHERITED)),
                new V(66, new EnumSetting<>(FolderClass.class, FolderClass.SECOND_CLASS)),
                new V(98, null)
        ));
        s.put("inTopGroup", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("integrate", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("notificationsEnabled", Settings.versions(
            new V(96, new BooleanSetting(false))
        ));
        s.put("pushEnabled", Settings.versions(
            new V(98, new BooleanSetting(false))
        ));
        s.put("syncEnabled", Settings.versions(
            new V(99, new BooleanSetting(false))
        ));
        s.put("visible", Settings.versions(
            new V(100, new BooleanSetting(true))
        ));

        SETTINGS = Collections.unmodifiableMap(s);

        // noinspection MismatchedQueryAndUpdateOfCollection, this map intentionally left blank
        Map<Integer, SettingsUpgrader> u = new HashMap<>();
        UPGRADERS = Collections.unmodifiableMap(u);
    }

    static Map<String, Object> validate(int version, Map<String, String> importedSettings, boolean useDefaultValues) {
        return Settings.validate(version, SETTINGS, importedSettings, useDefaultValues);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }
}
