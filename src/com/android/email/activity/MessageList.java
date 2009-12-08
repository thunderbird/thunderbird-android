package com.android.email.activity;
//import android.os.Debug;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Config;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.email.Account;
import com.android.email.Email;
import com.android.email.K9Activity;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.R;
import com.android.email.Utility;
import com.android.email.MessagingController.SORT_TYPE;
import com.android.email.activity.setup.AccountSettings;
import com.android.email.activity.setup.FolderSettings;
import com.android.email.mail.Address;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;
import com.android.email.mail.Message.RecipientType;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;
import com.android.email.mail.store.LocalStore.LocalMessage;


/**
 * MessageList is the primary user interface for the program. This
 * Activity shows a list of messages.
 * From this Activity the user can perform all standard message
 * operations.
 *
 */

public class MessageList
        extends K9Activity
        implements OnClickListener, AdapterView.OnItemClickListener
{

    private static final int DIALOG_MARK_ALL_AS_READ = 1;

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;

    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_STARTUP = "startup";
    private static final String EXTRA_FOLDER  = "folder";

    private static final String STATE_KEY_LIST = "com.android.email.activity.messagelist_state";
    private static final String STATE_CURRENT_FOLDER = "com.android.email.activity.messagelist_folder";
    private static final String STATE_KEY_SELECTION = "com.android.email.activity.messagelist_selection";
    private static final String STATE_KEY_SELECTED_COUNT = "com.android.email.activity.messagelist_selected_count";

    private static final int WIDGET_NONE = 1;
    private static final int WIDGET_FLAG = 2;
    private static final int WIDGET_MULTISELECT = 3;

    private static final int[] colorChipResIds = new int[]
    {
        R.drawable.appointment_indicator_leftside_1,
        R.drawable.appointment_indicator_leftside_2,
        R.drawable.appointment_indicator_leftside_3,
        R.drawable.appointment_indicator_leftside_4,
        R.drawable.appointment_indicator_leftside_5,
        R.drawable.appointment_indicator_leftside_6,
        R.drawable.appointment_indicator_leftside_7,
        R.drawable.appointment_indicator_leftside_8,
        R.drawable.appointment_indicator_leftside_9,
        R.drawable.appointment_indicator_leftside_10,
        R.drawable.appointment_indicator_leftside_11,
        R.drawable.appointment_indicator_leftside_12,
        R.drawable.appointment_indicator_leftside_13,
        R.drawable.appointment_indicator_leftside_14,
        R.drawable.appointment_indicator_leftside_15,
        R.drawable.appointment_indicator_leftside_16,
        R.drawable.appointment_indicator_leftside_17,
        R.drawable.appointment_indicator_leftside_18,
        R.drawable.appointment_indicator_leftside_19,
        R.drawable.appointment_indicator_leftside_20,
        R.drawable.appointment_indicator_leftside_21,
    };

    private ListView mListView;
    private int mSelectedWidget = WIDGET_FLAG;

    private int colorChipResId;

    private MessageListAdapter mAdapter;

    private FolderInfoHolder mCurrentFolder;

    private LayoutInflater mInflater;

    private Account mAccount;

    /**
    * Stores the name of the folder that we want to open as soon as possible
    * after load. It is set to null once the folder has been opened once.
     */
    private String mFolderName;

    private MessageListHandler mHandler = new MessageListHandler();

    private SORT_TYPE sortType = SORT_TYPE.SORT_DATE;

    private boolean sortAscending = true;

    private boolean sortDateAscending = false;

    private boolean mStartup = false;

    private int mSelectedCount = 0;

    private View mBatchButtonArea;
    private Button mBatchReadButton;
    private Button mBatchDeleteButton;
    private Button mBatchFlagButton;

    class MessageListHandler extends Handler
    {

        private static final int MSG_PROGRESS = 2;

        private static final int MSG_FOLDER_LOADING = 7;

        private static final int MSG_ADD_MESSAGE = 10;
        private static final int MSG_REMOVE_MESSAGE = 11;
        private static final int MSG_SORT_MESSAGES = 12;

        private static final int MSG_FOLDER_SYNCING = 18;

        private static final int MSG_SENDING_OUTBOX = 19;

        @Override
        public void handleMessage(android.os.Message msg)
        {
            switch (msg.what)
            {
                case MSG_PROGRESS:
                    showProgressIndicator(msg.arg1 != 0);
                    break;

                case MSG_SORT_MESSAGES:

                    synchronized (mAdapter.messages)
                    {
                        Collections.sort(mAdapter.messages);
                    }
                    mAdapter.notifyDataSetChanged();
                    break;

                case MSG_REMOVE_MESSAGE:
                {
                    List<MessageInfoHolder> messages = (List<MessageInfoHolder>)((Object[]) msg.obj)[0];

                    for (MessageInfoHolder message : messages)
                    {
                        if (message != null && message.selected && mSelectedCount > 0)
                        {
                            mSelectedCount--;
                        }
                        mAdapter.messages.remove(message);
                    }
                    mAdapter.notifyDataSetChanged();
                    configureBatchButtons();
                    break;
                }

                case MSG_ADD_MESSAGE:
                {
                    boolean wasEmpty = mAdapter.messages.isEmpty();
                    List<MessageInfoHolder> messages = (List<MessageInfoHolder>)((Object[]) msg.obj)[0];
                    for (MessageInfoHolder message : messages)
                    {
                        int index = Collections.binarySearch(mAdapter.messages, message);

                        if (index < 0)
                        {
                            index = (index * -1) - 1;
                        }

                        mAdapter.messages.add(index, message);
                    }

                    if (wasEmpty)
                        mListView.setSelection(0);
                    mAdapter.notifyDataSetChanged();
                    break;
                }

                case MSG_FOLDER_SYNCING:
                {
                    String folderName = (String)((Object[]) msg.obj)[0];
                    String dispString;
                    dispString = mAccount.getDescription();

                    if (folderName != null)
                    {
                        dispString += " (" + getString(R.string.status_loading)
                                      + folderName + ")";
                    }

                    setTitle(dispString);

                    break;
                }

                case MSG_FOLDER_LOADING:
                {
                    FolderInfoHolder folder = mCurrentFolder;
                    if (folder != null)
                    {
                        folder.loading = msg.arg1 != 0;
                    }
                    break;
                }

                case MSG_SENDING_OUTBOX:
                {
                    boolean sending = (msg.arg1 != 0);
                    String dispString;
                    dispString = mAccount.getDescription();

                    if (sending)
                    {
                        dispString += " (" + getString(R.string.status_sending) + ")";
                    }

                    setTitle(dispString);

                    break;
                }

                default:
                    super.handleMessage(msg);
            }
        }

        public void removeMessage(List<MessageInfoHolder> messages)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_REMOVE_MESSAGE;
            msg.obj = new Object[] { messages };
            sendMessage(msg);
        }

        public void addMessages(List<MessageInfoHolder> messages)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_ADD_MESSAGE;
            msg.obj = new Object[] { messages };
            sendMessage(msg);
        }

        private void sortMessages()
        {
            sendEmptyMessage(MSG_SORT_MESSAGES);
        }

        public void folderLoading(String folder, boolean loading)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_FOLDER_LOADING;
            msg.arg1 = loading ? 1 : 0;
            msg.obj = folder;
            sendMessage(msg);
        }

        public void progress(boolean progress)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_PROGRESS;
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void folderSyncing(String folder)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_FOLDER_SYNCING;
            msg.obj = new String[] { folder };
            sendMessage(msg);
        }

        public void sendingOutbox(boolean sending)
        {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SENDING_OUTBOX;
            msg.arg1 = sending ? 1 : 0;
            sendMessage(msg);
        }
    }

    /**
    * This class is responsible for reloading the list of local messages for a
    * given folder, notifying the adapter that the message have been loaded and
    * queueing up a remote update of the folder.
     */

    public static void actionHandleFolder(Context context, Account account, String folder, boolean startup)
    {
        Intent intent = new Intent(context, MessageList.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        intent.putExtra(EXTRA_STARTUP, startup);

        if (folder != null)
        {
            intent.putExtra(EXTRA_FOLDER, folder);
        }

        context.startActivity(intent);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id)
    {
        if ((position+1) == (mAdapter.getCount()))
        {
            MessagingController.getInstance(getApplication()).loadMoreMessages(
                mAccount,
                mFolderName,
                mAdapter.mListener);
            return;
        }
        else if (mSelectedWidget == WIDGET_MULTISELECT)
        {
            CheckBox selected = (CheckBox) v.findViewById(R.id.selected_checkbox);
            selected.setChecked(!selected.isChecked());
        }
        else
        {
            MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);
            onOpenMessage(message);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        //Debug.startMethodTracing("k9");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.message_list);

        mListView = (ListView) findViewById(R.id.message_list);
        mListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(true);
        mListView.setOnItemClickListener(this);



        registerForContextMenu(mListView);

        /*
        * We manually save and restore the list's state because our adapter is
        * slow.
         */
        mListView.setSaveEnabled(false);

        mInflater = getLayoutInflater();

        mBatchButtonArea = findViewById(R.id.batch_button_area);
        mBatchReadButton = (Button) findViewById(R.id.batch_read_button);
        mBatchReadButton.setOnClickListener(this);
        mBatchDeleteButton = (Button) findViewById(R.id.batch_delete_button);
        mBatchDeleteButton.setOnClickListener(this);
        mBatchFlagButton = (Button) findViewById(R.id.batch_flag_button);
        mBatchFlagButton.setOnClickListener(this);

        Intent intent = getIntent();
        mAccount = (Account)intent.getSerializableExtra(EXTRA_ACCOUNT);
        mStartup = (boolean)intent.getBooleanExtra(EXTRA_STARTUP, false);

        // Take the initial folder into account only if we are *not* restoring the
        // activity already

        if (savedInstanceState == null)
        {
            mFolderName = intent.getStringExtra(EXTRA_FOLDER);

            if (mFolderName == null)
            {
                mFolderName = mAccount.getAutoExpandFolderName();
            }
        }
        else
        {
            mFolderName = savedInstanceState.getString(STATE_CURRENT_FOLDER);
            mSelectedCount  = savedInstanceState.getInt(STATE_KEY_SELECTED_COUNT);
        }

        /*
        * Since the color chip is always the same color for a given account we just
        * cache the id of the chip right here.
         */
        colorChipResId = colorChipResIds[mAccount.getAccountNumber() % colorChipResIds.length];

        mAdapter = new MessageListAdapter();

        final Object previousData = getLastNonConfigurationInstance();

        if (previousData != null)
        {
            //noinspection unchecked
            mAdapter.messages.addAll((List<MessageInfoHolder>) previousData);
        }

        mCurrentFolder = mAdapter.getFolder(mFolderName);

        mListView.setAdapter(mAdapter);

        if (savedInstanceState != null)
        {
            onRestoreListState(savedInstanceState);
        }

    }

    private void onRestoreListState(Bundle savedInstanceState)
    {
        String currentFolder = savedInstanceState.getString(STATE_CURRENT_FOLDER);
        int selectedChild = savedInstanceState.getInt(STATE_KEY_SELECTION, -1);

        if (selectedChild != 0)
        {
            mListView.setSelection(selectedChild);
        }
        if (currentFolder != null)
        {
            mCurrentFolder = mAdapter.getFolder(currentFolder);
        }


        mListView.onRestoreInstanceState(savedInstanceState.getParcelable(STATE_KEY_LIST));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        //Debug.stopMethodTracing();
        MessagingController.getInstance(getApplication()).removeListener(mAdapter.mListener);
    }

    /**
    * On resume we refresh
    * messages for any folder that is currently open. This guarantees that things
    * like unread message count and read status are updated.
     */
    @Override
    public void onResume()
    {
        super.onResume();

        sortType = MessagingController.getInstance(getApplication()).getSortType();
        sortAscending = MessagingController.getInstance(getApplication()).isSortAscending(sortType);
        sortDateAscending = MessagingController.getInstance(getApplication()).isSortAscending(SORT_TYPE.SORT_DATE);

        MessagingController.getInstance(getApplication()).addListener(mAdapter.mListener);
        mAdapter.messages.clear();
        mAdapter.notifyDataSetChanged();
        MessagingController.getInstance(getApplication()).listLocalMessages(mAccount, mFolderName,  mAdapter.mListener);

        NotificationManager notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifMgr.cancel(mAccount.getAccountNumber());
        notifMgr.cancel(-1000 - mAccount.getAccountNumber());

        setTitle(
            mAccount.getDescription()
            + " - " +
            mCurrentFolder.displayName

        );

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_KEY_LIST, mListView.onSaveInstanceState());
        outState.putInt(STATE_KEY_SELECTION, mListView .getSelectedItemPosition());
        outState.putString(STATE_CURRENT_FOLDER, mCurrentFolder.name);
        outState.putInt(STATE_KEY_SELECTED_COUNT, mSelectedCount);
    }


    @Override public Object onRetainNonConfigurationInstance()
    {
        return mAdapter.messages;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        //Shortcuts that work no matter what is selected

        switch (keyCode)
        {


            case KeyEvent.KEYCODE_DPAD_LEFT:
            {
                if (mBatchButtonArea.hasFocus())
                {
                    return false;
                }
                else
                {

                    cycleVisibleWidgets(true);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            {
                if (mBatchButtonArea.hasFocus())
                {
                    return false;
                }
                else
                {
                    cycleVisibleWidgets(false);
                    return true;
                }
            }


            case KeyEvent.KEYCODE_C:
            {
                onCompose();
                return true;
            }

            case KeyEvent.KEYCODE_Q:
                //case KeyEvent.KEYCODE_BACK:
            {
                onShowFolderList();
                return true;
            }

            case KeyEvent.KEYCODE_O:
            {
                onCycleSort();
                return true;
            }

            case KeyEvent.KEYCODE_I:
            {
                onToggleSortAscending();
                return true;
            }

            case KeyEvent.KEYCODE_H:
            {
                Toast toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        }//switch

        int position = mListView.getSelectedItemPosition();
        try
        {
            if (position >= 0)
            {
                MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);


                if (message != null)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DEL:
                        {
                            onDelete(message, position);
                            return true;
                        }

                        case KeyEvent.KEYCODE_D:
                        {
                            onDelete(message, position);
                            return true;
                        }

                        case KeyEvent.KEYCODE_F:
                        {
                            onForward(message);
                            return true;
                        }

                        case KeyEvent.KEYCODE_A:
                        {
                            onReplyAll(message);
                            return true;
                        }

                        case KeyEvent.KEYCODE_R:
                        {
                            onReply(message);
                            return true;
                        }

                        case KeyEvent.KEYCODE_G:
                        {
                            onToggleFlag(message);
                            return true;
                        }

                        case KeyEvent.KEYCODE_M:
                        {
                            onMove(message);
                            return true;
                        }

                        case KeyEvent.KEYCODE_Y:
                        {
                            onCopy(message);
                            return true;
                        }

                        case KeyEvent.KEYCODE_Z:
                        {
                            onToggleRead(message);
                            return true;
                        }
                    }
                }
            }
        }
        finally
        {
            return super.onKeyDown(keyCode, event);
        }
    }//onKeyDown





    private void onOpenMessage(MessageInfoHolder message)
    {
        if (message.folder.name.equals(mAccount.getDraftsFolderName()))
        {
            MessageCompose.actionEditDraft(this, mAccount, message.message);
        }
        else
        {
            // Need to get the list before the sort starts
            ArrayList<String> messageUids = new ArrayList<String>();

            for (MessageInfoHolder holder : mAdapter.messages)
            {
                messageUids.add(holder.uid);
            }

            MessageView.actionView(this, mAccount, message.folder.name, message.uid, messageUids);
        }
        /*
        * We set read=true here for UI performance reasons. The actual value will
        * get picked up on the refresh when the Activity is resumed but that may
        * take a second or so and we don't want this to show and then go away. I've
        * gone back and forth on this, and this gives a better UI experience, so I
        * am putting it back in.
         */

        if (!message.read)
        {
            message.read = true;
            mHandler.sortMessages();
        }

    }

    public void cycleVisibleWidgets(boolean ascending)
    {
        if (ascending)
        {

            switch (mSelectedWidget)
            {
                case WIDGET_FLAG:
                {
                    mSelectedWidget = WIDGET_MULTISELECT;
                    showBatchButtons();
                    break;
                }
                case WIDGET_MULTISELECT:
                {
                    mSelectedWidget = WIDGET_NONE;
                    hideBatchButtons();
                    break;
                }
                case WIDGET_NONE:
                {
                    mSelectedWidget = WIDGET_FLAG;
                    break;
                }

            }
        }
        else
        {
            switch (mSelectedWidget)
            {
                case WIDGET_FLAG:
                {
                    mSelectedWidget=WIDGET_NONE;
                    break;
                }
                case WIDGET_NONE:
                {
                    mSelectedWidget=WIDGET_MULTISELECT;
                    showBatchButtons();
                    break;
                }
                case WIDGET_MULTISELECT:
                {
                    mSelectedWidget=WIDGET_FLAG;
                    hideBatchButtons();
                    break;
                }

            }

        }


        int count = mListView.getChildCount();
        for (int i=0; i<count; i++)
        {
            setVisibleWidgetsForListItem(mListView.getChildAt(i), mSelectedWidget);
        }
    }


    private void setVisibleWidgetsForListItem(View v, int nextWidget)
    {

        Button flagged = (Button) v.findViewById(R.id.flagged);
        CheckBox selected = (CheckBox) v.findViewById(R.id.selected_checkbox);

        if (flagged == null || selected == null)
        {
            return;
        }

        if (nextWidget == WIDGET_NONE)
        {
            v.findViewById(R.id.widgets).setVisibility(View.GONE);
            return;
        }
        else
        {
            v.findViewById(R.id.widgets).setVisibility(View.VISIBLE);
        }



        if (nextWidget == WIDGET_MULTISELECT)
        {
            flagged.setVisibility(View.GONE);
            selected.setVisibility(View.VISIBLE);
        }
        else
        {
            flagged.setVisibility(View.VISIBLE);
            selected.setVisibility(View.GONE);
        }
    }

    private void onShowFolderList()
    {
        if (mStartup || isTaskRoot())
        {
            FolderList.actionHandleAccount(this, mAccount, false);
        }

        finish();
    }

    private void onCompose()
    {
        MessageCompose.actionCompose(this, mAccount);
    }

    private void onEditAccount()
    {
        AccountSettings.actionSettings(this, mAccount);
    }

    private void changeSort(SORT_TYPE newSortType)
    {
        sortType = newSortType;
        MessagingController.getInstance(getApplication()).setSortType(sortType);
        sortAscending = MessagingController.getInstance(getApplication()).isSortAscending(sortType);
        sortDateAscending = MessagingController.getInstance(getApplication()).isSortAscending(SORT_TYPE.SORT_DATE);
        reSort();
    }

    private void reSort()
    {
        int toastString = sortType.getToast(sortAscending);

        Toast toast = Toast.makeText(this, toastString, Toast.LENGTH_SHORT);
        toast.show();

        mHandler.sortMessages();

    }

    private void onAccounts()
    {
        Accounts.listAccounts(this);
        finish();
    }

    private void onCycleSort()
    {
        SORT_TYPE[] sorts = SORT_TYPE.values();
        int curIndex = 0;

        for (int i = 0; i < sorts.length; i++)
        {
            if (sorts[i] == sortType)
            {
                curIndex = i;
                break;
            }
        }

        curIndex++;

        if (curIndex == sorts.length)
        {
            curIndex = 0;
        }

        changeSort(sorts[curIndex]);
    }

    private void onToggleSortAscending()
    {
        MessagingController.getInstance(getApplication()).setSortAscending(sortType, !sortAscending);

        sortAscending = MessagingController.getInstance(getApplication()).isSortAscending(sortType);
        sortDateAscending = MessagingController.getInstance(getApplication()).isSortAscending(SORT_TYPE.SORT_DATE);

        reSort();
    }

    private void onDelete(MessageInfoHolder holder, int position)
    {
        mAdapter.removeMessage(holder);
        MessagingController.getInstance(getApplication()).deleteMessages(mAccount, holder.message.getFolder().getName(), new Message[] { holder.message }, null);
        mListView.setSelection(position);

    }


    private void onMove(MessageInfoHolder holder)
    {
        if (MessagingController.getInstance(getApplication()).isMoveCapable(mAccount) == false)
        {
            return;
        }

        if (MessagingController.getInstance(getApplication()).isMoveCapable(holder.message) == false)
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        Intent intent = new Intent(this, ChooseFolder.class);

        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount);
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, holder.folder.name);
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE_UID, holder.message.getUid());
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_MOVE);
    }

    private void onCopy(MessageInfoHolder holder)
    {
        if (MessagingController.getInstance(getApplication()).isCopyCapable(mAccount) == false)
        {
            return;
        }

        if (MessagingController.getInstance(getApplication()).isCopyCapable(holder.message) == false)
        {
            Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        Intent intent = new Intent(this, ChooseFolder.class);

        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount);
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, holder.folder.name);
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE_UID, holder.message.getUid());
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode)
        {
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY:
                if (data == null)
                    return;

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);

                String uid = data.getStringExtra(ChooseFolder.EXTRA_MESSAGE_UID);

                FolderInfoHolder srcHolder = mCurrentFolder;

                if (srcHolder != null && destFolderName != null)
                {
                    MessageInfoHolder m = mAdapter.getMessage(uid);

                    if (m != null)
                    {
                        switch (requestCode)
                        {
                            case ACTIVITY_CHOOSE_FOLDER_MOVE:
                                onMoveChosen(m, destFolderName);

                                break;

                            case ACTIVITY_CHOOSE_FOLDER_COPY:
                                onCopyChosen(m, destFolderName);

                                break;
                        }
                    }
                }
        }
    }


    private void onMoveChosen(MessageInfoHolder holder, String folderName)
    {
        if (MessagingController.getInstance(getApplication()).isMoveCapable(mAccount) == false)
        {
            return;
        }

        if (folderName == null)
        {
            return;
        }

        mAdapter.removeMessage(holder);
        MessagingController.getInstance(getApplication()).moveMessage(mAccount, holder.message.getFolder().getName(), holder.message, folderName, null);

    }


    private void onCopyChosen(MessageInfoHolder holder, String folderName)
    {
        if (MessagingController.getInstance(getApplication()).isCopyCapable(mAccount) == false)
        {
            return;
        }
        if (folderName == null)
        {
            return;
        }
        MessagingController.getInstance(getApplication()).copyMessage(mAccount,
                holder.message.getFolder().getName(), holder.message, folderName, null);
    }


    private void onReply(MessageInfoHolder holder)
    {
        MessageCompose.actionReply(this, mAccount, holder.message, false);
    }

    private void onReplyAll(MessageInfoHolder holder)
    {
        MessageCompose.actionReply(this, mAccount, holder.message, true);
    }

    private void onForward(MessageInfoHolder holder)
    {
        MessageCompose.actionForward(this, mAccount, holder.message);
    }

    private void onMarkAllAsRead(final Account account, final String folder)
    {
        showDialog(DIALOG_MARK_ALL_AS_READ);
    }

    @Override
    public Dialog onCreateDialog(int id)
    {
        switch (id)
        {
            case DIALOG_MARK_ALL_AS_READ:
                return createMarkAllAsReadDialog();

        }

        return super.onCreateDialog(id);
    }

    public void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id)
        {
            case DIALOG_MARK_ALL_AS_READ:
                ((AlertDialog)dialog).setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                                 mCurrentFolder.displayName));

                break;

            default:
                super.onPrepareDialog(id, dialog);
        }
    }

    private Dialog createMarkAllAsReadDialog()
    {
        return new AlertDialog.Builder(this)
               .setTitle(R.string.mark_all_as_read_dlg_title)
               .setMessage(getString(R.string.mark_all_as_read_dlg_instructions_fmt,
                                     mCurrentFolder.displayName))
               .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dismissDialog(DIALOG_MARK_ALL_AS_READ);

                try
                {

                    MessagingController.getInstance(getApplication()).markAllMessagesRead(mAccount, mCurrentFolder.name);

                    for (MessageInfoHolder holder : mAdapter.messages)
                    {
                        holder.read = true;
                    }

                    mHandler.sortMessages();


                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
        })

               .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                dismissDialog(DIALOG_MARK_ALL_AS_READ);
            }
        })

               .create();
    }

    private void onToggleRead(MessageInfoHolder holder)
    {
        MessagingController.getInstance(getApplication()).setFlag(mAccount, holder.message.getFolder().getName(), new String[] { holder.uid }, Flag.SEEN, !holder.read);
        holder.read = !holder.read;
        mHandler.sortMessages();
    }

    private void onToggleFlag(MessageInfoHolder holder)
    {

        MessagingController.getInstance(getApplication()).setFlag(mAccount, holder.message.getFolder().getName(), new String[] { holder.uid }, Flag.FLAGGED, !holder.flagged);
        holder.flagged = !holder.flagged;
        mHandler.sortMessages();
    }

    private void checkMail(Account account, String folderName)
    {
        MessagingController.getInstance(getApplication()).synchronizeMailbox(account, folderName, mAdapter.mListener);
        sendMail(account);
    }

    private void sendMail(Account account)
    {
        MessagingController.getInstance(getApplication()).sendPendingMessages(account, mAdapter.mListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.check_mail:
                checkMail(mAccount, mFolderName);
                return true;
            case R.id.send_messages:
                sendMail(mAccount);
                return true;

            case R.id.compose:
                onCompose();

                return true;

            case R.id.accounts:
                onAccounts();

                return true;

            case R.id.set_sort_date:
                changeSort(SORT_TYPE.SORT_DATE);

                return true;

            case R.id.set_sort_subject:
                changeSort(SORT_TYPE.SORT_SUBJECT);

                return true;

            case R.id.set_sort_sender:
                changeSort(SORT_TYPE.SORT_SENDER);

                return true;

            case R.id.set_sort_flag:
                changeSort(SORT_TYPE.SORT_FLAGGED);

                return true;

            case R.id.set_sort_unread:
                changeSort(SORT_TYPE.SORT_UNREAD);

                return true;

            case R.id.set_sort_attach:
                changeSort(SORT_TYPE.SORT_ATTACHMENT);

                return true;

            case R.id.reverse_sort:
                onToggleSortAscending();

                return true;

            case R.id.list_folders:
                onShowFolderList();

                return true;

            case R.id.mark_all_as_read:
                onMarkAllAsRead(mAccount, mFolderName);

                return true;

            case R.id.folder_settings:
                FolderSettings.actionSettings(this, mAccount, mFolderName);

                return true;

            case R.id.account_settings:
                onEditAccount();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_list_option, menu);

        if (mCurrentFolder.outbox)
        {
            menu.findItem(R.id.check_mail).setVisible(false);
        }
        else
        {
            menu.findItem(R.id.send_messages).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item .getMenuInfo();
        MessageInfoHolder holder = (MessageInfoHolder) mAdapter.getItem(info.position);

        switch (item.getItemId())
        {
            case R.id.open:
                onOpenMessage(holder);

                break;

            case R.id.delete:
                onDelete(holder, info.position);

                break;

            case R.id.reply:
                onReply(holder);

                break;

            case R.id.reply_all:
                onReplyAll(holder);

                break;

            case R.id.forward:
                onForward(holder);

                break;

            case R.id.mark_as_read:
                onToggleRead(holder);

                break;

            case R.id.flag:
                onToggleFlag(holder);

                break;

            case R.id.move:
                onMove(holder);

                break;

            case R.id.copy:
                onCopy(holder);

                break;

            case R.id.send_alternate:
                onSendAlternate(mAccount, holder);

                break;

        }

        return super.onContextItemSelected(item);
    }

    public void onSendAlternate(Account account, MessageInfoHolder holder)
    {
        MessagingController.getInstance(getApplication()).sendAlternate(this, account, holder.message);
    }

    public void showProgressIndicator(boolean status)
    {
        setProgressBarIndeterminateVisibility(status);
        ProgressBar bar = (ProgressBar)mListView.findViewById(R.id.message_list_progress);
        if (bar == null)
        {
            return;
        }

        bar.setIndeterminate(true);
        if (status)
        {
            bar.setVisibility(bar.VISIBLE);
        }
        else
        {
            bar.setVisibility(bar.INVISIBLE);

        }
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);


        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(info.position);

        if (message == null)
        {
            return;
        }

        getMenuInflater().inflate(R.menu.message_list_context, menu);

        menu.setHeaderTitle((CharSequence) message.subject);

        if (message.read)
        {
            menu.findItem(R.id.mark_as_read).setTitle(R.string.mark_as_unread_action);
        }

        if (message.flagged)
        {
            menu.findItem(R.id.flag).setTitle(R.string.unflag_action);
        }

        if (MessagingController.getInstance(getApplication()).isCopyCapable(mAccount) == false)
        {
            menu.findItem(R.id.copy).setVisible(false);
        }

        if (MessagingController.getInstance(getApplication()).isMoveCapable(mAccount) == false)
        {
            menu.findItem(R.id.move).setVisible(false);
        }
    }

    private String truncateStatus(String mess)
    {
        if (mess != null && mess.length() > 27)
        {
            mess = mess.substring(0, 27);
        }

        return mess;
    }

    class MessageListAdapter extends BaseAdapter
    {
        private List<MessageInfoHolder> messages = java.util.Collections.synchronizedList(new ArrayList<MessageInfoHolder>());

        private MessagingListener mListener = new MessagingListener()
        {

            @Override
            public void synchronizeMailboxStarted(Account account, String folder)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }

                mHandler.progress(true);
                mHandler.folderLoading(folder, true);
                mHandler.folderSyncing(folder);
            }



            @Override
            public void synchronizeMailboxFinished(Account account, String folder,
                                                   int totalMessagesInMailbox, int numNewMessages)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }

                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
                mHandler.folderSyncing(null);
                mHandler.sortMessages();
            }

            @Override
            public void synchronizeMailboxFailed(Account account, String folder, String message)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }

                // Perhaps this can be restored, if done in the mHandler thread
                // Toast.makeText(MessageList.this, message, Toast.LENGTH_LONG).show();
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
                mHandler.folderSyncing(null);
                mHandler.sortMessages();
            }

            @Override
            public void synchronizeMailboxAddOrUpdateMessage(Account account, String folder, Message message)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }

                addOrUpdateMessage(folder, message);
            }

            @Override
            public void synchronizeMailboxRemovedMessage(Account account, String folder,Message message)
            {
                removeMessage(getMessage(message.getUid()));
            }

            @Override
            public void listLocalMessagesStarted(Account account, String folder)
            {
                if (!account.equals(mAccount))
                {
                    return;
                }
                mHandler.progress(true);
                mHandler.folderLoading(folder, true);
            }

            @Override
            public void listLocalMessagesFailed(Account account, String folder, String message)
            {
                if (!account.equals(mAccount))
                {
                    return;
                }

                mHandler.sortMessages();
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }

            @Override
            public void listLocalMessagesFinished(Account account, String folder)
            {
                if (!account.equals(mAccount))
                {
                    return;
                }

                mHandler.sortMessages();

                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }

            @Override
            public void listLocalMessages(Account account, String folder, Message[] messages)
            {
                if (!account.equals(mAccount))
                {
                    return;
                }

                if (folder != mFolderName)
                {
                    return;
                }

                //synchronizeMessages(folder, messages);
            }
            @Override
            public void listLocalMessagesRemoveMessage(Account account, String folder,Message message)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }
                MessageInfoHolder holder = getMessage(message.getUid());
                if (holder != null)
                {
                    removeMessage(getMessage(message.getUid()));
                }
            }


            @Override
            public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }

                addOrUpdateMessages(folder, messages);

            }

            @Override
            public void listLocalMessagesUpdateMessage(Account account, String folder, Message message)
            {
                if (!account.equals(mAccount) || !folder.equals(mFolderName))
                {
                    return;
                }

                addOrUpdateMessage(folder, message);
            }

        };

        private Drawable mAttachmentIcon;
        private Drawable mAnsweredIcon;
        private View footerView = null;

        MessageListAdapter()
        {
            mAttachmentIcon = getResources().getDrawable(R.drawable.ic_mms_attachment_small);
            mAnsweredIcon = getResources().getDrawable(R.drawable.ic_mms_answered_small);
        }

        public void removeMessages(List<MessageInfoHolder> holders)
        {
            if (holders == null)
            {
                return;
            }

            mHandler.removeMessage(holders);

        }

        public void removeMessage(MessageInfoHolder holder)
        {
            List<MessageInfoHolder> messages = new ArrayList<MessageInfoHolder>();
            messages.add(holder);
            removeMessages(messages);
        }

        private void addOrUpdateMessage(String folder, Message message)
        {
            FolderInfoHolder f = mCurrentFolder;

            if (f == null)
            {
                return;
            }

            addOrUpdateMessage(f, message);
        }

        private void addOrUpdateMessage(FolderInfoHolder folder, Message message)
        {
            List<Message> messages = new ArrayList<Message>();
            messages.add(message);
            addOrUpdateMessages(folder, messages);
        }

        private void addOrUpdateMessages(String folder, List<Message> messages)
        {
            FolderInfoHolder f = mCurrentFolder;

            if (f == null)
            {
                return;
            }

            addOrUpdateMessages(f, messages);
        }
        private void addOrUpdateMessages(FolderInfoHolder folder, List<Message> messages)
        {
            boolean needsSort = false;
            List<MessageInfoHolder> messagesToAdd = new ArrayList<MessageInfoHolder>();
            List<MessageInfoHolder> messagesToRemove = new ArrayList<MessageInfoHolder>();

            for (Message message : messages)
            {
                MessageInfoHolder m = getMessage(message.getUid());

                if (m == null)
                {
                    m = new MessageInfoHolder(message, folder);
                    messagesToAdd.add(m);
                }
                else
                {
                    if (message.isSet(Flag.DELETED))
                    {
                        messagesToRemove.add(m);

                    }
                    else
                    {
                        m.populate(message, folder);
                        needsSort = true;

                    }
                }
            }

            if (messagesToRemove.size() > 0)
            {
                removeMessages(messagesToRemove);
            }
            if (messagesToAdd.size() > 0)
            {
                mHandler.addMessages(messagesToAdd);
            }
            if (needsSort)
            {
                mHandler.sortMessages();
            }
        }

        // XXX TODO - make this not use a for loop
        public MessageInfoHolder getMessage(String messageUid)
        {
            MessageInfoHolder searchHolder = new MessageInfoHolder();
            searchHolder.uid = messageUid;
            int index = mAdapter.messages.indexOf((Object) searchHolder);
            if (index >= 0)
            {
                return (MessageInfoHolder)mAdapter.messages.get(index);
            }
            return null;
        }

        public FolderInfoHolder getFolder(String folder)
        {
            LocalFolder local_folder = null;
            try
            {
                LocalStore localStore = (LocalStore)Store.getInstance(mAccount.getLocalStoreUri(), getApplication());
                local_folder = localStore.getFolder(folder);
                return new FolderInfoHolder((Folder)local_folder);
            }
            catch (Exception e)
            {
                Log.e(Email.LOG_TAG, "getFolder(" + folder + ") goes boom: ",e);
                return null;
            }
            finally
            {
                if (local_folder != null)
                {
                    local_folder.close(false);
                }
            }
        }

        private static final int NON_MESSAGE_ITEMS = 1;
        public int getCount()
        {
            if (mAdapter.messages == null || mAdapter.messages.size() == 0)
            {
                return NON_MESSAGE_ITEMS ;
            }

            return mAdapter.messages.size() +NON_MESSAGE_ITEMS  ;
        }

        public long getItemId(int position)
        {
            try
            {
                MessageInfoHolder messageHolder =(MessageInfoHolder) getItem(position);
                if (messageHolder != null)
                {
                    return ((LocalStore.LocalMessage)  messageHolder.message).getId();
                }
            }
            catch (Exception e)
            {
                Log.i(Email.LOG_TAG,"getItemId("+position+") ",e);
            }
            return -1;
        }

        public Object getItem(long position)
        {
            return getItem((int)position);
        }

        public Object getItem(int position)
        {
            try
            {
                if (position < mAdapter.messages.size())
                {
                    return mAdapter.messages.get(position);
                }
            }
            catch (Exception e)
            {
                Log.e(Email.LOG_TAG, "getItem(" + position + "), but folder.messages.size() = " + mAdapter.messages.size(), e);
            }
            return null;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {

            if (position == mAdapter.messages.size())
            {
                return getFooterView(position, convertView, parent);
            }
            else
            {
                return  getItemView(position, convertView, parent);
            }
        }


        public View getItemView(int position, View convertView, ViewGroup parent)
        {
            MessageInfoHolder message = (MessageInfoHolder) getItem(position);
            View view;

            if ((convertView != null)
                 && (
                    (convertView.getId() == R.layout.message_list_item_left)
                    || (convertView.getId() == R.layout.message_list_item_right)
                )
            )
            {
                view = convertView;
            }
            else
            {
                int layoutId;
                if (Email.getMessageListWidgetSide()==Email.MessageListWidgetSide.LEFT)
                {
                    layoutId = R.layout.message_list_item_left;
                }
                else
                {
                    layoutId = R.layout.message_list_item_right;
                }
                view = mInflater.inflate(layoutId, parent, false);
                view.setId(layoutId);
            }


            MessageViewHolder holder = (MessageViewHolder) view.getTag();

            if (holder == null)
            {
                holder = new MessageViewHolder();
                holder.subject = (TextView) view.findViewById(R.id.subject);
                holder.from = (TextView) view.findViewById(R.id.from);
                holder.date = (TextView) view.findViewById(R.id.date);
                holder.chip = view.findViewById(R.id.chip);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged);
                holder.flagged.setOnClickListener(new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        // Perform action on clicks
                        MessageInfoHolder message = (MessageInfoHolder) getItem((Integer)v.getTag());
                        onToggleFlag(message);
                    }
                });

                holder.chip.setBackgroundResource(colorChipResId);
                holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
                if (holder.selected!=null)
                {
                    holder.selected.setOnCheckedChangeListener(holder);
                }
                view.setTag(holder);
            }


            if (message != null)
            {
                holder.chip.getBackground().setAlpha(message.read ? 0 : 255);
                holder.subject.setTypeface(null, message.read ? Typeface.NORMAL  : Typeface.BOLD);

                int subjectColor = holder.from.getCurrentTextColor();  // Get from another field that never changes color



                setVisibleWidgetsForListItem(view, mSelectedWidget);
                // XXX TODO there has to be some way to walk our view hierarchy and get this
                holder.flagged.setTag((Integer)position);


                holder.flagged.setChecked(message.flagged);
                //So that the mSelectedCount is only incremented/decremented
                //when a user checks the checkbox (vs code)
                holder.position = -1;
                holder.selected.setChecked(message.selected);

                if (message.downloaded)
                {
                    holder.chip.getBackground().setAlpha(message.read ? 0 : 127);
                    view.getBackground().setAlpha(0);
                }
                else
                {
                    view.getBackground().setAlpha(127);
                }

                holder.subject.setTextColor(0xff000000 | subjectColor);
                holder.subject.setText(message.subject);

                holder.from.setText(message.sender);
                holder.from.setTypeface(null, message.read ? Typeface.NORMAL : Typeface.BOLD);
                holder.date.setText(message.date);
                holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                    message.answered ? mAnsweredIcon : null, // left
                    null, // top
                    message.hasAttachments ? mAttachmentIcon : null, // right
                    null); // bottom
                holder.position = position;
            }
            else
            {
                holder.chip.getBackground().setAlpha(0);
                holder.subject.setText("No subject");
                holder.subject.setTypeface(null, Typeface.NORMAL);
                holder.from.setText("No sender");
                holder.from.setTypeface(null, Typeface.NORMAL);
                holder.date.setText("No date");
                holder.from.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                //WARNING: Order of the next 2 lines matter
                holder.position = -1;
                holder.selected.setChecked(false);
                holder.flagged.setChecked(false);
            }
            return view;
        }

        public View getFooterView(int position, View convertView, ViewGroup parent)
        {
            if (footerView == null)
            {
                footerView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
                footerView.setId(R.layout.message_list_item_footer);
                FooterViewHolder holder = new FooterViewHolder();
                holder.progress = (ProgressBar)footerView.findViewById(R.id.message_list_progress);
                holder.progress.setIndeterminate(true);
                holder.main = (TextView)footerView.findViewById(R.id.main_text);
                footerView.setTag(holder);
            }

            FooterViewHolder holder = (FooterViewHolder)footerView.getTag();

            if (mCurrentFolder.loading)
            {
                holder.main.setText(getString(R.string.status_loading_more));
                holder.progress.setVisibility(ProgressBar.VISIBLE);
            }
            else
            {
                if (mCurrentFolder.lastCheckFailed == false)
                {
                    holder.main.setText(String.format(getString(R.string.load_more_messages_fmt).toString(), mAccount.getDisplayCount()));
                }
                else
                {
                    holder.main.setText(getString(R.string.status_loading_more_failed));
                }
                holder.progress.setVisibility(ProgressBar.INVISIBLE);
            }

            return footerView;
        }

        public boolean hasStableIds()
        {
            return true;
        }

        public boolean isItemSelectable(int position)
        {
            if (position < mAdapter.messages.size())
            {
                return true;
            }
            else
            {
                return false;
            }
        }

    }

    public class MessageInfoHolder implements Comparable<MessageInfoHolder>
    {
        public String subject;

        public String date;

        public Date compareDate;

        public String compareSubject;

        public String sender;

        public String compareCounterparty;

        public String[] recipients;

        public boolean hasAttachments;

        public String uid;

        public boolean read;

        public boolean answered;

        public boolean flagged;

        public boolean downloaded;

        public boolean partially_downloaded;

        public Message message;

        public FolderInfoHolder folder;

        public boolean selected;

        // Empty constructor for comparison
        public MessageInfoHolder()
        {
            this.selected = false;
        }

        public MessageInfoHolder(Message m, FolderInfoHolder folder)
        {
            this();
            populate(m, folder);
        }

        public void populate(Message m, FolderInfoHolder folder)
        {

            try
            {
                LocalMessage message = (LocalMessage) m;
                Date date = message.getSentDate();
                this.compareDate = date;
                this.folder = folder;

                if (Utility.isDateToday(date))
                {
                    this.date = getTimeFormat().format(date);
                }
                else
                {
                    this.date = getDateFormat().format(date);
                }


                this.hasAttachments = message.getAttachmentCount() > 0;

                this.read = message.isSet(Flag.SEEN);
                this.answered = message.isSet(Flag.ANSWERED);
                this.flagged = message.isSet(Flag.FLAGGED);
                this.downloaded = message.isSet(Flag.X_DOWNLOADED_FULL);
                this.partially_downloaded = message.isSet(Flag.X_DOWNLOADED_PARTIAL);

                Address[] addrs = message.getFrom();

                if (addrs.length > 0 && mAccount.isAnIdentity(addrs[0]))
                {
                    this.compareCounterparty = Address.toFriendly(message .getRecipients(RecipientType.TO));
                    this.sender = String.format(getString(R.string.message_list_to_fmt), this.compareCounterparty);
                }
                else
                {
                    this.sender = Address.toFriendly(addrs);
                    this.compareCounterparty = this.sender;
                }

                this.subject = message.getSubject();

                this.uid = message.getUid();
                this.message = m;

            }
            catch (MessagingException me)
            {
                if (Config.LOGV)
                {
                    Log.v(Email.LOG_TAG, "Unable to load message info", me);
                }
            }
        }

        public boolean equals(Object o)
        {
            if (this.uid.equals(((MessageInfoHolder)o).uid))
            {
                return true;
            }
            else
            {
                return false;
            }
        }


        public int compareTo(MessageInfoHolder o)
        {
            int ascender = (sortAscending ? 1 : -1);
            int comparison = 0;

            if (sortType == SORT_TYPE.SORT_SUBJECT)
            {
                if (compareSubject == null)
                {
                    compareSubject = stripPrefixes(subject).toLowerCase();
                }

                if (o.compareSubject == null)
                {
                    o.compareSubject = stripPrefixes(o.subject).toLowerCase();
                }

                comparison = this.compareSubject.compareTo(o.compareSubject);
            }
            else if (sortType == SORT_TYPE.SORT_SENDER)
            {
                comparison = this.compareCounterparty.toLowerCase().compareTo(o.compareCounterparty.toLowerCase());
            }
            else if (sortType == SORT_TYPE.SORT_FLAGGED)
            {
                comparison = (this.flagged ? 0 : 1) - (o.flagged ? 0 : 1);

            }
            else if (sortType == SORT_TYPE.SORT_UNREAD)
            {
                comparison = (this.read ? 1 : 0) - (o.read ? 1 : 0);
            }
            else if (sortType == SORT_TYPE.SORT_ATTACHMENT)
            {
                comparison = (this.hasAttachments ? 0 : 1) - (o.hasAttachments ? 0 : 1);

            }

            if (comparison != 0)
            {
                return comparison * ascender;
            }

            int dateAscender = (sortDateAscending ? 1 : -1);


            return this.compareDate.compareTo(o.compareDate) * dateAscender;
        }

        Pattern pattern = null;
        String patternString = "^ *(re|aw|fw|fwd): *";
        private String stripPrefixes(String in)
        {
            synchronized (patternString)
            {
                if (pattern == null)
                {
                    pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
                }
            }

            Matcher matcher = pattern.matcher(in);

            int lastPrefix = -1;

            while (matcher.find())
            {
                lastPrefix = matcher.end();
            }

            if (lastPrefix > -1 && lastPrefix < in.length() - 1)
            {
                return in.substring(lastPrefix);
            }
            else
            {
                return in;
            }
        }

    }

    class MessageViewHolder
            implements OnCheckedChangeListener
    {
        public TextView subject;
        public TextView preview;
        public TextView from;
        public TextView time;
        public TextView date;
        public CheckBox flagged;
        public View chip;
        public CheckBox selected;
        public int position = -1;

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            if (position!=-1)
            {
                MessageInfoHolder message = (MessageInfoHolder) mAdapter.getItem(position);
                if (message.selected!=isChecked)
                {
                    if (isChecked)
                    {
                        mSelectedCount++;
                    }
                    else if (mSelectedCount > 0)
                    {
                        mSelectedCount--;
                    }
                    showBatchButtons();
                    message.selected = isChecked;
                }
            }
        }
    }

    private void enableBatchButtons()
    {
        mBatchDeleteButton.setEnabled(true);
        mBatchReadButton.setEnabled(true);
        mBatchFlagButton.setEnabled(true);
    }

    private void disableBatchButtons()
    {
        mBatchDeleteButton.setEnabled(false);
        mBatchReadButton.setEnabled(false);
        mBatchFlagButton.setEnabled(false);

    }
    private void hideBatchButtons()
    {
        //TODO: Fade out animation
        mBatchButtonArea.setVisibility(View.GONE);
    }
    private void showBatchButtons()
    {
        configureBatchButtons();
        //TODO: Fade in animation
        mBatchButtonArea.setVisibility(View.VISIBLE);
    }

    private void configureBatchButtons()
    {
        if (mSelectedCount < 0)
        {
            mSelectedCount = 0;
        }
        if (mSelectedCount==0)
        {
            disableBatchButtons();
        }
        else
        {
            enableBatchButtons();
        }
    }

    class FooterViewHolder
    {
        public ProgressBar progress;
        public TextView main;
    }

    public class FolderInfoHolder
    {
        public String name;

        public String displayName;

        public boolean loading;

        public boolean lastCheckFailed;

        /**
         * Outbox is handled differently from any other folder.
         */
        public boolean outbox;

        public FolderInfoHolder(Folder folder)
        {
            populate(folder);
        }
        public void populate(Folder folder)
        {
            this.name = folder.getName();

            if (this.name.equalsIgnoreCase(Email.INBOX))
            {
                this.displayName = getString(R.string.special_mailbox_name_inbox);
            }
            else
            {
                this.displayName = folder.getName();
            }

            if (this.name.equals(mAccount.getOutboxFolderName()))
            {
                this.displayName = String.format(getString(R.string.special_mailbox_name_outbox_fmt), this.name);
                this.outbox = true;
            }

            if (this.name.equals(mAccount.getDraftsFolderName()))
            {
                this.displayName = String.format(getString(R.string.special_mailbox_name_drafts_fmt), this.name);
            }

            if (this.name.equals(mAccount.getTrashFolderName()))
            {
                this.displayName = String.format(getString(R.string.special_mailbox_name_trash_fmt), this.name);
            }

            if (this.name.equals(mAccount.getSentFolderName()))
            {
                this.displayName = String.format(getString(R.string.special_mailbox_name_sent_fmt), this.name);
            }
        }
    }

    public void onClick(View v)
    {
        boolean newState = false;
        List<Message> messageList = new ArrayList<Message>();
        for (MessageInfoHolder holder : mAdapter.messages)
        {
            if (holder.selected)
            {
                if (v == mBatchDeleteButton)
                {
                    mAdapter.removeMessage(holder);
                }
                else if (v == mBatchFlagButton)
                {
                    if (!holder.flagged)
                    {
                        newState = true;
                    }
                }
                else if (v == mBatchReadButton)
                {
                    if (!holder.read)
                    {
                        newState = true;
                    }
                }
                messageList.add(holder.message);
            }
        }

        for (MessageInfoHolder holder : mAdapter.messages)
        {
            if (holder.selected)
            {
                if (v == mBatchDeleteButton)
                {
                    //nothing
                }
                else if (v == mBatchFlagButton)
                {
                    holder.flagged = newState;
                }
                else if (v == mBatchReadButton)
                {
                    holder.read = newState;
                }
            }
        }

        if (!messageList.isEmpty())
        {
            if (mBatchDeleteButton == v)
            {
                MessagingController.getInstance(getApplication()).deleteMessages(mAccount, mCurrentFolder.name, messageList.toArray(new Message[0]), null);
                mSelectedCount = 0;
                configureBatchButtons();
            }
            else
            {
                MessagingController.getInstance(getApplication()).setFlag(mAccount, mCurrentFolder.name, messageList.toArray(new Message[0]),
                        (v == mBatchReadButton ? Flag.SEEN : Flag.FLAGGED), newState);
            }
        }
        else
        {
            //Should not happen
            Toast.makeText(this, R.string.no_message_seletected_toast, Toast.LENGTH_SHORT).show();
        }
        mHandler.sortMessages();
    }


}
