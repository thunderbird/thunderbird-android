package com.android.email.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ExpandableListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Config;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.R;
import com.android.email.Utility;
import com.android.email.Preferences;
import com.android.email.activity.FolderMessageList.FolderMessageListAdapter.FolderInfoHolder;
import com.android.email.activity.FolderMessageList.FolderMessageListAdapter.MessageInfoHolder;
import com.android.email.activity.setup.AccountSettings;
import com.android.email.activity.setup.FolderSettings;
import com.android.email.mail.Address;
import com.android.email.mail.Flag;
import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.Store;
import com.android.email.mail.Message.RecipientType;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.store.LocalStore.LocalFolder;
import com.android.email.mail.store.LocalStore.LocalMessage;
import com.android.email.mail.store.LocalStore;

/**
 * FolderMessageList is the primary user interface for the program. This
 * Activity shows a two level list of the Account's folders and each folder's
 * messages. From this Activity the user can perform all standard message
 * operations.
 *
 *
 * TODO some things that are slowing us down: Need a way to remove state such as
 * progress bar and per folder progress on resume if the command has completed.
 *
 * TODO Break out seperate functions for: refresh local folders refresh remote
 * folders refresh open folder local messages refresh open folder remote
 * messages
 *
 * And don't refresh remote folders ever unless the user runs a refresh. Maybe
 * not even then.
 */
public class FolderMessageList extends ExpandableListActivity
{
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_CLEAR_NOTIFICATION = "clearNotification";

    private static final String EXTRA_INITIAL_FOLDER = "initialFolder";

	private static final String STATE_KEY_LIST = "com.android.email.activity.folderlist_expandableListState";

	private static final String STATE_KEY_EXPANDED_GROUP = "com.android.email.activity.folderlist_expandedGroup";

	private static final String STATE_KEY_EXPANDED_GROUP_SELECTION = "com.android.email.activity.folderlist_expandedGroupSelection";

	// private static final int UPDATE_FOLDER_ON_EXPAND_INTERVAL_MS = (1000 * 60 *
	// 3);

	private static final int[] colorChipResIds = new int[]
	{ R.drawable.appointment_indicator_leftside_1,
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
			R.drawable.appointment_indicator_leftside_21, };

    private ExpandableListView mListView;

    private int colorChipResId;

    private FolderMessageListAdapter mAdapter;

    private LayoutInflater mInflater;

    private Account mAccount;
    
    private Menu optionsMenu = null;


    /**
	 * Stores the name of the folder that we want to open as soon as possible
	 * after load. It is set to null once the folder has been opened once.
     */
    private String mInitialFolder;

	private int mExpandedGroup = -1;

    private boolean mRestoringState;

    private boolean mRefreshRemote;

    private FolderMessageListHandler mHandler = new FolderMessageListHandler();

	private DateFormat dateFormat = null;

	private DateFormat timeFormat = null;
	
	private boolean thread = false;

	private DateFormat getDateFormat()
	{
		if (dateFormat == null)
		{
	   String dateFormatS = android.provider.Settings.System.getString(getContentResolver(), 
          android.provider.Settings.System.DATE_FORMAT);
      if (dateFormatS != null) {
        dateFormat = new java.text.SimpleDateFormat(dateFormatS);
      }
      else
      {
        dateFormat = new java.text.SimpleDateFormat(Email.BACKUP_DATE_FORMAT);
      }
		}
		return dateFormat;
	}

	private DateFormat getTimeFormat()
	{
		if (timeFormat == null)
		{ 
		  String timeFormatS = android.provider.Settings.System.getString(getContentResolver(), 
		      android.provider.Settings.System.TIME_12_24);
	    boolean b24 =  !(timeFormatS == null || timeFormatS.equals("12"));
	    timeFormat = new java.text.SimpleDateFormat(b24 ? Email.TIME_FORMAT_24 : Email.TIME_FORMAT_12);
		}
		return timeFormat;
	}

	private void clearFormats()
	{
		dateFormat = null;
		timeFormat = null;
	}

	class FolderMessageListHandler extends Handler
	{
        private static final int MSG_PROGRESS = 2;

        private static final int MSG_DATA_CHANGED = 3;

        private static final int MSG_EXPAND_GROUP = 5;

        private static final int MSG_FOLDER_LOADING = 7;

        private static final int MSG_REMOVE_MESSAGE = 11;

        private static final int MSG_SYNC_MESSAGES = 13;

		//private static final int MSG_FOLDER_STATUS = 17;

		private static final int MSG_FOLDER_SYNCING = 18;

