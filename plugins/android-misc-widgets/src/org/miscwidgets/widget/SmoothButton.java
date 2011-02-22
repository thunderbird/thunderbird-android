package org.miscwidgets.widget;

import org.miscwidgets.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;

public class SmoothButton extends Button {

	private static final long DELAY = 25;

	private int transitionDrawableLength;
	private int level;

	private int[] colors;

	private boolean wasPressed;

	private Drawable background;

	public SmoothButton(Context context, AttributeSet attrs) {
		super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SmoothButton);
        LevelListDrawable transitionDrawable = (LevelListDrawable) a.getDrawable(R.styleable.SmoothButton_transitionDrawable);

        transitionDrawableLength = a.getInt(R.styleable.SmoothButton_transitionDrawableLength, 0);
        int useTextColors = 0;
        int c0 = 0;
        if (a.hasValue(R.styleable.SmoothButton_transitionTextColorUp)) {
		c0 = a.getColor(R.styleable.SmoothButton_transitionTextColorUp, 0);
		useTextColors++;
        }
        int c1 = 0;
        if (useTextColors == 1 && a.hasValue(R.styleable.SmoothButton_transitionTextColorDown)) {
		c1 = a.getColor(R.styleable.SmoothButton_transitionTextColorDown, 0);
		useTextColors++;
        }
        a.recycle();

        if (transitionDrawable == null) {
		throw new RuntimeException("transitionDrawable must be defined in XML (with valid xmlns)");
        }
        if (transitionDrawableLength == 0) {
		throw new RuntimeException("transitionDrawableLength must be defined in XML (with valid xmlns)");
        }
        if (useTextColors == 2) {
		setTextColor(c0);
		int a0 = Color.alpha(c0);
		int r0 = Color.red(c0);
		int g0 = Color.green(c0);
		int b0 = Color.blue(c0);
		int a1 = Color.alpha(c1);
		int r1 = Color.red(c1);
		int g1 = Color.green(c1);
		int b1 = Color.blue(c1);
		colors = new int[transitionDrawableLength];
		for (int i=0; i<transitionDrawableLength; i++) {
			int ai = a0 + i * (a1 - a0) / transitionDrawableLength;
			int ri = r0 + i * (r1 - r0) / transitionDrawableLength;
			int gi = g0 + i * (g1 - g0) / transitionDrawableLength;
			int bi = b0 + i * (b1 - b0) / transitionDrawableLength;
			colors[i] = Color.argb(ai, ri, gi, bi);
		}
        }

        int[] viewAttrs = {
			android.R.attr.background
        };
        a = context.obtainStyledAttributes(attrs, viewAttrs);
        boolean hasBackground = a.hasValue(0);
        a.recycle();
        if (!hasBackground) {
		int l = getPaddingLeft();
		int t = getPaddingTop();
		int r = getPaddingRight();
		int b = getPaddingBottom();
		setBackgroundDrawable(transitionDrawable);
		setPadding(l, t, r, b);
        }

        background = getBackground();
		level = 0;
		background.setLevel(level);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean rc = super.onKeyDown(keyCode, event);
		if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) &&
				event.getRepeatCount() == 0 && level + 1 < transitionDrawableLength) {
			handler.removeMessages(-1);
			handler.sendEmptyMessageDelayed(1, DELAY);
		}
		return rc;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean rc = super.onKeyUp(keyCode, event);
		if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) &&
				event.getRepeatCount() == 0 && level > 0) {
			handler.removeMessages(1);
			handler.sendEmptyMessageDelayed(-1, DELAY);
		}
		return rc;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		wasPressed = isPressed();
		boolean rc = super.onTouchEvent(event);
		post(checkPressed);
		return rc;
	}

	Runnable checkPressed = new Runnable() {
		public void run() {
			if (!wasPressed && isPressed()) {
				if (level + 1 < transitionDrawableLength) {
					handler.removeMessages(-1);
					handler.sendEmptyMessageDelayed(1, DELAY);
				}
			} else
			if (wasPressed && !isPressed()) {
				if (level > 0) {
					handler.removeMessages(1);
					handler.sendEmptyMessageDelayed(-1, DELAY);
				}
			}
		}
	};

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			level += what;
			if (level >= 0 && level < transitionDrawableLength) {
				background.setLevel(level);
		        if (colors != null) {
				setTextColor(colors[level]);
		        }
		        sendEmptyMessageDelayed(what, DELAY);
			} else {
				level = Math.max(0, level);
				level = Math.min(transitionDrawableLength-1, level);
			}
		}
	};
}
