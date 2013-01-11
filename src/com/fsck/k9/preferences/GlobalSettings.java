package com.fsck.k9.preferences;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.content.SharedPreferences;
import android.os.Environment;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.R;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.helper.DateFormatter;
import com.fsck.k9.preferences.Settings.*;

public class GlobalSettings {
    public static final Map<String, TreeMap<Integer, SettingsDescription>> SETTINGS;
    public static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription>> s =
            new LinkedHashMap<String, TreeMap<Integer, SettingsDescription>>();

        /**
         * When adding new settings here, be sure to increment {@link Settings.VERSION}
         * and use that for whatever you add here.
         */

        s.put("animations", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("attachmentdefaultpath", Settings.versions(
                new V(1, new DirectorySetting(Environment.getExternalStorageDirectory().toString()))
            ));
        s.put("backgroundOperations", Settings.versions(
                new V(1, new EnumSetting(K9.BACKGROUND_OPS.class, K9.BACKGROUND_OPS.WHEN_CHECKED))
            ));
        s.put("changeRegisteredNameColor", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("confirmDelete", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("confirmDeleteStarred", Settings.versions(
                new V(2, new BooleanSetting(false))
            ));
        s.put("confirmSpam", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("countSearchMessages", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("dateFormat", Settings.versions(
                new V(1, new DateFormatSetting(DateFormatter.DEFAULT_FORMAT))
            ));
        s.put("enableDebugLogging", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("enableSensitiveLogging", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("fontSizeAccountDescription", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.SMALL))
            ));
        s.put("fontSizeAccountName", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.MEDIUM))
            ));
        s.put("fontSizeFolderName", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.LARGE))
            ));
        s.put("fontSizeFolderStatus", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.SMALL))
            ));
        s.put("fontSizeMessageComposeInput", Settings.versions(
                new V(5, new FontSizeSetting(FontSizes.MEDIUM))
            ));
        s.put("fontSizeMessageListDate", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.SMALL))
            ));
        s.put("fontSizeMessageListPreview", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.SMALL))
            ));
        s.put("fontSizeMessageListSender", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.SMALL))
            ));
        s.put("fontSizeMessageListSubject", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_16SP))
            ));
        s.put("fontSizeMessageViewAdditionalHeaders", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_12SP))
            ));
        s.put("fontSizeMessageViewCC", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_12SP))
            ));
        s.put("fontSizeMessageViewContent", Settings.versions(
                new V(1, new WebFontSizeSetting(3))
            ));
        s.put("fontSizeMessageViewDate", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_10SP))
            ));
        s.put("fontSizeMessageViewSender", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.SMALL))
            ));
        s.put("fontSizeMessageViewSubject", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_12SP))
            ));
        s.put("fontSizeMessageViewTime", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_10SP))
            ));
        s.put("fontSizeMessageViewTo", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_12SP))
            ));
        s.put("gesturesEnabled", Settings.versions(
                new V(1, new BooleanSetting(true)),
                new V(4, new BooleanSetting(false))
            ));
        s.put("hideSpecialAccounts", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("keyguardPrivacy", Settings.versions(
                new V(1, new BooleanSetting(false)),
                new V(12, null)
            ));
        s.put("language", Settings.versions(
                new V(1, new LanguageSetting())
            ));
        s.put("measureAccounts", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("messageListCheckboxes", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("messageListPreviewLines", Settings.versions(
                new V(1, new IntegerRangeSetting(1, 100, 2))
            ));
        s.put("messageViewFixedWidthFont", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("messageViewReturnToList", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("messageViewShowNext", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("mobileOptimizedLayout", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("quietTimeEnabled", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("quietTimeEnds", Settings.versions(
                new V(1, new TimeSetting("7:00"))
            ));
        s.put("quietTimeStarts", Settings.versions(
                new V(1, new TimeSetting("21:00"))
            ));
        s.put("registeredNameColor", Settings.versions(
                new V(1, new ColorSetting(0xFF00008F))
            ));
        s.put("showContactName", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("showCorrespondentNames", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("sortTypeEnum", Settings.versions(
                new V(10, new EnumSetting(SortType.class, Account.DEFAULT_SORT_TYPE))
            ));
        s.put("sortAscending", Settings.versions(
                new V(10, new BooleanSetting(Account.DEFAULT_SORT_ASCENDING))
            ));
        s.put("startIntegratedInbox", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("theme", Settings.versions(
                new V(1, new ThemeSetting(K9.THEME_LIGHT))
            ));
        s.put("messageViewTheme", Settings.versions(
                new V(16, new ThemeSetting(K9.THEME_LIGHT))
            ));
        s.put("useGalleryBugWorkaround", Settings.versions(
                new V(1, new GalleryBugWorkaroundSetting())
            ));
        s.put("useVolumeKeysForListNavigation", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("useVolumeKeysForNavigation", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("wrapFolderNames", Settings.versions(
                new V(22, new BooleanSetting(false))
            ));
        s.put("batchButtonsMarkRead", Settings.versions(
                new V(8, new BooleanSetting(true))
            ));
        s.put("batchButtonsDelete", Settings.versions(
                new V(8, new BooleanSetting(true))
            ));
        s.put("batchButtonsArchive", Settings.versions(
                new V(8, new BooleanSetting(false))
            ));
        s.put("batchButtonsMove", Settings.versions(
                new V(8, new BooleanSetting(false))
            ));
        s.put("batchButtonsFlag", Settings.versions(
                new V(8, new BooleanSetting(true))
            ));
        s.put("batchButtonsUnselect", Settings.versions(
                new V(8, new BooleanSetting(true))
            ));
        s.put("notificationHideSubject", Settings.versions(
                new V(12, new EnumSetting(NotificationHideSubject.class,
                        NotificationHideSubject.NEVER))
            ));
        s.put("useBackgroundAsUnreadIndicator", Settings.versions(
                new V(19, new BooleanSetting(true))
            ));
        s.put("threadedView", Settings.versions(
                new V(20, new BooleanSetting(true))
            ));

        SETTINGS = Collections.unmodifiableMap(s);

        Map<Integer, SettingsUpgrader> u = new HashMap<Integer, SettingsUpgrader>();
        u.put(12, new SettingsUpgraderV12());

        UPGRADERS = Collections.unmodifiableMap(u);
    }

    public static Map<String, Object> validate(int version, Map<String, String> importedSettings) {
        return Settings.validate(version, SETTINGS, importedSettings, false);
    }

    public static Set<String> upgrade(int version, Map<String, Object> validatedSettings) {
        return Settings.upgrade(version, UPGRADERS, SETTINGS, validatedSettings);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }

    public static Map<String, String> getGlobalSettings(SharedPreferences storage) {
        Map<String, String> result = new HashMap<String, String>();
        for (String key : SETTINGS.keySet()) {
            String value = storage.getString(key, null);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Upgrades the settings from version 11 to 12
     *
     * Map the 'keyguardPrivacy' value to the new NotificationHideSubject enum.
     */
    public static class SettingsUpgraderV12 implements SettingsUpgrader {

        @Override
        public Set<String> upgrade(Map<String, Object> settings) {
            Boolean keyguardPrivacy = (Boolean) settings.get("keyguardPrivacy");
            if (keyguardPrivacy != null && keyguardPrivacy.booleanValue()) {
                // current setting: only show subject when unlocked
                settings.put("notificationHideSubject", NotificationHideSubject.WHEN_LOCKED);
            } else {
                // always show subject [old default]
                settings.put("notificationHideSubject", NotificationHideSubject.NEVER);
            }
            return new HashSet<String>(Arrays.asList("keyguardPrivacy"));
        }
    }

    /**
     * The gallery bug work-around setting.
     *
     * <p>
     * The default value varies depending on whether you have a version of Gallery 3D installed
     * that contains the bug we work around.
     * </p>
     *
     * @see K9#isGalleryBuggy()
     */
    public static class GalleryBugWorkaroundSetting extends BooleanSetting {
        public GalleryBugWorkaroundSetting() {
            super(false);
        }

        @Override
        public Object getDefaultValue() {
            return K9.isGalleryBuggy();
        }
    }

    /**
     * The language setting.
     *
     * <p>
     * Valid values are read from {@code settings_language_values} in
     * {@code res/values/arrays.xml}.
     * </p>
     */
    public static class LanguageSetting extends PseudoEnumSetting<String> {
        private final Map<String, String> mMapping;

        public LanguageSetting() {
            super("");

            Map<String, String> mapping = new HashMap<String, String>();
            String[] values = K9.app.getResources().getStringArray(R.array.settings_language_values);
            for (String value : values) {
                if (value.length() == 0) {
                    mapping.put("", "default");
                } else {
                    mapping.put(value, value);
                }
            }
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<String, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            if (mMapping.containsKey(value)) {
                return value;
            }

            throw new InvalidSettingValueException();
        }
    }

    /**
     * The theme setting.
     */
    public static class ThemeSetting extends SettingsDescription {
        private static final String THEME_LIGHT = "light";
        private static final String THEME_DARK = "dark";

        public ThemeSetting(int defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                Integer theme = Integer.parseInt(value);
                if (theme == K9.THEME_LIGHT ||
                        // We used to store the resource ID of the theme in the preference storage,
                        // but don't use the database upgrade mechanism to update the values. So
                        // we have to deal with the old format here.
                        theme == android.R.style.Theme_Light) {
                    return K9.THEME_LIGHT;
                } else if (theme == K9.THEME_DARK || theme == android.R.style.Theme) {
                    return K9.THEME_DARK;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }

        @Override
        public Object fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_LIGHT.equals(value)) {
                return K9.THEME_LIGHT;
            } else if (THEME_DARK.equals(value)) {
                return K9.THEME_DARK;
            }

            throw new InvalidSettingValueException();
        }

        @Override
        public String toPrettyString(Object value) {
            return (((Integer)value).intValue() == K9.THEME_LIGHT) ? THEME_LIGHT : THEME_DARK;
        }
    }

    /**
     * A date format setting.
     */
    public static class DateFormatSetting extends SettingsDescription {
        public DateFormatSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                // The placeholders "SHORT" and "MEDIUM" are fine.
                if (DateFormatter.SHORT_FORMAT.equals(value) ||
                        DateFormatter.MEDIUM_FORMAT.equals(value)) {
                    return value;
                }

                // If the SimpleDateFormat constructor doesn't throw an exception, we're good.
                new SimpleDateFormat(value);
                return value;
            } catch (Exception e) {
                throw new InvalidSettingValueException();
            }
        }
    }

    /**
     * A time setting.
     */
    public static class TimeSetting extends SettingsDescription {
        public TimeSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            if (!value.matches(TimePickerPreference.VALIDATION_EXPRESSION)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }
    }

    /**
     * A directory on the file system.
     */
    public static class DirectorySetting extends SettingsDescription {
        public DirectorySetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                if (new File(value).isDirectory()) {
                    return value;
                }
            } catch (Exception e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }
}
