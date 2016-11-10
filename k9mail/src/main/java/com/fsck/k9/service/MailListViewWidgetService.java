package com.fsck.k9.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Image;
import android.os.Binder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;
import com.fsck.k9.R;
import com.fsck.k9.provider.MessageProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class MailListViewWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MailListRemoteViewFactory(this.getApplicationContext(), intent);
    }
}

class MailListRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {
    private static String [] MAIL_LIST_PROJECTIONS = {
            MessageProvider.MessageColumns._ID,
            MessageProvider.MessageColumns.SENDER,
            MessageProvider.MessageColumns.SEND_DATE,
            MessageProvider.MessageColumns.SUBJECT,
            MessageProvider.MessageColumns.PREVIEW,
            MessageProvider.MessageColumns.UNREAD,
            MessageProvider.MessageColumns.HAS_ATTACHMENTS,
            MessageProvider.MessageColumns.URI
    };

    private Context mContext;
    private ArrayList<MailItem> mailItems;
    private int count;

    public MailListRemoteViewFactory(Context context, Intent intent) {
        mContext = context;
    }

    @Override
    public void onCreate() {
        mailItems = new ArrayList<>(25);
    }

    @Override
    public void onDataSetChanged() {
        final long identityToken = Binder.clearCallingIdentity();
        mailItems = new ArrayList<>(25);
        Cursor cursor = mContext.getContentResolver().query(
                MessageProvider.CONTENT_URI.buildUpon().appendPath("inbox_messages").build(),
                MAIL_LIST_PROJECTIONS,
                null,
                null,
                MessageProvider.MessageColumns.SEND_DATE + "DESC");
        while (cursor.moveToNext()) {
            mailItems.add(new MailItem(
                        cursor.getString(0), /* id */
                        cursor.getString(1), /* sender */
                        cursor.getString(2), /* date */
                        cursor.getString(3), /* subject */
                        cursor.getString(4), /* preview */
                        cursor.getString(5), /* unread */
                        cursor.getString(6), /* hasAttachment */
                        cursor.getString(7)) /* uri */
            );
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
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.mail_list_item);
        MailItem item = mailItems.get(position);

        rv.setFloat(R.id.sender, "setTextSize", 18f);
        rv.setTextViewText(R.id.sender, item.sender);
        rv.setTextColor(R.id.sender, item.getColor());

        rv.setFloat(R.id.mail_subject, "setTextSize", 15f);
        rv.setTextViewText(R.id.mail_subject, item.subject);
        rv.setTextColor(R.id.mail_subject, item.getColor());

        rv.setInt(R.id.mail_date, "setTextColor", Color.CYAN);
        rv.setTextViewText(R.id.mail_date, item.getDateFormatted("%d %s"));

        rv.setFloat(R.id.mail_preview, "setTextSize", 13f);
        rv.setTextViewText(R.id.mail_preview, item.preview);
        rv.setTextColor(R.id.mail_preview, item.getColor());
        if (item.hasAttachment()) {
            rv.setInt(R.id.attachment, "setVisibility", View.VISIBLE);
        } else {
            rv.setInt(R.id.attachment, "setVisibility", View.GONE);
        }

        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, item.uri);
        rv.setOnClickFillInIntent(R.id.mail_list_item, intent);
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private static class MailItem {
        private String id;
        private String date;
        private String sender;
        private String preview;
        private String subject;
        private String unread;
        private String hasAttachment;
        private String uri;

        private static Calendar cl = Calendar.getInstance();

        public MailItem(String id, String sender, String date, String subject,
                        String preview, String unread, String hasAttachment, String uri) {
            this.id = id;
            this.sender = sender;
            this.date = date;
            this.preview = preview;
            this.subject = subject;
            this.unread = unread;
            this.uri = uri;
            this.hasAttachment = hasAttachment;
        }
        private static Calendar cl = Calendar.getInstance();

        public MailItem(String id, String sender, String date, String subject,
                        String preview, String unread, String hasAttachment, String uri) {
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
                return Color.GRAY;
            } else {
                return Color.WHITE;
            }
        }

        public String getDateFormatted(String format) {
            // set default format if null is passed
            if (format == null || format.isEmpty()) {
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
<<<<<<< HEAD

=======
>>>>>>> 7dc9dcc5d... Display an attachement icon if the mail has attachment.
    }
}

