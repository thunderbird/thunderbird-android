package com.fsck.k9.preferences.upgrader;


import java.util.Map;
import java.util.Set;

import app.k9mail.legacy.preferences.AppTheme;
import com.fsck.k9.preferences.Settings.SettingsUpgrader;


/**
 * Set <em>theme</em> to {@link AppTheme#FOLLOW_SYSTEM} if <em>theme</em> has the value {@link AppTheme#LIGHT}.
 */
public class GeneralSettingsUpgraderTo58 implements SettingsUpgrader {

    @Override
    public Set<String> upgrade(Map<String, Object> settings) {
        AppTheme theme = (AppTheme) settings.get("theme");
        if (theme == AppTheme.LIGHT) {
            settings.put("theme", AppTheme.FOLLOW_SYSTEM);
        }

        return null;
    }
}
