package com.fsck.k9.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

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
        super(context, R.layout.actionbar_spinner, objects);
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
     View row = convertView;

     if(row==null){
	 LayoutInflater inflater = LayoutInflater.from(mContext);
	 row=inflater.inflate(R.layout.actionbar_spinner, parent, false);
     }

     TextView title = (TextView) row.findViewById(R.id.actionbar_title_first);
     TextView subtitle = (TextView) row.findViewById(R.id.actionbar_title_sub);

     title.setText(mTitle);
     subtitle.setText(mSubTitle);

     return row;

    }

    @Override
    public long getItemId(int position) {
        return mIds[position];
    }

	public static ActionBarNavigationSpinner getDefaultSpinner(Context context) {
		return new ActionBarNavigationSpinner(context,
			new String[] {context.getString(R.string.special_mailbox_name_inbox),
				context.getString(R.string.folders_title),
				context.getString(R.string.accounts_title)},
            new Long[] {ActionBarNavigationSpinner.AB_NAVIGATION_INBOX,
			ActionBarNavigationSpinner.AB_NAVIGATION_FOLDERS,
			ActionBarNavigationSpinner.AB_NAVIGATION_ACCOUNTS });
	}
}
