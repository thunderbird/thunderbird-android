package com.fsck.k9.ui.messageview;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.FileHelper;
import com.fsck.k9.helper.UrlEncodingHelper;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.provider.AttachmentProvider.AttachmentProviderColumns;
import org.apache.commons.io.IOUtils;


class DownloadImageTask extends AsyncTask<String, Void, String> {
    private static final String[] ATTACHMENT_PROJECTION = new String[] {
            AttachmentProviderColumns._ID,
            AttachmentProviderColumns.DISPLAY_NAME
    };
    private static final int DISPLAY_NAME_INDEX = 1;


    private final Context context;

    public DownloadImageTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected String doInBackground(String... params) {
        String urlString = params[0];
        try {
            boolean externalImage = urlString.startsWith("http");

            String filename = null;
            String mimeType = null;
            InputStream in = null;

            try {
                if (externalImage) {
                    URL url = new URL(urlString);
                    URLConnection conn = url.openConnection();
                    in = conn.getInputStream();

                    String path = url.getPath();

                    // Try to get the filename from the URL
                    int start = path.lastIndexOf("/");
                    if (start != -1 && start + 1 < path.length()) {
                        filename = UrlEncodingHelper.decodeUtf8(path.substring(start + 1));
                    } else {
                        // Use a dummy filename if necessary
                        filename = "saved_image";
                    }

                    // Get the MIME type if we couldn't find a file extension
                    if (filename.indexOf('.') == -1) {
                        mimeType = conn.getContentType();
                    }
                } else {
                    ContentResolver contentResolver = context.getContentResolver();
                    Uri uri = Uri.parse(urlString);

                    // Get the filename from AttachmentProvider
                    Cursor cursor = contentResolver.query(uri, ATTACHMENT_PROJECTION, null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToNext()) {
                                filename = cursor.getString(DISPLAY_NAME_INDEX);
                            }
                        } finally {
                            cursor.close();
                        }
                    }

                    // Use a dummy filename if necessary
                    if (filename == null) {
                        filename = "saved_image";
                    }

                    // Get the MIME type if we couldn't find a file extension
                    if (filename.indexOf('.') == -1) {
                        mimeType = contentResolver.getType(uri);
                    }

                    in = contentResolver.openInputStream(uri);
                }

                // Do we still need an extension?
                if (filename.indexOf('.') == -1) {
                    // Use JPEG as fallback
                    String extension = "jpeg";
                    if (mimeType != null) {
                        // Try to find an extension for the given MIME type
                        String ext = MimeUtility.getExtensionByMimeType(mimeType);
                        if (ext != null) {
                            extension = ext;
                        }
                    }
                    filename += "." + extension;
                }

                String sanitized = FileHelper.sanitizeFilename(filename);

                File directory = new File(K9.getAttachmentDefaultPath());
                File file = FileHelper.createUniqueFile(directory, sanitized);
                FileOutputStream out = new FileOutputStream(file);
                try {
                    IOUtils.copy(in, out);
                    out.flush();
                } finally {
                    out.close();
                }

                return file.getName();

            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String filename) {
        String text;
        if (filename == null) {
            text = context.getString(R.string.image_saving_failed);
        } else {
            text = context.getString(R.string.image_saved_as, filename);
        }

        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}
