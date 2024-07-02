package com.fsck.k9.preferences.upgrader;


import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.preferences.Settings.SettingsUpgrader;


/**
 * Convert value from <em>fontSizeMessageViewContent</em> to <em>fontSizeMessageViewContentPercent</em>.
 */
public class GeneralSettingsUpgraderTo31 implements SettingsUpgrader {

    @Override
    public Set<String> upgrade(Map<String, Object> settings) {
        int oldSize = (Integer) settings.get("fontSizeMessageViewContent");

        int newSize = convertFromOldSize(oldSize);

        settings.put("fontSizeMessageViewContentPercent", newSize);

        return new HashSet<>(Collections.singletonList("fontSizeMessageViewContent"));
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
