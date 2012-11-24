/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
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
 *******************************************************************************/
package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;
import com.handmark.pulltorefresh.library.internal.IndicatorLayout;

public abstract class PullToRefreshAdapterViewBase<T extends AbsListView> extends PullToRefreshBase<T> implements
		OnScrollListener {

	private int mSavedLastVisibleIndex = -1;
	private OnScrollListener mOnScrollListener;
	private OnLastItemVisibleListener mOnLastItemVisibleListener;
	private View mEmptyView;

	private IndicatorLayout mIndicatorIvTop;
	private IndicatorLayout mIndicatorIvBottom;

	private boolean mShowIndicator;
	private boolean mScrollEmptyView = true;

	public PullToRefreshAdapterViewBase(Context context) {
		super(context);
		mRefreshableView.setOnScrollListener(this);
	}

	public PullToRefreshAdapterViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		mRefreshableView.setOnScrollListener(this);
	}

	public PullToRefreshAdapterViewBase(Context context, Mode mode) {
		super(context, mode);
		mRefreshableView.setOnScrollListener(this);
	}

	abstract public ContextMenuInfo getContextMenuInfo();

	/**
	 * Gets whether an indicator graphic should be displayed when the View is in
	 * a state where a Pull-to-Refresh can happen. An example of this state is
	 * when the Adapter View is scrolled to the top and the mode is set to
	 * {@link Mode#PULL_DOWN_TO_REFRESH}. The default value is <var>true</var>
	 * if {@link PullToRefreshBase#isPullToRefreshOverScrollEnabled()
	 * isPullToRefreshOverScrollEnabled()} returns false.
	 * 
	 * @return true if the indicators will be shown
	 */
	public boolean getShowIndicator() {
		return mShowIndicator;
	}

	public final void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
			final int totalItemCount) {

		if (DEBUG) {
			Log.d(LOG_TAG, "First Visible: " + firstVisibleItem + ". Visible Count: " + visibleItemCount
					+ ". Total Items: " + totalItemCount);
		}

		// If we have a OnItemVisibleListener, do check...
		if (null != mOnLastItemVisibleListener) {

			// Detect whether the last visible item has changed
			final int lastVisibleItemIndex = firstVisibleItem + visibleItemCount;

			/**
			 * Check that the last item has changed, we have any items, and that
			 * the last item is visible. lastVisibleItemIndex is a zero-based
			 * index, so we add one to it to check against totalItemCount.
			 */
			if (visibleItemCount > 0 && (lastVisibleItemIndex + 1) == totalItemCount) {
				if (lastVisibleItemIndex != mSavedLastVisibleIndex) {
					mSavedLastVisibleIndex = lastVisibleItemIndex;
					mOnLastItemVisibleListener.onLastItemVisible();
				}
			}
		}

		// If we're showing the indicator, check positions...
		if (getShowIndicatorInternal()) {
			updateIndicatorViewsVisibility();
		}

		// Finally call OnScrollListener if we have one
		if (null != mOnScrollListener) {
			mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
	}

	public final void onScrollStateChanged(final AbsListView view, final int scrollState) {
		if (null != mOnScrollListener) {
			mOnScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	/**
	 * Pass-through method for {@link PullToRefreshBase#getRefreshableView()
	 * getRefreshableView()}.{@link AdapterView#setAdapter(ListAdapter)
	 * setAdapter(adapter)}. This is just for convenience!
	 * 
	 * @param adapter
	 *            - Adapter to set
	 */
	public void setAdapter(ListAdapter adapter) {
		((AdapterView<ListAdapter>) mRefreshableView).setAdapter(adapter);
	}

	/**
	 * Sets the Empty View to be used by the Adapter View.
	 * 
	 * We need it handle it ourselves so that we can Pull-to-Refresh when the
	 * Empty View is shown.
	 * 
	 * Please note, you do <strong>not</strong> usually need to call this method
	 * yourself. Calling setEmptyView on the AdapterView will automatically call
	 * this method and set everything up. This includes when the Android
	 * Framework automatically sets the Empty View based on it's ID.
	 * 
	 * @param newEmptyView
	 *            - Empty View to be used
	 */
	public final void setEmptyView(View newEmptyView) {
		FrameLayout refreshableViewWrapper = getRefreshableViewWrapper();

		// If we already have an Empty View, remove it
		if (null != mEmptyView) {
			refreshableViewWrapper.removeView(mEmptyView);
		}

		if (null != newEmptyView) {
			// New view needs to be clickable so that Android recognizes it as a
			// target for Touch Events
			newEmptyView.setClickable(true);

			ViewParent newEmptyViewParent = newEmptyView.getParent();
			if (null != newEmptyViewParent && newEmptyViewParent instanceof ViewGroup) {
				((ViewGroup) newEmptyViewParent).removeView(newEmptyView);
			}

			refreshableViewWrapper.addView(newEmptyView, ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);

			if (mRefreshableView instanceof EmptyViewMethodAccessor) {
				((EmptyViewMethodAccessor) mRefreshableView).setEmptyViewInternal(newEmptyView);
			} else {
				mRefreshableView.setEmptyView(newEmptyView);
			}
			mEmptyView = newEmptyView;
		}
	}

	/**
	 * Pass-through method for {@link PullToRefreshBase#getRefreshableView()
	 * getRefreshableView()}.
	 * {@link AdapterView#setOnItemClickListener(OnItemClickListener)
	 * setOnItemClickListener(listener)}. This is just for convenience!
	 * 
	 * @param listener
	 *            - OnItemClickListener to use
	 */
	public void setOnItemClickListener(OnItemClickListener listener) {
		mRefreshableView.setOnItemClickListener(listener);
	}

	public final void setOnLastItemVisibleListener(OnLastItemVisibleListener listener) {
		mOnLastItemVisibleListener = listener;
	}

	public final void setOnScrollListener(OnScrollListener listener) {
		mOnScrollListener = listener;
	}

	public final void setScrollEmptyView(boolean doScroll) {
		mScrollEmptyView = doScroll;
	}

	/**
	 * Sets whether an indicator graphic should be displayed when the View is in
	 * a state where a Pull-to-Refresh can happen. An example of this state is
	 * when the Adapter View is scrolled to the top and the mode is set to
	 * {@link Mode#PULL_DOWN_TO_REFRESH}
	 * 
	 * @param showIndicator
	 *            - true if the indicators should be shown.
	 */
	public void setShowIndicator(boolean showIndicator) {
		mShowIndicator = showIndicator;

		if (getShowIndicatorInternal()) {
			// If we're set to Show Indicator, add/update them
			addIndicatorViews();
		} else {
			// If not, then remove then
			removeIndicatorViews();
		}
	};

	@Override
	void onPullToRefresh() {
		super.onPullToRefresh();

		if (getShowIndicatorInternal()) {
			switch (getCurrentMode()) {
				case PULL_UP_TO_REFRESH:
					mIndicatorIvBottom.pullToRefresh();
					break;
				case PULL_DOWN_TO_REFRESH:
					mIndicatorIvTop.pullToRefresh();
					break;
			}
		}
	}

	void onRefreshing(boolean doScroll) {
		super.onRefreshing(doScroll);

		if (getShowIndicatorInternal()) {
			updateIndicatorViewsVisibility();
		}
	}

	@Override
	void onReleaseToRefresh() {
		super.onReleaseToRefresh();

		if (getShowIndicatorInternal()) {
			switch (getCurrentMode()) {
				case PULL_UP_TO_REFRESH:
					mIndicatorIvBottom.releaseToRefresh();
					break;
				case PULL_DOWN_TO_REFRESH:
					mIndicatorIvTop.releaseToRefresh();
					break;
			}
		}
	}

	@Override
	void onReset() {
		super.onReset();

		if (getShowIndicatorInternal()) {
			updateIndicatorViewsVisibility();
		}
	}

	@Override
	protected void handleStyledAttributes(TypedArray a) {
		// Set Show Indicator to the XML value, or default value
		mShowIndicator = a.getBoolean(R.styleable.PullToRefresh_ptrShowIndicator, !isPullToRefreshOverScrollEnabled());
	}

	protected boolean isReadyForPullDown() {
		return isFirstItemVisible();
	}

	protected boolean isReadyForPullUp() {
		return isLastItemVisible();
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		if (null != mEmptyView && !mScrollEmptyView) {
			mEmptyView.scrollTo(-l, -t);
		}
	}

	@Override
	protected void updateUIForMode() {
		super.updateUIForMode();

		// Check Indicator Views consistent with new Mode
		if (getShowIndicatorInternal()) {
			addIndicatorViews();
		} else {
			removeIndicatorViews();
		}
	}

	private void addIndicatorViews() {
		Mode mode = getMode();
		FrameLayout refreshableViewWrapper = getRefreshableViewWrapper();

		if (mode.canPullDown() && null == mIndicatorIvTop) {
			// If the mode can pull down, and we don't have one set already
			mIndicatorIvTop = new IndicatorLayout(getContext(), Mode.PULL_DOWN_TO_REFRESH);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			params.rightMargin = getResources().getDimensionPixelSize(R.dimen.indicator_right_padding);
			params.gravity = Gravity.TOP | Gravity.RIGHT;
			refreshableViewWrapper.addView(mIndicatorIvTop, params);

		} else if (!mode.canPullDown() && null != mIndicatorIvTop) {
			// If we can't pull down, but have a View then remove it
			refreshableViewWrapper.removeView(mIndicatorIvTop);
			mIndicatorIvTop = null;
		}

		if (mode.canPullUp() && null == mIndicatorIvBottom) {
			// If the mode can pull down, and we don't have one set already
			mIndicatorIvBottom = new IndicatorLayout(getContext(), Mode.PULL_UP_TO_REFRESH);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			params.rightMargin = getResources().getDimensionPixelSize(R.dimen.indicator_right_padding);
			params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
			refreshableViewWrapper.addView(mIndicatorIvBottom, params);

		} else if (!mode.canPullUp() && null != mIndicatorIvBottom) {
			// If we can't pull down, but have a View then remove it
			refreshableViewWrapper.removeView(mIndicatorIvBottom);
			mIndicatorIvBottom = null;
		}
	}

	private boolean getShowIndicatorInternal() {
		return mShowIndicator && isPullToRefreshEnabled();
	}

	private boolean isFirstItemVisible() {
		final Adapter adapter = mRefreshableView.getAdapter();

		if (null == adapter || adapter.isEmpty()) {
			if (DEBUG) {
				Log.d(LOG_TAG, "isFirstItemVisible. Empty View.");
			}
			return true;

		} else {

			/**
			 * This check should really just be:
			 * mRefreshableView.getFirstVisiblePosition() == 0, but PtRListView
			 * internally use a HeaderView which messes the positions up. For
			 * now we'll just add one to account for it and rely on the inner
			 * condition which checks getTop().
			 */
			if (mRefreshableView.getFirstVisiblePosition() <= 1) {
				final View firstVisibleChild = mRefreshableView.getChildAt(0);
				if (firstVisibleChild != null) {
					return firstVisibleChild.getTop() >= mRefreshableView.getTop();
				}
			}
		}

		return false;
	}

	private boolean isLastItemVisible() {
		final Adapter adapter = mRefreshableView.getAdapter();

		if (null == adapter || adapter.isEmpty()) {
			if (DEBUG) {
				Log.d(LOG_TAG, "isLastItemVisible. Empty View.");
			}
			return true;
		} else {
			final int lastItemPosition = mRefreshableView.getCount() - 1;
			final int lastVisiblePosition = mRefreshableView.getLastVisiblePosition();

			if (DEBUG) {
				Log.d(LOG_TAG, "isLastItemVisible. Last Item Position: " + lastItemPosition + " Last Visible Pos: "
						+ lastVisiblePosition);
			}

			/**
			 * This check should really just be: lastVisiblePosition ==
			 * lastItemPosition, but PtRListView internally uses a FooterView
			 * which messes the positions up. For me we'll just subtract one to
			 * account for it and rely on the inner condition which checks
			 * getBottom().
			 */
			if (lastVisiblePosition >= lastItemPosition - 1) {
				final int childIndex = lastVisiblePosition - mRefreshableView.getFirstVisiblePosition();
				final View lastVisibleChild = mRefreshableView.getChildAt(childIndex);
				if (lastVisibleChild != null) {
					return lastVisibleChild.getBottom() <= mRefreshableView.getBottom();
				}
			}
		}

		return false;
	}

	private void removeIndicatorViews() {
		if (null != mIndicatorIvTop) {
			getRefreshableViewWrapper().removeView(mIndicatorIvTop);
			mIndicatorIvTop = null;
		}

		if (null != mIndicatorIvBottom) {
			getRefreshableViewWrapper().removeView(mIndicatorIvBottom);
			mIndicatorIvBottom = null;
		}
	}

	private void updateIndicatorViewsVisibility() {
		if (null != mIndicatorIvTop) {
			if (!isRefreshing() && isReadyForPullDown()) {
				if (!mIndicatorIvTop.isVisible()) {
					mIndicatorIvTop.show();
				}
			} else {
				if (mIndicatorIvTop.isVisible()) {
					mIndicatorIvTop.hide();
				}
			}
		}

		if (null != mIndicatorIvBottom) {
			if (!isRefreshing() && isReadyForPullUp()) {
				if (!mIndicatorIvBottom.isVisible()) {
					mIndicatorIvBottom.show();
				}
			} else {
				if (mIndicatorIvBottom.isVisible()) {
					mIndicatorIvBottom.hide();
				}
			}
		}
	}
}
