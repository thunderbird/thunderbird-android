package com.fsck.k9.activity;


import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.fsck.k9.K9;


public abstract class K9ListActivity extends K9Activity {
    protected ListAdapter adapter;
    protected ListView list;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Shortcuts that work no matter what is selected
        if (K9.isUseVolumeKeysForListNavigation() &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {

            final ListView listView = getListView();

            int currentPosition = listView.getSelectedItemPosition();
            if (currentPosition == AdapterView.INVALID_POSITION || listView.isInTouchMode()) {
                currentPosition = listView.getFirstVisiblePosition();
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && currentPosition > 0) {
                listView.setSelection(currentPosition - 1);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
                    currentPosition < listView.getCount()) {
                listView.setSelection(currentPosition + 1);
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.isUseVolumeKeysForListNavigation() &&
                (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

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
