package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.fragment.MessageViewFragment.MessageViewFragmentListener;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.store.StorageManager;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.MessageTitleView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


public class MessageView extends K9FragmentActivity implements MessageViewFragmentListener {

    private static final String EXTRA_MESSAGE_REFERENCE = "com.fsck.k9.MessageView_messageReference";
    private static final String EXTRA_MESSAGE_REFERENCES = "com.fsck.k9.MessageView_messageReferences";
    private static final String EXTRA_MESSAGE_LIST_EXTRAS = "com.fsck.k9.MessageView_messageListExtras";

    /**
     * @see #mLastDirection
     */
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;


    public static void actionView(Context context, MessageReference messRef,
            ArrayList<MessageReference> messReferences, Bundle messageListExtras) {
        Intent i = new Intent(context, MessageView.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(EXTRA_MESSAGE_LIST_EXTRAS, messageListExtras);
        i.putExtra(EXTRA_MESSAGE_REFERENCE, messRef);
        i.putParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES, messReferences);
        context.startActivity(i);
    }


    private StorageManager.StorageListener mStorageListener = new StorageListenerImplementation();
    private Account mAccount;
    private MessageTitleView mTitleView;
    private MessageReference mMessageReference;
    private ArrayList<MessageReference> mMessageReferences;
    private int mLastDirection = (K9.messageViewShowNext()) ? NEXT : PREVIOUS;
    private MessageReference mNextMessage;
    private MessageReference mPreviousMessage;
    private MessageViewFragment mMessageViewFragment;
    private Menu mMenu;

