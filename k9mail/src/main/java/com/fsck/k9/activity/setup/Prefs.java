package com.fsck.k9.activity.setup;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.K9.SplitViewMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.notification.NotificationController;
import com.fsck.k9.preferences.CheckBoxListPreference;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.preferences.TimePickerPreference;
import com.fsck.k9.service.MailService;
import com.fsck.k9.ui.dialog.ApgDeprecationWarningDialog;
import org.openintents.openpgp.util.OpenPgpAppPreference;


public class Prefs extends K9PreferenceActivity {

    /**
     * Immutable empty {@link CharSequence} array
     */
    private static final CharSequence[] EMPTY_CHAR_SEQUENCE_ARRAY = new CharSequence[0];

    /*
     * Keys of the preferences defined in res/xml/global_preferences.xml
     */
    private static final String PREFERENCE_LANGUAGE = "language";
    private static final String PREFERENCE_THEME = "theme";
    private static final String PREFERENCE_MESSAGE_VIEW_THEME = "messageViewTheme";
    private static final String PREFERENCE_FIXED_MESSAGE_THEME = "fixed_message_view_theme";
    private static final String PREFERENCE_COMPOSER_THEME = "message_compose_theme";
    private static final String PREFERENCE_FONT_SIZE = "font_size";
    private static final String PREFERENCE_ANIMATIONS = "animations";
    private static final String PREFERENCE_GESTURES = "gestures";
    private static final String PREFERENCE_VOLUME_NAVIGATION = "volume_navigation";
    private static final String PREFERENCE_START_INTEGRATED_INBOX = "start_integrated_inbox";
    private static final String PREFERENCE_CONFIRM_ACTIONS = "confirm_actions";
    private static final String PREFERENCE_NOTIFICATION_HIDE_SUBJECT = "notification_hide_subject";
    private static final String PREFERENCE_MEASURE_ACCOUNTS = "measure_accounts";
    private static final String PREFERENCE_COUNT_SEARCH = "count_search";
    private static final String PREFERENCE_HIDE_SPECIAL_ACCOUNTS = "hide_special_accounts";
    private static final String PREFERENCE_MESSAGELIST_CHECKBOXES = "messagelist_checkboxes";
    private static final String PREFERENCE_MESSAGELIST_PREVIEW_LINES = "messagelist_preview_lines";
    private static final String PREFERENCE_MESSAGELIST_SENDER_ABOVE_SUBJECT = "messagelist_sender_above_subject";
    private static final String PREFERENCE_MESSAGELIST_STARS = "messagelist_stars";
    private static final String PREFERENCE_MESSAGELIST_SHOW_CORRESPONDENT_NAMES = "messagelist_show_correspondent_names";
    private static final String PREFERENCE_MESSAGELIST_SHOW_CONTACT_NAME = "messagelist_show_contact_name";
    private static final String PREFERENCE_MESSAGELIST_CONTACT_NAME_COLOR = "messagelist_contact_name_color";
    private static final String PREFERENCE_MESSAGELIST_SHOW_CONTACT_PICTURE = "messagelist_show_contact_picture";
    private static final String PREFERENCE_MESSAGELIST_COLORIZE_MISSING_CONTACT_PICTURES =
            "messagelist_colorize_missing_contact_pictures";
    private static final String PREFERENCE_MESSAGEVIEW_FIXEDWIDTH = "messageview_fixedwidth_font";
    private static final String PREFERENCE_MESSAGEVIEW_VISIBLE_REFILE_ACTIONS = "messageview_visible_refile_actions";

    private static final String PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST = "messageview_return_to_list";
    private static final String PREFERENCE_MESSAGEVIEW_SHOW_NEXT = "messageview_show_next";
    private static final String PREFERENCE_QUIET_TIME_ENABLED = "quiet_time_enabled";
    private static final String PREFERENCE_DISABLE_NOTIFICATION_DURING_QUIET_TIME =
            "disable_notifications_during_quiet_time";
    private static final String PREFERENCE_QUIET_TIME_STARTS = "quiet_time_starts";
    private static final String PREFERENCE_QUIET_TIME_ENDS = "quiet_time_ends";
    private static final String PREFERENCE_NOTIF_QUICK_DELETE = "notification_quick_delete";
    private static final String PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY = "lock_screen_notification_visibility";
    private static final String PREFERENCE_HIDE_USERAGENT = "privacy_hide_useragent";
    private static final String PREFERENCE_HIDE_TIMEZONE = "privacy_hide_timezone";

