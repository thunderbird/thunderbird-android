package com.fsck.k9.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.merge.MergeAdapter;
import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.K9.SplitViewMode;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.misc.ExtendedAsyncTask;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.activity.setup.WelcomeMessage;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.fragment.MessageViewFragment.MessageViewFragmentListener;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.helper.power.TracingPowerManager;
import com.fsck.k9.helper.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.mail.store.WebDavStore;
import com.fsck.k9.preferences.SettingsExporter;
import com.fsck.k9.preferences.SettingsImportExportException;
import com.fsck.k9.preferences.SettingsImporter;
import com.fsck.k9.preferences.SettingsImporter.AccountDescription;
import com.fsck.k9.preferences.SettingsImporter.AccountDescriptionPair;
import com.fsck.k9.preferences.SettingsImporter.ImportContents;
import com.fsck.k9.preferences.SettingsImporter.ImportResults;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import com.fsck.k9.search.SearchSpecification;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.Searchfield;
import com.fsck.k9.service.MailService;
import com.fsck.k9.view.ColorChip;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.MessageOpenPgpView;
import com.fsck.k9.view.MessageTitleView;
import com.fsck.k9.view.ViewSwitcher;
import com.fsck.k9.view.ViewSwitcher.OnSwitchCompleteListener;

import de.cketti.library.changelog.ChangeLog;

/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages. From this Activity the user can perform all
 * standard message operations.
 */
