package com.fsck.k9.provider;


import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.MainThread;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.fsck.k9.BuildConfig;
import com.fsck.k9.K9;
import com.fsck.k9.service.FileProviderInterface;


public class DecryptedFileProvider extends FileProvider {
    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".decryptedfileprovider";
    private static final String DECRYPTED_CACHE_DIRECTORY = "decrypted";
    private static final long FILE_DELETE_THRESHOLD_MILLISECONDS = 3 * 60 * 1000;


    private static DecryptedFileProviderCleanupReceiver receiverRegistered = null;


    @Override
    public String getType(Uri uri) {
        return uri.getQueryParameter("mime_type");
    }

    public static FileProviderInterface getFileProviderInterface(Context context) {
        final Context applicationContext = context.getApplicationContext();

        return new FileProviderInterface() {
            @Override
            public File createProvidedFile() throws IOException {
                registerFileCleanupReceiver(applicationContext);
                File decryptedTempDirectory = getDecryptedTempDirectory(applicationContext);
                return File.createTempFile("decrypted-", null, decryptedTempDirectory);
            }

            @Override
            public Uri getUriForProvidedFile(File file, String mimeType) throws IOException {
                Uri uri = FileProvider.getUriForFile(applicationContext, AUTHORITY, file);
                return uri.buildUpon().appendQueryParameter("mime_type", mimeType).build();
            }
        };
    }

    public static boolean deleteOldTemporaryFiles(Context context) {
        File tempDirectory = getDecryptedTempDirectory(context);
        boolean allFilesDeleted = true;
        long deletionThreshold = new Date().getTime() - FILE_DELETE_THRESHOLD_MILLISECONDS;
        for (File tempFile : tempDirectory.listFiles()) {
            long lastModified = tempFile.lastModified();
            if (lastModified < deletionThreshold) {
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

    @Override
    public void onTrimMemory(int level) {
        if (level < TRIM_MEMORY_COMPLETE) {
            return;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }

        deleteOldTemporaryFiles(context);
        if (receiverRegistered != null) {
            context.unregisterReceiver(receiverRegistered);
            receiverRegistered = null;
        }
    }

    @MainThread // no need to synchronize for receiverRegistered
    private static void registerFileCleanupReceiver(Context context) {
        if (receiverRegistered != null) {
            return;
        }
        receiverRegistered = new DecryptedFileProviderCleanupReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(receiverRegistered, intentFilter);
    }

    private static class DecryptedFileProviderCleanupReceiver extends BroadcastReceiver {
        @Override
        @MainThread
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                throw new IllegalArgumentException("onReceive called with action that isn't screen off!");
            }

            boolean allFilesDeleted = deleteOldTemporaryFiles(context);
            if (allFilesDeleted) {
                context.unregisterReceiver(this);
                receiverRegistered = null;
            }
        }
    }
}
