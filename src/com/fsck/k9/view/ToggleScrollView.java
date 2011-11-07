package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingListener;

/**
 * An extension of {@link ScrollView} that allows scrolling to be selectively disabled.
 */
public class ToggleScrollView extends ScrollView {
    private GestureDetector mDetector;
    private boolean mScrolling = true;
    private int mCurrentYPosition;
    private double mScrollPercentage;
    private ScrollToLastLocationListener mListener;

    public ToggleScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDetector = new GestureDetector(new YScrollDetector());
    }

    public void setScrolling(boolean enable) {
        mScrolling = enable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return (mScrolling) ? super.onTouchEvent(ev) : true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mScrolling) {
            return false;
        }

        // This doesn't quite get us to diagonal scrolling, but it's somewhat better than what we've
        // currently got.  This is based on
        // http://stackoverflow.com/questions/2646028/android-horizontalscrollview-within-scrollview-touch-handling
        boolean result = super.onInterceptTouchEvent(ev);
        // Let the original ScrollView handle ACTION_DOWN so we can stop the scroll when someone touches the screen.
        if (ev.getAction() == MotionEvent.ACTION_DOWN || mDetector.onTouchEvent(ev)) {
            return result;
        }

        return false;
    }

    // Return false if we're scrolling in the x direction. That is, decline to consume the event and
    // let the parent class take a stab at it.
    class YScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            try {
                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }

    /**
     * Fetch the current percentage by which this view has been scrolled.
     * @return Scroll percentage based on the top edge of the screen, from 0 to 100.  This number should never really
     * be 100, unless the screen is of 0 height...
     */
    public double getScrollPercentage() {
        // We save only the Y coordinate instead of the percentage because I don't know how expensive the
        // computeVerticalScrollRange() call is.
        final int scrollRange = computeVerticalScrollRange();
        if(scrollRange == 0) {
            return 0;
        }
        return (double) mCurrentYPosition / scrollRange;
    }

    /**
     * Set the percentage by which we should scroll the page once we get the load complete event.  This is
     * based on the top edge of the view.
     * @param percentage Percentage of page to scroll to.
     */
    public void setScrollPercentage(final double percentage) {
        Log.d(K9.LOG_TAG, "ToggleView: Setting last scroll percentage to " + percentage);
        this.mScrollPercentage = percentage;
    }

    /**
     * Override {@link ScrollView#onScrollChanged(int, int, int, int)} to record the current x/y position.  We use this
     * to save our current position for future scrolling.
     *
     * @param x
     * @param y
     * @param oldx
     * @param oldy
     */
    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);

        this.mCurrentYPosition = y;
        // I wish Android has a TRACE log level so I wouldn't have to comment this out.  This one is really noisy.
        // Log.d(K9.LOG_TAG, "ToggleScrollView: mCurrentYPosition=" + y + " scrollRange=" + computeVerticalScrollRange() + " pct=" + getScrollPercentage());
    }

    /**
     * This is a {@link MessagingListener} which listens for when the a message has finished being displayed on the
     * screen.  We'll scroll the message to the user's last known location once it's done.
     */
    class ScrollToLastLocationListener extends MessagingListener {
        public void messageViewFinished() {
            // Don't scroll if our last position was at the top.
            if(mScrollPercentage != 0.0) {
                final int scrollRange = computeVerticalScrollRange();
                final int newY = (int)(mScrollPercentage * scrollRange);
                Log.d(K9.LOG_TAG, "ToggleScrollView: requested " + (100 * mScrollPercentage) + "%, " +
                    "scrolling to " + newY + "/" + scrollRange);
                scrollTo(0, newY);
            }
        }
    }

    /**
     * Fetch the {@link MessagingListener} for this <code>ScrollView</code>.
     * @return
     */
    public MessagingListener getListener() {
        if(this.mListener != null) {
            return this.mListener;
        } else {
            return this.mListener = new ScrollToLastLocationListener();
        }
    }
}
