
package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import com.fsck.k9.*;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChooseFolder extends K9ListActivity
{
    String mFolder;
    String mSelectFolder;
    Account mAccount;
    MessageReference mMessageReference;
    ArrayAdapter<String> mAdapter;
    private ChooseFolderHandler mHandler = new ChooseFolderHandler();
    String heldInbox = null;
    boolean hideCurrentFolder = true;
    boolean showOptionNone = false;
    boolean showDisplayableOnly = false;

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
    private FolderListFilter<String> myFilter = null;

    public static final String EXTRA_ACCOUNT = "com.fsck.k9.ChooseFolder_account";
    public static final String EXTRA_CUR_FOLDER = "com.fsck.k9.ChooseFolder_curfolder";
    public static final String EXTRA_SEL_FOLDER = "com.fsck.k9.ChooseFolder_selfolder";
    public static final String EXTRA_NEW_FOLDER = "com.fsck.k9.ChooseFolder_newfolder";
    public static final String EXTRA_MESSAGE = "com.fsck.k9.ChooseFolder_message";
    public static final String EXTRA_SHOW_CURRENT = "com.fsck.k9.ChooseFolder_showcurrent";
    public static final String EXTRA_SHOW_FOLDER_NONE = "com.fsck.k9.ChooseFolder_showOptionNone";
    public static final String EXTRA_SHOW_DISPLAYABLE_ONLY = "com.fsck.k9.ChooseFolder_showDisplayableOnly";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getListView().setFastScrollEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMessageReference = (MessageReference)intent.getSerializableExtra(EXTRA_MESSAGE);
        mFolder = intent.getStringExtra(EXTRA_CUR_FOLDER);
        mSelectFolder = intent.getStringExtra(EXTRA_SEL_FOLDER);
        if (intent.getStringExtra(EXTRA_SHOW_CURRENT) != null)
        {
            hideCurrentFolder = false;
        }
        if (intent.getStringExtra(EXTRA_SHOW_FOLDER_NONE) != null)
        {
            showOptionNone = true;
        }
        if (intent.getStringExtra(EXTRA_SHOW_DISPLAYABLE_ONLY) != null)
        {
            showDisplayableOnly = true;
        }
        if (mFolder == null)
            mFolder = "";

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        {
            private Filter myFilter = null;

            @Override
            public Filter getFilter()
            {
                if (myFilter == null)
                {
                    myFilter = new FolderListFilter<String>(this);
                }
                return myFilter;
            }
        };

        setListAdapter(mAdapter);


        MessagingController.getInstance(getApplication()).listFolders(mAccount, false, mListener);


        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_ACCOUNT, mAccount.getUuid());
                intent.putExtra(EXTRA_CUR_FOLDER, mFolder);
                String destFolderName = (String)((TextView)view).getText();
                if (heldInbox != null && getString(R.string.special_mailbox_name_inbox).equals(destFolderName))
                {
                    destFolderName = heldInbox;
                }
                intent.putExtra(EXTRA_NEW_FOLDER, destFolderName);
                intent.putExtra(EXTRA_MESSAGE, mMessageReference);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

    }

    class ChooseFolderHandler extends Handler
    {

        private static final int MSG_PROGRESS = 2;

        private static final int MSG_DATA_CHANGED = 3;
        private static final int MSG_SET_SELECTED_FOLDER = 4;

        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
                case MSG_PROGRESS:
                    setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                    break;
                case MSG_DATA_CHANGED:
                    mAdapter.notifyDataSetChanged();

                    /*
                     * Only enable the text filter after the list has been
                     * populated to avoid possible race conditions because our
                     * FolderListFilter isn't really thread-safe.
                     */
                    getListView().setTextFilterEnabled(true);
                    break;
                case MSG_SET_SELECTED_FOLDER:
                    getListView().setSelection(msg.arg1);
                    break;
            }
        }

        public void progress(boolean progress)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_PROGRESS;
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void setSelectedFolder(int position)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SET_SELECTED_FOLDER;
            msg.arg1 = position;
            sendMessage(msg);
        }

        public void dataChanged()
        {
            sendEmptyMessage(MSG_DATA_CHANGED);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.folder_select_option, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {


            case R.id.display_1st_class:
            {
                setDisplayMode(FolderMode.FIRST_CLASS);
                return true;
            }
            case R.id.display_1st_and_2nd_class:
            {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS);
                return true;
            }
            case R.id.display_not_second_class:
            {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS);
                return true;
            }
            case R.id.display_all:
            {
                setDisplayMode(FolderMode.ALL);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setDisplayMode(FolderMode aMode)
    {
        mMode = aMode;
        // invalidate the current filter as it is working on an inval
        if (myFilter != null)
        {
            myFilter.invalidate();
        }
        //re-populate the list
        MessagingController.getInstance(getApplication()).listFolders(mAccount,
                false, mListener);
    }

    private MessagingListener mListener = new MessagingListener()
    {
        @Override
        public void listFoldersStarted(Account account)
        {
            if (!account.equals(mAccount))
            {
                return;
            }
            mHandler.progress(true);
        }

        @Override
        public void listFoldersFailed(Account account, String message)
        {
            if (!account.equals(mAccount))
            {
                return;
            }
            mHandler.progress(false);
        }

        @Override
        public void listFoldersFinished(Account account)
        {
            if (!account.equals(mAccount))
            {
                return;
            }
            mHandler.progress(false);
        }
        @Override
        public void listFolders(Account account, Folder[] folders)
        {
            if (!account.equals(mAccount))
            {
                return;
            }
            Account.FolderMode aMode = mMode;
            Preferences prefs = Preferences.getPreferences(getApplication().getApplicationContext());
            ArrayList<String> localFolders = new ArrayList<String>();

            for (Folder folder : folders)
            {
                String name = folder.getName();

                // Inbox needs to be compared case-insensitively
                if (hideCurrentFolder && (name.equals(mFolder) || (K9.INBOX.equalsIgnoreCase(mFolder) && K9.INBOX.equalsIgnoreCase(name))))
                {
                    continue;
                }
                try
                {
                    folder.refresh(prefs);
                    Folder.FolderClass fMode = folder.getDisplayClass();

                    if ((aMode == Account.FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS)
                            || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                                fMode != Folder.FolderClass.FIRST_CLASS &&
                                fMode != Folder.FolderClass.SECOND_CLASS)
                            || (aMode == Account.FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS))
                    {
                        continue;
                    }
                }
                catch (MessagingException me)
                {
                    Log.e(K9.LOG_TAG, "Couldn't get prefs to check for displayability of folder " + folder.getName(), me);
                }

                localFolders.add(folder.getName());

            }

            if (showOptionNone)
            {
                localFolders.add(K9.FOLDER_NONE);
            }

            Collections.sort(localFolders, new Comparator<String>()
            {
                public int compare(String aName, String bName)
                {
                    if (K9.FOLDER_NONE.equalsIgnoreCase(aName))
                    {
                        return -1;
                    }
                    if (K9.FOLDER_NONE.equalsIgnoreCase(bName))
                    {
                        return 1;
                    }
                    if (K9.INBOX.equalsIgnoreCase(aName))
                    {
                        return -1;
                    }
                    if (K9.INBOX.equalsIgnoreCase(bName))
                    {
                        return 1;
                    }

                    return aName.compareToIgnoreCase(bName);
                }
            });
            mAdapter.setNotifyOnChange(false);
            int selectedFolder = -1;
            try
            {
                mAdapter.clear();
                int position = 0;
                for (String name : localFolders)
                {
                    if (K9.INBOX.equalsIgnoreCase(name))
                    {
                        mAdapter.add(getString(R.string.special_mailbox_name_inbox));
                        heldInbox = name;
                    }
                    else if (!K9.ERROR_FOLDER_NAME.equals(name))
                    {
                        mAdapter.add(name);
                    }

                    if (mSelectFolder != null)
                    {
                        /*
                         * Never select EXTRA_CUR_FOLDER (mFolder) if EXTRA_SEL_FOLDER
                         * (mSelectedFolder) was provided.
                         */

                        if (name.equals(mSelectFolder))
                        {
                            selectedFolder = position;
                        }
                    }
                    else if (name.equals(mFolder) ||
                             (K9.INBOX.equalsIgnoreCase(mFolder) && K9.INBOX.equalsIgnoreCase(name)))
                    {
                        selectedFolder = position;
                    }
                    position++;
                }
            }
            finally
            {
                mAdapter.setNotifyOnChange(true);
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        //              runOnUiThread(
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            mHandler.dataChanged();

            if (selectedFolder != -1)
            {
                mHandler.setSelectedFolder(selectedFolder);
            }
        }
    };
}
