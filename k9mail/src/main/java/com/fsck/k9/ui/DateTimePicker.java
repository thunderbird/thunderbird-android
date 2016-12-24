package com.fsck.k9.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Shows a dialog to pick the date first, then the time.
 */
public class DateTimePicker {

    private final Context context;
    private final Calendar calendar;

    public interface OnDateTimePickedListener {
        void onDateTimePicked(long timestamp);
    }

    public DateTimePicker(Context context) {
        this.context = context;
        calendar = Calendar.getInstance();
    }

    // from http://stackoverflow.com/a/22217570/473201
    public void showDateTimePicker(final OnDateTimePickedListener listener) {

        DatePickerDialog dateDialog = new DatePickerDialog(
                context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        calendar.set(Calendar.YEAR, y);
                        calendar.set(Calendar.MONTH, m);
                        calendar.set(Calendar.DAY_OF_MONTH, d);

                        showTimePicker(listener);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dateDialog.show();
    }

    private void showTimePicker(final OnDateTimePickedListener listener) {
        TimePickerDialog timeDialog = new TimePickerDialog(
                context,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int h, int min) {
                        calendar.set(Calendar.HOUR_OF_DAY, h);
                        calendar.set(Calendar.MINUTE, min);

                        listener.onDateTimePicked(calendar.getTimeInMillis());
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);

        timeDialog.show();
    }
}
