package com.fsck.k9.activity;


import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.fsck.k9.ui.base.K9Activity;


public abstract class K9ListActivity extends K9Activity {
    protected ListAdapter adapter;
    protected ListView list;

    protected ListView getListView() {
        if (list == null) {
            list = findViewById(android.R.id.list);
            View emptyView = findViewById(android.R.id.empty);
            if (emptyView != null) {
                list.setEmptyView(emptyView);
            }
        }
        return list;
    }

    protected void setListAdapter(ListAdapter listAdapter) {
        if (list == null) {
            list = findViewById(android.R.id.list);
        }
        list.setAdapter(listAdapter);
        adapter = listAdapter;
    }
}
