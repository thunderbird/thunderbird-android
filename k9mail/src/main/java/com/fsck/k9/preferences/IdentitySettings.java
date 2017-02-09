package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.preferences.Settings.BooleanSetting;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import com.fsck.k9.preferences.Settings.SettingsDescription;
import com.fsck.k9.preferences.Settings.SettingsUpgrader;
import com.fsck.k9.preferences.Settings.V;


class IdentitySettings {
    static final Map<String, TreeMap<Integer, SettingsDescription>> SETTINGS;
    private static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription>> s = new LinkedHashMap<>();

        /*
         * When adding new settings here, be sure to increment {@link Settings.VERSION}
         * and use that for whatever you add here.
         */

        s.put("signature", Settings.versions(
                new V(1, new SignatureSetting())
        ));
        s.put("signatureUse", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("replyTo", Settings.versions(
                new V(1, new OptionalEmailAddressSetting())
        ));

        SETTINGS = Collections.unmodifiableMap(s);

        // noinspection MismatchedQueryAndUpdateOfCollection, this map intentionally left blank
        Map<Integer, SettingsUpgrader> u = new HashMap<>();
        UPGRADERS = Collections.unmodifiableMap(u);
    }

    static Map<String, Object> validate(int version, Map<String, String> importedSettings, boolean useDefaultValues) {
        return Settings.validate(version, SETTINGS, importedSettings, useDefaultValues);
    }

    public static Set<String> upgrade(int version, Map<String, Object> validatedSettings) {
        return Settings.upgrade(version, UPGRADERS, SETTINGS, validatedSettings);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }

    static Map<String, String> getIdentitySettings(Storage storage, String uuid, int identityIndex) {
        Map<String, String> result = new HashMap<>();
        String prefix = uuid + ".";
        String suffix = "." + Integer.toString(identityIndex);
        for (String key : SETTINGS.keySet()) {
            String value = storage.getString(prefix + key + suffix, null);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }


    static boolean isEmailAddressValid(String email) {
        return new EmailAddressValidator().isValidAddressOnly(email);
    }

    private static class SignatureSetting extends SettingsDescription<String> {
        SignatureSetting() {
            super(null);
        }

        @Override
        public String getDefaultValue() {
            return K9.app.getResources().getString(R.string.default_signature);
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            return value;
        }
    }

    private static class OptionalEmailAddressSetting extends SettingsDescription<String> {
        private EmailAddressValidator validator;

        OptionalEmailAddressSetting() {
            super(null);
            validator = new EmailAddressValidator();
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            if (value != null && !validator.isValidAddressOnly(value)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }

        @Override
        public String toString(String value) {
            return (value != null) ? value : null;
        }

        @Override
        public String toPrettyString(String value) {
            return (value == null) ? "" : value;
        }

        @Override
        public String fromPrettyString(String value) throws InvalidSettingValueException {
            return "".equals(value) ? null : fromString(value);
        }
    }
}
