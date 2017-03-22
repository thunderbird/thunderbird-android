/*
 * Copyright (C) 2015 Vincent Breitmoser <look@my.amazin.horse>
 * 
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.fsck.k9.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ViewAnimator;

import com.fsck.k9.R;


/** This view is essentially identical to ViewAnimator, but allows specifying the initial view
 * for preview as an xml attribute. */
public class ToolableViewAnimator extends ViewAnimator {
    private int mInitChild = -1;


    public ToolableViewAnimator(Context context) {
        super(context);
    }

    public ToolableViewAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToolableViewAnimator);
            mInitChild = a.getInt(R.styleable.ToolableViewAnimator_previewInitialChild, -1);
            a.recycle();
        }
    }

    public ToolableViewAnimator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        if (isInEditMode()) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToolableViewAnimator, defStyleAttr, 0);
            mInitChild = a.getInt(R.styleable.ToolableViewAnimator_previewInitialChild, -1);
            a.recycle();
        }
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (isInEditMode() && mInitChild-- > 0) {
            return;
        }
        super.addView(child, index, params);
    }

    @Override
    public void setDisplayedChild(int whichChild) {
        if (whichChild != getDisplayedChild()) {
            super.setDisplayedChild(whichChild);
        }
    }

    public void setDisplayedChild(int whichChild, boolean animate) {
        if (animate) {
            setDisplayedChild(whichChild);
            return;
        }

        Animation savedInAnim = getInAnimation();
        Animation savedOutAnim = getOutAnimation();
        setInAnimation(null);
        setOutAnimation(null);

        setDisplayedChild(whichChild);

        setInAnimation(savedInAnim);
        setOutAnimation(savedOutAnim);
    }
}
