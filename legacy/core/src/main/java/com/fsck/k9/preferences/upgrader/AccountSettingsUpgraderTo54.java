package com.fsck.k9.preferences.upgrader;


import java.util.Map;
import java.util.Set;

import com.fsck.k9.preferences.Settings.SettingsUpgrader;


/**
 * Inserts folder selection entries with a value of "MANUAL"
 */
public class AccountSettingsUpgraderTo54 implements SettingsUpgrader {
    private static final String FOLDER_SELECTION_MANUAL = "MANUAL";

    @Override
    public Set<String> upgrade(Map<String, Object> settings) {
        settings.put("archiveFolderSelection", FOLDER_SELECTION_MANUAL);
        settings.put("draftsFolderSelection", FOLDER_SELECTION_MANUAL);
        settings.put("sentFolderSelection", FOLDER_SELECTION_MANUAL);
        settings.put("spamFolderSelection", FOLDER_SELECTION_MANUAL);
        settings.put("trashFolderSelection", FOLDER_SELECTION_MANUAL);

        return null;
    }
}