		private static final int MSG_SENDING_OUTBOX = 19;
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
                    break;
                case MSG_EXPAND_GROUP:
                    mListView.expandGroup(msg.arg1);
                    break;
                /*
				 * The following functions modify the state of the adapter's underlying
				 * list and must be run here, in the main thread, so that
				 * notifyDataSetChanged is run before any further requests are made to
				 * the adapter.
                 */
				case MSG_FOLDER_LOADING:
				{
                    FolderInfoHolder folder = mAdapter.getFolder((String) msg.obj);
					if (folder != null)
					{
                        folder.loading = msg.arg1 != 0;
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                }
				case MSG_REMOVE_MESSAGE:
				{
                    FolderInfoHolder folder = (FolderInfoHolder) ((Object[]) msg.obj)[0];
                    MessageInfoHolder message = (MessageInfoHolder) ((Object[]) msg.obj)[1];
                    folder.messages.remove(message);
                    mAdapter.notifyDataSetChanged();
                    break;
                }
				case MSG_SYNC_MESSAGES:
				{
                    FolderInfoHolder folder = (FolderInfoHolder) ((Object[]) msg.obj)[0];
                    Message[] messages = (Message[]) ((Object[]) msg.obj)[1];
                    folder.messages.clear();
					for (Message message : messages)
					{
                        mAdapter.addOrUpdateMessage(folder, message, false, false);
                    }
                    Collections.sort(folder.messages);
                    mAdapter.notifyDataSetChanged();
                    break;
                }
//				case MSG_FOLDER_STATUS:
//				{
//					String folderName = (String) ((Object[]) msg.obj)[0];
//					String status = (String) ((Object[]) msg.obj)[1];
//					FolderInfoHolder folder = mAdapter.getFolder(folderName);
//					if (folder != null)
//					{
//						folder.status = status;
//						mAdapter.notifyDataSetChanged();
//					}
//					break;
//				}
				case MSG_FOLDER_SYNCING:
				{
                    String folderName = (String) ((Object[]) msg.obj)[0];
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

		public void synchronizeMessages(FolderInfoHolder folder, Message[] messages)
		{
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SYNC_MESSAGES;
			msg.obj = new Object[]
			{ folder, messages };
            sendMessage(msg);
        }

		public void removeMessage(FolderInfoHolder folder, MessageInfoHolder message)
		{
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_REMOVE_MESSAGE;
			msg.obj = new Object[]
			{ folder, message };
            sendMessage(msg);
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

		public void dataChanged()
		{
            sendEmptyMessage(MSG_DATA_CHANGED);
        }

		public void expandGroup(int groupPosition)
		{
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_EXPAND_GROUP;
            msg.arg1 = groupPosition;
            sendMessage(msg);
        }

//		public void folderStatus(String folder, String status)
//		{
//			android.os.Message msg = new android.os.Message();
//			msg.what = MSG_FOLDER_STATUS;
//			msg.obj = new String[]
//			{ folder, status };
//			sendMessage(msg);
//		}

		public void folderSyncing(String folder)
		{
            android.os.Message msg = new android.os.Message();
			msg.what = MSG_FOLDER_SYNCING;
			msg.obj = new String[]
			{ folder };
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
	class FolderUpdateWorker implements Runnable
	{
        String mFolder;

        boolean mSynchronizeRemote;

        /**
		 * Create a worker for the given folder and specifying whether the worker
		 * should synchronize the remote folder or just the local one.
		 * 
         * @param folder
         * @param synchronizeRemote
         */
		public FolderUpdateWorker(String folder, boolean synchronizeRemote)
		{
            mFolder = folder;
            mSynchronizeRemote = synchronizeRemote;
        }

		public void run()
		{
            // Lower our priority
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            // Synchronously load the list of local messages
            
			try
			{
				Store localStore = Store.getInstance(mAccount.getLocalStoreUri(),
						getApplication());
				LocalFolder localFolder = (LocalFolder) localStore.getFolder(mFolder);
				if (localFolder.getMessageCount() == 0 && localFolder.getLastChecked() <= 0)
				{
					mSynchronizeRemote = true;
				}
			} catch (MessagingException me)
			{
				Log.e(Email.LOG_TAG,
						"Unable to get count of local messages for folder " + mFolder, me);
			}

			if (mSynchronizeRemote)
			{
                // Tell the MessagingController to run a remote update of this folder
                // at it's leisure
                MessagingController.getInstance(getApplication()).synchronizeMailbox(
						mAccount, mFolder, mAdapter.mListener);
      }
			else
			{
			  MessagingController.getInstance(getApplication()).listLocalMessages(
	          mAccount, mFolder, mAdapter.mListener);
			}
        }
    }

	public static void actionHandleAccount(Context context, Account account,
			String initialFolder)
	{
        Intent intent = new Intent(context, FolderMessageList.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
		if (initialFolder != null)
		{
            intent.putExtra(EXTRA_INITIAL_FOLDER, initialFolder);
        }
        context.startActivity(intent);
    }

	public static void actionHandleAccount(Context context, Account account)
	{
        actionHandleAccount(context, account, null);
    }

	public static Intent actionHandleAccountIntent(Context context,
			Account account, String initialFolder)
	{
        Intent intent = new Intent(context, FolderMessageList.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        intent.putExtra(EXTRA_CLEAR_NOTIFICATION, true);
		if (initialFolder != null)
		{
            intent.putExtra(EXTRA_INITIAL_FOLDER, initialFolder);
        }
        return intent;
    }

	public static Intent actionHandleAccountIntent(Context context,
			Account account)
	{
        return actionHandleAccountIntent(context, account, null);
    }

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mListView = getExpandableListView();
        mListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        mListView.setLongClickable(true);
        registerForContextMenu(mListView);

        /*
		 * We manually save and restore the list's state because our adapter is
		 * slow.
         */
        mListView.setSaveEnabled(false);

        getExpandableListView().setGroupIndicator(
                getResources().getDrawable(R.drawable.expander_ic_folder));

        mInflater = getLayoutInflater();

        Intent intent = getIntent();
        mAccount = (Account)intent.getSerializableExtra(EXTRA_ACCOUNT);

		// Take the initial folder into account only if we are *not* restoring the
		// activity already
		if (savedInstanceState == null)
		{
            mInitialFolder = intent.getStringExtra(EXTRA_INITIAL_FOLDER);
        }

        /*
		 * Since the color chip is always the same color for a given account we just
		 * cache the id of the chip right here.
         */
		colorChipResId = colorChipResIds[mAccount.getAccountNumber()
				% colorChipResIds.length];

        mAdapter = new FolderMessageListAdapter();

        final Object previousData = getLastNonConfigurationInstance();
		if (previousData != null)
		{
            //noinspection unchecked
            mAdapter.mFolders = (ArrayList<FolderInfoHolder>) previousData;
        }

        setListAdapter(mAdapter);

		if (savedInstanceState != null)
		{
            mRestoringState = true;
            onRestoreListState(savedInstanceState);
            mRestoringState = false;
        }

        setTitle(mAccount.getDescription());
    }

	private void onRestoreListState(Bundle savedInstanceState)
	{
		final int expandedGroup = savedInstanceState.getInt(
				STATE_KEY_EXPANDED_GROUP, -1);
		
		if (expandedGroup >= 0 && mAdapter.getGroupCount() > expandedGroup)
		{
            mListView.expandGroup(expandedGroup);
			long selectedChild = savedInstanceState.getLong(
					STATE_KEY_EXPANDED_GROUP_SELECTION, -1);
			if (selectedChild != ExpandableListView.PACKED_POSITION_VALUE_NULL)
			{
                mListView.setSelection(mListView.getFlatListPosition(selectedChild));
            }
        }
		mListView.onRestoreInstanceState(savedInstanceState
				.getParcelable(STATE_KEY_LIST));
    }

    @Override
	public Object onRetainNonConfigurationInstance()
	{
        return mAdapter.mFolders;
    }

    @Override
	public void onPause()
	{
        super.onPause();
		MessagingController.getInstance(getApplication()).removeListener(
				mAdapter.mListener);
    }

    /**
	 * On resume we refresh the folder list (in the background) and we refresh the
	 * messages for any folder that is currently open. This guarantees that things
	 * like unread message count and read status are updated.
     */
    @Override
	public void onResume()
	{
    super.onResume();
		clearFormats();

		MessagingController.getInstance(getApplication()).addListener(
				mAdapter.mListener);
    mAccount.refresh(Preferences.getPreferences(this));
    onRefresh(false);
		
		NotificationManager notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifMgr.cancel(mAccount.getAccountNumber());
    thread = MessagingController.getInstance(getApplication()).isThreading();


  }

    @Override
	public void onSaveInstanceState(Bundle outState)
	{
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_KEY_LIST, mListView.onSaveInstanceState());
        outState.putInt(STATE_KEY_EXPANDED_GROUP, mExpandedGroup);
		outState.putLong(STATE_KEY_EXPANDED_GROUP_SELECTION, mListView
				.getSelectedPosition());
    }

    @Override
	public void onGroupCollapse(int groupPosition)
	{
        super.onGroupCollapse(groupPosition);
        mExpandedGroup = -1;
    }

    @Override
	public void onGroupExpand(int groupPosition)
	{
        super.onGroupExpand(groupPosition);
		if (mExpandedGroup != -1)
		{
            mListView.collapseGroup(mExpandedGroup);
        }
        mExpandedGroup = groupPosition;

		if (!mRestoringState)
		{
            /*
             * Scroll the selected item to the top of the screen.
             */
			int position = mListView.getFlatListPosition(ExpandableListView
					.getPackedPositionForGroup(groupPosition));
            mListView.setSelectionFromTop(position, 0);
        }

		final FolderInfoHolder folder = (FolderInfoHolder) mAdapter
				.getGroup(groupPosition);
		if (folder.messages.size() == 0 || folder.needsRefresh)
		{
		  folder.needsRefresh = false;
			new Thread(new FolderUpdateWorker(folder.name, false)).start();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	//Shortcuts that work no matter what is selected
    	switch (keyCode) {
	        case KeyEvent.KEYCODE_C: { onCompose(); return true;}
	        case KeyEvent.KEYCODE_Q: { onAccounts(); return true; }
	        case KeyEvent.KEYCODE_S: { onEditAccount(); return true; }
	        case KeyEvent.KEYCODE_T: { onToggleThread(); return true; }
	        case KeyEvent.KEYCODE_H: {
	            Toast toast = Toast.makeText(this, R.string.message_list_help_key, Toast.LENGTH_LONG);
	            toast.show();
	            return true; }
	    }//switch
    	
    	long packedPosition = mListView.getSelectedPosition();
       int group = ExpandableListView
           .getPackedPositionGroup(packedPosition);
       int child = ExpandableListView
           .getPackedPositionChild(packedPosition);

        try {
          if (group >= 0 && child >= 0)
          {
          
              MessageInfoHolder message = (MessageInfoHolder) mAdapter.getChild(group, child);
              if (message != null)
              {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL: { onDelete(message); return true;}
                    case KeyEvent.KEYCODE_D: { onDelete(message); return true;}
                    case KeyEvent.KEYCODE_F: { onForward(message); return true;}
                    case KeyEvent.KEYCODE_A: { onReplyAll(message); return true; }
                    case KeyEvent.KEYCODE_R: { onReply(message); return true; }
                    case KeyEvent.KEYCODE_G: { onToggleFlag(message); return true; }
                }
              }
          }
        }
        finally {
          return super.onKeyDown(keyCode, event);
     }
    }//onKeyDown


    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
            int childPosition, long id) {
        FolderInfoHolder folder = (FolderInfoHolder) mAdapter.getGroup(groupPosition);

		if (!folder.outbox && childPosition == folder.messages.size() && !folder.loading)
		{
			if (folder.status == null)
			{
                MessagingController.getInstance(getApplication()).loadMoreMessages(
						mAccount, folder.name, mAdapter.mListener);
                return false;
			} else
			{
                MessagingController.getInstance(getApplication()).synchronizeMailbox(
						mAccount, folder.name, mAdapter.mListener);
                return false;
            }
		} else if (childPosition >= folder.messages.size())
		{
            return false;
        }
		MessageInfoHolder message = (MessageInfoHolder) mAdapter.getChild(
				groupPosition, childPosition);

        onOpenMessage(folder, message);

        return true;
    }

	private void onRefresh(final boolean forceRemote)
	{
		if (forceRemote)
		{
            mRefreshRemote = true;
        }
		new Thread()
		{
			public void run()
			{
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				MessagingController.getInstance(getApplication()).listFolders(mAccount,
						forceRemote, mAdapter.mListener);
				if (forceRemote)
				{
					MessagingController.getInstance(getApplication())
							.sendPendingMessages(mAccount, null);
                }
            }
        }.start();
    }

	private void onOpenMessage(FolderInfoHolder folder, MessageInfoHolder message)
	{
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
            mHandler.dataChanged();
        }

		if (folder.name.equals(mAccount.getDraftsFolderName()))
		{
            MessageCompose.actionEditDraft(this, mAccount, message.message);
		} else
		{
            ArrayList<String> folderUids = new ArrayList<String>();
			for (MessageInfoHolder holder : folder.messages)
			{
                folderUids.add(holder.uid);
            }
			MessageView.actionView(this, mAccount, folder.name, message.uid,
					folderUids);
        }
    }

	private void onEditAccount()
	{
        AccountSettings.actionSettings(this, mAccount);
    }

	private void onEditFolder(Account account, String folderName)
	{
		FolderSettings.actionSettings(this, account, folderName);
	}


	private void onAccounts()
	{
        startActivity(new Intent(this, Accounts.class));
        finish();
    }

	private void onCompose()
	{
        MessageCompose.actionCompose(this, mAccount);
    }
	
	private void onToggleThread()
	{
	  thread = !thread;
	  
	  for (FolderInfoHolder folder : mAdapter.mFolders)
	  { 
  	  Collections.sort(folder.messages);
	  }
	  mAdapter.notifyDataSetChanged();
	  setMenuThread();
	  MessagingController.getInstance(getApplication()).setThreading(thread);
	  
	}

	private void onDelete(MessageInfoHolder holder)
	{
		if (holder.read == false && holder.folder.unreadMessageCount > 0)
		{
		  holder.folder.unreadMessageCount--;
		}
		FolderInfoHolder trashHolder = mAdapter.getFolder(mAccount.getTrashFolderName());
		if (trashHolder != null)
		{
		  trashHolder.needsRefresh = true;
		}

    mAdapter.removeMessage(holder.message.getFolder().getName(), holder.uid);
    
    MessagingController.getInstance(getApplication()).deleteMessage(mAccount,
        holder.message.getFolder().getName(), holder.message, null);

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

	private void onMarkAllAsRead(Account account, FolderInfoHolder folder)
	{
		MessagingController.getInstance(getApplication()).markAllMessagesRead(mAccount,
				folder.name);
		
		for (MessageInfoHolder holder : folder.messages){
			holder.read = true;
		}
		folder.unreadMessageCount = 0;
    mHandler.dataChanged();

  		//onRefresh(false);
	}
	
	private void onEmptyTrash(final Account account)
	{
	  mAdapter.removeAllMessages(account.getTrashFolderName());
	  mHandler.dataChanged();
	  
	  MessagingListener listener = new MessagingListener() 
	  {
	    @Override
	    public void controllerCommandCompleted(boolean moreToDo)
	    {
	      Log.v(Email.LOG_TAG, "Empty Trash background task completed");
	    }
	  };
	  
		MessagingController.getInstance(getApplication()).emptyTrash(account, listener);
	}


	private void onToggleRead(MessageInfoHolder holder)
	{
	  
	  holder.folder.unreadMessageCount += (holder.read ? 1 : -1);
	  if (holder.folder.unreadMessageCount < 0)
	  {
	    holder.folder.unreadMessageCount = 0;
	  }
	  
		MessagingController.getInstance(getApplication()).markMessageRead(mAccount,
				holder.message.getFolder().getName(), holder.uid, !holder.read);
        holder.read = !holder.read;
        mHandler.dataChanged();

      //  onRefresh(false);
    }

	private void onToggleFlag(MessageInfoHolder holder)
  {
    
    MessagingController.getInstance(getApplication()).setMessageFlag(mAccount,
        holder.message.getFolder().getName(), holder.uid, Flag.FLAGGED, !holder.flagged);
        holder.flagged = !holder.flagged;
        mHandler.dataChanged();
    }
	
    @Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
            case R.id.refresh:
                onRefresh(true);
                return true;
            case R.id.accounts:
                onAccounts();
                return true;
            case R.id.compose:
                onCompose();
                return true;
            case R.id.account_settings:
                onEditAccount();
                return true;
            case R.id.thread:
              onToggleThread();
              return true;
			case R.id.empty_trash:
				onEmptyTrash(mAccount);
				return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.folder_message_list_option, menu);
        optionsMenu = menu;
        setMenuThread();
        return true;
    }
    
    private void setMenuThread()
    {
      Menu menu = optionsMenu;
      if (menu != null)
      {
        MenuItem threadItem = menu.findItem(R.id.thread);
        if (threadItem != null)
        {
          threadItem.setTitle(thread ? R.string.unthread_action : R.string.thread_action);
          threadItem.setIcon(thread ? R.drawable.ic_menu_unthread : R.drawable.ic_menu_thread);
        }
      }
    }

    @Override
	public boolean onContextItemSelected(MenuItem item)
	{
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		int groupPosition = ExpandableListView
				.getPackedPositionGroup(info.packedPosition);
		int childPosition = ExpandableListView
				.getPackedPositionChild(info.packedPosition);
		FolderInfoHolder folder = (FolderInfoHolder) mAdapter
				.getGroup(groupPosition);
		if (childPosition >= 0 && childPosition < mAdapter.getChildrenCount(groupPosition))
		{
			MessageInfoHolder holder = (MessageInfoHolder) mAdapter.getChild(
					groupPosition, childPosition);
			switch (item.getItemId())
			{
                case R.id.open:
                    onOpenMessage(folder, holder);
                    break;
                case R.id.delete:
                    onDelete(holder);
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
				case R.id.send_alternate:
					onSendAlternate(mAccount, holder);
					break;
	
            }
        }
		else
		{
			switch (item.getItemId())
			{
				case R.id.refresh:
					if (folder.outbox)
					{
							Log.i(Email.LOG_TAG, "sending pending messages from " + folder.name);
							MessagingController.getInstance(getApplication())
							.sendPendingMessages(mAccount, null);
					}
					else
					{
						Log.i(Email.LOG_TAG, "refresh folder " + folder.name);
						new Thread(new FolderUpdateWorker(folder.name, true)).start();
					}
					break;
				case R.id.folder_settings:
					Log.i(Email.LOG_TAG, "edit folder settings for " + folder.name);
					onEditFolder(mAccount, folder.name);
					break;
				case R.id.empty_trash:
					Log.i(Email.LOG_TAG, "empty trash");
					onEmptyTrash(mAccount);
					break;
				case R.id.mark_all_as_read:
					Log.i(Email.LOG_TAG, "mark all unread messages as read " + folder.name);
					onMarkAllAsRead(mAccount, folder);
					break;

			}
		}
		
        return super.onContextItemSelected(item);
    }

	public void onSendAlternate(Account account, MessageInfoHolder holder)
	{
			MessagingController.getInstance(getApplication()).sendAlternate(this, account, holder.message);
	}

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
		{
            long packedPosition = info.packedPosition;
			int groupPosition = ExpandableListView
					.getPackedPositionGroup(packedPosition);
			int childPosition = ExpandableListView
					.getPackedPositionChild(packedPosition);
			FolderInfoHolder folder = (FolderInfoHolder) mAdapter
					.getGroup(groupPosition);
//			if (folder.outbox)
//			{
//                return;
//            }
			if (childPosition < folder.messages.size())
			{
                getMenuInflater().inflate(R.menu.folder_message_list_context, menu);
				MessageInfoHolder message = (MessageInfoHolder) mAdapter.getChild(
						groupPosition, childPosition);
				if (message.read)
				{
					menu.findItem(R.id.mark_as_read).setTitle(
							R.string.mark_as_unread_action);
        }
				if (message.flagged)
				{
				  menu.findItem(R.id.flag).setTitle(
              R.string.unflag_action);
				}
            }
		} else if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_GROUP)
		{
			getMenuInflater().inflate(R.menu.folder_context, menu);

			long packedPosition = info.packedPosition;
			int groupPosition = ExpandableListView
					.getPackedPositionGroup(packedPosition);
			FolderInfoHolder folder = (FolderInfoHolder) mAdapter
					.getGroup(groupPosition);
	
			if (!folder.name.equals(mAccount.getTrashFolderName()))
			{
				menu.findItem(R.id.empty_trash).setVisible(false);
        }
			menu.setHeaderTitle(R.string.folder_context_menu_title);
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

	class FolderMessageListAdapter extends BaseExpandableListAdapter
	{
        private ArrayList<FolderInfoHolder> mFolders = new ArrayList<FolderInfoHolder>();

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
				if (Config.LOGV)
				{
                    Log.v(Email.LOG_TAG, "listFoldersFailed " + message);
                }
            }

            @Override
			public void listFoldersFinished(Account account)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                mHandler.progress(false);
				if (mInitialFolder != null)
				{
                    int groupPosition = getFolderPosition(mInitialFolder);
                    mInitialFolder = null;
					if (groupPosition != -1)
					{
                        mHandler.expandGroup(groupPosition);
                    }
                }
				mHandler.dataChanged();
            }

