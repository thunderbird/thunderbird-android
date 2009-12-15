package com.fsck.k9;

import android.app.ListActivity;
import android.os.Bundle;
import com.fsck.k9.activity.DateFormatter;


public class K9ListActivity extends ListActivity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        setTheme(K9.getK9Theme());
        super.onCreate(icicle);
        setupFormats();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setupFormats();
    }

    private java.text.DateFormat mDateFormat;
    private java.text.DateFormat mTimeFormat;

    private void setupFormats()
    {
        mDateFormat = DateFormatter.getDateFormat(this);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(this);   // 12/24 date format
    }

    public java.text.DateFormat getTimeFormat()
    {
        return mTimeFormat;
    }

    public java.text.DateFormat getDateFormat()
    {
        return mDateFormat;
    }
}
