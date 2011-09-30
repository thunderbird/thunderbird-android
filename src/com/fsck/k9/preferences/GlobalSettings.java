package com.fsck.k9.preferences;

import java.text.SimpleDateFormat;
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
    public static final ISettingValidator FONT_SIZE_VALIDATOR = new DipFontSizeValidator();
    public static final ISettingValidator TIME_VALIDATOR = new TimeValidator();

    public static final Map<String, SettingsDescription> SETTINGS;

    static {
        SETTINGS = new LinkedHashMap<String, SettingsDescription>();

        SETTINGS.put("animations",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("backgroundOperations",
                SD(SettingType.ENUM, K9.BACKGROUND_OPS.WHEN_CHECKED, new ResourceArrayValidator(
                        R.array.background_ops_values)));
        SETTINGS.put("changeRegisteredNameColor",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("compactLayouts",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("confirmDelete",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("countSearchMessages",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("dateFormat",
                SD(SettingType.ENUM, DateFormatter.DEFAULT_FORMAT, new DateFormatValidator()));
        SETTINGS.put("enableDebugLogging",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("enableSensitiveLogging",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("fontSizeAccountDescription",
                SD(SettingType.INTEGER, 14, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeAccountName",
                SD(SettingType.INTEGER, 18, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeFolderName",
                SD(SettingType.INTEGER, 22, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeFolderStatus",
                SD(SettingType.INTEGER, 14, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageListDate",
                SD(SettingType.INTEGER, 14, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageListPreview",
                SD(SettingType.INTEGER, 14, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageListSender",
                SD(SettingType.INTEGER, 14, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageListSubject",
                SD(SettingType.INTEGER, 16, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewAdditionalHeaders",
                SD(SettingType.INTEGER, 12, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewCC",
                SD(SettingType.INTEGER, 12, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewContent",
                SD(SettingType.INTEGER, 3, new WebViewFontSizeValidator()));
        SETTINGS.put("fontSizeMessageViewDate",
                SD(SettingType.INTEGER, 10, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewSender",
                SD(SettingType.INTEGER, 14, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewSubject",
                SD(SettingType.INTEGER, 12, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewTime",
                SD(SettingType.INTEGER, 10, FONT_SIZE_VALIDATOR));
        SETTINGS.put("fontSizeMessageViewTo",
                SD(SettingType.INTEGER, 12, FONT_SIZE_VALIDATOR));
        SETTINGS.put("gesturesEnabled",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("hideSpecialAccounts",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("keyguardPrivacy",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("language",
                SD(SettingType.STRING, "", new ResourceArrayValidator(
                        R.array.settings_language_values)));
        SETTINGS.put("manageBack",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("measureAccounts",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("messageListCheckboxes",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("messageListPreviewLines",
                SD(SettingType.INTEGER, 2, Settings.POSITIVE_INTEGER_VALIDATOR));
        SETTINGS.put("messageListStars",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("messageListTouchable",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("messageViewFixedWidthFont",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("messageViewReturnToList",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("mobileOptimizedLayout",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("quietTimeEnabled",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("quietTimeEnds",
                SD(SettingType.STRING, "7:00", TIME_VALIDATOR));
        SETTINGS.put("quietTimeStarts",
                SD(SettingType.STRING, "21:00", TIME_VALIDATOR));
        SETTINGS.put("registeredNameColor",
                SD(SettingType.INTEGER, 0xFF00008F, Settings.SOLID_COLOR_VALIDATOR));
        SETTINGS.put("showContactName",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("showCorrespondentNames",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("startIntegratedInbox",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("theme",
                SD(SettingType.INTEGER, android.R.style.Theme_Light, new ThemeValidator()));
        SETTINGS.put("useGalleryBugWorkaround",
                SD(SettingType.BOOLEAN, new GalleryBugWorkaroundDefaultValue(), null));
        SETTINGS.put("useVolumeKeysForListNavigation",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("useVolumeKeysForNavigation",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("zoomControlsEnabled",
                SD(SettingType.BOOLEAN, false, null));
    }

    // Just to have shorter lines in SETTINGS initialization
    private static SettingsDescription SD(SettingType type,
            Object defaultValue, ISettingValidator validator) {
        return new SettingsDescription(type, defaultValue, validator);
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


    public static class GalleryBugWorkaroundDefaultValue implements IDefaultValue {
        @Override
        public Object computeDefaultValue(String key, Map<String, String> validatedSettings) {
            return K9.isGalleryBuggy();
        }
    }

    public static class DipFontSizeValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            int val = Integer.parseInt(value);
            switch (val) {
                case FontSizes.FONT_10DIP:
                case FontSizes.FONT_12DIP:
                case FontSizes.SMALL:
                case FontSizes.FONT_16DIP:
                case FontSizes.MEDIUM:
                case FontSizes.FONT_20DIP:
                case FontSizes.LARGE:
                    return true;
                default:
                    return false;
            }
        }
    }

    public static class WebViewFontSizeValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            int val = Integer.parseInt(value);
            return (val >= 1 && val <= 5);
        }
    }

    public static class TimeValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            return value.matches(TimePickerPreference.VALIDATION_EXPRESSION);
        }
    }

    public static class DateFormatValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            try {
                // The placeholders "SHORT" and "MEDIUM" are fine.
                if (DateFormatter.SHORT_FORMAT.equals(value) ||
                        DateFormatter.MEDIUM_FORMAT.equals(value)) {
                    return true;
                }

                // If the SimpleDateFormat constructor doesn't throw an exception, we're good.
                new SimpleDateFormat(value);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class ThemeValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            int val = Integer.parseInt(value);
            return (val == android.R.style.Theme_Light || val == android.R.style.Theme);
        }
    }
}
