package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.content.SharedPreferences;

import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.preferences.Settings.*;

public class IdentitySettings {
    public static final Map<String, SettingsDescription> SETTINGS;

    static {
        Map<String, SettingsDescription> s = new LinkedHashMap<String, SettingsDescription>();

        s.put("signature", new SignatureSetting());
        s.put("signatureUse", new BooleanSetting(true));
        s.put("replyTo", new OptionalEmailAddressSetting());

        SETTINGS = Collections.unmodifiableMap(s);
    }

    public static Map<String, String> validate(Map<String, String> importedSettings,
            boolean useDefaultValues) {
        return Settings.validate(SETTINGS, importedSettings, useDefaultValues);
    }

    public static Map<String, String> getIdentitySettings(SharedPreferences storage, String uuid,
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
        public String toPrettyString(Object value) {
            return (value == null) ? "" : value.toString();
        }

        @Override
        public Object fromPrettyString(String value) throws InvalidSettingValueException {
            return ("".equals(value)) ? null : fromString(value);
        }
    }
}
