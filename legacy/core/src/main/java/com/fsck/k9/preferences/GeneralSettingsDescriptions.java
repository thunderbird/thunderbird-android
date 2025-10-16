package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;

import app.k9mail.feature.telemetry.api.TelemetryManager;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.K9.PostMarkAsUnreadNavigation;
import com.fsck.k9.UiDensity;
import com.fsck.k9.core.R;
import com.fsck.k9.preferences.Settings.BooleanSetting;
import com.fsck.k9.preferences.Settings.ColorSetting;
import com.fsck.k9.preferences.Settings.EnumSetting;
import com.fsck.k9.preferences.Settings.FontSizeSetting;
import com.fsck.k9.preferences.Settings.IntegerRangeSetting;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import com.fsck.k9.preferences.Settings.PseudoEnumSetting;
import com.fsck.k9.preferences.Settings.SettingsDescription;
import com.fsck.k9.preferences.Settings.V;
import com.fsck.k9.preferences.Settings.WebFontSizeSetting;
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo24;
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo31;
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo58;
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo69;
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo79;
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo89;
import net.thunderbird.core.android.account.AccountDefaultsProvider;
import net.thunderbird.core.android.account.SortType;
import net.thunderbird.core.common.action.SwipeAction;
import net.thunderbird.core.preference.AppTheme;
import net.thunderbird.core.preference.BackgroundOps;
import net.thunderbird.core.preference.GeneralSettingsManager;
import net.thunderbird.core.preference.SplitViewMode;
import net.thunderbird.core.preference.SubTheme;
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsKt;
import net.thunderbird.core.preference.interaction.PostRemoveNavigation;
import net.thunderbird.core.preference.network.NetworkSettingsKt;
import net.thunderbird.core.preference.storage.Storage;

import static com.fsck.k9.K9.LockScreenNotificationVisibility;
import static net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT;
import static net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST;
import static net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_MESSAGE_LIST_STAR;
import static net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_STAR_COUNT;
import static net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_UNIFIED_INBOX;
import static net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettingsKt.DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG;
import static net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettingsKt.DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES;
import static net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsKt.DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT;
import static net.thunderbird.core.preference.notification.NotificationPreferenceKt.NOTIFICATION_PREFERENCE_DEFAULT_IS_QUIET_TIME_ENABLED;
import static net.thunderbird.core.preference.notification.NotificationPreferenceKt.NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_END;
import static net.thunderbird.core.preference.notification.NotificationPreferenceKt.NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_STARTS;
import static net.thunderbird.core.preference.privacy.PrivacySettingsKt.PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE;
import static net.thunderbird.core.preference.privacy.PrivacySettingsKt.PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT;


class GeneralSettingsDescriptions {
    static final Map<String, TreeMap<Integer, SettingsDescription<?>>> SETTINGS;
    private static final Map<Integer, SettingsUpgrader> UPGRADERS;

    private static final TelemetryManager telemetryManager = DI.get(TelemetryManager.class);
    private static final GeneralSettingsManager generalSettingManager = DI.get(GeneralSettingsManager.class);