    /**
     * Screen width in pixels.
     *
     * <p>
     * Used to detect right-to-left bezel swipes.
     * </p>
     *
     * @see #onSwipeRightToLeft(MotionEvent, MotionEvent)
     */
    private int mScreenWidthInPixels;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(K9.getK9ThemeResourceId(K9.getK9MessageViewTheme()));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.message_view);

        initializeActionBar();
        setTitle("");

        mScreenWidthInPixels = getResources().getDisplayMetrics().widthPixels;

        // Enable gesture detection for MessageViews
        mGestureDetector = new GestureDetector(new MyGestureDetector(false));

        final Intent intent = getIntent();

        Uri uri = intent.getData();
        if (savedInstanceState != null) {
            mMessageReference = savedInstanceState.getParcelable(EXTRA_MESSAGE_REFERENCE);
            mMessageReferences = savedInstanceState.getParcelableArrayList(EXTRA_MESSAGE_REFERENCES);
        } else {
            if (uri == null) {
                mMessageReference = intent.getParcelableExtra(EXTRA_MESSAGE_REFERENCE);
                mMessageReferences = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);
            } else {
                List<String> segmentList = uri.getPathSegments();
                if (segmentList.size() != 3) {
                    //TODO: Use resource to externalize message
                    Toast.makeText(this, "Invalid intent uri: " + uri.toString(), Toast.LENGTH_LONG).show();
                    return;
                }

                String accountId = segmentList.get(0);
                Collection<Account> accounts = Preferences.getPreferences(this).getAvailableAccounts();
                for (Account account : accounts) {
                    if (String.valueOf(account.getAccountNumber()).equals(accountId)) {
                        mMessageReference = new MessageReference();
                        mMessageReference.accountUuid = account.getUuid();
                        mMessageReference.folderName = segmentList.get(1);
                        mMessageReference.uid = segmentList.get(2);
                        mMessageReferences = new ArrayList<MessageReference>();
                        mAccount = account;
                        break;
                    }
                }

                if (mMessageReference == null) {
                    //TODO: Use resource to externalize message
                    Toast.makeText(this, "Invalid account id: " + accountId, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        if (mAccount == null) {
            Preferences preferences = Preferences.getPreferences(getApplicationContext());
            mAccount = preferences.getAccount(mMessageReference.accountUuid);
        }

        findSurroundingMessagesUid();


        FragmentManager fragmentManager = getSupportFragmentManager();

        mMessageViewFragment = (MessageViewFragment) fragmentManager.findFragmentById(R.id.message);

        if (mMessageViewFragment == null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mMessageViewFragment = MessageViewFragment.newInstance(mMessageReference);
            ft.add(R.id.message, mMessageViewFragment);
            ft.commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mAccount.isAvailable(this)) {
            onAccountUnavailable();
            return;
        }
        StorageManager.getInstance(getApplication()).addListener(mStorageListener);
    }

    @Override
    protected void onPause() {
        StorageManager.getInstance(getApplication()).removeListener(mStorageListener);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_MESSAGE_REFERENCE, mMessageReference);
        outState.putParcelableArrayList(EXTRA_MESSAGE_REFERENCES, mMessageReferences);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getSupportMenuInflater().inflate(R.menu.message_view_option, menu);
        mMenu = menu;
        configureMenu(menu);

        return true;
    }

    private void configureMenu(Menu menu) {
        if (menu == null) {
            return;
        }

        if (mNextMessage != null) {
            menu.findItem(R.id.next_message).setEnabled(true);
            menu.findItem(R.id.next_message).getIcon().setAlpha(255);
        } else {
            menu.findItem(R.id.next_message).getIcon().setAlpha(127);
            menu.findItem(R.id.next_message).setEnabled(false);
        }

        if (mPreviousMessage != null) {
            menu.findItem(R.id.previous_message).setEnabled(true);
            menu.findItem(R.id.previous_message).getIcon().setAlpha(255);
        } else {
            menu.findItem(R.id.previous_message).getIcon().setAlpha(127);
            menu.findItem(R.id.previous_message).setEnabled(false);
        }
    }

    private void toggleActionsState(Menu menu, boolean state) {
        for (int i = 0; i < menu.size(); ++i) {
            menu.getItem(i).setEnabled(state);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.next_message: {
                onNext();
                break;
            }
            case R.id.previous_message: {
                onPrevious();
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }

        return true;
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

    /**
     * Handle hotkeys
     *
     * <p>
     * This method is called by {@link #dispatchKeyEvent(KeyEvent)} before any view had the chance
     * to consume this key event.
     * </p>
     *
     * @param keyCode
     *         The value in {@code event.getKeyCode()}.
     * @param event
     *         Description of the key event.
     *
     * @return {@code true} if this event was consumed.
     */
    public boolean onCustomKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP: {
            if (K9.useVolumeKeysForNavigationEnabled()) {
                onNext();
                return true;
            }
            break;
        }
        case KeyEvent.KEYCODE_VOLUME_DOWN: {
            if (K9.useVolumeKeysForNavigationEnabled()) {
                onPrevious();
                return true;
            }
            break;
        }
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_D: {
            mMessageViewFragment.onDelete();
            return true;
        }
        case KeyEvent.KEYCODE_F: {
            mMessageViewFragment.onForward();
            return true;
        }
        case KeyEvent.KEYCODE_A: {
            mMessageViewFragment.onReplyAll();
            return true;
        }
        case KeyEvent.KEYCODE_R: {
            mMessageViewFragment.onReply();
            return true;
        }
        case KeyEvent.KEYCODE_G: {
            mMessageViewFragment.onFlag();
            return true;
        }
        case KeyEvent.KEYCODE_M: {
            mMessageViewFragment.onMove();
            return true;
        }
        case KeyEvent.KEYCODE_S: {
            mMessageViewFragment.onRefile(mAccount.getSpamFolderName());
            return true;
        }
        case KeyEvent.KEYCODE_V: {
            mMessageViewFragment.onRefile(mAccount.getArchiveFolderName());
            return true;
        }
        case KeyEvent.KEYCODE_Y: {
            mMessageViewFragment.onCopy();
            return true;
        }
        case KeyEvent.KEYCODE_J:
        case KeyEvent.KEYCODE_P: {
            onPrevious();
            return true;
        }
        case KeyEvent.KEYCODE_N:
        case KeyEvent.KEYCODE_K: {
            onNext();
            return true;
        }
        case KeyEvent.KEYCODE_Z: {
            mMessageViewFragment.zoom(event);
            return true;
        }
        case KeyEvent.KEYCODE_H: {
            Toast toast = Toast.makeText(this, R.string.message_help_key, Toast.LENGTH_LONG);
            toast.show();
            return true;
        }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Swallow these events too to avoid the audible notification of a volume change
        if (K9.useVolumeKeysForNavigationEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                if (K9.DEBUG) {
                    Log.v(K9.LOG_TAG, "Swallowed key up.");
                }
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    protected void onAccountUnavailable() {
        finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);
    }

    private void initializeActionBar() {
        final ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.actionbar_message_view);

        final View customView = actionBar.getCustomView();
        mTitleView = (MessageTitleView) customView.findViewById(android.R.id.title);
    }

    @Override
    public void messageHeaderViewAvailable(MessageHeader header) {
        mTitleView.setMessageHeader(header);
    }

    /**
     * Set the title of the view.
     *
     * <p>Since we're using a custom ActionBar view, the normal {@code setTitle()} doesn't do what
     * we think. This version sets the text value into the proper ActionBar title view.</p>
     *
     * @param title
     *         Title to set.
     */
    @Override
    public void setTitle(CharSequence title) {
        mTitleView.setText(title);
    }

    @Override
    public void setProgress(boolean enable) {
        setSupportProgressBarIndeterminateVisibility(enable);
    }

    /**
     * Handle a right-to-left swipe starting at the edge of the screen as "move to next message."
     */
    @Override
    protected void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        if ((int) e1.getRawX() > mScreenWidthInPixels - BEZEL_SWIPE_THRESHOLD) {
            onNext();
        }
    }

    /**
     * Handle a left-to-right swipe starting at the edge of the screen as
     * "move to previous message."
     */
    @Override
    protected void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        if ((int) e1.getRawX() < BEZEL_SWIPE_THRESHOLD) {
            onPrevious();
        }
    }

    @Override
    public void showNextMessageOrReturn() {
        if (K9.messageViewReturnToList()) {
            finish();
        } else {
            showNextMessage();
        }
    }

    private void showNextMessage() {
        findSurroundingMessagesUid();
        mMessageReferences.remove(mMessageReference);
        if (mLastDirection == NEXT && mNextMessage != null) {
            onNext();
        } else if (mLastDirection == PREVIOUS && mPreviousMessage != null) {
            onPrevious();
        } else if (mNextMessage != null) {
            onNext();
        } else if (mPreviousMessage != null) {
            onPrevious();
        } else {
            finish();
        }
    }

    protected void onNext() {
        // Reset scroll percentage when we change messages
        if (mNextMessage == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = NEXT;
        toggleActionsState(mMenu, false);
//        if (K9.showAnimations()) {
//            mMessageView.startAnimation(outToLeftAnimation());
//        }
        displayMessage(mNextMessage);
    }

    protected void onPrevious() {
        // Reset scroll percentage when we change messages
        if (mPreviousMessage == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        mLastDirection = PREVIOUS;
        toggleActionsState(mMenu, false);
//        if (K9.showAnimations()) {
//            mMessageView.startAnimation(inFromRightAnimation());
//        }
        displayMessage(mPreviousMessage);
    }

    private void displayMessage(MessageReference reference) {
        mMessageReference = reference;
        findSurroundingMessagesUid();
        configureMenu(mMenu);
        mMessageViewFragment.displayMessage(reference);
    }


    private void findSurroundingMessagesUid() {
        mNextMessage = mPreviousMessage = null;

        int i = mMessageReferences.indexOf(mMessageReference);
        if (i < 0) {
            return;
        }

        if (i != 0) {
            mNextMessage = mMessageReferences.get(i - 1);
        }

        if (i != (mMessageReferences.size() - 1)) {
            mPreviousMessage = mMessageReferences.get(i + 1);
        }
    }


    private final class StorageListenerImplementation implements StorageManager.StorageListener {
        @Override
        public void onUnmount(String providerId) {
            if (!providerId.equals(mAccount.getLocalStorageProviderId())) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onAccountUnavailable();
                }
            });
        }

        @Override
        public void onMount(String providerId) { /* no-op */ }
    }


    @Override
    public void restartActivity() {
        // restart the current activity, so that the theme change can be applied
        if (Build.VERSION.SDK_INT < 11) {
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0); // disable animations to speed up the switch
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else {
            recreate();
        }
    }

    @Override
    public void displayMessageSubject(String subject) {
        setTitle(subject);
    }

    @Override
    public void onReply(Message message, PgpData pgpData) {
        MessageCompose.actionReply(this, mAccount, message, false, pgpData.getDecryptedData());
        finish();
    }

    @Override
    public void onReplyAll(Message message, PgpData pgpData) {
        MessageCompose.actionReply(this, mAccount, message, true, pgpData.getDecryptedData());
        finish();
    }

    @Override
    public void onForward(Message mMessage, PgpData mPgpData) {
        MessageCompose.actionForward(this, mAccount, mMessage, mPgpData.getDecryptedData());
        finish();
    }
}
