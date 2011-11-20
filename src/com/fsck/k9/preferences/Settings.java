package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;

import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;

/*
 * TODO:
 * - add support for different settings versions (validate old version and upgrade to new format)
 * - use the default values defined in GlobalSettings and AccountSettings when creating new
 *   accounts
 * - think of a better way to validate enums than to use the resource arrays (i.e. get rid of
 *   ResourceArrayValidator); maybe even use the settings description for the settings UI
 * - add unit test that validates the default values are actually valid according to the validator
 */

public class Settings {
    /**
     * Version number of global and account settings.
     *
     * <p>
     * This value is used as "version" attribute in the export file. It needs to be incremented
     * when a global or account setting is added or removed, or when the format of a setting
     * is changed (e.g. add a value to an enum).
     * </p>
     *
     * @see SettingsExporter
     */
    public static final int VERSION = 3;

    public static Map<String, String> validate(Map<String, SettingsDescription> settings,
            Map<String, String> importedSettings, boolean useDefaultValues) {

        Map<String, String> validatedSettings = new HashMap<String, String>();
        for (Map.Entry<String, SettingsDescription> setting : settings.entrySet()) {
            String key = setting.getKey();
            SettingsDescription desc = setting.getValue();

            boolean useDefaultValue;
            if (!importedSettings.containsKey(key)) {
                Log.v(K9.LOG_TAG, "Key \"" + key + "\" wasn't found in the imported file." +
                        ((useDefaultValues) ? " Using default value." : ""));
                useDefaultValue = useDefaultValues;
            } else {
                String prettyValue = importedSettings.get(key);
                try {
                    Object internalValue = desc.fromPrettyString(prettyValue);
                    String importedValue = desc.toString(internalValue);
                    validatedSettings.put(key, importedValue);
                    useDefaultValue = false;
                } catch (InvalidSettingValueException e) {
                    Log.v(K9.LOG_TAG, "Key \"" + key + "\" has invalid value \"" + prettyValue +
                            "\" in imported file. " +
                            ((useDefaultValues) ? "Using default value." : "Skipping."));
                    useDefaultValue = useDefaultValues;
                }
            }

            if (useDefaultValue) {
                Object defaultValue = desc.getDefaultValue();
                String value = (defaultValue != null) ? desc.toString(defaultValue) : null;
                validatedSettings.put(key, value);
            }
        }

        return validatedSettings;
    }


