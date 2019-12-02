package com.fsck.k9.activity;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils.TruncateAt;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.DI;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.job.K9JobManager;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.folders.FolderIconProvider;
import com.fsck.k9.ui.helper.SizeFormatter;
import timber.log.Timber;

/**
 * FolderList is the primary user interface for the program. This
 * Activity shows list of the Account's folders
 */

public class ManageFoldersActivity extends K9ListActivity {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_FROM_SHORTCUT = "fromShortcut";

    private static final boolean REFRESH_REMOTE = true;

    private final K9JobManager jobManager = DI.get(K9JobManager.class);

    private FolderListAdapter folderListAdapter;

    private LayoutInflater inflater;

    private Account account;

    private FolderListHandler handler = new FolderListHandler();

    private FontSizes fontSizes = K9.getFontSizes();
    private Context context;

    private ActionBar actionBar;

    class FolderListHandler extends Handler {

        public void refreshTitle() {
            runOnUiThread(new Runnable() {
                public void run() {
                    actionBar.setTitle(R.string.folders_action);

                    String operation = folderListAdapter.activityListener.getOperation(ManageFoldersActivity.this);
                    if (operation.length() < 1) {
                        actionBar.setSubtitle(account.getEmail());
                    } else {
                        actionBar.setSubtitle(operation);
                    }
                }
            });
        }


        public void newFolders(final List<FolderInfoHolder> newFolders) {
            runOnUiThread(new Runnable() {
                public void run() {
                    folderListAdapter.folders.clear();
                    folderListAdapter.folders.addAll(newFolders);
                    folderListAdapter.filteredFolders = folderListAdapter.folders;
                    handler.dataChanged();
                }
            });
        }

        public void workingAccount(final int res) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String toastText = getString(res, account.getDescription());
                    Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }

