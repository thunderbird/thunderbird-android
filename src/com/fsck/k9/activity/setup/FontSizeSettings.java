
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.*;
import android.view.KeyEvent;
import com.fsck.k9.*;

/**
 * Activity to configure the font size of the information displayed in the
 * account list, folder list, message list and in the message view.
 * 
 * @see FontSizes
 */
public class FontSizeSettings extends K9PreferenceActivity
{
    /*
     * Keys of the preferences defined in res/xml/font_preferences.xml 
     */
    private static final String PREFERENCE_ACCOUNT_NAME_FONT = "account_name_font";
    private static final String PREFERENCE_ACCOUNT_DESCRIPTION_FONT = "account_description_font";
    private static final String PREFERENCE_FOLDER_NAME_FONT = "folder_name_font";
    private static final String PREFERENCE_FOLDER_STATUS_FONT = "folder_status_font";
    private static final String PREFERENCE_MESSAGE_LIST_SUBJECT_FONT = "message_list_subject_font";
    private static final String PREFERENCE_MESSAGE_LIST_SENDER_FONT = "message_list_sender_font";
    private static final String PREFERENCE_MESSAGE_LIST_DATE_FONT = "message_list_date_font";
    private static final String PREFERENCE_MESSAGE_VIEW_SENDER_FONT = "message_view_sender_font";
    private static final String PREFERENCE_MESSAGE_VIEW_TO_FONT = "message_view_to_font";
    private static final String PREFERENCE_MESSAGE_VIEW_CC_FONT = "message_view_cc_font";
    private static final String PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT = "message_view_subject_font";
    private static final String PREFERENCE_MESSAGE_VIEW_TIME_FONT = "message_view_time_font";
    private static final String PREFERENCE_MESSAGE_VIEW_DATE_FONT = "message_view_date_font";
    private static final String PREFERENCE_MESSAGE_VIEW_CONTENT_FONT = "message_view_content_font";

    private ListPreference mAccountName;
    private ListPreference mAccountDescription;
    private ListPreference mFolderName;
    private ListPreference mFolderStatus;
    private ListPreference mMessageListSubject;
    private ListPreference mMessageListSender;
    private ListPreference mMessageListDate;
    private ListPreference mMessageViewSender;
    private ListPreference mMessageViewTo;
    private ListPreference mMessageViewCC;
    private ListPreference mMessageViewSubject;
    private ListPreference mMessageViewTime;
    private ListPreference mMessageViewDate;
    private ListPreference mMessageViewContent;


