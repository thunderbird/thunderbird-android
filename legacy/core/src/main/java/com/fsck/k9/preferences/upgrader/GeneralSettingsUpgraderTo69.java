package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.Settings.SettingsUpgrader;


/**
 * Renames {@code hideSpecialAccounts} to {@code showUnifiedInbox}.
 */
public class GeneralSettingsUpgraderTo69 implements SettingsUpgrader {

    @Override
    public void upgrade(Map<String, Object> settings) {
        Boolean hideSpecialAccounts = (Boolean) settings.get("hideSpecialAccounts");
        boolean showUnifiedInbox = hideSpecialAccounts == null || !hideSpecialAccounts;
        settings.put("showUnifiedInbox", showUnifiedInbox);
    }
}