    /**
     * Indicates an invalid setting value.
     *
     * @see SettingsDescription#fromString(String)
     * @see SettingsDescription#fromPrettyString(String)
     */
    public static class InvalidSettingValueException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Describes a setting.
     *
     * <p>
     * Instances of this class are used to convert the string representations of setting values to
     * an internal representation (e.g. an integer) and back.
     * </p><p>
     * Currently we use two different string representations:
     * </p>
     * <ol>
     *   <li>
     *   The one that is used by the internal preference {@link Storage}. It is usually obtained by
     *   calling {@code toString()} on the internal representation of the setting value (see e.g.
     *   {@link K9#save(android.content.SharedPreferences.Editor)}).
     *   </li>
     *   <li>
     *   The "pretty" version that is used by the import/export settings file (e.g. colors are
     *   exported in #rrggbb format instead of a integer string like "-8734021").
     *   </li>
     * </ol>
     * <p>
     * <strong>Note:</strong>
     * For the future we should aim to get rid of the "internal" string representation. The
     * "pretty" version makes reading a database dump easier and the performance impact should be
     * negligible.
     * </p>
     */
    public static abstract class SettingsDescription {
        /**
         * The setting's default value (internal representation).
         */
        protected Object mDefaultValue;

        public SettingsDescription(Object defaultValue) {
            mDefaultValue = defaultValue;
        }

        /**
         * Get the default value.
         *
         * @return The internal representation of the default value.
         */
        public Object getDefaultValue() {
            return mDefaultValue;
        }

        /**
         * Convert a setting's value to the string representation.
         *
         * @param value
         *         The internal representation of a setting's value.
         *
         * @return The string representation of {@code value}.
         */
        public String toString(Object value) {
            return value.toString();
        }

        /**
         * Parse the string representation of a setting's value .
         *
         * @param value
         *         The string representation of a setting's value.
         *
         * @return The internal representation of the setting's value.
         *
         * @throws InvalidSettingValueException
         *         If {@code value} contains an invalid value.
         */
        public abstract Object fromString(String value) throws InvalidSettingValueException;

        /**
         * Convert a setting value to the "pretty" string representation.
         *
         * @param value
         *         The setting's value.
         *
         * @return A pretty-printed version of the setting's value.
         */
        public String toPrettyString(Object value) {
            return toString(value);
        }

        /**
         * Convert the pretty-printed version of a setting's value to the internal representation.
         *
         * @param value
         *         The pretty-printed version of the setting's value. See
         *         {@link #toPrettyString(Object)}.
         *
         * @return The internal representation of the setting's value.
         *
         * @throws InvalidSettingValueException
         *         If {@code value} contains an invalid value.
         */
        public Object fromPrettyString(String value) throws InvalidSettingValueException {
            return fromString(value);
        }
    }

    /**
     * A string setting.
     */
    public static class StringSetting extends SettingsDescription {
        public StringSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) {
            return value;
        }
    }

    /**
     * A boolean setting.
     */
    public static class BooleanSetting extends SettingsDescription {
        public BooleanSetting(boolean defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            if (Boolean.TRUE.toString().equals(value)) {
                return true;
            } else if (Boolean.FALSE.toString().equals(value)) {
                return false;
            }
            throw new InvalidSettingValueException();
        }
    }

    /**
     * A color setting.
     */
    public static class ColorSetting extends SettingsDescription {
        public ColorSetting(int defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingValueException();
            }
        }

        @Override
        public String toPrettyString(Object value) {
            int color = ((Integer) value) & 0x00FFFFFF;
            return String.format("#%06x", color);
        }

        @Override
        public Object fromPrettyString(String value) throws InvalidSettingValueException {
            try {
                if (value.length() == 7) {
                    return Integer.parseInt(value.substring(1), 16) | 0xFF000000;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }

    /**
     * An {@code Enum} setting.
     *
     * <p>
     * {@link Enum#toString()} is used to obtain the "pretty" string representation.
     * </p>
     */
    public static class EnumSetting extends SettingsDescription {
        private Class<? extends Enum<?>> mEnumClass;

        public EnumSetting(Class<? extends Enum<?>> enumClass, Object defaultValue) {
            super(defaultValue);
            mEnumClass = enumClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                return Enum.valueOf((Class<? extends Enum>)mEnumClass, value);
            } catch (Exception e) {
                throw new InvalidSettingValueException();
            }
        }
    }

    /**
     * A setting that has multiple valid values but doesn't use an {@link Enum} internally.
     *
     * @param <A>
     *         The type of the internal representation (e.g. {@code Integer}).
     */
    public abstract static class PseudoEnumSetting<A> extends SettingsDescription {
        public PseudoEnumSetting(Object defaultValue) {
            super(defaultValue);
        }

        protected abstract Map<A, String> getMapping();

        @Override
        public String toPrettyString(Object value) {
            return getMapping().get(value);
        }

        @Override
        public Object fromPrettyString(String value) throws InvalidSettingValueException {
            for (Entry<A, String> entry : getMapping().entrySet()) {
                if (entry.getValue().equals(value)) {
                    return entry.getKey();
                }
            }

            throw new InvalidSettingValueException();
        }
    }

    /**
     * A font size setting.
     */
    public static class FontSizeSetting extends PseudoEnumSetting<Integer> {
        private final Map<Integer, String> mMapping;

        public FontSizeSetting(int defaultValue) {
            super(defaultValue);

            Map<Integer, String> mapping = new HashMap<Integer, String>();
            mapping.put(FontSizes.FONT_10DIP, "tiniest");
            mapping.put(FontSizes.FONT_12DIP, "tiny");
            mapping.put(FontSizes.SMALL, "smaller");
            mapping.put(FontSizes.FONT_16DIP, "small");
            mapping.put(FontSizes.MEDIUM, "medium");
            mapping.put(FontSizes.FONT_20DIP, "large");
            mapping.put(FontSizes.LARGE, "larger");
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                Integer fontSize = Integer.parseInt(value);
                if (mMapping.containsKey(fontSize)) {
                    return fontSize;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }

    /**
     * A {@link android.webkit.WebView} font size setting.
     */
    public static class WebFontSizeSetting extends PseudoEnumSetting<Integer> {
        private final Map<Integer, String> mMapping;

        public WebFontSizeSetting(int defaultValue) {
            super(defaultValue);

            Map<Integer, String> mapping = new HashMap<Integer, String>();
            mapping.put(1, "smallest");
            mapping.put(2, "smaller");
            mapping.put(3, "normal");
            mapping.put(4, "larger");
            mapping.put(5, "largest");
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                Integer fontSize = Integer.parseInt(value);
                if (mMapping.containsKey(fontSize)) {
                    return fontSize;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }

    /**
     * An integer settings whose values a limited to a certain range.
     */
    public static class IntegerRangeSetting extends SettingsDescription {
        private int mStart;
        private int mEnd;

        public IntegerRangeSetting(int start, int end, int defaultValue) {
            super(defaultValue);
            mStart = start;
            mEnd = end;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                int intValue = Integer.parseInt(value);
                if (mStart <= intValue && intValue <= mEnd) {
                    return intValue;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }
}
