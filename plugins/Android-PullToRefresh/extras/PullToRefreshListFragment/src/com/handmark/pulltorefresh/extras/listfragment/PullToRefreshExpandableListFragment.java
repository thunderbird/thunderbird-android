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
package com.handmark.pulltorefresh.extras.listfragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;

import com.handmark.pulltorefresh.library.PullToRefreshExpandableListView;

/**
 * A sample implementation of how to use {@link PullToRefreshExpandableListView}
 * with {@link ListFragment}. This implementation simply replaces the ListView
 * that {@code ListFragment} creates with a new
 * {@code PullToRefreshExpandableListView}. This means that ListFragment still
 * works 100% (e.g. <code>setListShown(...)</code> ).
 * <p/>
 * The new PullToRefreshListView is created in the method
 * {@link #onCreatePullToRefreshListView(LayoutInflater, Bundle)}. If you wish
 * to customise the {@code PullToRefreshExpandableListView} then override this
 * method and return your customised instance.
 * 
 * @author Chris Banes
 * 
 */
public class PullToRefreshExpandableListFragment extends PullToRefreshBaseListFragment<PullToRefreshExpandableListView> {

	protected PullToRefreshExpandableListView onCreatePullToRefreshListView(LayoutInflater inflater,
			Bundle savedInstanceState) {
		return new PullToRefreshExpandableListView(getActivity());
	}

}