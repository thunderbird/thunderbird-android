package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import app.k9mail.legacy.preferences.AppTheme;
import app.k9mail.legacy.preferences.SubTheme;
import com.fsck.k9.preferences.SettingsUpgrader;


/**
 * Set <em>messageViewTheme</em> to {@link SubTheme#USE_GLOBAL} if <em>messageViewTheme</em> has the same value as
 * <em>theme</em>.
 */
public class GeneralSettingsUpgraderTo24 implements SettingsUpgrader {

    @Override
    public void upgrade(Map<String, Object> settings) {
        SubTheme messageViewTheme = (SubTheme) settings.get("messageViewTheme");
        AppTheme theme = (AppTheme) settings.get("theme");
        if ((theme == AppTheme.LIGHT && messageViewTheme == SubTheme.LIGHT) ||
            (theme == AppTheme.DARK && messageViewTheme == SubTheme.DARK)) {
            settings.put("messageViewTheme", SubTheme.USE_GLOBAL);
        }
    }
}
