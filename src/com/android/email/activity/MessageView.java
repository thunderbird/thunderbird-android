
package com.android.email.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.util.Regex;
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.CacheManager;
import android.webkit.UrlInterceptHandler;
import android.webkit.WebView;
import android.webkit.CacheManager.CacheResult;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.R;
import com.android.email.Utility;
import com.android.email.activity.FolderMessageList.FolderMessageListAdapter.FolderInfoHolder;
import com.android.email.activity.FolderMessageList.FolderMessageListAdapter.MessageInfoHolder;
import com.android.email.mail.Address;
import com.android.email.mail.Flag;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Multipart;
import com.android.email.mail.Part;
import com.android.email.mail.Message.RecipientType;
import com.android.email.mail.internet.MimeHeader;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.store.LocalStore.LocalAttachmentBody;
import com.android.email.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.android.email.mail.store.LocalStore.LocalMessage;
import com.android.email.provider.AttachmentProvider;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MessageView extends Activity
        implements UrlInterceptHandler, OnClickListener {
    private static final String EXTRA_ACCOUNT = "com.android.email.MessageView_account";
    private static final String EXTRA_FOLDER = "com.android.email.MessageView_folder";
    private static final String EXTRA_MESSAGE = "com.android.email.MessageView_message";
    private static final String EXTRA_FOLDER_UIDS = "com.android.email.MessageView_folderUids";
    private static final String EXTRA_NEXT = "com.android.email.MessageView_next";
    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 120000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());


    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;

    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    
    private TextView mFromView;
    private TextView mDateView;
    private TextView mToView;
    private TextView mSubjectView;
    private WebView mMessageContentView;
    private LinearLayout mAttachments;
    private View mAttachmentIcon;
    private View mShowPicturesSection;
    View next;
    View next_scrolling;
    View previous;
    View previous_scrolling;

    private Account mAccount;
    private String mFolder;
    private String mMessageUid;
    private ArrayList<String> mFolderUids;

    private Message mMessage;
    private String mNextMessageUid = null;
    private String mPreviousMessageUid = null;

    private DateFormat dateFormat = null;
    private DateFormat timeFormat = null;
    
    private Menu optionsMenu = null;
    
    
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
       return  dateFormat;
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

    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();




       public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL: { onDelete(); return true;}
            case KeyEvent.KEYCODE_D: { onDelete(); return true;}
            case KeyEvent.KEYCODE_F: { onForward(); return true;}
            case KeyEvent.KEYCODE_A: { onReplyAll(); return true; }
            case KeyEvent.KEYCODE_R: { onReply(); return true; }
            case KeyEvent.KEYCODE_G: { onFlag(); return true; }

            case KeyEvent.KEYCODE_M: { onMove(); return true; }
            case KeyEvent.KEYCODE_Y: { onCopy(); return true; }
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_P:
            { onPrevious(); return true; }
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_K: { onNext(); return true; }
            case KeyEvent.KEYCODE_Z: { if (event.isShiftPressed()) {
                                            mMessageContentView.zoomIn();
                                        } else {
                                            mMessageContentView.zoomOut();
                                        }
                                     return true; }

           case KeyEvent.KEYCODE_H: {
               Toast toast = Toast.makeText(this, R.string.message_help_key, Toast.LENGTH_LONG);
               toast.show();
               return true; }
            }
           return super.onKeyDown(keyCode, event);
        }



    class MessageViewHandler extends Handler {
        private static final int MSG_PROGRESS = 2;
        private static final int MSG_ADD_ATTACHMENT = 3;
        private static final int MSG_SET_ATTACHMENTS_ENABLED = 4;
        private static final int MSG_SET_HEADERS = 5;
        private static final int MSG_NETWORK_ERROR = 6;
        private static final int MSG_ATTACHMENT_SAVED = 7;
        private static final int MSG_ATTACHMENT_NOT_SAVED = 8;
        private static final int MSG_SHOW_SHOW_PICTURES = 9;
        private static final int MSG_FETCHING_ATTACHMENT = 10;
        private static final int FLAG_FLAGGED = 1;
        private static final int FLAG_ANSWERED = 2;

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_PROGRESS:
                    setProgressBarIndeterminateVisibility(msg.arg1 != 0);
                    break;
                case MSG_ADD_ATTACHMENT:
                    mAttachments.addView((View) msg.obj);
                    mAttachments.setVisibility(View.VISIBLE);
                    break;
                case MSG_SET_ATTACHMENTS_ENABLED:
                    for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
                        Attachment attachment = (Attachment) mAttachments.getChildAt(i).getTag();
                        attachment.viewButton.setEnabled(msg.arg1 == 1);
                        attachment.downloadButton.setEnabled(msg.arg1 == 1);
                    }
                    break;
                case MSG_SET_HEADERS:
                    String[] values = (String[]) msg.obj;
                    setTitle(values[0]);
                    mSubjectView.setText(values[0]);
                    mFromView.setText(values[1]);
                    mDateView.setText(values[2]);
                    mToView.setText(values[3]);
                    mAttachmentIcon.setVisibility(msg.arg1 == 1 ? View.VISIBLE : View.GONE);
                    if ((msg.arg2 & FLAG_FLAGGED) != 0) {
                      mSubjectView.setTextColor(0xff000000 | Email.FLAGGED_COLOR);
                    }
                    else {
                      mSubjectView.setTextColor(0xff000000);
                    }
                    if ((msg.arg2 & FLAG_ANSWERED) != 0) {
                     Drawable answeredIcon = getResources().getDrawable(
                          R.drawable.ic_mms_answered_small);
                     mSubjectView.setCompoundDrawablesWithIntrinsicBounds(
                          answeredIcon, // left 
                              null, // top
                              null, // right 
                              null); // bottom
                    }
                    
                    break;
                case MSG_NETWORK_ERROR:
                    Toast.makeText(MessageView.this,
                            R.string.status_network_error, Toast.LENGTH_LONG).show();
                    break;
                case MSG_ATTACHMENT_SAVED:
                    Toast.makeText(MessageView.this, String.format(
                            getString(R.string.message_view_status_attachment_saved), msg.obj),
                            Toast.LENGTH_LONG).show();
                    break;
                case MSG_ATTACHMENT_NOT_SAVED:
                    Toast.makeText(MessageView.this,
                            getString(R.string.message_view_status_attachment_not_saved),
                            Toast.LENGTH_LONG).show();
                    break;
                case MSG_SHOW_SHOW_PICTURES:
                    mShowPicturesSection.setVisibility(msg.arg1 == 1 ? View.VISIBLE : View.GONE);
                    break;
                case MSG_FETCHING_ATTACHMENT:
                    Toast.makeText(MessageView.this,
                            getString(R.string.message_view_fetching_attachment_toast),
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        public void progress(boolean progress) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_PROGRESS;
            msg.arg1 = progress ? 1 : 0;
            sendMessage(msg);
        }

        public void addAttachment(View attachmentView) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_ADD_ATTACHMENT;
            msg.obj = attachmentView;
            sendMessage(msg);
        }

        public void setAttachmentsEnabled(boolean enabled) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SET_ATTACHMENTS_ENABLED;
            msg.arg1 = enabled ? 1 : 0;
            sendMessage(msg);
        }

        public void setHeaders(
                String subject,
                String from,
                String date,
                String to,
                boolean hasAttachments,
                boolean flagged,
                boolean seen) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SET_HEADERS;
            msg.arg1 = hasAttachments ? 1 : 0;
            msg.arg2 += (flagged ? FLAG_FLAGGED : 0);
            msg.arg2 += (seen ? FLAG_ANSWERED : 0);
           
            msg.obj = new String[] { subject, from, date, to };
            sendMessage(msg);
        }

        public void networkError() {
            sendEmptyMessage(MSG_NETWORK_ERROR);
        }

        public void attachmentSaved(String filename) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_ATTACHMENT_SAVED;
            msg.obj = filename;
            sendMessage(msg);
        }

        public void attachmentNotSaved() {
            sendEmptyMessage(MSG_ATTACHMENT_NOT_SAVED);
        }

        public void fetchingAttachment() {
            sendEmptyMessage(MSG_FETCHING_ATTACHMENT);
        }

        public void showShowPictures(boolean show) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SHOW_SHOW_PICTURES;
            msg.arg1 = show ? 1 : 0;
            sendMessage(msg);
        }



    }

    class Attachment {
        public String name;
        public String contentType;
        public long size;
        public LocalAttachmentBodyPart part;
        public Button viewButton;
        public Button downloadButton;
        public ImageView iconView;
    }

    public static void actionView(Context context, Account account,
            String folder, String messageUid, ArrayList<String> folderUids) {
        actionView(context, account, folder, messageUid, folderUids, null);
    }

    public static void actionView(Context context, Account account,
            String folder, String messageUid, ArrayList<String> folderUids, Bundle extras) {
        Intent i = new Intent(context, MessageView.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        i.putExtra(EXTRA_FOLDER, folder);
        i.putExtra(EXTRA_MESSAGE, messageUid);
        i.putExtra(EXTRA_FOLDER_UIDS, folderUids);
        if (extras != null) {
            i.putExtras(extras);
        }
        context.startActivity(i);
     }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.message_view);

        mFromView = (TextView)findViewById(R.id.from);
        mToView = (TextView)findViewById(R.id.to);
        mSubjectView = (TextView)findViewById(R.id.subject);
        mDateView = (TextView)findViewById(R.id.date);
        mMessageContentView = (WebView)findViewById(R.id.message_content);
        //mMessageContentView.setWebViewClient(new MessageWebViewClient());
        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mAttachmentIcon = findViewById(R.id.attachment);
        mShowPicturesSection = findViewById(R.id.show_pictures_section);

        mMessageContentView.setVerticalScrollBarEnabled(false);
        mAttachments.setVisibility(View.GONE);
        mAttachmentIcon.setVisibility(View.GONE);

        setOnClickListener(R.id.reply);
        setOnClickListener(R.id.reply_all);
        setOnClickListener(R.id.delete);
        setOnClickListener(R.id.forward);
        setOnClickListener(R.id.next);
        setOnClickListener(R.id.previous);
 
        setOnClickListener(R.id.reply_scrolling);
 //       setOnClickListener(R.id.reply_all_scrolling);
        setOnClickListener(R.id.delete_scrolling);
        setOnClickListener(R.id.forward_scrolling);
        setOnClickListener(R.id.next_scrolling);
        setOnClickListener(R.id.previous_scrolling);

        setOnClickListener(R.id.show_pictures);

        // UrlInterceptRegistry.registerHandler(this);

        mMessageContentView.getSettings().setBlockNetworkImage(true);
        mMessageContentView.getSettings().setSupportZoom(true);

        setTitle("");

        Intent intent = getIntent();
        mAccount = (Account) intent.getSerializableExtra(EXTRA_ACCOUNT);
        mFolder = intent.getStringExtra(EXTRA_FOLDER);
        mMessageUid = intent.getStringExtra(EXTRA_MESSAGE);
        mFolderUids = intent.getStringArrayListExtra(EXTRA_FOLDER_UIDS);
       
        next = findViewById(R.id.next);
        previous = findViewById(R.id.previous);
        
       
        setOnClickListener(R.id.next);
        setOnClickListener(R.id.previous);

        next_scrolling = findViewById(R.id.next_scrolling);
        
        
        previous_scrolling = findViewById(R.id.previous_scrolling);

        boolean goNext = intent.getBooleanExtra(EXTRA_NEXT, false);
        if (goNext) {
            next.requestFocus();
        }
        
        Account.HideButtons hideButtons = mAccount.getHideMessageViewButtons();
        
        MessagingController.getInstance(getApplication()).addListener(mListener);
        if (Account.HideButtons.ALWAYS == hideButtons)
        {
          hideButtons();
        }
        else if (Account.HideButtons.NEVER == hideButtons)
        {
          showButtons();
        }
        else // Account.HideButtons.KEYBOARD_AVAIL
        {
            final Configuration config = this.getResources().getConfiguration();
            if (config.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO )
            {
              hideButtons();
            }
            else
            {
              showButtons();
            }
        }
        displayMessage(mMessageUid);
  }

    Thread loaderThread = new Thread() {
            public void run() {
                // TODO this is a spot that should be eventually handled by a MessagingController
                // thread pool. We want it in a thread but it can't be blocked by the normal
                // synchronization stuff in MC.
                MessagingController.getInstance(getApplication()).loadMessageForViewSynchronous(
                    mAccount,
                    mFolder,
                    mMessageUid,
                    null);
            }
        };

    private void displayMessage(String uid)
    {
        mMessageUid = uid;
        mAttachments.removeAllViews();
        findSurroundingMessagesUid();
        next.setEnabled(mNextMessageUid != null );
        previous.setEnabled(mPreviousMessageUid != null);
        if (next_scrolling != null)
            next_scrolling.setEnabled(mNextMessageUid != null );
        if (previous_scrolling != null)
            previous_scrolling.setEnabled(mPreviousMessageUid != null);
        threadPool.execute(loaderThread);
    }
    
    
  private void showButtons()
  {
    View buttons = findViewById(R.id.scrolling_buttons);
    if (buttons != null) {
      buttons.setVisibility(View.GONE);
    }
  }
  
  private void hideButtons()
  {
    View buttons = findViewById(R.id.bottom_buttons);
    if (buttons != null) {
      buttons.setVisibility(View.GONE);
    }
  }
    
    private void setOnClickListener(int viewCode)
    {
      View thisView = findViewById(viewCode);
      if (thisView != null)
      {
        thisView.setOnClickListener(this);
      }
    }

    private void findSurroundingMessagesUid() {
        mNextMessageUid = mPreviousMessageUid = null;
        int i = mFolderUids.indexOf(mMessageUid);
        if(i < 0)
            return;
        if(i != 0)
            mNextMessageUid = mFolderUids.get(i - 1);
        if(i != (mFolderUids.size() - 1))
            mPreviousMessageUid = mFolderUids.get(i + 1);
    }

    public void onResume() {
        super.onResume();
        clearFormats();
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }

    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
    }
    
    private void onDelete() {
        if (mMessage != null) {
           Message messageToDelete = mMessage;
           String folderForDelete = mFolder;
           Account accountForDelete = mAccount;

           findSurroundingMessagesUid();

            // Remove this message's Uid locally
            mFolderUids.remove(messageToDelete.getUid());
            
            
            MessagingController.getInstance(getApplication()).deleteMessage(
                accountForDelete,
                folderForDelete,
                messageToDelete,
                null);
            if (mNextMessageUid != null) {
              onNext();
            }
            else if (mPreviousMessageUid != null) {
                onPrevious();
            } else {
                finish();
            }
        }
    }

    private void onReply() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mAccount, mMessage, false);
            finish();
        }
    }

    private void onReplyAll() {
        if (mMessage != null) {
            MessageCompose.actionReply(this, mAccount, mMessage, true);
            finish();
        }
    }

    private void onForward() {
        if (mMessage != null) {
            MessageCompose.actionForward(this, mAccount, mMessage);
            finish();
        }
    }
    
    private void onFlag() {
      if (mMessage != null) {
        MessagingController.getInstance(getApplication()).setMessageFlag(mAccount,
            mMessage.getFolder().getName(), mMessage.getUid(), Flag.FLAGGED, !mMessage.isSet(Flag.FLAGGED));
        try
        {
          mMessage.setFlag(Flag.FLAGGED, !mMessage.isSet(Flag.FLAGGED));
          setHeaders(mAccount, mMessage.getFolder().getName(), mMessage.getUid(), mMessage);
          setMenuFlag();
        }
        catch (MessagingException me)
        {
          Log.e(Email.LOG_TAG, "Could not set flag on local message", me);
        }
      }
  }
    
    private void onMove()
    {
      if (MessagingController.getInstance(getApplication()).isMoveCapable(mAccount) == false)
      {
        return;
      }
      if (MessagingController.getInstance(getApplication()).isMoveCapable(mMessage) == false)
      {
       Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
       toast.show();
       return;
      }
      Intent intent = new Intent(this, ChooseFolder.class);
      intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount);
      intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mFolder);
      intent.putExtra(ChooseFolder.EXTRA_MESSAGE_UID, mMessageUid);
      startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_MOVE);
    }
    
     private void onCopy()
      {
       if (MessagingController.getInstance(getApplication()).isCopyCapable(mAccount) == false)
       {
         return;
       }
       if (MessagingController.getInstance(getApplication()).isCopyCapable(mMessage) == false)
       {
        Toast toast = Toast.makeText(this, R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
        toast.show();
        return;
       }
        Intent intent = new Intent(this, ChooseFolder.class);

        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount);
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mFolder);
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE_UID, mMessageUid);
        startActivityForResult(intent, ACTIVITY_CHOOSE_FOLDER_COPY);
      }
     
     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       if(resultCode != RESULT_OK)
         return;

       switch(requestCode) {
         case ACTIVITY_CHOOSE_FOLDER_MOVE:
         case ACTIVITY_CHOOSE_FOLDER_COPY:
           if (data == null)
             return;
           String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
           String srcFolderName = data.getStringExtra(ChooseFolder.EXTRA_CUR_FOLDER);
           String uid = data.getStringExtra(ChooseFolder.EXTRA_MESSAGE_UID);
           
           if (uid.equals(mMessageUid) && srcFolderName.equals(mFolder))
           {
             
             switch (requestCode) {
               case ACTIVITY_CHOOSE_FOLDER_MOVE:
                 MessagingController.getInstance(getApplication()).moveMessage(mAccount,
                     srcFolderName, mMessage, destFolderName, null);
                 break;
               case ACTIVITY_CHOOSE_FOLDER_COPY:
                 MessagingController.getInstance(getApplication()).copyMessage(mAccount,
                     srcFolderName, mMessage, destFolderName, null);
                 break;
             }
           }
       }
     }
  
    
    private void onSendAlternate() {
      if (mMessage != null) {
                       MessagingController.getInstance(getApplication()).sendAlternate(this, mAccount, mMessage);

      }
  }

    private void onNext() {
        if (mNextMessageUid == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        displayMessage(mNextMessageUid);
        next.requestFocus();
    }

    private void onPrevious() {
        if (mPreviousMessageUid == null) {
            Toast.makeText(this, getString(R.string.end_of_folder), Toast.LENGTH_SHORT).show();
            return;
        }
        displayMessage(mPreviousMessageUid);
        previous.requestFocus();
    }

    private void onMarkAsUnread() {
      if (mMessage != null)
      {
        MessagingController.getInstance(getApplication()).markMessageRead(
                mAccount,
                mFolder,
                mMessage.getUid(),
                false);
      }
    }

    /**
     * Creates a unique file in the given directory by appending a hyphen
     * and a number to the given filename.
     * @param directory
     * @param filename
     * @return
     */
    private File createUniqueFile(File directory, String filename) {
        File file = new File(directory, filename);
        if (!file.exists()) {
            return file;
        }
        // Get the extension of the file, if any.
        int index = filename.lastIndexOf('.');
        String format;
        if (index != -1) {
            String name = filename.substring(0, index);
            String extension = filename.substring(index);
            format = name + "-%d" + extension;
        }
        else {
            format = filename + "-%d";
        }
        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            file = new File(directory, String.format(format, i));
            if (!file.exists()) {
                return file;
            }
        }
        return null;
    }

    private void onDownloadAttachment(Attachment attachment) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            /*
             * Abort early if there's no place to save the attachment. We don't want to spend
             * the time downloading it and then abort.
             */
            Toast.makeText(this,
                    getString(R.string.message_view_status_attachment_not_saved),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMessage != null)
        {
          MessagingController.getInstance(getApplication()).loadAttachment(
                  mAccount,
                  mMessage,
                  attachment.part,
                  new Object[] { true, attachment },
                  mListener);
        }
    }

    private void onViewAttachment(Attachment attachment) {
      if (mMessage != null)
      {
        MessagingController.getInstance(getApplication()).loadAttachment(
                mAccount,
                mMessage,
                attachment.part,
                new Object[] { false, attachment },
                mListener);
      }
    }

    private void onShowPictures() {
        mMessageContentView.getSettings().setBlockNetworkImage(false);
        mShowPicturesSection.setVisibility(View.GONE);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reply:
            case R.id.reply_scrolling:
                onReply();
                break;
            case R.id.reply_all:
              onReplyAll();
              break;
            case R.id.delete:
            case R.id.delete_scrolling:
                onDelete();
                break;
            case R.id.forward:
            case R.id.forward_scrolling:
                onForward();
                break;
            case R.id.next:
            case R.id.next_scrolling:
                onNext();
                break;
            case R.id.previous:
            case R.id.previous_scrolling:
                onPrevious();
                break;
            case R.id.download:
                onDownloadAttachment((Attachment) view.getTag());
                break;
            case R.id.view:
                onViewAttachment((Attachment) view.getTag());
                break;
            case R.id.show_pictures:
                onShowPictures();
                break;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                onDelete();
                break;
            case R.id.reply:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.forward:
                onForward();
                break;
            case R.id.send_alternate:
              onSendAlternate();
              break;
            case R.id.mark_as_unread:
                onMarkAsUnread();
                break;
            case R.id.flag:
              onFlag();
              break;
            case R.id.move:
              onMove();
              break;
            case R.id.copy:
              onCopy();
              break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_view_option, menu);
        optionsMenu = menu;
        setMenuFlag();
        if (MessagingController.getInstance(getApplication()).isCopyCapable(mAccount) == false)
        {
         menu.findItem(R.id.copy).setVisible(false);
        }
       if (MessagingController.getInstance(getApplication()).isMoveCapable(mAccount) == false)
       {
        menu.findItem(R.id.move).setVisible(false);
       }
        return true;
    }
    
    private void setMenuFlag()
    {
      Menu menu = optionsMenu;
      if (menu != null)
      {
        MenuItem flagItem = menu.findItem(R.id.flag);
        if (flagItem != null && mMessage != null)
        {
          flagItem.setTitle((mMessage.isSet(Flag.FLAGGED) ? R.string.unflag_action : R.string.flag_action));
        }
      }
    }
    
    public CacheResult service(String url, Map<String, String> headers) {
        String prefix = "http://cid/";
        if (url.startsWith(prefix) && mMessage != null) {
            try {
                String contentId = url.substring(prefix.length());
                final Part part = MimeUtility.findPartByContentId(mMessage, "<" + contentId + ">");
                if (part != null) {
                    CacheResult cr = new CacheManager.CacheResult();
                    // TODO looks fixed in Mainline, cr.setInputStream
                    // part.getBody().writeTo(cr.getStream());
                    return cr;
                }
            }
            catch (Exception e) {
                // TODO
            }
        }
        return null;
    }

    private Bitmap getPreviewIcon(Attachment attachment) throws MessagingException {
        try {
            return BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(
                            AttachmentProvider.getAttachmentThumbnailUri(mAccount,
                                    attachment.part.getAttachmentId(),
                                    62,
                                    62)));
        }
        catch (Exception e) {
            /*
             * We don't care what happened, we just return null for the preview icon.
             */
            return null;
        }
    }

    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single digit
     * of precision. Ex: 12,315,000 = 12.3 MB
     */
    public static String formatSize(float size) {
        long kb = 1024;
        long mb = (kb * 1024);
        long gb  = (mb * 1024);
        if (size < kb) {
            return String.format("%d bytes", (int) size);
        }
        else if (size < mb) {
            return String.format("%.1f kB", size / kb);
        }
        else if (size < gb) {
            return String.format("%.1f MB", size / mb);
        }
        else {
            return String.format("%.1f GB", size / gb);
        }
    }

    private void renderAttachments(Part part, int depth) throws MessagingException {
        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name == null)
        {
          name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }
        if (name != null) {
            /*
             * We're guaranteed size because LocalStore.fetch puts it there.
             */
            int size = Integer.parseInt(MimeUtility.getHeaderParameter(contentDisposition, "size"));

            Attachment attachment = new Attachment();
            attachment.size = size;
            attachment.contentType = part.getMimeType();
            attachment.name = name;
            attachment.part = (LocalAttachmentBodyPart) part;

            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.message_view_attachment, null);

            TextView attachmentName = (TextView)view.findViewById(R.id.attachment_name);
            TextView attachmentInfo = (TextView)view.findViewById(R.id.attachment_info);
            ImageView attachmentIcon = (ImageView)view.findViewById(R.id.attachment_icon);
            Button attachmentView = (Button)view.findViewById(R.id.view);
            Button attachmentDownload = (Button)view.findViewById(R.id.download);

            if ((!MimeUtility.mimeTypeMatches(attachment.contentType,
                    Email.ACCEPTABLE_ATTACHMENT_VIEW_TYPES))
                    || (MimeUtility.mimeTypeMatches(attachment.contentType,
                            Email.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES))) {
                attachmentView.setVisibility(View.GONE);
            }
            if ((!MimeUtility.mimeTypeMatches(attachment.contentType,
                    Email.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))
                    || (MimeUtility.mimeTypeMatches(attachment.contentType,
                            Email.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))) {
                attachmentDownload.setVisibility(View.GONE);
            }

            if (attachment.size > Email.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
                attachmentView.setVisibility(View.GONE);
                attachmentDownload.setVisibility(View.GONE);
            }

            attachment.viewButton = attachmentView;
            attachment.downloadButton = attachmentDownload;
            attachment.iconView = attachmentIcon;

            view.setTag(attachment);
            attachmentView.setOnClickListener(this);
            attachmentView.setTag(attachment);
            attachmentDownload.setOnClickListener(this);
            attachmentDownload.setTag(attachment);

            attachmentName.setText(name);
            attachmentInfo.setText(formatSize(size));

            Bitmap previewIcon = getPreviewIcon(attachment);
            if (previewIcon != null) {
                attachmentIcon.setImageBitmap(previewIcon);
            }

            mHandler.addAttachment(view);
        }

        if (part.getBody() instanceof Multipart) {
            Multipart mp = (Multipart)part.getBody();
            for (int i = 0; i < mp.getCount(); i++) {
                renderAttachments(mp.getBodyPart(i), depth + 1);
            }
        }
    }
    
    private void setHeaders(Account account, String folder, String uid,
                final Message message) throws MessagingException
    {
      String subjectText = message.getSubject();
      String fromText = Address.toFriendly(message.getFrom());
      String dateText = Utility.isDateToday(message.getSentDate()) ?
              getTimeFormat().format(message.getSentDate()) :
                  getDateFormat().format(message.getSentDate());
      String toText = Address.toFriendly(message.getRecipients(RecipientType.TO));
      boolean hasAttachments = ((LocalMessage) message).getAttachmentCount() > 0;
      mHandler.setHeaders(subjectText,
              fromText,
              dateText,
              toText,
              hasAttachments,
              message.isSet(Flag.FLAGGED),
              message.isSet(Flag.ANSWERED));
    }

    class Listener extends MessagingListener {

        @Override
        public void loadMessageForViewHeadersAvailable(Account account, String folder, String uid,
                final Message message) {
            MessageView.this.mMessage = message;
            try {
                setHeaders(account, folder, uid, message);
            }
            catch (MessagingException me) {
                if (Config.LOGV) {
                    Log.v(Email.LOG_TAG, "loadMessageForViewHeadersAvailable", me);
                }
            }
        }

        @Override
        public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
                Message message) {
            Spannable markup;
            MessageView.this.mMessage = message;
            try {
            Part part = MimeUtility.findFirstPartByMimeType(mMessage, "text/html");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(mMessage, "text/plain");
            }
            if (part != null) {
                String text = MimeUtility.getTextFromPart(part);
                  /*
                   * TODO this should be smarter, change to regex for img, but consider how to
                   * get background images and a million other things that HTML allows.
                   */
                 mHandler.showShowPictures(text.contains("<img"));
                 mMessageContentView.loadDataWithBaseURL("email://", text, "text/html", "utf-8", null);
              }
             else
                  mMessageContentView.loadUrl("file:///android_asset/empty.html");
              renderAttachments(mMessage, 0);
              }
            catch (Exception e) {
                    if (Config.LOGV) {
                        Log.v(Email.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                }
            }
  


        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid,
                final String message) {
            mHandler.post(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(false);
                    mHandler.networkError();
                    mMessageContentView.loadUrl("file:///android_asset/empty.html");
                }
            });
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid,
                Message message) {
            mHandler.post(new Runnable() {
                public void run() {
                    setProgressBarIndeterminateVisibility(false);
                }
            });
        }

        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            mHandler.post(new Runnable() {
                public void run() {
                    mMessageContentView.loadUrl("file:///android_asset/loading.html");
                    setProgressBarIndeterminateVisibility(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message,
                Part part, Object tag, boolean requiresDownload) {
            mHandler.setAttachmentsEnabled(false);
            mHandler.progress(true);
            if (requiresDownload) {
                mHandler.fetchingAttachment();
            }
        }

        @Override
        public void loadAttachmentFinished(Account account, Message message,
                Part part, Object tag) {
            mHandler.setAttachmentsEnabled(true);
            mHandler.progress(false);

            Object[] params = (Object[]) tag;
            boolean download = (Boolean) params[0];
            Attachment attachment = (Attachment) params[1];

            if (download) {
                try {
                    File file = createUniqueFile(Environment.getExternalStorageDirectory(),
                            attachment.name);
                    Uri uri = AttachmentProvider.getAttachmentUri(
                            mAccount,
                            attachment.part.getAttachmentId());
                    InputStream in = getContentResolver().openInputStream(uri);
                    OutputStream out = new FileOutputStream(file);
                    IOUtils.copy(in, out);
                    out.flush();
                    out.close();
                    in.close();
                    mHandler.attachmentSaved(file.getName());
                    new MediaScannerNotifier(MessageView.this, file);
                }
                catch (IOException ioe) {
                    mHandler.attachmentNotSaved();
                }
            }
            else {
                Uri uri = AttachmentProvider.getAttachmentUri(
                        mAccount,
                        attachment.part.getAttachmentId());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part,
                Object tag, String reason) {
            mHandler.setAttachmentsEnabled(true);
            mHandler.progress(false);
            mHandler.networkError();
        }
    }

    class MediaScannerNotifier implements MediaScannerConnectionClient {
        private MediaScannerConnection mConnection;
        private File mFile;

        public MediaScannerNotifier(Context context, File file) {
            mFile = file;
            mConnection = new MediaScannerConnection(context, this);
            mConnection.connect();
        }

        public void onMediaScannerConnected() {
            mConnection.scanFile(mFile.getAbsolutePath(), null);
        }

        public void onScanCompleted(String path, Uri uri) {
            try {
                if (uri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    startActivity(intent);
                }
            } finally {
                mConnection.disconnect();
            }
        }
    }

}
