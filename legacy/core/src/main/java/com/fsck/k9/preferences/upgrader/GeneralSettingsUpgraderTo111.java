package com.fsck.k9.preferences.upgrader;


import java.util.Map;

import com.fsck.k9.preferences.SettingsUpgrader;
import net.thunderbird.core.preference.AnimationPreference;


/**
 * Convert <em>animations</em> from boolean to {@link AnimationPreference} enum.
 *
 * {@code true} maps to {@link AnimationPreference#ON}, {@code false} maps to {@link AnimationPreference#OFF}.
 */
public class GeneralSettingsUpgraderTo111 implements SettingsUpgrader {

    @Override
    public void upgrade(Map<String, Object> settings) {
        Object animations = settings.get("animations");
        if (animations instanceof Boolean) {
            boolean value = (Boolean) animations;
            settings.put("animations", value ? AnimationPreference.ON : AnimationPreference.OFF);
        }
    }
}
