package com.fsck.k9.preferences;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import com.fsck.k9.K9;

/*
 * TODO:
 * - add support for different settings versions (validate old version and upgrade to new format)
 * - use the default values defined in GlobalSettings and AccountSettings when creating new
 *   accounts
 * - use the settings description to decide which keys to export
 * - convert internal representation to a "pretty" format when exporting (e.g. we want to export
 *   the value "light" rather than the integer constant for android.R.style.Theme_Light); revert
 *   that conversion when importing
 * - think of a better way to validate enums than to use the resource arrays (i.e. get rid of
 *   ResourceArrayValidator); maybe even use the settings description for the settings UI
 * - add unit test that validates the default values are actually valid according to the validator
 */

public class Settings {
    public static final IDefaultValue EXCEPTION_DEFAULT_VALUE = new ExceptionDefaultValue();

    public static final ISettingValidator BOOLEAN_VALIDATOR = new BooleanValidator();
    public static final ISettingValidator INTEGER_VALIDATOR = new IntegerValidator();
    public static final ISettingValidator POSITIVE_INTEGER_VALIDATOR = new PositiveIntegerValidator();
    public static final ISettingValidator SOLID_COLOR_VALIDATOR = new SolidColorValidator();

    public static Map<String, String> validate(Map<String, SettingsDescription> settings,
            Map<String, String> importedSettings) {

        Map<String, String> validatedSettings = new HashMap<String, String>();
        for (Map.Entry<String, SettingsDescription> setting : settings.entrySet()) {
            String key = setting.getKey();
            SettingsDescription desc = setting.getValue();

            boolean useDefaultValue;
            if (!importedSettings.containsKey(key)) {
                Log.v(K9.LOG_TAG, "Key \"" + key + "\" wasn't found in the imported file. Using default value.");
                useDefaultValue = true;
            } else {
                String importedValue = importedSettings.get(key);
                if (Settings.isValid(desc, key, importedValue, validatedSettings)) {
                    validatedSettings.put(key, importedValue);
                    useDefaultValue = false;
                } else {
                    Log.v(K9.LOG_TAG, "Key \"" + key + "\" has invalid value \"" + importedValue + "\" in " +
                            "imported file. Using default value.");
                    useDefaultValue = true;
                }
            }

            if (useDefaultValue) {
                Object defaultValue;
                if (desc.defaultValue instanceof IDefaultValue) {
                    defaultValue = ((IDefaultValue)desc.defaultValue).computeDefaultValue(key, validatedSettings);
                } else {
                    defaultValue = desc.defaultValue;
                }

                validatedSettings.put(key, defaultValue.toString());
            }
        }

        return validatedSettings;
    }

    public static boolean isValid(SettingsDescription desc, String key, String value,
            Map<String, String> validatedSettings) {
        try {
            switch (desc.type) {
                case BOOLEAN:
                    if (!Settings.BOOLEAN_VALIDATOR.isValid(key, value, validatedSettings)) {
                        return false;
                    }
                    break;
                case INTEGER:
                    if (!Settings.INTEGER_VALIDATOR.isValid(key, value, validatedSettings)) {
                        return false;
                    }
                    break;
                default:
                    break;
            }

            if (desc.validator != null) {
                return desc.validator.isValid(key, value, validatedSettings);
            }

            return true;
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception while running validator for value \"" + value + "\"", e);
            return false;
        }
    }

    public enum SettingType {
        BOOLEAN,
        INTEGER,
        STRING,
        ENUM
    }

    public static class SettingsDescription {
        public final SettingType type;
        public final Object defaultValue;
        public final ISettingValidator validator;

        protected SettingsDescription(SettingType type,
                Object defaultValue, ISettingValidator validator) {
            this.type = type;
            this.defaultValue = defaultValue;
            this.validator = validator;
        }
    }

    public interface IDefaultValue {
        Object computeDefaultValue(String key, Map<String, String> validatedSettings);
    }

    public static class ExceptionDefaultValue implements IDefaultValue {
        @Override
        public Object computeDefaultValue(String key, Map<String, String> validatedSettings) {
            throw new RuntimeException("There is no default value for key \"" + key + "\".");
        }

    }

    public interface ISettingValidator {
        boolean isValid(String key, String value, Map<String, String> validatedSettings);
    }

    public static class BooleanValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            return Boolean.TRUE.toString().equals(value) || Boolean.FALSE.toString().equals(value);
        }
    }

    public static class IntegerValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    public static class PositiveIntegerValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            return (Integer.parseInt(value) >= 0);
        }
    }

    public static class ResourceArrayValidator implements ISettingValidator {
        private final int mResource;

        public ResourceArrayValidator(int res) {
            mResource = res;
        }

        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            try {
                String[] values = K9.app.getResources().getStringArray(mResource);

                for (String validValue : values) {
                    if (validValue.equals(value)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Something went wrong during validation of key " + key, e);
            }

            return false;
        }
    }

    public static class SolidColorValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            int color = Integer.parseInt(value);
            return ((color & 0xFF000000) == 0xFF000000);
        }

    }
}
