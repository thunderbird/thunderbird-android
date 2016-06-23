package com.fsck.k9.provider;


import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.service.FileProviderInterface;


public class DecryptedFileProvider extends FileProvider {
    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".decryptedfileprovider";
    private static final String DECRYPTED_CACHE_DIRECTORY = "decrypted";


    public static final long FILE_DELETE_THRESHOLD_MILLISECONDS = 15 * 60 * 1000;


    @Override
    public String getType(Uri uri) {
        return uri.getQueryParameter("mime_type");
    }

    public static FileProviderInterface getFileProviderInterface(final Context context) {
        return new FileProviderInterface() {
            @Override
            public File createProvidedFile() throws IOException {
                File decryptedTempDirectory = getDecryptedTempDirectory(context);
                return File.createTempFile("decrypted-", null, decryptedTempDirectory);
            }

            @Override
            public Uri getUriForProvidedFile(File file, String mimeType) throws IOException {
                Uri uri = FileProvider.getUriForFile(context, AUTHORITY, file);
                return uri.buildUpon().appendQueryParameter("mime_type", mimeType).build();
            }
        };
    }

    public static boolean deleteOldTemporaryFiles(Context context) {
        File tempDirectory = getDecryptedTempDirectory(context);
        boolean allFilesDeleted = true;
        for (File tempFile : tempDirectory.listFiles()) {
            if (tempFile.lastModified() < new Date().getTime() - FILE_DELETE_THRESHOLD_MILLISECONDS) {
                boolean fileDeleted = tempFile.delete();
                if (!fileDeleted) {
                    Log.e(K9.LOG_TAG, "Failed to delete temporary file");
                    // TODO really do this? might cause our service to stay up indefinitely if a file can't be deleted
                    allFilesDeleted = false;
                }
            } else {
                allFilesDeleted = false;
            }
        }

        return allFilesDeleted;
    }

    private static File getDecryptedTempDirectory(Context context) {
        File directory = new File(context.getCacheDir(), DECRYPTED_CACHE_DIRECTORY);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                Log.e(K9.LOG_TAG, "Error creating directory: " + directory.getAbsolutePath());
            }
        }

        return directory;
    }
}
