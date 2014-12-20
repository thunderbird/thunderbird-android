package com.fsck.k9.activity.misc;

import android.content.Context;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;


public class SwipeGestureDetector extends SimpleOnGestureListener {
    public static final int BEZEL_SWIPE_THRESHOLD = 20;

    private static final float SWIPE_MAX_OFF_PATH_DIP = 250f;
    private static final float SWIPE_THRESHOLD_VELOCITY_DIP = 325f;


    private final OnSwipeGestureListener mListener;
    private int mMinVelocity;
    private int mMaxOffPath;
    private MotionEvent mLastOnDownEvent = null;


    public SwipeGestureDetector(Context context, OnSwipeGestureListener listener) {
        super();

        if (listener == null) {
            throw new IllegalArgumentException("'listener' may not be null");
        }

        mListener = listener;

        // Calculate the minimum distance required for this to count as a swipe.
        // Convert the constant dips to pixels.
        float gestureScale = context.getResources().getDisplayMetrics().density;
        mMinVelocity = (int) (SWIPE_THRESHOLD_VELOCITY_DIP * gestureScale + 0.5f);
        mMaxOffPath = (int) (SWIPE_MAX_OFF_PATH_DIP * gestureScale + 0.5f);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mLastOnDownEvent = e;
        return super.onDown(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Apparently sometimes e1 is null
        // Found a workaround here: http://stackoverflow.com/questions/4151385/
        if (e1 == null) {
            e1 = mLastOnDownEvent;
        }

        // Make sure we avoid NullPointerExceptions
        if (e1 == null || e2 == null) {
            return false;
        }

        // Calculate how much was actually swiped.
        final float deltaX = e2.getX() - e1.getX();
        final float deltaY = e2.getY() - e1.getY();

        // Calculate the minimum distance required for this to be considered a swipe.
        final int minDistance = (int) Math.abs(deltaY * 4);

        try {
            if (Math.abs(deltaY) > mMaxOffPath || Math.abs(velocityX) < mMinVelocity) {
                return false;
            }

            if (deltaX < (minDistance * -1)) {
                mListener.onSwipeRightToLeft(e1, e2);
            } else if (deltaX > minDistance) {
                mListener.onSwipeLeftToRight(e1, e2);
            } else {
                return false;
            }

            // successful fling, cancel the 2nd event to prevent any other action from happening
            // see http://code.google.com/p/android/issues/detail?id=8497
            e2.setAction(MotionEvent.ACTION_CANCEL);
        } catch (Exception e) {
            // nothing
        }

        return false;
    }


    /**
     * A listener that will be notified when a right to left or left to right swipe has been
     * detected.
     */
    public interface OnSwipeGestureListener {
        /**
         * Called when a swipe from right to left is handled by {@link MyGestureDetector}.
         *
         * <p>See {@link OnGestureListener#onFling(MotionEvent, MotionEvent, float, float)}
         * for more information on the {@link MotionEvent}s being passed.</p>
         *
         * @param e1
         *         First down motion event that started the fling.
         * @param e2
         *         The move motion event that triggered the current onFling.
         */
        void onSwipeRightToLeft(final MotionEvent e1, final MotionEvent e2);

        /**
         * Called when a swipe from left to right is handled by {@link MyGestureDetector}.
         *
         * <p>See {@link OnGestureListener#onFling(MotionEvent, MotionEvent, float, float)}
         * for more information on the {@link MotionEvent}s being passed.</p>
         *
         * @param e1
         *         First down motion event that started the fling.
         * @param e2
         *         The move motion event that triggered the current onFling.
         */
        void onSwipeLeftToRight(final MotionEvent e1, final MotionEvent e2);
    }
}
