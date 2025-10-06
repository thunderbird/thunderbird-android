package com.fsck.k9.preferences;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import android.content.Context;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.core.R;
import com.fsck.k9.preferences.Settings.BooleanSetting;
import com.fsck.k9.preferences.Settings.ColorSetting;
import com.fsck.k9.preferences.Settings.EnumSetting;
import com.fsck.k9.preferences.Settings.IntegerRangeSetting;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import com.fsck.k9.preferences.Settings.PseudoEnumSetting;
import com.fsck.k9.preferences.Settings.SettingsDescription;
import com.fsck.k9.preferences.Settings.StringSetting;
import com.fsck.k9.preferences.Settings.V;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo104;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo106;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo53;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo54;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo74;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo80;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo81;
import com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo91;
import net.thunderbird.core.android.account.AccountDefaultsProvider;
import net.thunderbird.core.android.account.DeletePolicy;
import net.thunderbird.core.android.account.Expunge;
import net.thunderbird.core.android.account.FolderMode;
import net.thunderbird.core.android.account.MessageFormat;
import net.thunderbird.core.android.account.QuoteStyle;
import net.thunderbird.core.android.account.ShowPictures;
import net.thunderbird.core.android.account.SortType;
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer;
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto;
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection;
import net.thunderbird.feature.notification.NotificationLight;
import static com.fsck.k9.preferences.upgrader.AccountSettingsUpgraderTo53.FOLDER_NONE;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_MESSAGE_FORMAT_AUTO;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_MESSAGE_READ_RECEIPT;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_QUOTED_TEXT_SHOWN;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_QUOTE_PREFIX;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_REMOTE_SEARCH_NUM_RESULTS;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_REPLY_AFTER_QUOTE;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_SORT_ASCENDING;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_STRIP_SIGNATURE;
import static net.thunderbird.core.android.account.AccountDefaultsProvider.DEFAULT_VISIBLE_LIMIT;
import static net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.FOLDER_PATH_DELIMITER_KEY;
import static net.thunderbird.feature.mail.folder.api.FolderPathDelimiterKt.FOLDER_DEFAULT_PATH_DELIMITER;


class AccountSettingsDescriptions {
    static final Map<String, TreeMap<Integer, SettingsDescription<?>>> SETTINGS;
    static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription<?>>> s = new LinkedHashMap<>();

        /*
         * When adding new settings here, be sure to increment {@link Settings.VERSION}
         * and use that for whatever you add here.
         */

