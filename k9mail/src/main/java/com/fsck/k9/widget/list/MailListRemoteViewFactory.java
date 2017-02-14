package com.fsck.k9.widget.list;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Binder;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.provider.MessageProvider;


public class MailListRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
    private static String[] MAIL_LIST_PROJECTIONS = {
            MessageProvider.MessageColumns._ID,
            MessageProvider.MessageColumns.SENDER,
            MessageProvider.MessageColumns.SEND_DATE,
            MessageProvider.MessageColumns.SUBJECT,
            MessageProvider.MessageColumns.PREVIEW,
            MessageProvider.MessageColumns.UNREAD,
            MessageProvider.MessageColumns.HAS_ATTACHMENTS,
            MessageProvider.MessageColumns.URI
    };


    private Context context;
    private ArrayList<MailItem> mailItems;
    private int count;
    private boolean senderAboveSubject;


    public MailListRemoteViewFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        mailItems = new ArrayList<>(25);
        senderAboveSubject = K9.messageListSenderAboveSubject();
    }

    @Override
    public void onDataSetChanged() {
        final long identityToken = Binder.clearCallingIdentity();
        mailItems.clear();
        Cursor cursor = context.getContentResolver().query(
                MessageProvider.CONTENT_URI.buildUpon().appendPath("inbox_messages").build(),
                MAIL_LIST_PROJECTIONS,
                null,
                null,
                MessageProvider.MessageColumns.SEND_DATE + " DESC");
        while (cursor.moveToNext()) {
            final String id = cursor.getString(0);
            final String sender = cursor.getString(1);
            final String date = cursor.getString(2);
            final String subject = cursor.getString(3);
            final String preview = cursor.getString(4);
            final String unread = cursor.getString(5);
            final String hasAttachment = cursor.getString(6);
            final String uri = cursor.getString(7);
            mailItems.add(new MailItem(id, sender, date, subject, preview, unread, hasAttachment, uri));
        }
        count = cursor.getCount();
        cursor.close();

        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.mail_list_item);
        MailItem item = mailItems.get(position);

        CharSequence sender = Boolean.valueOf(item.unread) ? bold(item.sender) : item.sender;
        CharSequence subject = Boolean.valueOf(item.unread) ? bold(item.subject) : item.subject;

        /* Populate the views from the mailItem object */
        if (senderAboveSubject) {
            remoteView.setTextViewText(R.id.sender, sender);
            remoteView.setTextViewText(R.id.mail_subject, subject);
        } else {
            remoteView.setTextViewText(R.id.sender, subject);
            remoteView.setTextViewText(R.id.mail_subject, sender);
        }
        remoteView.setTextViewText(R.id.mail_date, item.getDateFormatted("%d %s"));
        remoteView.setTextViewText(R.id.mail_preview, item.preview);

        int textColor = item.getColor();
        remoteView.setTextColor(R.id.sender, textColor);
        remoteView.setTextColor(R.id.mail_subject, textColor);
        remoteView.setTextColor(R.id.mail_date, textColor);
        remoteView.setTextColor(R.id.mail_preview, textColor);

        if (item.hasAttachment()) {
            remoteView.setInt(R.id.attachment, "setVisibility", View.VISIBLE);
        } else {
            remoteView.setInt(R.id.attachment, "setVisibility", View.GONE);
        }

        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, item.uri);
        remoteView.setOnClickFillInIntent(R.id.mail_list_item, intent);
        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        RemoteViews loadingView = new RemoteViews(context.getPackageName(), R.layout.mail_list_loading_view);
        loadingView.setTextViewText(R.id.loadingText, "Loading emails");
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

    private static class MailItem {
        private static Calendar cl = Calendar.getInstance();


        private String id;
        private String date;
        private String sender;
        private String preview;
        private String subject;
        private String unread;
        private String hasAttachment;
        private String uri;


        public MailItem(String id, String sender, String date, String subject, String preview, String unread,
                String hasAttachment, String uri) {
            this.id = id;
            this.sender = sender;
            this.date = date;
            this.preview = preview;
            this.subject = subject;
            this.unread = unread;
            this.uri = uri;
            this.hasAttachment = hasAttachment;
        }

        public int getColor() {
            if (Boolean.valueOf(unread)) {
                return Color.BLACK;
            } else {
                /* light_black */
                return Color.parseColor("#444444");
            }
        }

        public String getDateFormatted(String format) {
            // set default format if null is passed
            if (format.isEmpty()) {
                format = "%d %s";
            }
            cl.setTimeInMillis(Long.valueOf(date));
            return String.format(format,
                    cl.get(Calendar.DAY_OF_MONTH),
                    cl.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()));
        }

        public boolean hasAttachment() {
            return Boolean.valueOf(hasAttachment);
        }
    }
}

