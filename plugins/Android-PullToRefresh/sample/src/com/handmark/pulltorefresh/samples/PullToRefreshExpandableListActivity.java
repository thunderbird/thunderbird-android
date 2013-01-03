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
package com.handmark.pulltorefresh.samples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshExpandableListView;

public final class PullToRefreshExpandableListActivity extends ExpandableListActivity {
	private static final String KEY = "key";

	private List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
	private List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();

	private PullToRefreshExpandableListView mPullRefreshListView;
	private SimpleExpandableListAdapter mAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ptr_expandable_list);

		mPullRefreshListView = (PullToRefreshExpandableListView) findViewById(R.id.pull_refresh_expandable_list);

		// Set a listener to be invoked when the list should be refreshed.
		mPullRefreshListView.setOnRefreshListener(new OnRefreshListener<ExpandableListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ExpandableListView> refreshView) {
				// Do work to refresh the list here.
				new GetDataTask().execute();
			}
		});

		for (String group : mGroupStrings) {
			Map<String, String> groupMap1 = new HashMap<String, String>();
			groupData.add(groupMap1);
			groupMap1.put(KEY, group);

			List<Map<String, String>> childList = new ArrayList<Map<String, String>>();
			for (String string : mChildStrings) {
				Map<String, String> childMap = new HashMap<String, String>();
				childList.add(childMap);
				childMap.put(KEY, string);
			}
			childData.add(childList);
		}

		mAdapter = new SimpleExpandableListAdapter(this, groupData, android.R.layout.simple_expandable_list_item_1,
				new String[] { KEY }, new int[] { android.R.id.text1 }, childData,
				android.R.layout.simple_expandable_list_item_2, new String[] { KEY }, new int[] { android.R.id.text1 });
		setListAdapter(mAdapter);
	}

	private class GetDataTask extends AsyncTask<Void, Void, String[]> {

		@Override
		protected String[] doInBackground(Void... params) {
			// Simulates a background job.
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			return mChildStrings;
		}

		@Override
		protected void onPostExecute(String[] result) {
			Map<String, String> newMap = new HashMap<String, String>();
			newMap.put(KEY, "Added after refresh...");
			groupData.add(newMap);

			List<Map<String, String>> childList = new ArrayList<Map<String, String>>();
			for (String string : mChildStrings) {
				Map<String, String> childMap = new HashMap<String, String>();
				childMap.put(KEY, string);
				childList.add(childMap);
			}
			childData.add(childList);

			mAdapter.notifyDataSetChanged();

			// Call onRefreshComplete when the list has been refreshed.
			mPullRefreshListView.onRefreshComplete();

			super.onPostExecute(result);
		}
	}

	private String[] mChildStrings = { "Child One", "Child Two", "Child Three", "Child Four", "Child Five", "Child Six" };

	private String[] mGroupStrings = { "Group One", "Group Two", "Group Three" };
}