            @Override
			public void listFolders(Account account, Folder[] folders)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
				ArrayList<FolderInfoHolder> newFolders = new ArrayList<FolderInfoHolder>();

				Account.FolderMode aMode = account.getFolderDisplayMode();
				Preferences prefs = Preferences.getPreferences(getApplication().getApplicationContext());

				for (Folder folder : folders)
				{
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
						Log.e(Email.LOG_TAG, "Couldn't get prefs to check for displayability of folder " + folder.getName(), me);
					}

					
                    FolderInfoHolder holder = getFolder(folder.getName());

					if (holder == null)
					{
                        holder = new FolderInfoHolder();
                    }
					newFolders.add(holder);
					int unreadCount = 0;
					try
					{
						folder.open(Folder.OpenMode.READ_WRITE);
						unreadCount = folder.getUnreadMessageCount();
					} catch (MessagingException me)
					{
						Log.e(Email.LOG_TAG, "Folder.getUnreadMessageCount() failed", me);
					}
                    holder.name = folder.getName();
					if (holder.name.equalsIgnoreCase(Email.INBOX))
					{
                        holder.displayName = getString(R.string.special_mailbox_name_inbox);
					} else
					{
                        holder.displayName = folder.getName();
                    }
					if (holder.name.equals(mAccount.getOutboxFolderName()))
					{
                        holder.outbox = true;
                    }
					if (holder.messages == null)
					{
                        holder.messages = new ArrayList<MessageInfoHolder>();
                    }
					holder.lastChecked = folder.getLastChecked();
					String mess = truncateStatus(folder.getStatus());
					
