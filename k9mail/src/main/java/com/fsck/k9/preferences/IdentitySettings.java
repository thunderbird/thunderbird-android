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
import com.fsck.k9.preferences.Settings.*;

public class IdentitySettings {
    public static final Map<String, TreeMap<Integer, SettingsDescription>> SETTINGS;
    public static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription>> s =
            new LinkedHashMap<String, TreeMap<Integer, SettingsDescription>>();

        /**
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

        Map<Integer, SettingsUpgrader> u = new HashMap<Integer, SettingsUpgrader>();
        UPGRADERS = Collections.unmodifiableMap(u);
    }

    public static Map<String, Object> validate(int version, Map<String, String> importedSettings,
            boolean useDefaultValues) {
        return Settings.validate(version, SETTINGS, importedSettings, useDefaultValues);
    }

    public static Set<String> upgrade(int version, Map<String, Object> validatedSettings) {
        return Settings.upgrade(version, UPGRADERS, SETTINGS, validatedSettings);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }

    public static Map<String, String> getIdentitySettings(Storage storage, String uuid,
            int identityIndex) {
        Map<String, String> result = new HashMap<String, String>();
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


    public static boolean isEmailAddressValid(String email) {
        return new EmailAddressValidator().isValidAddressOnly(email);
    }

    /**
     * The message signature setting.
     */
    public static class SignatureSetting extends SettingsDescription {
        public SignatureSetting() {
            super(null);
        }

        @Override
        public Object getDefaultValue() {
            return K9.app.getResources().getString(R.string.default_signature);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            return value;
        }
    }

    /**
     * An optional email address setting.
     */
    public static class OptionalEmailAddressSetting extends SettingsDescription {
        private EmailAddressValidator mValidator;

        public OptionalEmailAddressSetting() {
            super(null);
            mValidator = new EmailAddressValidator();
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            if (value != null && !mValidator.isValidAddressOnly(value)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }

        @Override
        public String toString(Object value) {
            return (value != null) ? value.toString() : null;
        }

        @Override
        public String toPrettyString(Object value) {
            return (value == null) ? "" : value.toString();
        }

        @Override
        public Object fromPrettyString(String value) throws InvalidSettingValueException {
            return ("".equals(value)) ? null : fromString(value);
        }
    }
}
