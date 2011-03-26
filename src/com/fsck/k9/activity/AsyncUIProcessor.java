package com.fsck.k9.activity;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashSet;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.preferences.StorageExporter;
import com.fsck.k9.preferences.StorageImporter;

/**
 * The class should be used to run long-running processes invoked from the UI that
 * do not affect the Stores.  There are probably pieces of MessagingController
 * that can be moved here.
 *
 */
public class AsyncUIProcessor {

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private Application mApplication;
    private static AsyncUIProcessor inst = null;
    private AsyncUIProcessor(Application application) {
        mApplication = application;
    }
    public synchronized static AsyncUIProcessor getInstance(Application application) {
        if (inst == null) {
            inst = new AsyncUIProcessor(application);
        }
        return inst;
    }
    public void execute(Runnable runnable) {
        threadPool.execute(runnable);
    }
    public void exportSettings(final Activity activity, final String storageFormat, final HashSet<String> accountUuids, final ExportListener listener) {
        threadPool.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    // Do not store with application files.  Settings exports should *not* be
                    // deleted when the application is uninstalled
                    File dir = new File(Environment.getExternalStorageDirectory() + File.separator
                                        + mApplication.getPackageName());
                    dir.mkdirs();
                    File file = Utility.createUniqueFile(dir, "settings.k9s");
                    String fileName = file.getAbsolutePath();
                    StorageExporter.exportPreferences(activity, storageFormat, accountUuids, fileName, null, listener);
                } catch (Exception e) {
                    Log.w(K9.LOG_TAG, "Exception during export", e);
                    listener.failure(e.getLocalizedMessage(), e);
                }
            }
        }
                          );

    }

    public void importSettings(final Activity activity, final Uri uri, final ImportListener listener) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                try {
                    ContentResolver resolver = mApplication.getContentResolver();
                    is = resolver.openInputStream(uri);
                } catch (Exception e) {
                    Log.w(K9.LOG_TAG, "Exception while resolving Uri to InputStream", e);
                    if (listener != null) {
                        listener.failure(e.getLocalizedMessage(), e);
                    }
                    return;
                }
                final InputStream myIs = is;
                StorageImporter.importPreferences(activity, is, null, new ImportListener() {
                    @Override
                    public void failure(String message, Exception e) {
                        quietClose(myIs);
                        if (listener != null) {
                            listener.failure(message, e);
                        }
                    }

                    @Override
                    public void success(int numAccounts) {
                        quietClose(myIs);
                        if (listener != null) {
                            listener.success(numAccounts);
                        }
                    }

                    @Override
                    public void canceled() {
                        quietClose(myIs);
                        if (listener != null) {
                            listener.canceled();
                        }
                    }

                    @Override
                    public void started() {
                        if (listener != null) {
                            listener.started();
                        }
                    }
                });
            }
        }
                          );
    }

    private void quietClose(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (Exception e) {
                Log.w(K9.LOG_TAG, "Unable to close inputStream", e);
            }
        }
    }


}
