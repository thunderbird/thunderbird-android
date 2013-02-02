package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.ViewAnimator;

/**
 * A {@link ViewAnimator} that animates between two child views using different animations
 * depending on which view is displayed.
 */
public class ViewSwitcher extends ViewAnimator {
    private Animation mFirstInAnimation;
    private Animation mFirstOutAnimation;
    private Animation mSecondInAnimation;
    private Animation mSecondOutAnimation;


    public ViewSwitcher(Context context) {
        super(context);
    }

    public ViewSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void showFirstView() {
        if (getDisplayedChild() == 0) {
            return;
        }

        setInAnimation(mFirstInAnimation);
        setOutAnimation(mFirstOutAnimation);
        setDisplayedChild(0);
    }

    public void showSecondView() {
        if (getDisplayedChild() == 1) {
            return;
        }

        setInAnimation(mSecondInAnimation);
        setOutAnimation(mSecondOutAnimation);
        setDisplayedChild(1);
    }

    public Animation getFirstInAnimation() {
        return mFirstInAnimation;
    }

    public void setFirstInAnimation(Animation inAnimation) {
        this.mFirstInAnimation = inAnimation;
    }

    public Animation getmFirstOutAnimation() {
        return mFirstOutAnimation;
    }

    public void setFirstOutAnimation(Animation outAnimation) {
        mFirstOutAnimation = outAnimation;
    }

    public Animation getSecondInAnimation() {
        return mSecondInAnimation;
    }

    public void setSecondInAnimation(Animation inAnimation) {
        mSecondInAnimation = inAnimation;
    }

    public Animation getSecondOutAnimation() {
        return mSecondOutAnimation;
    }

    public void setSecondOutAnimation(Animation outAnimation) {
        mSecondOutAnimation = outAnimation;
    }
}
