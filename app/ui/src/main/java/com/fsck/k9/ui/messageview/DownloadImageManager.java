package com.fsck.k9.ui.messageview;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.ui.R;

import timber.log.Timber;

public class DownloadImageManager {
    private DownloadManager downloadManager;
    private Context context;
    private long downloadId;

    public DownloadImageManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.context.registerReceiver(downloadReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);

                Cursor c = downloadManager.query(query);
                if (c.moveToFirst()) {
                    int columnIndex = c
                            .getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c
                            .getInt(columnIndex)) {

                        String name = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                        String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        Timber.i(uriString);
                        Uri fileUri = downloadManager.getUriForDownloadedFile(downloadId);
                        String mimeType = downloadManager.getMimeTypeForDownloadedFile(downloadId);

                        downloadFinished(fileUri, mimeType, name);
                    }
                }
            }
        }
    };

    private void downloadImage(String url) {
        boolean isExternalImage = url.startsWith("http");

        String fileName;
        if (isExternalImage) {
            downloadExternalImage(url);
        } else {
         //   new DownloadImageTask(context).execute(new );
        }
    }

    private void downloadExternalImage(String url) {
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(url));
        request.setDescription("Iamge download").setTitle("Notification titile");
        request.setVisibleInDownloadsUi(false);

        final long dlId = downloadManager.enqueue(request);
    }

    private void downloadFinished(Uri fileUri, String mimeType, String displayName) {
        AttachmentViewInfo info = new AttachmentViewInfo(mimeType, displayName, 0, fileUri, true, null, true);

        boolean errorSavingFile = (fileUri == null);

        String text;
        if (errorSavingFile) {
            text = context.getString(R.string.image_saving_failed);
        } else {
            text = context.getString(R.string.image_saved_as, fileUri.toString());
        }

        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

}
