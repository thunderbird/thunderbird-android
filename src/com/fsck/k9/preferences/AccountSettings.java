package com.fsck.k9.preferences;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Account.ScrollButtons;
import com.fsck.k9.crypto.Apg;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.preferences.Settings.*;

public class AccountSettings {
    public static final Map<String, SettingsDescription> SETTINGS;

    static {
        SETTINGS = new LinkedHashMap<String, SettingsDescription>();

        // mandatory
        /*
        SETTINGS.put("storeUri",
                SD(SettingType.STRING, Settings.EXCEPTION_DEFAULT_VALUE, new StoreUriValidator()));
        SETTINGS.put("transportUri",
                SD(SettingType.STRING, Settings.EXCEPTION_DEFAULT_VALUE,
                        new TransportUriValidator()));
        */

        SETTINGS.put("archiveFolderName",
                SD(SettingType.STRING, "Archive", null));
        SETTINGS.put("autoExpandFolderName",
                SD(SettingType.STRING, "INBOX", null));
        SETTINGS.put("automaticCheckIntervalMinutes",
                SD(SettingType.INTEGER, -1, new ResourceArrayValidator(
                        R.array.account_settings_check_frequency_values)));
        SETTINGS.put("chipColor",
                SD(SettingType.INTEGER, 0xff0000ff, Settings.SOLID_COLOR_VALIDATOR));
        SETTINGS.put("cryptoApp",
                SD(SettingType.STRING, Apg.NAME, null));
        SETTINGS.put("cryptoAutoSignature",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("deletePolicy",
                SD(SettingType.STRING, 0, new ResourceArrayValidator(
                        R.array.account_setup_delete_policy_values)));
        SETTINGS.put("displayCount",
                SD(SettingType.STRING, K9.DEFAULT_VISIBLE_LIMIT, new ResourceArrayValidator(
                        R.array.account_settings_display_count_values)));
        SETTINGS.put("draftsFolderName",
                SD(SettingType.STRING, "Drafts", null));
        SETTINGS.put("enableMoveButtons",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("expungePolicy",
                SD(SettingType.STRING, Account.EXPUNGE_IMMEDIATELY, new ResourceArrayValidator(
                        R.array.account_setup_expunge_policy_values)));
        SETTINGS.put("folderDisplayMode",
                SD(SettingType.ENUM, FolderMode.NOT_SECOND_CLASS, new ResourceArrayValidator(
                        R.array.account_settings_folder_display_mode_values)));
        SETTINGS.put("folderPushMode",
                SD(SettingType.ENUM, FolderMode.FIRST_CLASS, new ResourceArrayValidator(
                        R.array.account_settings_folder_push_mode_values)));
        SETTINGS.put("folderSyncMode",
                SD(SettingType.ENUM, FolderMode.FIRST_CLASS, new ResourceArrayValidator(
                        R.array.folder_settings_folder_sync_mode_values)));
        SETTINGS.put("folderTargetMode",
                SD(SettingType.ENUM, FolderMode.NOT_SECOND_CLASS, new ResourceArrayValidator(
                        R.array.account_settings_folder_target_mode_values)));
        SETTINGS.put("goToUnreadMessageSearch",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("hideButtonsEnum",
                SD(SettingType.ENUM, ScrollButtons.NEVER, new ResourceArrayValidator(
                        R.array.account_settings_hide_buttons_values)));
        SETTINGS.put("hideMoveButtonsEnum",
                SD(SettingType.ENUM, ScrollButtons.NEVER, new ResourceArrayValidator(
                        R.array.account_settings_hide_move_buttons_values)));
        SETTINGS.put("idleRefreshMinutes",
                SD(SettingType.INTEGER, 24, new ResourceArrayValidator(
                        R.array.idle_refresh_period_values)));
        SETTINGS.put("led",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("ledColor",
                SD(SettingType.INTEGER, 0xff0000ff, Settings.SOLID_COLOR_VALIDATOR));
        SETTINGS.put("localStorageProvider",
                SD(SettingType.STRING, new StorageProviderDefaultValue(),
                        new StorageProviderValidator()));
        SETTINGS.put("maxPushFolders",
                SD(SettingType.INTEGER, 10, Settings.POSITIVE_INTEGER_VALIDATOR));
        SETTINGS.put("maximumAutoDownloadMessageSize",
                SD(SettingType.ENUM, 32768, new ResourceArrayValidator(
                        R.array.account_settings_autodownload_message_size_values)));
        SETTINGS.put("maximumPolledMessageAge",
                SD(SettingType.ENUM, -1, new ResourceArrayValidator(
                        R.array.account_settings_message_age_values)));
        SETTINGS.put("messageFormat",
                SD(SettingType.ENUM, Account.DEFAULT_MESSAGE_FORMAT, new ResourceArrayValidator(
                        R.array.account_settings_message_format_values)));
        SETTINGS.put("notificationUnreadCount",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("notifyMailCheck",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("notifyNewMail",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("notifySelfNewMail",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("pushPollOnConnect",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("quotePrefix",
                SD(SettingType.STRING, Account.DEFAULT_QUOTE_PREFIX, null));
        SETTINGS.put("quoteStyle",
                SD(SettingType.ENUM, Account.DEFAULT_QUOTE_STYLE, new ResourceArrayValidator(
                        R.array.account_settings_quote_style_values)));
        SETTINGS.put("replyAfterQuote",
                SD(SettingType.BOOLEAN, Account.DEFAULT_REPLY_AFTER_QUOTE, null));
        SETTINGS.put("ring",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("ringtone",
                SD(SettingType.STRING, "content://settings/system/notification_sound",
                        new RingtoneValidator()));
        SETTINGS.put("saveAllHeaders",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("searchableFolders",
                SD(SettingType.ENUM, Account.Searchable.ALL, new ResourceArrayValidator(
                        R.array.account_settings_searchable_values)));
        SETTINGS.put("sentFolderName",
                SD(SettingType.STRING, "Sent", null));
        SETTINGS.put("showPicturesEnum",
                SD(SettingType.ENUM, Account.ShowPictures.NEVER, new ResourceArrayValidator(
                        R.array.account_settings_show_pictures_values)));
        SETTINGS.put("signatureBeforeQuotedText",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("spamFolderName",
                SD(SettingType.STRING, "Spam", null));
        SETTINGS.put("subscribedFoldersOnly",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("syncRemoteDeletions",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("trashFolderName",
                SD(SettingType.STRING, "Trash", null));
        SETTINGS.put("useCompression.MOBILE",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("useCompression.OTHER",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("useCompression.WIFI",
                SD(SettingType.BOOLEAN, true, null));
        SETTINGS.put("vibrate",
                SD(SettingType.BOOLEAN, false, null));
        SETTINGS.put("vibratePattern",
                SD(SettingType.INTEGER, 0, new ResourceArrayValidator(
                        R.array.account_settings_vibrate_pattern_values)));
        SETTINGS.put("vibrateTimes",
                SD(SettingType.INTEGER, 5, new ResourceArrayValidator(
                        R.array.account_settings_vibrate_times_label)));
    }

    // Just to have shorter lines in SETTINGS initialization
    private static SettingsDescription SD(SettingType type,
            Object defaultValue, ISettingValidator validator) {
        return new SettingsDescription(type, defaultValue, validator);
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


    public static class StorageProviderDefaultValue implements IDefaultValue {
        @Override
        public Object computeDefaultValue(String key, Map<String, String> validatedSettings) {
            return StorageManager.getInstance(K9.app).getDefaultProviderId();
        }

    }

    public static class StorageProviderValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            Map<String, String> providers = StorageManager.getInstance(K9.app).getAvailableProviders();
            for (String storageProvider : providers.keySet()) {
                if (storageProvider.equals(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class RingtoneValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            // TODO implement
            return true;
        }
    }

    public static class StoreUriValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            try {
                String uriString = Utility.base64Decode(value);
                if (!uriString.startsWith("imap") && !uriString.startsWith("pop3") &&
                        !uriString.startsWith("webdav")) {
                    return false;
                }

                //TODO: check complete scheme (imap+ssl etc.)

                Uri uri = Uri.parse(uriString);
                String[] userInfoParts = uri.getUserInfo().split(":");
                if (userInfoParts.length < 2) {
                    return false;
                }
                //TODO: check if username and password are urlencoded

                String host = uri.getHost();
                if (host == null || host.length() == 0) {
                    return false;
                }

                //TODO: check store specifics

                return true;
            } catch (Exception e) { Log.e(K9.LOG_TAG, "oops", e); /* Ignore */ }

            return false;
        }
    }

    public static class TransportUriValidator implements ISettingValidator {
        @Override
        public boolean isValid(String key, String value, Map<String, String> validatedSettings) {
            try {
                String uriString = Utility.base64Decode(value);
                if (!uriString.startsWith("smtp") && !uriString.startsWith("webdav")) {
                    return false;
                }

                //TODO: check complete scheme (smtp+ssl etc.)

                Uri uri = Uri.parse(uriString);
                String[] userInfoParts = uri.getUserInfo().split(":");
                if (userInfoParts.length < 2) {
                    return false;
                }
                //TODO: check if username and password are urlencoded

                String host = uri.getHost();
                if (host == null || host.length() == 0) {
                    return false;
                }

                //TODO: check store specifics

                return true;
            } catch (Exception e) { Log.e(K9.LOG_TAG, "oops", e); /* Ignore */ }

            return false;
        }
    }
}
