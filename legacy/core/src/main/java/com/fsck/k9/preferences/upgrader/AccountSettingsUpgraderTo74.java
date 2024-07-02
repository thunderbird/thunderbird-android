package com.fsck.k9.preferences.upgrader;


import java.util.Map;
import java.util.Set;

import com.fsck.k9.preferences.Settings.SettingsUpgrader;


/**
 * Rewrites 'idleRefreshMinutes' from '1' to '2' if necessary
 */
public class AccountSettingsUpgraderTo74 implements SettingsUpgrader {
    @Override
    public Set<String> upgrade(Map<String, Object> settings) {
        Integer idleRefreshMinutes = (Integer) settings.get("idleRefreshMinutes");
        if (idleRefreshMinutes == 1) {
            settings.put("idleRefreshMinutes", 2);
        }

        return null;
    }
}
