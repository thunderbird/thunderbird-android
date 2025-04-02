package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import app.k9mail.legacy.di.DI;
import com.fsck.k9.notification.NotificationLightDecoder;
import com.fsck.k9.preferences.SettingsUpgrader;
import net.thunderbird.feature.notification.NotificationLight;


/**
 * Rewrites 'led' and 'lecColor' to 'notificationLight'.
 */
public class AccountSettingsUpgraderTo80 implements SettingsUpgrader {
    private final NotificationLightDecoder notificationLightDecoder = DI.get(NotificationLightDecoder.class);

    @Override
    public void upgrade(Map<String, Object> settings) {
        Boolean isLedEnabled = (Boolean) settings.get("led");
        Integer ledColor = (Integer) settings.get("ledColor");
        Integer chipColor = (Integer) settings.get("chipColor");

        if (isLedEnabled != null && ledColor != null) {
            int accountColor = chipColor != null ? chipColor : 0;
            NotificationLight light = notificationLightDecoder.decode(isLedEnabled, ledColor, accountColor);
            settings.put("notificationLight", light.name());
        }
    }
}