    private static final String PREFERENCE_OPENPGP_PROVIDER = "openpgp_provider";
    private static final String PREFERENCE_OPENPGP_SUPPORT_SIGN_ONLY = "openpgp_support_sign_only";

    private static final String PREFERENCE_AUTOFIT_WIDTH = "messageview_autofit_width";
    private static final String PREFERENCE_BACKGROUND_OPS = "background_ops";
    private static final String PREFERENCE_DEBUG_LOGGING = "debug_logging";
    private static final String PREFERENCE_SENSITIVE_LOGGING = "sensitive_logging";

    private static final String PREFERENCE_ATTACHMENT_DEF_PATH = "attachment_default_path";
    private static final String PREFERENCE_BACKGROUND_AS_UNREAD_INDICATOR = "messagelist_background_as_unread_indicator";
    private static final String PREFERENCE_THREADED_VIEW = "threaded_view";
    private static final String PREFERENCE_FOLDERLIST_WRAP_NAME = "folderlist_wrap_folder_name";
    private static final String PREFERENCE_SPLITVIEW_MODE = "splitview_mode";

    private static final String APG_PROVIDER_PLACEHOLDER = "apg-placeholder";

    private static final int ACTIVITY_CHOOSE_FOLDER = 1;

    private static final int DIALOG_APG_DEPRECATION_WARNING = 1;

    // Named indices for the visibleRefileActions field
    private static final int VISIBLE_REFILE_ACTIONS_DELETE = 0;
    private static final int VISIBLE_REFILE_ACTIONS_ARCHIVE = 1;
    private static final int VISIBLE_REFILE_ACTIONS_MOVE = 2;
    private static final int VISIBLE_REFILE_ACTIONS_COPY = 3;
    private static final int VISIBLE_REFILE_ACTIONS_SPAM = 4;

    private ListPreference language;
    private ListPreference theme;
    private CheckBoxPreference fixedMessageTheme;
    private ListPreference messageTheme;
    private ListPreference composerTheme;
    private CheckBoxPreference animations;
    private CheckBoxPreference gestures;
    private CheckBoxListPreference volumeNavigation;
    private CheckBoxPreference startIntegratedInbox;
    private CheckBoxListPreference confirmActions;
    private ListPreference notificationHideSubject;
    private CheckBoxPreference measureAccounts;
    private CheckBoxPreference countSearch;
    private CheckBoxPreference hideSpecialAccounts;
    private ListPreference previewLines;
    private CheckBoxPreference senderAboveSubject;
    private CheckBoxPreference checkboxes;
    private CheckBoxPreference stars;
    private CheckBoxPreference showCorrespondentNames;
    private CheckBoxPreference showContactName;
    private CheckBoxPreference changeContactNameColor;
    private CheckBoxPreference showContactPicture;
    private CheckBoxPreference colorizeMissingContactPictures;
    private CheckBoxPreference fixedWidth;
    private CheckBoxPreference returnToList;
    private CheckBoxPreference showNext;
    private CheckBoxPreference autofitWidth;
    private ListPreference backgroundOps;
    private CheckBoxPreference debugLogging;
    private CheckBoxPreference sensitiveLogging;
    private CheckBoxPreference hideUserAgent;
    private CheckBoxPreference hideTimeZone;
    private CheckBoxPreference wrapFolderNames;
    private CheckBoxListPreference visibleRefileActions;

    private OpenPgpAppPreference openPgpProvider;
    private CheckBoxPreference openPgpSupportSignOnly;

    private CheckBoxPreference quietTimeEnabled;
    private CheckBoxPreference disableNotificationDuringQuietTime;
    private com.fsck.k9.preferences.TimePickerPreference quietTimeStarts;
    private com.fsck.k9.preferences.TimePickerPreference quietTimeEnds;
    private ListPreference notificationQuickDelete;
    private ListPreference lockScreenNotificationVisibility;
    private Preference attachmentPathPreference;

    private CheckBoxPreference backgroundAsUnreadIndicator;
    private CheckBoxPreference threadedView;
    private ListPreference splitViewMode;


