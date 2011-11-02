package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;
import com.fsck.k9.K9;

/**
 * An extension of {@link ScrollView} that allows scrolling to be selectively disabled.
 */
public class ToggleScrollView extends ScrollView {
    private GestureDetector mDetector;
    private boolean mScrolling = true;
    private int currentYPosition;

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
        if(computeVerticalScrollRange() == 0) {
            return 0;
        }
        return (double)currentYPosition / computeVerticalScrollRange();
    }

    /**
     * Scroll the screen to a specific percentage of the page.  This is based on the top edge of the page.
     * @param percentage Percentage of page to scroll to.
     */
    public void setScrollPercentage(final double percentage) {
        final int newY = (int)(percentage * computeVerticalScrollRange());
        Log.d(K9.LOG_TAG, "ToggleScrollView: requested " + (100 * percentage) + "%, scrolling to " + newY + "/" + computeVerticalScrollRange());
        scrollTo(0, newY);
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

        this.currentYPosition = y;
        // I wish Android has a TRACE log level so I wouldn't have to comment this out.  This one is really noisy.
        // Log.d(K9.LOG_TAG, "ToggleScrollView: currentYPosition=" + y + " scrollRange=" + computeVerticalScrollRange() + " pct=" + getScrollPercentage());
    }
}
