package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ToggleScrollView extends ScrollView
{
    private GestureDetector mDetector;
    private boolean mScrolling = true;

    public ToggleScrollView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mDetector = new GestureDetector(new YScrollDetector());
    }

    public void setScrolling(boolean enable)
    {
        mScrolling = enable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        return (mScrolling) ? super.onTouchEvent(ev) : true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if(!mScrolling)
        {
            return false;
        }

        // This doesn't quite get us to diagonal scrolling, but it's somewhat better than what we've
        // currently got.  This is based on
        // http://stackoverflow.com/questions/2646028/android-horizontalscrollview-within-scrollview-touch-handling
        boolean result = super.onInterceptTouchEvent(ev);
        if (mDetector.onTouchEvent(ev))
        {
            return result;
        }

        return false;
    }

    // Return false if we're scrolling in the x direction
    class YScrollDetector extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            try
            {
                if (Math.abs(distanceY) > Math.abs(distanceX))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            catch (Exception e)
            {
                // nothing
            }
            return false;
        }
    }
}
