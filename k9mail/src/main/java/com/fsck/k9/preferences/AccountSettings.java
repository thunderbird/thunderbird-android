package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fsck.k9.Account;
import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Account.Searchable;
import com.fsck.k9.Account.ShowPictures;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.preferences.Settings.*;

public class AccountSettings {
    public static final Map<String, TreeMap<Integer, SettingsDescription>> SETTINGS;
    public static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription>> s =
            new LinkedHashMap<String, TreeMap<Integer, SettingsDescription>>();

        /**
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
                new V(1, new StringSetting("Archive"))
            ));
        s.put("autoExpandFolderName", Settings.versions(
                new V(1, new StringSetting("INBOX"))
            ));
        s.put("automaticCheckIntervalMinutes", Settings.versions(
                new V(1, new IntegerResourceSetting(-1,
                        R.array.account_settings_check_frequency_values))
            ));
        s.put("chipColor", Settings.versions(
                new V(1, new ColorSetting(0xFF0000FF))
            ));
        s.put("cryptoApp", Settings.versions(
                new V(1, new StringSetting("apg")),
                new V(36, new StringSetting(Account.NO_OPENPGP_PROVIDER))
            ));
        s.put("defaultQuotedTextShown", Settings.versions(
                new V(1, new BooleanSetting(Account.DEFAULT_QUOTED_TEXT_SHOWN))
            ));
        s.put("deletePolicy", Settings.versions(
                new V(1, new DeletePolicySetting(DeletePolicy.NEVER))
            ));
        s.put("displayCount", Settings.versions(
                new V(1, new IntegerResourceSetting(K9.DEFAULT_VISIBLE_LIMIT,
                        R.array.account_settings_display_count_values))
            ));
        s.put("draftsFolderName", Settings.versions(
                new V(1, new StringSetting("Drafts"))
            ));
        s.put("expungePolicy", Settings.versions(
                new V(1, new StringResourceSetting(Expunge.EXPUNGE_IMMEDIATELY.name(),
                        R.array.account_setup_expunge_policy_values))
            ));
        s.put("folderDisplayMode", Settings.versions(
                new V(1, new EnumSetting<FolderMode>(FolderMode.class, FolderMode.NOT_SECOND_CLASS))
            ));
        s.put("folderPushMode", Settings.versions(
                new V(1, new EnumSetting<FolderMode>(FolderMode.class, FolderMode.FIRST_CLASS))
            ));
        s.put("folderSyncMode", Settings.versions(
                new V(1, new EnumSetting<FolderMode>(FolderMode.class, FolderMode.FIRST_CLASS))
            ));
        s.put("folderTargetMode", Settings.versions(
                new V(1, new EnumSetting<FolderMode>(FolderMode.class, FolderMode.NOT_SECOND_CLASS))
            ));
        s.put("goToUnreadMessageSearch", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("idleRefreshMinutes", Settings.versions(
                new V(1, new IntegerResourceSetting(24, R.array.idle_refresh_period_values))
            ));
        s.put("inboxFolderName", Settings.versions(
                new V(1, new StringSetting("INBOX"))
            ));
        s.put("led", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("ledColor", Settings.versions(
                new V(1, new ColorSetting(0xFF0000FF))
            ));
        s.put("localStorageProvider", Settings.versions(
                new V(1, new StorageProviderSetting())
            ));
        s.put("markMessageAsReadOnView", Settings.versions(
                new V(7, new BooleanSetting(true))
            ));
        s.put("maxPushFolders", Settings.versions(
                new V(1, new IntegerRangeSetting(0, 100, 10))
            ));
        s.put("maximumAutoDownloadMessageSize", Settings.versions(
                new V(1, new IntegerResourceSetting(32768,
                        R.array.account_settings_autodownload_message_size_values))
            ));
        s.put("maximumPolledMessageAge", Settings.versions(
                new V(1, new IntegerResourceSetting(-1,
                        R.array.account_settings_message_age_values))
            ));
        s.put("messageFormat", Settings.versions(
                new V(1, new EnumSetting<MessageFormat>(
                        MessageFormat.class, Account.DEFAULT_MESSAGE_FORMAT))
            ));
        s.put("messageFormatAuto", Settings.versions(
                new V(2, new BooleanSetting(Account.DEFAULT_MESSAGE_FORMAT_AUTO))
            ));
        s.put("messageReadReceipt", Settings.versions(
                new V(1, new BooleanSetting(Account.DEFAULT_MESSAGE_READ_RECEIPT))
            ));
        s.put("notifyMailCheck", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("notifyNewMail", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("folderNotifyNewMailMode", Settings.versions(
                new V(34, new EnumSetting<FolderMode>(FolderMode.class, FolderMode.ALL))
            ));
        s.put("notifySelfNewMail", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("pushPollOnConnect", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("quotePrefix", Settings.versions(
                new V(1, new StringSetting(Account.DEFAULT_QUOTE_PREFIX))
            ));
        s.put("quoteStyle", Settings.versions(
                new V(1, new EnumSetting<QuoteStyle>(
                        QuoteStyle.class, Account.DEFAULT_QUOTE_STYLE))
            ));
        s.put("replyAfterQuote", Settings.versions(
                new V(1, new BooleanSetting(Account.DEFAULT_REPLY_AFTER_QUOTE))
            ));
        s.put("ring", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("ringtone", Settings.versions(
                new V(1, new RingtoneSetting("content://settings/system/notification_sound"))
            ));
        s.put("searchableFolders", Settings.versions(
                new V(1, new EnumSetting<Searchable>(
                        Searchable.class, Searchable.ALL))
            ));
        s.put("sentFolderName", Settings.versions(
                new V(1, new StringSetting("Sent"))
            ));
        s.put("sortTypeEnum", Settings.versions(
                new V(9, new EnumSetting<SortType>(SortType.class, Account.DEFAULT_SORT_TYPE))
            ));
        s.put("sortAscending", Settings.versions(
                new V(9, new BooleanSetting(Account.DEFAULT_SORT_ASCENDING))
            ));
        s.put("showPicturesEnum", Settings.versions(
                new V(1, new EnumSetting<ShowPictures>(
                        ShowPictures.class, ShowPictures.NEVER))
            ));
        s.put("signatureBeforeQuotedText", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("spamFolderName", Settings.versions(
                new V(1, new StringSetting("Spam"))
            ));
        s.put("stripSignature", Settings.versions(
                new V(2, new BooleanSetting(Account.DEFAULT_STRIP_SIGNATURE))
            ));
        s.put("subscribedFoldersOnly", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("syncRemoteDeletions", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("trashFolderName", Settings.versions(
                new V(1, new StringSetting("Trash"))
            ));
        s.put("useCompression.MOBILE", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("useCompression.OTHER", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("useCompression.WIFI", Settings.versions(
                new V(1, new BooleanSetting(true))
            ));
        s.put("vibrate", Settings.versions(
                new V(1, new BooleanSetting(false))
            ));
        s.put("vibratePattern", Settings.versions(
                new V(1, new IntegerResourceSetting(0,
                R.array.account_settings_vibrate_pattern_values))
            ));
        s.put("vibrateTimes", Settings.versions(
                new V(1, new IntegerResourceSetting(5,
                R.array.account_settings_vibrate_times_label))
            ));
        s.put("allowRemoteSearch", Settings.versions(
                new V(18, new BooleanSetting(true))
            ));
        s.put("remoteSearchNumResults", Settings.versions(
                new V(18, new IntegerResourceSetting(Account.DEFAULT_REMOTE_SEARCH_NUM_RESULTS,
                R.array.account_settings_remote_search_num_results_values))
            ));
        s.put("remoteSearchFullText", Settings.versions(
                new V(18, new BooleanSetting(false))
            ));
        s.put("notifyContactsMailOnly", Settings.versions(
                new V(42, new BooleanSetting(false))
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

    public static Map<String, String> getAccountSettings(Storage storage, String uuid) {
        Map<String, String> result = new HashMap<String, String>();
        String prefix = uuid + ".";
        for (String key : SETTINGS.keySet()) {
            String value = storage.getString(prefix + key, null);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * An integer resource setting.
     *
     * <p>
     * Basically a {@link PseudoEnumSetting} that is initialized from a resource array containing
     * integer strings.
     * </p>
     */
    public static class IntegerResourceSetting extends PseudoEnumSetting<Integer> {
        private final Map<Integer, String> mMapping;

