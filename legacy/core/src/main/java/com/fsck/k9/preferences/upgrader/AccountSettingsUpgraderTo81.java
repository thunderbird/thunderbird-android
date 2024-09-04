package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.SettingsUpgrader;


/**
 * Rewrite the per-network type IMAP compression settings to a single setting.
 */
public class AccountSettingsUpgraderTo81 implements SettingsUpgrader {
    @Override
    public void upgrade(Map<String, Object> settings) {
        Boolean useCompressionWifi = (Boolean) settings.get("useCompression.WIFI");
        Boolean useCompressionMobile = (Boolean) settings.get("useCompression.MOBILE");
        Boolean useCompressionOther = (Boolean) settings.get("useCompression.OTHER");

        boolean useCompression = useCompressionWifi != null && useCompressionMobile != null &&
            useCompressionOther != null && useCompressionWifi && useCompressionMobile && useCompressionOther;
        settings.put("useCompression", useCompression);
    }
}
