package com.fsck.k9.activity.setup;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.ColorPickerDialog;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.DateFormatter;
import com.fsck.k9.helper.FileBrowserHelper;
import com.fsck.k9.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.fsck.k9.preferences.CheckBoxListPreference;
import com.fsck.k9.preferences.TimePickerPreference;

import com.fsck.k9.service.MailService;
import com.fsck.k9.view.MessageWebView;


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
    private static final String PREFERENCE_FONT_SIZE = "font_size";
    private static final String PREFERENCE_DATE_FORMAT = "dateFormat";
    private static final String PREFERENCE_ANIMATIONS = "animations";
    private static final String PREFERENCE_GESTURES = "gestures";
    private static final String PREFERENCE_VOLUME_NAVIGATION = "volumeNavigation";
    private static final String PREFERENCE_START_INTEGRATED_INBOX = "start_integrated_inbox";
    private static final String PREFERENCE_CONFIRM_ACTIONS = "confirm_actions";
    private static final String PREFERENCE_NOTIFICATION_HIDE_SUBJECT = "notification_hide_subject";
    private static final String PREFERENCE_MEASURE_ACCOUNTS = "measure_accounts";
    private static final String PREFERENCE_COUNT_SEARCH = "count_search";
    private static final String PREFERENCE_HIDE_SPECIAL_ACCOUNTS = "hide_special_accounts";
    private static final String PREFERENCE_MESSAGELIST_CHECKBOXES = "messagelist_checkboxes";
    private static final String PREFERENCE_MESSAGELIST_PREVIEW_LINES = "messagelist_preview_lines";
    private static final String PREFERENCE_MESSAGELIST_SENDER_ABOVE_SUBJECT = "messagelist_sender_above_subject";
    private static final String PREFERENCE_MESSAGELIST_SHOW_CORRESPONDENT_NAMES = "messagelist_show_correspondent_names";
    private static final String PREFERENCE_MESSAGELIST_SHOW_CONTACT_NAME = "messagelist_show_contact_name";
    private static final String PREFERENCE_MESSAGELIST_CONTACT_NAME_COLOR = "messagelist_contact_name_color";
    private static final String PREFERENCE_MESSAGEVIEW_FIXEDWIDTH = "messageview_fixedwidth_font";

    private static final String PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST = "messageview_return_to_list";
    private static final String PREFERENCE_MESSAGEVIEW_SHOW_NEXT = "messageview_show_next";
    private static final String PREFERENCE_QUIET_TIME_ENABLED = "quiet_time_enabled";
    private static final String PREFERENCE_QUIET_TIME_STARTS = "quiet_time_starts";
    private static final String PREFERENCE_QUIET_TIME_ENDS = "quiet_time_ends";
    private static final String PREFERENCE_NOTIF_QUICK_DELETE = "notification_quick_delete";
    private static final String PREFERENCE_BATCH_BUTTONS_MARK_READ = "batch_buttons_mark_read";
    private static final String PREFERENCE_BATCH_BUTTONS_DELETE = "batch_buttons_delete";
    private static final String PREFERENCE_BATCH_BUTTONS_ARCHIVE = "batch_buttons_archive";
    private static final String PREFERENCE_BATCH_BUTTONS_MOVE = "batch_buttons_move";
    private static final String PREFERENCE_BATCH_BUTTONS_FLAG = "batch_buttons_flag";
    private static final String PREFERENCE_BATCH_BUTTONS_UNSELECT = "batch_buttons_unselect";

    private static final String PREFERENCE_MESSAGEVIEW_MOBILE_LAYOUT = "messageview_mobile_layout";
    private static final String PREFERENCE_BACKGROUND_OPS = "background_ops";
    private static final String PREFERENCE_GALLERY_BUG_WORKAROUND = "use_gallery_bug_workaround";
    private static final String PREFERENCE_DEBUG_LOGGING = "debug_logging";
    private static final String PREFERENCE_SENSITIVE_LOGGING = "sensitive_logging";

    private static final String PREFERENCE_ATTACHMENT_DEF_PATH = "attachment_default_path";
    private static final String PREFERENCE_BACKGROUND_AS_UNREAD_INDICATOR = "messagelist_background_as_unread_indicator";
    private static final String PREFERENCE_THREADED_VIEW = "threaded_view";
    private static final String PREFERENCE_FOLDERLIST_WRAP_NAME = "folderlist_wrap_folder_name";

    private static final int ACTIVITY_CHOOSE_FOLDER = 1;


    private ListPreference mLanguage;
    private ListPreference mTheme;
    private ListPreference mDateFormat;
    private CheckBoxPreference mAnimations;
    private CheckBoxPreference mGestures;
    private CheckBoxListPreference mVolumeNavigation;
    private CheckBoxPreference mStartIntegratedInbox;
    private CheckBoxListPreference mConfirmActions;
    private ListPreference mNotificationHideSubject;
    private CheckBoxPreference mMeasureAccounts;
    private CheckBoxPreference mCountSearch;
    private CheckBoxPreference mHideSpecialAccounts;
    private ListPreference mPreviewLines;
    private CheckBoxPreference mSenderAboveSubject;
    private CheckBoxPreference mCheckboxes;
    private CheckBoxPreference mShowCorrespondentNames;
    private CheckBoxPreference mShowContactName;
    private CheckBoxPreference mChangeContactNameColor;
    private CheckBoxPreference mFixedWidth;
    private CheckBoxPreference mReturnToList;
    private CheckBoxPreference mShowNext;
    private CheckBoxPreference mMobileOptimizedLayout;
    private ListPreference mBackgroundOps;
    private CheckBoxPreference mUseGalleryBugWorkaround;
    private CheckBoxPreference mDebugLogging;
    private CheckBoxPreference mSensitiveLogging;
    private CheckBoxPreference mWrapFolderNames;

    private CheckBoxPreference mQuietTimeEnabled;
    private com.fsck.k9.preferences.TimePickerPreference mQuietTimeStarts;
    private com.fsck.k9.preferences.TimePickerPreference mQuietTimeEnds;
    private ListPreference mNotificationQuickDelete;
    private Preference mAttachmentPathPreference;

    private CheckBoxPreference mBatchButtonsMarkRead;
    private CheckBoxPreference mBatchButtonsDelete;
    private CheckBoxPreference mBatchButtonsArchive;
    private CheckBoxPreference mBatchButtonsMove;
    private CheckBoxPreference mBatchButtonsFlag;
    private CheckBoxPreference mBatchButtonsUnselect;
    private CheckBoxPreference mBackgroundAsUnreadIndicator;
    private CheckBoxPreference mThreadedView;


    public static void actionPrefs(Context context) {
        Intent i = new Intent(context, Prefs.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.global_preferences);

        mLanguage = (ListPreference) findPreference(PREFERENCE_LANGUAGE);
        List<CharSequence> entryVector = new ArrayList<CharSequence>(Arrays.asList(mLanguage.getEntries()));
        List<CharSequence> entryValueVector = new ArrayList<CharSequence>(Arrays.asList(mLanguage.getEntryValues()));
        String supportedLanguages[] = getResources().getStringArray(R.array.supported_languages);
        HashSet<String> supportedLanguageSet = new HashSet<String>(Arrays.asList(supportedLanguages));
        for (int i = entryVector.size() - 1; i > -1; --i) {
            if (!supportedLanguageSet.contains(entryValueVector.get(i))) {
                entryVector.remove(i);
                entryValueVector.remove(i);
            }
        }
        initListPreference(mLanguage, K9.getK9Language(),
                           entryVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY),
                           entryValueVector.toArray(EMPTY_CHAR_SEQUENCE_ARRAY));

        final String theme = (K9.getK9Theme() == K9.THEME_DARK) ? "dark" : "light";
        mTheme = setupListPreference(PREFERENCE_THEME, theme);

        findPreference(PREFERENCE_FONT_SIZE).setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                onFontSizeSettings();
                return true;
            }
        });

        mDateFormat = (ListPreference) findPreference(PREFERENCE_DATE_FORMAT);
        String[] formats = DateFormatter.getFormats(this);
        CharSequence[] entries = new CharSequence[formats.length];
        CharSequence[] values = new CharSequence[formats.length];
        for (int i = 0 ; i < formats.length; i++) {
            String format = formats[i];
            entries[i] = DateFormatter.getSampleDate(this, format);
            values[i] = format;
        }
        initListPreference(mDateFormat, DateFormatter.getFormat(this), entries, values);

        mAnimations = (CheckBoxPreference)findPreference(PREFERENCE_ANIMATIONS);
        mAnimations.setChecked(K9.showAnimations());

        mGestures = (CheckBoxPreference)findPreference(PREFERENCE_GESTURES);
        mGestures.setChecked(K9.gesturesEnabled());

        mVolumeNavigation = (CheckBoxListPreference)findPreference(PREFERENCE_VOLUME_NAVIGATION);
        mVolumeNavigation.setItems(new CharSequence[] {getString(R.string.volume_navigation_message), getString(R.string.volume_navigation_list)});
        mVolumeNavigation.setCheckedItems(new boolean[] {K9.useVolumeKeysForNavigationEnabled(), K9.useVolumeKeysForListNavigationEnabled()});

        mStartIntegratedInbox = (CheckBoxPreference)findPreference(PREFERENCE_START_INTEGRATED_INBOX);
        mStartIntegratedInbox.setChecked(K9.startIntegratedInbox());

        mConfirmActions = (CheckBoxListPreference) findPreference(PREFERENCE_CONFIRM_ACTIONS);

        boolean canDeleteFromNotification = MessagingController.platformSupportsExtendedNotifications();
        CharSequence[] confirmActionEntries = new CharSequence[canDeleteFromNotification ? 4 : 3];
        boolean[] confirmActionValues = new boolean[canDeleteFromNotification ? 4 : 3];
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

        mConfirmActions.setItems(confirmActionEntries);
        mConfirmActions.setCheckedItems(confirmActionValues);

        mNotificationHideSubject = setupListPreference(PREFERENCE_NOTIFICATION_HIDE_SUBJECT,
                K9.getNotificationHideSubject().toString());

        mMeasureAccounts = (CheckBoxPreference)findPreference(PREFERENCE_MEASURE_ACCOUNTS);
        mMeasureAccounts.setChecked(K9.measureAccounts());

        mCountSearch = (CheckBoxPreference)findPreference(PREFERENCE_COUNT_SEARCH);
        mCountSearch.setChecked(K9.countSearchMessages());

        mHideSpecialAccounts = (CheckBoxPreference)findPreference(PREFERENCE_HIDE_SPECIAL_ACCOUNTS);
        mHideSpecialAccounts.setChecked(K9.isHideSpecialAccounts());


        mPreviewLines = setupListPreference(PREFERENCE_MESSAGELIST_PREVIEW_LINES,
                                            Integer.toString(K9.messageListPreviewLines()));

        mSenderAboveSubject = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SENDER_ABOVE_SUBJECT);
        mSenderAboveSubject.setChecked(K9.messageListSenderAboveSubject());
        mCheckboxes = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_CHECKBOXES);
        mCheckboxes.setChecked(K9.messageListCheckboxes());


        mShowCorrespondentNames = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SHOW_CORRESPONDENT_NAMES);
        mShowCorrespondentNames.setChecked(K9.showCorrespondentNames());

        mShowContactName = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_SHOW_CONTACT_NAME);
        mShowContactName.setChecked(K9.showContactName());

        mBackgroundAsUnreadIndicator = (CheckBoxPreference)findPreference(PREFERENCE_BACKGROUND_AS_UNREAD_INDICATOR);
        mBackgroundAsUnreadIndicator.setChecked(K9.useBackgroundAsUnreadIndicator());

        mChangeContactNameColor = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_CONTACT_NAME_COLOR);
        mChangeContactNameColor.setChecked(K9.changeContactNameColor());

        mThreadedView = (CheckBoxPreference) findPreference(PREFERENCE_THREADED_VIEW);
        mThreadedView.setChecked(K9.isThreadedViewEnabled());

        if (K9.changeContactNameColor()) {
            mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_changed);
        } else {
            mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_default);
        }
        mChangeContactNameColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean checked = (Boolean) newValue;
                if (checked) {
                    onChooseContactNameColor();
                    mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_changed);
                } else {
                    mChangeContactNameColor.setSummary(R.string.global_settings_registered_name_color_default);
                }
                mChangeContactNameColor.setChecked(checked);
                return false;
            }
        });

        mFixedWidth = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGEVIEW_FIXEDWIDTH);
        mFixedWidth.setChecked(K9.messageViewFixedWidthFont());

        mReturnToList = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST);
        mReturnToList.setChecked(K9.messageViewReturnToList());

        mShowNext = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGEVIEW_SHOW_NEXT);
        mShowNext.setChecked(K9.messageViewShowNext());

        mMobileOptimizedLayout = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGEVIEW_MOBILE_LAYOUT);
        if (!MessageWebView.isSingleColumnLayoutSupported()) {
            mMobileOptimizedLayout.setEnabled(false);
            mMobileOptimizedLayout.setChecked(false);
        } else {
            mMobileOptimizedLayout.setChecked(K9.mobileOptimizedLayout());
        }

        mQuietTimeEnabled = (CheckBoxPreference) findPreference(PREFERENCE_QUIET_TIME_ENABLED);
        mQuietTimeEnabled.setChecked(K9.getQuietTimeEnabled());

        mQuietTimeStarts = (TimePickerPreference) findPreference(PREFERENCE_QUIET_TIME_STARTS);
        mQuietTimeStarts.setDefaultValue(K9.getQuietTimeStarts());
        mQuietTimeStarts.setSummary(K9.getQuietTimeStarts());
        mQuietTimeStarts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String time = (String) newValue;
                mQuietTimeStarts.setSummary(time);
                return false;
            }
        });

        mQuietTimeEnds = (TimePickerPreference) findPreference(PREFERENCE_QUIET_TIME_ENDS);
        mQuietTimeEnds.setSummary(K9.getQuietTimeEnds());
        mQuietTimeEnds.setDefaultValue(K9.getQuietTimeEnds());
        mQuietTimeEnds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String time = (String) newValue;
                mQuietTimeEnds.setSummary(time);
                return false;
            }
        });

        mNotificationQuickDelete = setupListPreference(PREFERENCE_NOTIF_QUICK_DELETE,
                K9.getNotificationQuickDeleteBehaviour().toString());
        if (!MessagingController.platformSupportsExtendedNotifications()) {
            PreferenceScreen prefs = (PreferenceScreen) findPreference("notification_preferences");
            prefs.removePreference(mNotificationQuickDelete);
            mNotificationQuickDelete = null;
        }

        mBackgroundOps = setupListPreference(PREFERENCE_BACKGROUND_OPS, K9.getBackgroundOps().toString());
        // In ICS+ there is no 'background data' setting that apps can chose to ignore anymore. So
        // we hide that option for "Background Sync".
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            CharSequence[] oldEntries = mBackgroundOps.getEntries();
            CharSequence[] newEntries = new CharSequence[3];
            // Use "When 'Auto-sync' is checked" instead of "When 'Background data' & 'Auto-sync'
            // are checked" as description.
            newEntries[0] = getString(R.string.background_ops_auto_sync_only);
            newEntries[1] = oldEntries[2];
            newEntries[2] = oldEntries[3];

            CharSequence[] oldValues = mBackgroundOps.getEntryValues();
            CharSequence[] newValues = new CharSequence[3];
            newValues[0] = oldValues[1];
            newValues[1] = oldValues[2];
            newValues[2] = oldValues[3];

            mBackgroundOps.setEntries(newEntries);
            mBackgroundOps.setEntryValues(newValues);

            // Since ConnectivityManager.getBackgroundDataSetting() always returns 'true' on ICS+
            // we map WHEN_CHECKED to ALWAYS.
            if (K9.getBackgroundOps() == K9.BACKGROUND_OPS.WHEN_CHECKED) {
                mBackgroundOps.setValue(K9.BACKGROUND_OPS.ALWAYS.toString());
                mBackgroundOps.setSummary(mBackgroundOps.getEntry());
            }
        }

        mUseGalleryBugWorkaround = (CheckBoxPreference)findPreference(PREFERENCE_GALLERY_BUG_WORKAROUND);
        mUseGalleryBugWorkaround.setChecked(K9.useGalleryBugWorkaround());

        mDebugLogging = (CheckBoxPreference)findPreference(PREFERENCE_DEBUG_LOGGING);
        mSensitiveLogging = (CheckBoxPreference)findPreference(PREFERENCE_SENSITIVE_LOGGING);

        mDebugLogging.setChecked(K9.DEBUG);
        mSensitiveLogging.setChecked(K9.DEBUG_SENSITIVE);

        mAttachmentPathPreference = findPreference(PREFERENCE_ATTACHMENT_DEF_PATH);
        mAttachmentPathPreference.setSummary(K9.getAttachmentDefaultPath());
        mAttachmentPathPreference
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
                    mAttachmentPathPreference.setSummary(path);
                    K9.setAttachmentDefaultPath(path);
                }

                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });
        
        mWrapFolderNames = (CheckBoxPreference)findPreference(PREFERENCE_FOLDERLIST_WRAP_NAME);
        mWrapFolderNames.setChecked(K9.wrapFolderNames());

        mBatchButtonsMarkRead = (CheckBoxPreference)findPreference(PREFERENCE_BATCH_BUTTONS_MARK_READ);
        mBatchButtonsDelete = (CheckBoxPreference)findPreference(PREFERENCE_BATCH_BUTTONS_DELETE);
        mBatchButtonsArchive = (CheckBoxPreference)findPreference(PREFERENCE_BATCH_BUTTONS_ARCHIVE);
        mBatchButtonsMove = (CheckBoxPreference)findPreference(PREFERENCE_BATCH_BUTTONS_MOVE);
        mBatchButtonsFlag = (CheckBoxPreference)findPreference(PREFERENCE_BATCH_BUTTONS_FLAG);
        mBatchButtonsUnselect = (CheckBoxPreference)findPreference(PREFERENCE_BATCH_BUTTONS_UNSELECT);
        mBatchButtonsMarkRead.setChecked(K9.batchButtonsMarkRead());
        mBatchButtonsDelete.setChecked(K9.batchButtonsDelete());
        mBatchButtonsArchive.setChecked(K9.batchButtonsArchive());
        mBatchButtonsMove.setChecked(K9.batchButtonsMove());
        mBatchButtonsFlag.setChecked(K9.batchButtonsFlag());
        mBatchButtonsUnselect.setChecked(K9.batchButtonsUnselect());

        // If we don't have any accounts with an archive folder, then don't enable the preference.
        boolean hasArchiveFolder = false;
        for (final Account acct : Preferences.getPreferences(this).getAccounts()) {
            if (acct.hasArchiveFolder()) {
                hasArchiveFolder = true;
                break;
            }
        }
        if (!hasArchiveFolder) {
            mBatchButtonsArchive.setEnabled(false);
            mBatchButtonsArchive.setSummary(R.string.global_settings_archive_disabled_reason);
        }
    }

    private void saveSettings() {
        SharedPreferences preferences = Preferences.getPreferences(this).getPreferences();

        K9.setK9Language(mLanguage.getValue());

        int newTheme = mTheme.getValue().equals("dark") ? K9.THEME_DARK : K9.THEME_LIGHT;
        if (K9.getK9Theme() != newTheme) {
            // Reset the message view theme when the app theme changes
            K9.setK9MessageViewTheme(newTheme);
        }
        K9.setK9Theme(newTheme);

        K9.setAnimations(mAnimations.isChecked());
        K9.setGesturesEnabled(mGestures.isChecked());
        K9.setUseVolumeKeysForNavigation(mVolumeNavigation.getCheckedItems()[0]);
        K9.setUseVolumeKeysForListNavigation(mVolumeNavigation.getCheckedItems()[1]);
        K9.setStartIntegratedInbox(!mHideSpecialAccounts.isChecked() && mStartIntegratedInbox.isChecked());
        K9.setNotificationHideSubject(NotificationHideSubject.valueOf(mNotificationHideSubject.getValue()));

        int index = 0;
        K9.setConfirmDelete(mConfirmActions.getCheckedItems()[index++]);
        K9.setConfirmDeleteStarred(mConfirmActions.getCheckedItems()[index++]);
        if (MessagingController.platformSupportsExtendedNotifications()) {
            K9.setConfirmDeleteFromNotification(mConfirmActions.getCheckedItems()[index++]);
        }
        K9.setConfirmSpam(mConfirmActions.getCheckedItems()[index++]);

        K9.setMeasureAccounts(mMeasureAccounts.isChecked());
        K9.setCountSearchMessages(mCountSearch.isChecked());
        K9.setHideSpecialAccounts(mHideSpecialAccounts.isChecked());
        K9.setMessageListPreviewLines(Integer.parseInt(mPreviewLines.getValue()));
        K9.setMessageListCheckboxes(mCheckboxes.isChecked());
        K9.setShowCorrespondentNames(mShowCorrespondentNames.isChecked());
        K9.setMessageListSenderAboveSubject(mSenderAboveSubject.isChecked());
        K9.setShowContactName(mShowContactName.isChecked());
        K9.setUseBackgroundAsUnreadIndicator(mBackgroundAsUnreadIndicator.isChecked());
        K9.setThreadedViewEnabled(mThreadedView.isChecked());
        K9.setChangeContactNameColor(mChangeContactNameColor.isChecked());
        K9.setMessageViewFixedWidthFont(mFixedWidth.isChecked());
        K9.setMessageViewReturnToList(mReturnToList.isChecked());
        K9.setMessageViewShowNext(mShowNext.isChecked());
        K9.setMobileOptimizedLayout(mMobileOptimizedLayout.isChecked());
        K9.setQuietTimeEnabled(mQuietTimeEnabled.isChecked());

        K9.setQuietTimeStarts(mQuietTimeStarts.getTime());
        K9.setQuietTimeEnds(mQuietTimeEnds.getTime());
        K9.setWrapFolderNames(mWrapFolderNames.isChecked());

        if (mNotificationQuickDelete != null) {
            K9.setNotificationQuickDeleteBehaviour(
                    NotificationQuickDelete.valueOf(mNotificationQuickDelete.getValue()));
        }

        K9.setBatchButtonsMarkRead(mBatchButtonsMarkRead.isChecked());
        K9.setBatchButtonsDelete(mBatchButtonsDelete.isChecked());
        K9.setBatchButtonsArchive(mBatchButtonsArchive.isChecked());
        K9.setBatchButtonsMove(mBatchButtonsMove.isChecked());
        K9.setBatchButtonsFlag(mBatchButtonsFlag.isChecked());
        K9.setBatchButtonsUnselect(mBatchButtonsUnselect.isChecked());

        K9.setAttachmentDefaultPath(mAttachmentPathPreference.getSummary().toString());
        boolean needsRefresh = K9.setBackgroundOps(mBackgroundOps.getValue());
        K9.setUseGalleryBugWorkaround(mUseGalleryBugWorkaround.isChecked());

        if (!K9.DEBUG && mDebugLogging.isChecked()) {
            Toast.makeText(this, R.string.debug_logging_enabled, Toast.LENGTH_LONG).show();
        }
        K9.DEBUG = mDebugLogging.isChecked();
        K9.DEBUG_SENSITIVE = mSensitiveLogging.isChecked();

        Editor editor = preferences.edit();
        K9.save(editor);
        DateFormatter.setDateFormat(editor, mDateFormat.getValue());
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ACTIVITY_CHOOSE_FOLDER:
            if (resultCode == RESULT_OK && data != null) {
                // obtain the filename
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    String filePath = fileUri.getPath();
                    if (filePath != null) {
                        mAttachmentPathPreference.setSummary(filePath.toString());
                        K9.setAttachmentDefaultPath(filePath.toString());
                    }
                }
            }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