    /**
     * Start the FontSizeSettings activity.
     * 
     * @param context The application context.
     */
    public static void actionEditSettings(Context context)
    {
        Intent i = new Intent(context, FontSizeSettings.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FontSizes fontSizes = K9.getFontSizes();
        addPreferencesFromResource(R.xml.font_preferences);

        mAccountName = initializeListPreference(PREFERENCE_ACCOUNT_NAME_FONT, fontSizes.getAccountName());
        mAccountDescription = initializeListPreference(PREFERENCE_ACCOUNT_DESCRIPTION_FONT, fontSizes.getAccountDescription());

        mFolderName = initializeListPreference(PREFERENCE_FOLDER_NAME_FONT, fontSizes.getFolderName());
        mFolderStatus = initializeListPreference(PREFERENCE_FOLDER_STATUS_FONT, fontSizes.getFolderStatus());

        mMessageListSubject = initializeListPreference(PREFERENCE_MESSAGE_LIST_SUBJECT_FONT, fontSizes.getMessageListSubject());
        mMessageListSender = initializeListPreference(PREFERENCE_MESSAGE_LIST_SENDER_FONT, fontSizes.getMessageListSender());
        mMessageListDate = initializeListPreference(PREFERENCE_MESSAGE_LIST_DATE_FONT, fontSizes.getMessageListDate());

        mMessageViewSender = initializeListPreference(PREFERENCE_MESSAGE_VIEW_SENDER_FONT, fontSizes.getMessageViewSender());
        mMessageViewTo = initializeListPreference(PREFERENCE_MESSAGE_VIEW_TO_FONT, fontSizes.getMessageViewTo());
        mMessageViewCC = initializeListPreference(PREFERENCE_MESSAGE_VIEW_CC_FONT, fontSizes.getMessageViewCC());
        mMessageViewSubject = initializeListPreference(PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT, fontSizes.getMessageViewSubject());
        mMessageViewTime = initializeListPreference(PREFERENCE_MESSAGE_VIEW_TIME_FONT, fontSizes.getMessageViewTime());
        mMessageViewDate = initializeListPreference(PREFERENCE_MESSAGE_VIEW_DATE_FONT, fontSizes.getMessageViewDate());
        mMessageViewContent = initializeListPreference(PREFERENCE_MESSAGE_VIEW_CONTENT_FONT, fontSizes.getMessageViewContentAsInt());
    }

    /**
     * Update the global FontSize object and permanently store the (possibly
     * changed) font size settings.
     */
    private void saveSettings()
    {
        FontSizes fontSizes = K9.getFontSizes();

        fontSizes.setAccountName(Integer.parseInt(mAccountName.getValue()));
        fontSizes.setAccountDescription(Integer.parseInt(mAccountDescription.getValue()));

        fontSizes.setFolderName(Integer.parseInt(mFolderName.getValue()));
        fontSizes.setFolderStatus(Integer.parseInt(mFolderStatus.getValue()));

        fontSizes.setMessageListSubject(Integer.parseInt(mMessageListSubject.getValue()));
        fontSizes.setMessageListSender(Integer.parseInt(mMessageListSender.getValue()));
        fontSizes.setMessageListDate(Integer.parseInt(mMessageListDate.getValue()));

        fontSizes.setMessageViewSender(Integer.parseInt(mMessageViewSender.getValue()));
        fontSizes.setMessageViewTo(Integer.parseInt(mMessageViewTo.getValue()));
        fontSizes.setMessageViewCC(Integer.parseInt(mMessageViewCC.getValue()));
        fontSizes.setMessageViewSubject(Integer.parseInt(mMessageViewSubject.getValue()));
        fontSizes.setMessageViewTime(Integer.parseInt(mMessageViewTime.getValue()));
        fontSizes.setMessageViewDate(Integer.parseInt(mMessageViewDate.getValue()));
        fontSizes.setMessageViewContent(Integer.parseInt(mMessageViewContent.getValue()));

        SharedPreferences preferences = Preferences.getPreferences(this).getPreferences();
        Editor editor = preferences.edit();
        fontSizes.save(editor);
        editor.commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Set up the ListPreference instance identified by <code>key</code>.
     * 
     * @param key The key of the ListPreference object.
     * @param value Initial value for the ListPreference object.
     * @return The ListPreference instance identified by <code>key</code>. 
     */
    private ListPreference initializeListPreference(String key, int value)
    {
        ListPreference prefView = (ListPreference) findPreference(key);
        prefView.setValue(Integer.toString(value));
        prefView.setSummary(prefView.getEntry());
        prefView.setOnPreferenceChangeListener(new PreferenceChangeListener(prefView));
        return prefView;
    }

    /**
     * This class handles value changes of the ListPreference objects.  
     */
    private class PreferenceChangeListener implements Preference.OnPreferenceChangeListener
    {
        private ListPreference mPrefView;

        private PreferenceChangeListener(ListPreference prefView)
        {
            this.mPrefView = prefView;
        }

        /**
         * Show the preference value in the preference summary field. 
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            final String summary = newValue.toString();
            int index = mPrefView.findIndexOfValue(summary);
            mPrefView.setSummary(mPrefView.getEntries()[index]);
            mPrefView.setValue(summary);
            return false;
        }
    }
}