    public static void actionPrefs(Context context) {
        Intent i = new Intent(context, Prefs.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.global_preferences);

        language = (ListPreference) findPreference(PREFERENCE_LANGUAGE);
        List<CharSequence> entryVector = new ArrayList<CharSequence>(Arrays.asList(language.getEntries()));
        List<CharSequence> entryValueVector = new ArrayList<CharSequence>(Arrays.asList(language.getEntryValues()));
        String supportedLanguages[] = getResources().getStringArray(R.array.supported_languages);
        Set<String> supportedLanguageSet = new HashSet<String>(Arrays.asList(supportedLanguages));
        for (int i = entryVector.size() - 1; i > -1; --i) {
            if (!supportedLanguageSet.contains(entryValueVector.get(i))) {
                entryVector.remove(i);
                entryValueVector.remove(i);
            }
        }
        initListPreference(language, K9.getK9Language(),
                           entryVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY),
                           entryValueVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY));

        theme = setupListPreference(PREFERENCE_THEME, themeIdToName(K9.getK9Theme()));
        fixedMessageTheme = (CheckBoxPreference) findPreference(PREFERENCE_FIXED_MESSAGE_THEME);
        fixedMessageTheme.setChecked(K9.useFixedMessageViewTheme());
        messageTheme = setupListPreference(PREFERENCE_MESSAGE_VIEW_THEME,
                themeIdToName(K9.getK9MessageViewThemeSetting()));
        composerTheme = setupListPreference(PREFERENCE_COMPOSER_THEME,
                themeIdToName(K9.getK9ComposerThemeSetting()));

        findPreference(PREFERENCE_FONT_SIZE).setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onFontSizeSettings();
                return true;
            }
        });

        animations = (CheckBoxPreference)findPreference(PREFERENCE_ANIMATIONS);
        animations.setChecked(K9.showAnimations());

        gestures = (CheckBoxPreference)findPreference(PREFERENCE_GESTURES);
        gestures.setChecked(K9.gesturesEnabled());

        volumeNavigation = (CheckBoxListPreference)findPreference(PREFERENCE_VOLUME_NAVIGATION);
        volumeNavigation.setItems(new CharSequence[] {getString(R.string.volume_navigation_message), getString(R.string.volume_navigation_list)});
        volumeNavigation.setCheckedItems(new boolean[] {K9.useVolumeKeysForNavigationEnabled(), K9.useVolumeKeysForListNavigationEnabled()});

        startIntegratedInbox = (CheckBoxPreference)findPreference(PREFERENCE_START_INTEGRATED_INBOX);
        startIntegratedInbox.setChecked(K9.startIntegratedInbox());

        confirmActions = (CheckBoxListPreference) findPreference(PREFERENCE_CONFIRM_ACTIONS);

        boolean canDeleteFromNotification = NotificationController.platformSupportsExtendedNotifications();
        CharSequence[] confirmActionEntries = new CharSequence[canDeleteFromNotification ? 6 : 5];
        boolean[] confirmActionValues = new boolean[confirmActionEntries.length];
        int index = 0;

        confirmActionEntries[index] = getString(R.string.global_settings_confirm_action_delete);
        confirmActionValues[index++] = K9.confirmDelete();
        confirmActionEntries[index] = getString(R.string.global_settings_confirm_action_delete_starred);
        confirmActionValues[index++] = K9.confirmDeleteStarred();
        if (canDeleteFromNotification) {
            confirmActionEntries[index] = getString(R.string.global_settings_confirm_action_delete_notif);
            confirmActionValues[index++] = K9.confirmDeleteFromNotification();
        }
        confirmActionEntries[index] = getString(R.string.global_settings_confirm_action_spam);
        confirmActionValues[index++] = K9.confirmSpam();
        confirmActionEntries[index] = getString(R.string.global_settings_confirm_menu_discard);
        confirmActionValues[index++] = K9.confirmDiscardMessage();
        confirmActionEntries[index] = getString(R.string.global_settings_confirm_menu_mark_all_read);
        confirmActionValues[index++] = K9.confirmMarkAllRead();

        confirmActions.setItems(confirmActionEntries);
        confirmActions.setCheckedItems(confirmActionValues);

        notificationHideSubject = setupListPreference(PREFERENCE_NOTIFICATION_HIDE_SUBJECT,
                K9.getNotificationHideSubject().toString());

        measureAccounts = (CheckBoxPreference)findPreference(PREFERENCE_MEASURE_ACCOUNTS);
        measureAccounts.setChecked(K9.measureAccounts());

        countSearch = (CheckBoxPreference)findPreference(PREFERENCE_COUNT_SEARCH);
        countSearch.setChecked(K9.countSearchMessages());

        hideSpecialAccounts = (CheckBoxPreference)findPreference(PREFERENCE_HIDE_SPECIAL_ACCOUNTS);
        hideSpecialAccounts.setChecked(K9.isHideSpecialAccounts());


        previewLines = setupListPreference(PREFERENCE_MESSAGELIST_PREVIEW_LINES,
                                            Integer.toString(K9.messageListPreviewLines()));

        senderAboveSubject = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SENDER_ABOVE_SUBJECT);
        senderAboveSubject.setChecked(K9.messageListSenderAboveSubject());
        checkboxes = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_CHECKBOXES);
        checkboxes.setChecked(K9.messageListCheckboxes());

        stars = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_STARS);
        stars.setChecked(K9.messageListStars());

        showCorrespondentNames = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SHOW_CORRESPONDENT_NAMES);
        showCorrespondentNames.setChecked(K9.showCorrespondentNames());

        showContactName = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SHOW_CONTACT_NAME);
        showContactName.setChecked(K9.showContactName());

        showContactPicture = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SHOW_CONTACT_PICTURE);
        showContactPicture.setChecked(K9.showContactPicture());

        colorizeMissingContactPictures = (CheckBoxPreference)findPreference(
                PREFERENCE_MESSAGELIST_COLORIZE_MISSING_CONTACT_PICTURES);
        colorizeMissingContactPictures.setChecked(K9.isColorizeMissingContactPictures());

        backgroundAsUnreadIndicator = (CheckBoxPreference)findPreference(PREFERENCE_BACKGROUND_AS_UNREAD_INDICATOR);
        backgroundAsUnreadIndicator.setChecked(K9.useBackgroundAsUnreadIndicator());

        changeContactNameColor = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_CONTACT_NAME_COLOR);
        changeContactNameColor.setChecked(K9.changeContactNameColor());

        threadedView = (CheckBoxPreference) findPreference(PREFERENCE_THREADED_VIEW);
        threadedView.setChecked(K9.isThreadedViewEnabled());

        if (K9.changeContactNameColor()) {
            changeContactNameColor.setSummary(R.string.global_settings_registered_name_color_changed);
        } else {
            changeContactNameColor.setSummary(R.string.global_settings_registered_name_color_default);
        }
        changeContactNameColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean checked = (Boolean) newValue;
                if (checked) {
                    onChooseContactNameColor();
                    changeContactNameColor.setSummary(R.string.global_settings_registered_name_color_changed);
                } else {
                    changeContactNameColor.setSummary(R.string.global_settings_registered_name_color_default);
                }
                changeContactNameColor.setChecked(checked);
                return false;
            }
        });

        fixedWidth = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGEVIEW_FIXEDWIDTH);
        fixedWidth.setChecked(K9.messageViewFixedWidthFont());

        returnToList = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST);
        returnToList.setChecked(K9.messageViewReturnToList());

        showNext = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGEVIEW_SHOW_NEXT);
        showNext.setChecked(K9.messageViewShowNext());

        autofitWidth = (CheckBoxPreference) findPreference(PREFERENCE_AUTOFIT_WIDTH);
        autofitWidth.setChecked(K9.autofitWidth());

        quietTimeEnabled = (CheckBoxPreference) findPreference(PREFERENCE_QUIET_TIME_ENABLED);
        quietTimeEnabled.setChecked(K9.getQuietTimeEnabled());

        disableNotificationDuringQuietTime = (CheckBoxPreference) findPreference(
                PREFERENCE_DISABLE_NOTIFICATION_DURING_QUIET_TIME);
        disableNotificationDuringQuietTime.setChecked(!K9.isNotificationDuringQuietTimeEnabled());
        quietTimeStarts = (TimePickerPreference) findPreference(PREFERENCE_QUIET_TIME_STARTS);
        quietTimeStarts.setDefaultValue(K9.getQuietTimeStarts());
        quietTimeStarts.setSummary(K9.getQuietTimeStarts());
        quietTimeStarts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String time = (String) newValue;
                quietTimeStarts.setSummary(time);
                return false;
            }
        });

        quietTimeEnds = (TimePickerPreference) findPreference(PREFERENCE_QUIET_TIME_ENDS);
        quietTimeEnds.setSummary(K9.getQuietTimeEnds());
        quietTimeEnds.setDefaultValue(K9.getQuietTimeEnds());
        quietTimeEnds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String time = (String) newValue;
                quietTimeEnds.setSummary(time);
                return false;
            }
        });

        notificationQuickDelete = setupListPreference(PREFERENCE_NOTIF_QUICK_DELETE,
                K9.getNotificationQuickDeleteBehaviour().toString());
        if (!NotificationController.platformSupportsExtendedNotifications()) {
            PreferenceScreen prefs = (PreferenceScreen) findPreference("notification_preferences");
            prefs.removePreference(notificationQuickDelete);
            notificationQuickDelete = null;
        }

        lockScreenNotificationVisibility = setupListPreference(PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY,
            K9.getLockScreenNotificationVisibility().toString());
        if (!NotificationController.platformSupportsLockScreenNotifications()) {
            ((PreferenceScreen) findPreference("notification_preferences"))
                .removePreference(lockScreenNotificationVisibility);
            lockScreenNotificationVisibility = null;
        }

        backgroundOps = setupListPreference(PREFERENCE_BACKGROUND_OPS, K9.getBackgroundOps().name());

        debugLogging = (CheckBoxPreference)findPreference(PREFERENCE_DEBUG_LOGGING);
        sensitiveLogging = (CheckBoxPreference)findPreference(PREFERENCE_SENSITIVE_LOGGING);
        hideUserAgent = (CheckBoxPreference)findPreference(PREFERENCE_HIDE_USERAGENT);
        hideTimeZone = (CheckBoxPreference)findPreference(PREFERENCE_HIDE_TIMEZONE);

        debugLogging.setChecked(K9.isDebug());
        sensitiveLogging.setChecked(K9.DEBUG_SENSITIVE);
        hideUserAgent.setChecked(K9.hideUserAgent());
        hideTimeZone.setChecked(K9.hideTimeZone());

        openPgpProvider = (OpenPgpAppPreference) findPreference(PREFERENCE_OPENPGP_PROVIDER);
        openPgpProvider.setValue(K9.getOpenPgpProvider());
        if (OpenPgpAppPreference.isApgInstalled(getApplicationContext())) {
            openPgpProvider.addLegacyProvider(
                    APG_PROVIDER_PLACEHOLDER, getString(R.string.apg), R.drawable.ic_apg_small);
        }
        openPgpProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = newValue.toString();
                if (APG_PROVIDER_PLACEHOLDER.equals(value)) {
                    openPgpProvider.setValue("");
                    showDialog(DIALOG_APG_DEPRECATION_WARNING);
                } else {
                    openPgpProvider.setValue(value);
                }
                return false;
            }
        });

        openPgpSupportSignOnly = (CheckBoxPreference) findPreference(PREFERENCE_OPENPGP_SUPPORT_SIGN_ONLY);
        openPgpSupportSignOnly.setChecked(K9.getOpenPgpSupportSignOnly());

        attachmentPathPreference = findPreference(PREFERENCE_ATTACHMENT_DEF_PATH);
        attachmentPathPreference.setSummary(K9.getAttachmentDefaultPath());
        attachmentPathPreference
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FileBrowserHelper
                .getInstance()
                .showFileBrowserActivity(Prefs.this,
                                         new File(K9.getAttachmentDefaultPath()),
                                         ACTIVITY_CHOOSE_FOLDER, callback);

                return true;
            }

            FileBrowserFailOverCallback callback = new FileBrowserFailOverCallback() {

                @Override
                public void onPathEntered(String path) {
                    attachmentPathPreference.setSummary(path);
                    K9.setAttachmentDefaultPath(path);
                }

                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });

        wrapFolderNames = (CheckBoxPreference)findPreference(PREFERENCE_FOLDERLIST_WRAP_NAME);
        wrapFolderNames.setChecked(K9.wrapFolderNames());

        visibleRefileActions = (CheckBoxListPreference) findPreference(PREFERENCE_MESSAGEVIEW_VISIBLE_REFILE_ACTIONS);
        CharSequence[] visibleRefileActionsEntries = new CharSequence[5];
        visibleRefileActionsEntries[VISIBLE_REFILE_ACTIONS_DELETE] = getString(R.string.delete_action);
        visibleRefileActionsEntries[VISIBLE_REFILE_ACTIONS_ARCHIVE] = getString(R.string.archive_action);
        visibleRefileActionsEntries[VISIBLE_REFILE_ACTIONS_MOVE] = getString(R.string.move_action);
        visibleRefileActionsEntries[VISIBLE_REFILE_ACTIONS_COPY] = getString(R.string.copy_action);
        visibleRefileActionsEntries[VISIBLE_REFILE_ACTIONS_SPAM] = getString(R.string.spam_action);

        boolean[] visibleRefileActionsValues = new boolean[5];
        visibleRefileActionsValues[VISIBLE_REFILE_ACTIONS_DELETE] = K9.isMessageViewDeleteActionVisible();
        visibleRefileActionsValues[VISIBLE_REFILE_ACTIONS_ARCHIVE] = K9.isMessageViewArchiveActionVisible();
        visibleRefileActionsValues[VISIBLE_REFILE_ACTIONS_MOVE] = K9.isMessageViewMoveActionVisible();
        visibleRefileActionsValues[VISIBLE_REFILE_ACTIONS_COPY] = K9.isMessageViewCopyActionVisible();
        visibleRefileActionsValues[VISIBLE_REFILE_ACTIONS_SPAM] = K9.isMessageViewSpamActionVisible();

        visibleRefileActions.setItems(visibleRefileActionsEntries);
        visibleRefileActions.setCheckedItems(visibleRefileActionsValues);

        splitViewMode = (ListPreference) findPreference(PREFERENCE_SPLITVIEW_MODE);
        initListPreference(splitViewMode, K9.getSplitViewMode().name(),
                splitViewMode.getEntries(), splitViewMode.getEntryValues());
    }

    private static String themeIdToName(K9.Theme theme) {
        switch (theme) {
            case DARK: return "dark";
            case USE_GLOBAL: return "global";
            default: return "light";
        }
    }

    private static K9.Theme themeNameToId(String theme) {
        if (TextUtils.equals(theme, "dark")) {
            return K9.Theme.DARK;
        } else if (TextUtils.equals(theme, "global")) {
            return K9.Theme.USE_GLOBAL;
        } else {
            return K9.Theme.LIGHT;
        }
    }

    private void saveSettings() {
        Storage storage = Preferences.getPreferences(this).getStorage();

        K9.setK9Language(language.getValue());

        K9.setK9Theme(themeNameToId(theme.getValue()));
        K9.setUseFixedMessageViewTheme(fixedMessageTheme.isChecked());
        K9.setK9MessageViewThemeSetting(themeNameToId(messageTheme.getValue()));
        K9.setK9ComposerThemeSetting(themeNameToId(composerTheme.getValue()));

        K9.setAnimations(animations.isChecked());
        K9.setGesturesEnabled(gestures.isChecked());
        K9.setUseVolumeKeysForNavigation(volumeNavigation.getCheckedItems()[0]);
        K9.setUseVolumeKeysForListNavigation(volumeNavigation.getCheckedItems()[1]);
        K9.setStartIntegratedInbox(!hideSpecialAccounts.isChecked() && startIntegratedInbox.isChecked());
        K9.setNotificationHideSubject(NotificationHideSubject.valueOf(notificationHideSubject.getValue()));

        int index = 0;
        K9.setConfirmDelete(confirmActions.getCheckedItems()[index++]);
        K9.setConfirmDeleteStarred(confirmActions.getCheckedItems()[index++]);
        if (NotificationController.platformSupportsExtendedNotifications()) {
            K9.setConfirmDeleteFromNotification(confirmActions.getCheckedItems()[index++]);
        }
        K9.setConfirmSpam(confirmActions.getCheckedItems()[index++]);
        K9.setConfirmDiscardMessage(confirmActions.getCheckedItems()[index++]);
        K9.setConfirmMarkAllRead(confirmActions.getCheckedItems()[index++]);

        K9.setMeasureAccounts(measureAccounts.isChecked());
        K9.setCountSearchMessages(countSearch.isChecked());
        K9.setHideSpecialAccounts(hideSpecialAccounts.isChecked());
        K9.setMessageListPreviewLines(Integer.parseInt(previewLines.getValue()));
        K9.setMessageListCheckboxes(checkboxes.isChecked());
        K9.setMessageListStars(stars.isChecked());
        K9.setShowCorrespondentNames(showCorrespondentNames.isChecked());
        K9.setMessageListSenderAboveSubject(senderAboveSubject.isChecked());
        K9.setShowContactName(showContactName.isChecked());
        K9.setShowContactPicture(showContactPicture.isChecked());
        K9.setColorizeMissingContactPictures(colorizeMissingContactPictures.isChecked());
        K9.setUseBackgroundAsUnreadIndicator(backgroundAsUnreadIndicator.isChecked());
        K9.setThreadedViewEnabled(threadedView.isChecked());
        K9.setChangeContactNameColor(changeContactNameColor.isChecked());
        K9.setMessageViewFixedWidthFont(fixedWidth.isChecked());
        K9.setMessageViewReturnToList(returnToList.isChecked());
        K9.setMessageViewShowNext(showNext.isChecked());
        K9.setAutofitWidth(autofitWidth.isChecked());
        K9.setQuietTimeEnabled(quietTimeEnabled.isChecked());

        boolean[] enabledRefileActions = visibleRefileActions.getCheckedItems();
        K9.setMessageViewDeleteActionVisible(enabledRefileActions[VISIBLE_REFILE_ACTIONS_DELETE]);
        K9.setMessageViewArchiveActionVisible(enabledRefileActions[VISIBLE_REFILE_ACTIONS_ARCHIVE]);
        K9.setMessageViewMoveActionVisible(enabledRefileActions[VISIBLE_REFILE_ACTIONS_MOVE]);
        K9.setMessageViewCopyActionVisible(enabledRefileActions[VISIBLE_REFILE_ACTIONS_COPY]);
        K9.setMessageViewSpamActionVisible(enabledRefileActions[VISIBLE_REFILE_ACTIONS_SPAM]);

        K9.setNotificationDuringQuietTimeEnabled(!disableNotificationDuringQuietTime.isChecked());
        K9.setQuietTimeStarts(quietTimeStarts.getTime());
        K9.setQuietTimeEnds(quietTimeEnds.getTime());
        K9.setWrapFolderNames(wrapFolderNames.isChecked());

        if (notificationQuickDelete != null) {
            K9.setNotificationQuickDeleteBehaviour(
                    NotificationQuickDelete.valueOf(notificationQuickDelete.getValue()));
        }

        if(lockScreenNotificationVisibility != null) {
            K9.setLockScreenNotificationVisibility(
                K9.LockScreenNotificationVisibility.valueOf(lockScreenNotificationVisibility.getValue()));
        }

        K9.setSplitViewMode(SplitViewMode.valueOf(splitViewMode.getValue()));
        K9.setAttachmentDefaultPath(attachmentPathPreference.getSummary().toString());
        boolean needsRefresh = K9.setBackgroundOps(backgroundOps.getValue());

        if (!K9.isDebug() && debugLogging.isChecked()) {
            Toast.makeText(this, R.string.debug_logging_enabled, Toast.LENGTH_LONG).show();
        }
        K9.setDebug(debugLogging.isChecked());
        K9.DEBUG_SENSITIVE = sensitiveLogging.isChecked();
        K9.setHideUserAgent(hideUserAgent.isChecked());
        K9.setHideTimeZone(hideTimeZone.isChecked());

        K9.setOpenPgpProvider(openPgpProvider.getValue());
        K9.setOpenPgpSupportSignOnly(openPgpSupportSignOnly.isChecked());

        StorageEditor editor = storage.edit();
        K9.save(editor);
        editor.commit();

        if (needsRefresh) {
            MailService.actionReset(this, null);
        }
    }

    @Override
    protected void onPause() {
        saveSettings();
        super.onPause();
    }

    private void onFontSizeSettings() {
        FontSizeSettings.actionEditSettings(this);
    }

    private void onChooseContactNameColor() {
        new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
            public void colorChanged(int color) {
                K9.setContactNameColor(color);
            }
        },
        K9.getContactNameColor()).show();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_APG_DEPRECATION_WARNING: {
                dialog = new ApgDeprecationWarningDialog(this);
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        openPgpProvider.show();
                    }
                });
                break;
            }

        }
        return dialog;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACTIVITY_CHOOSE_FOLDER:
            if (resultCode == RESULT_OK && data != null) {
                // obtain the filename
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    String filePath = fileUri.getPath();
                    if (filePath != null) {
                        attachmentPathPreference.setSummary(filePath.toString());
                        K9.setAttachmentDefaultPath(filePath.toString());
                    }
                }
            }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
