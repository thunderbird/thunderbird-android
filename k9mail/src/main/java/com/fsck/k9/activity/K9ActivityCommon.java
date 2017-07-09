package com.fsck.k9.activity;


import android.app.Activity;
import android.content.res.TypedArray;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.fsck.k9.K9;
import com.fsck.k9.activity.misc.SwipeGestureDetector;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;


/**
 * This class implements functionality common to most activities used in K-9 Mail.
 *
 * @see K9Activity
 * @see K9ListActivity
 */
public class K9ActivityCommon {
    /**
     * Creates a new instance of {@link K9ActivityCommon} bound to the specified activity.
     *
     * @param activity
     *         The {@link Activity} the returned {@code K9ActivityCommon} instance will be bound to.
     *
     * @return The {@link K9ActivityCommon} instance that will provide the base functionality of the
     *         "K9" activities.
     */
    public static K9ActivityCommon newInstance(Activity activity) {
        return new K9ActivityCommon(activity);
    }

    public static K9ActivityCommon newMaterialInstance(Activity activity) {
        return new K9ActivityCommon(activity, true);
    }


    /**
     * Base activities need to implement this interface.
     *
     * <p>The implementing class simply has to call through to the implementation of these methods
     * in {@link K9ActivityCommon}.</p>
     */
    public interface K9ActivityMagic {
        void setupGestureDetector(OnSwipeGestureListener listener);
    }


    private Activity mActivity;
    private GestureDetector mGestureDetector;


    private K9ActivityCommon(Activity activity) {
        this(activity, false);
    }

    private K9ActivityCommon(Activity activity, boolean isMaterial) {
        mActivity = activity;
        if (isMaterial) {
            mActivity.setTheme(K9.getK9MaterialThemeResourceId());
        } else {
            mActivity.setTheme(K9.getK9ThemeResourceId());
        }
    }

    /**
     * Call this before calling {@code super.dispatchTouchEvent(MotionEvent)}.
     */
    public void preDispatchTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }
    }

    /**
     * Get the background color of the theme used for this activity.
     *
     * @return The background color of the current theme.
     */
    public int getThemeBackgroundColor() {
        TypedArray array = mActivity.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.colorBackground });

        int backgroundColor = array.getColor(0, 0xFF00FF);

        array.recycle();

        return backgroundColor;
    }

    /**
     * Call this if you wish to use the swipe gesture detector.
     *
     * @param listener
     *         A listener that will be notified if a left to right or right to left swipe has been
     *         detected.
     */
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mGestureDetector = new GestureDetector(mActivity,
                new SwipeGestureDetector(mActivity, listener));
    }
}