public class MessageList extends K9ListActivity implements OnItemClickListener,
		MessageListFragmentListener, MessageViewFragmentListener,
		OnBackStackChangedListener, OnSwipeGestureListener,
		OnSwitchCompleteListener {

	private static final BaseAccount[] EMPTY_BASE_ACCOUNT_ARRAY = new BaseAccount[0];

	/**
	 * URL used to open Android Market application
	 */
	private static final String ANDROID_MARKET_URL = "https://play.google.com/store/apps/details?id=org.openintents.filemanager";

	/**
	 * Number of special accounts ('Unified Inbox' and 'All Messages')
	 */
	private static final int SPECIAL_ACCOUNTS_COUNT = 2;

	private ConcurrentHashMap<BaseAccount, String> pendingWork = new ConcurrentHashMap<BaseAccount, String>();

	// for this activity
	private static final String EXTRA_SEARCH = "search";
	private static final String EXTRA_NO_THREADING = "no_threading";

	private static final String ACTION_SHORTCUT = "shortcut";
	private static final String EXTRA_SPECIAL_FOLDER = "special_folder";

	private static final String EXTRA_MESSAGE_REFERENCE = "message_reference";

	// used for remote search
	public static final String EXTRA_SEARCH_ACCOUNT = "com.fsck.k9.search_account";
	private static final String EXTRA_SEARCH_FOLDER = "com.fsck.k9.search_folder";

	private static final String STATE_DISPLAY_MODE = "displayMode";
	private static final String STATE_MESSAGE_LIST_WAS_DISPLAYED = "messageListWasDisplayed";

	// Used for navigating to next/previous message
	private static final int PREVIOUS = 1;
	private static final int NEXT = 2;

	private ArrayList<Object> mList = new ArrayList<Object>();

	private static final String EXTRA_ACCOUNT = "account";

	private static final String EXTRA_FROM_SHORTCUT = "fromShortcut";

	private static final boolean REFRESH_REMOTE = true;

	private static ListView mListView;

	private FolderListAdapter mAdapter;

	private LayoutInflater mInflater;

	private ProgressDialog progress;

	private static final String TAG = "K9MailExtension";

	public static final String PREF_NAME = "pref_name";

	static final Uri k9AccountsUri = Uri
			.parse("content://com.fsck.k9.messageprovider/accounts/");
	static final String k9UnreadUri = "content://com.fsck.k9.messageprovider/account_unread/";
	static final String k9MessageProvider = "content://com.fsck.k9.messageprovider/";

	ContentObserver contentObserver = null;
	BroadcastReceiver receiver = null;
	IntentFilter filter = null;

	private Object mSelectedAccountItem1;

	private View header_folders;

	private View header_inbox;

	private View header_drawer;

	private AccountsAdapter1 mAdapter_Accounts;

	private ConcurrentHashMap<String, AccountStats> accountStats = new ConcurrentHashMap<String, AccountStats>();

	private Accounts mAccounts;

	private FontSizes mFontSizes = K9.getFontSizes();
	private Context context;

	private MenuItem mRefreshMenuItem;
	private View mActionBarProgressView;

	private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();

	private android.app.ActionBar mActionBar;
	private View mActionBarMessageList;
	private View mActionBarMessageView;
	private MessageTitleView mActionBarSubject;
	private TextView mActionBarTitle;
	private TextView mActionBarSubTitle;
	private TextView mActionBarUnread;
	private android.view.Menu mMenu;

	DrawerLayout mDrawerLayout;
	LinearLayout mDrawerLinear;

	TextView mWelcomePerson;
	ListView mDrawerList;
	ListView mDrawerList_Inbox;

	ActionBarDrawerToggle mDrawerToggle;

	String actionbar_colors, background_colorsString;
	private String Show_View;
	String[] title;
	String[] count;
	int[] icon;
	String[] title_Inbox;
	String[] count_Inbox;
	int[] icon_Inbox;
	private String counterss;
	private int counters;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private int shared1;

	private BaseAccount mBaseAccount;

	private int countersss1;

	private int mUnreadMessageCount = 0;

	private String due_tommorow_shared, due_tommorow_shared_content;

	private List<String> read_due_today_list;

	private String K9count;

	private View swipe;

	private int shared;

	private int countersss;

	private ViewGroup mMessageViewContainer;
	private View mMessageViewPlaceHolder;

	private MessageListFragment mMessageListFragment;
	private MessageViewFragment mMessageViewFragment;
	private int mFirstBackStackId = -1;

	private ScrollView mDrawer_Scroll;

	private Account mAccount;
	private String mFolderName;
	private LocalSearch mSearch;
	private boolean mSingleFolderMode;
	private boolean mSingleAccountMode;

	private String[] Accountsone;
	private String[] BeaconPortaltwo;
	private String[] Foldersthree;

	int[] mAccountsHeights;
	int[] mBeaconPortalHeights;
	int[] mFoldersHeights;

	private K9FragmentActivity mK9FragmentActivity;

	private K9ActivityCommon mBase;

	private BaseAccount mSelectedContextAccount;

	private ProgressBar mActionBarProgress;
	private android.view.MenuItem mMenuButtonCheckMail;
	private View mActionButtonIndeterminateProgress;
	private int mLastDirection = (K9.messageViewShowNext()) ? NEXT : PREVIOUS;

	/**
	 * {@code true} if the message list should be displayed as flat list (i.e.
	 * no threading) regardless whether or not message threading was enabled in
	 * the settings. This is used for filtered views, e.g. when only displaying
	 * the unread messages in a folder.
	 */
	private boolean mNoThreading;

	private String K9counts;

	private MessageList currentActivity;

	private DisplayMode mDisplayMode;
	private MessageReference mMessageReference;

	/**
	 * {@code true} when the message list was displayed once. This is used in
	 * {@link #onBackPressed()} to decide whether to go from the message view to
	 * the message list or finish the activity.
	 */
	private boolean mMessageListWasDisplayed = false;
	private ViewSwitcher mViewSwitcher;

	private FolderListHandler mHandler = new FolderListHandler();

	class FolderListHandler extends Handler {

		public void refreshTitle() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					if (mUnreadMessageCount == 0) {

					} else {

					}

					String operation = mAdapter.mListener
							.getOperation(MessageList.this);
					if (operation.length() < 1) {

					} else {

					}
				}
			});
		}

		public void newFolders(final List<FolderInfoHolder> newFolders) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.mFolders.clear();
					mAdapter.mFolders.addAll(newFolders);
					mAdapter.mFilteredFolders = mAdapter.mFolders;
					mHandler.dataChanged();
				}
			});
		}

		public void workingAccount(final int res) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String toastText = getString(res, mAccount.getDescription());
					Toast toast = Toast.makeText(getApplication(), toastText,
							Toast.LENGTH_SHORT);
					toast.show();
				}
			});
		}

		public void accountSizeChanged(final long oldSize, final long newSize) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String toastText = getString(
							R.string.account_size_changed,
							mAccount.getDescription(),
							SizeFormatter.formatSize(getApplication(), oldSize),
							SizeFormatter.formatSize(getApplication(), newSize));

					Toast toast = Toast.makeText(getApplication(), toastText,
							Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}

		public void folderLoading(final String folder, final boolean loading) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					FolderInfoHolder folderHolder = mAdapter.getFolder(folder);

					if (folderHolder != null) {
						folderHolder.loading = loading;
					}

				}
			});
		}

		public void progress(final boolean progress) {
			// Make sure we don't try this before the menu is initialized
			// this could happen while the activity is initialized.
			if (mRefreshMenuItem == null) {
				return;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (progress) {
						mRefreshMenuItem.setActionView(mActionBarProgressView);
					} else {
						mRefreshMenuItem.setActionView(null);
					}
				}
			});

		}

		public void dataChanged() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mAdapter.notifyDataSetChanged();
					mergeadapter.notifyDataSetChanged();
				}
			});
		}
	}

	/**
	 * This class is responsible for reloading the list of local messages for a
	 * given folder, notifying the adapter that the message have been loaded and
	 * queueing up a remote update of the folder.
	 */

	private void checkMail(FolderInfoHolder folder) {
		TracingPowerManager pm = TracingPowerManager.getPowerManager(this);
		final TracingWakeLock wakeLock = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "FolderList checkMail");
		wakeLock.setReferenceCounted(false);
		wakeLock.acquire(K9.WAKE_LOCK_TIMEOUT);
		MessagingListener listener = new MessagingListener() {
			@Override
			public void synchronizeMailboxFinished(Account account,
					String folder, int totalMessagesInMailbox,
					int numNewMessages) {
				if (!account.equals(mAccount)) {
					return;
				}
				wakeLock.release();
			}

			@Override
			public void synchronizeMailboxFailed(Account account,
					String folder, String message) {
				if (!account.equals(mAccount)) {
					return;
				}
				wakeLock.release();
			}
		};
		MessagingController.getInstance(getApplication()).synchronizeMailbox(
				mAccount, folder.name, listener, null);
		sendMail(mAccount);
	}

	public static Intent actionHandleAccountIntent(Context context,
			Account account, boolean fromShortcut) {
		Intent intent = new Intent(context, FolderList.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(EXTRA_ACCOUNT, account.getUuid());

		if (fromShortcut) {
			intent.putExtra(EXTRA_FROM_SHORTCUT, true);
		}

		return intent;
	}

	public static void actionHandleAccount(Context context, Account account) {
		Intent intent = actionHandleAccountIntent(context, account, false);
		context.startActivity(intent);
	}

	public static void actionDisplaySearch(Context context,
			SearchSpecification search, boolean noThreading, boolean newTask) {
		actionDisplaySearch(context, search, noThreading, newTask, true);
	}

	public static void actionDisplaySearch(Context context,
			SearchSpecification search, boolean noThreading, boolean newTask,
			boolean clearTop) {
		context.startActivity(intentDisplaySearch(context, search, noThreading,
				newTask, clearTop));
	}

	public static Intent intentDisplaySearch(Context context,
			SearchSpecification search, boolean noThreading, boolean newTask,
			boolean clearTop) {
		Intent intent = new Intent(context, MessageList.class);
		intent.putExtra(EXTRA_SEARCH, search);
		intent.putExtra(EXTRA_NO_THREADING, noThreading);

		if (clearTop) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		if (newTask) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}

		return intent;
	}

	public static Intent shortcutIntent(Context context, String specialFolder) {
		Intent intent = new Intent(context, MessageList.class);
		intent.setAction(ACTION_SHORTCUT);
		intent.putExtra(EXTRA_SPECIAL_FOLDER, specialFolder);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		return intent;
	}

	public static Intent actionDisplayMessageIntent(Context context,
			MessageReference messageReference) {
		Intent intent = new Intent(context, MessageList.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(EXTRA_MESSAGE_REFERENCE, messageReference);
		return intent;
	}

	private enum DisplayMode {
		MESSAGE_LIST, MESSAGE_VIEW, SPLIT_VIEW
	}

	private MergeAdapter mergeadapter = null;

	private View accounts_view;

	private static View folders_view;

	private View portals_view;

	private SearchAccount mAllMessagesAccount = null;
	private SearchAccount mUnifiedInboxAccount = null;

	private static final int DIALOG_REMOVE_ACCOUNT = 1;
	private static final int DIALOG_CLEAR_ACCOUNT = 2;
	private static final int DIALOG_RECREATE_ACCOUNT = 3;
	private static final int DIALOG_NO_FILE_MANAGER = 4;

	/**
	 * Contains information about objects that need to be retained on
	 * configuration changes.
	 * 
	 * @see #onRetainNonConfigurationInstance()
	 */
	private NonConfigurationInstance mNonConfigurationInstance;

	private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;

	public class AccountsHandler extends Handler {
		private void setViewTitle() {
			mActionBarTitle.setText(getString(R.string.accounts_title));

			if (mUnreadMessageCount == 0) {
				mActionBarUnread.setVisibility(View.GONE);
			} else {
				mActionBarUnread.setText(Integer.toString(mUnreadMessageCount));
				mActionBarUnread.setVisibility(View.VISIBLE);
			}

			String operation = mListener_Accounts
					.getOperation(MessageList.this);
			operation.trim();
			if (operation.length() < 1) {
				mActionBarSubTitle.setVisibility(View.GONE);
			} else {
				mActionBarSubTitle.setVisibility(View.VISIBLE);
				mActionBarSubTitle.setText(operation);
			}
		}

		public void refreshTitle() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setViewTitle();
				}
			});
		}

		public LocalSearch createUnreadSearch(Context context,
				BaseAccount account) {
			String searchTitle = context.getString(R.string.search_title,
					account.getDescription(),
					context.getString(R.string.unread_modifier));

			LocalSearch search;
			if (account instanceof SearchAccount) {
				search = ((SearchAccount) account).getRelatedSearch().clone();
				search.setName(searchTitle);
			} else {
				search = new LocalSearch(searchTitle);
				search.addAccountUuid(account.getUuid());

				Account realAccount = (Account) account;
				realAccount.excludeSpecialFolders(search);
				realAccount.limitToDisplayableFolders(search);
			}

			search.and(Searchfield.READ, "1", Attribute.NOT_EQUALS);

			return search;
		}

		public void dataChanged() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mAdapter_Accounts != null) {
						mAdapter_Accounts.notifyDataSetChanged();
						mergeadapter.notifyDataSetChanged();
					}
				}
			});
		}

		public void workingAccount(final Account account, final int res) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String toastText = getString(res, account.getDescription());

					Toast toast = Toast.makeText(getApplication(), toastText,
							Toast.LENGTH_SHORT);
					toast.show();
				}
			});
		}

		public void accountSizeChanged(final Account account,
				final long oldSize, final long newSize) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AccountStats stats = accountStats.get(account.getUuid());
					if (newSize != -1 && stats != null && K9.measureAccounts()) {
						stats.size = newSize;
					}
					String toastText = getString(
							R.string.account_size_changed,
							account.getDescription(),
							SizeFormatter.formatSize(getApplication(), oldSize),
							SizeFormatter.formatSize(getApplication(), newSize));

					Toast toast = Toast.makeText(getApplication(), toastText,
							Toast.LENGTH_LONG);
					toast.show();
					if (mAdapter_Accounts != null) {
						mAdapter_Accounts.notifyDataSetChanged();
						mergeadapter.notifyDataSetChanged();
					}
				}
			});
		}

		public void progress_boolean(final boolean progress_boolean) {
			// Make sure we don't try this before the menu is initialized
			// this could happen while the activity is initialized.
			if (mRefreshMenuItem == null) {
				return;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (progress_boolean) {
						mRefreshMenuItem
								.setActionView(R.layout.actionbar_indeterminate_progress_actionview);
					} else {
						mRefreshMenuItem.setActionView(null);
					}
				}
			});

		}

		public void progress_int(final int progress_int) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
							progress_int);
				}
			});
		}
	}

	public void setProgress_Accounts(boolean progress) {
		mHandler.progress(progress);
	}

	ActivityListener mListener_Accounts = new ActivityListener() {
		@Override
		public void informUserOfStatus() {
			mHandler.refreshTitle();
		}

		@Override
		public void folderStatusChanged(Account account, String folderName,
				int unreadMessageCount) {
			try {
				AccountStats stats = account.getStats(MessageList.this);
				if (stats == null) {
					Log.w(K9.LOG_TAG, "Unable to get account stats");
				} else {
					accountStatusChanged(account, stats);
				}
			} catch (Exception e) {
				Log.e(K9.LOG_TAG, "Unable to get account stats", e);
			}
		}

		@Override
		public void accountStatusChanged(BaseAccount account, AccountStats stats) {
			AccountStats oldStats = accountStats.get(account.getUuid());
			int oldUnreadMessageCount = 0;
			if (oldStats != null) {
				oldUnreadMessageCount = oldStats.unreadMessageCount;
			}
			if (stats == null) {
				stats = new AccountStats(); // empty stats for unavailable
											// accounts
				stats.available = false;
			}
			accountStats.put(account.getUuid(), stats);
			if (account instanceof Account) {
				mUnreadMessageCount += stats.unreadMessageCount
						- oldUnreadMessageCount;
			}
			mHandler.dataChanged();
			pendingWork.remove(account);

		}

		@Override
		public void synchronizeMailboxFinished(Account account, String folder,
				int totalMessagesInMailbox, int numNewMessages) {
			MessagingController.getInstance(getApplication()).getAccountStats(
					MessageList.this, account, mListener_Accounts);
			super.synchronizeMailboxFinished(account, folder,
					totalMessagesInMailbox, numNewMessages);

			mHandler.progress(false);

		}

		@Override
		public void synchronizeMailboxStarted(Account account, String folder) {
			super.synchronizeMailboxStarted(account, folder);
			mHandler.progress(true);
		}

		@Override
		public void synchronizeMailboxFailed(Account account, String folder,
				String message) {
			super.synchronizeMailboxFailed(account, folder, message);
			mHandler.progress(false);

		}

	};

	private static String ACCOUNT_STATS = "accountStats";
	private static String STATE_UNREAD_COUNT = "unreadCount";
	private static String SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount";

	public static final String EXTRA_STARTUP = "startup";

	public static final String ACTION_IMPORT_SETTINGS = "importSettings";

	public static void listAccounts(Context context) {
		Intent intent = new Intent(context, Accounts.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra(EXTRA_STARTUP, false);
		context.startActivity(intent);
	}

	public static void importSettings(Context context) {
		Intent intent = new Intent(context, Accounts.class);
		intent.setAction(ACTION_IMPORT_SETTINGS);
		context.startActivity(intent);
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!K9.isHideSpecialAccounts()) {
			createSpecialAccounts();
		}

		Account[] accounts = Preferences.getPreferences(this).getAccounts();
		Intent intent = getIntent();
		// onNewIntent(intent);

		// see if we should show the welcome message
		if (ACTION_IMPORT_SETTINGS.equals(intent.getAction())) {
			mAccounts.onImport();
		} else if (accounts.length < 1) {
			WelcomeMessage.showWelcomeMessage(this);
			finish();
			return;
		}

		if (UpgradeDatabases.actionUpgradeDatabases(this, intent)) {
			finish();
			return;
		}

		requestWindowFeature(Window.FEATURE_PROGRESS);

		Log.d(TAG, "onCreate()");

		String packageName = "com.fsck.k9";

		int versionNumber = 0;

		try {
			PackageInfo pi = getApplicationContext().getPackageManager()
					.getPackageInfo(packageName, PackageManager.GET_META_DATA);
			versionNumber = pi.versionCode;
			String versionName = pi.versionName;

			Log.d(TAG, "K-9 is installed - " + versionNumber + " "
					+ versionName);

		} catch (NameNotFoundException e) {
			Log.d(TAG, "K-9 not found");
		}

		if (versionNumber <= 1) {
			// Register a listener for broadcasts (needed for the older versions
			// of k9)
			Log.d(TAG, "Initialising BroadcastReceiver for old K-9 version");
			receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.d(TAG, "receiver.onReceive()");
					doRefresh();
				}
			};

			filter = new IntentFilter();
			filter.addAction("com.fsck.k9.intent.action.EMAIL_RECEIVED");
			filter.addAction("com.fsck.k9.intent.action.EMAIL_DELETED");
			filter.addDataScheme("email");
			registerReceiver(receiver, filter);
		} else {
			// Register our own content observer, rather than using
			// addWatchContentUris()
			// since DashClock might not have permission to access the database
			Log.d(TAG, "Initialising ContentObserver for new K-9 version");
			contentObserver = new ContentObserver(null) {
				@Override
				public void onChange(boolean selfChange) {
					Log.d(TAG, "contentResolver.onChange()");
					doRefresh();
				}
			};
			getContentResolver().registerContentObserver(
					Uri.parse(k9UnreadUri), true, contentObserver);
		}

		doRefresh();

		if (UpgradeDatabases.actionUpgradeDatabases(this, getIntent())) {
			finish();
			return;
		}

		if (useSplitView()) {
			setContentView(R.layout.split_drawer_main);
		} else {
			setContentView(R.layout.drawer);
			mViewSwitcher = (ViewSwitcher) findViewById(R.id.container);
			mViewSwitcher.setFirstInAnimation(AnimationUtils.loadAnimation(
					this, R.anim.slide_in_left));
			mViewSwitcher.setFirstOutAnimation(AnimationUtils.loadAnimation(
					this, R.anim.slide_out_right));
			mViewSwitcher.setSecondInAnimation(AnimationUtils.loadAnimation(
					this, R.anim.slide_in_right));
			mViewSwitcher.setSecondOutAnimation(AnimationUtils.loadAnimation(
					this, R.anim.slide_out_left));
			mViewSwitcher.setOnSwitchCompleteListener(this);
		}

		mergeadapter = new MergeAdapter();

		LayoutInflater inflater = getLayoutInflater();

		accounts_view = inflater.inflate(R.layout.accounts_list, null);

		folders_view = inflater.inflate(R.layout.folders_list, null);

		header_folders = inflater.inflate(R.layout.header_folders, null);

		initializeActionBar();

		mListView = (ListView) findViewById(android.R.id.list);
		// mListView.addHeaderView(header_folders, null, false);

		mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		mListView.setLongClickable(true);
		mListView.setFastScrollEnabled(true);
		mListView.setScrollingCacheEnabled(false);

		setResult(RESULT_CANCELED);

		mListView.setSaveEnabled(true);

		mInflater = getLayoutInflater();

		onNewIntent(getIntent());

		context = this;

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		mDrawerLinear = (LinearLayout) findViewById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		getListView().setOnItemClickListener(new DrawerItemClickListener());

		if (!decodeExtras(getIntent())) {
			return;
		}

		findFragments();
		initializeDisplayMode(savedInstanceState);
		initializeLayout();
		initializeFragments();
		displayViews();
		// registerForContextMenu(mDrawerList_Inbox);
		registerForContextMenu(mListView);

		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {

			@Override
			public void onDrawerClosed(View view) {
				// TODO Auto-generated method stub
				super.onDrawerClosed(view);
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				// TODO Auto-generated method stub

				getActionBar().setTitle(mDrawerTitle);
				super.onDrawerOpened(drawerView);
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

	}

	private StorageManager.StorageListener storageListener = new StorageManager.StorageListener() {

		@Override
		public void onUnmount(String providerId) {
			refresh();
		}

		@Override
		public void onMount(String providerId) {
			refresh();
		}
	};

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onResume() {
		super.onResume();

		refresh();
		MessagingController.getInstance(getApplication()).addListener(
				mListener_Accounts);
		StorageManager.getInstance(getApplication()).addListener(
				storageListener);
		mListener_Accounts.onResume(this);

		new LoadAccounts().execute();

		// figure out why below if statement throws null later

		// if (!mAccount.isAvailable(this)) {
		// Log.i(K9.LOG_TAG,
		// "account unavaliabale, not showing folder-list but account-list");
		// Accounts.listAccounts(this);
		// finish();
		// return;
		// }
		if (mAdapter == null)
			initializeActivityView();

		MessagingController.getInstance(getApplication()).addListener(
				mAdapter.mListener);
		// mAccount.refresh(Preferences.getPreferences(this));
		MessagingController.getInstance(getApplication()).getAccountStats(this,
				mAccount, mAdapter.mListener);

		onRefresh(!REFRESH_REMOTE);

		MessagingController.getInstance(getApplication()).notifyAccountCancel(
				this, mAccount);
		mAdapter.mListener.onResume(this);

		if (!(this instanceof Search)) {
			// necessary b/c no guarantee Search.onStop will be called before
			// MessageList.onResume
			// when returning from search results
			Search.setActive(false);
		}

		if (mAccount != null && !mAccount.isAvailable(this)) {
			onAccountUnavailable();
			return;
		}
		StorageManager.getInstance(getApplication()).addListener(
				mStorageListener);

		// mergeadapter = new MergeAdapter();

		LayoutInflater inflater = getLayoutInflater();

		header_folders = inflater.inflate(R.layout.header_folders, null);

		header_inbox = inflater.inflate(R.layout.header_inbox, null);

		mListView = getListView();

		mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		mListView.setLongClickable(true);
		mListView.setFastScrollEnabled(true);
		mListView.setScrollingCacheEnabled(false);

		setResult(RESULT_CANCELED);

		getListView().setSaveEnabled(true);

		mInflater = getLayoutInflater();

		context = this;

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		mDrawerLinear = (LinearLayout) findViewById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent); // onNewIntent doesn't autoset our "internal" intent

		removeFragments();

		mUnreadMessageCount = 0;
		String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT);
		mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

		// if (mAccount == null) {
		// // This shouldn't normally happen. But apparently it does. See issue
		// 2261.
		// finish();
		// return;
		// }

		if (intent.getBooleanExtra(EXTRA_FROM_SHORTCUT, false)
				&& !K9.FOLDER_NONE.equals(mAccount.getAutoExpandFolderName())) {
			onOpenFolder(mAccount.getAutoExpandFolderName());
			finish();
		} else {
			initializeActivityView();
		}

		if (mFirstBackStackId >= 0) {
			getFragmentManager().popBackStackImmediate(mFirstBackStackId,
					FragmentManager.POP_BACK_STACK_INCLUSIVE);
			mFirstBackStackId = -1;
		}
		removeMessageListFragment();
		removeMessageViewFragment();

		mMessageReference = null;
		mSearch = null;
		mFolderName = null;

		if (!decodeExtras(intent)) {
			return;
		}

		initializeDisplayMode(null);
		initializeFragments();
		displayViews();
		refresh();
	}

	private void initializeActivityView() {
		mAdapter = new FolderListAdapter();
		restorePreviousData();

		setListAdapter(mAdapter);

		getListView().setTextFilterEnabled(mAdapter.getFilter() != null);

	}

	@SuppressWarnings("unchecked")
	private void restorePreviousData() {
		final Object previousData = getLastNonConfigurationInstance();

		if (previousData != null) {
			mAdapter.mFolders = (ArrayList<FolderInfoHolder>) previousData;
			mAdapter.mFilteredFolders = Collections
					.unmodifiableList(mAdapter.mFolders);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		return (mAdapter == null) ? null : mAdapter.mFolders;

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Shortcuts that work no matter what is selected
		switch (keyCode) {
		case KeyEvent.KEYCODE_Q: {
			onAccounts();
			return true;
		}

		case KeyEvent.KEYCODE_S: {
			onEditAccount();
			return true;
		}

		case KeyEvent.KEYCODE_H: {
			Toast toast = Toast.makeText(this, R.string.folder_list_help_key,
					Toast.LENGTH_LONG);
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
		}// switch

		return super.onKeyDown(keyCode, event);
	}// onKeyDown

	private void setDisplayMode(FolderMode newMode) {
		mAccount.setFolderDisplayMode(newMode);
		mAccount.save(Preferences.getPreferences(this));
		if (mAccount.getFolderPushMode() != FolderMode.NONE) {
			MailService.actionRestartPushers(this, null);
		}
		mAdapter.getFilter().filter(null);
		onRefresh(false);
	}

	private void onRefresh(final boolean forceRemote) {

		MessagingController.getInstance(getApplication()).listFolders(mAccount,
				forceRemote, mAdapter.mListener);

	}

	private void onEditPrefs() {
		Prefs.actionPrefs(this);
	}

	private void onEditAccount() {
		AccountSettings.actionSettings(this, mAccount);
	}

	private void onAccounts() {
		Accounts.listAccounts(this);
		finish();
	}

	private void onEmptyTrash(final Account account) {
		mHandler.dataChanged();

		MessagingController.getInstance(getApplication()).emptyTrash(account,
				null);
	}

	private void onClearFolder(Account account, String folderName) {
		// There has to be a cheaper way to get at the localFolder object than
		// this
		LocalFolder localFolder = null;
		try {
			if (account == null || folderName == null
					|| !account.isAvailable(MessageList.this)) {
				Log.i(K9.LOG_TAG, "not clear folder of unavailable account");
				return;
			}
			localFolder = account.getLocalStore().getFolder(folderName);
			localFolder.open(Folder.OPEN_MODE_RW);
			localFolder.clearAllMessages();
		} catch (Exception e) {
			Log.e(K9.LOG_TAG, "Exception while clearing folder", e);
		} finally {
			if (localFolder != null) {
				localFolder.close();
			}
		}

		onRefresh(!REFRESH_REMOTE);
	}

	private void sendMail(Account account) {
		MessagingController.getInstance(getApplication()).sendPendingMessages(
				account, mAdapter.mListener);
	}

	@Override
	public boolean onSearchRequested() {
		Bundle appData = new Bundle();
		appData.putString(MessageList.EXTRA_SEARCH_ACCOUNT, mAccount.getUuid());
		startSearch(null, false, appData, false);
		return true;
	}

	private void onOpenFolder(String folder) {
		LocalSearch search = new LocalSearch(folder);
		search.addAccountUuid(mAccount.getUuid());
		search.addAllowedFolder(folder);
		MessageList.actionDisplaySearch(this, search, false, false);
	}

	private void onCompact(Account account) {
		mHandler.workingAccount(R.string.compacting_account);
		MessagingController.getInstance(getApplication())
				.compact(account, null);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		if (v == getListView()) {

			if (info.position > mAdapter_Accounts.getCount()) {

				getMenuInflater().inflate(R.menu.folder_context, menu);

				FolderInfoHolder folder = (FolderInfoHolder) mAdapter
						.getItem(info.position
								- (mAdapter_Accounts.getCount() + 2)); // 2
																		// seems
																		// to be
																		// the
				// lucky number here,
				// tested with gmail and
				// exchange, but i have
				// no idea why

				menu.setHeaderTitle(folder.displayName);

			} else if (info.position < mAdapter_Accounts.getCount() + 1) {

				menu.setHeaderTitle(R.string.accounts_context_menu_title);

				if (mAdapter_Accounts == null) {

					Log.d("info =", "mAdapter_Accounts = null");

				}

				BaseAccount account = (BaseAccount) mergeadapter
						.getItem(info.position);

				if ((account instanceof Account)
						&& !((Account) account).isEnabled()) {
					getMenuInflater().inflate(R.menu.disabled_accounts_context,
							menu);
				} else {
					getMenuInflater().inflate(R.menu.accounts_context, menu);
				}

				if (account instanceof SearchAccount) {
					for (int i = 0; i < menu.size(); i++) {
						android.view.MenuItem item = menu.getItem(i);
						item.setVisible(false);
					}
				} else {
					EnumSet<ACCOUNT_LOCATION> accountLocation = accountLocation(account);
					if (accountLocation.contains(ACCOUNT_LOCATION.TOP)) {
						menu.findItem(R.id.move_up).setEnabled(false);
					} else {
						menu.findItem(R.id.move_up).setEnabled(true);
					}
					if (accountLocation.contains(ACCOUNT_LOCATION.BOTTOM)) {
						menu.findItem(R.id.move_down).setEnabled(false);
					} else {
						menu.findItem(R.id.move_down).setEnabled(true);
					}
				}

			}

		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();

		Account realAccount = null;

		FolderInfoHolder folder = null;

		if (mListView.getItemAtPosition(menuInfo.position) != null) {

			if (menuInfo.position - 1 > mAdapter_Accounts.getCount()) {

				if (0 < menuInfo.position - (mAdapter_Accounts.getCount())) {

					folder = (FolderInfoHolder) mAdapter
							.getItem(menuInfo.position
									- (mAdapter_Accounts.getCount() + 2)); // 2
																			// seems
																			// to
																			// be
																			// the
																			// lucky
																			// number,
																			// tested
																			// with
																			// a
																			// number
																			// of
																			// email
																			// configurations

				} else {
					folder = (FolderInfoHolder) mAdapter
							.getItem(menuInfo.position
									- (mAdapter_Accounts.getCount() + 2));

				}

			} else {

				folder = (FolderInfoHolder) mAdapter.getItem(menuInfo.position);

				if (menuInfo != null) {

					mSelectedContextAccount = (BaseAccount) getListView()
							.getItemAtPosition(menuInfo.position);
				}

				if (mSelectedContextAccount instanceof Account) {
					realAccount = (Account) mSelectedContextAccount;

				}

			}

		}

		switch (item.getItemId()) {
		case R.id.clear_local_folder:
			onClearFolder(mAccount, folder.name);
			break;
		case R.id.refresh_folder:
			checkMail(folder);
			break;
		case R.id.folder_settings:
			FolderSettings.actionSettings(this, mAccount, folder.name);
			break;
		case R.id.delete_account:
			onDeleteAccount(realAccount);

			break;
		case R.id.account_settings:
			onEditAccount();

			break;
		case R.id.activate:
			onActivateAccount(realAccount);

			break;
		case R.id.clear_pending:
			onClearCommands(realAccount);

			break;
		case R.id.empty_trash:
			onEmptyTrash(realAccount);

			break;
		case R.id.clear:
			onClear(realAccount);

			break;
		case R.id.recreate:
			onRecreate(realAccount);

			break;
		case R.id.export:
			onExport(false, realAccount);

			break;
		case R.id.move_up:
			onMove(realAccount, true);

			break;
		case R.id.move_down:
			onMove(realAccount, false);

			break;
		}

		return super.onContextItemSelected(item);

	}

	public void onDeleteAccount(Account account) {
		BaseAccount mSelectedContextAccount1 = account;

		showDialog(DIALOG_REMOVE_ACCOUNT);
	}

	void onEditAccount(Account account) {
		AccountSettings.actionSettings(this, mAccount);
	}

	@Override
	public Dialog onCreateDialog(int id) {
		// Android recreates our dialogs on configuration changes even when they
		// have been
		// dismissed. Make sure we have all information necessary before
		// creating a new dialog.
		switch (id) {
		case DIALOG_REMOVE_ACCOUNT: {
			if (mSelectedContextAccount == null) {

				System.out.println(mSelectedContextAccount);

				Log.d("this is returning null", "yep :(");

				return null;
			}

			return ConfirmationDialog.create(
					this,
					id,
					R.string.account_delete_dlg_title,
					getString(R.string.account_delete_dlg_instructions_fmt,
							mSelectedContextAccount.getDescription()),
					R.string.okay_action, R.string.cancel_action,
					new Runnable() {
						@Override
						public void run() {
							if (mSelectedContextAccount instanceof Account) {
								Account realAccount = (Account) mSelectedContextAccount;
								try {
									realAccount.getLocalStore().delete();
								} catch (Exception e) {
									// Ignore, this may lead to localStores on
									// sd-cards that
									// are currently not inserted to be left
								}
								MessagingController.getInstance(
										getApplication()).notifyAccountCancel(
										MessageList.this, realAccount);
								Preferences.getPreferences(MessageList.this)
										.deleteAccount(realAccount);
								K9.setServicesEnabled(MessageList.this);

							}
						}
					});
		}
		case DIALOG_CLEAR_ACCOUNT: {
			if (mSelectedContextAccount == null) {

				Log.d("this is returning null", "yep :(");
				return null;
			}

			return ConfirmationDialog.create(
					this,
					id,
					R.string.account_clear_dlg_title,
					getString(R.string.account_clear_dlg_instructions_fmt,
							mSelectedContextAccount.getDescription()),
					R.string.okay_action, R.string.cancel_action,
					new Runnable() {
						@Override
						public void run() {
							if (mSelectedContextAccount instanceof Account) {
								Account realAccount = (Account) mSelectedContextAccount;
								mHandler.workingAccount(R.string.clearing_account);
								MessagingController.getInstance(
										getApplication()).clear(realAccount,
										null);
							}
						}
					});
		}
		case DIALOG_RECREATE_ACCOUNT: {
			if (mSelectedContextAccount == null) {

				Log.d("this is returning null", "yep :(");

				return null;
			}

			return ConfirmationDialog.create(
					this,
					id,
					R.string.account_recreate_dlg_title,
					getString(R.string.account_recreate_dlg_instructions_fmt,
							mSelectedContextAccount.getDescription()),
					R.string.okay_action, R.string.cancel_action,
					new Runnable() {
						@Override
						public void run() {
							if (mSelectedContextAccount instanceof Account) {
								Account realAccount = (Account) mSelectedContextAccount;
								mHandler.workingAccount(R.string.recreating_account);
								MessagingController.getInstance(
										getApplication()).recreate(realAccount,
										null);
							}
						}
					});
		}
		case DIALOG_NO_FILE_MANAGER: {
			return ConfirmationDialog.create(this, id,
					R.string.import_dialog_error_title,
					getString(R.string.import_dialog_error_message),
					R.string.open_market, R.string.close, new Runnable() {
						@Override
						public void run() {
							Uri uri = Uri.parse(ANDROID_MARKET_URL);
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							startActivity(intent);
						}
					});
		}
		}

		return super.onCreateDialog(id);
	}

	@Override
	public void onPrepareDialog(int id, Dialog d) {
		AlertDialog alert = (AlertDialog) d;
		switch (id) {
		case DIALOG_REMOVE_ACCOUNT: {
			alert.setMessage(getString(
					R.string.account_delete_dlg_instructions_fmt,
					mSelectedContextAccount.getDescription()));
			break;
		}
		case DIALOG_CLEAR_ACCOUNT: {
			alert.setMessage(getString(
					R.string.account_clear_dlg_instructions_fmt,
					mSelectedContextAccount.getDescription()));
			break;
		}
		case DIALOG_RECREATE_ACCOUNT: {
			alert.setMessage(getString(
					R.string.account_recreate_dlg_instructions_fmt,
					mSelectedContextAccount.getDescription()));
			break;
		}
		}

		super.onPrepareDialog(id, d);
	}

	void onClear(Account account) {
		showDialog(DIALOG_CLEAR_ACCOUNT);

	}

	void onRecreate(Account account) {
		showDialog(DIALOG_RECREATE_ACCOUNT);
	}

	class FolderListAdapter extends BaseAdapter implements Filterable {
		private ArrayList<FolderInfoHolder> mFolders = new ArrayList<FolderInfoHolder>();
		private List<FolderInfoHolder> mFilteredFolders = Collections
				.unmodifiableList(mFolders);
		private Filter mFilter = new FolderListFilter();

		public Object getItem(long position) {
			return getItem((int) position);
		}

		@Override
		public Object getItem(int position) {
			return mFilteredFolders.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mFilteredFolders.get(position).folder.getName().hashCode();
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

		private ActivityListener mListener = new ActivityListener() {
			@Override
			public void informUserOfStatus() {

				mHandler.dataChanged();
			}

			@Override
			public void accountStatusChanged(BaseAccount account,
					AccountStats stats) {
				if (!account.equals(mAccount)) {
					return;
				}
				if (stats == null) {
					return;
				}
				mUnreadMessageCount = stats.unreadMessageCount;

				System.out.println("1 Unread Messages= " + mUnreadMessageCount);

			}

			@Override
			public void listFoldersStarted(Account account) {
				if (account.equals(mAccount)) {
					mHandler.progress(true);
				}
				super.listFoldersStarted(account);

			}

			@Override
			public void listFoldersFailed(Account account, String message) {
				if (account.equals(mAccount)) {

					mHandler.progress(false);
				}
				super.listFoldersFailed(account, message);
			}

			@Override
			public void listFoldersFinished(Account account) {
				if (account.equals(mAccount)) {

					mHandler.progress(false);
					MessagingController.getInstance(getApplication())
							.refreshListener(mAdapter.mListener);
					mHandler.dataChanged();
				}
				super.listFoldersFinished(account);

			}

			@Override
			public void listFolders(Account account, Folder[] folders) {
				if (account.equals(mAccount)) {

					List<FolderInfoHolder> newFolders = new LinkedList<FolderInfoHolder>();
					List<FolderInfoHolder> topFolders = new LinkedList<FolderInfoHolder>();

					Account.FolderMode aMode = account.getFolderDisplayMode();
					Preferences prefs = Preferences
							.getPreferences(getApplication()
									.getApplicationContext());
					for (Folder folder : folders) {
						try {
							folder.refresh(prefs);

							Folder.FolderClass fMode = folder.getDisplayClass();

							if ((aMode == Account.FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS)
									|| (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS
											&& fMode != Folder.FolderClass.FIRST_CLASS && fMode != Folder.FolderClass.SECOND_CLASS)
									|| (aMode == Account.FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS)) {
								continue;
							}
						} catch (MessagingException me) {
							Log.e(K9.LOG_TAG,
									"Couldn't get prefs to check for displayability of folder "
											+ folder.getName(), me);
						}

						FolderInfoHolder holder = null;

						int folderIndex = getFolderIndex(folder.getName());
						if (folderIndex >= 0) {
							holder = (FolderInfoHolder) getItem(folderIndex);
						}

						if (holder == null) {
							holder = new FolderInfoHolder(context, folder,
									mAccount, -1);
						} else {
							holder.populate(context, folder, mAccount, -1);

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
					mHandler.newFolders(topFolders);
				}
				super.listFolders(account, folders);
			}

			@Override
			public void synchronizeMailboxStarted(Account account, String folder) {
				super.synchronizeMailboxStarted(account, folder);
				if (account.equals(mAccount)) {

					mHandler.progress(true);
					mHandler.folderLoading(folder, true);
					mHandler.dataChanged();
				}

			}

			@Override
			public void synchronizeMailboxFinished(Account account,
					String folder, int totalMessagesInMailbox,
					int numNewMessages) {
				super.synchronizeMailboxFinished(account, folder,
						totalMessagesInMailbox, numNewMessages);
				if (account.equals(mAccount)) {
					mHandler.progress(false);
					mHandler.folderLoading(folder, false);

					refreshFolder(account, folder);
				}

			}

			private void refreshFolder(Account account, String folderName) {
				// There has to be a cheaper way to get at the localFolder
				// object than this
				Folder localFolder = null;
				try {
					if (account != null && folderName != null) {
						if (!account.isAvailable(MessageList.this)) {
							Log.i(K9.LOG_TAG,
									"not refreshing folder of unavailable account");
							return;
						}
						localFolder = account.getLocalStore().getFolder(
								folderName);
						FolderInfoHolder folderHolder = getFolder(folderName);
						if (folderHolder != null) {
							folderHolder.populate(context, localFolder,
									mAccount, -1);
							folderHolder.flaggedMessageCount = -1;

							mHandler.dataChanged();
						}
					}
				} catch (Exception e) {
					Log.e(K9.LOG_TAG, "Exception while populating folder", e);
				} finally {
					if (localFolder != null) {
						localFolder.close();
					}
				}

			}

			@Override
			public void synchronizeMailboxFailed(Account account,
					String folder, String message) {
				super.synchronizeMailboxFailed(account, folder, message);
				if (!account.equals(mAccount)) {
					return;
				}

				mHandler.progress(false);

				mHandler.folderLoading(folder, false);

				// String mess = truncateStatus(message);

				// mHandler.folderStatus(folder, mess);
				FolderInfoHolder holder = getFolder(folder);

				if (holder != null) {
					holder.lastChecked = 0;
				}

				mHandler.dataChanged();

			}

			@Override
			public void setPushActive(Account account, String folderName,
					boolean enabled) {
				if (!account.equals(mAccount)) {
					return;
				}
				FolderInfoHolder holder = getFolder(folderName);

				if (holder != null) {
					holder.pushActive = enabled;

					mHandler.dataChanged();
				}
			}

			@Override
			public void messageDeleted(Account account, String folder,
					Message message) {
				synchronizeMailboxRemovedMessage(account, folder, message);
			}

			@Override
			public void emptyTrashCompleted(Account account) {
				if (account.equals(mAccount)) {
					refreshFolder(account, mAccount.getTrashFolderName());
				}
			}

			@Override
			public void folderStatusChanged(Account account, String folderName,
					int unreadMessageCount) {
				if (account.equals(mAccount)) {
					refreshFolder(account, folderName);
					informUserOfStatus();
				}
			}

			@Override
			public void sendPendingMessagesCompleted(Account account) {
				super.sendPendingMessagesCompleted(account);
				if (account.equals(mAccount)) {
					refreshFolder(account, mAccount.getOutboxFolderName());
				}
			}

			@Override
			public void sendPendingMessagesStarted(Account account) {
				super.sendPendingMessagesStarted(account);

				if (account.equals(mAccount)) {
					mHandler.dataChanged();
				}
			}

			@Override
			public void sendPendingMessagesFailed(Account account) {
				super.sendPendingMessagesFailed(account);
				if (account.equals(mAccount)) {
					refreshFolder(account, mAccount.getOutboxFolderName());
				}
			}

			@Override
			public void accountSizeChanged(Account account, long oldSize,
					long newSize) {
				if (account.equals(mAccount)) {
					mHandler.accountSizeChanged(oldSize, newSize);
				}
			}
		};

		public int getFolderIndex(String folder) {
			FolderInfoHolder searchHolder = new FolderInfoHolder();
			searchHolder.name = folder;
			return mFilteredFolders.indexOf(searchHolder);
		}

		public FolderInfoHolder getFolder(String folder) {
			FolderInfoHolder holder = null;

			int index = getFolderIndex(folder);
			if (index >= 0) {
				holder = (FolderInfoHolder) getItem(index);
				if (holder != null) {
					return holder;
				}
			}
			return null;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position <= getCount()) {
				return getItemView(position, convertView, parent);
			} else {
				Log.e(K9.LOG_TAG, "getView with illegal positon=" + position
						+ " called! count is only " + getCount());
				return null;
			}
		}

		public View getItemView(int itemPosition, View convertView,
				ViewGroup parent) {
			FolderInfoHolder folder = (FolderInfoHolder) getItem(itemPosition);
			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.folder_list_item, parent,
						false);
			}

			FolderViewHolder holder = (FolderViewHolder) view.getTag();

			if (holder == null) {
				holder = new FolderViewHolder();
				holder.folderName = (TextView) view
						.findViewById(R.id.folder_name);
				holder.newMessageCount = (TextView) view
						.findViewById(R.id.new_message_count);
				holder.flaggedMessageCount = (TextView) view
						.findViewById(R.id.flagged_message_count);
				holder.newMessageCountWrapper = view
						.findViewById(R.id.new_message_count_wrapper);
				holder.flaggedMessageCountWrapper = view
						.findViewById(R.id.flagged_message_count_wrapper);
				holder.newMessageCountIcon = view
						.findViewById(R.id.new_message_count_icon);
				holder.flaggedMessageCountIcon = view
						.findViewById(R.id.flagged_message_count_icon);
				holder.chip = view.findViewById(R.id.chip);
				holder.folderStatus = (TextView) view
						.findViewById(R.id.folder_status);
				holder.activeIcons = (RelativeLayout) view
						.findViewById(R.id.active_icons);

				holder.folderListItemLayout = (LinearLayout) view
						.findViewById(R.id.folder_list_item_layout);
				holder.rawFolderName = folder.name;

				view.setTag(holder);
			}

			if (folder == null) {
				return view;
			}

			final String folderStatus;

			if (folder.loading) {
				folderStatus = getString(R.string.status_loading);
			} else if (folder.status != null) {
				folderStatus = folder.status;
			} else if (folder.lastChecked != 0) {
				long now = System.currentTimeMillis();
				int flags = DateUtils.FORMAT_SHOW_TIME
						| DateUtils.FORMAT_SHOW_DATE
						| DateUtils.FORMAT_SHOW_YEAR;
				CharSequence formattedDate;

				if (Math.abs(now - folder.lastChecked) > DateUtils.WEEK_IN_MILLIS) {
					formattedDate = getString(R.string.preposition_for_date,
							DateUtils.formatDateTime(context,
									folder.lastChecked, flags));
				} else {
					formattedDate = DateUtils.getRelativeTimeSpanString(
							folder.lastChecked, now,
							DateUtils.MINUTE_IN_MILLIS, flags);
				}

				folderStatus = getString(
						folder.pushActive ? R.string.last_refresh_time_format_with_push
								: R.string.last_refresh_time_format,
						formattedDate);
			} else {
				folderStatus = null;
			}

			holder.folderName.setText(folder.displayName);
			Log.d("Folder Unread = ", folder.displayName);

			if (folderStatus != null) {
				holder.folderStatus.setText(folderStatus);
				holder.folderStatus.setVisibility(View.VISIBLE);
			} else {
				holder.folderStatus.setVisibility(View.GONE);
			}

			if (folder.unreadMessageCount == -1) {
				folder.unreadMessageCount = 0;
				try {
					folder.unreadMessageCount = folder.folder
							.getUnreadMessageCount();

				} catch (Exception e) {
					Log.e(K9.LOG_TAG, "Unable to get unreadMessageCount for "
							+ mAccount.getDescription() + ":" + folder.name);
				}
			}
			if (folder.unreadMessageCount > 0) {
				holder.newMessageCount.setText(Integer
						.toString(folder.unreadMessageCount));

				holder.newMessageCountWrapper
						.setOnClickListener(createUnreadSearch(mAccount, folder));
				holder.newMessageCountWrapper.setVisibility(View.VISIBLE);

			} else {
				holder.newMessageCountWrapper.setVisibility(View.GONE);
			}

			if (folder.flaggedMessageCount == -1) {
				folder.flaggedMessageCount = 0;
				try {
					folder.flaggedMessageCount = folder.folder
							.getFlaggedMessageCount();
				} catch (Exception e) {
					Log.e(K9.LOG_TAG, "Unable to get flaggedMessageCount for "
							+ mAccount.getDescription() + ":" + folder.name);
				}

			}

			if (K9.messageListStars() && folder.flaggedMessageCount > 0) {
				holder.flaggedMessageCount.setText(Integer
						.toString(folder.flaggedMessageCount));
				holder.flaggedMessageCountWrapper
						.setOnClickListener(createFlaggedSearch(mAccount,
								folder));
				holder.flaggedMessageCountWrapper.setVisibility(View.VISIBLE);

			} else {
				holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
			}

			holder.activeIcons.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast toast = Toast.makeText(getApplication(),
							getString(R.string.tap_hint), Toast.LENGTH_SHORT);
					toast.show();
				}
			});

			holder.chip.setBackgroundColor(mAccount.getChipColor());

			mFontSizes.setViewTextSize(holder.folderName,
					mFontSizes.getFolderName());

			if (K9.wrapFolderNames()) {
				holder.folderName.setEllipsize(null);
				holder.folderName.setSingleLine(false);
			} else {
				holder.folderName.setEllipsize(TruncateAt.START);
				holder.folderName.setSingleLine(true);
			}
			mFontSizes.setViewTextSize(holder.folderStatus,
					mFontSizes.getFolderStatus());

			return view;
		}

		private OnClickListener createFlaggedSearch(Account account,
				FolderInfoHolder folder) {
			Log.d("clicked_folder6", "clicked");

			String searchTitle = getString(
					R.string.search_title,
					getString(R.string.message_list_title,
							account.getDescription(), folder.displayName),
					getString(R.string.flagged_modifier));

			LocalSearch search = new LocalSearch(searchTitle);
			search.and(Searchfield.FLAGGED, "1", Attribute.EQUALS);

			search.addAllowedFolder(folder.name);
			search.addAccountUuid(account.getUuid());

			return new FolderClickListener(search);
		}

		private OnClickListener createUnreadSearch(Account mAccount,
				FolderInfoHolder folder) {
			Log.d("clicked_folder7", "clicked");
			String searchTitle = getString(
					R.string.search_title,
					getString(R.string.message_list_title,
							((BaseAccount) mAccount).getDescription(),
							folder.displayName),
					getString(R.string.unread_modifier));

			LocalSearch search = new LocalSearch(searchTitle);
			search.and(Searchfield.READ, "1", Attribute.NOT_EQUALS);

			search.addAllowedFolder(folder.name);
			search.addAccountUuid(((BaseAccount) mAccount).getUuid());

			return new FolderClickListener(search);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		public boolean isItemSelectable(int position) {
			return true;
		}

		public void setFilter(final Filter filter) {
			this.mFilter = filter;
		}

		@Override
		public Filter getFilter() {
			return mFilter;
		}

		/**
		 * Filter to search for occurences of the search-expression in any place
		 * of the folder-name instead of doing jsut a prefix-search.
		 * 
		 * @author Marcus@Wolschon.biz
		 */
		public class FolderListFilter extends Filter {
			private CharSequence mSearchTerm;

			public CharSequence getSearchTerm() {
				return mSearchTerm;
			}

			/**
			 * Do the actual search. {@inheritDoc}
			 * 
			 * @see #publishResults(CharSequence, FilterResults)
			 */
			@Override
			protected FilterResults performFiltering(CharSequence searchTerm) {
				mSearchTerm = searchTerm;
				FilterResults results = new FilterResults();

				Locale locale = Locale.getDefault();
				if ((searchTerm == null) || (searchTerm.length() == 0)) {
					ArrayList<FolderInfoHolder> list = new ArrayList<FolderInfoHolder>(
							mFolders);
					results.values = list;
					results.count = list.size();
				} else {
					final String searchTermString = searchTerm.toString()
							.toLowerCase(locale);
					final String[] words = searchTermString.split(" ");
					final int wordCount = words.length;

					final ArrayList<FolderInfoHolder> newValues = new ArrayList<FolderInfoHolder>();

					for (final FolderInfoHolder value : mFolders) {
						if (value.displayName == null) {
							continue;
						}
						final String valueText = value.displayName
								.toLowerCase(locale);

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
			 * Publish the results to the user-interface. {@inheritDoc}
			 */
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				// noinspection unchecked
				mFilteredFolders = Collections
						.unmodifiableList((ArrayList<FolderInfoHolder>) results.values);
				// Send notification that the data set changed now
				notifyDataSetChanged();
			}
		}
	}

	static class FolderViewHolder {
		public TextView folderName;

		public TextView folderStatus;

		public TextView newMessageCount;
		public TextView flaggedMessageCount;
		public View newMessageCountIcon;
		public View flaggedMessageCountIcon;
		public View newMessageCountWrapper;
		public View flaggedMessageCountWrapper;
		public View chip;
		public RelativeLayout activeIcons;
		public String rawFolderName;

		public LinearLayout folderListItemLayout;
	}

	private class FolderClickListener implements OnClickListener {

		final LocalSearch search;

		FolderClickListener(LocalSearch search) {
			this.search = search;
		}

		@Override
		public void onClick(View v) {
			MessageList.actionDisplaySearch(MessageList.this, search, true,
					false);

			Log.d("clicked_folder8", "clicked");
		}
	}

	/**
	 * Get references to existing fragments if the activity was restarted.
	 */
	private void findFragments() {
		FragmentManager fragmentManager = getFragmentManager();
		mMessageListFragment = (MessageListFragment) fragmentManager
				.findFragmentById(R.id.message_list_container);
		mMessageViewFragment = (MessageViewFragment) fragmentManager
				.findFragmentById(R.id.message_view_container);
	}

	/**
	 * Create fragment instances if necessary.
	 * 
	 * @see #findFragments()
	 */

	private void removeFragments() {
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.addOnBackStackChangedListener(this);

		boolean hasMessageListFragment = (mMessageListFragment != null);

		if (!hasMessageListFragment) {
			FragmentTransaction ft = fragmentManager.beginTransaction();
			mMessageListFragment = MessageListFragment.newInstance(mSearch,
					false, (K9.isThreadedViewEnabled() && !mNoThreading));
			ft.remove(mMessageListFragment);
			ft.commit();

			Log.d("removed fragment?", "yes");
		}

		// Check if the fragment wasn't restarted and has a MessageReference in
		// the arguments. If
		// so, open the referenced message.
		if (!hasMessageListFragment && mMessageViewFragment == null
				&& mMessageReference != null) {
			openMessage(mMessageReference);
		}
	}

	private void initializeFragments() {
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.addOnBackStackChangedListener(this);

		boolean hasMessageListFragment = (mMessageListFragment != null);

		if (!hasMessageListFragment) {
			FragmentTransaction ft = fragmentManager.beginTransaction();
			mMessageListFragment = MessageListFragment.newInstance(mSearch,
					false, (K9.isThreadedViewEnabled() && !mNoThreading));
			ft.add(R.id.message_list_container, mMessageListFragment);
			ft.commit();
		}

		// Check if the fragment wasn't restarted and has a MessageReference in
		// the arguments. If
		// so, open the referenced message.
		if (!hasMessageListFragment && mMessageViewFragment == null
				&& mMessageReference != null) {
			openMessage(mMessageReference);
		}
	}

	/**
	 * Set the initial display mode (message list, message view, or split view).
	 * 
	 * <p>
	 * <strong>Note:</strong> This method has to be called after
	 * {@link #findFragments()} because the result depends on the availability
	 * of a {@link MessageViewFragment} instance.
	 * </p>
	 * 
	 * @param savedInstanceState
	 *            The saved instance state that was passed to the activity as
	 *            argument to {@link #onCreate(Bundle)}. May be {@code null}.
	 */
	private void initializeDisplayMode(Bundle savedInstanceState) {
		if (useSplitView()) {
			mDisplayMode = DisplayMode.SPLIT_VIEW;
			return;
		}

		if (savedInstanceState != null) {
			DisplayMode savedDisplayMode = (DisplayMode) savedInstanceState
					.getSerializable(STATE_DISPLAY_MODE);
			if (savedDisplayMode != DisplayMode.SPLIT_VIEW) {
				mDisplayMode = savedDisplayMode;
				return;
			}
		}

		if (mMessageViewFragment != null || mMessageReference != null) {
			mDisplayMode = DisplayMode.MESSAGE_VIEW;
		} else {
			mDisplayMode = DisplayMode.MESSAGE_LIST;
		}
	}

	private boolean useSplitView() {
		SplitViewMode splitViewMode = K9.getSplitViewMode();
		int orientation = getResources().getConfiguration().orientation;

		return (splitViewMode == SplitViewMode.ALWAYS || (splitViewMode == SplitViewMode.WHEN_IN_LANDSCAPE && orientation == Configuration.ORIENTATION_LANDSCAPE));
	}

	private void initializeLayout() {
		mMessageViewContainer = (ViewGroup) findViewById(R.id.message_view_container);
		mMessageViewPlaceHolder = getLayoutInflater().inflate(
				R.layout.empty_message_view, null);
	}

	private void displayViews() {
		switch (mDisplayMode) {
		case MESSAGE_LIST: {
			showMessageList();
			break;
		}
		case MESSAGE_VIEW: {
			showMessageView();
			break;
		}
		case SPLIT_VIEW: {
			mMessageListWasDisplayed = true;
			if (mMessageViewFragment == null) {
				showMessageViewPlaceHolder();
			} else {
				MessageReference activeMessage = mMessageViewFragment
						.getMessageReference();
				if (activeMessage != null) {
					mMessageListFragment.setActiveMessage(activeMessage);
				}
			}
			break;
		}
		}
	}

	private boolean decodeExtras(Intent intent) {
		String action = intent.getAction();
		if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
			Uri uri = intent.getData();
			List<String> segmentList = uri.getPathSegments();

			String accountId = segmentList.get(0);
			Collection<Account> accounts = Preferences.getPreferences(this)
					.getAvailableAccounts();
			for (Account account : accounts) {
				if (String.valueOf(account.getAccountNumber())
						.equals(accountId)) {
					mMessageReference = new MessageReference();
					mMessageReference.accountUuid = account.getUuid();
					mMessageReference.folderName = segmentList.get(1);
					mMessageReference.uid = segmentList.get(2);
					break;
				}
			}
		} else if (ACTION_SHORTCUT.equals(action)) {
			// Handle shortcut intents
			String specialFolder = intent.getStringExtra(EXTRA_SPECIAL_FOLDER);
			if (SearchAccount.UNIFIED_INBOX.equals(specialFolder)) {
				mSearch = SearchAccount.createUnifiedInboxAccount(this)
						.getRelatedSearch();
			} else if (SearchAccount.ALL_MESSAGES.equals(specialFolder)) {
				mSearch = SearchAccount.createAllMessagesAccount(this)
						.getRelatedSearch();
			}
		} else if (intent.getStringExtra(SearchManager.QUERY) != null) {
			// check if this intent comes from the system search ( remote )
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				// Query was received from Search Dialog
				String query = intent.getStringExtra(SearchManager.QUERY);

				mSearch = new LocalSearch(getString(R.string.search_results));
				mSearch.setManualSearch(true);
				mNoThreading = true;

				mSearch.or(new SearchCondition(Searchfield.SENDER,
						Attribute.CONTAINS, query));
				mSearch.or(new SearchCondition(Searchfield.SUBJECT,
						Attribute.CONTAINS, query));
				mSearch.or(new SearchCondition(Searchfield.MESSAGE_CONTENTS,
						Attribute.CONTAINS, query));

				Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
				if (appData != null) {
					mSearch.addAccountUuid(appData
							.getString(EXTRA_SEARCH_ACCOUNT));
					// searches started from a folder list activity will provide
					// an account, but no folder
					if (appData.getString(EXTRA_SEARCH_FOLDER) != null) {
						mSearch.addAllowedFolder(appData
								.getString(EXTRA_SEARCH_FOLDER));
					}
				} else {
					mSearch.addAccountUuid(SearchSpecification.ALL_ACCOUNTS);
				}
			}
		} else {
			// regular LocalSearch object was passed
			mSearch = intent.getParcelableExtra(EXTRA_SEARCH);
			mNoThreading = intent.getBooleanExtra(EXTRA_NO_THREADING, false);
		}

		if (mMessageReference == null) {
			mMessageReference = intent
					.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
		}

		if (mMessageReference != null) {
			mSearch = new LocalSearch();
			mSearch.addAccountUuid(mMessageReference.accountUuid);
			mSearch.addAllowedFolder(mMessageReference.folderName);
		}

		if (mSearch == null) {
			// We've most likely been started by an old unread widget
			String accountUuid = intent.getStringExtra("account");
			String folderName = intent.getStringExtra("folder");

			mSearch = new LocalSearch(folderName);
			mSearch.addAccountUuid((accountUuid == null) ? "invalid"
					: accountUuid);
			if (folderName != null) {
				mSearch.addAllowedFolder(folderName);
			}
		}

		Preferences prefs = Preferences.getPreferences(getApplicationContext());

		String[] accountUuids = mSearch.getAccountUuids();
		if (mSearch.searchAllAccounts()) {
			Account[] accounts = prefs.getAccounts();
			mSingleAccountMode = (accounts.length == 1);
			if (mSingleAccountMode) {
				mAccount = accounts[0];
			}
		} else {
			mSingleAccountMode = (accountUuids.length == 1);
			if (mSingleAccountMode) {
				mAccount = prefs.getAccount(accountUuids[0]);
			}
		}
		mSingleFolderMode = mSingleAccountMode
				&& (mSearch.getFolderNames().size() == 1);

		if (mSingleAccountMode
				&& (mAccount == null || !mAccount.isAvailable(this))) {
			Log.i(K9.LOG_TAG, "not opening MessageList of unavailable account");
			onAccountUnavailable();
			return false;
		}

		if (mSingleFolderMode) {
			mFolderName = mSearch.getFolderNames().get(0);
		}

		// now we know if we are in single account mode and need a subtitle
		mActionBarSubTitle.setVisibility((!mSingleFolderMode) ? View.GONE
				: View.VISIBLE);

		return true;
	}

	@Override
	public void onPause() {
		super.onPause();

		StorageManager.getInstance(getApplication()).removeListener(
				mStorageListener);
		MessagingController.getInstance(getApplication()).removeListener(
				mAdapter.mListener);
		mAdapter.mListener.onPause(this);

		MessagingController.getInstance(getApplication()).removeListener(
				mListener_Accounts);
		StorageManager.getInstance(getApplication()).removeListener(
				storageListener);
		mListener_Accounts.onPause(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(STATE_DISPLAY_MODE, mDisplayMode);
		outState.putBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED,
				mMessageListWasDisplayed);
	}

	public void onExport(final boolean includeGlobals, final Account account) {

		// TODO, prompt to allow a user to choose which accounts to export
		Set<String> accountUuids = null;
		if (account != null) {
			accountUuids = new HashSet<String>();
			accountUuids.add(account.getUuid());
		}

		ExportAsyncTask asyncTask = new ExportAsyncTask(this, includeGlobals,
				accountUuids);
		setNonConfigurationInstance(asyncTask);
		asyncTask.execute();
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		mMessageListWasDisplayed = savedInstanceState
				.getBoolean(STATE_MESSAGE_LIST_WAS_DISPLAYED);
	}

	private void initializeActionBar() {
		mActionBar = getActionBar();

		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setCustomView(R.layout.actionbar_custom);

		View customView = mActionBar.getCustomView();
		mActionBarMessageList = customView
				.findViewById(R.id.actionbar_message_list);
		mActionBarMessageView = customView
				.findViewById(R.id.actionbar_message_view);
		mActionBarSubject = (MessageTitleView) customView
				.findViewById(R.id.message_title_view);
		mActionBarTitle = (TextView) customView
				.findViewById(R.id.actionbar_title_first);
		mActionBarSubTitle = (TextView) customView
				.findViewById(R.id.actionbar_title_sub);
		mActionBarUnread = (TextView) customView
				.findViewById(R.id.actionbar_unread_count);
		mActionBarProgress = (ProgressBar) customView
				.findViewById(R.id.actionbar_progress);
		mActionButtonIndeterminateProgress = getLayoutInflater().inflate(
				R.layout.actionbar_indeterminate_progress_actionview, null);

		mActionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void setupGestureDetector(OnSwipeGestureListener listener) {
		mBase.setupGestureDetector(listener);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		boolean ret = false;
		if (KeyEvent.ACTION_DOWN == event.getAction()) {
			ret = onCustomKeyDown(event.getKeyCode(), event);
		}
		if (!ret) {
			ret = super.dispatchKeyEvent(event);
		}
		return ret;
	}

	@Override
	public void onBackPressed() {
		if (mDisplayMode == DisplayMode.MESSAGE_VIEW
				&& mMessageListWasDisplayed) {
			showMessageList();
		} else {
			super.onBackPressed();
		}
	}

	/**
	 * Handle hotkeys
	 * 
	 * <p>
	 * This method is called by {@link #dispatchKeyEvent(KeyEvent)} before any
	 * view had the chance to consume this key event.
	 * </p>
	 * 
	 * @param keyCode
	 *            The value in {@code event.getKeyCode()}.
	 * @param event
	 *            Description of the key event.
	 * 
	 * @return {@code true} if this event was consumed.
	 */
	public boolean onCustomKeyDown(final int keyCode, final KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP: {
			if (mMessageViewFragment != null
					&& mDisplayMode != DisplayMode.MESSAGE_LIST
					&& K9.useVolumeKeysForNavigationEnabled()) {
				showPreviousMessage();
				return true;
			} else if (mDisplayMode != DisplayMode.MESSAGE_VIEW
					&& K9.useVolumeKeysForListNavigationEnabled()) {
				mMessageListFragment.onMoveUp();
				return true;
			}

			break;
		}
		case KeyEvent.KEYCODE_VOLUME_DOWN: {
			if (mMessageViewFragment != null
					&& mDisplayMode != DisplayMode.MESSAGE_LIST
					&& K9.useVolumeKeysForNavigationEnabled()) {
				showNextMessage();
				return true;
			} else if (mDisplayMode != DisplayMode.MESSAGE_VIEW
					&& K9.useVolumeKeysForListNavigationEnabled()) {
				mMessageListFragment.onMoveDown();
				return true;
			}

			break;
		}
		case KeyEvent.KEYCODE_C: {
			mMessageListFragment.onCompose();
			return true;
		}
		case KeyEvent.KEYCODE_Q: {
			if (mMessageListFragment != null
					&& mMessageListFragment.isSingleAccountMode()) {
				onShowFolderList();
			}
			return true;
		}
		case KeyEvent.KEYCODE_O: {
			mMessageListFragment.onCycleSort();
			return true;
		}
		case KeyEvent.KEYCODE_I: {
			mMessageListFragment.onReverseSort();
			return true;
		}
		case KeyEvent.KEYCODE_DEL:
		case KeyEvent.KEYCODE_D: {
			if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
				mMessageListFragment.onDelete();
			} else if (mMessageViewFragment != null) {
				mMessageViewFragment.onDelete();
			}
			return true;
		}
		case KeyEvent.KEYCODE_S: {
			mMessageListFragment.toggleMessageSelect();
			return true;
		}
		case KeyEvent.KEYCODE_G: {
			if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
				mMessageListFragment.onToggleFlagged();
			} else if (mMessageViewFragment != null) {
				mMessageViewFragment.onToggleFlagged();
			}
			return true;
		}
		case KeyEvent.KEYCODE_M: {
			if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
				mMessageListFragment.onMove();
			} else if (mMessageViewFragment != null) {
				mMessageViewFragment.onMove();
			}
			return true;
		}
		case KeyEvent.KEYCODE_V: {
			if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
				mMessageListFragment.onArchive();
			} else if (mMessageViewFragment != null) {
				mMessageViewFragment.onArchive();
			}
			return true;
		}
		case KeyEvent.KEYCODE_Y: {
			if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
				mMessageListFragment.onCopy();
			} else if (mMessageViewFragment != null) {
				mMessageViewFragment.onCopy();
			}
			return true;
		}
		case KeyEvent.KEYCODE_Z: {
			if (mDisplayMode == DisplayMode.MESSAGE_LIST) {
				mMessageListFragment.onToggleRead();
			} else if (mMessageViewFragment != null) {
				mMessageViewFragment.onToggleRead();
			}
			return true;
		}
		case KeyEvent.KEYCODE_F: {
			if (mMessageViewFragment != null) {
				mMessageViewFragment.onForward();
			}
			return true;
		}
		case KeyEvent.KEYCODE_A: {
			if (mMessageViewFragment != null) {
				mMessageViewFragment.onReplyAll();
			}
			return true;
		}
		case KeyEvent.KEYCODE_R: {
			if (mMessageViewFragment != null) {
				mMessageViewFragment.onReply();
			}
			return true;
		}
		case KeyEvent.KEYCODE_J:
		case KeyEvent.KEYCODE_P: {
			if (mMessageViewFragment != null) {
				showPreviousMessage();
			}
			return true;
		}
		case KeyEvent.KEYCODE_N:
		case KeyEvent.KEYCODE_K: {
			if (mMessageViewFragment != null) {
				showNextMessage();
			}
			return true;
		}
		/*
		 * FIXME case KeyEvent.KEYCODE_Z: { mMessageViewFragment.zoom(event);
		 * return true; }
		 */
		case KeyEvent.KEYCODE_H: {
			Toast toast = Toast.makeText(this, R.string.message_list_help_key,
					Toast.LENGTH_LONG);
			toast.show();
			return true;
		}

		}

		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Swallow these events too to avoid the audible notification of a
		// volume change
		if (K9.useVolumeKeysForListNavigationEnabled()) {
			if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)
					|| (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
				if (K9.DEBUG)
					Log.v(K9.LOG_TAG, "Swallowed key up.");
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private void onShowFolderList() {
		FolderList.actionHandleAccount(this, mAccount);
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		int itemId = item.getItemId();

		if (item.getItemId() == android.R.id.home) {

			if (mDrawerLayout.isDrawerOpen(mDrawerLinear)) {
				mDrawerLayout.closeDrawer(mDrawerLinear);
			} else {
				mDrawerLayout.openDrawer(mDrawerLinear);
			}
		}

		switch (itemId) {

		case R.id.compose: {
			mMessageListFragment.onCompose();
			return true;
		}
		case R.id.toggle_message_view_theme: {
			onToggleTheme();
			return true;
		}
		// MessageList
		case R.id.check_mail: {
			mMessageListFragment.checkMail();
			return true;
		}
		case R.id.set_sort_date: {
			mMessageListFragment.changeSort(SortType.SORT_DATE);
			return true;
		}
		case R.id.set_sort_arrival: {
			mMessageListFragment.changeSort(SortType.SORT_ARRIVAL);
			return true;
		}
		case R.id.set_sort_subject: {
			mMessageListFragment.changeSort(SortType.SORT_SUBJECT);
			return true;
		}
		case R.id.set_sort_sender: {
			mMessageListFragment.changeSort(SortType.SORT_SENDER);
			return true;
		}
		case R.id.set_sort_flag: {
			mMessageListFragment.changeSort(SortType.SORT_FLAGGED);
			return true;
		}
		case R.id.set_sort_unread: {
			mMessageListFragment.changeSort(SortType.SORT_UNREAD);
			return true;
		}
		case R.id.set_sort_attach: {
			mMessageListFragment.changeSort(SortType.SORT_ATTACHMENT);
			return true;
		}
		case R.id.select_all: {
			mMessageListFragment.selectAll();
			return true;
		}
		case R.id.app_settings: {
			onEditPrefs();
			return true;
		}
		case R.id.account_settings: {
			onEditAccount();
			return true;
		}
		case R.id.search: {
			mMessageListFragment.onSearchRequested();
			return true;
		}
		case R.id.search_remote: {
			mMessageListFragment.onRemoteSearch();
			return true;
		}
		case R.id.mark_all_as_read: {
			mMessageListFragment.markAllAsRead();
			return true;
		}
		case R.id.show_folder_list: {
			onShowFolderList();
			return true;
		}
		// MessageView
		case R.id.next_message: {
			showNextMessage();
			return true;
		}
		case R.id.previous_message: {
			showPreviousMessage();
			return true;
		}
		case R.id.delete: {
			mMessageViewFragment.onDelete();
			return true;
		}
		case R.id.reply: {
			mMessageViewFragment.onReply();
			return true;
		}
		case R.id.reply_all: {
			mMessageViewFragment.onReplyAll();
			return true;
		}
		case R.id.forward: {
			mMessageViewFragment.onForward();
			return true;
		}
		case R.id.share: {
			mMessageViewFragment.onSendAlternate();
			return true;
		}
		case R.id.toggle_unread: {
			mMessageViewFragment.onToggleRead();
			return true;
		}
		case R.id.archive:
		case R.id.refile_archive: {
			mMessageViewFragment.onArchive();
			return true;
		}
		case R.id.spam:
		case R.id.refile_spam: {
			mMessageViewFragment.onSpam();
			return true;
		}
		case R.id.move:
		case R.id.refile_move: {
			mMessageViewFragment.onMove();
			return true;
		}
		case R.id.copy:
		case R.id.refile_copy: {
			mMessageViewFragment.onCopy();
			return true;
		}
		case R.id.select_text: {
			mMessageViewFragment.onSelectText();
			return true;
		}
		case R.id.show_headers:
		case R.id.hide_headers: {
			mMessageViewFragment.onToggleAllHeadersView();
			updateMenu();
			return true;
		}
		}

		if (!mSingleFolderMode) {
			// None of the options after this point are "safe" for search
			// results
			// TODO: This is not true for "unread" and "starred" searches in
			// regular folders
			return false;
		}

		switch (itemId) {
		case R.id.send_messages: {
			mMessageListFragment.onSendPendingMessages();
			return true;
		}
		case R.id.folder_settings: {
			if (mFolderName != null) {
				FolderSettings.actionSettings(this, mAccount, mFolderName);
			}
			return true;
		}
		case R.id.expunge: {
			mMessageListFragment.onExpunge();
			return true;
		}
		default: {
			return super.onOptionsItemSelected(item);
		}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		getMenuInflater().inflate(R.menu.message_list_option, menu);
		mMenu = menu;
		mMenuButtonCheckMail = menu.findItem(R.id.check_mail);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		configureMenu(menu);
		return true;
	}

	/**
	 * Hide menu items not appropriate for the current context.
	 * 
	 * <p>
	 * <strong>Note:</strong> Please adjust the comments in
	 * {@code res/menu/message_list_option.xml} if you change the visibility of
	 * a menu item in this method.
	 * </p>
	 * 
	 * @param mMenu2
	 *            The {@link Menu} instance that should be modified. May be
	 *            {@code null}; in that case the method does nothing and
	 *            immediately returns.
	 */
	private void configureMenu(android.view.Menu mMenu2) {
		if (mMenu2 == null) {
			return;
		}

		// Set visibility of account/folder settings menu items
		if (mMessageListFragment == null) {
			mMenu2.findItem(R.id.account_settings).setVisible(false);
			mMenu2.findItem(R.id.folder_settings).setVisible(false);
		} else {
			mMenu2.findItem(R.id.account_settings).setVisible(
					mMessageListFragment.isSingleAccountMode());
			mMenu2.findItem(R.id.folder_settings).setVisible(
					mMessageListFragment.isSingleFolderMode());
		}

		/*
		 * Set visibility of menu items related to the message view
		 */

		if (mDisplayMode == DisplayMode.MESSAGE_LIST
				|| mMessageViewFragment == null
				|| !mMessageViewFragment.isInitialized()) {
			mMenu2.findItem(R.id.next_message).setVisible(false);
			mMenu2.findItem(R.id.previous_message).setVisible(false);
			mMenu2.findItem(R.id.single_message_options).setVisible(false);
			mMenu2.findItem(R.id.delete).setVisible(false);
			mMenu2.findItem(R.id.compose).setVisible(false);
			mMenu2.findItem(R.id.archive).setVisible(false);
			mMenu2.findItem(R.id.move).setVisible(false);
			mMenu2.findItem(R.id.copy).setVisible(false);
			mMenu2.findItem(R.id.spam).setVisible(false);
			mMenu2.findItem(R.id.refile).setVisible(false);
			mMenu2.findItem(R.id.toggle_unread).setVisible(false);
			mMenu2.findItem(R.id.select_text).setVisible(false);
			mMenu2.findItem(R.id.toggle_message_view_theme).setVisible(false);
			mMenu2.findItem(R.id.show_headers).setVisible(false);
			mMenu2.findItem(R.id.hide_headers).setVisible(false);
		} else {
			// hide prev/next buttons in split mode
			if (mDisplayMode != DisplayMode.MESSAGE_VIEW) {
				mMenu2.findItem(R.id.next_message).setVisible(false);
				mMenu2.findItem(R.id.previous_message).setVisible(false);
			} else {
				MessageReference ref = mMessageViewFragment
						.getMessageReference();
				boolean initialized = (mMessageListFragment != null && mMessageListFragment
						.isLoadFinished());
				boolean canDoPrev = (initialized && !mMessageListFragment
						.isFirst(ref));
				boolean canDoNext = (initialized && !mMessageListFragment
						.isLast(ref));

				MenuItem prev = mMenu2.findItem(R.id.previous_message);
				prev.setEnabled(canDoPrev);
				// prev.getIcon().setAlpha(canDoPrev ? 255 : 127);

				// MenuItem next = menu.findItem(R.id.next_message);
				// next.setEnabled(canDoNext);
				// next.getIcon().setAlpha(canDoNext ? 255 : 127);
			}

			MenuItem toggleTheme = mMenu2
					.findItem(R.id.toggle_message_view_theme);
			if (K9.useFixedMessageViewTheme()) {
				toggleTheme.setVisible(false);
			} else {
				// Set title of menu item to switch to dark/light theme
				if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
					toggleTheme
							.setTitle(R.string.message_view_theme_action_light);
				} else {
					toggleTheme
							.setTitle(R.string.message_view_theme_action_dark);
				}
				toggleTheme.setVisible(true);
			}

			// Set title of menu item to toggle the read state of the currently
			// displayed message
			if (mMessageViewFragment.isMessageRead()) {
				mMenu2.findItem(R.id.toggle_unread).setTitle(
						R.string.mark_as_unread_action);
			} else {
				mMenu2.findItem(R.id.toggle_unread).setTitle(
						R.string.mark_as_read_action);
			}

			// Jellybean has built-in long press selection support
			mMenu2.findItem(R.id.select_text).setVisible(
					Build.VERSION.SDK_INT < 16);

			mMenu2.findItem(R.id.delete).setVisible(
					K9.isMessageViewDeleteActionVisible());

			/*
			 * Set visibility of copy, move, archive, spam in action bar and
			 * refile submenu
			 */
			if (mMessageViewFragment.isCopyCapable()) {
				mMenu2.findItem(R.id.copy).setVisible(
						K9.isMessageViewCopyActionVisible());
				mMenu2.findItem(R.id.refile_copy).setVisible(true);
			} else {
				mMenu2.findItem(R.id.copy).setVisible(false);
				mMenu2.findItem(R.id.refile_copy).setVisible(false);
			}

			if (mMessageViewFragment.isMoveCapable()) {
				boolean canMessageBeArchived = mMessageViewFragment
						.canMessageBeArchived();
				boolean canMessageBeMovedToSpam = mMessageViewFragment
						.canMessageBeMovedToSpam();

				mMenu2.findItem(R.id.move).setVisible(
						K9.isMessageViewMoveActionVisible());
				mMenu2.findItem(R.id.archive).setVisible(
						canMessageBeArchived
								&& K9.isMessageViewArchiveActionVisible());
				mMenu2.findItem(R.id.spam).setVisible(
						canMessageBeMovedToSpam
								&& K9.isMessageViewSpamActionVisible());

				mMenu2.findItem(R.id.refile_move).setVisible(true);
				mMenu2.findItem(R.id.refile_archive).setVisible(
						canMessageBeArchived);
				mMenu2.findItem(R.id.refile_spam).setVisible(
						canMessageBeMovedToSpam);
			} else {
				mMenu2.findItem(R.id.move).setVisible(false);
				mMenu2.findItem(R.id.archive).setVisible(false);
				mMenu2.findItem(R.id.spam).setVisible(false);

				mMenu2.findItem(R.id.refile).setVisible(false);
			}

			if (mMessageViewFragment.allHeadersVisible()) {
				mMenu2.findItem(R.id.show_headers).setVisible(false);
			} else {
				mMenu2.findItem(R.id.hide_headers).setVisible(false);
			}
		}

		/*
		 * Set visibility of menu items related to the message list
		 */

		// Hide both search menu items by default and enable one when
		// appropriate
		mMenu2.findItem(R.id.search).setVisible(false);
		mMenu2.findItem(R.id.search_remote).setVisible(false);

		if (mDisplayMode == DisplayMode.MESSAGE_VIEW
				|| mMessageListFragment == null
				|| !mMessageListFragment.isInitialized()) {
			mMenu2.findItem(R.id.check_mail).setVisible(false);
			mMenu2.findItem(R.id.set_sort).setVisible(false);
			mMenu2.findItem(R.id.select_all).setVisible(false);
			mMenu2.findItem(R.id.send_messages).setVisible(false);
			mMenu2.findItem(R.id.expunge).setVisible(false);
			mMenu2.findItem(R.id.mark_all_as_read).setVisible(false);
			mMenu2.findItem(R.id.show_folder_list).setVisible(false);
		} else {
			mMenu2.findItem(R.id.set_sort).setVisible(true);
			mMenu2.findItem(R.id.select_all).setVisible(true);
			mMenu2.findItem(R.id.compose).setVisible(true);
			mMenu2.findItem(R.id.mark_all_as_read).setVisible(
					mMessageListFragment.isMarkAllAsReadSupported());

			if (!mMessageListFragment.isSingleAccountMode()) {
				mMenu2.findItem(R.id.expunge).setVisible(false);
				mMenu2.findItem(R.id.send_messages).setVisible(false);
				mMenu2.findItem(R.id.show_folder_list).setVisible(false);
			} else {
				mMenu2.findItem(R.id.send_messages).setVisible(
						mMessageListFragment.isOutbox());
				mMenu2.findItem(R.id.expunge).setVisible(
						mMessageListFragment.isRemoteFolder()
								&& mMessageListFragment
										.isAccountExpungeCapable());
				mMenu2.findItem(R.id.show_folder_list).setVisible(true);
			}

			mMenu2.findItem(R.id.check_mail).setVisible(
					mMessageListFragment.isCheckMailSupported());

			// If this is an explicit local search, show the option to search on
			// the server
			if (!mMessageListFragment.isRemoteSearch()
					&& mMessageListFragment.isRemoteSearchAllowed()) {
				mMenu2.findItem(R.id.search_remote).setVisible(true);
			} else if (!mMessageListFragment.isManualSearch()) {
				mMenu2.findItem(R.id.search).setVisible(true);
			}
		}
	}

	protected void onAccountUnavailable() {
		finish();
		// TODO inform user about account unavailability using Toast
		Accounts.listAccounts(this);
	}

	public void setActionBarTitle(String title) {
		mActionBarTitle.setText(title);
	}

	public void setActionBarSubTitle(String subTitle) {
		mActionBarSubTitle.setText(subTitle);
	}

	public void setActionBarUnread(int unread) {
		if (unread == 0) {
			mActionBarUnread.setVisibility(View.GONE);
		} else {
			mActionBarUnread.setVisibility(View.VISIBLE);
			mActionBarUnread.setText(Integer.toString(unread));

		}
	}

	@Override
	public void setMessageListTitle(String title) {
		setActionBarTitle(title);
	}

	@Override
	public void setMessageListSubTitle(String subTitle) {
		setActionBarSubTitle(subTitle);
	}

	@Override
	public void setUnreadCount(int unread) {
		setActionBarUnread(unread);
	}

	@Override
	public void setMessageListProgress(int progress) {
		setProgress(progress);
	}

	@Override
	public void openMessage(MessageReference messageReference) {
		Preferences prefs = Preferences.getPreferences(getApplicationContext());
		Account account = prefs.getAccount(messageReference.accountUuid);
		String folderName = messageReference.folderName;

		if (folderName.equals(account.getDraftsFolderName())) {
			MessageCompose.actionEditDraft(this, messageReference);
		} else {
			mMessageViewContainer.removeView(mMessageViewPlaceHolder);

			if (mMessageListFragment != null) {
				mMessageListFragment.setActiveMessage(messageReference);
			}

			MessageViewFragment fragment = MessageViewFragment
					.newInstance(messageReference);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.message_view_container, fragment);
			mMessageViewFragment = fragment;
			ft.commit();

			if (mDisplayMode != DisplayMode.SPLIT_VIEW) {
				showMessageView();
			}
		}
	}

	@Override
	public void onResendMessage(Message message) {
		MessageCompose.actionEditDraft(this, message.makeMessageReference());
	}

	@Override
	public void onForward(Message message) {
		MessageCompose.actionForward(this, message.getFolder().getAccount(),
				message, null);
	}

	@Override
	public void onReply(Message message) {
		MessageCompose.actionReply(this, message.getFolder().getAccount(),
				message, false, null);
	}

	@Override
	public void onReplyAll(Message message) {
		MessageCompose.actionReply(this, message.getFolder().getAccount(),
				message, true, null);
	}

	@Override
	public void onCompose(Account account) {
		MessageCompose.actionCompose(this, account);
	}

	@Override
	public void showMoreFromSameSender(String senderAddress) {
		LocalSearch tmpSearch = new LocalSearch("From " + senderAddress);
		tmpSearch.addAccountUuids(mSearch.getAccountUuids());
		tmpSearch.and(Searchfield.SENDER, senderAddress, Attribute.CONTAINS);

		MessageListFragment fragment = MessageListFragment.newInstance(
				tmpSearch, false, false);

		addMessageListFragment(fragment, true);
	}

	@Override
	public void onBackStackChanged() {
		findFragments();

		if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
			showMessageViewPlaceHolder();
		}

		configureMenu(mMenu);
	}

	@Override
	public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
		if (mMessageListFragment != null
				&& mDisplayMode != DisplayMode.MESSAGE_VIEW) {
			mMessageListFragment.onSwipeRightToLeft(e1, e2);
		}
	}

	@Override
	public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
		if (mMessageListFragment != null
				&& mDisplayMode != DisplayMode.MESSAGE_VIEW) {
			mMessageListFragment.onSwipeLeftToRight(e1, e2);
		}
	}

	private final class StorageListenerImplementation implements
			StorageManager.StorageListener {
		@Override
		public void onUnmount(String providerId) {
			if (mAccount != null
					&& providerId.equals(mAccount.getLocalStorageProviderId())) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onAccountUnavailable();
					}
				});
			}
		}

		@Override
		public void onMount(String providerId) {
			// no-op
		}
	}

	private void addMessageListFragment(MessageListFragment fragment,
			boolean addToBackStack) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();

		ft.replace(R.id.message_list_container, fragment);
		if (addToBackStack)
			ft.addToBackStack(null);

		mMessageListFragment = fragment;

		int transactionId = ft.commit();
		if (transactionId >= 0 && mFirstBackStackId < 0) {
			mFirstBackStackId = transactionId;
		}
	}

	// makes the listviews act as one listview
	public static void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			// pre-condition
			return;
		}

		int totalHeight = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight
				+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}

	@Override
	public boolean startSearch(Account account, String folderName) {
		// If this search was started from a MessageList of a single folder,
		// pass along that folder info
		// so that we can enable remote search.
		if (account != null && folderName != null) {
			final Bundle appData = new Bundle();
			appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
			appData.putString(EXTRA_SEARCH_FOLDER, folderName);
			startSearch(null, false, appData, false);
		} else {
			// TODO Handle the case where we're searching from within a search
			// result.
			startSearch(null, false, null, false);
		}

		return true;
	}

	@Override
	public void showThread(Account account, String folderName, long threadRootId) {
		showMessageViewPlaceHolder();

		LocalSearch tmpSearch = new LocalSearch();
		tmpSearch.addAccountUuid(account.getUuid());
		tmpSearch.and(Searchfield.THREAD_ID, String.valueOf(threadRootId),
				Attribute.EQUALS);

		MessageListFragment fragment = MessageListFragment.newInstance(
				tmpSearch, true, false);
		addMessageListFragment(fragment, true);
	}

	private void showMessageViewPlaceHolder() {
		removeMessageViewFragment();

		// Add placeholder view if necessary
		if (mMessageViewPlaceHolder.getParent() == null) {
			mMessageViewContainer.addView(mMessageViewPlaceHolder);
		}

		mMessageListFragment.setActiveMessage(null);
	}

	/**
	 * Remove MessageViewFragment if necessary.
	 */
	private void removeMessageViewFragment() {
		if (mMessageViewFragment != null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.remove(mMessageViewFragment);
			mMessageViewFragment = null;
			ft.commit();

			showDefaultTitleView();
		}
	}

	private void removeMessageListFragment() {
		if (mMessageViewFragment != null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.remove(mMessageListFragment);
			mMessageListFragment = null;
			ft.commit();
		}
	}

	@Override
	public void remoteSearchStarted() {
		// Remove action button for remote search
		configureMenu(mMenu);
	}

	@Override
	public void goBack() {
		FragmentManager fragmentManager = getFragmentManager();
		if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
			showMessageList();
		} else if (fragmentManager.getBackStackEntryCount() > 0) {
			fragmentManager.popBackStack();
		} else if (mMessageListFragment.isManualSearch()) {
			finish();
		} else if (!mSingleFolderMode) {
			onAccounts();
		} else {
			onShowFolderList();
		}
	}

	@Override
	public void enableActionBarProgress(boolean enable) {
		if (mMenuButtonCheckMail != null && mMenuButtonCheckMail.isVisible()) {
			mActionBarProgress.setVisibility(View.GONE);
			if (enable) {
				mMenuButtonCheckMail
						.setActionView(mActionButtonIndeterminateProgress);
			} else {
				mMenuButtonCheckMail.setActionView(null);
			}
		} else {
			if (mMenuButtonCheckMail != null)
				mMenuButtonCheckMail.setActionView(null);
			if (enable) {
				mActionBarProgress.setVisibility(View.VISIBLE);
			} else {
				mActionBarProgress.setVisibility(View.GONE);
			}
		}
	}

	private void restartActivity() {
		// restart the current activity, so that the theme change can be applied
		if (Build.VERSION.SDK_INT < 11) {
			Intent intent = getIntent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0); // disable animations to speed up
												// the switch
			startActivity(intent);
			overridePendingTransition(0, 0);
		} else {
			recreate();
		}
	}

	@Override
	public void displayMessageSubject(String subject) {
		if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
			mActionBarSubject.setText(subject);
		}
	}

	@Override
	public void onReply(Message message, PgpData pgpData) {
		MessageCompose.actionReply(this, mAccount, message, false,
				pgpData.getDecryptedData());
	}

	@Override
	public void onReplyAll(Message message, PgpData pgpData) {
		MessageCompose.actionReply(this, mAccount, message, true,
				pgpData.getDecryptedData());
	}

	@Override
	public void onForward(Message mMessage, PgpData mPgpData) {
		MessageCompose.actionForward(this, mAccount, mMessage,
				mPgpData.getDecryptedData());
	}

	@Override
	public void showNextMessageOrReturn() {
		if (K9.messageViewReturnToList() || !showLogicalNextMessage()) {
			if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
				showMessageViewPlaceHolder();
			} else {
				showMessageList();
			}
		}
	}

	/**
	 * Shows the next message in the direction the user was displaying messages.
	 * 
	 * @return {@code true}
	 */
	private boolean showLogicalNextMessage() {
		boolean result = false;
		if (mLastDirection == NEXT) {
			result = showNextMessage();
		} else if (mLastDirection == PREVIOUS) {
			result = showPreviousMessage();
		}

		if (!result) {
			result = showNextMessage() || showPreviousMessage();
		}

		return result;
	}

	@Override
	public void setProgress(boolean enable) {
		setProgressBarIndeterminateVisibility(enable);
	}

	@Override
	public void messageHeaderViewAvailable(MessageHeader header) {
		mActionBarSubject.setMessageHeader(header);
	}

	private boolean showNextMessage() {
		MessageReference ref = mMessageViewFragment.getMessageReference();
		if (ref != null) {
			if (mMessageListFragment.openNext(ref)) {
				mLastDirection = NEXT;
				return true;
			}
		}
		return false;
	}

	private boolean showPreviousMessage() {
		MessageReference ref = mMessageViewFragment.getMessageReference();
		if (ref != null) {
			if (mMessageListFragment.openPrevious(ref)) {
				mLastDirection = PREVIOUS;
				return true;
			}
		}
		return false;
	}

	private void showMessageList() {
		mMessageListWasDisplayed = true;
		mDisplayMode = DisplayMode.MESSAGE_LIST;
		mViewSwitcher.showFirstView();

		mMessageListFragment.setActiveMessage(null);

		showDefaultTitleView();
		configureMenu(mMenu);
	}

	private void showMessageView() {
		mDisplayMode = DisplayMode.MESSAGE_VIEW;

		if (!mMessageListWasDisplayed) {
			mViewSwitcher.setAnimateFirstView(false);
		}
		mViewSwitcher.showSecondView();

		showMessageTitleView();
		configureMenu(mMenu);
	}

	@Override
	public void updateMenu() {
		invalidateOptionsMenu();
	}

	@Override
	public void disableDeleteAction() {
		mMenu.findItem(R.id.delete).setEnabled(false);
	}

	private void onToggleTheme() {
		if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
			K9.setK9MessageViewThemeSetting(K9.Theme.LIGHT);
		} else {
			K9.setK9MessageViewThemeSetting(K9.Theme.DARK);
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				Context appContext = getApplicationContext();
				Preferences prefs = Preferences.getPreferences(appContext);
				Editor editor = prefs.getPreferences().edit();
				K9.save(editor);
				editor.commit();
			}
		}).start();

		restartActivity();
	}

	private void showDefaultTitleView() {
		mActionBarMessageView.setVisibility(View.GONE);
		mActionBarMessageList.setVisibility(View.VISIBLE);

		if (mMessageListFragment != null) {
			mMessageListFragment.updateTitle();
		}

		mActionBarSubject.setMessageHeader(null);
	}

	private void showMessageTitleView() {
		mActionBarMessageList.setVisibility(View.GONE);
		mActionBarMessageView.setVisibility(View.VISIBLE);

		if (mMessageViewFragment != null) {
			displayMessageSubject(null);
			mMessageViewFragment.updateTitle();
		}
	}

	@Override
	public void onSwitchComplete(int displayedChild) {
		if (displayedChild == 0) {
			removeMessageViewFragment();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.i(K9.LOG_TAG, "onActivityResult requestCode = " + requestCode
				+ ", resultCode = " + resultCode + ", data = " + data);
		if (resultCode != RESULT_OK)
			return;
		if (data == null) {
			return;
		}
		switch (requestCode) {
		case ACTIVITY_REQUEST_PICK_SETTINGS_FILE:
			onImport(data.getData());
			break;
		}
		// handle OpenPGP results from PendingIntents in OpenPGP view
		// must be handled in this main activity, because
		// startIntentSenderForResult() does not support Fragments
		MessageOpenPgpView openPgpView = (MessageOpenPgpView) findViewById(R.id.layout_decrypt_openpgp);
		if (openPgpView != null
				&& openPgpView.handleOnActivityResult(requestCode, resultCode,
						data)) {
			return;
		}
	}

	public class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {

		if (position < mAdapter_Accounts.getCount() + 1) {
			Log.d("Accounts", "clicked");

			progress = ProgressDialog.show(this, "", "", true);

			goBack();

			BaseAccount account = (BaseAccount) getListView()
					.getItemAtPosition(position);
			onOpenAccount(account);

			recreate();

			progress.dismiss();

		}

		if (position > mAdapter_Accounts.getCount() + 5) {

			progress = ProgressDialog.show(this, "", "", true);

			goBack();

			Log.d("Folders", "clicked");

			onOpenFolder(((FolderInfoHolder) mAdapter.getItem(position
					- (mAdapter_Accounts.getCount() + 2))).name);

			Log.d("Folder Click Listener", "clicked");

			progress.dismiss();

		}

		mListView.setItemChecked(position, true);

		mDrawerLayout.closeDrawer(mDrawerLinear);
	}

	protected void onUpdateData(int reason) {
		Log.d(TAG, "onUpdateData(" + reason + ")");
		doRefresh();
	}

	private void alert_logout() {

		SharedPreferences name = getSharedPreferences("Login_info",
				Context.MODE_PRIVATE);

		String person = name.getString("name", "");

		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Are You Sure You Want to Logout " + person
							+ "? This Will Delete All Your Data.").setTitle(
					"Logout");

			builder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {

							clearApplicationData();

							android.os.Process.killProcess(android.os.Process
									.myPid());

						}
					});

			builder.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {

							dialog.dismiss();

						}
					});

			AlertDialog alertDialog = builder.create();

			alertDialog.show();

		}

	}

	public void clearApplicationData() {
		File cache = getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
					Log.i("TAG",
							"**************** File /data/data/APP_PACKAGE/" + s
									+ " DELETED *******************");
				}
			}
		}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		if (contentObserver != null) {
			getContentResolver().unregisterContentObserver(contentObserver);
			contentObserver = null;
		}
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
	}

	protected void doRefresh() {
		Log.d(TAG, "doRefresh()");

		int countssssss = getUnreadK9Count(this);

		Log.d(TAG, "" + countssssss + " unread emails");

	}

	public static class CursorHandler {
		private List<Cursor> cursors = new ArrayList<Cursor>();

		public Cursor add(Cursor c) {
			if (c != null)
				cursors.add(c);
			return c;
		}

		public void closeAll() {
			for (Cursor c : cursors) {
				if (!c.isClosed())
					c.close();
			}
		}
	}

	private static int k9UnreadCount = 0;

	public static int getUnreadK9Count(Context context) {
		refreshUnreadK9Count(context);

		return k9UnreadCount;
	}

	private static int getUnreadK9Count(Context context, int accountNumber) {
		CursorHandler ch = new CursorHandler();
		try {
			Cursor cur = ch.add(context.getContentResolver().query(
					Uri.parse(k9UnreadUri + "/" + accountNumber + "/"), null,
					null, null, null));
			if (cur != null) {
				Log.d(TAG, "k9: " + cur.getCount() + " unread rows returned");

				if (cur.getCount() > 0) {
					cur.moveToFirst();
					int unread = 0;
					int nameIndex = cur.getColumnIndex("accountName");
					int unreadIndex = cur.getColumnIndex("unread");
					do {
						String acct = cur.getString(nameIndex);
						int unreadForAcct = cur.getInt(unreadIndex);
						Log.d(TAG, "k9: " + acct + " - " + unreadForAcct
								+ " unread");
						unread += unreadForAcct;
					} while (cur.moveToNext());
					cur.close();
					return unread;
				}
			} else {
				Log.d(TAG, "Failed to query k9 unread contentprovider.");
			}
		} catch (IllegalStateException e) {
			Log.d(TAG, "k-9 unread uri unknown.");
		}
		return 0;
	}

	public static void refreshUnreadK9Count(Context context) {
		int accounts = getK9AccountCount(context);
		if (accounts > 0) {
			int countssssss = 0;
			for (int acct = 0; acct < accounts; ++acct) {
				countssssss += getUnreadK9Count(context, acct);
			}
			k9UnreadCount = countssssss;
		}
	}

	public static int getK9AccountCount(Context context) {
		CursorHandler ch = new CursorHandler();
		try {
			Cursor cur = ch.add(context.getContentResolver().query(
					k9AccountsUri, null, null, null, null));
			if (cur != null) {
				// if (Preferences.logging) Log.d(MetaWatch.TAG,
				// "k9: "+cur.getCount()+ " account rows returned");

				int count = cur.getCount();

				return count;
			} else {
				// if (Preferences.logging) Log.d(MetaWatch.TAG,
				// "Failed to query k9 unread contentprovider.");
			}
		} catch (IllegalStateException e) {
			// if (Preferences.logging) Log.d(MetaWatch.TAG,
			// "k-9 accounts uri unknown.");
		} catch (java.lang.SecurityException e) {
			// if (Preferences.logging) Log.d(MetaWatch.TAG,
			// "Permissions failure accessing k-9 databases");
		} finally {
			ch.closeAll();
		}
		return 0;

	}

	/**
	 * Create a new {@link AccountsAdapter} instance and assign it to the
	 * {@link ListView}.
	 * 
	 * @param realAccounts
	 *            An array of accounts to display.
	 */
	public void populateListView(Account[] realAccounts) {
		List<BaseAccount> accounts = new ArrayList<BaseAccount>();

		if (displaySpecialAccounts() && !K9.isHideSpecialAccounts()) {
			BaseAccount unifiedInboxAccount = SearchAccount
					.createUnifiedInboxAccount(this);
			BaseAccount allMessagesAccount = SearchAccount
					.createAllMessagesAccount(this);

			accounts.add(unifiedInboxAccount);
			accounts.add(allMessagesAccount);
		}

		accounts.addAll(Arrays.asList(realAccounts));
		AccountsAdapter adapter = new AccountsAdapter(accounts);

		getListView().invalidate();

		mergeadapter.addView(header_inbox);

		mergeadapter.addAdapter(adapter);

		mergeadapter.addView(header_folders);

		mergeadapter.addAdapter(mAdapter);

		getListView().setAdapter(mergeadapter);

	}

	/**
	 * Implementing decide whether or not to display special accounts in the
	 * list.
	 * 
	 * @return {@code true}, if special accounts should be listed. {@code false}
	 *         , otherwise.
	 */
	protected boolean displaySpecialAccounts() {
		return true;
	}

	/**
	 * This method will be called when an account was selected.
	 * 
	 * @param account
	 *            The account the user selected.
	 */
	protected void onAccountSelected(BaseAccount account) {
	}

	/**
	 * Load accounts in a background thread
	 */
	class LoadAccounts extends AsyncTask<Void, Void, Account[]> {
		@Override
		protected Account[] doInBackground(Void... params) {
			Account[] accounts = Preferences.getPreferences(
					getApplicationContext()).getAccounts();
			return accounts;
		}

		@Override
		protected void onPostExecute(Account[] accounts) {
			populateListView(accounts);
		}
	}

	public class AccountsAdapter extends ArrayAdapter<BaseAccount> {
		public AccountsAdapter(List<BaseAccount> accounts) {
			super(MessageList.this, 0, accounts);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final BaseAccount account = getItem(position);

			final View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = getLayoutInflater().inflate(R.layout.accounts_item,
						parent, false);
				view.findViewById(R.id.active_icons);
				view.findViewById(R.id.folders);
			}

			AccountViewHolder holder = (AccountViewHolder) view.getTag();
			if (holder == null) {
				holder = new AccountViewHolder();
				holder.description = (TextView) view
						.findViewById(R.id.description);
				holder.email = (TextView) view.findViewById(R.id.email);
				holder.description = (TextView) view
						.findViewById(R.id.description);
				holder.email = (TextView) view.findViewById(R.id.email);
				holder.newMessageCount = (TextView) view
						.findViewById(R.id.new_message_count);
				holder.flaggedMessageCount = (TextView) view
						.findViewById(R.id.flagged_message_count);
				holder.newMessageCountWrapper = view
						.findViewById(R.id.new_message_count_wrapper);
				holder.flaggedMessageCountWrapper = view
						.findViewById(R.id.flagged_message_count_wrapper);
				holder.newMessageCountIcon = view
						.findViewById(R.id.new_message_count_icon);
				holder.flaggedMessageCountIcon = view
						.findViewById(R.id.flagged_message_count_icon);
				holder.activeIcons = (RelativeLayout) view
						.findViewById(R.id.active_icons);
				holder.chip = view.findViewById(R.id.chip);
				holder.folders = (ImageButton) view.findViewById(R.id.folders);
				holder.accountsItemLayout = (LinearLayout) view
						.findViewById(R.id.accounts_item_layout);

				view.setTag(holder);
			}

			String description = account.getDescription();
			if (account.getEmail().equals(description)) {
				holder.email.setVisibility(View.GONE);
			} else {
				holder.email.setVisibility(View.VISIBLE);
				holder.email.setText(account.getEmail());
			}

			if (description == null || description.isEmpty()) {
				description = account.getEmail();
			}

			holder.description.setText(description);

			if (account instanceof Account) {
				Account realAccount = (Account) account;

			} else {

			}

			mFontSizes.setViewTextSize(holder.description,
					mFontSizes.getAccountName());
			mFontSizes.setViewTextSize(holder.email,
					mFontSizes.getAccountDescription());

			AccountStats stats = accountStats.get(account.getUuid());

			if (stats != null && account instanceof Account && stats.size >= 0) {
				holder.email.setText(SizeFormatter.formatSize(MessageList.this,
						stats.size));
				holder.email.setVisibility(View.VISIBLE);
			} else {
				if (account.getEmail().equals(account.getDescription())) {
					holder.email.setVisibility(View.GONE);
				} else {
					holder.email.setVisibility(View.VISIBLE);
					holder.email.setText(account.getEmail());
				}
			}

			description = account.getDescription();
			if (description == null || description.isEmpty()) {
				description = account.getEmail();
			}

			holder.description.setText(description);

			Integer unreadMessageCount = null;
			if (stats != null) {
				unreadMessageCount = stats.unreadMessageCount;
				holder.newMessageCount.setText(Integer
						.toString(unreadMessageCount));
				holder.newMessageCountWrapper
						.setVisibility(unreadMessageCount > 0 ? View.VISIBLE
								: View.GONE);

				holder.flaggedMessageCount.setText(Integer
						.toString(stats.flaggedMessageCount));
				holder.flaggedMessageCountWrapper
						.setVisibility(K9.messageListStars()
								&& stats.flaggedMessageCount > 0 ? View.VISIBLE
								: View.GONE);

				holder.flaggedMessageCountWrapper
						.setOnClickListener(createFlaggedSearchListener(account));
				holder.newMessageCountWrapper
						.setOnClickListener(createUnreadSearchListener(account));

				holder.activeIcons.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast toast = Toast.makeText(getApplication(),
								getString(R.string.tap_hint),
								Toast.LENGTH_SHORT);
						toast.show();
					}
				});

			} else {
				holder.newMessageCountWrapper.setVisibility(View.GONE);
				holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
			}
			if (account instanceof Account) {
				Account realAccount = (Account) account;

				holder.flaggedMessageCountIcon
						.setBackgroundDrawable(realAccount.generateColorChip(
								false, false, false, false, true).drawable());
				holder.newMessageCountIcon.setBackgroundDrawable(realAccount
						.generateColorChip(false, false, false, false, false)
						.drawable());

			} else {

				holder.newMessageCountIcon.setBackgroundDrawable(new ColorChip(
						0xff999999, false, ColorChip.CIRCULAR).drawable());
				holder.flaggedMessageCountIcon
						.setBackgroundDrawable(new ColorChip(0xff999999, false,
								ColorChip.STAR).drawable());
			}

			mFontSizes.setViewTextSize(holder.description,
					mFontSizes.getAccountName());
			mFontSizes.setViewTextSize(holder.email,
					mFontSizes.getAccountDescription());

			if (account instanceof SearchAccount) {
				holder.folders.setVisibility(View.GONE);
			} else {
				holder.folders.setVisibility(View.VISIBLE);
				holder.folders.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						Log.d("clicked_folder1", "clicked");

						FolderList.actionHandleAccount(MessageList.this,
								(Account) account);

					}
				});
			}

			if (account instanceof Account) {
				Account realAccount = (Account) account;
				holder.chip.setBackgroundColor(realAccount.getChipColor());
			} else {
				holder.chip.setBackgroundColor(0xff999999);
			}

			holder.chip.getBackground().setAlpha(255);

			return view;
		}

		private OnClickListener createFlaggedSearchListener(BaseAccount account) {
			String searchTitle = getString(R.string.search_title,
					account.getDescription(),
					getString(R.string.flagged_modifier));

			Log.d("clicked_folder2", "clicked");

			LocalSearch search;
			if (account instanceof SearchAccount) {
				search = ((SearchAccount) account).getRelatedSearch().clone();
				search.setName(searchTitle);
			} else {
				search = new LocalSearch(searchTitle);
				search.addAccountUuid(account.getUuid());

				Account realAccount = (Account) account;
				realAccount.excludeSpecialFolders(search);
				realAccount.limitToDisplayableFolders(search);
			}

			search.and(Searchfield.FLAGGED, "1", Attribute.EQUALS);

			return new AccountClickListener(search);
		}

		private OnClickListener createUnreadSearchListener(BaseAccount account) {
			LocalSearch search = Accounts.createUnreadSearch(MessageList.this,
					account);
			return new AccountClickListener(search);
		}

		class AccountViewHolder {
			public TextView description;
			public TextView email;
			public TextView newMessageCount;
			public TextView flaggedMessageCount;
			public View newMessageCountIcon;
			public View flaggedMessageCountIcon;
			public View newMessageCountWrapper;
			public View flaggedMessageCountWrapper;
			public View chip;
			public RelativeLayout activeIcons;

			public ImageButton folders;
			public LinearLayout accountsItemLayout;
		}
	}

	private class AccountClickListener implements OnClickListener {

		final LocalSearch search;

		AccountClickListener(LocalSearch search) {
			this.search = search;
		}

		@Override
		public void onClick(View v) {
			MessageList.actionDisplaySearch(MessageList.this, search, true,
					false);
			Log.d("clicked_folder3", "clicked");
		}

	}

	private BaseAccount[] accounts = new BaseAccount[0];

	private enum ACCOUNT_LOCATION {
		TOP, MIDDLE, BOTTOM;
	}

	private EnumSet<ACCOUNT_LOCATION> accountLocation(BaseAccount account) {
		EnumSet<ACCOUNT_LOCATION> accountLocation = EnumSet
				.of(ACCOUNT_LOCATION.MIDDLE);
		if (accounts.length > 0) {
			if (accounts[0].equals(account)) {
				accountLocation.remove(ACCOUNT_LOCATION.MIDDLE);
				accountLocation.add(ACCOUNT_LOCATION.TOP);
			}
			if (accounts[accounts.length - 1].equals(account)) {
				accountLocation.remove(ACCOUNT_LOCATION.MIDDLE);
				accountLocation.add(ACCOUNT_LOCATION.BOTTOM);
			}
		}
		return accountLocation;
	}

	private void onAddNewAccount() {
		AccountSetupBasics.actionNewAccount(this);
	}

	/*
	 * This method is called with 'null' for the argument 'account' if all
	 * accounts are to be checked. This is handled accordingly in
	 * MessagingController.checkMail().
	 */
	private void onCheckMail(Account account) {
		MessagingController.getInstance(getApplication()).checkMail(this,
				account, true, true, null);
		if (account == null) {
			MessagingController.getInstance(getApplication())
					.sendPendingMessages(null);
		} else {
			MessagingController.getInstance(getApplication())
					.sendPendingMessages(account, null);
		}

	}

	void onClearCommands(Account account) {
		MessagingController.getInstance(getApplication()).clearAllPending(
				account);
	}

	private void onCompose() {
		Account defaultAccount = Preferences.getPreferences(this)
				.getDefaultAccount();
		if (defaultAccount != null) {
			MessageCompose.actionCompose(this, defaultAccount);
		} else {
			onAddNewAccount();
		}
	}

	/**
	 * Show that account's inbox or folder-list or return false if the account
	 * is not available.
	 * 
	 * @param account
	 *            the account to open ({@link SearchAccount} or {@link Account})
	 * @return false if unsuccessfull
	 */
	boolean onOpenAccount(BaseAccount account) {
		if (account instanceof SearchAccount) {
			SearchAccount searchAccount = (SearchAccount) account;
			MessageList.actionDisplaySearch(this,
					searchAccount.getRelatedSearch(), false, false);
		} else {
			Account realAccount = (Account) account;
			if (!realAccount.isEnabled()) {
				onActivateAccount(realAccount);
				return false;
			} else if (!realAccount.isAvailable(this)) {
				String toastText = getString(R.string.account_unavailable,
						account.getDescription());
				Toast toast = Toast.makeText(getApplication(), toastText,
						Toast.LENGTH_SHORT);
				toast.show();

				Log.i(K9.LOG_TAG,
						"refusing to open account that is not available");
				return false;
			}
			if (K9.FOLDER_NONE.equals(realAccount.getAutoExpandFolderName())) {
				FolderList.actionHandleAccount(this, realAccount);
			} else {
				LocalSearch search = new LocalSearch(
						realAccount.getAutoExpandFolderName());
				search.addAllowedFolder(realAccount.getAutoExpandFolderName());
				search.addAccountUuid(realAccount.getUuid());
				MessageList.actionDisplaySearch(this, search, false, true);
			}
		}
		return true;
	}

	void onActivateAccount(Account account) {
		List<Account> disabledAccounts = new ArrayList<Account>();
		disabledAccounts.add(account);
		promptForServerPasswords(disabledAccounts);
	}

	/**
	 * Ask the user to enter the server passwords for disabled accounts.
	 * 
	 * @param disabledAccounts
	 *            A non-empty list of {@link Account}s to ask the user for
	 *            passwords. Never {@code null}.
	 *            <p>
	 *            <strong>Note:</strong> Calling this method will modify the
	 *            supplied list.
	 *            </p>
	 */
	void promptForServerPasswords(final List<Account> disabledAccounts) {
		Account account = disabledAccounts.remove(0);
		PasswordPromptDialog dialog = new PasswordPromptDialog(account,
				disabledAccounts);
		setNonConfigurationInstance(dialog);
		dialog.show(this);
	}

	/**
	 * Ask the user for the incoming/outgoing server passwords.
	 */
	private static class PasswordPromptDialog implements
			NonConfigurationInstance, TextWatcher {
		private AlertDialog mDialog;
		private EditText mIncomingPasswordView;
		private EditText mOutgoingPasswordView;
		private CheckBox mUseIncomingView;

		private Account mAccount;
		private List<Account> mRemainingAccounts;
		private String mIncomingPassword;
		private String mOutgoingPassword;
		private boolean mUseIncoming;

		/**
		 * Constructor
		 * 
		 * @param account
		 *            The {@link Account} to ask the server passwords for. Never
		 *            {@code null}.
		 * @param accounts
		 *            The (possibly empty) list of remaining accounts to ask
		 *            passwords for. Never {@code null}.
		 */
		PasswordPromptDialog(Account account, List<Account> accounts) {
			mAccount = account;
			mRemainingAccounts = accounts;
		}

		@Override
		public void restore(Activity activity) {
			show((Accounts) activity, true);
		}

		@Override
		public boolean retain() {
			if (mDialog != null) {
				// Retain entered passwords and checkbox state
				mIncomingPassword = mIncomingPasswordView.getText().toString();
				if (mOutgoingPasswordView != null) {
					mOutgoingPassword = mOutgoingPasswordView.getText()
							.toString();
					mUseIncoming = mUseIncomingView.isChecked();
				}

				// Dismiss dialog
				mDialog.dismiss();

				// Clear all references to UI objects
				mDialog = null;
				mIncomingPasswordView = null;
				mOutgoingPasswordView = null;
				mUseIncomingView = null;
				return true;
			}
			return false;
		}

		public void show(MessageList messageList) {
			show(messageList);
		}

		private void show(final Accounts activity, boolean restore) {
			ServerSettings incoming = Store.decodeStoreUri(mAccount
					.getStoreUri());
			ServerSettings outgoing = Transport.decodeTransportUri(mAccount
					.getTransportUri());

			// Don't ask for the password to the outgoing server for WebDAV
			// accounts, because
			// incoming and outgoing servers are identical for this account
			// type.
			boolean configureOutgoingServer = !WebDavStore.STORE_TYPE
					.equals(outgoing.type);

			// Create a ScrollView that will be used as container for the whole
			// layout
			final ScrollView scrollView = new ScrollView(activity);

			// Create the dialog
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					activity);
			builder.setTitle(activity
					.getString(R.string.settings_import_activate_account_header));
			builder.setView(scrollView);
			builder.setPositiveButton(activity.getString(R.string.okay_action),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String incomingPassword = mIncomingPasswordView
									.getText().toString();
							String outgoingPassword = null;
							if (mOutgoingPasswordView != null) {
								outgoingPassword = (mUseIncomingView
										.isChecked()) ? incomingPassword
										: mOutgoingPasswordView.getText()
												.toString();
							}

							dialog.dismiss();

							// Set the server passwords in the background
							SetPasswordsAsyncTask asyncTask = new SetPasswordsAsyncTask(
									activity, mAccount, incomingPassword,
									outgoingPassword, mRemainingAccounts);
							activity.setNonConfigurationInstance(asyncTask);
							asyncTask.execute();
						}
					});
			builder.setNegativeButton(
					activity.getString(R.string.cancel_action),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							activity.setNonConfigurationInstance(null);
						}
					});
			mDialog = builder.create();

			// Use the dialog's layout inflater so its theme is used (and not
			// the activity's theme).
			View layout = mDialog.getLayoutInflater().inflate(
					R.layout.accounts_password_prompt, null);

			// Set the intro text that tells the user what to do
			TextView intro = (TextView) layout
					.findViewById(R.id.password_prompt_intro);
			String serverPasswords = activity.getResources().getQuantityString(
					R.plurals.settings_import_server_passwords,
					(configureOutgoingServer) ? 2 : 1);
			intro.setText(activity.getString(
					R.string.settings_import_activate_account_intro,
					mAccount.getDescription(), serverPasswords));

			// Display the hostname of the incoming server
			TextView incomingText = (TextView) layout
					.findViewById(R.id.password_prompt_incoming_server);
			incomingText.setText(activity.getString(
					R.string.settings_import_incoming_server, incoming.host));

			mIncomingPasswordView = (EditText) layout
					.findViewById(R.id.incoming_server_password);
			mIncomingPasswordView.addTextChangedListener(this);

			if (configureOutgoingServer) {
				// Display the hostname of the outgoing server
				TextView outgoingText = (TextView) layout
						.findViewById(R.id.password_prompt_outgoing_server);
				outgoingText.setText(activity
						.getString(R.string.settings_import_outgoing_server,
								outgoing.host));

				mOutgoingPasswordView = (EditText) layout
						.findViewById(R.id.outgoing_server_password);
				mOutgoingPasswordView.addTextChangedListener(this);

				mUseIncomingView = (CheckBox) layout
						.findViewById(R.id.use_incoming_server_password);
				mUseIncomingView.setChecked(true);
				mUseIncomingView
						.setOnCheckedChangeListener(new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								if (isChecked) {
									mOutgoingPasswordView.setText(null);
									mOutgoingPasswordView.setEnabled(false);
								} else {
									mOutgoingPasswordView
											.setText(mIncomingPasswordView
													.getText());
									mOutgoingPasswordView.setEnabled(true);
								}
							}
						});
			} else {
				layout.findViewById(R.id.outgoing_server_prompt).setVisibility(
						View.GONE);
			}

			// Add the layout to the ScrollView
			scrollView.addView(layout);

			// Show the dialog
			mDialog.show();

			// Restore the contents of the password boxes and the checkbox (if
			// the dialog was
			// retained during a configuration change).
			if (restore) {
				mIncomingPasswordView.setText(mIncomingPassword);
				if (configureOutgoingServer) {
					mOutgoingPasswordView.setText(mOutgoingPassword);
					mUseIncomingView.setChecked(mUseIncoming);
				}
			} else {
				// Trigger afterTextChanged() being called
				// Work around this bug:
				// https://code.google.com/p/android/issues/detail?id=6360
				mIncomingPasswordView.setText(mIncomingPasswordView.getText());
			}
		}

		@Override
		public void afterTextChanged(Editable arg0) {
			boolean enable = false;
			// Is the password box for the incoming server password empty?
			if (mIncomingPasswordView.getText().length() > 0) {
				// Do we need to check the outgoing server password box?
				if (mOutgoingPasswordView == null) {
					enable = true;
				}
				// If the checkbox to use the incoming server password is
				// checked we need to make
				// sure that the password box for the outgoing server isn't
				// empty.
				else if (mUseIncomingView.isChecked()
						|| mOutgoingPasswordView.getText().length() > 0) {
					enable = true;
				}
			}

			// Disable "OK" button if the user hasn't specified all necessary
			// passwords.
			mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
					enable);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// Not used
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// Not used
		}
	}

	/**
	 * Set the incoming/outgoing server password in the background.
	 */
	private static class SetPasswordsAsyncTask extends
			ExtendedAsyncTask<Void, Void, Void> {
		private Account mAccount;
		private String mIncomingPassword;
		private String mOutgoingPassword;
		private List<Account> mRemainingAccounts;
		private Application mApplication;

		protected SetPasswordsAsyncTask(Activity activity, Account account,
				String incomingPassword, String outgoingPassword,
				List<Account> remainingAccounts) {
			super(activity);
			mAccount = account;
			mIncomingPassword = incomingPassword;
			mOutgoingPassword = outgoingPassword;
			mRemainingAccounts = remainingAccounts;
			mApplication = mActivity.getApplication();
		}

		@Override
		protected void showProgressDialog() {
			String title = mActivity
					.getString(R.string.settings_import_activate_account_header);
			int passwordCount = (mOutgoingPassword == null) ? 1 : 2;
			String message = mActivity.getResources().getQuantityString(
					R.plurals.settings_import_setting_passwords, passwordCount);
			mProgressDialog = ProgressDialog.show(mActivity, title, message,
					true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				// Set incoming server password
				String storeUri = mAccount.getStoreUri();
				ServerSettings incoming = Store.decodeStoreUri(storeUri);
				ServerSettings newIncoming = incoming
						.newPassword(mIncomingPassword);
				String newStoreUri = Store.createStoreUri(newIncoming);
				mAccount.setStoreUri(newStoreUri);

				if (mOutgoingPassword != null) {
					// Set outgoing server password
					String transportUri = mAccount.getTransportUri();
					ServerSettings outgoing = Transport
							.decodeTransportUri(transportUri);
					ServerSettings newOutgoing = outgoing
							.newPassword(mOutgoingPassword);
					String newTransportUri = Transport
							.createTransportUri(newOutgoing);
					mAccount.setTransportUri(newTransportUri);
				}

				// Mark account as enabled
				mAccount.setEnabled(true);

				// Save the account settings
				mAccount.save(Preferences.getPreferences(mContext));

				// Start services if necessary
				K9.setServicesEnabled(mContext);

				// Get list of folders from remote server
				MessagingController.getInstance(mApplication).listFolders(
						mAccount, true, null);
			} catch (Exception e) {
				Log.e(K9.LOG_TAG,
						"Something went while setting account passwords", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Accounts activity = (Accounts) mActivity;

			// Let the activity know that the background task is complete
			activity.setNonConfigurationInstance(null);

			activity.refresh();
			removeProgressDialog();

			if (mRemainingAccounts.size() > 0) {
				activity.promptForServerPasswords(mRemainingAccounts);
			}
		}
	}

	void onMove(final Account account, final boolean up) {
		MoveAccountAsyncTask asyncTask = new MoveAccountAsyncTask(this,
				account, up);
		setNonConfigurationInstance(asyncTask);
		asyncTask.execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		if (position < mAdapter_Accounts.getCount() + 1) {

			BaseAccount account = (BaseAccount) parent
					.getItemAtPosition(position);
			onOpenAccount(account);

		}
	}

	private static String[][] USED_LIBRARIES = new String[][] {
			new String[] { "jutf7", "http://jutf7.sourceforge.net/" },
			new String[] { "JZlib", "http://www.jcraft.com/jzlib/" },
			new String[] { "Commons IO", "http://commons.apache.org/io/" },
			new String[] { "Mime4j", "http://james.apache.org/mime4j/" },
			new String[] { "HtmlCleaner", "http://htmlcleaner.sourceforge.net/" },
			new String[] { "Android-PullToRefresh",
					"https://github.com/chrisbanes/Android-PullToRefresh" },
			new String[] { "ckChangeLog",
					"https://github.com/cketti/ckChangeLog" },
			new String[] { "HoloColorPicker",
					"https://github.com/LarsWerkman/HoloColorPicker" } };

	private void onAbout() {
		String appName = getString(R.string.app_name);
		int year = Calendar.getInstance().get(Calendar.YEAR);
		WebView wv = new WebView(this);
		StringBuilder html = new StringBuilder()
				.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />")
				.append("<img src=\"file:///android_asset/icon.png\" alt=\"")
				.append(appName)
				.append("\"/>")
				.append("<h1>")
				.append(String.format(getString(R.string.about_title_fmt),
						"<a href=\"" + getString(R.string.app_webpage_url))
						+ "\">")
				.append(appName)
				.append("</a>")
				.append("</h1><p>")
				.append(appName)
				.append(" ")
				.append(String.format(getString(R.string.debug_version_fmt),
						getVersionNumber()))
				.append("</p><p>")
				.append(String.format(getString(R.string.app_authors_fmt),
						getString(R.string.app_authors)))
				.append("</p><p>")
				.append(String.format(getString(R.string.app_revision_fmt),
						"<a href=\"" + getString(R.string.app_revision_url)
								+ "\">" + getString(R.string.app_revision_url)
								+ "</a>"))
				.append("</p><hr/><p>")
				.append(String.format(getString(R.string.app_copyright_fmt),
						year, year)).append("</p><hr/><p>")
				.append(getString(R.string.app_license)).append("</p><hr/><p>");

		StringBuilder libs = new StringBuilder().append("<ul>");
		for (String[] library : USED_LIBRARIES) {
			libs.append("<li><a href=\"").append(library[1]).append("\">")
					.append(library[0]).append("</a></li>");
		}
		libs.append("</ul>");

		html.append(
				String.format(getString(R.string.app_libraries),
						libs.toString()))
				.append("</p><hr/><p>")
				.append(String
						.format(getString(R.string.app_emoji_icons),
								"<div>TypePad \u7d75\u6587\u5b57\u30a2\u30a4\u30b3\u30f3\u753b\u50cf "
										+ "(<a href=\"http://typepad.jp/\">Six Apart Ltd</a>) / "
										+ "<a href=\"http://creativecommons.org/licenses/by/2.1/jp/\">CC BY 2.1</a></div>"))
				.append("</p><hr/><p>")
				.append(getString(R.string.app_htmlcleaner_license));

		wv.loadDataWithBaseURL("file:///android_res/drawable/",
				html.toString(), "text/html", "utf-8", null);
		new AlertDialog.Builder(this)
				.setView(wv)
				.setCancelable(true)
				.setPositiveButton(R.string.okay_action,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface d, int c) {
								d.dismiss();
							}
						})
				.setNeutralButton(R.string.changelog_full_title,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface d, int c) {
								new ChangeLog(MessageList.this)
										.getFullLogDialog().show();
							}
						}).show();
	}

	/**
	 * Get current version number.
	 * 
	 * @return String version
	 */
	private String getVersionNumber() {
		String version = "?";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			// Log.e(TAG, "Package name not found", e);
		}
		return version;
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		return true;
	}

	void onImport() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");

		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> infos = packageManager.queryIntentActivities(i, 0);

		if (infos.size() > 0) {
			startActivityForResult(Intent.createChooser(i, null),
					ACTIVITY_REQUEST_PICK_SETTINGS_FILE);
		} else {
			showDialog(DIALOG_NO_FILE_MANAGER);
		}
	}

	private void onImport(Uri uri) {
		ListImportContentsAsyncTask asyncTask = new ListImportContentsAsyncTask(
				this, uri);
		setNonConfigurationInstance(asyncTask);
		asyncTask.execute();
	}

	void showSimpleDialog(int headerRes, int messageRes, Object... args) {
		SimpleDialog dialog = new SimpleDialog(headerRes, messageRes, args);
		dialog.show(this);
		setNonConfigurationInstance(dialog);
	}

	/**
	 * A simple dialog.
	 */
	private static class SimpleDialog implements NonConfigurationInstance {
		private final int mHeaderRes;
		private final int mMessageRes;
		private Object[] mArguments;
		private Dialog mDialog;

		SimpleDialog(int headerRes, int messageRes, Object... args) {
			this.mHeaderRes = headerRes;
			this.mMessageRes = messageRes;
			this.mArguments = args;
		}

		@Override
		public void restore(Activity activity) {
			show((MessageList) activity);
		}

		@Override
		public boolean retain() {
			if (mDialog != null) {
				mDialog.dismiss();
				mDialog = null;
				return true;
			}
			return false;
		}

		public void show(final MessageList messageList) {
			final String message = generateMessage(messageList);

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					messageList);
			builder.setTitle(mHeaderRes);
			builder.setMessage(message);
			builder.setPositiveButton(R.string.okay_action,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							messageList.setNonConfigurationInstance(null);
							okayAction(messageList);
						}
					});
			mDialog = builder.show();
		}

		/**
		 * Returns the message the dialog should display.
		 * 
		 * @param messageList
		 *            The {@code Activity} this dialog belongs to.
		 * 
		 * @return The message the dialog should display
		 */
		protected String generateMessage(MessageList messageList) {
			return messageList.getString(mMessageRes, mArguments);
		}

		/**
		 * This method is called after the "OK" button was pressed.
		 * 
		 * @param messageList
		 *            The {@code Activity} this dialog belongs to.
		 */
		protected void okayAction(MessageList messageList) {
			// Do nothing
		}
	}

	/**
	 * Shows a dialog that displays how many accounts were successfully
	 * imported.
	 * 
	 * @param importResults
	 *            The {@link ImportResults} instance returned by the
	 *            {@link SettingsImporter}.
	 * @param filename
	 *            The name of the settings file that was imported.
	 */
	void showAccountsImportedDialog(ImportResults importResults, String filename) {
		AccountsImportedDialog dialog = new AccountsImportedDialog(
				importResults, filename);
		dialog.show(MessageList.this);
		setNonConfigurationInstance(dialog);
	}

	/**
	 * A dialog that displays how many accounts were successfully imported.
	 */
	private static class AccountsImportedDialog extends SimpleDialog {
		private ImportResults mImportResults;
		private String mFilename;

		AccountsImportedDialog(ImportResults importResults, String filename) {
			super(R.string.settings_import_success_header,
					R.string.settings_import_success);
			mImportResults = importResults;
			mFilename = filename;
		}

		@Override
		protected String generateMessage(MessageList activity) {
			// TODO: display names of imported accounts (name from file *and*
			// possibly new name)

			int imported = mImportResults.importedAccounts.size();
			String accounts = activity.getResources().getQuantityString(
					R.plurals.settings_import_accounts, imported, imported);
			return activity.getString(R.string.settings_import_success,
					accounts, mFilename);
		}

		@Override
		protected void okayAction(MessageList activity) {
			Context context = activity.getApplicationContext();
			Preferences preferences = Preferences.getPreferences(context);
			List<Account> disabledAccounts = new ArrayList<Account>();
			for (AccountDescriptionPair accountPair : mImportResults.importedAccounts) {
				Account account = preferences
						.getAccount(accountPair.imported.uuid);
				if (account != null && !account.isEnabled()) {
					disabledAccounts.add(account);
				}
			}
			if (disabledAccounts.size() > 0) {
				activity.promptForServerPasswords(disabledAccounts);
			} else {
				activity.setNonConfigurationInstance(null);
			}
		}
	}

	/**
	 * Display a dialog that lets the user select which accounts to import from
	 * the settings file.
	 * 
	 * @param importContents
	 *            The {@link ImportContents} instance returned by
	 *            {@link SettingsImporter#getImportStreamContents(InputStream)}
	 * @param uri
	 *            The (content) URI of the settings file.
	 */
	void showImportSelectionDialog(ImportContents importContents, Uri uri) {
		ImportSelectionDialog dialog = new ImportSelectionDialog(
				importContents, uri);
		dialog.show(this);
		setNonConfigurationInstance(dialog);
	}

	/**
	 * A dialog that lets the user select which accounts to import from the
	 * settings file.
	 */
	private static class ImportSelectionDialog implements
			NonConfigurationInstance {
		private ImportContents mImportContents;
		private Uri mUri;
		private AlertDialog mDialog;

		private SparseBooleanArray mSelection;

		ImportSelectionDialog(ImportContents importContents, Uri uri) {
			mImportContents = importContents;
			mUri = uri;
		}

		@Override
		public void restore(Activity activity) {
			show((MessageList) activity, mSelection);
		}

		@Override
		public boolean retain() {

			if (mDialog != null) {
				// Save the selection state of each list item

				mSelection = mDialog.getListView().getCheckedItemPositions();

				mDialog.dismiss();
				mDialog = null;
				return true;
			}
			return false;
		}

		public void show(MessageList messageList) {
			show(messageList, null);
		}

		public void show(final MessageList messageList,
				SparseBooleanArray selection) {
			List<String> contents = new ArrayList<String>();

			if (mImportContents.globalSettings) {
				contents.add(messageList
						.getString(R.string.settings_import_global_settings));
			}

			for (AccountDescription account : mImportContents.accounts) {
				contents.add(account.name);
			}

			int count = contents.size();
			boolean[] checkedItems = new boolean[count];
			if (selection != null) {
				for (int i = 0; i < count; i++) {
					checkedItems[i] = selection.get(i);
				}
			} else {
				for (int i = 0; i < count; i++) {
					checkedItems[i] = true;
				}
			}

			// TODO: listview header:
			// "Please select the settings you wish to import"
			// TODO: listview footer: "Select all" / "Select none" buttons?
			// TODO: listview footer: "Overwrite existing accounts?" checkbox

			OnMultiChoiceClickListener listener = new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which,
						boolean isChecked) {
					((AlertDialog) dialog).getListView().setItemChecked(which,
							isChecked);
				}
			};

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					messageList);
			builder.setMultiChoiceItems(contents.toArray(new String[0]),
					checkedItems, listener);
			builder.setTitle(messageList
					.getString(R.string.settings_import_selection));
			builder.setInverseBackgroundForced(true);
			builder.setPositiveButton(R.string.okay_action,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							ListView listView = ((AlertDialog) dialog)
									.getListView();
							SparseBooleanArray pos = listView
									.getCheckedItemPositions();

							boolean includeGlobals = mImportContents.globalSettings ? pos
									.get(0) : false;
							List<String> accountUuids = new ArrayList<String>();
							int start = mImportContents.globalSettings ? 1 : 0;
							for (int i = start, end = listView.getCount(); i < end; i++) {
								if (pos.get(i)) {
									accountUuids.add(mImportContents.accounts
											.get(i - start).uuid);
								}
							}

							/*
							 * TODO: Think some more about this. Overwriting
							 * could change the store type. This requires some
							 * additional code in order to work smoothly while
							 * the app is running.
							 */
							boolean overwrite = false;

							dialog.dismiss();
							messageList.setNonConfigurationInstance(null);

							ImportAsyncTask importAsyncTask = new ImportAsyncTask(
									messageList, includeGlobals, accountUuids,
									overwrite, mUri);
							messageList
									.setNonConfigurationInstance(importAsyncTask);
							importAsyncTask.execute();
						}
					});
			builder.setNegativeButton(R.string.cancel_action,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							messageList.setNonConfigurationInstance(null);
						}
					});
			mDialog = builder.show();
		}
	}

	/**
	 * Set the {@code NonConfigurationInstance} this activity should retain on
	 * configuration changes.
	 * 
	 * @param inst
	 *            The {@link NonConfigurationInstance} that should be retained
	 *            when {@link Accounts#onRetainNonConfigurationInstance()} is
	 *            called.
	 */
	void setNonConfigurationInstance(NonConfigurationInstance inst) {
		mNonConfigurationInstance = inst;
	}

	public class AccountsAdapter1 extends ArrayAdapter<BaseAccount> {
		public AccountsAdapter1(BaseAccount[] accounts) {
			super(MessageList.this, 0, accounts);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final BaseAccount account = getItem(position);
			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = getLayoutInflater().inflate(R.layout.accounts_item,
						parent, false);
			}
			AccountViewHolder holder = (AccountViewHolder) view.getTag();
			if (holder == null) {
				holder = new AccountViewHolder();
				holder.description = (TextView) view
						.findViewById(R.id.description);
				holder.email = (TextView) view.findViewById(R.id.email);
				holder.newMessageCount = (TextView) view
						.findViewById(R.id.new_message_count);
				holder.flaggedMessageCount = (TextView) view
						.findViewById(R.id.flagged_message_count);
				holder.newMessageCountWrapper = view
						.findViewById(R.id.new_message_count_wrapper);
				holder.flaggedMessageCountWrapper = view
						.findViewById(R.id.flagged_message_count_wrapper);
				holder.newMessageCountIcon = view
						.findViewById(R.id.new_message_count_icon);
				holder.flaggedMessageCountIcon = view
						.findViewById(R.id.flagged_message_count_icon);
				holder.activeIcons = (RelativeLayout) view
						.findViewById(R.id.active_icons);
				holder.chip = view.findViewById(R.id.chip);
				holder.folders = (ImageButton) view.findViewById(R.id.folders);
				holder.accountsItemLayout = (LinearLayout) view
						.findViewById(R.id.accounts_item_layout);

				view.setTag(holder);
			}
			AccountStats stats = accountStats.get(account.getUuid());

			if (stats != null && account instanceof Account && stats.size >= 0) {
				holder.email.setText(SizeFormatter.formatSize(MessageList.this,
						stats.size));
				holder.email.setVisibility(View.VISIBLE);
			} else {
				if (account.getEmail().equals(account.getDescription())) {
					holder.email.setVisibility(View.GONE);
				} else {
					holder.email.setVisibility(View.VISIBLE);
					holder.email.setText(account.getEmail());
				}
			}

			String description = account.getDescription();
			if (description == null || description.isEmpty()) {
				description = account.getEmail();
			}

			holder.description.setText(description);

			Integer unreadMessageCount = null;
			if (stats != null) {
				unreadMessageCount = stats.unreadMessageCount;
				holder.newMessageCount.setText(Integer
						.toString(unreadMessageCount));
				holder.newMessageCountWrapper
						.setVisibility(unreadMessageCount > 0 ? View.VISIBLE
								: View.GONE);

				holder.flaggedMessageCount.setText(Integer
						.toString(stats.flaggedMessageCount));
				holder.flaggedMessageCountWrapper
						.setVisibility(K9.messageListStars()
								&& stats.flaggedMessageCount > 0 ? View.VISIBLE
								: View.GONE);

				holder.flaggedMessageCountWrapper
						.setOnClickListener(createFlaggedSearchListener(account));
				holder.newMessageCountWrapper
						.setOnClickListener(createUnreadSearchListener(account));

				holder.activeIcons.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast toast = Toast.makeText(getApplication(),
								getString(R.string.tap_hint),
								Toast.LENGTH_SHORT);
						toast.show();
					}
				});

			} else {
				holder.newMessageCountWrapper.setVisibility(View.GONE);
				holder.flaggedMessageCountWrapper.setVisibility(View.GONE);
			}
			if (account instanceof Account) {
				Account realAccount = (Account) account;

			} else {

			}

			mFontSizes.setViewTextSize(holder.description,
					mFontSizes.getAccountName());
			mFontSizes.setViewTextSize(holder.email,
					mFontSizes.getAccountDescription());

			if (account instanceof SearchAccount) {
				holder.folders.setVisibility(View.GONE);
			} else {
				holder.folders.setVisibility(View.VISIBLE);
				holder.folders.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						Log.d("clicked_folder1", "clicked");

						FolderList.actionHandleAccount(MessageList.this,
								(Account) account);

					}
				});
			}

			if (account instanceof Account) {
				Account realAccount = (Account) account;
				holder.chip.setBackgroundColor(realAccount.getChipColor());
			} else {
				holder.chip.setBackgroundColor(0xff999999);
			}

			holder.chip.getBackground().setAlpha(255);

			return view;
		}

		private OnClickListener createFlaggedSearchListener(BaseAccount account) {
			String searchTitle = getString(R.string.search_title,
					account.getDescription(),
					getString(R.string.flagged_modifier));

			Log.d("clicked_folder2", "clicked");

			LocalSearch search;
			if (account instanceof SearchAccount) {
				search = ((SearchAccount) account).getRelatedSearch().clone();
				search.setName(searchTitle);
			} else {
				search = new LocalSearch(searchTitle);
				search.addAccountUuid(account.getUuid());

				Account realAccount = (Account) account;
				realAccount.excludeSpecialFolders(search);
				realAccount.limitToDisplayableFolders(search);
			}

			search.and(Searchfield.FLAGGED, "1", Attribute.EQUALS);

			return new AccountClickListener(search);
		}

		private OnClickListener createUnreadSearchListener(BaseAccount account) {
			LocalSearch search = Accounts.createUnreadSearch(MessageList.this,
					account);
			return new AccountClickListener(search);
		}

		class AccountViewHolder {
			public TextView description;
			public TextView email;
			public TextView newMessageCount;
			public TextView flaggedMessageCount;
			public View newMessageCountIcon;
			public View flaggedMessageCountIcon;
			public View newMessageCountWrapper;
			public View flaggedMessageCountWrapper;
			public View chip;
			public RelativeLayout activeIcons;

			public ImageButton folders;
			public LinearLayout accountsItemLayout;
		}
	}

	/**
	 * Handles exporting of global settings and/or accounts in a background
	 * thread.
	 */
	private static class ExportAsyncTask extends
			ExtendedAsyncTask<Void, Void, Boolean> {
		private boolean mIncludeGlobals;
		private Set<String> mAccountUuids;
		private String mFileName;

		private ExportAsyncTask(Activity accountClickListener,
				boolean includeGlobals, Set<String> accountUuids) {
			super(accountClickListener);
			mIncludeGlobals = includeGlobals;
			mAccountUuids = accountUuids;
		}

		@Override
		protected void showProgressDialog() {
			String title = mContext
					.getString(R.string.settings_export_dialog_title);
			String message = mContext.getString(R.string.settings_exporting);
			mProgressDialog = ProgressDialog.show(mActivity, title, message,
					true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				mFileName = SettingsExporter.exportToFile(mContext,
						mIncludeGlobals, mAccountUuids);
			} catch (SettingsImportExportException e) {
				Log.w(K9.LOG_TAG, "Exception during export", e);
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			MessageList activity = (MessageList) mActivity;

			// Let the activity know that the background task is complete
			activity.setNonConfigurationInstance(null);

			removeProgressDialog();

			if (success) {
				activity.showSimpleDialog(
						R.string.settings_export_success_header,
						R.string.settings_export_success, mFileName);
			} else {
				// TODO: better error messages
				activity.showSimpleDialog(
						R.string.settings_export_failed_header,
						R.string.settings_export_failure);
			}
		}
	}

	/**
	 * Handles importing of global settings and/or accounts in a background
	 * thread.
	 */
	private static class ImportAsyncTask extends
			ExtendedAsyncTask<Void, Void, Boolean> {
		private boolean mIncludeGlobals;
		private List<String> mAccountUuids;
		private boolean mOverwrite;
		private Uri mUri;
		private ImportResults mImportResults;

		private ImportAsyncTask(MessageList messageList,
				boolean includeGlobals, List<String> accountUuids,
				boolean overwrite, Uri uri) {
			super(messageList);
			mIncludeGlobals = includeGlobals;
			mAccountUuids = accountUuids;
			mOverwrite = overwrite;
			mUri = uri;
		}

		@Override
		protected void showProgressDialog() {
			String title = mContext
					.getString(R.string.settings_import_dialog_title);
			String message = mContext.getString(R.string.settings_importing);
			mProgressDialog = ProgressDialog.show(mActivity, title, message,
					true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				InputStream is = mContext.getContentResolver().openInputStream(
						mUri);
				try {
					mImportResults = SettingsImporter.importSettings(mContext,
							is, mIncludeGlobals, mAccountUuids, mOverwrite);
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						/* Ignore */
					}
				}
			} catch (SettingsImportExportException e) {
				Log.w(K9.LOG_TAG, "Exception during import", e);
				return false;
			} catch (FileNotFoundException e) {
				Log.w(K9.LOG_TAG, "Couldn't open import file", e);
				return false;
			} catch (Exception e) {
				Log.w(K9.LOG_TAG, "Unknown error", e);
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			MessageList activity = (MessageList) mActivity;

			// Let the activity know that the background task is complete
			activity.setNonConfigurationInstance(null);

			removeProgressDialog();

			String filename = mUri.getLastPathSegment();
			boolean globalSettings = mImportResults.globalSettings;
			int imported = mImportResults.importedAccounts.size();
			if (success && (globalSettings || imported > 0)) {
				if (imported == 0) {
					activity.showSimpleDialog(
							R.string.settings_import_success_header,
							R.string.settings_import_global_settings_success,
							filename);
				} else {
					activity.showAccountsImportedDialog(mImportResults,
							filename);
				}

				activity.refresh();
			} else {
				// TODO: better error messages
				activity.showSimpleDialog(
						R.string.settings_import_failed_header,
						R.string.settings_import_failure, filename);
			}
		}
	}

	static class ListImportContentsAsyncTask extends
			ExtendedAsyncTask<Void, Void, Boolean> {
		private Uri mUri;
		private ImportContents mImportContents;

		private ListImportContentsAsyncTask(MessageList messageList, Uri uri) {
			super(messageList);

			mUri = uri;
		}

		@Override
		protected void showProgressDialog() {
			String title = mContext
					.getString(R.string.settings_import_dialog_title);
			String message = mContext
					.getString(R.string.settings_import_scanning_file);
			mProgressDialog = ProgressDialog.show(mActivity, title, message,
					true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				ContentResolver resolver = mContext.getContentResolver();
				InputStream is = resolver.openInputStream(mUri);
				try {
					mImportContents = SettingsImporter
							.getImportStreamContents(is);
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						/* Ignore */
					}
				}
			} catch (SettingsImportExportException e) {
				Log.w(K9.LOG_TAG, "Exception during export", e);
				return false;
			} catch (FileNotFoundException e) {
				Log.w(K9.LOG_TAG, "Couldn't read content from URI " + mUri);
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			MessageList activity = (MessageList) mActivity;

			// Let the activity know that the background task is complete
			activity.setNonConfigurationInstance(null);

			removeProgressDialog();

			if (success) {
				activity.showImportSelectionDialog(mImportContents, mUri);
			} else {
				String filename = mUri.getLastPathSegment();
				// TODO: better error messages
				activity.showSimpleDialog(
						R.string.settings_import_failed_header,
						R.string.settings_import_failure, filename);
			}
		}
	}

	private static class MoveAccountAsyncTask extends
			ExtendedAsyncTask<Void, Void, Void> {
		private Account mAccount;
		private boolean mUp;

		protected MoveAccountAsyncTask(Activity activity, Account account,
				boolean up) {
			super(activity);
			mAccount = account;
			mUp = up;
		}

		@Override
		protected void showProgressDialog() {
			String message = mActivity
					.getString(R.string.manage_accounts_moving_message);
			mProgressDialog = ProgressDialog.show(mActivity, null, message,
					true);
		}

		@Override
		protected Void doInBackground(Void... args) {
			mAccount.move(Preferences.getPreferences(mContext), mUp);
			return null;
		}

		@Override
		protected void onPostExecute(Void arg) {
			MessageList activity = (MessageList) mActivity;

			// Let the activity know that the background task is complete
			activity.setNonConfigurationInstance(null);

			activity.refresh();
			removeProgressDialog();
		}
	}

	private void createSpecialAccounts() {
		mUnifiedInboxAccount = SearchAccount.createUnifiedInboxAccount(this);
		mAllMessagesAccount = SearchAccount.createAllMessagesAccount(this);
	}

	void refresh() {
		accounts = Preferences.getPreferences(this).getAccounts();

		// see if we should show the welcome message
		// if (accounts.length < 1) {
		// WelcomeMessage.showWelcomeMessage(this);
		// finish();
		// }

		List<BaseAccount> newAccounts;
		if (!K9.isHideSpecialAccounts() && accounts.length > 0) {
			if (mUnifiedInboxAccount == null || mAllMessagesAccount == null) {
				createSpecialAccounts();
			}

			newAccounts = new ArrayList<BaseAccount>(accounts.length
					+ SPECIAL_ACCOUNTS_COUNT);
			newAccounts.add(mUnifiedInboxAccount);
			newAccounts.add(mAllMessagesAccount);
		} else {
			newAccounts = new ArrayList<BaseAccount>(accounts.length);
		}

		newAccounts.addAll(Arrays.asList(accounts));

		mAdapter_Accounts = new AccountsAdapter1(
				newAccounts.toArray(EMPTY_BASE_ACCOUNT_ARRAY));

		getListView().setAdapter(mAdapter);
		if (!newAccounts.isEmpty()) {

		}
		pendingWork.clear();
		mHandler.refreshTitle();

		MessagingController controller = MessagingController
				.getInstance(getApplication());

		for (BaseAccount account : newAccounts) {
			pendingWork.put(account, "true");

			if (account instanceof Account) {
				Account realAccount = (Account) account;
				controller.getAccountStats(this, realAccount,
						mListener_Accounts);
			} else if (K9.countSearchMessages()
					&& account instanceof SearchAccount) {
				final SearchAccount searchAccount = (SearchAccount) account;
				controller.getSearchAccountStats(searchAccount,
						mListener_Accounts);
			}
		}
	}

}