					holder.status = mess;

					holder.unreadMessageCount = unreadCount;
					try
					{
					  folder.close(false);
					} catch (MessagingException me)
					{
						Log.e(Email.LOG_TAG, "Folder.close() failed", me);
                    }
        }

				mFolders.clear();
				mFolders.addAll(newFolders);
                Collections.sort(mFolders);
                mHandler.dataChanged();

                /*
				 * We will do this eventually. This restores the state of the list in
				 * the case of a killed Activity but we have some message sync issues to
				 * take care of.
                 */
//                if (mRestoredState != null) {
//                    if (Config.LOGV) {
//                        Log.v(Email.LOG_TAG, "Attempting to restore list state");
//                    }
//                    Parcelable listViewState =
//                    mListView.onRestoreInstanceState(mListViewState);
//                    mListViewState = null;
//                }
                /*
                 * Now we need to refresh any folders that are currently expanded. We do this
                 * in case the status or number of messages has changed.
                 */
				for (int i = 0, count = getGroupCount(); i < count; i++)
				{
					if (mListView.isGroupExpanded(i))
					{
						final FolderInfoHolder folder = (FolderInfoHolder) mAdapter
								.getGroup(i);
						new Thread(new FolderUpdateWorker(folder.name, mRefreshRemote))
								.start();
                    }
                }
                mRefreshRemote = false;
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
			public void listLocalMessagesFailed(Account account, String folder,
					String message)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
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
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }

            @Override
			public void listLocalMessages(Account account, String folder,
					Message[] messages)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                synchronizeMessages(folder, messages);
            }

            @Override
			public void synchronizeMailboxStarted(Account account, String folder)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                mHandler.progress(true);
                mHandler.folderLoading(folder, true);
