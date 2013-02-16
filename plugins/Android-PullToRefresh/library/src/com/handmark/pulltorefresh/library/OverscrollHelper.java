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

import android.annotation.TargetApi;
import android.util.Log;
import android.view.View;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;

@TargetApi(9)
public final class OverscrollHelper {

	static final String LOG_TAG = "OverscrollHelper";
	static final float DEFAULT_OVERSCROLL_SCALE = 1f;

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function.
	 * <p/>
	 * This should only be used on AdapterView's such as ListView as it just
	 * calls through to overScrollBy() with the scrollRange = 0. AdapterView's
	 * do not have a scroll range (i.e. getScrollY() doesn't work).
	 * 
	 * @param view - PullToRefreshView that is calling this.
	 * @param deltaX - Change in X in pixels, passed through from from
	 *            overScrollBy call
	 * @param scrollX - Current X scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param deltaY - Change in Y in pixels, passed through from from
	 *            overScrollBy call
	 * @param scrollY - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param isTouchEvent - true if this scroll operation is the result of a
	 *            touch event, passed through from from overScrollBy call
	 */
	public static void overScrollBy(final PullToRefreshBase<?> view, final int deltaX, final int scrollX,
			final int deltaY, final int scrollY, final boolean isTouchEvent) {
		overScrollBy(view, deltaX, scrollX, deltaY, scrollY, 0, isTouchEvent);
	}

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function. This version of the call is used for Views that need to specify
	 * a Scroll Range but scroll back to it's edge correctly.
	 * 
	 * @param view - PullToRefreshView that is calling this.
	 * @param deltaX - Change in X in pixels, passed through from from
	 *            overScrollBy call
	 * @param scrollX - Current X scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param deltaY - Change in Y in pixels, passed through from from
	 *            overScrollBy call
	 * @param scrollY - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param scrollRange - Scroll Range of the View, specifically needed for
	 *            ScrollView
	 * @param isTouchEvent - true if this scroll operation is the result of a
	 *            touch event, passed through from from overScrollBy call
	 */
	public static void overScrollBy(final PullToRefreshBase<?> view, final int deltaX, final int scrollX,
			final int deltaY, final int scrollY, final int scrollRange, final boolean isTouchEvent) {
		overScrollBy(view, deltaX, scrollX, deltaY, scrollY, scrollRange, 0, DEFAULT_OVERSCROLL_SCALE, isTouchEvent);
	}

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function. This is the advanced version of the call.
	 * 
	 * @param view - PullToRefreshView that is calling this.
	 * @param deltaX - Change in X in pixels, passed through from from
	 *            overScrollBy call
	 * @param scrollX - Current X scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param deltaY - Change in Y in pixels, passed through from from
	 *            overScrollBy call
	 * @param scrollY - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param scrollRange - Scroll Range of the View, specifically needed for
	 *            ScrollView
	 * @param fuzzyThreshold - Threshold for which the values how fuzzy we
	 *            should treat the other values. Needed for WebView as it
	 *            doesn't always scroll back to it's edge. 0 = no fuzziness.
	 * @param scaleFactor - Scale Factor for overscroll amount
	 * @param isTouchEvent - true if this scroll operation is the result of a
	 *            touch event, passed through from from overScrollBy call
	 */
	public static void overScrollBy(final PullToRefreshBase<?> view, final int deltaX, final int scrollX,
			final int deltaY, final int scrollY, final int scrollRange, final int fuzzyThreshold,
			final float scaleFactor, final boolean isTouchEvent) {

		final int deltaValue, currentScrollValue, scrollValue;
		switch (view.getPullToRefreshScrollDirection()) {
			case HORIZONTAL:
				deltaValue = deltaX;
				scrollValue = scrollX;
				currentScrollValue = view.getScrollX();
				break;
			case VERTICAL:
			default:
				deltaValue = deltaY;
				scrollValue = scrollY;
				currentScrollValue = view.getScrollY();
				break;
		}

		// Check that OverScroll is enabled and that we're not currently
		// refreshing.
		if (view.isPullToRefreshOverScrollEnabled() && !view.isRefreshing()) {
			final Mode mode = view.getMode();

			// Check that Pull-to-Refresh is enabled, and the event isn't from
			// touch
			if (mode.permitsPullToRefresh() && !isTouchEvent && deltaValue != 0) {
				final int newScrollValue = (deltaValue + scrollValue);

				if (PullToRefreshBase.DEBUG) {
					Log.d(LOG_TAG, "OverScroll. DeltaX: " + deltaX + ", ScrollX: " + scrollX + ", DeltaY: " + deltaY
							+ ", ScrollY: " + scrollY + ", NewY: " + newScrollValue + ", ScrollRange: " + scrollRange
							+ ", CurrentScroll: " + currentScrollValue);
				}

				if (newScrollValue < (0 - fuzzyThreshold)) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if (mode.showHeaderLoadingLayout()) {
						// If we're currently at zero, we're about to start
						// overscrolling, so change the state
						if (currentScrollValue == 0) {
							view.setState(State.OVERSCROLLING);
						}

						view.setHeaderScroll((int) (scaleFactor * (currentScrollValue + newScrollValue)));
					}
				} else if (newScrollValue > (scrollRange + fuzzyThreshold)) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if (mode.showFooterLoadingLayout()) {
						// If we're currently at zero, we're about to start
						// overscrolling, so change the state
						if (currentScrollValue == 0) {
							view.setState(State.OVERSCROLLING);
						}

						view.setHeaderScroll((int) (scaleFactor * (currentScrollValue + newScrollValue - scrollRange)));
					}
				} else if (Math.abs(newScrollValue) <= fuzzyThreshold
						|| Math.abs(newScrollValue - scrollRange) <= fuzzyThreshold) {
					// Means we've stopped overscrolling, so scroll back to 0
					view.setState(State.RESET);
				}
			} else if (isTouchEvent && State.OVERSCROLLING == view.getState()) {
				// This condition means that we were overscrolling from a fling,
				// but the user has touched the View and is now overscrolling
				// from touch instead. We need to just reset.
				view.setState(State.RESET);
			}
		}
	}

	static boolean isAndroidOverScrollEnabled(View view) {
		return view.getOverScrollMode() != View.OVER_SCROLL_NEVER;
	}
}
