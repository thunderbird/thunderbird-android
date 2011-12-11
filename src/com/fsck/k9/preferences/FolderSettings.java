package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import android.content.SharedPreferences;

import com.fsck.k9.mail.Folder.FolderClass;
import com.fsck.k9.preferences.Settings.*;

public class FolderSettings {
    public static final Map<String, TreeMap<Integer, SettingsDescription>> SETTINGS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription>> s =
            new LinkedHashMap<String, TreeMap<Integer, SettingsDescription>>();

        s.put("displayMode", Settings.versions(
                new V(1, new EnumSetting(FolderClass.class, FolderClass.NO_CLASS))
            ));
        s.put("syncMode", Settings.versions(
                new V(1, new EnumSetting(FolderClass.class, FolderClass.INHERITED))
            ));
        s.put("pushMode", Settings.versions(
                new V(1, new EnumSetting(FolderClass.class, FolderClass.INHERITED))
            ));
        s.put("inTopGroup", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("integrate", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));

        SETTINGS = Collections.unmodifiableMap(s);
    }

    public static Map<String, String> validate(int version, Map<String, String> importedSettings,
            boolean useDefaultValues) {
        return Settings.validate(version, SETTINGS, importedSettings, useDefaultValues);
    }

    public static Map<String, String> getFolderSettings(SharedPreferences storage, String uuid,
            String folderName) {
        Map<String, String> result = new HashMap<String, String>();
        String prefix = uuid + "." + folderName + ".";
        for (String key : SETTINGS.keySet()) {
            String value = storage.getString(prefix + key, null);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
}