//				mHandler.folderStatus(folder, getString(R.string.status_loading));
				mHandler.folderSyncing(folder);
            }

            @Override
			public void synchronizeMailboxFinished(Account account, String folder,
					int totalMessagesInMailbox, int numNewMessages)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
				FolderInfoHolder holder = getFolder(folder);
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
			//	mHandler.folderStatus(folder, null);
				mHandler.folderSyncing(null);

                onRefresh(false);
            }

            @Override
			public void synchronizeMailboxFailed(Account account, String folder,
					String message)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
        
        
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
				
	//			String mess = truncateStatus(message);
				
	//			mHandler.folderStatus(folder, mess);
                FolderInfoHolder holder = getFolder(folder);
				if (holder != null)
				{
                    holder.lastChecked = 0;
                }
				mHandler.folderSyncing(null);
            }

            @Override
			public void synchronizeMailboxNewMessage(Account account, String folder,
					Message message)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                addOrUpdateMessage(folder, message);
            }

            
            
            @Override
              public void messageDeleted(Account account,
                  String folder, Message message)
            {
              synchronizeMailboxRemovedMessage(account,
                  folder, message);
            }
              
              @Override  
			public void synchronizeMailboxRemovedMessage(Account account,
					String folder, Message message)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                removeMessage(folder, message.getUid());
            }

            @Override
			public void emptyTrashCompleted(Account account)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                onRefresh(false);
            }

            @Override
			public void folderStatusChanged(Account account, String folderName)
			{
				if (!account.equals(mAccount))
				{
                    return;
                }
                onRefresh(false);
            }

            @Override
			public void sendPendingMessagesCompleted(Account account)
			{
				if (!account.equals(mAccount))
				{
					return;
				}
				mHandler.sendingOutbox(false);
				onRefresh(false);
			}
			
			@Override
			public void sendPendingMessagesStarted(Account account)
			{
				if (!account.equals(mAccount))
				{
					return;
				}
				mHandler.sendingOutbox(true);
			}
			
			@Override
			public void sendPendingMessagesFailed(Account account)
			{
				if (!account.equals(mAccount))
				{
					return;
				}
				mHandler.sendingOutbox(false);
			}

			@Override
			public void messageUidChanged(Account account, String folder,
					String oldUid, String newUid)
			{
				if (mAccount.equals(account))
				{
                    FolderInfoHolder holder = getFolder(folder);
					if (holder != null)
					{
						for (MessageInfoHolder message : holder.messages)
						{
							if (message.uid.equals(oldUid))
							{
                                message.uid = newUid;
                                message.message.setUid(newUid);
                            }
                        }
                    }
                }
            }
        };

        private Drawable mAttachmentIcon;
        private Drawable mAnsweredIcon;

		FolderMessageListAdapter()
		{
			mAttachmentIcon = getResources().getDrawable(
					R.drawable.ic_mms_attachment_small);
			mAnsweredIcon = getResources().getDrawable(
          R.drawable.ic_mms_answered_small);
        }
		
		public void removeAllMessages(String folder)
		{
	     FolderInfoHolder f = getFolder(folder);
	      if (f == null)
	      {
	                return;
	            }
	      for (MessageInfoHolder m : f.messages)
	      {
	        removeMessage(folder, m.uid);
	      }
	      f.unreadMessageCount = 0;
		}
		
		public void removeMessage(String folder, String messageUid)
		{
            FolderInfoHolder f = getFolder(folder);
			if (f == null)
			{
                return;
            }
            MessageInfoHolder m = getMessage(f, messageUid);
			if (m == null)
			{
                return;
            }
			
            mHandler.removeMessage(f, m);
        }

		public void synchronizeMessages(String folder, Message[] messages)
		{
            FolderInfoHolder f = getFolder(folder);
			if (f == null)
			{
                return;
            }
            mHandler.synchronizeMessages(f, messages);
        }

		public void addOrUpdateMessage(String folder, Message message)
		{
            addOrUpdateMessage(folder, message, true, true);
        }

        private void addOrUpdateMessage(FolderInfoHolder folder, Message message,
				boolean sort, boolean notify)
		{
            MessageInfoHolder m = getMessage(folder, message.getUid());
			if (m == null)
			{
                m = new MessageInfoHolder(message, folder);
                folder.messages.add(m);
			} else
			{
                m.populate(message, folder);
            }
			if (sort)
			{
                Collections.sort(folder.messages);
            }
			if (notify)
			{
                mHandler.dataChanged();
            }
        }

        private void addOrUpdateMessage(String folder, Message message,
				boolean sort, boolean notify)
		{
            FolderInfoHolder f = getFolder(folder);
			if (f == null)
			{
                return;
            }
            addOrUpdateMessage(f, message, sort, notify);
        }

		public MessageInfoHolder getMessage(FolderInfoHolder folder,
				String messageUid)
		{
			for (MessageInfoHolder message : folder.messages)
			{
				if (message.uid.equals(messageUid))
				{
                    return message;
                }
            }
            return null;
        }

		public int getGroupCount()
		{
          return mFolders.size();
        }

		public long getGroupId(int groupPosition)
		{
            return groupPosition;
        }

		public Object getGroup(int groupPosition)
		{
		   try{
            return mFolders.get(groupPosition);
          }
          catch (Exception e)
          {
            Log.e(Email.LOG_TAG, "getGroup(" + groupPosition + "), but mFolders.size() = " + mFolders.size(), e);
            return null;
          }
        }

		public FolderInfoHolder getFolder(String folder)
		{
            FolderInfoHolder folderHolder = null;
			for (int i = 0, count = getGroupCount(); i < count; i++)
			{
                FolderInfoHolder holder = (FolderInfoHolder) getGroup(i);
				if (holder != null && holder.name != null && holder.name.equals(folder))
				{
                    folderHolder = holder;
                }
            }
            return folderHolder;
        }

        /**
		 * Gets the group position of the given folder or returns -1 if the folder
		 * is not found.
		 * 
         * @param folder
         * @return
         */
		public int getFolderPosition(String folder)
		{
			for (int i = 0, count = getGroupCount(); i < count; i++)
			{
                FolderInfoHolder holder = (FolderInfoHolder) getGroup(i);
				if (holder != null && holder.name.equals(folder))
				{
                    return i;
                }
            }
            return -1;
        }

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent)
		{
            FolderInfoHolder folder = (FolderInfoHolder) getGroup(groupPosition);
            View view;
			if (convertView != null)
			{
                view = convertView;
			} else
			{
				view = mInflater.inflate(R.layout.folder_message_list_group, parent,
						false);
            }
            FolderViewHolder holder = (FolderViewHolder) view.getTag();
			if (holder == null)
			{
                holder = new FolderViewHolder();
                holder.folderName = (TextView) view.findViewById(R.id.folder_name);
				holder.newMessageCount = (TextView) view
						.findViewById(R.id.folder_unread_message_count);
                holder.folderStatus = (TextView) view.findViewById(R.id.folder_status);
                view.setTag(holder);
            }
			
			if (folder == null)
			{
			  return view;
			}
			
      holder.folderName.setText(folder.displayName);

			String statusText = "";

			if (folder.loading)
			{
				statusText = getString(R.string.status_loading);
            }
			else if (folder.status != null)
			{
				statusText = folder.status;
			} 
			else if (folder.lastChecked != 0)
			{
				Date lastCheckedDate = new Date(folder.lastChecked);

				statusText = (getDateFormat().format(lastCheckedDate) + " " + getTimeFormat()
						.format(lastCheckedDate));
			}
			if (statusText != null)
			{
				holder.folderStatus.setText(statusText);
                holder.folderStatus.setVisibility(View.VISIBLE);
            }
			else
			{
				holder.folderStatus.setText(null);
				holder.folderStatus.setVisibility(View.GONE);
			}

			if (folder.unreadMessageCount != 0)
			{
				holder.newMessageCount.setText(Integer
						.toString(folder.unreadMessageCount));
                holder.newMessageCount.setVisibility(View.VISIBLE);
			} else
			{
                holder.newMessageCount.setVisibility(View.GONE);
            }
            return view;
        }

		public int getChildrenCount(int groupPosition)
		{
            FolderInfoHolder folder = (FolderInfoHolder) getGroup(groupPosition);
            if (folder == null || folder.messages == null)
            {
              return 0;
            }
            return folder.messages.size() + 1;
        }

		public long getChildId(int groupPosition, int childPosition)
		{
            FolderInfoHolder folder = (FolderInfoHolder) getGroup(groupPosition);
			if (childPosition < folder.messages.size())
			{
                MessageInfoHolder holder = folder.messages.get(childPosition);
                return ((LocalStore.LocalMessage) holder.message).getId();
			} else
			{
                return -1;
            }
        }

		public Object getChild(int groupPosition, int childPosition)
		{
			FolderInfoHolder folder = null;
			try
			{
				folder = (FolderInfoHolder) getGroup(groupPosition);
				if (folder == null)
				{
					Log.e(Email.LOG_TAG, "Got null folder while retrieving groupPosition " + groupPosition);
				}
            return folder.messages.get(childPosition);
        }
			catch (Exception e)
			{
				Log.e(Email.LOG_TAG, "getChild(" + groupPosition + ", "+ childPosition + "), but folder.messages.size() = " + folder.messages.size(), e);
				return null;
			}
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent)
		{
            FolderInfoHolder folder = (FolderInfoHolder) getGroup(groupPosition);
			if (isLastChild)
			{
                View view;
                if ((convertView != null)
						&& (convertView.getId() == R.layout.folder_message_list_child_footer))
				{
                    view = convertView;
				} else
				{
                    view = mInflater.inflate(R.layout.folder_message_list_child_footer,
                            parent, false);
                    view.setId(R.layout.folder_message_list_child_footer);
                }
                FooterViewHolder holder = (FooterViewHolder) view.getTag();
				if (holder == null)
				{
                    holder = new FooterViewHolder();
                    holder.progress = (ProgressBar) view.findViewById(R.id.progress);
                    holder.main = (TextView) view.findViewById(R.id.main_text);
                    view.setTag(holder);
                }
				if (folder.loading)
				{
                    holder.main.setText(getString(R.string.status_loading_more));
                    holder.progress.setVisibility(View.VISIBLE);
				} else
				{
					if (folder.lastCheckFailed == false)
					{
					    holder.main.setText(String.format(getString(R.string.load_more_messages_fmt).toString(), 
									      mAccount.getDisplayCount()));
					} else
					{
                        holder.main.setText(getString(R.string.status_loading_more_failed));
                    }
                    holder.progress.setVisibility(View.GONE);
                }
                return view;
			} else
			{
				MessageInfoHolder message = (MessageInfoHolder) getChild(groupPosition,
						childPosition);
                View view;
                if ((convertView != null)
						&& (convertView.getId() != R.layout.folder_message_list_child_footer))
				{
                    view = convertView;
				} else
				{
					view = mInflater.inflate(R.layout.folder_message_list_child, parent,
							false);
                }
                MessageViewHolder holder = (MessageViewHolder) view.getTag();
				if (holder == null)
				{
                    holder = new MessageViewHolder();
                    holder.subject = (TextView) view.findViewById(R.id.subject);
                    holder.from = (TextView) view.findViewById(R.id.from);
                    holder.date = (TextView) view.findViewById(R.id.date);
                    holder.chip = view.findViewById(R.id.chip);
                    /*
					 * TODO The line below and the commented lines a bit further down are
					 * work in progress for outbox status. They should not be removed.
                     */
//                    holder.status = (TextView) view.findViewById(R.id.status);
                    /*
                     * This will need to move to below if we ever convert this whole thing
                     * to a combined inbox.
                     */
                    holder.chip.setBackgroundResource(colorChipResId);

                    view.setTag(holder);
                }
				if (message != null)
				{
                holder.chip.getBackground().setAlpha(message.read ? 0 : 255);
               
                holder.subject.setTypeface(null, 
                    message.read && !message.flagged ? Typeface.NORMAL  : Typeface.BOLD);
                
                if (message.flagged)
                {
                  holder.subject.setTextColor(Email.FLAGGED_COLOR);
                }
                else
                {
                  holder.subject.setTextColor(0xffffffff);
                }
                holder.subject.setText(message.subject);
                
                holder.from.setText(message.sender);
                holder.from.setTypeface(null, message.read ? Typeface.NORMAL : Typeface.BOLD);
                holder.date.setText(message.date);
                holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                    message.answered ? mAnsweredIcon : null, // left 
                        null, // top
                        message.hasAttachments ? mAttachmentIcon : null, // right 
                            null); // bottom
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
				}
//                if (folder.outbox) {
//                    holder.status.setText("Sending");
//                }
//                else {
//                    holder.status.setText("");
//                }
                return view;
            }
        }

		public boolean hasStableIds()
		{
            return true;
        }

		public boolean isChildSelectable(int groupPosition, int childPosition)
		{
            return childPosition < getChildrenCount(groupPosition);
        }

		public class FolderInfoHolder implements Comparable<FolderInfoHolder>
		{
            public String name;

            public String displayName;

            public ArrayList<MessageInfoHolder> messages;

            public long lastChecked;

            public int unreadMessageCount;

            public boolean loading;

            public String status;

            public boolean lastCheckFailed;
            
            public boolean needsRefresh = false;
            
            /**
             * Outbox is handled differently from any other folder.
             */
            public boolean outbox;

			public int compareTo(FolderInfoHolder o)
			{
                String s1 = this.name;
                String s2 = o.name;
				if (Email.INBOX.equalsIgnoreCase(s1))
				{
                    return -1;
				} else if (Email.INBOX.equalsIgnoreCase(s2))
				{
                    return 1;
                } else
                    return s1.compareToIgnoreCase(s2);
            }
        }

		public class MessageInfoHolder implements Comparable<MessageInfoHolder>
		{
            public String subject;

            public String date;

            public Date compareDate;
            
            public String compareSubject;

            public String sender;
            
            public String[] recipients;

            public boolean hasAttachments;

            public String uid;

            public boolean read;
            
            public boolean answered;
            
            public boolean flagged;

            public Message message;
            
            public FolderInfoHolder folder;

			public MessageInfoHolder(Message m, FolderInfoHolder folder)
			{
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
					} else
					{
						this.date = getDateFormat().format(date);
                    }
                    this.hasAttachments = message.getAttachmentCount() > 0;
                    this.read = message.isSet(Flag.SEEN);
                    this.answered = message.isSet(Flag.ANSWERED);
                    this.flagged = message.isSet(Flag.FLAGGED);
					if (folder.outbox)
					{
						this.sender = Address.toFriendly(message
								.getRecipients(RecipientType.TO));
					} else
					{
                        this.sender = Address.toFriendly(message.getFrom());
                    }
                    this.subject = message.getSubject();
                    this.uid = message.getUid();
                    this.message = m;
				} catch (MessagingException me)
				{
					if (Config.LOGV)
					{
                        Log.v(Email.LOG_TAG, "Unable to load message info", me);
                    }
                }
            }

			public int compareTo(MessageInfoHolder o)
			{
			  if (thread)
			  {
  			  if (compareSubject == null)
  			  {
  			    compareSubject = stripPrefixes(subject).toLowerCase();
  			  }
  			  if (o.compareSubject == null)
  			  {
  			    o.compareSubject = stripPrefixes(o.subject).toLowerCase();
  			  }
  			  int subjCompare = this.compareSubject.compareTo(o.compareSubject);
  			  if (subjCompare != 0)
  			  {
  			    return subjCompare;
  			  }
			  }
			  
        return this.compareDate.compareTo(o.compareDate) * -1;
            }
			Pattern pattern = null;
			String patternString = "^ *(re|fw|fwd): *";
			private String stripPrefixes(String in)
			{
			  synchronized(patternString)
			  {
  			  if (pattern == null)
          {
            pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE );
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

		class FolderViewHolder
		{
            public TextView folderName;

            public TextView folderStatus;

            public TextView newMessageCount;
        }

		class MessageViewHolder
		{
            public TextView subject;

            public TextView preview;

            public TextView from;

            public TextView date;

            public View chip;
        }

		class FooterViewHolder
		{
            public ProgressBar progress;

            public TextView main;
        }
    }
}
