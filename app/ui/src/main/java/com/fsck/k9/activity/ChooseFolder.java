
package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import android.widget.TextView;
import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.FolderListFilter.FolderAdapter;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.ui.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.ui.folders.FolderIconProvider;
import timber.log.Timber;

import static java.util.Collections.emptyList;


public class ChooseFolder extends K9ListActivity {
    public static final String EXTRA_ACCOUNT = "com.fsck.k9.ChooseFolder_account";
    public static final String EXTRA_CUR_FOLDER = "com.fsck.k9.ChooseFolder_curfolder";
    public static final String EXTRA_SEL_FOLDER = "com.fsck.k9.ChooseFolder_selfolder";
    public static final String EXTRA_NEW_FOLDER = "com.fsck.k9.ChooseFolder_newfolder";
    public static final String EXTRA_MESSAGE = "com.fsck.k9.ChooseFolder_message";
    public static final String EXTRA_SHOW_CURRENT = "com.fsck.k9.ChooseFolder_showcurrent";
    public static final String EXTRA_SHOW_DISPLAYABLE_ONLY = "com.fsck.k9.ChooseFolder_showDisplayableOnly";
    public static final String RESULT_FOLDER_DISPLAY_NAME = "folderDisplayName";


    private String currentFolder;
    private String mSelectFolder;
    private Account mAccount;
    private MessageReference mMessageReference;
    private FolderListAdapter mAdapter;
    private ChooseFolderHandler mHandler = new ChooseFolderHandler();
    private boolean mHideCurrentFolder = true;
    private boolean mShowDisplayableOnly = false;

    /**
     * What folders to display.<br/>
     * Initialized to whatever is configured
     * but can be overridden via {@link #onOptionsItemSelected(MenuItem)}
     * while this activity is showing.
     */
    private Account.FolderMode mMode;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setLayout(R.layout.list_content_simple);

        getListView().setFastScrollEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        if (intent.hasExtra(EXTRA_MESSAGE)) {
            String messageReferenceString = intent.getStringExtra(EXTRA_MESSAGE);
            mMessageReference = MessageReference.parse(messageReferenceString);
        }
        currentFolder = intent.getStringExtra(EXTRA_CUR_FOLDER);
        mSelectFolder = intent.getStringExtra(EXTRA_SEL_FOLDER);
        if (intent.getStringExtra(EXTRA_SHOW_CURRENT) != null) {
            mHideCurrentFolder = false;
        }
        if (intent.getStringExtra(EXTRA_SHOW_DISPLAYABLE_ONLY) != null) {
            mShowDisplayableOnly = true;
        }
        if (currentFolder == null)
            currentFolder = "";

        mAdapter = new FolderListAdapter();
        setListAdapter(mAdapter);

        mMode = mAccount.getFolderTargetMode();
        MessagingController.getInstance(getApplication()).listFolders(mAccount, false, mListener);

        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FolderInfoHolder folder = mAdapter.getItem(position);
                if (folder == null) {
                    throw new AssertionError("Couldn't get item at adapter position " + position);
                }

