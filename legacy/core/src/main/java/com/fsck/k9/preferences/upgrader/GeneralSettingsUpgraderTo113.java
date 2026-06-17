package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.SettingsUpgrader;
import net.thunderbird.core.preference.network.NetworkProxyType;


/**
 * Enable global proxy for existing users who already configured proxy type, host, and port.
 */
public class GeneralSettingsUpgraderTo113 implements SettingsUpgrader {

    @Override
    public void upgrade(Map<String, Object> settings) {
        if (settings.containsKey("isProxyEnabled")) {
            return;
        }

        Object proxyType = settings.get("defaultProxyType");
        Object proxyHost = settings.get("defaultProxyHost");
        Object proxyPort = settings.get("defaultProxyPort");

        boolean enabled = proxyType instanceof NetworkProxyType
            && proxyType != NetworkProxyType.NONE
            && proxyHost instanceof String
            && !((String) proxyHost).isBlank()
            && proxyPort instanceof Integer
            && (Integer) proxyPort > 0;

        settings.put("isProxyEnabled", enabled);
    }
}
