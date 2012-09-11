package com.fsck.k9.activity;


import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import com.fsck.k9.K9;


public class K9Activity extends Activity {
    protected static final int BEZEL_SWIPE_THRESHOLD = 20;

    protected GestureDetector mGestureDetector;

    @Override
    public void onCreate(Bundle icicle) {
        setLanguage(this, K9.getK9Language());
        setTheme(K9.getK9ThemeResourceId());
        super.onCreate(icicle);
        setupFormats();
    }

    public static void setLanguage(Context context, String language) {
        Locale locale;
        if (language == null || language.equals("")) {
            locale = Locale.getDefault();
        } else if (language.length() == 5 && language.charAt(2) == '_') {
            // language is in the form: en_US
            locale = new Locale(language.substring(0, 2), language.substring(3));
        } else {
            locale = new Locale(language);
        }
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupFormats();
    }

    private java.text.DateFormat mTimeFormat;

    private void setupFormats() {
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(this);   // 12/24 date format
    }

    public java.text.DateFormat getTimeFormat() {
        return mTimeFormat;
    }

    /**
     * Called when a swipe from right to left is handled by {@link MyGestureDetector}.  See
     * {@link android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)}
     * for more information on the {@link MotionEvent}s being passed.
     * @param e1 First down motion event that started the fling.
     * @param e2 The move motion event that triggered the current onFling.
     */
    protected void onSwipeRightToLeft(final MotionEvent e1, final MotionEvent e2) {
    }

    /**
     * Called when a swipe from left to right is handled by {@link MyGestureDetector}.  See
     * {@link android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)}
     * for more information on the {@link MotionEvent}s being passed.
     * @param e1 First down motion event that started the fling.
     * @param e2 The move motion event that triggered the current onFling.
     */
    protected void onSwipeLeftToRight(final MotionEvent e1, final MotionEvent e2) {
    }

    protected Animation inFromRightAnimation() {
        return slideAnimation(0.0f, +1.0f);
    }

    protected Animation outToLeftAnimation() {
        return slideAnimation(0.0f, -1.0f);
    }

    private Animation slideAnimation(float right, float left) {

        Animation slide = new TranslateAnimation(
            Animation.RELATIVE_TO_PARENT,  right, Animation.RELATIVE_TO_PARENT,  left,
            Animation.RELATIVE_TO_PARENT,  0.0f, Animation.RELATIVE_TO_PARENT,   0.0f
        );
        slide.setDuration(125);
        slide.setFillBefore(true);
        slide.setInterpolator(new AccelerateInterpolator());
        return slide;
    }

    class MyGestureDetector extends SimpleOnGestureListener {
        private boolean gesturesEnabled = false;

        /**
         * Creates a new {@link android.view.GestureDetector.OnGestureListener}.  Enabled/disabled based upon
         * {@link com.fsck.k9.K9#gesturesEnabled()}}.
         */
        public MyGestureDetector() {
            super();
        }

        /**
         * Create a new {@link android.view.GestureDetector.OnGestureListener}.
         * @param gesturesEnabled Setting to <code>true</code> will enable gesture detection,
         * regardless of the system-wide gesture setting.
         */
        public MyGestureDetector(final boolean gesturesEnabled) {
            super();
            this.gesturesEnabled = gesturesEnabled;
        }

        private static final float SWIPE_MAX_OFF_PATH_DIP = 250f;
        private static final float SWIPE_THRESHOLD_VELOCITY_DIP = 325f;


        protected MotionEvent mLastOnDownEvent = null;

        @Override
        public boolean onDown(MotionEvent e) {
            mLastOnDownEvent = e;
            return super.onDown(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Do fling-detection if gestures are force-enabled or we have system-wide gestures enabled.
            if (gesturesEnabled || K9.gesturesEnabled()) {

                // Apparently sometimes e1 is null
                // Found a workaround here: http://stackoverflow.com/questions/4151385/
                if (e1 == null) {
                    e1 = mLastOnDownEvent;
                }

                // Make sure we avoid NullPointerExceptions
                if (e1 == null || e2 == null) {
                    return false;
                }

                // Calculate the minimum distance required for this to count as a swipe.
                // Convert the constant dips to pixels.
                final float mGestureScale = getResources().getDisplayMetrics().density;
                final int minVelocity = (int)(SWIPE_THRESHOLD_VELOCITY_DIP * mGestureScale + 0.5f);
                final int maxOffPath = (int)(SWIPE_MAX_OFF_PATH_DIP * mGestureScale + 0.5f);

                // Calculate how much was actually swiped.
                final float deltaX = e2.getX() - e1.getX();
                final float deltaY = e2.getY() - e1.getY();

                // Calculate the minimum distance required for this to be considered a swipe.
                final int minDistance = (int)Math.abs(deltaY * 4);

                if(K9.DEBUG) {
                    final boolean movedAcross = (Math.abs(deltaX) > Math.abs(deltaY * 4));
                    final boolean steadyHand = (Math.abs(deltaX / deltaY) > 2);
                    Log.d(K9.LOG_TAG, String.format("Old swipe algorithm: movedAcross=%s steadyHand=%s result=%s", movedAcross, steadyHand, movedAcross && steadyHand));
                    Log.d(K9.LOG_TAG, String.format("New swipe algorithm: deltaX=%.2f deltaY=%.2f minDistance=%d velocity=%.2f (min=%d)", deltaX, deltaY, minDistance, velocityX, minVelocity));
                }

                try {
                    if (Math.abs(deltaY) > maxOffPath) {
                        if(K9.DEBUG)
                            Log.d(K9.LOG_TAG, "New swipe algorithm: Swipe too far off horizontal path.");
                        return false;
                    }
                    if(Math.abs(velocityX) < minVelocity) {
                        if(K9.DEBUG)
                            Log.d(K9.LOG_TAG, "New swipe algorithm: Swipe too slow.");
                        return false;
                    }
                    // right to left swipe
                    if (deltaX < (minDistance * -1)) {
                        onSwipeRightToLeft(e1, e2);
                        if(K9.DEBUG)
                            Log.d(K9.LOG_TAG, "New swipe algorithm: Right to Left swipe OK.");
                    } else if (deltaX > minDistance) {
                        onSwipeLeftToRight(e1, e2);
                        if(K9.DEBUG)
                            Log.d(K9.LOG_TAG, "New swipe algorithm: Left to Right swipe OK.");
                    } else {
                        if(K9.DEBUG)
                            Log.d(K9.LOG_TAG, "New swipe algorithm: Swipe did not meet minimum distance requirements.");
                        return false;
                    }

                    // successful fling, cancel the 2nd event to prevent any other action from happening
                    // see http://code.google.com/p/android/issues/detail?id=8497
                    e2.setAction(MotionEvent.ACTION_CANCEL);
                } catch (Exception e) {
                    // nothing
                }
            }
            return false;
        }
    }

    public int getThemeBackgroundColor() {
        TypedArray array = getTheme().obtainStyledAttributes(new int[] {
            android.R.attr.colorBackground,
        });
        int backgroundColor = array.getColor(0, 0xFF00FF);
        array.recycle();
        return backgroundColor;
    }

}
