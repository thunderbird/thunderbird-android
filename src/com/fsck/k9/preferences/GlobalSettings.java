package com.fsck.k9.preferences;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.content.SharedPreferences;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.DateFormatter;
import com.fsck.k9.preferences.Settings.*;

public class GlobalSettings {
    public static final Map<String, SettingsDescription> SETTINGS;

    static {
        SETTINGS = new LinkedHashMap<String, SettingsDescription>();

        SETTINGS.put("animations", new BooleanSetting(false));
        SETTINGS.put("backgroundOperations",
                new EnumSetting(K9.BACKGROUND_OPS.class, K9.BACKGROUND_OPS.WHEN_CHECKED));
        SETTINGS.put("changeRegisteredNameColor", new BooleanSetting(false));
        SETTINGS.put("compactLayouts", new BooleanSetting(false));
        SETTINGS.put("confirmDelete", new BooleanSetting(false));
        SETTINGS.put("countSearchMessages", new BooleanSetting(false));
        SETTINGS.put("dateFormat", new DateFormatSetting(DateFormatter.DEFAULT_FORMAT));
        SETTINGS.put("enableDebugLogging", new BooleanSetting(false));
        SETTINGS.put("enableSensitiveLogging", new BooleanSetting(false));
        SETTINGS.put("fontSizeAccountDescription", new FontSizeSetting(FontSizes.SMALL));
        SETTINGS.put("fontSizeAccountName", new FontSizeSetting(FontSizes.MEDIUM));
        SETTINGS.put("fontSizeFolderName", new FontSizeSetting(FontSizes.LARGE));
        SETTINGS.put("fontSizeFolderStatus", new FontSizeSetting(FontSizes.SMALL));
        SETTINGS.put("fontSizeMessageListDate", new FontSizeSetting(FontSizes.SMALL));
        SETTINGS.put("fontSizeMessageListPreview", new FontSizeSetting(FontSizes.SMALL));
        SETTINGS.put("fontSizeMessageListSender", new FontSizeSetting(FontSizes.SMALL));
        SETTINGS.put("fontSizeMessageListSubject", new FontSizeSetting(FontSizes.FONT_16DIP));
        SETTINGS.put("fontSizeMessageViewAdditionalHeaders",
                new FontSizeSetting(FontSizes.FONT_12DIP));
        SETTINGS.put("fontSizeMessageViewCC", new FontSizeSetting(FontSizes.FONT_12DIP));
        SETTINGS.put("fontSizeMessageViewContent", new WebFontSizeSetting(3));
        SETTINGS.put("fontSizeMessageViewDate", new FontSizeSetting(FontSizes.FONT_10DIP));
        SETTINGS.put("fontSizeMessageViewSender", new FontSizeSetting(FontSizes.SMALL));
        SETTINGS.put("fontSizeMessageViewSubject", new FontSizeSetting(FontSizes.FONT_12DIP));
        SETTINGS.put("fontSizeMessageViewTime", new FontSizeSetting(FontSizes.FONT_10DIP));
        SETTINGS.put("fontSizeMessageViewTo", new FontSizeSetting(FontSizes.FONT_12DIP));
        SETTINGS.put("gesturesEnabled", new BooleanSetting(true));
        SETTINGS.put("hideSpecialAccounts", new BooleanSetting(false));
        SETTINGS.put("keyguardPrivacy", new BooleanSetting(false));
        SETTINGS.put("language", new LanguageSetting());
        SETTINGS.put("manageBack", new BooleanSetting(false));
        SETTINGS.put("measureAccounts", new BooleanSetting(true));
        SETTINGS.put("messageListCheckboxes", new BooleanSetting(false));
        SETTINGS.put("messageListPreviewLines", new IntegerRangeSetting(1, 100, 2));
        SETTINGS.put("messageListStars", new BooleanSetting(true));
        SETTINGS.put("messageListTouchable", new BooleanSetting(false));
        SETTINGS.put("messageViewFixedWidthFont", new BooleanSetting(false));
        SETTINGS.put("messageViewReturnToList", new BooleanSetting(false));
        SETTINGS.put("mobileOptimizedLayout", new BooleanSetting(false));
        SETTINGS.put("quietTimeEnabled", new BooleanSetting(false));
        SETTINGS.put("quietTimeEnds", new TimeSetting("7:00"));
        SETTINGS.put("quietTimeStarts", new TimeSetting("21:00"));
        SETTINGS.put("registeredNameColor", new ColorSetting(0xFF00008F));
        SETTINGS.put("showContactName", new BooleanSetting(false));
        SETTINGS.put("showCorrespondentNames", new BooleanSetting(true));
        SETTINGS.put("startIntegratedInbox", new BooleanSetting(false));
        SETTINGS.put("theme", new ThemeSetting(android.R.style.Theme_Light));
        SETTINGS.put("useGalleryBugWorkaround", new GalleryBugWorkaroundSetting());
        SETTINGS.put("useVolumeKeysForListNavigation", new BooleanSetting(false));
        SETTINGS.put("useVolumeKeysForNavigation", new BooleanSetting(false));
        SETTINGS.put("zoomControlsEnabled", new BooleanSetting(false));
    }

    public static Map<String, String> validate(Map<String, String> importedSettings) {
        return Settings.validate(SETTINGS, importedSettings, false);
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
    public static class ThemeSetting extends PseudoEnumSetting<Integer> {
        private final Map<Integer, String> mMapping;

        public ThemeSetting(int defaultValue) {
            super(defaultValue);

            Map<Integer, String> mapping = new HashMap<Integer, String>();
            mapping.put(android.R.style.Theme_Light, "light");
            mapping.put(android.R.style.Theme, "dark");
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                Integer theme = Integer.parseInt(value);
                if (mMapping.containsKey(theme)) {
                    return theme;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
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
}
