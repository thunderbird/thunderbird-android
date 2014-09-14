package com.fsck.k9.view;

import android.content.Context;
import android.graphics.Canvas;
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

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (getAdapter() != null) {
                    int count = getChildCount();
                    int itemCount = getAdapter().getCount();

                    /*
                     * 2013-03-18 - cketti
                     *
                     * Work around a bug in ListView (?) that leads to a crash deep inside the
                     * framework code.
                     * I didn't track down the exact cause of this. My best guess is that we change
                     * the data (remove items) while the ListView isn't visible. Probably because
                     * the view is hidden, layoutChildren() is never called and when this method
                     * runs the layout contains more item views than the adapter contains elements
                     * and bad things(tm) happen.
                     */
                    if (itemCount < count) {
                        layoutChildren();
                    }
                }

                super.dispatchDraw(canvas);
            }
        };
    }
}
