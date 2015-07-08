package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalStore;

import java.util.ArrayList;
import java.util.Map;

/**
 * This Activity shows a list of the Account's known IMAP keywords and their corresponding tag names
 */
public class TagMapping extends K9ListActivity {

    private ListView mListView;
    private LayoutInflater mInflater;

    private TagMappingListAdapter mAdapter;

    private TagMappingViewHolder mLastEdited;
    private int mLastEditedPosition = -1;

    private static LocalStore localStoreRef;
    private static Account mAccount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = getLayoutInflater();

        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra("account");

        if (mAccount == null) {
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        if (localStoreRef == null) {
            try {
                localStoreRef = LocalStore.getInstance(mAccount, this);
            } catch (MessagingException e) {
                /* FIXME what to do here? */
            }
        }

        setContentView(R.layout.folder_list);

        mListView = getListView();
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(false);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Flag f = (Flag) mAdapter.getItem(position);
                //onOpenFolder(((FolderInfoHolder)mAdapter.getItem(position)).name);

                if (mLastEdited != null) {
                    saveChanges();
                    mLastEdited.hideEditables();
                }

                TagMappingViewHolder holder = (TagMappingViewHolder) view.getTag();
                holder.showEditables(f);

                mLastEdited = holder;
                mLastEditedPosition = position;
            }
        });
        registerForContextMenu(mListView);
    }

    private void initializeActivityView() {
        mAdapter = new TagMappingListAdapter();

        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter == null) {
            initializeActivityView();
        }

        //mAdapter.mListener.onResume(this);
    }

    @Override
    public void onBackPressed() {
        saveChanges();
        super.onBackPressed();
        /* TODO need to update the MessageHeader */
    }

    private void saveChanges() {
        if (mLastEdited != null && mLastEditedPosition >= 0) {
            Flag f = (Flag) mAdapter.getItem(mLastEditedPosition);
            String newTagName = mLastEdited.editTagName.getText().toString();

            f.setTagName(newTagName);
        }
    }

    class TagMappingListAdapter extends BaseAdapter {
        private Map<String, Flag> mTagMap = localStoreRef.getTagMappings();
        private ArrayList<String> keys;

        private ArrayList<String> getKeys() {
            if (keys == null) {
                keys = new ArrayList<String>(mTagMap.keySet());
            }
            return keys;
        }

        public Object getItem(int position) {
            return mTagMap.get(getKeys().get(position));
        }

        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        public int getCount() {
            return getKeys().size();
        }

        @Override
        public boolean isEnabled(int item) {
            return true;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (position <= getCount()) {
                return  getItemView(position, convertView, parent);
            } else {
                Log.e(K9.LOG_TAG, "getView with illegal positon=" + position
                        + " called! count is only " + getCount());
                return null;
            }
        }

        public View getItemView(int itemPosition, View convertView, ViewGroup parent) {
            Flag flag = (Flag) getItem(itemPosition);
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.tagmapping_list_item, parent, false);
            }

            TagMappingViewHolder holder = (TagMappingViewHolder) view.getTag();

            if (holder == null) {
                holder = new TagMappingViewHolder();
                holder.tagName = (TextView) view.findViewById(R.id.tag_name);
                holder.keywordName = (TextView) view.findViewById(R.id.keyword_name);
                holder.editTagName = (EditText) view.findViewById(R.id.edit_tag_name);
                //holder.tagColor = view.findViewById(R.id.tag_color);

                holder.tagMappingListItemLayout = (LinearLayout)view.findViewById(R.id.tagmapping_list_item_layout);
                holder.tagMappingEditablesLayout = (LinearLayout)view.findViewById(R.id.tagmapping_editables_layout);

                view.setTag(holder);
            }

            holder.tagName.setText(flag.tagName());
            holder.tagName.setVisibility(View.VISIBLE);
            holder.keywordName.setText(flag.name());
            holder.keywordName.setVisibility(View.VISIBLE);

            holder.hideEditables();
            if (mLastEditedPosition == itemPosition) {
                holder.showEditables(flag);
                holder.editTagName.requestFocus(); /* TODO put cursor at the end */
            }

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public boolean isItemSelectable(int position) {
            return true;
        }
    }

    static class TagMappingViewHolder {
        public TextView tagName;
        public TextView keywordName;
        public EditText editTagName;
        public View tagColor;

        public LinearLayout tagMappingListItemLayout;
        public LinearLayout tagMappingEditablesLayout;

        public void showEditables(Flag f) {
            tagName.setVisibility(View.INVISIBLE);

            tagMappingEditablesLayout.setVisibility(View.VISIBLE);
            editTagName.setText(f.tagName());
        }

        public void hideEditables() {
            tagName.setVisibility(View.VISIBLE);

            tagMappingEditablesLayout.setVisibility(View.GONE);
        }
    }

}
