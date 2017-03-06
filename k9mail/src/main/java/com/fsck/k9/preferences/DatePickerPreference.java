package com.fsck.k9.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import timber.log.Timber;

/**
 * A preference type that allows a user to choose a date
 */
public class DatePickerPreference extends DialogPreference implements
        DatePicker.OnDateChangedListener {

    private DatePicker datePicker;
    private Calendar minDate;
    /**
     * The validation expression for this preference
     */
    public static final String VALIDATION_EXPRESSION = "[0-3]*[0-9] [A-Z][a-z]*[a-z] [1-9]*[1-9]";

    /**
     * The default value for this preference
     */
    private String defaultValue;
    /**
     * Store the original values, in case the user
     * chooses to abort the {@link DialogPreference}
     * after making a change.
     */
    private int originalDay = 0;
    private String originalMonth = null;
    private int originalYear = 0;

    /**
     * @param context
     * @param attrs
     */
    public DatePickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public DatePickerPreference(final Context context, final AttributeSet attrs,
                                final int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Initialize this preference
     */
    private void initialize() {
        setPersistent(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.preference.DialogPreference#onCreateDialogView()
     */
    @Override
    protected View onCreateDialogView() {
        datePicker = new DatePicker(getContext());
        Calendar now = Calendar.getInstance();
        datePicker.init(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), this);
        originalDay = getDay();
        originalMonth = getMonth();
        originalYear = getYear();
        if (originalDay >= 0 && originalMonth != null && originalYear >= 0) {
            datePicker.updateDate(originalYear, getMonthAsInt(originalMonth), originalDay);
        }
        if (minDate != null) {
            datePicker.setMinDate(minDate.getTimeInMillis());
        }
        return datePicker;
    }

    public void setMinDateAsToday() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, now.getMinimum(Calendar.HOUR_OF_DAY));
        now.set(Calendar.MINUTE, now.getMinimum(Calendar.MINUTE));
        now.set(Calendar.SECOND, now.getMinimum(Calendar.SECOND));
        now.set(Calendar.MILLISECOND, now.getMinimum(Calendar.MILLISECOND));
        minDate = now;
    }

    /**
     * @see
     * android.widget.DatePicker.OnDateChangedListener#onDateChanged(android
     * .widget.DatePicker, int, int)
     */

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        persistString(String.format(Locale.US, "%02d %s %04d", dayOfMonth, getMonthAsString(monthOfYear), year));
        callChangeListener(String.format(Locale.US, "%02d %s %04d", dayOfMonth, getMonthAsString(monthOfYear), year));
    }

    /**
     * If not a positive result, restore the original value
     * before going to super.onDialogClosed(positiveResult).
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {

        if (!positiveResult) {
            persistString(String.format(Locale.US, "%02d %s %04d", originalDay, originalMonth, originalYear));
            callChangeListener(String.format(Locale.US, "%02d %s %04d", originalDay, originalMonth, originalYear));
        }
        super.onDialogClosed(positiveResult);
    }

    /**
     * @see android.preference.Preference#setDefaultValue(java.lang.Object)
     */
    @Override
    public void setDefaultValue(final Object defaultValue) {

        super.setDefaultValue(defaultValue);

        if (!(defaultValue instanceof String)) {
            return;
        }

        if (!((String) defaultValue).matches(VALIDATION_EXPRESSION)) {
            return;
        }

        this.defaultValue = (String) defaultValue;
    }

    /**
     * Get the day value
     *
     * @return The day value, will be 1 to 31 (inclusive) or -1 if illegal
     */
    private int getDay() {
        String date = getDate();
        if (date == null) {
            return -1;
        }

        return Integer.parseInt(date.split(" ")[0]);
    }

    /**
     * Get the month value
     *
     * @return the month value, will be the month's full name or null if illegal
     */
    private String getMonth() {
        String date = getDate();
        if (date == null) {
            return null;
        }

        return date.split(" ")[1];
    }

    /**
     * Get the year value
     *
     * @return the year value, will be a four digit number (inclusive) or -1 if illegal
     */
    private int getYear() {
        String date = getDate();
        if (date == null) {
            return -1;
        }

        return Integer.parseInt(date.split(" ")[2]);
    }

    /**
     * Get the date. It is only legal, if it matches
     * {@link #VALIDATION_EXPRESSION}.
     *
     * @return the date as dd MMMM yyyy
     */
    public String getDate() {
        return getPersistedString(this.defaultValue);
    }

    private int getMonthAsInt(String monthString) {
        if (monthString == null) {
            return -1;
        }
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(new SimpleDateFormat("MMMM").parse(monthString));
            return cal.get(Calendar.MONTH);
        }
        catch (ParseException pe) {
            Timber.e(pe, "Failed to parse date in DatePickerPreference");
            return -1;
        }
    }

    private String getMonthAsString(int monthInt) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, monthInt);
        return new SimpleDateFormat("MMMM").format(cal.getTime());
    }

    public void copyDate(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, getDay());
        calendar.set(Calendar.MONTH, getMonthAsInt(getMonth()));
        calendar.set(Calendar.YEAR, getYear());
    }
}