                Intent result = new Intent();
                result.putExtra(EXTRA_ACCOUNT, mAccount.getUuid());
                result.putExtra(EXTRA_CUR_FOLDER, currentFolder);
                String targetFolder = folder.serverId;
                result.putExtra(EXTRA_NEW_FOLDER, targetFolder);
                if (mMessageReference != null) {
                    result.putExtra(EXTRA_MESSAGE, mMessageReference.toIdentityString());
                }
                result.putExtra(RESULT_FOLDER_DISPLAY_NAME, folder.displayName);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    class ChooseFolderHandler extends Handler {
        private static final int MSG_PROGRESS = 1;
        private static final int MSG_SET_SELECTED_FOLDER = 2;

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS: {
                    setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                    break;
                }
                case MSG_SET_SELECTED_FOLDER: {
                    getListView().setSelection(msg.arg1);
                    break;
                }
            }
        }

        public void progress(boolean progress) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_PROGRESS;
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void setSelectedFolder(int position) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SET_SELECTED_FOLDER;
            msg.arg1 = position;
            sendMessage(msg);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.folder_select_option, menu);
        configureFolderSearchView(menu);
        return true;
    }

    private void configureFolderSearchView(Menu menu) {
        final MenuItem folderMenuItem = menu.findItem(R.id.filter_folders);
        final SearchView folderSearchView = (SearchView) folderMenuItem.getActionView();
        folderSearchView.setQueryHint(getString(R.string.folder_list_filter_hint));
        folderSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                folderMenuItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.display_1st_class) {
            setDisplayMode(FolderMode.FIRST_CLASS);
            return true;
        } else if (id == R.id.display_1st_and_2nd_class) {
            setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
            return true;
        } else if (id == R.id.display_not_second_class) {
            setDisplayMode(FolderMode.NOT_SECOND_CLASS);
            return true;
        } else if (id == R.id.display_all) {
            setDisplayMode(FolderMode.ALL);
            return true;
        } else if (id == R.id.list_folders) {
            onRefresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void onRefresh() {
        MessagingController.getInstance(getApplication()).listFolders(mAccount, true, mListener);
    }

    private void setDisplayMode(FolderMode aMode) {
        mMode = aMode;
        //re-populate the list
        MessagingController.getInstance(getApplication()).listFolders(mAccount, false, mListener);
    }

    private MessagingListener mListener = new SimpleMessagingListener() {
        @Override
        public void listFoldersStarted(Account account) {
            if (!account.equals(mAccount)) {
                return;
            }
            mHandler.progress(true);
        }

        @Override
        public void listFoldersFailed(Account account, String message) {
            if (!account.equals(mAccount)) {
                return;
            }
            mHandler.progress(false);
        }

        @Override
        public void listFoldersFinished(Account account) {
            if (!account.equals(mAccount)) {
                return;
            }
            mHandler.progress(false);
        }
        @Override
        public void listFolders(Account account, List<LocalFolder> folders) {
            if (!account.equals(mAccount)) {
                return;
            }
            Account.FolderMode aMode = mMode;

            List<FolderInfoHolder> newFolders = new ArrayList<>();
            List<FolderInfoHolder> topFolders = new ArrayList<>();

            for (LocalFolder folder : folders) {
                String serverId = folder.getServerId();

                if (mHideCurrentFolder && serverId.equals(currentFolder)) {
                    continue;
                }
                if (account.getOutboxFolder().equals(serverId)) {
                    continue;
                }

                Folder.FolderClass fMode = folder.getDisplayClass();

                if ((aMode == FolderMode.FIRST_CLASS &&
                        fMode != Folder.FolderClass.FIRST_CLASS) || (
                            aMode == FolderMode.FIRST_AND_SECOND_CLASS &&
                            fMode != Folder.FolderClass.FIRST_CLASS &&
                            fMode != Folder.FolderClass.SECOND_CLASS) || (
                            aMode == FolderMode.NOT_SECOND_CLASS &&
                            fMode == Folder.FolderClass.SECOND_CLASS)) {
                    continue;
                }

                FolderInfoHolder folderDisplayData = new FolderInfoHolder(folder, account);

                if (folder.isInTopGroup()) {
                    topFolders.add(folderDisplayData);
                } else {
                    newFolders.add(folderDisplayData);
                }
            }

            final Comparator<FolderInfoHolder> comparator = new Comparator<FolderInfoHolder>() {
                @Override
                public int compare(FolderInfoHolder lhs, FolderInfoHolder rhs) {
                    int result = lhs.displayName.compareToIgnoreCase(rhs.displayName);
                    return (result != 0) ? result : lhs.displayName.compareTo(rhs.displayName);
                }
            };

            Collections.sort(topFolders, comparator);
            Collections.sort(newFolders, comparator);

            final List<FolderInfoHolder> folderList = new ArrayList<>(newFolders.size() + topFolders.size());

            folderList.addAll(topFolders);
            folderList.addAll(newFolders);

            int selectedFolder = -1;

            /*
             * We're not allowed to change the adapter from a background thread, so we collect the
             * folder names and update the adapter in the UI thread (see finally block).
             */
            try {
                int position = 0;
                for (FolderInfoHolder folder : folderList) {
                    if (mSelectFolder != null) {
                        /*
                         * Never select EXTRA_CUR_FOLDER (mFolder) if EXTRA_SEL_FOLDER
                         * (mSelectedFolder) was provided.
                         */

                        if (folder.serverId.equals(mSelectFolder)) {
                            selectedFolder = position;
                            break;
                        }
                    } else if (folder.serverId.equals(currentFolder)) {
                        selectedFolder = position;
                        break;
                    }
                    position++;
                }
            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Now we're in the UI-thread, we can safely change the contents of the adapter.
                        mAdapter.setFolders(folderList);
                    }
                });
            }

            if (selectedFolder != -1) {
                mHandler.setSelectedFolder(selectedFolder);
            }
        }
    };

    class FolderListAdapter extends BaseAdapter implements Filterable, FolderAdapter {
        private List<FolderInfoHolder> mFolders = emptyList();
        private List<FolderInfoHolder> mFilteredFolders = emptyList();
        private Filter mFilter = new FolderListFilter(this, mFolders);
        private FolderIconProvider folderIconProvider = new FolderIconProvider(getTheme());
        private CharSequence filterText;

        public FolderInfoHolder getItem(long position) {
            return getItem((int)position);
        }

        @Override
        public FolderInfoHolder getItem(int position) {
            return mFilteredFolders.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mFilteredFolders.get(position).folder.getDatabaseId();
        }

        @Override
        public int getCount() {
            return mFilteredFolders.size();
        }

        @Override
        public boolean isEnabled(int item) {
            return true;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position <= getCount()) {
                return  getItemView(position, convertView, parent);
            } else {
                Timber.e("getView with illegal position=%d called! count is only %d", position, getCount());
                return null;
            }
        }

        public View getItemView(int itemPosition, View convertView, ViewGroup parent) {
            FolderInfoHolder folder = getItem(itemPosition);
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = View.inflate(ChooseFolder.this, R.layout.choose_folder_list_item, null);
            }

            FolderViewHolder holder = (FolderViewHolder) view.getTag();

            if (holder == null) {
                holder = new FolderViewHolder();
                holder.folderName = view.findViewById(R.id.folder_name);
                holder.folderIcon = view.findViewById(R.id.folder_icon);
                holder.folderListItemLayout = view.findViewById(R.id.folder_list_item_layout);

                view.setTag(holder);
            }

            if (folder == null) {
                return view;
            }

            holder.folderName.setText(folder.displayName);
            holder.folderIcon.setImageResource(folderIconProvider.getFolderIcon(folder.folder.getType()));

            if (K9.isWrapFolderNames()) {
                holder.folderName.setEllipsize(null);
                holder.folderName.setSingleLine(false);
            }
            else {
                holder.folderName.setEllipsize(TextUtils.TruncateAt.START);
                holder.folderName.setSingleLine(true);
            }

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public Filter getFilter() {
            return mFilter;
        }

        @Override
        public void setFilteredFolders(CharSequence filterText, List<FolderInfoHolder> folders) {
            this.filterText = filterText;
            mFilteredFolders = folders;
            notifyDataSetChanged();
        }

        void setFolders(List<FolderInfoHolder> folders) {
            mFolders = folders;
            mFilter = new FolderListFilter(this, folders);
            mFilter.filter(filterText);
        }
    }

    static class FolderViewHolder {
        TextView folderName;
        ImageView folderIcon;
        LinearLayout folderListItemLayout;
    }
}
