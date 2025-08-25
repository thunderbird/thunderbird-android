package com.fsck.k9.provider;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import net.thunderbird.core.logging.legacy.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mailstore.util.FileFactory;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.util.MimeUtil;
import org.openintents.openpgp.util.ParcelFileDescriptorUtil;


public class DecryptedFileProvider extends FileProvider {
    private static final String DECRYPTED_CACHE_DIRECTORY = "decrypted";
    private static final long FILE_DELETE_THRESHOLD_MILLISECONDS = 3 * 60 * 1000;
    private static final Object cleanupReceiverMonitor = new Object();

    private static String AUTHORITY;
    private static DecryptedFileProviderCleanupReceiver cleanupReceiver = null;


    @Override
    public boolean onCreate() {
        String packageName = getContext().getPackageName();
        AUTHORITY = packageName + ".decryptedfileprovider";
        return true;
    }

    public static FileFactory getFileFactory(Context context) {
        final Context applicationContext = context.getApplicationContext();

        return new FileFactory() {
            @Override
            public File createFile() throws IOException {
                registerFileCleanupReceiver(applicationContext);
                File decryptedTempDirectory = getDecryptedTempDirectory(applicationContext);
                return File.createTempFile("decrypted-", null, decryptedTempDirectory);
            }
        };
    }

    @Nullable
    public static Uri getUriForProvidedFile(@NonNull Context context, File file,
            @Nullable String encoding, @Nullable String mimeType) {
        try {
            Uri.Builder uriBuilder = FileProvider.getUriForFile(context, AUTHORITY, file).buildUpon();
            if (mimeType != null) {
                uriBuilder.appendQueryParameter("mime_type", mimeType);
            }
            if (encoding != null) {
                uriBuilder.appendQueryParameter("encoding", encoding);
            }
            return uriBuilder.build();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean deleteOldTemporaryFiles(Context context) {
        File tempDirectory = getDecryptedTempDirectory(context);
        boolean allFilesDeleted = true;
        long deletionThreshold = System.currentTimeMillis() - FILE_DELETE_THRESHOLD_MILLISECONDS;
        for (File tempFile : tempDirectory.listFiles()) {
            long lastModified = tempFile.lastModified();
            if (lastModified < deletionThreshold) {
                boolean fileDeleted = tempFile.delete();
                if (!fileDeleted) {
                    Log.e("Failed to delete temporary file");
                    // TODO really do this? might cause our service to stay up indefinitely if a file can't be deleted
                    allFilesDeleted = false;
                }
            } else {
                if (K9.isDebugLoggingEnabled()) {
                    String timeLeftStr = String.format(
                            Locale.ENGLISH, "%.2f", (lastModified - deletionThreshold) / 1000 / 60.0);
                    Log.e("Not deleting temp file (for another %s minutes)", timeLeftStr);
                }
                allFilesDeleted = false;
            }
        }

        return allFilesDeleted;
    }

    private static File getDecryptedTempDirectory(Context context) {
        File directory = new File(context.getCacheDir(), DECRYPTED_CACHE_DIRECTORY);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                Log.e("Error creating directory: %s", directory.getAbsolutePath());
            }
        }

        return directory;
    }


    @Override
    public String getType(Uri uri) {
        return uri.getQueryParameter("mime_type");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor pfd = super.openFile(uri, "r");

        InputStream decodedInputStream;
        String encoding = uri.getQueryParameter("encoding");
        if (MimeUtil.isBase64Encoding(encoding)) {
            InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            decodedInputStream = new Base64InputStream(inputStream);
        } else if (MimeUtil.isQuotedPrintableEncoded(encoding)) {
            InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            decodedInputStream = new QuotedPrintableInputStream(inputStream);
        } else { // no or unknown encoding
            if (!TextUtils.isEmpty(encoding)) {
                Log.e("unsupported encoding, returning raw stream");
            }
            return pfd;
        }

        try {
            return ParcelFileDescriptorUtil.pipeFrom(decodedInputStream);
        } catch (IOException e) {
            // not strictly a FileNotFoundException, but failure to create a pipe is basically "can't access right now"
            throw new FileNotFoundException();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level < TRIM_MEMORY_COMPLETE) {
            return;
        }
        final Context context = getContext();
        if (context == null) {
            return;
        }

        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                deleteOldTemporaryFiles(context);
                return null;
            }
        }.execute();

        unregisterFileCleanupReceiver(context);
    }

    private static void unregisterFileCleanupReceiver(Context context) {
        synchronized (cleanupReceiverMonitor) {
            if (cleanupReceiver == null) {
                return;
            }

            Log.d("Unregistering temp file cleanup receiver");
            context.unregisterReceiver(cleanupReceiver);
            cleanupReceiver = null;
        }
    }

    private static void registerFileCleanupReceiver(Context context) {
        synchronized (cleanupReceiverMonitor) {
            if (cleanupReceiver != null) {
                return;
            }

            Log.d("Registering temp file cleanup receiver");
            cleanupReceiver = new DecryptedFileProviderCleanupReceiver();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            ContextCompat.registerReceiver(context, cleanupReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    private static class DecryptedFileProviderCleanupReceiver extends BroadcastReceiver {
        @Override
        @MainThread
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                throw new IllegalArgumentException("onReceive called with action that isn't screen off!");
            }

            Log.d("Cleaning up temp files");

            boolean allFilesDeleted = deleteOldTemporaryFiles(context);
            if (allFilesDeleted) {
                unregisterFileCleanupReceiver(context);
            }
        }
    }
}
