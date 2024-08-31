package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.SettingsUpgrader;


/**
 * Replace folder entries of "-NONE-" with {@code null}.
 */
public class AccountSettingsUpgraderTo53 implements SettingsUpgrader {
    public static final String FOLDER_NONE = "-NONE-";

    @Override
    public void upgrade(Map<String, Object> settings) {
        upgradeFolderEntry(settings, "archiveFolderName");
        upgradeFolderEntry(settings, "autoExpandFolderName");
        upgradeFolderEntry(settings, "draftsFolderName");
        upgradeFolderEntry(settings, "sentFolderName");
        upgradeFolderEntry(settings, "spamFolderName");
        upgradeFolderEntry(settings, "trashFolderName");
    }

    private void upgradeFolderEntry(Map<String, Object> settings, String key) {
        String archiveFolderName = (String) settings.get(key);
        if (FOLDER_NONE.equals(archiveFolderName)) {
            settings.put(key, null);
        }
    }
}
