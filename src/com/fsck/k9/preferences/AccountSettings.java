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
        Map<String, SettingsDescription> s = new LinkedHashMap<String, SettingsDescription>();

        s.put("archiveFolderName", new StringSetting("Archive"));
        s.put("autoExpandFolderName", new StringSetting("INBOX"));
        s.put("automaticCheckIntervalMinutes",
                new IntegerResourceSetting(-1, R.array.account_settings_check_frequency_values));
        s.put("chipColor", new ColorSetting(0xFF0000FF));
        s.put("cryptoApp", new StringSetting(Apg.NAME));
        s.put("cryptoAutoSignature", new BooleanSetting(false));
        s.put("defaultQuotedTextShown", new BooleanSetting(Account.DEFAULT_QUOTED_TEXT_SHOWN));
        s.put("deletePolicy", new DeletePolicySetting(Account.DELETE_POLICY_NEVER));
        s.put("displayCount", new IntegerResourceSetting(K9.DEFAULT_VISIBLE_LIMIT,
                R.array.account_settings_display_count_values));
        s.put("draftsFolderName", new StringSetting("Drafts"));
        s.put("enableMoveButtons", new BooleanSetting(false));
        s.put("expungePolicy", new StringResourceSetting(Account.EXPUNGE_IMMEDIATELY,
                R.array.account_setup_expunge_policy_values));
        s.put("folderDisplayMode", new EnumSetting(FolderMode.class, FolderMode.NOT_SECOND_CLASS));
        s.put("folderPushMode", new EnumSetting(FolderMode.class, FolderMode.FIRST_CLASS));
        s.put("folderSyncMode", new EnumSetting(FolderMode.class, FolderMode.FIRST_CLASS));
        s.put("folderTargetMode", new EnumSetting(FolderMode.class, FolderMode.NOT_SECOND_CLASS));
        s.put("goToUnreadMessageSearch", new BooleanSetting(false));
        s.put("hideButtonsEnum", new EnumSetting(ScrollButtons.class, ScrollButtons.NEVER));
        s.put("hideMoveButtonsEnum", new EnumSetting(ScrollButtons.class, ScrollButtons.NEVER));
        s.put("idleRefreshMinutes", new IntegerResourceSetting(24,
                R.array.idle_refresh_period_values));
        s.put("inboxFolderName", new StringSetting("INBOX"));
        s.put("led", new BooleanSetting(true));
        s.put("ledColor", new ColorSetting(0xFF0000FF));
        s.put("localStorageProvider", new StorageProviderSetting());
        s.put("maxPushFolders", new IntegerRangeSetting(0, 100, 10));
        s.put("maximumAutoDownloadMessageSize", new IntegerResourceSetting(32768,
                R.array.account_settings_autodownload_message_size_values));
        s.put("maximumPolledMessageAge", new IntegerResourceSetting(-1,
                R.array.account_settings_message_age_values));
        s.put("messageFormat",
                new EnumSetting(Account.MessageFormat.class, Account.DEFAULT_MESSAGE_FORMAT));
        s.put("messageFormatAuto", new BooleanSetting(Account.DEFAULT_MESSAGE_FORMAT_AUTO)); // added to version 2
        s.put("messageReadReceipt", new BooleanSetting(Account.DEFAULT_MESSAGE_READ_RECEIPT));
        s.put("notificationUnreadCount", new BooleanSetting(true));
        s.put("notifyMailCheck", new BooleanSetting(false));
        s.put("notifyNewMail", new BooleanSetting(false));
        s.put("notifySelfNewMail", new BooleanSetting(true));
        s.put("pushPollOnConnect", new BooleanSetting(true));
        s.put("quotePrefix", new StringSetting(Account.DEFAULT_QUOTE_PREFIX));
        s.put("quoteStyle",
                new EnumSetting(Account.QuoteStyle.class, Account.DEFAULT_QUOTE_STYLE));
        s.put("replyAfterQuote", new BooleanSetting(Account.DEFAULT_REPLY_AFTER_QUOTE));
        s.put("stripSignature", new BooleanSetting(Account.DEFAULT_STRIP_SIGNATURE)); // added to version 2
        s.put("ring", new BooleanSetting(true));
        s.put("ringtone", new RingtoneSetting("content://settings/system/notification_sound"));
        s.put("saveAllHeaders", new BooleanSetting(true));
        s.put("searchableFolders",
                new EnumSetting(Account.Searchable.class, Account.Searchable.ALL));
        s.put("sentFolderName", new StringSetting("Sent"));
        s.put("showPicturesEnum",
                new EnumSetting(Account.ShowPictures.class, Account.ShowPictures.NEVER));
        s.put("signatureBeforeQuotedText", new BooleanSetting(false));
        s.put("spamFolderName", new StringSetting("Spam"));
        s.put("subscribedFoldersOnly", new BooleanSetting(false));
        s.put("syncRemoteDeletions", new BooleanSetting(true));
        s.put("trashFolderName", new StringSetting("Trash"));
        s.put("useCompression.MOBILE", new BooleanSetting(true));
        s.put("useCompression.OTHER", new BooleanSetting(true));
        s.put("useCompression.WIFI", new BooleanSetting(true));
        s.put("vibrate", new BooleanSetting(false));
        s.put("vibratePattern", new IntegerResourceSetting(0,
                R.array.account_settings_vibrate_pattern_values));
        s.put("vibrateTimes", new IntegerResourceSetting(5,
                R.array.account_settings_vibrate_times_label));

        SETTINGS = Collections.unmodifiableMap(s);
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
