package com.fsck.k9.helper;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.fsck.k9.R;

public class ActionBarNavigationSpinner extends ArrayAdapter<String> implements SpinnerAdapter {

    public static final Long AB_NAVIGATION_INBOX = 0l;
    public static final Long AB_NAVIGATION_FOLDERS = 1l;
    public static final Long AB_NAVIGATION_ACCOUNTS = 2l;

    private String mTitle = "";
    private String mSubTitle = "";

    private Long[] mIds;

    private Context mContext;

    public ActionBarNavigationSpinner(Context context, String[] objects, Long[] ids) {
        super(context, R.layout.actionbar_spinner,
              android.R.id.text1, objects);
        setDropDownViewResource(android.R.layout.simple_list_item_1);
        mIds = new Long[ids.length];
        mIds = ids;
        this.mContext = context;
    }

    public boolean setTitle(String title) {
        if (!title.equals(mTitle)) {
            mTitle = title;
            notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    public boolean setSubTitle(String subtitle) {
        if (!subtitle.equals(mSubTitle)) {
            mSubTitle = subtitle;
            notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        // TODO Auto-generated method stub
        return super.getDropDownView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        return super.getView(position, convertView, parent);
    }

    @Override
    public long getItemId(int position) {
        return mIds[position];
    }
}
