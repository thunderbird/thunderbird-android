
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.*;
import android.view.KeyEvent;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9PreferenceActivity;

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
    private static final String PREFERENCE_MESSAGE_LIST_PREVIEW_FONT = "message_list_preview_font";
    private static final String PREFERENCE_MESSAGE_VIEW_SENDER_FONT = "message_view_sender_font";
    private static final String PREFERENCE_MESSAGE_VIEW_TO_FONT = "message_view_to_font";
    private static final String PREFERENCE_MESSAGE_VIEW_CC_FONT = "message_view_cc_font";
    private static final String PREFERENCE_MESSAGE_VIEW_ADDITIONAL_HEADERS_FONT = "message_view_additional_headers_font";
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
    private ListPreference mMessageListPreview;
    private ListPreference mMessageViewSender;
    private ListPreference mMessageViewTo;
    private ListPreference mMessageViewCC;
    private ListPreference mMessageViewAdditionalHeaders;
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

        mAccountName = setupListPreference(
                           PREFERENCE_ACCOUNT_NAME_FONT,
                           Integer.toString(fontSizes.getAccountName()));
        mAccountDescription = setupListPreference(
                                  PREFERENCE_ACCOUNT_DESCRIPTION_FONT,
                                  Integer.toString(fontSizes.getAccountDescription()));

        mFolderName = setupListPreference(
                          PREFERENCE_FOLDER_NAME_FONT,
                          Integer.toString(fontSizes.getFolderName()));
        mFolderStatus = setupListPreference(
                            PREFERENCE_FOLDER_STATUS_FONT,
                            Integer.toString(fontSizes.getFolderStatus()));

        mMessageListSubject = setupListPreference(
                                  PREFERENCE_MESSAGE_LIST_SUBJECT_FONT,
                                  Integer.toString(fontSizes.getMessageListSubject()));
        mMessageListSender = setupListPreference(
                                 PREFERENCE_MESSAGE_LIST_SENDER_FONT,
                                 Integer.toString(fontSizes.getMessageListSender()));
        mMessageListDate = setupListPreference(
                               PREFERENCE_MESSAGE_LIST_DATE_FONT,
                               Integer.toString(fontSizes.getMessageListDate()));
        mMessageListPreview = setupListPreference(
                                  PREFERENCE_MESSAGE_LIST_PREVIEW_FONT,
                                  Integer.toString(fontSizes.getMessageListPreview()));

        mMessageViewSender = setupListPreference(
                                 PREFERENCE_MESSAGE_VIEW_SENDER_FONT,
                                 Integer.toString(fontSizes.getMessageViewSender()));
        mMessageViewTo = setupListPreference(
                             PREFERENCE_MESSAGE_VIEW_TO_FONT,
                             Integer.toString(fontSizes.getMessageViewTo()));
        mMessageViewCC = setupListPreference(
                             PREFERENCE_MESSAGE_VIEW_CC_FONT,
                             Integer.toString(fontSizes.getMessageViewCC()));
        mMessageViewAdditionalHeaders = setupListPreference(
                                            PREFERENCE_MESSAGE_VIEW_ADDITIONAL_HEADERS_FONT,
                                            Integer.toString(fontSizes.getMessageViewAdditionalHeaders()));
        mMessageViewSubject = setupListPreference(
                                  PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT,
                                  Integer.toString(fontSizes.getMessageViewSubject()));
        mMessageViewTime = setupListPreference(
                               PREFERENCE_MESSAGE_VIEW_TIME_FONT,
                               Integer.toString(fontSizes.getMessageViewTime()));
        mMessageViewDate = setupListPreference(
                               PREFERENCE_MESSAGE_VIEW_DATE_FONT,
                               Integer.toString(fontSizes.getMessageViewDate()));
        mMessageViewContent = setupListPreference(
                                  PREFERENCE_MESSAGE_VIEW_CONTENT_FONT,
                                  Integer.toString(fontSizes.getMessageViewContentAsInt()));
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
        fontSizes.setMessageListPreview(Integer.parseInt(mMessageListPreview.getValue()));

        fontSizes.setMessageViewSender(Integer.parseInt(mMessageViewSender.getValue()));
        fontSizes.setMessageViewTo(Integer.parseInt(mMessageViewTo.getValue()));
        fontSizes.setMessageViewCC(Integer.parseInt(mMessageViewCC.getValue()));
        fontSizes.setMessageViewAdditionalHeaders(Integer.parseInt(mMessageViewAdditionalHeaders.getValue()));
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
}
