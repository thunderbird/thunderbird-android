package com.nobu_games.android.view.web;

/*
 * Copyright (C) 2012 Thomas Werner
 * Portions Copyright (C) 2006 The Android Open Source Project
 * Portions Copyright (C) 2012 The K-9 Dog Walkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClassic.TitleBarDelegate;
import android.widget.FrameLayout;

/**
 * WebView derivative with custom setEmbeddedTitleBar implementation for Android
 * versions that do not support that feature anymore.
 * <p>
 * <b>Usage</b>
 * <p>
 * Call {@link #setEmbeddedTitleBarCompat(View)} for setting a view as embedded
 * title bar on top of the displayed WebView page.
 */
public class TitleBarWebView extends WebView implements TitleBarDelegate {
    /**
     * Internally used view wrapper for suppressing unwanted touch events on the
     * title bar view when WebView contents is being touched.
     */
    private class TouchBlockView extends FrameLayout {
        public TouchBlockView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if(!mTouchInTitleBar) {
                return false;
            } else {
                switch(ev.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mTouchInTitleBar = false;
                        break;

                    default:

                }

                return super.dispatchTouchEvent(ev);
            }
        }
    }

    private static final String TAG = "TitleBarWebView";
    View mTitleBar;
    int mTitleBarOffs;
    boolean mTouchInTitleBar;
    boolean mTouchMove;
    private Rect mClipBounds = new Rect();
    private Matrix mMatrix = new Matrix();
    private Method mNativeGetVisibleTitleHeightMethod;

    /**
     * This will always contain a reference to the title bar view no matter if
     * {@code setEmbeddedTitleBar()} or the Jelly Bean workaround is used. We use this in
     * {@link #getTitleHeight()}.
     */
    private View mTitleBarView;

    public TitleBarWebView(Context context) {
        super(context);
        init();
    }

    public TitleBarWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TitleBarWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * <i>Corrects the visual displacement caused by the title bar view.</i>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(mTitleBar != null) {
            final int sy = getScrollY();
            final int visTitleHeight = getVisibleTitleHeightCompat();
            final float x = event.getX();
            float y = event.getY();

            switch(event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if(y <= visTitleHeight) {
                        mTouchInTitleBar = true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    mTouchMove = true;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mTouchMove = false;
                    break;

                default:
            }

            if(mTouchInTitleBar) {
                y += sy;
                event.setLocation(x, y);

                return mTitleBar.dispatchTouchEvent(event);
            } else {
                if(Build.VERSION.SDK_INT < 16) {
                    if(!mTouchMove) {
                        mTitleBarOffs = getVisibleTitleHeightCompat();
                    }

                    y -= mTitleBarOffs;
                    if(y < 0) y = 0;
                    event.setLocation(x, y);
                }

                return super.dispatchTouchEvent(event);
            }
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    /**
     * Sets a {@link View} as an embedded title bar to appear on top of the
     * WebView page.
     * <p>
     * This method tries to call the hidden API method setEmbeddedTitleBar if
     * present. On failure the custom implementation provided by this class will
     * be used instead.
     *
     * @param v The view to set or null for removing the title bar view.
     */
    public void setEmbeddedTitleBarCompat(View v) {
        try {
            Method nativeMethod = getClass().getMethod("setEmbeddedTitleBar",
                    View.class);
            nativeMethod.invoke(this, v);
        } catch(Exception e) {
            Log.d(TAG,
                    "Native setEmbeddedTitleBar not available. Starting workaround");
            setEmbeddedTitleBarJellyBean(v);
        }

        mTitleBarView = v;
    }

    @Override
    protected int computeVerticalScrollExtent() {
        if(mTitleBar == null) return super.computeVerticalScrollExtent();
        return getViewHeightWithTitle() - getVisibleTitleHeightCompat();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if(mTitleBar == null) return super.computeVerticalScrollOffset();
        return Math.max(getScrollY() - getTitleHeight(), 0);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        canvas.save();

        if(child == mTitleBar) {
            mTitleBar.offsetLeftAndRight(getScrollX() - mTitleBar.getLeft());

            if(Build.VERSION.SDK_INT < 16) {
                mMatrix.set(canvas.getMatrix());
                mMatrix.postTranslate(0, -getScrollY());
                canvas.setMatrix(mMatrix);
            }
        }

        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restore();

        return result;
    }

    /**
     * Gets the currently visible height of the title bar view if set.
     *
     * @return Visible height of title bar view or 0 if not set.
     */
    protected int getVisibleTitleHeightCompat() {
        if(mTitleBar == null && mNativeGetVisibleTitleHeightMethod != null) {
            try {
                return (Integer) mNativeGetVisibleTitleHeightMethod
                        .invoke(this);
            } catch(Exception e) {
            }
        }

        return Math.max(getTitleHeight() - Math.max(0, getScrollY()), 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= 16) {
            super.onDraw(canvas);
            return;
        }

        canvas.save();

        if(mTitleBar != null) {
            final int sy = getScrollY();
            final int sx = getScrollX();
            mClipBounds.top = sy;
            mClipBounds.left = sx;
            mClipBounds.right = mClipBounds.left + getWidth();
            mClipBounds.bottom = mClipBounds.top + getHeight();
            canvas.clipRect(mClipBounds);
            mMatrix.set(canvas.getMatrix());
            int titleBarOffs = getVisibleTitleHeightCompat();
            if(titleBarOffs < 0) titleBarOffs = 0;
            mMatrix.postTranslate(0, titleBarOffs);
            canvas.setMatrix(mMatrix);
        }

        super.onDraw(canvas);
        canvas.restore();
    }

    /**
     * Overrides a hidden method by replicating the behavior of the original
     * WebView class from Android 2.3.4.
     * <p>
     * The worst that could happen is that this method never gets called, which
     * isn't too bad because this does not harm the functionality of this class.
     */
    protected void onDrawVerticalScrollBar(Canvas canvas, Drawable scrollBar,
            int l, int t, int r, int b) {
        int sy = getScrollY();

        if(sy < 0) {
            t -= sy;
        }
        scrollBar.setBounds(l, t + getVisibleTitleHeightCompat(), r, b);
        scrollBar.draw(canvas);
    }

    /**
     * Get the height of the title bar view.
     *
     * <p>
     * In the Jelly Bean workaround we need this method because we have to implement the
     * {@link TitleBarDelegate} interface. But by implementing this method we override the hidden
     * {@code getTitleHeight()} of the {@link WebView}s in older Android versions.
     * <br>
     * What we should do, is return the title height on Jelly Bean and call through to the parent
     * class on older Android versions. But this would require even more trickery, so we just
     * inline the parent functionality which simply calls {@link View#getHeight()}. This is exactly
     * what we do on Jelly Bean anyway.
     * </p>
     */
    @Override
    public int getTitleHeight() {
        if (mTitleBarView != null) {
            return mTitleBarView.getHeight();
        }
        return 0;
    }

    private int getViewHeightWithTitle() {
        int height = getHeight();
        if(isHorizontalScrollBarEnabled() && !overlayHorizontalScrollbar()) {
            height -= getHorizontalScrollbarHeight();
        }
        return height;
    }

    private void init() {
        try {
            mNativeGetVisibleTitleHeightMethod = WebView.class
                    .getDeclaredMethod("getVisibleTitleHeight");
        } catch(NoSuchMethodException e) {
            Log.w(TAG,
                    "Could not retrieve native hidden getVisibleTitleHeight method");
        }
    }

    /**
     * The hidden method setEmbeddedTitleBar has been removed from Jelly Bean.
     * This method replicates the functionality.
     *
     * @param v
     */
    private void setEmbeddedTitleBarJellyBean(View v) {
        if(mTitleBar == v) return;

        if(mTitleBar != null) {
            removeView(mTitleBar);
        }

        if(null != v) {
            LayoutParams vParams = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0);

            TouchBlockView tbv = new TouchBlockView(getContext());
            FrameLayout.LayoutParams tbvParams = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            tbv.addView(v, tbvParams);
            addView(tbv, vParams);
            v = tbv;
        }

        mTitleBar = v;
    }

    @Override
    public void onSetEmbeddedTitleBar(View title) { /* unused */ }
}
