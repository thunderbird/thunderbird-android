package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.SettingsUpgrader;


/**
 * Convert value from <em>fontSizeMessageViewContent</em> to <em>fontSizeMessageViewContentPercent</em>.
 */
public class GeneralSettingsUpgraderTo31 implements SettingsUpgrader {

    @Override
    public void upgrade(Map<String, Object> settings) {
        int oldSize = (Integer) settings.get("fontSizeMessageViewContent");

        int newSize = convertFromOldSize(oldSize);

        settings.put("fontSizeMessageViewContentPercent", newSize);
    }

    public static int convertFromOldSize(int oldSize) {
        switch (oldSize) {
            case 1: {
                return 40;
            }
            case 2: {
                return 75;
            }
            case 4: {
                return 175;
            }
            case 5: {
                return 250;
            }
            case 3:
            default: {
                return 100;
            }
        }
    }
}