        s.put("alwaysBcc", Settings.versions(
                new V(11, new StringSetting(""))
        ));
        s.put("alwaysShowCcBcc", Settings.versions(
                new V(13, new BooleanSetting(false))
        ));
        s.put("archiveFolderName", Settings.versions(
                new V(1, new StringSetting(FOLDER_NONE)),
                new V(53, new StringSetting(null))
        ));
        s.put("autoExpandFolderName", Settings.versions(
                new V(1, new StringSetting("INBOX")),
                new V(78, new StringSetting(null))
        ));
        s.put("automaticCheckIntervalMinutes", Settings.versions(
                new V(1, new IntegerResourceSetting(-1, R.array.check_frequency_values)),
                new V(61, new IntegerResourceSetting(60, R.array.check_frequency_values))
        ));
        s.put("chipColor", Settings.versions(
                new V(1, new ColorSetting(0xFF0000FF))
        ));
        s.put("defaultQuotedTextShown", Settings.versions(
                new V(1, new BooleanSetting(DEFAULT_QUOTED_TEXT_SHOWN))
        ));
        s.put("deletePolicy", Settings.versions(
                new V(1, new DeletePolicySetting(DeletePolicy.NEVER))
        ));
        s.put("displayCount", Settings.versions(
                new V(1, new IntegerResourceSetting(DEFAULT_VISIBLE_LIMIT,
                        R.array.display_count_values))
        ));
        s.put("draftsFolderName", Settings.versions(
                new V(1, new StringSetting(FOLDER_NONE)),
                new V(53, new StringSetting(null))
        ));
        s.put("expungePolicy", Settings.versions(
                new V(1, new StringResourceSetting(Expunge.EXPUNGE_IMMEDIATELY.name(),
                        R.array.expunge_policy_values))
        ));
        s.put("folderDisplayMode", Settings.versions(
                new V(1, new EnumSetting<>(FolderMode.class, FolderMode.NOT_SECOND_CLASS)),
                new V(100, null)
        ));
        s.put("folderPushMode", Settings.versions(
                new V(1, new EnumSetting<>(FolderMode.class, FolderMode.FIRST_CLASS)),
                new V(72, new EnumSetting<>(FolderMode.class, FolderMode.NONE)),
                new V(98, null)
        ));
        s.put("folderSyncMode", Settings.versions(
                new V(1, new EnumSetting<>(FolderMode.class, FolderMode.FIRST_CLASS)),
                new V(99, null)
        ));
        s.put("idleRefreshMinutes", Settings.versions(
                new V(1, new IntegerArraySetting(24, new int[] { 1, 2, 3, 6, 12, 24, 36, 48, 60 })),
                new V(74, new IntegerResourceSetting(24, R.array.idle_refresh_period_values))
        ));
        s.put("led", Settings.versions(
                new V(1, new BooleanSetting(true)),
                new V(80, null)
        ));
        s.put("ledColor", Settings.versions(
                new V(1, new ColorSetting(0xFF0000FF)),
                new V(80, null)
        ));
        s.put("markMessageAsReadOnView", Settings.versions(
                new V(7, new BooleanSetting(true))
        ));
        s.put("markMessageAsReadOnDelete", Settings.versions(
                new V(63, new BooleanSetting(true))
        ));
        s.put("maxPushFolders", Settings.versions(
                new V(1, new IntegerRangeSetting(0, 100, 10))
        ));
        s.put("maximumAutoDownloadMessageSize", Settings.versions(
                new V(1, new IntegerResourceSetting(32768, R.array.autodownload_message_size_values)),
                new V(93, new IntegerResourceSetting(131072, R.array.autodownload_message_size_values))
        ));
        s.put("maximumPolledMessageAge", Settings.versions(
                new V(1, new IntegerResourceSetting(-1, R.array.message_age_values))
        ));
        s.put("messageFormat", Settings.versions(
                new V(1, new EnumSetting<>(
                    MessageFormat.class,
                    AccountDefaultsProvider.getDEFAULT_MESSAGE_FORMAT()
                ))
        ));
        s.put("messageFormatAuto", Settings.versions(
                new V(2, new BooleanSetting(DEFAULT_MESSAGE_FORMAT_AUTO))
        ));
        s.put("messageReadReceipt", Settings.versions(
                new V(1, new BooleanSetting(DEFAULT_MESSAGE_READ_RECEIPT))
        ));
        s.put("notifyMailCheck", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("notifyNewMail", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("folderNotifyNewMailMode", Settings.versions(
                new V(34, new EnumSetting<>(FolderMode.class, FolderMode.ALL)),
                new V(96, null)
        ));
        s.put("notifySelfNewMail", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("quotePrefix", Settings.versions(
                new V(1, new StringSetting(DEFAULT_QUOTE_PREFIX))
        ));
        s.put("quoteStyle", Settings.versions(
                new V(1, new EnumSetting<>(QuoteStyle.class, AccountDefaultsProvider.getDEFAULT_QUOTE_STYLE()))
        ));
        s.put("replyAfterQuote", Settings.versions(
                new V(1, new BooleanSetting(DEFAULT_REPLY_AFTER_QUOTE))
        ));
        s.put("ring", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("ringtone", Settings.versions(
                new V(1, new RingtoneSetting("content://settings/system/notification_sound"))
        ));
        s.put("sentFolderName", Settings.versions(
                new V(1, new StringSetting(FOLDER_NONE)),
                new V(53, new StringSetting(null))
        ));
        s.put("sortTypeEnum", Settings.versions(
                new V(9, new EnumSetting<>(SortType.class, AccountDefaultsProvider.getDEFAULT_SORT_TYPE()))
        ));
        s.put("sortAscending", Settings.versions(
                new V(9, new BooleanSetting(DEFAULT_SORT_ASCENDING))
        ));
        s.put("showPicturesEnum", Settings.versions(
                new V(1, new EnumSetting<>(ShowPictures.class, ShowPictures.NEVER))
        ));
        s.put("signatureBeforeQuotedText", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("spamFolderName", Settings.versions(
                new V(1, new StringSetting(FOLDER_NONE)),
                new V(53, new StringSetting(null))
        ));
        s.put("stripSignature", Settings.versions(
                new V(2, new BooleanSetting(DEFAULT_STRIP_SIGNATURE))
        ));
        s.put("subscribedFoldersOnly", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("syncRemoteDeletions", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("trashFolderName", Settings.versions(
                new V(1, new StringSetting(FOLDER_NONE)),
                new V(53, new StringSetting(null))
        ));
        s.put("useCompression.MOBILE", Settings.versions(
                new V(1, new BooleanSetting(true)),
                new V(81, null)
        ));
        s.put("useCompression.OTHER", Settings.versions(
                new V(1, new BooleanSetting(true)),
                new V(81, null)
        ));
        s.put("useCompression.WIFI", Settings.versions(
                new V(1, new BooleanSetting(true)),
                new V(81, null)
        ));
        s.put("vibrate", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("vibratePattern", Settings.versions(
                new V(1, new IntegerResourceSetting(0, R.array.vibrate_pattern_values))
        ));
        s.put("vibrateTimes", Settings.versions(
                new V(1, new IntegerRangeSetting(1, 10, 5))
        ));
        s.put("remoteSearchNumResults", Settings.versions(
                new V(18, new IntegerResourceSetting(DEFAULT_REMOTE_SEARCH_NUM_RESULTS,
                        R.array.remote_search_num_results_values))
        ));
        s.put("remoteSearchFullText", Settings.versions(
                new V(18, new BooleanSetting(false))
        ));
        s.put("notifyContactsMailOnly", Settings.versions(
                new V(42, new BooleanSetting(false))
        ));
        s.put("openPgpHideSignOnly", Settings.versions(
                new V(50, new BooleanSetting(true))
        ));
        s.put("openPgpEncryptSubject", Settings.versions(
                new V(51, new BooleanSetting(true))
        ));
        s.put("openPgpEncryptAllDrafts", Settings.versions(
                new V(55, new BooleanSetting(true))
        ));
        s.put("autocryptMutualMode", Settings.versions(
                new V(50, new BooleanSetting(false))
        ));
        s.put("uploadSentMessages", Settings.versions(
                new V(52, new BooleanSetting(true))
        ));
        s.put("archiveFolderSelection", Settings.versions(
                new V(54, new EnumSetting<>(SpecialFolderSelection.class, SpecialFolderSelection.AUTOMATIC))
        ));
        s.put("draftsFolderSelection", Settings.versions(
                new V(54, new EnumSetting<>(SpecialFolderSelection.class, SpecialFolderSelection.AUTOMATIC))
        ));
        s.put("sentFolderSelection", Settings.versions(
                new V(54, new EnumSetting<>(SpecialFolderSelection.class, SpecialFolderSelection.AUTOMATIC))
        ));
        s.put("spamFolderSelection", Settings.versions(
                new V(54, new EnumSetting<>(SpecialFolderSelection.class, SpecialFolderSelection.AUTOMATIC))
        ));
        s.put("trashFolderSelection", Settings.versions(
                new V(54, new EnumSetting<>(SpecialFolderSelection.class, SpecialFolderSelection.AUTOMATIC))
        ));
        s.put("ignoreChatMessages", Settings.versions(
                new V(76, new BooleanSetting(false))
        ));
        s.put("notificationLight", Settings.versions(
                new V(80, new EnumSetting<>(NotificationLight.class, NotificationLight.Disabled))
        ));
        s.put("useCompression", Settings.versions(
                new V(81, new BooleanSetting(true))
        ));
        s.put("sendClientId", Settings.versions(
                new V(88, new BooleanSetting(true)),
                new V(91, null)
        ));
        s.put("sendClientInfo", Settings.versions(
                new V(91, new BooleanSetting(true))
        ));
        s.put("avatarType", Settings.versions(
                new V(104, new EnumSetting<>(AvatarTypeDto.class, AvatarTypeDto.MONOGRAM))
        ));
        s.put("avatarMonogram", Settings.versions(
            new V(104, new StringSetting("XX"))
        ));
        s.put("avatarImageUri", Settings.versions(
            new V(104, new StringSetting(null))
        ));
        s.put("avatarIconName", Settings.versions(
            new V(104, new StringSetting(null))
        ));
        s.put(
            FOLDER_PATH_DELIMITER_KEY,
            Settings.versions(new V(106, new StringSetting(FOLDER_DEFAULT_PATH_DELIMITER)))
        );
        // note that there is no setting for openPgpProvider, because this will have to be set up together
        // with the actual provider after import anyways.

        SETTINGS = Collections.unmodifiableMap(s);

        Map<Integer, SettingsUpgrader> u = new HashMap<>();
        u.put(53, new AccountSettingsUpgraderTo53());
        u.put(54, new AccountSettingsUpgraderTo54());
        u.put(74, new AccountSettingsUpgraderTo74());
        u.put(80, new AccountSettingsUpgraderTo80());
        u.put(81, new AccountSettingsUpgraderTo81());
        u.put(91, new AccountSettingsUpgraderTo91());
        u.put(104, new AccountSettingsUpgraderTo104());
        u.put(106, new AccountSettingsUpgraderTo106(new ServerSettingsDtoSerializer()));

        UPGRADERS = Collections.unmodifiableMap(u);
    }

    static Map<String, Object> validate(int version, Map<String, String> importedSettings, boolean useDefaultValues) {
        return Settings.validate(version, SETTINGS, importedSettings, useDefaultValues);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }

    private static class IntegerResourceSetting extends PseudoEnumSetting<Integer> {
        private final Context context = DI.get(Context.class);
        private final Map<Integer, String> mapping;

        IntegerResourceSetting(int defaultValue, int resId) {
            super(defaultValue);

            Map<Integer, String> mapping = new HashMap<>();
            String[] values = context.getResources().getStringArray(resId);
            for (String value : values) {
                int intValue = Integer.parseInt(value);
                mapping.put(intValue, value);
            }
            this.mapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mapping;
        }

        @Override
        public Integer fromString(String value) throws InvalidSettingValueException {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingValueException();
            }
        }
    }

    private static class IntegerArraySetting extends SettingsDescription<Integer> {
        private final int[] values;

        IntegerArraySetting(int defaultValue, int[] values) {
            super(defaultValue);
            this.values = values;
        }

        @Override
        public Integer fromString(String value) throws InvalidSettingValueException {
            try {
                int number = Integer.parseInt(value);
                for (int validValue : values) {
                    if (number == validValue) {
                        return number;
                    }
                }

                throw new InvalidSettingValueException();
            } catch (NumberFormatException e) {
                throw new InvalidSettingValueException();
            }
        }
    }

    private static class StringResourceSetting extends PseudoEnumSetting<String> {
        private final Context context = DI.get(Context.class);
        private final Map<String, String> mapping;

        StringResourceSetting(String defaultValue, int resId) {
            super(defaultValue);

            Map<String, String> mapping = new HashMap<>();
            String[] values = context.getResources().getStringArray(resId);
            for (String value : values) {
                mapping.put(value, value);
            }
            this.mapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<String, String> getMapping() {
            return mapping;
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            if (!mapping.containsKey(value)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }
    }

    private static class RingtoneSetting extends SettingsDescription<String> {
        RingtoneSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public String fromString(String value) {
            //TODO: add validation
            return value;
        }
    }

    private static class DeletePolicySetting extends PseudoEnumSetting<Integer> {
        private Map<Integer, String> mapping;

        DeletePolicySetting(DeletePolicy defaultValue) {
            super(defaultValue.setting);
            Map<Integer, String> mapping = new HashMap<>();
            mapping.put(DeletePolicy.NEVER.setting, "NEVER");
            mapping.put(DeletePolicy.ON_DELETE.setting, "DELETE");
            mapping.put(DeletePolicy.MARK_AS_READ.setting, "MARK_AS_READ");
            this.mapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mapping;
        }

        @Override
        public Integer fromString(String value) throws InvalidSettingValueException {
            try {
                Integer deletePolicy = Integer.parseInt(value);
                if (mapping.containsKey(deletePolicy)) {
                    return deletePolicy;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }

}
