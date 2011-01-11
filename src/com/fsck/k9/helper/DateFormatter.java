package com.fsck.k9.helper;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DateFormatter
{
    private DateFormatter()
    {
    }
    private final static Calendar SAMPLE_DATE = Calendar.getInstance();
    static
    {
        SAMPLE_DATE.set(SAMPLE_DATE.get(Calendar.YEAR), SAMPLE_DATE.getActualMaximum(Calendar.MONTH), SAMPLE_DATE.getActualMaximum(Calendar.DAY_OF_MONTH));
    }

    public static final String SHORT_FORMAT = "SHORT";
    public static final String MEDIUM_FORMAT = "MEDIUM";
    public static final String DEFAULT_FORMAT = SHORT_FORMAT;

    public static final String PREF_KEY = "dateFormat";

    private static volatile String sChosenFormat = null;

    public static String getSampleDate(Context context, String formatString)
    {
        java.text.DateFormat formatter = getDateFormat(context, formatString);
        return formatter.format(SAMPLE_DATE.getTime());
    }

    public static String[] getFormats(Context context)
    {
        return context.getResources().getStringArray(R.array.date_formats);
    }

    private static ThreadLocal<Map<String, DateFormat>> storedFormats = new ThreadLocal<Map<String, DateFormat>>()
    {
        @Override
        public synchronized Map<String, DateFormat> initialValue()
        {
            return new HashMap<String, DateFormat>();
        }
    };

    public static DateFormat getDateFormat(Context context, String formatString)
    {
        java.text.DateFormat dateFormat;

        if (SHORT_FORMAT.equals(formatString))
        {
            dateFormat = android.text.format.DateFormat.getDateFormat(context);
        }
        else if (MEDIUM_FORMAT.equals(formatString))
        {
            dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
        }
        else
        {
            Map<String, DateFormat> formatMap = storedFormats.get();
            dateFormat = formatMap.get(formatString);

            if (dateFormat == null)
            {
                dateFormat = new SimpleDateFormat(formatString);
                formatMap.put(formatString, dateFormat);
            }
        }
        return dateFormat;
    }

    public static void setDateFormat(Editor editor, String formatString)
    {
        sChosenFormat = formatString;
        editor.putString(PREF_KEY, formatString);
    }

    public static String getFormat(Context context)
    {
        if (sChosenFormat == null)
        {
            Preferences prefs = Preferences.getPreferences(context);
            sChosenFormat = prefs.getPreferences().getString(PREF_KEY, DEFAULT_FORMAT);
        }
        return sChosenFormat;
    }

    public static DateFormat getDateFormat(Context context)
    {
        String formatString = getFormat(context);
        return getDateFormat(context, formatString);
    }
}
