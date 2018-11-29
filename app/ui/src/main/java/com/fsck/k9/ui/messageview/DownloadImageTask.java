package com.fsck.k9.ui.messageview;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import timber.log.Timber;

import android.os.Build;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.ui.R;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.provider.AttachmentProvider.AttachmentProviderColumns;
import org.apache.commons.io.IOUtils;


@Deprecated
class DownloadImageTask extends AsyncTask<Uri, Void, String> {
    private static final String[] ATTACHMENT_PROJECTION = new String[] {
            AttachmentProviderColumns._ID,
            AttachmentProviderColumns.DISPLAY_NAME
    };
    private static final int DISPLAY_NAME_INDEX = 1;

    private static final String DEFAULT_FILE_NAME = "saved_image";


    private final Context context;

    public DownloadImageTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected String doInBackground(Uri... params) {
        String url = params[0].toString();
        Uri outputFile = params[1];

        try {
            boolean isExternalImage = url.startsWith("http");

            String fileName;
            if (isExternalImage) {
                fileName = downloadAndStoreImage(url, outputFile);
            } else {
                fileName = fetchAndStoreImage(url, outputFile);
            }

            return fileName;
        } catch (Exception e) {
            Timber.e(e, "Error while downloading image");
            return null;
        }
    }

    @Override
    protected void onPostExecute(String fileName) {
        boolean errorSavingFile = (fileName == null);

        String text;
        if (errorSavingFile) {
            text = context.getString(R.string.image_saving_failed);
        } else {
            text = context.getString(R.string.image_saved_as, fileName);
        }

        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    private String downloadAndStoreImage(String urlString, Uri outputFile) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        try (InputStream in = conn.getInputStream()) {
            return writeFileToStorage(in, outputFile);
        }
    }

    private String fetchAndStoreImage(String urlString, Uri outputFile) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse(urlString);

        try (InputStream in = contentResolver.openInputStream(uri)) {
            return writeFileToStorage(in, outputFile);
        }
    }

    private String writeFileToStorage(InputStream in, Uri outputFile) throws IOException {
        DocumentFile file = DocumentFile.fromSingleUri(context, outputFile);
        OutputStream out = context.getContentResolver().openOutputStream(outputFile);
        String name = file.getName();

        if (out == null) {
            Timber.e("unable to open outputstream for %s", outputFile.toString());
            return null;
        }

        try {
            IOUtils.copy(in, out);
            out.flush();
        } finally {
            out.close();
        }

        return name;
    }
}
