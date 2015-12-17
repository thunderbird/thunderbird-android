package com.fsck.k9.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewAnimator;

import com.fsck.k9.R;


/** This view extends the common ViewAnimator, allowing different sets of animations
 * for increasing and decreasing the displayed child.
 */
public class LinearViewAnimator extends ViewAnimator {

    Animation mUpInAnimation;
    Animation mUpOutAnimation;

    Animation mDownInAnimation;
    Animation mDownOutAnimation;

    public LinearViewAnimator(Context context) {
        super(context);
    }

    public LinearViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LinearViewAnimator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LinearViewAnimator);

        int resource = a.getResourceId(R.styleable.LinearViewAnimator_downInAnimation, 0);
        if (resource > 0) {
            setDownInAnimation(context, resource);
        }

        resource = a.getResourceId(R.styleable.LinearViewAnimator_downOutAnimation, 0);
        if (resource > 0) {
            setDownOutAnimation(context, resource);
        }

        resource = a.getResourceId(R.styleable.LinearViewAnimator_upInAnimation, 0);
        if (resource > 0) {
            setUpInAnimation(context, resource);
        }

        resource = a.getResourceId(R.styleable.LinearViewAnimator_upOutAnimation, 0);
        if (resource > 0) {
            setUpOutAnimation(context, resource);
        }

        a.recycle();
    }

    public void setUpOutAnimation(Context context, int resourceID) {
        setUpOutAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setUpOutAnimation(Animation animation) {
        mUpOutAnimation = animation;
    }

    public void setUpInAnimation(Context context, int resourceID) {
        setUpInAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setUpInAnimation(Animation animation) {
        mUpInAnimation = animation;
    }
    
    public void setDownOutAnimation(Context context, int resourceID) {
        setDownOutAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setDownOutAnimation(Animation animation) {
        mDownOutAnimation = animation;
    }

    public void setDownInAnimation(Context context, int resourceID) {
        setDownInAnimation(AnimationUtils.loadAnimation(context, resourceID));
    }

    public void setDownInAnimation(Animation animation) {
        mDownInAnimation = animation;
    }

    @Override
    public void setDisplayedChild(int whichChild) {
        setDisplayedChild(whichChild, true);
    }

    public void setDisplayedChild(int whichChild, boolean animate) {
        int displayedChild = getDisplayedChild();
        if (displayedChild == whichChild) {
            return;
        }
        if (!animate) {
            setInAnimation(null);
            setOutAnimation(null);
        } else if (displayedChild < whichChild) {
            setInAnimation(mDownInAnimation);
            setOutAnimation(mDownOutAnimation);
        } else {
            setInAnimation(mUpInAnimation);
            setOutAnimation(mUpOutAnimation);
        }
        super.setDisplayedChild(whichChild);
    }

}
