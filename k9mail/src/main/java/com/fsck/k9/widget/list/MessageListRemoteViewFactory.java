package com.fsck.k9.widget.list;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Binder;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.provider.MessageProvider;


public class MessageListRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
    private static String[] MAIL_LIST_PROJECTIONS = {
            MessageProvider.MessageColumns.SENDER,
            MessageProvider.MessageColumns.SEND_DATE,
            MessageProvider.MessageColumns.SUBJECT,
            MessageProvider.MessageColumns.PREVIEW,
            MessageProvider.MessageColumns.UNREAD,
            MessageProvider.MessageColumns.HAS_ATTACHMENTS,
            MessageProvider.MessageColumns.URI
    };


    private final Context context;
    private final Calendar calendar;
    private final ArrayList<MailItem> mailItems = new ArrayList<>(25);
    private boolean senderAboveSubject;
    private int readTextColor;
    private int unreadTextColor;


    public MessageListRemoteViewFactory(Context context) {
        this.context = context;
        calendar = Calendar.getInstance();
    }

    @Override
    public void onCreate() {
        senderAboveSubject = K9.messageListSenderAboveSubject();
        readTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_read);
        unreadTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_unread);
    }

    @Override
    public void onDataSetChanged() {
        long identityToken = Binder.clearCallingIdentity();
        try {
            loadMessageList();
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void loadMessageList() {
        mailItems.clear();

        Uri unifiedInboxUri = MessageProvider.CONTENT_URI.buildUpon().appendPath("inbox_messages").build();
        Cursor cursor = context.getContentResolver().query(unifiedInboxUri, MAIL_LIST_PROJECTIONS, null, null, null);

        if (cursor == null) {
            return;
        }

        try {
            while (cursor.moveToNext()) {
                String sender = cursor.getString(0);
                long date = cursor.isNull(1) ? 0L : cursor.getLong(1);
                String subject = cursor.getString(2);
                String preview = cursor.getString(3);
                boolean unread = toBoolean(cursor.getString(4));
                boolean hasAttachment = toBoolean(cursor.getString(5));
                Uri viewUri = Uri.parse(cursor.getString(6));

                mailItems.add(new MailItem(sender, date, subject, preview, unread, hasAttachment, viewUri));
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onDestroy() {
        // Implement interface
    }

    @Override
    public int getCount() {
        return mailItems.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.message_list_widget_list_item);
        MailItem item = mailItems.get(position);

        CharSequence sender = item.unread ? bold(item.sender) : item.sender;
        CharSequence subject = item.unread ? bold(item.subject) : item.subject;

        if (senderAboveSubject) {
            remoteView.setTextViewText(R.id.sender, sender);
            remoteView.setTextViewText(R.id.mail_subject, subject);
        } else {
            remoteView.setTextViewText(R.id.sender, subject);
            remoteView.setTextViewText(R.id.mail_subject, sender);
        }
        remoteView.setTextViewText(R.id.mail_date, item.getDateFormatted("%d %s"));
        remoteView.setTextViewText(R.id.mail_preview, item.preview);

        int textColor = item.getTextColor();
        remoteView.setTextColor(R.id.sender, textColor);
        remoteView.setTextColor(R.id.mail_subject, textColor);
        remoteView.setTextColor(R.id.mail_date, textColor);
        remoteView.setTextColor(R.id.mail_preview, textColor);

        if (item.hasAttachment) {
            remoteView.setInt(R.id.attachment, "setVisibility", View.VISIBLE);
        } else {
            remoteView.setInt(R.id.attachment, "setVisibility", View.GONE);
        }

        Intent intent = new Intent();
        intent.setData(item.uri);
        remoteView.setOnClickFillInIntent(R.id.mail_list_item, intent);
        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        RemoteViews loadingView = new RemoteViews(context.getPackageName(), R.layout.message_list_widget_loading);
        loadingView.setTextViewText(R.id.loadingText, context.getString(R.string.mail_list_widget_loading));
        loadingView.setViewVisibility(R.id.loadingText, View.VISIBLE);
        return loadingView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private CharSequence bold(String text) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
        return spannableString;
    }

    private boolean toBoolean(String value) {
        return Boolean.valueOf(value);
    }


    private class MailItem {
        final long date;
        final String sender;
        final String preview;
        final String subject;
        final boolean unread;
        final boolean hasAttachment;
        final Uri uri;


        MailItem(String sender, long date, String subject, String preview, boolean unread, boolean hasAttachment,
                Uri viewUri) {
            this.sender = sender;
            this.date = date;
            this.preview = preview;
            this.subject = subject;
            this.unread = unread;
            this.uri = viewUri;
            this.hasAttachment = hasAttachment;
        }

        int getTextColor() {
            return unread ? unreadTextColor : readTextColor;
        }

        String getDateFormatted(String format) {
            calendar.setTimeInMillis(date);

            return String.format(format,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        }
    }
}

