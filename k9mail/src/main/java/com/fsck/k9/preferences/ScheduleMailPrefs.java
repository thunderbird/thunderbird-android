package com.fsck.k9.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.MenuItem;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.activity.MessageCompose;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScheduleMailPrefs extends K9PreferenceActivity {

    private static final String PREFERENCE_SCHEDULE_MAIL_ENABLED = "schedule_mail_enabled";
    private static final String PREFERENCE_SCHEDULE_MAIL_DATE = "schedule_mail_date";
    private static final String PREFERENCE_SCHEDULE_MAIL_TIME = "schedule_mail_time";

    private CheckBoxPreference scheduleMailEnabled;
    private DatePickerPreference scheduleMailDate;
    private TimePickerPreference scheduleMailTime;

    private Calendar sendDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.scheduled_mail_preferences);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        scheduleMailEnabled = (CheckBoxPreference) findPreference(PREFERENCE_SCHEDULE_MAIL_ENABLED);
        scheduleMailDate = (DatePickerPreference) findPreference(PREFERENCE_SCHEDULE_MAIL_DATE);
        scheduleMailTime = (TimePickerPreference) findPreference(PREFERENCE_SCHEDULE_MAIL_TIME);

        //Default schedule time is current time + 15 minutes
        sendDate = Calendar.getInstance();
        long currentSendDate = getIntent().getLongExtra(MessageCompose.EXTRA_SEND_DATE, 0L);
        if (currentSendDate != 0L && currentSendDate > System.currentTimeMillis()) {
            scheduleMailEnabled.setChecked(true);
            scheduleMailEnabled.notifyDependencyChange(false);
            sendDate.setTimeInMillis(currentSendDate);
        } else {
            scheduleMailEnabled.setChecked(false);
            scheduleMailEnabled.notifyDependencyChange(true);
            sendDate.setTimeInMillis(System.currentTimeMillis() + 900000);
        }

        scheduleMailDate.setDefaultValue(getDefaultScheduleDate());
        scheduleMailDate.setSummary(getDefaultScheduleDate());
        scheduleMailDate.setMinDateAsToday();
        scheduleMailTime.setDefaultValue(getDefaultScheduleTime());
        scheduleMailTime.setSummary(getDefaultScheduleTime());

        scheduleMailDate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                scheduleMailDate.copyDate(sendDate);
                scheduleMailDate.setSummary((String) newValue);
                return false;
            }
        });

        scheduleMailTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                scheduleMailTime.copyTime(sendDate);
                scheduleMailTime.setSummary((String) newValue);
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (checkSendDateValidity()) {
            setResult();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (checkSendDateValidity()) {
                    setResult();
                    finish();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkSendDateValidity() {
        if (sendDate.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, R.string.invalid_send_date, Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

    private void setResult() {
        if (scheduleMailEnabled.isChecked()) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(MessageCompose.EXTRA_SEND_DATE, sendDate.getTimeInMillis());
            setResult(RESULT_OK, returnIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
    }

    private String getDefaultScheduleDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        return sdf.format(sendDate.getTime());
    }

    private String getDefaultScheduleTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
        return sdf.format(sendDate.getTime());
    }
}
