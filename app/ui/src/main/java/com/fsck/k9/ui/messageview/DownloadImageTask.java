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
class DownloadImageTask extends AsyncTask<String, Void, String> {
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
    protected String doInBackground(String... params) {
        String url = params[0];
        try {
            boolean isExternalImage = url.startsWith("http");

            String fileName;
            if (isExternalImage) {
                fileName = downloadAndStoreImage(url);
            } else {
                fileName = fetchAndStoreImage(url);
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

    private String downloadAndStoreImage(String urlString) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        InputStream in = conn.getInputStream();
        try {
            String fileName = getFileNameFromUrl(url);
            String mimeType = getMimeType(conn, fileName);

            String fileNameWithExtension = getFileNameWithExtension(fileName, mimeType);
            return writeFileToStorage(fileNameWithExtension, in, mimeType);
        } finally {
            in.close();
        }
    }

    private String getFileNameFromUrl(URL url) {
        String fileName;

        String path = url.getPath();
        int start = path.lastIndexOf("/");
        if (start != -1 && start + 1 < path.length()) {
            fileName = UrlEncodingHelper.decodeUtf8(path.substring(start + 1));
        } else {
            fileName = DEFAULT_FILE_NAME;
        }

        return fileName;
    }

    private String getMimeType(URLConnection conn, String fileName) {
        String mimeType = null;
        if (fileName.indexOf('.') == -1) {
            mimeType = conn.getContentType();
        }

        return mimeType;
    }

    private String fetchAndStoreImage(String urlString) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse(urlString);

        String fileName = getFileNameFromContentProvider(contentResolver, uri);
        String mimeType = getMimeType(contentResolver, uri, fileName);

        InputStream in = contentResolver.openInputStream(uri);
        try {
            String fileNameWithExtension = getFileNameWithExtension(fileName, mimeType);
            return writeFileToStorage(fileNameWithExtension, in, mimeType);
        } finally {
            in.close();
        }
    }

    private String getMimeType(ContentResolver contentResolver, Uri uri, String fileName) {
        String mimeType = null;
        if (fileName.indexOf('.') == -1) {
            mimeType = contentResolver.getType(uri);
        }

        return mimeType;
    }

    private String getFileNameFromContentProvider(ContentResolver contentResolver, Uri uri) {
        String displayName = DEFAULT_FILE_NAME;

        Cursor cursor = contentResolver.query(uri, ATTACHMENT_PROJECTION, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext() && !cursor.isNull(DISPLAY_NAME_INDEX)) {
                    displayName = cursor.getString(DISPLAY_NAME_INDEX);
                }
            } finally {
                cursor.close();
            }
        }

        return displayName;
    }

    private String getFileNameWithExtension(String fileName, String mimeType) {
        if (fileName.indexOf('.') != -1) {
            return fileName;
        }

        // Use JPEG as fallback
        String extension = "jpeg";
        if (mimeType != null) {
            String extensionFromMimeType = MimeUtility.getExtensionByMimeType(mimeType);
            if (extensionFromMimeType != null) {
                extension = extensionFromMimeType;
            }
        }

        return fileName + "." + extension;
    }

    private String writeFileToStorage(String fileName, InputStream in, String mimeType) throws IOException {
        String sanitized = FileHelper.sanitizeFilename(fileName);
        OutputStream out;
        String name;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, Uri.parse(K9.getAttachmentDefaultPath()));
            DocumentFile newFile = FileHelper.createUniqueFile(pickedDir, sanitized, mimeType);

            out = context.getContentResolver().openOutputStream(newFile.getUri());
            name = newFile.getName();


        } else {
            File directory = new File(K9.getAttachmentDefaultPath());
            File file = FileHelper.createUniqueFile(directory, sanitized);

            out = new FileOutputStream(file);
            name = file.getName();
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
