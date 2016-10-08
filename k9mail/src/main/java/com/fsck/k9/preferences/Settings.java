package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.util.Log;

import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;

/*
 * TODO:
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
    public static final int VERSION = 43;

    public static Map<String, Object> validate(int version, Map<String,
            TreeMap<Integer, SettingsDescription>> settings,
            Map<String, String> importedSettings, boolean useDefaultValues) {

        Map<String, Object> validatedSettings = new HashMap<String, Object>();
        for (Map.Entry<String, TreeMap<Integer, SettingsDescription>> versionedSetting :
                settings.entrySet()) {

            // Get the setting description with the highest version lower than or equal to the
            // supplied content version.
            TreeMap<Integer, SettingsDescription> versions = versionedSetting.getValue();
            SortedMap<Integer, SettingsDescription> headMap = versions.headMap(version + 1);

            // Skip this setting if it was introduced after 'version'
            if (headMap.isEmpty()) {
                continue;
            }

            Integer settingVersion = headMap.lastKey();
            SettingsDescription desc = versions.get(settingVersion);

            // Skip this setting if it is no longer used in 'version'
            if (desc == null) {
                continue;
            }

            String key = versionedSetting.getKey();

            boolean useDefaultValue;
            if (!importedSettings.containsKey(key)) {
                Log.v(K9.LOG_TAG, "Key \"" + key + "\" wasn't found in the imported file." +
                        ((useDefaultValues) ? " Using default value." : ""));
                useDefaultValue = useDefaultValues;
            } else {
                String prettyValue = importedSettings.get(key);
                try {
                    Object internalValue = desc.fromPrettyString(prettyValue);
                    validatedSettings.put(key, internalValue);
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
                validatedSettings.put(key, defaultValue);
            }
        }

        return validatedSettings;
    }

    /**
     * Upgrade settings using the settings structure and/or special upgrade code.
     *
     * @param version
     *         The content version of the settings in {@code validatedSettings}.
     * @param upgraders
     *         A map of {@link SettingsUpgrader}s for nontrivial settings upgrades.
     * @param settings
     *         The structure describing the different settings, possibly containing multiple
     *         versions.
     * @param validatedSettings
     *         The settings as returned by {@link Settings#validate(int, Map, Map, boolean)}.
     *         This map is modified and contains the upgraded settings when this method returns.
     *
     * @return A set of setting names that were removed during the upgrade process or {@code null}
     *         if none were removed.
     */
    public static Set<String> upgrade(int version, Map<Integer, SettingsUpgrader> upgraders,
            Map<String, TreeMap<Integer, SettingsDescription>> settings,
            Map<String, Object> validatedSettings) {

        Map<String, Object> upgradedSettings = validatedSettings;
        Set<String> deletedSettings = null;

        for (int toVersion = version + 1; toVersion <= VERSION; toVersion++) {

            // Check if there's an SettingsUpgrader for that version
            SettingsUpgrader upgrader = upgraders.get(toVersion);
            if (upgrader != null) {
                deletedSettings = upgrader.upgrade(upgradedSettings);
            }

            // Deal with settings that don't need special upgrade code
            for (Entry<String, TreeMap<Integer, SettingsDescription>> versions :
                settings.entrySet()) {

                String settingName = versions.getKey();
                TreeMap<Integer, SettingsDescription> versionedSettings = versions.getValue();

                // Handle newly added settings
                if (versionedSettings.firstKey().intValue() == toVersion) {

                    // Check if it was already added to upgradedSettings by the SettingsUpgrader
                    if (!upgradedSettings.containsKey(settingName)) {
                        // Insert default value to upgradedSettings
                        SettingsDescription setting = versionedSettings.get(toVersion);
                        Object defaultValue = setting.getDefaultValue();
                        upgradedSettings.put(settingName, defaultValue);

                        if (K9.DEBUG) {
                            String prettyValue = setting.toPrettyString(defaultValue);
                            Log.v(K9.LOG_TAG, "Added new setting \"" + settingName +
                                    "\" with default value \"" + prettyValue + "\"");
                        }
                    }
                }

                // Handle removed settings
                Integer highestVersion = versionedSettings.lastKey();
                if (highestVersion.intValue() == toVersion &&
                        versionedSettings.get(highestVersion) == null) {
                    upgradedSettings.remove(settingName);
                    if (deletedSettings == null) {
                        deletedSettings = new HashSet<String>();
                    }
                    deletedSettings.add(settingName);

                    if (K9.DEBUG) {
                        Log.v(K9.LOG_TAG, "Removed setting \"" + settingName + "\"");
                    }
                }
            }
        }

        return deletedSettings;
    }

    /**
     * Convert settings from the internal representation to the string representation used in the
     * preference storage.
     *
     * @param settings
     *         The map of settings to convert.
     * @param settingDescriptions
     *         The structure containing the {@link SettingsDescription} objects that will be used
     *         to convert the setting values.
     *
     * @return The settings converted to the string representation used in the preference storage.
     */
    public static Map<String, String> convert(Map<String, Object> settings,
            Map<String, TreeMap<Integer, SettingsDescription>> settingDescriptions) {

        Map<String, String> serializedSettings = new HashMap<String, String>();

        for (Entry<String, Object> setting : settings.entrySet()) {
            String settingName = setting.getKey();
            Object internalValue = setting.getValue();

            TreeMap<Integer, SettingsDescription> versionedSetting =
                settingDescriptions.get(settingName);
            Integer highestVersion = versionedSetting.lastKey();
            SettingsDescription settingDesc = versionedSetting.get(highestVersion);

            if (settingDesc != null) {
                String stringValue = settingDesc.toString(internalValue);

                serializedSettings.put(settingName, stringValue);
            } else {
                if (K9.DEBUG) {
                    Log.w(K9.LOG_TAG, "Settings.serialize() called with a setting that should " +
                            "have been removed: " + settingName);
                }
            }
        }

        return serializedSettings;
    }

    /**
     * Creates a {@link TreeMap} linking version numbers to {@link SettingsDescription} instances.
     *
     * <p>
     * This {@code TreeMap} is used to quickly find the {@code SettingsDescription} belonging to a
     * content version as read by {@link SettingsImporter}. See e.g.
     * {@link Settings#validate(int, Map, Map, boolean)}.
     * </p>
     *
     * @param versionDescriptions
     *         A list of descriptions for a specific setting mapped to version numbers. Never
     *         {@code null}.
     *
     * @return A {@code TreeMap} using the version number as key, the {@code SettingsDescription}
     *         as value.
     */
    public static TreeMap<Integer, SettingsDescription> versions(
            V... versionDescriptions) {
        TreeMap<Integer, SettingsDescription> map = new TreeMap<Integer, SettingsDescription>();
        for (V v : versionDescriptions) {
            map.put(v.version, v.description);
        }
        return map;
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
     * Container to hold a {@link SettingsDescription} instance and a version number.
     *
     * @see Settings#versions(V...)
     */
    public static class V {
        public final Integer version;
        public final SettingsDescription description;

        public V(Integer version, SettingsDescription description) {
            this.version = version;
            this.description = description;
        }
    }

    /**
     * Used for a nontrivial settings upgrade.
     *
     * @see Settings#upgrade(int, Map, Map, Map)
     */
    public interface SettingsUpgrader {
        /**
         * Upgrade the provided settings.
         *
         * @param settings
         *         The settings to upgrade.  This map is modified and contains the upgraded
         *         settings when this method returns.
         *
         * @return A set of setting names that were removed during the upgrade process or
         *         {@code null} if none were removed.
         */
        public Set<String> upgrade(Map<String, Object> settings);
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
    public static class EnumSetting<T extends Enum<T>> extends SettingsDescription {
        private Class<T> mEnumClass;

        public EnumSetting(Class<T> enumClass, Object defaultValue) {
            super(defaultValue);
            mEnumClass = enumClass;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                return Enum.valueOf(mEnumClass, value);
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
            mapping.put(FontSizes.FONT_10SP, "tiniest");
            mapping.put(FontSizes.FONT_12SP, "tiny");
            mapping.put(FontSizes.SMALL, "smaller");
            mapping.put(FontSizes.FONT_16SP, "small");
            mapping.put(FontSizes.MEDIUM, "medium");
            mapping.put(FontSizes.FONT_20SP, "large");
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
