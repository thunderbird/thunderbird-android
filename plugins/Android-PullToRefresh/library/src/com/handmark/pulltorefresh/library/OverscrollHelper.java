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

@TargetApi(9)
final class OverscrollHelper {

	static final String LOG_TAG = "OverscrollHelper";
	static final float DEFAULT_OVERSCROLL_SCALE = 1f;

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function.
	 * 
	 * This should only be used on AdapterView's such as ListView as it just
	 * calls through to overScrollBy() with the scrollRange = 0. AdapterView's
	 * do not have a scroll range (i.e. getScrollY() doesn't work).
	 * 
	 * @param view
	 *            - PullToRefreshView that is calling this.
	 * @param deltaY
	 *            - Change in Y in pixels, passed through from from overScrollBy
	 *            call
	 * @param scrollY
	 *            - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param isTouchEvent
	 *            - true if this scroll operation is the result of a touch
	 *            event, passed through from from overScrollBy call
	 */
	static void overScrollBy(final PullToRefreshBase<?> view, final int deltaY, final int scrollY,
			final boolean isTouchEvent) {
		overScrollBy(view, deltaY, scrollY, 0, isTouchEvent);
	}

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function. This version of the call is used for Views that need to specify
	 * a Scroll Range but scroll back to it's edge correctly.
	 * 
	 * @param view
	 *            - PullToRefreshView that is calling this.
	 * @param deltaY
	 *            - Change in Y in pixels, passed through from from overScrollBy
	 *            call
	 * @param scrollY
	 *            - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param scrollRange
	 *            - Scroll Range of the View, specifically needed for ScrollView
	 * @param isTouchEvent
	 *            - true if this scroll operation is the result of a touch
	 *            event, passed through from from overScrollBy call
	 */
	static void overScrollBy(final PullToRefreshBase<?> view, final int deltaY, final int scrollY,
			final int scrollRange, final boolean isTouchEvent) {
		overScrollBy(view, deltaY, scrollY, scrollRange, 0, DEFAULT_OVERSCROLL_SCALE, isTouchEvent);
	}

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function. This is the advanced version of the call.
	 * 
	 * @param view
	 *            - PullToRefreshView that is calling this.
	 * @param deltaY
	 *            - Change in Y in pixels, passed through from from overScrollBy
	 *            call
	 * @param scrollY
	 *            - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param scrollRange
	 *            - Scroll Range of the View, specifically needed for ScrollView
	 * @param fuzzyThreshold
	 *            - Threshold for which the values how fuzzy we should treat the
	 *            other values. Needed for WebView as it doesn't always scroll
	 *            back to it's edge. 0 = no fuzziness.
	 * @param scaleFactor
	 *            - Scale Factor for overscroll amount
	 * @param isTouchEvent
	 *            - true if this scroll operation is the result of a touch
	 *            event, passed through from from overScrollBy call
	 */
	static void overScrollBy(final PullToRefreshBase<?> view, final int deltaY, final int scrollY,
			final int scrollRange, final int fuzzyThreshold, final float scaleFactor, final boolean isTouchEvent) {

		// Check that OverScroll is enabled
		if (view.isPullToRefreshOverScrollEnabled()) {
			final Mode mode = view.getMode();

			// Check that we're not disabled, and the event isn't from touch
			if (mode != Mode.DISABLED && !isTouchEvent && deltaY != 0) {
				final int newY = (deltaY + scrollY);

				if (PullToRefreshBase.DEBUG) {
					Log.d(LOG_TAG, "OverScroll. DeltaY: " + deltaY + ", ScrollY: " + scrollY + ", NewY: " + newY
							+ ", ScrollRange: " + scrollRange);
				}

				if (newY < (0 - fuzzyThreshold)) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if (mode.canPullDown()) {
						view.setHeaderScroll((int) (scaleFactor * (view.getScrollY() + newY)));
					}
				} else if (newY > (scrollRange + fuzzyThreshold)) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if (mode.canPullUp()) {
						view.setHeaderScroll((int) (scaleFactor * (view.getScrollY() + newY - scrollRange)));
					}
				} else if (Math.abs(newY) <= fuzzyThreshold || Math.abs(newY - scrollRange) <= fuzzyThreshold) {
					// Means we've stopped overscrolling, so scroll back to 0
					view.smoothScrollToLonger(0);
				}
			}
		}
	}

	static boolean isAndroidOverScrollEnabled(View view) {
		return view.getOverScrollMode() != View.OVER_SCROLL_NEVER;
	}
}
