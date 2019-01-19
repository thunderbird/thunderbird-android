
package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.ui.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;


public class ChooseFolder extends K9ListActivity {
    public static final String EXTRA_ACCOUNT = "com.fsck.k9.ChooseFolder_account";
    public static final String EXTRA_CUR_FOLDER = "com.fsck.k9.ChooseFolder_curfolder";
    public static final String EXTRA_SEL_FOLDER = "com.fsck.k9.ChooseFolder_selfolder";
    public static final String EXTRA_NEW_FOLDER = "com.fsck.k9.ChooseFolder_newfolder";
    public static final String EXTRA_MESSAGE = "com.fsck.k9.ChooseFolder_message";
    public static final String EXTRA_SHOW_CURRENT = "com.fsck.k9.ChooseFolder_showcurrent";
    public static final String EXTRA_SHOW_DISPLAYABLE_ONLY = "com.fsck.k9.ChooseFolder_showDisplayableOnly";
    public static final String RESULT_FOLDER_DISPLAY_NAME = "folderDisplayName";


    String currentFolder;
    String mSelectFolder;
    Account mAccount;
    MessageReference mMessageReference;
    ArrayAdapter<FolderDisplayData> mAdapter;
    private ChooseFolderHandler mHandler = new ChooseFolderHandler();
    boolean mHideCurrentFolder = true;
    boolean mShowDisplayableOnly = false;

    /**
     * What folders to display.<br/>
     * Initialized to whatever is configured
     * but can be overridden via {@link #onOptionsItemSelected(MenuItem)}
     * while this activity is showing.
     */
    private Account.FolderMode mMode;

    /**
     * Current filter used by our ArrayAdapter.<br/>
     * Created on the fly and invalidated if a new
     * set of folders is chosen via {@link #onOptionsItemSelected(MenuItem)}
     */
    private FolderListFilter<String> mMyFilter = null;


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

        mAdapter = new ArrayAdapter<FolderDisplayData>(this, android.R.layout.simple_list_item_1) {
            private Filter myFilter = null;

            @Override
            public Filter getFilter() {
                if (myFilter == null) {
                    myFilter = new FolderListFilter<>(this);
                }
                return myFilter;
            }
        };

        setListAdapter(mAdapter);

        mMode = mAccount.getFolderTargetMode();
        MessagingController.getInstance(getApplication()).listFolders(mAccount, false, mListener);

        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FolderDisplayData folder = mAdapter.getItem(position);
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
        // invalidate the current filter as it is working on an inval
        if (mMyFilter != null) {
            mMyFilter.invalidate();
        }
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

            List<FolderDisplayData> newFolders = new ArrayList<>();
            List<FolderDisplayData> topFolders = new ArrayList<>();

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

                long id = folder.getDatabaseId();
                String name = folder.getName();
                String displayName = buildDisplayName(account, serverId, name);
                FolderDisplayData folderDisplayData = new FolderDisplayData(id, serverId, displayName);

                if (folder.isInTopGroup()) {
                    topFolders.add(folderDisplayData);
                } else {
                    newFolders.add(folderDisplayData);
                }
            }

            final Comparator<FolderDisplayData> comparator = new Comparator<FolderDisplayData>() {
                @Override
                public int compare(FolderDisplayData lhs, FolderDisplayData rhs) {
                    int result = lhs.displayName.compareToIgnoreCase(rhs.displayName);
                    return (result != 0) ? result : lhs.displayName.compareTo(rhs.displayName);
                }
            };

            Collections.sort(topFolders, comparator);
            Collections.sort(newFolders, comparator);

            final List<FolderDisplayData> folderList = new ArrayList<>(newFolders.size() + topFolders.size());

            folderList.addAll(topFolders);
            folderList.addAll(newFolders);

            int selectedFolder = -1;

            /*
             * We're not allowed to change the adapter from a background thread, so we collect the
             * folder names and update the adapter in the UI thread (see finally block).
             */
            try {
                int position = 0;
                for (FolderDisplayData folder : folderList) {
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
                        mAdapter.clear();
                        mAdapter.addAll(folderList);
                        mAdapter.notifyDataSetChanged();

                        /*
                         * Only enable the text filter after the list has been
                         * populated to avoid possible race conditions because our
                         * FolderListFilter isn't really thread-safe.
                         */
                        getListView().setTextFilterEnabled(true);
                    }
                });
            }

            if (selectedFolder != -1) {
                mHandler.setSelectedFolder(selectedFolder);
            }
        }
    };

    private String buildDisplayName(Account account, String serverId, String name) {
        if (account.getInboxFolder().equals(serverId)) {
            return getString(R.string.special_mailbox_name_inbox);
        } else {
            return name;
        }
    }


    static class FolderDisplayData {
        final long id;
        final String serverId;
        final String displayName;

        FolderDisplayData(long id, String serverId, String displayName) {
            this.id = id;
            this.serverId = serverId;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
