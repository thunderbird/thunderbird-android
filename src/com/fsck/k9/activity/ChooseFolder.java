
package com.fsck.k9.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.fsck.k9.*;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChooseFolder extends K9ListActivity
{
    String mFolder;
    Account mAccount;
    MessageReference mMessageReference;
    ArrayAdapter<String> adapter;
    private ChooseFolderHandler mHandler = new ChooseFolderHandler();
    String heldInbox = null;
    boolean hideCurrentFolder = true;
    boolean showOptionNone = false;
    boolean showDisplayableOnly = false;

    public static final String EXTRA_ACCOUNT = "com.fsck.k9.ChooseFolder_account";
    public static final String EXTRA_CUR_FOLDER = "com.fsck.k9.ChooseFolder_curfolder";
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
        getListView().setTextFilterEnabled(true);
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMessageReference = (MessageReference)intent.getSerializableExtra(EXTRA_MESSAGE);
        mFolder = intent.getStringExtra(EXTRA_CUR_FOLDER);
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

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        setListAdapter(adapter);


        MessagingController.getInstance(getApplication()).listFolders(mAccount,
                false, mListener);


        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent();
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
                    adapter.notifyDataSetChanged();
                    break;
                case MSG_SET_SELECTED_FOLDER:
                    // TODO: I want this to highlight the chosen folder, but this doesn't work.
//          getListView().setSelection(msg.arg1);
//          getListView().setItemChecked(msg.arg1, true);
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
            Account.FolderMode aMode = Account.FolderMode.ALL;
            if (showDisplayableOnly)
            {
                aMode = account.getFolderDisplayMode();
            }
            else
            {
                aMode = account.getFolderTargetMode();
            }
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
            adapter.setNotifyOnChange(false);
            adapter.clear();
            int selectedFolder = -1;
            int position = 0;
            for (String name : localFolders)
            {
                if (K9.INBOX.equalsIgnoreCase(name))
                {
                    adapter.add(getString(R.string.special_mailbox_name_inbox));
                    heldInbox = name;
                }
                else
                {
                    adapter.add(name);
                }

                if ((name.equals(mFolder) || (K9.INBOX.equalsIgnoreCase(mFolder) && K9.INBOX.equalsIgnoreCase(name))))
                {
                    selectedFolder = position;
                }
                position++;
            }
            if (selectedFolder != -1)
            {
                mHandler.setSelectedFolder(selectedFolder);
            }
            mHandler.dataChanged();

        }
    };
}
