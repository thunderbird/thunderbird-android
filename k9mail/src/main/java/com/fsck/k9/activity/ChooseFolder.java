
package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
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
    public static final String EXTRA_SHOW_FOLDER_NONE = "com.fsck.k9.ChooseFolder_showOptionNone";
    public static final String EXTRA_SHOW_DISPLAYABLE_ONLY = "com.fsck.k9.ChooseFolder_showDisplayableOnly";
    public static final String EXTRA_SHOW_AS_TREE_STRUCTURE = "com.fsck.k9.ChooseFolder_showAsTreeStructure";


    String mFolderId;
    String mSelectFolder;
    Account mAccount;
    MessageReference mMessageReference;

    private class FolderIdNamePair {
        @NonNull final String id;
        @NonNull String name;
        final int level;
        String paddedName;

        private FolderIdNamePair(@NonNull String folderId, @NonNull String folderName, int depthLevel) {
            id = folderId;
            name = folderName;
            level = depthLevel;
            updatePaddedName();
        }

        private void updatePaddedName() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < level; i++) {
                builder.append("    ");
            }
            builder.append(name);
            paddedName = builder.toString();
        }

        void setName(String newName) {
            name = newName;
            updatePaddedName();
        }

        public String toString() {
            return paddedName;
        }

        public boolean equals(Object o) {
            return o instanceof FolderIdNamePair && ((FolderIdNamePair) o).id.equals(id);
        }

        public int hashCode() {
            return id.hashCode();
        }
    }

    //TODO: Change this to use <ID, Name> tuple with ID for lookup and Name for rendering.
    ArrayAdapter<FolderIdNamePair> mAdapter;
    private ChooseFolderHandler mHandler = new ChooseFolderHandler();
    String mHeldInbox = null;
    boolean mHideCurrentFolder = true;
    boolean mShowOptionNone = false;
    boolean mShowDisplayableOnly = false;
    boolean mShowAsTreeStructure = false;

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
        setContentView(R.layout.list_content_simple);

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
        mFolderId = intent.getStringExtra(EXTRA_CUR_FOLDER);
        mSelectFolder = intent.getStringExtra(EXTRA_SEL_FOLDER);
        if (intent.getStringExtra(EXTRA_SHOW_CURRENT) != null) {
            mHideCurrentFolder = false;
        }
        if (intent.getStringExtra(EXTRA_SHOW_FOLDER_NONE) != null) {
            mShowOptionNone = true;
        }
        if (intent.getStringExtra(EXTRA_SHOW_DISPLAYABLE_ONLY) != null) {
            mShowDisplayableOnly = true;
        }
        if (intent.getStringExtra(EXTRA_SHOW_AS_TREE_STRUCTURE) != null) {
            mShowAsTreeStructure = true;
        }
        if (mFolderId == null)
            mFolderId = "";

        mAdapter = new ArrayAdapter<FolderIdNamePair>(this, android.R.layout.simple_list_item_1) {
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
                Intent result = new Intent();
                result.putExtra(EXTRA_ACCOUNT, mAccount.getUuid());
                result.putExtra(EXTRA_CUR_FOLDER, mFolderId);
                String destFolderName = ((TextView)view).getText().toString();
                if (mHeldInbox != null && getString(R.string.special_mailbox_name_inbox).equals(destFolderName)) {
                    destFolderName = mHeldInbox;
                }
                result.putExtra(EXTRA_NEW_FOLDER, destFolderName);
                if (mMessageReference != null) {
                    result.putExtra(EXTRA_MESSAGE, mMessageReference.toIdentityString());
                }
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    private class ChooseFolderHandler extends Handler {
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

        void setSelectedFolder(int position) {
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
        switch (item.getItemId()) {
            case R.id.display_1st_class: {
                setDisplayMode(FolderMode.FIRST_CLASS);
                return true;
            }
            case R.id.display_1st_and_2nd_class: {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
                return true;
            }
            case R.id.display_not_second_class: {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS);
                return true;
            }
            case R.id.display_all: {
                setDisplayMode(FolderMode.ALL);
                return true;
            }
            case R.id.list_folders: {
                onRefresh();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
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
            List<FolderIdNamePair> topFolders = new ArrayList<>();
            Map<String, String> folderMap = new HashMap<>();

            for (Folder folder : folders) {
                String folderId = folder.getId();
                String folderName = folder.getName();

                // Inbox needs to be compared case-insensitively
                boolean showNotShowInTopSection = mHideCurrentFolder && (folderId.equals(mFolderId) || (
                        mAccount.getInboxFolderId().equalsIgnoreCase(mFolderId) &&
                                mAccount.getInboxFolderId().equalsIgnoreCase(folderId)));
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

                if (folder.isInTopGroup() && !showNotShowInTopSection) {
                    topFolders.add(new FolderIdNamePair(folderId, folderName, 0));
                }
                folderMap.put(folder.getId(), folder.getName());
            }

            SortedMap<String, SortedMap> folderOrdering = buildHierarchy(folders);

            List<FolderIdNamePair> newFolders = new ArrayList<>();
            generateFolderPairs(newFolders, folderMap, folderOrdering, 0);

            final Comparator<FolderIdNamePair> comparator = new Comparator<FolderIdNamePair>() {
                @Override
                public int compare(FolderIdNamePair s1, FolderIdNamePair s2) {
                    int ret = s1.name.compareToIgnoreCase(s2.name);
                    return (ret != 0) ? ret : s1.name.compareTo(s2.name);
                }
            };

            Collections.sort(topFolders, comparator);



            List<FolderIdNamePair> localFolders = new ArrayList<>(newFolders.size() +
                    topFolders.size() + ((mShowOptionNone) ? 1 : 0));

            if (mShowOptionNone) {
                localFolders.add(new FolderIdNamePair(K9.FOLDER_NONE, K9.FOLDER_NONE, 0));
            }

            localFolders.addAll(topFolders);
            localFolders.addAll(newFolders);

            int selectedFolder = -1;

            /*
             * We're not allowed to change the adapter from a background thread, so we collect the
             * folder ids and update the adapter in the UI thread (see finally block).
             */
            final List<FolderIdNamePair> folderList = new ArrayList<>();
            try {
                int position = 0;
                for (FolderIdNamePair folderTuple : localFolders) {
                    if (mAccount.getInboxFolderId().equalsIgnoreCase(folderTuple.id)) {
                        folderTuple.setName(getString(R.string.special_mailbox_name_inbox));
                        folderList.add(folderTuple);
                        mHeldInbox = folderTuple.id;
                    } else if (!K9.ERROR_FOLDER_ID.equals(folderTuple.id) &&
                            !account.getOutboxFolderId().equals(folderTuple.id)) {
                        folderList.add(folderTuple);
                    }

                    if (mSelectFolder != null) {
                        /*
                         * Never select EXTRA_CUR_FOLDER (mFolderId) if EXTRA_SEL_FOLDER
                         * (mSelectedFolder) was provided.
                         */

                        if (folderTuple.id.equals(mSelectFolder)) {
                            selectedFolder = position;
                        }
                    } else if (folderTuple.id.equals(mFolderId) || (
                            mAccount.getInboxFolderId().equalsIgnoreCase(mFolderId) &&
                            mAccount.getInboxFolderId().equalsIgnoreCase(folderTuple.id))) {
                        selectedFolder = position;
                    }
                    position++;
                }
            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Now we're in the UI-thread, we can safely change the contents of the adapter.
                        mAdapter.clear();
                        for (FolderIdNamePair folderTuple: folderList) {
                            mAdapter.add(folderTuple);
                        }

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

        private SortedMap<String, SortedMap> buildHierarchy(List<LocalFolder> folders) {
            SortedMap<String, SortedMap> folderStructure = new TreeMap<>();
            for (Folder folder: folders) {
                if (folder.getParentId() == null || "".equals(folder.getParentId())) {
                    folderStructure.put(folder.getId(), buildHierarchy(folder.getId(), folders));
                }
            }
            return folderStructure;
        }

        private SortedMap<String, SortedMap> buildHierarchy(String parentId, List<LocalFolder> folders) {
            SortedMap<String, SortedMap> folderStructure = new TreeMap<>();
            for (Folder folder: folders) {
                if (parentId.equals(folder.getParentId())) {
                    folderStructure.put(folder.getId(), buildHierarchy(folder.getId(), folders));
                }
            }
            return folderStructure;
        }

        private void generateFolderPairs(List<FolderIdNamePair> folders, Map<String, String> folderNames,
                SortedMap<String, SortedMap> folderOrdering, int level) {
            for (Entry<String, SortedMap> entry : folderOrdering.entrySet()) {
                folders.add(new FolderIdNamePair(entry.getKey(), folderNames.get(entry.getKey()), level));
                //noinspection unchecked Recursive data structure, not possible.
                generateFolderPairs(folders, folderNames, (SortedMap<String, SortedMap>) entry.getValue(), level+1);
            }
        }
    };
}
