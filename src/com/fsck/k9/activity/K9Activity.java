package com.fsck.k9.activity;


import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import com.fsck.k9.K9;


public class K9Activity extends Activity
{
    private GestureDetector gestureDetector;

    protected ScrollView mTopView;

    @Override
    public void onCreate(Bundle icicle)
    {
        onCreate(icicle, true);
    }

    public void onCreate(Bundle icicle, boolean useTheme)
    {
        setLanguage(this, K9.getK9Language());
        if (useTheme)
        {
            setTheme(K9.getK9Theme());
        }
        super.onCreate(icicle);
        setupFormats();

        // Gesture detection
        gestureDetector = new GestureDetector(new MyGestureDetector());

    }

    public static void setLanguage(Context context, String language)
    {
        Locale locale;
        if (language == null || language.equals(""))
        {
            locale = Locale.getDefault();
        }
        else if (language.length() == 5 && language.charAt(2) == '_')
        {
            // language is in the form: en_US
            locale = new Locale(language.substring(0, 2), language.substring(3));
        }
        else
        {
            locale = new Locale(language);
        }
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        super.dispatchTouchEvent(ev);
        return gestureDetector.onTouchEvent(ev);
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
    protected void onNext(boolean animate)
    {

    }
    protected void onPrevious(boolean animate)
    {
    }

    class MyGestureDetector extends SimpleOnGestureListener
    {

        private static final float SWIPE_MIN_DISTANCE_DIP = 130.0f;
        private static final float SWIPE_MAX_OFF_PATH_DIP = 250f;
        private static final float SWIPE_THRESHOLD_VELOCITY_DIP = 325f;

        @Override
        public boolean onDoubleTap(MotionEvent ev)
        {
            super.onDoubleTap(ev);
            if (mTopView != null)
            {
                int height = getResources().getDisplayMetrics().heightPixels;
                if (ev.getRawY() < (height/4))
                {
                    mTopView.fullScroll(View.FOCUS_UP);

                }
                else if (ev.getRawY() > (height - height/4))
                {
                    mTopView.fullScroll(View.FOCUS_DOWN);

                }
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if (K9.gesturesEnabled())
            {
                // Convert the dips to pixels
                final float mGestureScale = getResources().getDisplayMetrics().density;
                int min_distance = (int)(SWIPE_MIN_DISTANCE_DIP * mGestureScale + 0.5f);
                int min_velocity = (int)(SWIPE_THRESHOLD_VELOCITY_DIP * mGestureScale + 0.5f);
                int max_off_path = (int)(SWIPE_MAX_OFF_PATH_DIP * mGestureScale + 0.5f);


                try
                {
                    if (Math.abs(e1.getY() - e2.getY()) > max_off_path)
                        return false;
                    // right to left swipe
                    if (e1.getX() - e2.getX() > min_distance && Math.abs(velocityX) > min_velocity)
                    {
                        onNext(true);
                    }
                    else if (e2.getX() - e1.getX() > min_distance && Math.abs(velocityX) > min_velocity)
                    {
                        onPrevious(true);
                    }
                }
                catch (Exception e)
                {
                    // nothing
                }
            }
            return false;
        }
    }

}