        public void accountSizeChanged(final long oldSize, final long newSize) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String toastText = getString(R.string.account_size_changed, account.getDescription(), SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));

                    Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }

        public void progress(final boolean progress) {
            //TODO: Display progress indicator
        }

        public void dataChanged() {
            runOnUiThread(new Runnable() {
                public void run() {
                    folderListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public static void launch(Context context, Account account) {
        Intent intent = new Intent(context, ManageFoldersActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
            finish();
            return;
        }

        setLayout(R.layout.folder_list);
        initializeActionBar();
        ListView listView = getListView();
        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        listView.setFastScrollEnabled(true);
        listView.setScrollingCacheEnabled(false);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FolderSettings.actionSettings(ManageFoldersActivity.this, account, ((FolderInfoHolder) folderListAdapter.getItem(position)).serverId);
            }
        });

        listView.setSaveEnabled(true);

        inflater = getLayoutInflater();

        context = this;

        onNewIntent(getIntent());
        if (isFinishing()) {
            /*
             * onNewIntent() may call finish(), but execution will still continue here.
             * We return now because we don't want to display the changelog which can
             * result in a leaked window error.
             */
            return;
        }
    }

    private void initializeActionBar() {
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent); // onNewIntent doesn't autoset our "internal" intent

        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        if (account == null) {
            /*
             * This can happen when a launcher shortcut is created for an
             * account, and then the account is deleted or data is wiped, and
             * then the shortcut is used.
             */
            finish();
            return;
        }

        if (intent.getBooleanExtra(EXTRA_FROM_SHORTCUT, false) && account.getAutoExpandFolder() != null) {
            onOpenFolder(account.getAutoExpandFolder());
            finish();
        } else {
            initializeActivityView();
        }
    }

    private void initializeActivityView() {
        folderListAdapter = new FolderListAdapter();
        restorePreviousData();

        setListAdapter(folderListAdapter);
        getListView().setTextFilterEnabled(folderListAdapter.getFilter() != null); // should never be false but better safe then sorry
    }

    @SuppressWarnings("unchecked")
    private void restorePreviousData() {
        final Object previousData = getLastCustomNonConfigurationInstance();

        if (previousData != null) {
            folderListAdapter.folders = (ArrayList<FolderInfoHolder>) previousData;
            folderListAdapter.filteredFolders = Collections.unmodifiableList(folderListAdapter.folders);
        }
    }


    @Override public Object onRetainCustomNonConfigurationInstance() {
        return (folderListAdapter == null) ? null : folderListAdapter.folders;
    }

    @Override public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(folderListAdapter.activityListener);
        folderListAdapter.activityListener.onPause(this);
    }

    /**
    * On resume we refresh the folder list (in the background) and we refresh the
    * messages for any folder that is currently open. This guarantees that things
    * like unread message count and read status are updated.
     */
    @Override public void onResume() {
        super.onResume();

        if (!account.isAvailable(this)) {
            Timber.i("Account is unavailable right now: " + account);
            finish();
            return;
        }
        if (folderListAdapter == null)
            initializeActivityView();

        handler.refreshTitle();

        MessagingController.getInstance(getApplication()).addListener(folderListAdapter.activityListener);
        //account.refresh(Preferences.getPreferences(this));

        onRefresh(!REFRESH_REMOTE);

        MessagingController.getInstance(getApplication()).cancelNotificationsForAccount(account);
        folderListAdapter.activityListener.onResume(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Shortcuts that work no matter what is selected
        switch (keyCode) {
        case KeyEvent.KEYCODE_H: {
            Toast toast = Toast.makeText(this, R.string.folder_list_help_key, Toast.LENGTH_LONG);
            toast.show();
            return true;
        }
        case KeyEvent.KEYCODE_1: {
            setDisplayMode(FolderMode.FIRST_CLASS);
            return true;
        }
        case KeyEvent.KEYCODE_2: {
            setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
            return true;
        }
        case KeyEvent.KEYCODE_3: {
            setDisplayMode(FolderMode.NOT_SECOND_CLASS);
            return true;
        }
        case KeyEvent.KEYCODE_4: {
            setDisplayMode(FolderMode.ALL);
            return true;
        }
        }//switch


        return super.onKeyDown(keyCode, event);
    }//onKeyDown

    private void setDisplayMode(FolderMode newMode) {
        account.setFolderDisplayMode(newMode);
        Preferences.getPreferences(getApplicationContext()).saveAccount(account);
        if (account.getFolderPushMode() != FolderMode.NONE) {
            jobManager.schedulePusherRefresh();
        }
        folderListAdapter.getFilter().filter(null);
        onRefresh(false);
    }


    private void onRefresh(final boolean forceRemote) {
        MessagingController.getInstance(getApplication()).listFolders(account, forceRemote, folderListAdapter.activityListener);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.list_folders) {
            onRefresh(REFRESH_REMOTE);
            return true;
        } else if (id == R.id.compact) {
            onCompact(account);
            return true;
        } else if (id == R.id.display_1st_class) {
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
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void onOpenFolder(String folder) {
        LocalSearch search = new LocalSearch(folder);
        search.addAccountUuid(account.getUuid());
        search.addAllowedFolder(folder);
        MessageList.actionDisplaySearch(this, search, false, false);
    }

    private void onCompact(Account account) {
        handler.workingAccount(R.string.compacting_account);
        MessagingController.getInstance(getApplication()).compact(account, null);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.folder_list_option, menu);
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
                actionBar.setTitle(R.string.filter_folders_action);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                folderListAdapter.getFilter().filter(newText);
                return true;
            }
        });

        folderSearchView.setOnCloseListener(new SearchView.OnCloseListener() {

            @Override
            public boolean onClose() {
                actionBar.setTitle(R.string.folders_title);
                return false;
            }
        });
    }

    class FolderListAdapter extends BaseAdapter implements Filterable {
        private List<FolderInfoHolder> folders = new ArrayList<>();
        private List<FolderInfoHolder> filteredFolders = Collections.unmodifiableList(folders);
        private Filter filter = new FolderListFilter();
        private FolderIconProvider folderIconProvider = new FolderIconProvider(getTheme());


        @Override
        public Object getItem(int position) {
            return filteredFolders.get(position);
        }

        @Override
        public long getItemId(int position) {
            return filteredFolders.get(position).folder.getDatabaseId();
        }

        @Override
        public int getCount() {
            return filteredFolders.size();
        }

        private ActivityListener activityListener = new ActivityListener() {
            @Override
            public void listFoldersStarted(Account account) {
                if (account.equals(ManageFoldersActivity.this.account)) {
                    handler.progress(true);
                }
                super.listFoldersStarted(account);

            }

            @Override
            public void listFoldersFailed(Account account, String message) {
                if (account.equals(ManageFoldersActivity.this.account)) {
                    handler.progress(false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.fetching_folders_failed, Toast.LENGTH_SHORT).show();

                        }
                    });
                }
                super.listFoldersFailed(account, message);
            }

            @Override
            public void listFoldersFinished(Account account) {
                if (account.equals(ManageFoldersActivity.this.account)) {

                    handler.progress(false);
                    MessagingController.getInstance(getApplication()).refreshListener(folderListAdapter.activityListener);
                    handler.dataChanged();
                }
                super.listFoldersFinished(account);

            }

            @Override
            public void listFolders(Account account, List<LocalFolder> folders) {
                if (account.equals(ManageFoldersActivity.this.account)) {

                    List<FolderInfoHolder> newFolders = new LinkedList<>();
                    List<FolderInfoHolder> topFolders = new LinkedList<>();

                    Account.FolderMode aMode = account.getFolderDisplayMode();
                    for (LocalFolder folder : folders) {
                        Folder.FolderClass fMode = folder.getDisplayClass();

                        if ((aMode == FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS)
                                || (aMode == FolderMode.FIRST_AND_SECOND_CLASS &&
                                    fMode != Folder.FolderClass.FIRST_CLASS &&
                                    fMode != Folder.FolderClass.SECOND_CLASS)
                        || (aMode == FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS)) {
                            continue;
                        }

                        FolderInfoHolder holder = null;

                        int folderIndex = getFolderIndex(folder.getServerId());
                        if (folderIndex >= 0) {
                            holder = (FolderInfoHolder) getItem(folderIndex);
                        }

                        if (holder == null) {
                            holder = new FolderInfoHolder(folder, ManageFoldersActivity.this.account, -1);
                        } else {
                            holder.populate(folder, ManageFoldersActivity.this.account, -1);

                        }
                        if (folder.isInTopGroup()) {
                            topFolders.add(holder);
                        } else {
                            newFolders.add(holder);
                        }
                    }
                    Collections.sort(newFolders);
                    Collections.sort(topFolders);
                    topFolders.addAll(newFolders);
                    handler.newFolders(topFolders);
                }
                super.listFolders(account, folders);
            }

            @Override
            public void accountSizeChanged(Account account, long oldSize, long newSize) {
                if (account.equals(ManageFoldersActivity.this.account)) {
                    handler.accountSizeChanged(oldSize, newSize);
                }
            }
        };


        public int getFolderIndex(String folder) {
            FolderInfoHolder searchHolder = new FolderInfoHolder();
            searchHolder.serverId = folder;
            return   filteredFolders.indexOf(searchHolder);
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
            FolderInfoHolder folder = (FolderInfoHolder) getItem(itemPosition);
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = inflater.inflate(R.layout.folder_list_item, parent, false);
            }

            FolderViewHolder holder = (FolderViewHolder) view.getTag();

            if (holder == null) {
                holder = new FolderViewHolder();
                holder.folderName = view.findViewById(R.id.folder_name);
                holder.folderIcon = view.findViewById(R.id.folder_icon);
                holder.folderListItemLayout = view.findViewById(R.id.folder_list_item_layout);
                holder.folderServerId = folder.serverId;

                view.setTag(holder);
            }

            if (folder == null) {
                return view;
            }

            holder.folderName.setText(folder.displayName);
            holder.folderIcon.setImageResource(folderIconProvider.getFolderIcon(folder.folder.getType()));

            fontSizes.setViewTextSize(holder.folderName, fontSizes.getFolderName());

            if (K9.isWrapFolderNames()) {
                holder.folderName.setEllipsize(null);
                holder.folderName.setSingleLine(false);
            }
            else {
                holder.folderName.setEllipsize(TruncateAt.START);
                holder.folderName.setSingleLine(true);
            }

            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }

        /**
         * Filter to search for occurrences of the search-expression in any place of the
         * folder-name instead of doing just a prefix-search.
         *
         * @author Marcus@Wolschon.biz
         */
        public class FolderListFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence searchTerm) {
                FilterResults results = new FilterResults();

                Locale locale = Locale.getDefault();
                if ((searchTerm == null) || (searchTerm.length() == 0)) {
                    List<FolderInfoHolder> list = new ArrayList<>(folders);
                    results.values = list;
                    results.count = list.size();
                } else {
                    final String searchTermString = searchTerm.toString().toLowerCase(locale);
                    final String[] words = searchTermString.split(" ");
                    final int wordCount = words.length;

                    final List<FolderInfoHolder> newValues = new ArrayList<>();

                    for (final FolderInfoHolder value : folders) {
                        if (value.displayName == null) {
                            continue;
                        }
                        final String valueText = value.displayName.toLowerCase(locale);

                        for (int k = 0; k < wordCount; k++) {
                            if (valueText.contains(words[k])) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            /**
             * Publish the results to the user-interface.
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //noinspection unchecked
                filteredFolders = Collections.unmodifiableList((ArrayList<FolderInfoHolder>) results.values);
                // Send notification that the data set changed now
                notifyDataSetChanged();
            }
        }
    }

    static class FolderViewHolder {
        public TextView folderName;
        public String folderServerId;
        public ImageView folderIcon;
        public LinearLayout folderListItemLayout;
    }
}
