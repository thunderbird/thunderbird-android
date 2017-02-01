
package com.fsck.k9.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TimePicker;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.controller.SnoozeController;
import com.fsck.k9.ui.DateTimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.text.DateFormat;


public class ChooseSnooze extends K9ListActivity {
    public static final String EXTRA_SNOOZE_UNTIL = "com.fsck.k9.ChooseSnooze_EXTRA_SNOOZE_UNTIL";

    private static final long SNOOZE_TIMESTAMP_CHOOSE_CUSTOM_TIME = 0;

    private static final boolean DEBUG = false;

    Account mAccount;
    MessageReference mMessageReference;
    ArrayAdapter<SnoozeTime> mAdapter;

    private DateFormat mTimeFormatter;
    private DateFormat mTimeAndDayFormatter;
    Resources mRes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_content_simple);

        initFormatters();

        mRes = getResources();

        getListView().setFastScrollEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(ChooseFolder.EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMessageReference = intent.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mAdapter.addAll(getTimes());

        setListAdapter(mAdapter);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SnoozeTime time = mAdapter.getItem(position);

                if (time.timestamp == SNOOZE_TIMESTAMP_CHOOSE_CUSTOM_TIME) {
                    chooseCustomTimeFromPicker();
                    return;
                }

                finishWithResult(time.timestamp);
            }
        });
    }

    private void chooseCustomTimeFromPicker() {
        DateTimePicker picker = new DateTimePicker(this);
        picker.showDateTimePicker(new DateTimePicker.OnDateTimePickedListener() {
            @Override
            public void onDateTimePicked(long timestamp) {
                finishWithResult(timestamp);
            }
        });
    }

    private void initFormatters() {
        String timeFormat;
        if (android.text.format.DateFormat.is24HourFormat(this)) {
            timeFormat = "EEEE HH:mm";
        } else {
            timeFormat = "EEEE hh:mm a";
        }
        mTimeAndDayFormatter = new SimpleDateFormat(timeFormat, Locale.getDefault());
        mTimeFormatter = android.text.format.DateFormat.getTimeFormat(this);
    }

    private void finishWithResult(long timestamp) {
        Intent result = new Intent();
        result.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        result.putExtra(EXTRA_SNOOZE_UNTIL, timestamp);
        result.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        setResult(RESULT_OK, result);
        finish();
    }

    private List<SnoozeTime> getTimes() {
        List<SnoozeTime> times = new LinkedList<>();

        long now = System.currentTimeMillis();
        Calendar cal = new GregorianCalendar();
        if (DEBUG) {
            times.add(new SnoozeTime(now + TimeUnit.MINUTES.toMillis(1)));
        }

        times.add(new SnoozeTime(now + TimeUnit.HOURS.toMillis(1)));

        // tomorrow
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, 9);

        times.add(new SnoozeTime(formatTimeLabel(cal, R.string.time_tomorrow_morning), cal.getTimeInMillis()));

        cal.set(Calendar.HOUR_OF_DAY, 13);
        times.add(new SnoozeTime(formatTimeLabel(cal, R.string.time_tomorrow_afternoon), cal.getTimeInMillis()));

        cal.set(Calendar.HOUR_OF_DAY, 20);
        times.add(new SnoozeTime(formatTimeLabel(cal, R.string.time_tomorrow_night), cal.getTimeInMillis()));

        // next monday morning
        cal.setTimeInMillis(now);
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);

        times.add(new SnoozeTime(formatTimeLabel(cal, R.string.time_next_week), cal.getTimeInMillis()));

        // TODO(tf): add most recently chosen custom times

        times.add(new SnoozeTime(mRes.getString(R.string.pick_a_time), SNOOZE_TIMESTAMP_CHOOSE_CUSTOM_TIME));

        return times;
    }

    private CharSequence formatTimeLabel(Calendar cal, int strRes) {

        String label = mRes.getString(strRes);

        GregorianCalendar tomorrowEOD = new GregorianCalendar();
        tomorrowEOD.add(Calendar.DAY_OF_YEAR, 2);
        tomorrowEOD.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowEOD.set(Calendar.MINUTE, 0);
        tomorrowEOD.set(Calendar.SECOND, 0);

        String time;
        if (cal.before(tomorrowEOD)) {
            time = mTimeFormatter.format(cal.getTime());
        } else {
            time = mTimeAndDayFormatter.format(cal.getTime());
        }
        return String.format(label, time);
    }

    private static class SnoozeTime {
        public final long timestamp;
        public final CharSequence label;

        private SnoozeTime(@NonNull CharSequence label, long timestamp) {
            this.timestamp = timestamp;
            this.label = label;
        }

        private SnoozeTime(long timestamp) {
            this.timestamp = timestamp;
            this.label = SnoozeController.getSnoozeMessage(timestamp);
        }

        @Override
        public String toString() {
            return label.toString();
        }
    }
}
