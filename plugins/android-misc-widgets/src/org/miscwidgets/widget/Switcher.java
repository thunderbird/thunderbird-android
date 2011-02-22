package org.miscwidgets.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.miscwidgets.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Interpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Scroller;
import android.widget.SpinnerAdapter;

public class Switcher extends ViewGroup {

	private static final int SCROLL = 0;
	private static final int JUSTIFY = 1;
	private static final int DISMISS_CONTROLS = 2;

	private static final int ANIMATION_DURATION = 750;
	private static final int IDLE_TIMEOUT = 3 * 1000;

	private int mOrientation;
	private int mSize;
	private Drawable mDecreaseButtonDrawable;
	private Drawable mIncreaseButtonDrawable;
	private ImageButton mDecreaseButton;
	private ImageButton mIncreaseButton;
	private PopupWindow mDecreasePopup;
	private PopupWindow mIncreasePopup;
	private int mIndex;
	private int mPosition;
	private Scroller mScroller;
	private Map<View, Integer> mViews;
	private SpinnerAdapter mAdapter;
	private int mPackedViews;
	private GestureDetector mGestureDetector;
	private Rect mGlobal;
	private int mAnimationDuration;

	public Switcher(Context context, AttributeSet attrs) {
		super(context, attrs);

		int[] linerarLayoutAttrs = {
			android.R.attr.orientation
		};
		TypedArray a = context.obtainStyledAttributes(attrs, linerarLayoutAttrs);
		mOrientation = a.getInteger(0, LinearLayout.HORIZONTAL);
		a.recycle();

		a = context.obtainStyledAttributes(attrs, R.styleable.Switcher);
		mDecreaseButtonDrawable = a.getDrawable(R.styleable.Switcher_decreaseButton);
		mIncreaseButtonDrawable = a.getDrawable(R.styleable.Switcher_increaseButton);
		mAnimationDuration = a.getInteger(R.styleable.Switcher_animationDuration, ANIMATION_DURATION);
		mIdleTimeout = a.getInteger(R.styleable.Switcher_idleTimeout, IDLE_TIMEOUT);
		a.recycle();

		if(mDecreaseButtonDrawable == null) {
			throw new IllegalArgumentException(a.getPositionDescription() + ": decreaseButton attrubute not specified.");
		}
		if(mIncreaseButtonDrawable == null) {
			throw new IllegalArgumentException(a.getPositionDescription() + ": increaseButton attrubute not specified.");
		}

		mDecreaseButton = new ImageButton(context);
		mDecreaseButton.setEnabled(false);
		mDecreaseButton.setBackgroundDrawable(mDecreaseButtonDrawable);
		mIncreaseButton = new ImageButton(context);
		mIncreaseButton.setEnabled(false);
		mIncreaseButton.setBackgroundDrawable(mIncreaseButtonDrawable);

		mDecreaseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setPreviousView();
			}
		});
		mIncreaseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setNextView();
			}
		});

		mScroller = new Scroller(context);
		mIndex = -1;
		mPosition = -1;
		mPackedViews = -1;
		mViews = new HashMap<View, Integer>();

		mGestureDetector = new GestureDetector(gestureListener);
		mGestureDetector.setIsLongpressEnabled(false);
		setFocusable(true);
		setFocusableInTouchMode(true);
		mDecreasePopup = new PopupWindow(mDecreaseButton, mDecreaseButtonDrawable.getIntrinsicWidth(), mDecreaseButtonDrawable.getIntrinsicHeight());
		mIncreasePopup = new PopupWindow(mIncreaseButton, mIncreaseButtonDrawable.getIntrinsicWidth(), mIncreaseButtonDrawable.getIntrinsicHeight());
		mDecreasePopup.setAnimationStyle(android.R.style.Animation_Toast);
		mIncreasePopup.setAnimationStyle(android.R.style.Animation_Toast);
		mGlobal = new Rect();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		mSize = mOrientation == LinearLayout.HORIZONTAL? getMeasuredWidth() : getMeasuredHeight();
	}

	private int getPackedViews(int offset) {
		int size = mSize;
		int start = offset / size;
		int numViews = offset % size != 0? 1 : 0;
		return start << 1 | numViews;
	}

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == DISMISS_CONTROLS) {
				mDecreasePopup.dismiss();
				mIncreasePopup.dismiss();
				return;
			}

			mScroller.computeScrollOffset();
			int currX = mScroller.getCurrX();
			int delta = mPosition - currX;
			mPosition = currX;
			int packed = getPackedViews(mPosition);
			manageViews(packed);
			scroll(delta);
			if (!mScroller.isFinished()) {
				handler.sendEmptyMessage(msg.what);
			} else {
				if (msg.what == SCROLL) {
					justify();
				} else {
					mIndex = mPosition / mSize;
					setupButtons();
				}
			}
		}
	};
	private long mIdleTimeout;

	private void justify() {
		int offset = mPosition % mSize;
		if (offset != 0) {
			int endPosition = mPosition - offset;
			if (offset > mSize / 2) {
				endPosition += mSize;
			}
			mScroller.startScroll(mPosition, 0, endPosition - mPosition, 0, mAnimationDuration);
			handler.sendEmptyMessage(JUSTIFY);
		} else {
			mIndex = mPosition / mSize;
			setupButtons();
		}
	}

	private void scroll(int offset) {
		if (mOrientation == LinearLayout.HORIZONTAL) {
			for (View view : mViews.keySet()) {
				view.offsetLeftAndRight(offset);
			}
		} else {
			for (View view : mViews.keySet()) {
				view.offsetTopAndBottom(offset);
			}
		}
		invalidate();
	}

	private void setupButtons() {
		if (mAdapter != null) {
			boolean enabled = mIndex > 0;
			mDecreaseButton.setEnabled(enabled);
			enabled = mIndex + 1 < mAdapter.getCount();
			mIncreaseButton.setEnabled(enabled);
		}
	}

	public void setSelection(int index, boolean animate) {
		if (index == mIndex) {
			return;
		}
		int endPosition = index * mSize;
		int diff = Math.abs(index - mIndex);
		int sign = index > mIndex? 1 : -1;
		mIndex = index;
		if (diff > 1) {
			mPosition = endPosition - sign * mSize;
		}
		if (animate) {
			mScroller.startScroll(mPosition, 0, endPosition - mPosition, 0, mAnimationDuration);
			handler.removeMessages(JUSTIFY);
			handler.removeMessages(SCROLL);
			handler.sendEmptyMessage(JUSTIFY);
		} else {
			mPosition = endPosition;
			manageViews(index << 1);
			setupButtons();
			invalidate();
		}
	}

	private void manageViews(int packedViews) {
		if (packedViews == mPackedViews) {
			return;
		}

		mPackedViews = packedViews;
		int startIdx = packedViews >> 1;
		int endIdx = startIdx + (packedViews & 1);
		int viewIdx = startIdx;
		while (viewIdx <= endIdx) {
			if (!mViews.containsValue(viewIdx)) {
				if (viewIdx >= 0 && viewIdx < mAdapter.getCount()) {
					View view = mAdapter.getView(viewIdx, null, this);
					mViews.put(view, viewIdx);
					addView(view);
				}
			}
			viewIdx++;
		}

		// remove not visible views
		Iterator<View> iterator = mViews.keySet().iterator();
		while (iterator.hasNext()) {
			View view = iterator.next();
			int idx = mViews.get(view);
			if (idx < startIdx || idx > endIdx) {
				iterator.remove();
				removeView(view);
			}
		}
	}

	public int getSelection() {
		return mIndex;
	}

	public void setPreviousView() {
		if (mAdapter != null && mIndex > 0) {
			setSelection(mIndex-1, true);
			setupDismiss();
		}
	}

	public void setNextView() {
		if (mAdapter != null && mIndex + 1 < mAdapter.getCount()) {
			setSelection(mIndex+1, true);
			setupDismiss();
		}
	}

	public void setAdapter(SpinnerAdapter adapter) {
		mAdapter = adapter;
		if (mAdapter != null) {
			setSelection(0, false);
			setupButtons();
		}
	}

	private void setupDismiss() {
		handler.removeMessages(DISMISS_CONTROLS);
		handler.sendEmptyMessageDelayed(DISMISS_CONTROLS, mIdleTimeout);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean rc = mGestureDetector.onTouchEvent(event);
		if (!rc && event.getAction() == MotionEvent.ACTION_UP) {
			justify();
		}
		return true;
	}

	SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
		@Override
		public boolean onDown(MotionEvent e) {
			requestFocus();
			popup();
			return true;
		}
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (mAdapter != null) {
				int distance = (int) (mOrientation == LinearLayout.HORIZONTAL? distanceX : distanceY);
				int pos = mPosition + distance;
				if (pos >= 0 && pos < (mAdapter.getCount() - 1) * mSize) {
					mPosition = pos;
					int packed = getPackedViews(mPosition);
					manageViews(packed);
					scroll(-distance);
					return true;
				}
			}
			return false;
		}
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (mAdapter != null) {
				float velocity = mOrientation == LinearLayout.HORIZONTAL? velocityX : velocityY;
				mScroller.fling(mPosition, 0, (int) -velocity, 0,
						0, (mAdapter.getCount() - 1) * mSize,
						0, 0);
				handler.removeMessages(JUSTIFY);
				handler.removeMessages(SCROLL);
				handler.sendEmptyMessage(SCROLL);
				return true;
			}
			return false;
		}
	};

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (gainFocus) {
			popup();
		} else {
			handler.removeMessages(DISMISS_CONTROLS);
			mDecreasePopup.dismiss();
			mIncreasePopup.dismiss();
		}
	}

	private void popup() {
		if (mDecreasePopup.isShowing() && mIncreasePopup.isShowing()) {
			return;
		}
		getGlobalVisibleRect(mGlobal);
		if (mOrientation == LinearLayout.HORIZONTAL) {
			mDecreasePopup.showAtLocation(this,
					Gravity.NO_GRAVITY,
					mGlobal.left,
					mGlobal.centerY() - mDecreasePopup.getHeight()/2);
			mIncreasePopup.showAtLocation(this,
					Gravity.NO_GRAVITY,
					mGlobal.right - mIncreasePopup.getWidth(),
					mGlobal.centerY() - mIncreasePopup.getHeight()/2);
		} else {
			// TODO: re-position when Switcher is at the very top/bottom of screen
			mDecreasePopup.showAtLocation(this,
					Gravity.NO_GRAVITY,
					mGlobal.centerX() - mDecreasePopup.getWidth()/2,
					mGlobal.top-mDecreasePopup.getHeight());
			mIncreasePopup.showAtLocation(this,
					Gravity.NO_GRAVITY,
					mGlobal.centerX() - mIncreasePopup.getWidth()/2,
					mGlobal.bottom);
		}
		setupDismiss();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		for (View view : mViews.keySet()) {
			if (view.getWidth() == 0) {
				// new View: not layout()ed
				int idx = mViews.get(view);
				if (mOrientation == LinearLayout.HORIZONTAL) {
					int left = mSize * idx - mPosition;
					view.layout(left, 0, left+r-l, b-t);
				} else {
					int top = mSize * idx - mPosition;
					view.layout(0, top, r-l, top+b-t);
				}
			}
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mDecreasePopup.dismiss();
		mIncreasePopup.dismiss();
	}

	public void setInterpolator(Interpolator interpolator) {
		mScroller = new Scroller(getContext(), interpolator);
	}
}
