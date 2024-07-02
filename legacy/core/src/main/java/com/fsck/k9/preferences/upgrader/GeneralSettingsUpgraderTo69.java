package com.fsck.k9.preferences.upgrader;


import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.preferences.Settings.SettingsUpgrader;


/**
 * Renames {@code hideSpecialAccounts} to {@code showUnifiedInbox}.
 */
public class GeneralSettingsUpgraderTo69 implements SettingsUpgrader {

    @Override
    public Set<String> upgrade(Map<String, Object> settings) {
        Boolean hideSpecialAccounts = (Boolean) settings.get("hideSpecialAccounts");
        boolean showUnifiedInbox = hideSpecialAccounts == null || !hideSpecialAccounts;
        settings.put("showUnifiedInbox", showUnifiedInbox);

        return new HashSet<>(Collections.singleton("hideSpecialAccounts"));
    }
}
