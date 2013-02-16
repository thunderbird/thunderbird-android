package com.fsck.k9.view;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshListView;


public class K9PullToRefreshListView extends PullToRefreshListView {

    public K9PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected ListView createListView(Context context, AttributeSet attrs) {
        return new ListView(context, attrs) {
            @Override
            public void onRestoreInstanceState(Parcelable state) {
                super.onRestoreInstanceState(state);

                /*
                 * Force the list view to apply the restored state instantly instead of
                 * asynchronously, so potential data changes (which in turn cause an internal
                 * position save/restore) don't overwrite our saved position.
                 */
                layoutChildren();
            }
        };
    }
}
