package com.fsck.k9.activity;

import android.app.ListActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;
import android.os.Bundle;
import com.fsck.k9.K9;


public class K9ListActivity extends ListActivity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        K9Activity.setLanguage(this, K9.getK9Language());
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        // Shortcuts that work no matter what is selected
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                ListView listView = getListView();
                if (K9.useVolumeKeysForNavigationEnabled())
                {

                    if (listView.getSelectedItemPosition() > 0)
                    {
                        listView.setSelection(listView.getSelectedItemPosition()-1);
                    }
                    return true;
                }
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                ListView listView = getListView();
                if (K9.useVolumeKeysForNavigationEnabled())
                {
                    if (listView.getSelectedItemPosition() < listView.getCount())
                    {
                        listView.setSelection(listView.getSelectedItemPosition()+1);
                    }
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForNavigationEnabled())
        {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                return true;
            }
        }
        return super.onKeyUp(keyCode,event);
    }
}
