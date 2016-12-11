
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

        String timeFormat;
        if (android.text.format.DateFormat.is24HourFormat(this)) {
            timeFormat = "EEEE HH:mm";
        } else {
            timeFormat = "EEEE hh:mm a";
        }
        mTimeAndDayFormatter = new SimpleDateFormat(timeFormat, Locale.getDefault());
        mTimeFormatter = android.text.format.DateFormat.getTimeFormat(this);
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

                if (time.timestamp <= 0) {
                    // choose custom time from picker
                    showDateTimePicker(new OnDateTimePickedListener() {
                        @Override
                        public void onDateTimePicked(long timestamp) {
                            finishWithResult(timestamp);
                        }
                    });
                    return;
                }

                finishWithResult(time.timestamp);
            }
        });
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
        int monday = Calendar.MONDAY;
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.set(Calendar.DAY_OF_WEEK, monday);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);

        times.add(new SnoozeTime(formatTimeLabel(cal, R.string.time_next_week), cal.getTimeInMillis()));

        // TODO(tf): add most recently chosen custom times

        times.add(new SnoozeTime(mRes.getString(R.string.pick_a_time), 0));

        return times;
    }

    private CharSequence formatTimeLabel(Calendar cal, int strRes) {

        String label = mRes.getString(strRes);

        GregorianCalendar tomorrowEOD = new GregorianCalendar();
        tomorrowEOD.add(Calendar.DAY_OF_YEAR, 1);
        tomorrowEOD.set(Calendar.HOUR_OF_DAY, 23);
        tomorrowEOD.set(Calendar.MINUTE, 59);

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


    interface OnDateTimePickedListener {
        void onDateTimePicked(long timestamp);
    }

    // from http://stackoverflow.com/a/22217570/473201
    private void showDateTimePicker(final OnDateTimePickedListener listener) {

        final Calendar cal = Calendar.getInstance();
        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        cal.set(Calendar.YEAR, y);
                        cal.set(Calendar.MONTH, m);
                        cal.set(Calendar.DAY_OF_MONTH, d);

                        // now show the time picker
                        TimePickerDialog timeDialog = new TimePickerDialog(
                                ChooseSnooze.this,
                                new TimePickerDialog.OnTimeSetListener() {

                                    @Override
                                    public void onTimeSet(TimePicker view, int h, int min) {
                                        cal.set(Calendar.HOUR_OF_DAY, h);
                                        cal.set(Calendar.MINUTE, min);

                                        listener.onDateTimePicked(cal.getTimeInMillis());
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true);

                        timeDialog.show();
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        dateDialog.show();
    }
}
