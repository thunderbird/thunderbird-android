
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.text.util.Regex;
import android.util.Config;
import android.util.Log;
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
import com.android.email.mail.Address;
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

public class MessageView extends Activity
        implements UrlInterceptHandler, OnClickListener {
    private static final String EXTRA_ACCOUNT = "com.android.email.MessageView_account";
    private static final String EXTRA_FOLDER = "com.android.email.MessageView_folder";
    private static final String EXTRA_MESSAGE = "com.android.email.MessageView_message";
    private static final String EXTRA_FOLDER_UIDS = "com.android.email.MessageView_folderUids";
    private static final String EXTRA_NEXT = "com.android.email.MessageView_next";

    private TextView mFromView;
    private TextView mDateView;
    private TextView mToView;
    private TextView mSubjectView;
    private WebView mMessageContentView;
    private LinearLayout mAttachments;
    private View mAttachmentIcon;
    private View mShowPicturesSection;

    private Account mAccount;
    private String mFolder;
    private String mMessageUid;
    private ArrayList<String> mFolderUids;

    private Message mMessage;
    private String mNextMessageUid = null;
    private String mPreviousMessageUid = null;

    private DateFormat mDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private DateFormat mTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();

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
                boolean hasAttachments) {
            android.os.Message msg = new android.os.Message();
            msg.what = MSG_SET_HEADERS;
            msg.arg1 = hasAttachments ? 1 : 0;
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
        mAttachments = (LinearLayout)findViewById(R.id.attachments);
        mAttachmentIcon = findViewById(R.id.attachment);
        mShowPicturesSection = findViewById(R.id.show_pictures_section);

        mMessageContentView.setVerticalScrollBarEnabled(false);
        mAttachments.setVisibility(View.GONE);
        mAttachmentIcon.setVisibility(View.GONE);

        findViewById(R.id.reply).setOnClickListener(this);
        findViewById(R.id.reply_all).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        findViewById(R.id.show_pictures).setOnClickListener(this);

        // UrlInterceptRegistry.registerHandler(this);

        mMessageContentView.getSettings().setBlockNetworkImage(true);
        mMessageContentView.getSettings().setSupportZoom(false);

        setTitle("");

        Intent intent = getIntent();
        mAccount = (Account) intent.getSerializableExtra(EXTRA_ACCOUNT);
        mFolder = intent.getStringExtra(EXTRA_FOLDER);
        mMessageUid = intent.getStringExtra(EXTRA_MESSAGE);
        mFolderUids = intent.getStringArrayListExtra(EXTRA_FOLDER_UIDS);

        View next = findViewById(R.id.next);
        View previous = findViewById(R.id.previous);
        /*
         * Next and Previous Message are not shown in landscape mode, so
         * we need to check before we use them.
         */
        if (next != null && previous != null) {
            next.setOnClickListener(this);
            previous.setOnClickListener(this);

            findSurroundingMessagesUid();

            previous.setVisibility(mPreviousMessageUid != null ? View.VISIBLE : View.GONE);
            next.setVisibility(mNextMessageUid != null ? View.VISIBLE : View.GONE);

            boolean goNext = intent.getBooleanExtra(EXTRA_NEXT, false);
            if (goNext) {
                next.requestFocus();
            }
        }

        MessagingController.getInstance(getApplication()).addListener(mListener);
        new Thread() {
            public void run() {
                // TODO this is a spot that should be eventually handled by a MessagingController
                // thread pool. We want it in a thread but it can't be blocked by the normal
                // synchronization stuff in MC.
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                MessagingController.getInstance(getApplication()).loadMessageForView(
                        mAccount,
                        mFolder,
                        mMessageUid,
                        mListener);
            }
        }.start();
    }

    private void findSurroundingMessagesUid() {
        for (int i = 0, count = mFolderUids.size(); i < count; i++) {
            String messageUid = mFolderUids.get(i);
            if (messageUid.equals(mMessageUid)) {
                if (i != 0) {
                    mPreviousMessageUid = mFolderUids.get(i - 1);
                }

                if (i != count - 1) {
                    mNextMessageUid = mFolderUids.get(i + 1);
                }
                break;
            }
        }
    }

    public void onResume() {
        super.onResume();
        MessagingController.getInstance(getApplication()).addListener(mListener);
    }

    public void onPause() {
        super.onPause();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void onDelete() {
        if (mMessage != null) {
            MessagingController.getInstance(getApplication()).deleteMessage(
                    mAccount,
                    mFolder,
                    mMessage,
                    null);
            Toast.makeText(this, R.string.message_deleted_toast, Toast.LENGTH_SHORT).show();

            // Remove this message's Uid locally
            mFolderUids.remove(mMessage.getUid());
            // Check if we have previous/next messages available before choosing
            // which one to display
            findSurroundingMessagesUid();

            if (mPreviousMessageUid != null) {
                onPrevious();
            } else if (mNextMessageUid != null) {
                onNext();
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

    private void onNext() {
        Bundle extras = new Bundle(1);
        extras.putBoolean(EXTRA_NEXT, true);
        MessageView.actionView(this, mAccount, mFolder, mNextMessageUid, mFolderUids, extras);
        finish();
    }

    private void onPrevious() {
        MessageView.actionView(this, mAccount, mFolder, mPreviousMessageUid, mFolderUids);
        finish();
    }

    private void onMarkAsUnread() {
        MessagingController.getInstance(getApplication()).markMessageRead(
                mAccount,
                mFolder,
                mMessage.getUid(),
                false);
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
        MessagingController.getInstance(getApplication()).loadAttachment(
                mAccount,
                mMessage,
                attachment.part,
                new Object[] { true, attachment },
                mListener);
    }

    private void onViewAttachment(Attachment attachment) {
        MessagingController.getInstance(getApplication()).loadAttachment(
                mAccount,
                mMessage,
                attachment.part,
                new Object[] { false, attachment },
                mListener);
    }

    private void onShowPictures() {
        mMessageContentView.getSettings().setBlockNetworkImage(false);
        mShowPicturesSection.setVisibility(View.GONE);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reply:
                onReply();
                break;
            case R.id.reply_all:
                onReplyAll();
                break;
            case R.id.delete:
                onDelete();
                break;
            case R.id.next:
                onNext();
                break;
            case R.id.previous:
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
            case R.id.mark_as_unread:
                onMarkAsUnread();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_view_option, menu);
        return true;
    }

    public CacheResult service(String url, Map<String, String> headers) {
        String prefix = "http://cid/";
        if (url.startsWith(prefix)) {
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
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name != null) {
            /*
             * We're guaranteed size because LocalStore.fetch puts it there.
             */
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
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

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewHeadersAvailable(Account account, String folder, String uid,
                final Message message) {
            MessageView.this.mMessage = message;
            try {
                String subjectText = message.getSubject();
                String fromText = Address.toFriendly(message.getFrom());
                String dateText = Utility.isDateToday(message.getSentDate()) ?
                        mTimeFormat.format(message.getSentDate()) :
                            mDateTimeFormat.format(message.getSentDate());
                String toText = Address.toFriendly(message.getRecipients(RecipientType.TO));
                boolean hasAttachments = ((LocalMessage) message).getAttachmentCount() > 0;
                mHandler.setHeaders(subjectText,
                        fromText,
                        dateText,
                        toText,
                        hasAttachments);
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
            MessageView.this.mMessage = message;
            try {
                Part part = MimeUtility.findFirstPartByMimeType(mMessage, "text/html");
                if (part == null) {
                    part = MimeUtility.findFirstPartByMimeType(mMessage, "text/plain");
                }
                if (part != null) {
                    String text = MimeUtility.getTextFromPart(part);
                    if (part.getMimeType().equalsIgnoreCase("text/html")) {
                        text = text.replaceAll("cid:", "http://cid/");
                    } else {
                        /*
                         * Linkify the plain text and convert it to HTML by replacing
                         * \r?\n with <br> and adding a html/body wrapper.
                         */
                        Matcher m = Regex.WEB_URL_PATTERN.matcher(text);
                        StringBuffer sb = new StringBuffer();
                        while (m.find()) {
                            int start = m.start();
                            if (start != 0 && text.charAt(start - 1) != '@') {
                                m.appendReplacement(sb, "<a href=\"$0\">$0</a>");
                            }
                            else {
                                m.appendReplacement(sb, "$0");
                            }
                        }
                        m.appendTail(sb);
                        text = sb.toString().replaceAll("\r?\n", "<br>");
                        text = "<html><body>" + text + "</body></html>";
                    }

                    /*
                     * TODO this should be smarter, change to regex for img, but consider how to
                     * get backgroung images and a million other things that HTML allows.
                     */
                    if (text.contains("img")) {
                        mHandler.showShowPictures(true);
                    }

                    mMessageContentView.loadDataWithBaseURL("email://", text, "text/html",
                            "utf-8", null);
                }
                else {
                    mMessageContentView.loadUrl("file:///android_asset/empty.html");
                }
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
