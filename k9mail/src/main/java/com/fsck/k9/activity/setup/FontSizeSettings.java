package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.*;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;


/**
 * Activity to configure the font size of the information displayed in the
 * account list, folder list, message list and in the message view.
 *
 * @see FontSizes
 */
public class FontSizeSettings extends K9PreferenceActivity {
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
    private static final String PREFERENCE_MESSAGE_VIEW_BCC_FONT = "message_view_bcc_font";
    private static final String PREFERENCE_MESSAGE_VIEW_ADDITIONAL_HEADERS_FONT = "message_view_additional_headers_font";
    private static final String PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT = "message_view_subject_font";
    private static final String PREFERENCE_MESSAGE_VIEW_DATE_FONT = "message_view_date_font";
    private static final String PREFERENCE_MESSAGE_VIEW_CONTENT_FONT_SLIDER = "message_view_content_font_slider";
    private static final String PREFERENCE_MESSAGE_COMPOSE_INPUT_FONT = "message_compose_input_font";

    private ListPreference accountName;
    private ListPreference accountDescription;
    private ListPreference folderName;
    private ListPreference folderStatus;
    private ListPreference messageListSubject;
    private ListPreference messageListSender;
    private ListPreference messageListDate;
    private ListPreference messageListPreview;
    private ListPreference messageViewSender;
    private ListPreference messageViewTo;
    private ListPreference messageViewCC;
    private ListPreference messageViewBCC;
    private ListPreference messageViewAdditionalHeaders;
    private ListPreference messageViewSubject;
    private ListPreference messageViewDate;
    private SliderPreference messageViewContentSlider;
    private ListPreference messageComposeInput;

    private static final int FONT_PERCENT_MIN = 40;
    private static final int FONT_PERCENT_MAX = 250;