    static {
        Map<String, TreeMap<Integer, SettingsDescription<?>>> s = new LinkedHashMap<>();

        /*
         * When adding new settings here, be sure to increment {@link Settings.VERSION}
         * and use that for whatever you add here.
         */

        s.put("animations", Settings.versions(
            new V(1, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION))
        ));
        s.put("backgroundOperations", Settings.versions(
            new V(1, new EnumSetting<>(BackgroundOps.class, BackgroundOps.WHEN_CHECKED_AUTO_SYNC)),
            new V(83, new EnumSetting<>(BackgroundOps.class, NetworkSettingsKt.getNETWORK_SETTINGS_DEFAULT_BACKGROUND_OPS()))
        ));
        s.put("changeRegisteredNameColor", Settings.versions(
            new V(1, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR))
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
        s.put("confirmMarkAllRead", Settings.versions(
            new V(44, new BooleanSetting(true))
        ));
        s.put("enableDebugLogging", Settings.versions(
            new V(1, new BooleanSetting(false))
        ));
        s.put("enableSyncDebugLogging", Settings.versions(
            new V(103, new BooleanSetting(false))
        ));
        s.put("enableSensitiveLogging", Settings.versions(
            new V(1, new BooleanSetting(false))
        ));
        s.put("fontSizeMessageComposeInput", Settings.versions(
            new V(5, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListDate", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListPreview", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListSender", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListSubject", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewAdditionalHeaders", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewCC", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewContent", Settings.versions(
            new V(1, new WebFontSizeSetting(3)),
            new V(31, null)
        ));
        s.put("fontSizeMessageViewDate", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewSender", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewSubject", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewTime", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewTo", Settings.versions(
            new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("hideSpecialAccounts", Settings.versions(
            new V(1, new BooleanSetting(false)),
            new V(69, null)
        ));
        s.put("language", Settings.versions(
            new V(1, new LanguageSetting())
        ));
        s.put("messageListPreviewLines", Settings.versions(
            new V(1, new IntegerRangeSetting(0, 6, 2))
        ));
        s.put("messageListStars", Settings.versions(
            new V(1, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_MESSAGE_LIST_STAR))
        ));
        s.put("messageViewFixedWidthFont", Settings.versions(
            new V(1, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT))
        ));
        s.put("messageViewReturnToList", Settings.versions(
            new V(1, new BooleanSetting(false)),
            new V(89, null)
        ));
        s.put("messageViewShowNext", Settings.versions(
            new V(1, new BooleanSetting(false)),
            new V(89, null)
        ));
        s.put("quietTimeEnabled", Settings.versions(
            new V(1, new BooleanSetting(NOTIFICATION_PREFERENCE_DEFAULT_IS_QUIET_TIME_ENABLED))
        ));
        s.put("quietTimeEnds", Settings.versions(
            new V(1, new TimeSetting(NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_END))
        ));
        s.put("quietTimeStarts", Settings.versions(
            new V(1, new TimeSetting(NOTIFICATION_PREFERENCE_DEFAULT_QUIET_TIME_STARTS))
        ));
        s.put("registeredNameColor", Settings.versions(
            new V(1, new ColorSetting(0xFF00008F)),
            new V(79, new ColorSetting(0xFF1093F5))
        ));
        s.put("showContactName", Settings.versions(
            new V(1, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME))
        ));
        s.put("showCorrespondentNames", Settings.versions(
            new V(1, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES))
        ));
        s.put("showUnifiedInbox", Settings.versions(
            new V(69, new BooleanSetting(true)),
            new V(101, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_UNIFIED_INBOX))
        ));
        s.put("isShowAccountSelector", Settings.versions(
            new V(102, new BooleanSetting(true))
        ));
        s.put("sortTypeEnum", Settings.versions(
            new V(10, new EnumSetting<>(SortType.class, AccountDefaultsProvider.getDEFAULT_SORT_TYPE()))
        ));
        s.put("sortAscending", Settings.versions(
            new V(10, new BooleanSetting(AccountDefaultsProvider.DEFAULT_SORT_ASCENDING))
        ));
        s.put("theme", Settings.versions(
            new V(1, new LegacyThemeSetting(AppTheme.LIGHT)),
            new V(58, new ThemeSetting(DisplayCoreSettingsKt.getDISPLAY_SETTINGS_DEFAULT_APP_THEME()))
        ));
        s.put("messageViewTheme", Settings.versions(
            new V(16, new LegacyThemeSetting(AppTheme.LIGHT)),
            new V(24, new SubThemeSetting(DisplayCoreSettingsKt.getDISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME()))
        ));
        s.put("useVolumeKeysForNavigation", Settings.versions(
            new V(1, new BooleanSetting(false))
        ));
        s.put("useBackgroundAsUnreadIndicator", Settings.versions(
            new V(19, new BooleanSetting(true)),
            new V(59, new BooleanSetting(false))
        ));
        s.put("threadedView", Settings.versions(
            new V(20, new BooleanSetting(true))
        ));
        s.put("splitViewMode", Settings.versions(
            new V(23, new EnumSetting<>(SplitViewMode.class, SplitViewMode.NEVER))
        ));
        s.put("messageComposeTheme", Settings.versions(
            new V(24, new SubThemeSetting(DisplayCoreSettingsKt.getDISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME()))
        ));
        s.put("fixedMessageViewTheme", Settings.versions(
            new V(24, new BooleanSetting(DisplayCoreSettingsKt.DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME))
        ));
        s.put("showContactPicture", Settings.versions(
            new V(25, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE))
        ));
        s.put("autofitWidth", Settings.versions(
            new V(28, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH))
        ));
        s.put("colorizeMissingContactPictures", Settings.versions(
            new V(29, new BooleanSetting(true))
        ));
        s.put("messageViewDeleteActionVisible", Settings.versions(
            new V(30, new BooleanSetting(true))
        ));
        s.put("messageViewArchiveActionVisible", Settings.versions(
            new V(30, new BooleanSetting(false))
        ));
        s.put("messageViewMoveActionVisible", Settings.versions(
            new V(30, new BooleanSetting(false))
        ));
        s.put("messageViewCopyActionVisible", Settings.versions(
            new V(30, new BooleanSetting(false))
        ));
        s.put("messageViewSpamActionVisible", Settings.versions(
            new V(30, new BooleanSetting(false))
        ));
        s.put("fontSizeMessageViewContentPercent", Settings.versions(
            new V(31, new IntegerRangeSetting(40, 250, 100))
        ));
        s.put("hideUserAgent", Settings.versions(
            new V(32, new BooleanSetting(PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT))
        ));
        s.put("hideTimeZone", Settings.versions(
            new V(32, new BooleanSetting(PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE))
        ));
        s.put("lockScreenNotificationVisibility", Settings.versions(
            new V(37, new EnumSetting<>(LockScreenNotificationVisibility.class,
                LockScreenNotificationVisibility.MESSAGE_COUNT))
        ));
        s.put("confirmDeleteFromNotification", Settings.versions(
            new V(38, new BooleanSetting(true))
        ));
        s.put("messageListSenderAboveSubject", Settings.versions(
            new V(38, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT))
        ));
        s.put("notificationQuickDelete", Settings.versions(
            new V(38, new EnumSetting<>(NotificationQuickDelete.class, NotificationQuickDelete.NEVER)),
            new V(67, new EnumSetting<>(NotificationQuickDelete.class, NotificationQuickDelete.ALWAYS))
        ));
        s.put("notificationDuringQuietTimeEnabled", Settings.versions(
            new V(39, new BooleanSetting(true))
        ));
        s.put("confirmDiscardMessage", Settings.versions(
            new V(40, new BooleanSetting(true))
        ));
        s.put("pgpInlineDialogCounter", Settings.versions(
            new V(43, new IntegerRangeSetting(0, Integer.MAX_VALUE, 0))
        ));
        s.put("pgpSignOnlyDialogCounter", Settings.versions(
            new V(45, new IntegerRangeSetting(0, Integer.MAX_VALUE, 0))
        ));
        s.put("fontSizeMessageViewBCC", Settings.versions(
            new V(48, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("hideHostnameWhenConnecting", Settings.versions(
            new V(49, new BooleanSetting(false)),
            new V(56, null)
        ));
        s.put("showRecentChanges", Settings.versions(
            new V(73, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES))
        ));
        s.put("showStarredCount", Settings.versions(
            new V(75, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_STAR_COUNT))
        ));
        s.put("swipeRightAction", Settings.versions(
            new V(83, new EnumSetting<>(SwipeAction.class, SwipeAction.ToggleSelection))
        ));
        s.put("swipeLeftAction", Settings.versions(
            new V(83, new EnumSetting<>(SwipeAction.class, SwipeAction.ToggleRead))
        ));
        s.put("showComposeButtonOnMessageList", Settings.versions(
            new V(85, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST))
        ));
        s.put("messageListDensity", Settings.versions(
            new V(86, new EnumSetting<>(UiDensity.class, UiDensity.Default))
        ));
        s.put("fontSizeMessageViewAccountName", Settings.versions(
            new V(87, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("messageViewPostDeleteAction", Settings.versions(
            new V(89, new EnumSetting<>(PostRemoveNavigation.class, PostRemoveNavigation.ReturnToMessageList))
        ));
        s.put("messageViewPostMarkAsUnreadAction", Settings.versions(
            new V(90,
                new EnumSetting<>(PostMarkAsUnreadNavigation.class, PostMarkAsUnreadNavigation.ReturnToMessageList))
        ));
        s.put("shouldShowSetupArchiveFolderDialog", Settings.versions(
            new V(105, new BooleanSetting(DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG)))
        );

        // TODO: Add a way to properly support feature-specific settings.
        if (telemetryManager.isTelemetryFeatureIncluded()) {
            s.put("enableTelemetry", Settings.versions(
                new V(97, new BooleanSetting(true))
            ));
        }

        SETTINGS = Collections.unmodifiableMap(s);

        Map<Integer, SettingsUpgrader> u = new HashMap<>();
        u.put(24, new GeneralSettingsUpgraderTo24());
        u.put(31, new GeneralSettingsUpgraderTo31());
        u.put(58, new GeneralSettingsUpgraderTo58());
        u.put(69, new GeneralSettingsUpgraderTo69());
        u.put(79, new GeneralSettingsUpgraderTo79());
        u.put(89, new GeneralSettingsUpgraderTo89());

        UPGRADERS = Collections.unmodifiableMap(u);
    }

    static Map<String, Object> validate(int version, Map<String, String> importedSettings) {
        return Settings.validate(version, SETTINGS, importedSettings, false);
    }

    public static Map<String, Object> upgrade(int version, Map<String, Object> validatedSettings) {
        return SettingsUpgradeHelper.upgrade(version, UPGRADERS, SETTINGS, validatedSettings, generalSettingManager);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }

    static Map<String, String> getGlobalSettings(Storage storage) {
        Map<String, String> result = new HashMap<>();
        for (String key : SETTINGS.keySet()) {
            String value = storage.getStringOrNull(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static class LanguageSetting extends PseudoEnumSetting<String> {
        private final Context context = DI.get(Context.class);
        private final Map<String, String> mapping;

        LanguageSetting() {
            super("");

            Map<String, String> mapping = new HashMap<>();
            String[] values = context.getResources().getStringArray(R.array.language_values);
            for (String value : values) {
                if (value.length() == 0) {
                    mapping.put("", "default");
                } else {
                    mapping.put(value, value);
                }
            }
            this.mapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<String, String> getMapping() {
            return mapping;
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            if (mapping.containsKey(value)) {
                return value;
            }

            throw new InvalidSettingValueException();
        }
    }

    static class LegacyThemeSetting extends SettingsDescription<AppTheme> {
        private static final String THEME_LIGHT = "light";
        private static final String THEME_DARK = "dark";

        LegacyThemeSetting(AppTheme defaultValue) {
            super(defaultValue);
        }

        @Override
        public AppTheme fromString(String value) throws InvalidSettingValueException {
            try {
                return AppTheme.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingValueException();
            }
        }

        @Override
        public AppTheme fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_LIGHT.equals(value)) {
                return AppTheme.LIGHT;
            } else if (THEME_DARK.equals(value)) {
                return AppTheme.DARK;
            }

            throw new InvalidSettingValueException();
        }

        @Override
        public String toPrettyString(AppTheme value) {
            switch (value) {
                case LIGHT:
                    return THEME_LIGHT;
                case DARK:
                    return THEME_DARK;
            }

            throw new AssertionError("Unexpected case: " + value);
        }

        @Override
        public String toString(AppTheme value) {
            return value.name();
        }
    }

    private static class ThemeSetting extends SettingsDescription<AppTheme> {
        private static final String THEME_LIGHT = "light";
        private static final String THEME_DARK = "dark";
        private static final String THEME_FOLLOW_SYSTEM = "follow_system";

        ThemeSetting(AppTheme defaultValue) {
            super(defaultValue);
        }

        @Override
        public AppTheme fromString(String value) throws InvalidSettingValueException {
            try {
                return AppTheme.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingValueException();
            }
        }

        @Override
        public AppTheme fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_LIGHT.equals(value)) {
                return AppTheme.LIGHT;
            } else if (THEME_DARK.equals(value)) {
                return AppTheme.DARK;
            } else if (THEME_FOLLOW_SYSTEM.equals(value)) {
                return AppTheme.FOLLOW_SYSTEM;
            }

            throw new InvalidSettingValueException();
        }

        @Override
        public String toPrettyString(AppTheme value) {
            switch (value) {
                case LIGHT:
                    return THEME_LIGHT;
                case DARK:
                    return THEME_DARK;
                case FOLLOW_SYSTEM:
                    return THEME_FOLLOW_SYSTEM;
            }

            throw new AssertionError("Unexpected case: " + value);
        }

        @Override
        public String toString(AppTheme value) {
            return value.name();
        }
    }

    private static class SubThemeSetting extends SettingsDescription<SubTheme> {
        private static final String THEME_LIGHT = "light";
        private static final String THEME_DARK = "dark";
        private static final String THEME_USE_GLOBAL = "use_global";

        SubThemeSetting(SubTheme defaultValue) {
            super(defaultValue);
        }

        @Override
        public SubTheme fromString(String value) throws InvalidSettingValueException {
            try {
                return SubTheme.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingValueException();
            }
        }

        @Override
        public SubTheme fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_LIGHT.equals(value)) {
                return SubTheme.LIGHT;
            } else if (THEME_DARK.equals(value)) {
                return SubTheme.DARK;
            } else if (THEME_USE_GLOBAL.equals(value)) {
                return SubTheme.USE_GLOBAL;
            }

            throw new InvalidSettingValueException();
        }

        @Override
        public String toPrettyString(SubTheme value) {
            switch (value) {
                case LIGHT:
                    return THEME_LIGHT;
                case DARK:
                    return THEME_DARK;
                case USE_GLOBAL:
                    return THEME_USE_GLOBAL;
            }

            throw new AssertionError("Unexpected case: " + value);
        }

        @Override
        public String toString(SubTheme value) {
            return value.name();
        }
    }

    private static class TimeSetting extends SettingsDescription<String> {
        private static final String VALIDATION_EXPRESSION = "[0-2]*[0-9]:[0-5]*[0-9]";

        TimeSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            if (!value.matches(VALIDATION_EXPRESSION)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }
    }
}
