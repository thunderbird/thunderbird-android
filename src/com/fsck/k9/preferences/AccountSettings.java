package com.fsck.k9.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.content.SharedPreferences;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Account.ScrollButtons;
import com.fsck.k9.crypto.Apg;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.preferences.Settings.*;

public class AccountSettings {
    public static final Map<String, SettingsDescription> SETTINGS;

    static {
        SETTINGS = new LinkedHashMap<String, SettingsDescription>();

        SETTINGS.put("archiveFolderName", new StringSetting("Archive"));
        SETTINGS.put("autoExpandFolderName", new StringSetting("INBOX"));
        SETTINGS.put("automaticCheckIntervalMinutes",
                new IntegerResourceSetting(-1, R.array.account_settings_check_frequency_values));
        SETTINGS.put("chipColor", new ColorSetting(0xFF0000FF));
        SETTINGS.put("cryptoApp", new StringSetting(Apg.NAME));
        SETTINGS.put("cryptoAutoSignature", new BooleanSetting(false));
        SETTINGS.put("deletePolicy", new DeletePolicySetting(Account.DELETE_POLICY_NEVER));
        SETTINGS.put("displayCount", new IntegerResourceSetting(K9.DEFAULT_VISIBLE_LIMIT,
                R.array.account_settings_display_count_values));
        SETTINGS.put("draftsFolderName", new StringSetting("Drafts"));
        SETTINGS.put("enableMoveButtons", new BooleanSetting(false));
        SETTINGS.put("expungePolicy", new StringResourceSetting(Account.EXPUNGE_IMMEDIATELY,
                R.array.account_setup_expunge_policy_values));
        SETTINGS.put("folderDisplayMode",
                new EnumSetting(FolderMode.class, FolderMode.NOT_SECOND_CLASS));
        SETTINGS.put("folderPushMode", new EnumSetting(FolderMode.class, FolderMode.FIRST_CLASS));
        SETTINGS.put("folderSyncMode", new EnumSetting(FolderMode.class, FolderMode.FIRST_CLASS));
        SETTINGS.put("folderTargetMode",
                new EnumSetting(FolderMode.class, FolderMode.NOT_SECOND_CLASS));
        SETTINGS.put("goToUnreadMessageSearch", new BooleanSetting(false));
        SETTINGS.put("hideButtonsEnum", new EnumSetting(ScrollButtons.class, ScrollButtons.NEVER));
        SETTINGS.put("hideMoveButtonsEnum",
                new EnumSetting(ScrollButtons.class, ScrollButtons.NEVER));
        SETTINGS.put("idleRefreshMinutes", new IntegerResourceSetting(24,
                R.array.idle_refresh_period_values));
        SETTINGS.put("led", new BooleanSetting(true));
        SETTINGS.put("ledColor", new ColorSetting(0xFF0000FF));
        SETTINGS.put("localStorageProvider", new StorageProviderSetting());
        SETTINGS.put("maxPushFolders", new IntegerRangeSetting(0, 100, 10));
        SETTINGS.put("maximumAutoDownloadMessageSize", new IntegerResourceSetting(32768,
                R.array.account_settings_autodownload_message_size_values));
        SETTINGS.put("maximumPolledMessageAge", new IntegerResourceSetting(-1,
                R.array.account_settings_message_age_values));
        SETTINGS.put("messageFormat",
                new EnumSetting(Account.MessageFormat.class, Account.DEFAULT_MESSAGE_FORMAT));
        SETTINGS.put("notificationUnreadCount", new BooleanSetting(true));
        SETTINGS.put("notifyMailCheck", new BooleanSetting(false));
        SETTINGS.put("notifyNewMail", new BooleanSetting(false));
        SETTINGS.put("notifySelfNewMail", new BooleanSetting(true));
        SETTINGS.put("pushPollOnConnect", new BooleanSetting(true));
        SETTINGS.put("quotePrefix", new StringSetting(Account.DEFAULT_QUOTE_PREFIX));
        SETTINGS.put("quoteStyle",
                new EnumSetting(Account.QuoteStyle.class, Account.DEFAULT_QUOTE_STYLE));
        SETTINGS.put("replyAfterQuote", new BooleanSetting(Account.DEFAULT_REPLY_AFTER_QUOTE));
        SETTINGS.put("ring", new BooleanSetting(true));
        SETTINGS.put("ringtone",
                new RingtoneSetting("content://settings/system/notification_sound"));
        SETTINGS.put("saveAllHeaders", new BooleanSetting(true));
        SETTINGS.put("searchableFolders",
                new EnumSetting(Account.Searchable.class, Account.Searchable.ALL));
        SETTINGS.put("sentFolderName", new StringSetting("Sent"));
        SETTINGS.put("showPicturesEnum",
                new EnumSetting(Account.ShowPictures.class, Account.ShowPictures.NEVER));
        SETTINGS.put("signatureBeforeQuotedText", new BooleanSetting(false));
        SETTINGS.put("spamFolderName", new StringSetting("Spam"));
        SETTINGS.put("subscribedFoldersOnly", new BooleanSetting(false));
        SETTINGS.put("syncRemoteDeletions", new BooleanSetting(true));
        SETTINGS.put("trashFolderName", new StringSetting("Trash"));
        SETTINGS.put("useCompression.MOBILE", new BooleanSetting(true));
        SETTINGS.put("useCompression.OTHER", new BooleanSetting(true));
        SETTINGS.put("useCompression.WIFI", new BooleanSetting(true));
        SETTINGS.put("vibrate", new BooleanSetting(false));
        SETTINGS.put("vibratePattern", new IntegerResourceSetting(0,
                R.array.account_settings_vibrate_pattern_values));
        SETTINGS.put("vibrateTimes", new IntegerResourceSetting(5,
                R.array.account_settings_vibrate_times_label));
    }

    public static Map<String, String> validate(Map<String, String> importedSettings,
            boolean useDefaultValues) {
        return Settings.validate(SETTINGS, importedSettings, useDefaultValues);
    }

    public static Map<String, String> getAccountSettings(SharedPreferences storage, String uuid) {
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

    /**
     * The delete policy setting.
     */
    public static class DeletePolicySetting extends PseudoEnumSetting<Integer> {
        private Map<Integer, String> mMapping;

        public DeletePolicySetting(int defaultValue) {
            super(defaultValue);
            Map<Integer, String> mapping = new HashMap<Integer, String>();
            mapping.put(Account.DELETE_POLICY_NEVER, "NEVER");
            mapping.put(Account.DELETE_POLICY_ON_DELETE, "DELETE");
            mapping.put(Account.DELETE_POLICY_MARK_AS_READ, "MARK_AS_READ");
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