    /**
     * Start the FontSizeSettings activity.
     *
     * @param context The application context.
     */
    public static void actionEditSettings(Context context) {
        Intent i = new Intent(context, FontSizeSettings.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FontSizes fontSizes = K9.getFontSizes();
        addPreferencesFromResource(R.xml.font_preferences);

        accountName = setupListPreference(
                           PREFERENCE_ACCOUNT_NAME_FONT,
                           Integer.toString(fontSizes.getAccountName()));
        accountDescription = setupListPreference(
                                  PREFERENCE_ACCOUNT_DESCRIPTION_FONT,
                                  Integer.toString(fontSizes.getAccountDescription()));

        folderName = setupListPreference(
                          PREFERENCE_FOLDER_NAME_FONT,
                          Integer.toString(fontSizes.getFolderName()));
        folderStatus = setupListPreference(
                            PREFERENCE_FOLDER_STATUS_FONT,
                            Integer.toString(fontSizes.getFolderStatus()));

        messageListSubject = setupListPreference(
                                  PREFERENCE_MESSAGE_LIST_SUBJECT_FONT,
                                  Integer.toString(fontSizes.getMessageListSubject()));
        messageListSender = setupListPreference(
                                 PREFERENCE_MESSAGE_LIST_SENDER_FONT,
                                 Integer.toString(fontSizes.getMessageListSender()));
        messageListDate = setupListPreference(
                               PREFERENCE_MESSAGE_LIST_DATE_FONT,
                               Integer.toString(fontSizes.getMessageListDate()));
        messageListPreview = setupListPreference(
                                  PREFERENCE_MESSAGE_LIST_PREVIEW_FONT,
                                  Integer.toString(fontSizes.getMessageListPreview()));

        messageViewSender = setupListPreference(
                                 PREFERENCE_MESSAGE_VIEW_SENDER_FONT,
                                 Integer.toString(fontSizes.getMessageViewSender()));
        messageViewTo = setupListPreference(
                             PREFERENCE_MESSAGE_VIEW_TO_FONT,
                             Integer.toString(fontSizes.getMessageViewTo()));
        messageViewCC = setupListPreference(
                             PREFERENCE_MESSAGE_VIEW_CC_FONT,
                             Integer.toString(fontSizes.getMessageViewCC()));
        messageViewBCC = setupListPreference(
                             PREFERENCE_MESSAGE_VIEW_BCC_FONT,
                             Integer.toString(fontSizes.getMessageViewBCC()));
        messageViewAdditionalHeaders = setupListPreference(
                                            PREFERENCE_MESSAGE_VIEW_ADDITIONAL_HEADERS_FONT,
                                            Integer.toString(fontSizes.getMessageViewAdditionalHeaders()));
        messageViewSubject = setupListPreference(
                                  PREFERENCE_MESSAGE_VIEW_SUBJECT_FONT,
                                  Integer.toString(fontSizes.getMessageViewSubject()));
        messageViewDate = setupListPreference(
                               PREFERENCE_MESSAGE_VIEW_DATE_FONT,
                               Integer.toString(fontSizes.getMessageViewDate()));

        messageViewContentSlider = (SliderPreference) findPreference(
                                  PREFERENCE_MESSAGE_VIEW_CONTENT_FONT_SLIDER);

        final String summaryFormat = getString(R.string.font_size_message_view_content_summary);
        final String titleFormat = getString(R.string.font_size_message_view_content_dialog_title);
        messageViewContentSlider.setValue(scaleFromInt(fontSizes.getMessageViewContentAsPercent()));
        messageViewContentSlider.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                // Show the preference value in the preference summary field.
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    final SliderPreference slider = (SliderPreference) preference;
                    final Float value = (Float) newValue;
                    slider.setSummary(String.format(summaryFormat, scaleToInt(value)));
                    slider.setDialogTitle(
                            String.format(titleFormat, slider.getTitle(), slider.getSummary()));
                    if (slider.getDialog() != null) {
                        slider.getDialog().setTitle(slider.getDialogTitle());
                    }
                    return true;
                }
            }
        );
        messageViewContentSlider.getOnPreferenceChangeListener().onPreferenceChange(
                messageViewContentSlider, messageViewContentSlider.getValue());

        messageComposeInput = setupListPreference(
                PREFERENCE_MESSAGE_COMPOSE_INPUT_FONT,
                Integer.toString(fontSizes.getMessageComposeInput()));
    }

    /**
     * Update the global FontSize object and permanently store the (possibly
     * changed) font size settings.
     */
    private void saveSettings() {
        FontSizes fontSizes = K9.getFontSizes();

        fontSizes.setAccountName(Integer.parseInt(accountName.getValue()));
        fontSizes.setAccountDescription(Integer.parseInt(accountDescription.getValue()));

        fontSizes.setFolderName(Integer.parseInt(folderName.getValue()));
        fontSizes.setFolderStatus(Integer.parseInt(folderStatus.getValue()));

        fontSizes.setMessageListSubject(Integer.parseInt(messageListSubject.getValue()));
        fontSizes.setMessageListSender(Integer.parseInt(messageListSender.getValue()));
        fontSizes.setMessageListDate(Integer.parseInt(messageListDate.getValue()));
        fontSizes.setMessageListPreview(Integer.parseInt(messageListPreview.getValue()));

        fontSizes.setMessageViewSender(Integer.parseInt(messageViewSender.getValue()));
        fontSizes.setMessageViewTo(Integer.parseInt(messageViewTo.getValue()));
        fontSizes.setMessageViewCC(Integer.parseInt(messageViewCC.getValue()));
        fontSizes.setMessageViewBCC(Integer.parseInt(messageViewBCC.getValue()));
        fontSizes.setMessageViewAdditionalHeaders(Integer.parseInt(messageViewAdditionalHeaders.getValue()));
        fontSizes.setMessageViewSubject(Integer.parseInt(messageViewSubject.getValue()));
        fontSizes.setMessageViewDate(Integer.parseInt(messageViewDate.getValue()));
        fontSizes.setMessageViewContentAsPercent(scaleToInt(messageViewContentSlider.getValue()));

        fontSizes.setMessageComposeInput(Integer.parseInt(messageComposeInput.getValue()));

        Storage storage = Preferences.getPreferences(this).getStorage();
        StorageEditor editor = storage.edit();
        fontSizes.save(editor);
        editor.commit();
    }
    
    private int scaleToInt(float sliderValue) {
        return (int) (FONT_PERCENT_MIN + sliderValue * (FONT_PERCENT_MAX - FONT_PERCENT_MIN));
    }

    private float scaleFromInt(int value) {
        return (float) (value - FONT_PERCENT_MIN) / (FONT_PERCENT_MAX - FONT_PERCENT_MIN);
    }

    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }
}