        public IntegerResourceSetting(int defaultValue, int resId) {
            super(defaultValue);

            Map<Integer, String> mapping = new HashMap<Integer, String>();
            String[] values = K9.app.getResources().getStringArray(resId);
            for (String value : values) {
                int intValue = Integer.parseInt(value);
                mapping.put(intValue, value);
            }
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingValueException();
            }
        }
    }

    /**
     * A string resource setting.
     *
     * <p>
     * Basically a {@link PseudoEnumSetting} that is initialized from a resource array.
     * </p>
     */
    public static class StringResourceSetting extends PseudoEnumSetting<String> {
        private final Map<String, String> mMapping;

        public StringResourceSetting(String defaultValue, int resId) {
            super(defaultValue);

            Map<String, String> mapping = new HashMap<String, String>();
            String[] values = K9.app.getResources().getStringArray(resId);
            for (String value : values) {
                mapping.put(value, value);
            }
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<String, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            if (!mMapping.containsKey(value)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }
    }

    /**
     * The notification ringtone setting.
     */
    public static class RingtoneSetting extends SettingsDescription {
        public RingtoneSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public Object fromString(String value) {
            //TODO: add validation
            return value;
        }
    }

    /**
     * The storage provider setting.
     */
    public static class StorageProviderSetting extends SettingsDescription {
        public StorageProviderSetting() {
            super(null);
        }

        @Override
        public Object getDefaultValue() {
            return StorageManager.getInstance(K9.app).getDefaultProviderId();
        }

        @Override
        public Object fromString(String value) {
            StorageManager storageManager = StorageManager.getInstance(K9.app);
            Map<String, String> providers = storageManager.getAvailableProviders();
            if (providers.containsKey(value)) {
                return value;
            }
            throw new RuntimeException("Validation failed");
        }
    }

    public static class DeletePolicySetting extends PseudoEnumSetting<Integer> {
        private Map<Integer, String> mMapping;

        public DeletePolicySetting(DeletePolicy defaultValue) {
            super(defaultValue);
            Map<Integer, String> mapping = new HashMap<Integer, String>();
            mapping.put(DeletePolicy.NEVER.setting, "NEVER");
            mapping.put(DeletePolicy.ON_DELETE.setting, "DELETE");
            mapping.put(DeletePolicy.MARK_AS_READ.setting, "MARK_AS_READ");
            mMapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<Integer, String> getMapping() {
            return mMapping;
        }

        @Override
        public Object fromString(String value) throws InvalidSettingValueException {
            try {
                Integer deletePolicy = Integer.parseInt(value);
                if (mMapping.containsKey(deletePolicy)) {
                    return deletePolicy;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }
}
