package com.fsck.k9.activity;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Application;
import android.os.Environment;

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
public class AsyncUIProcessor
{
    
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private Application mApplication;
    private static AsyncUIProcessor inst = null;
    private AsyncUIProcessor(Application application)
    {
        mApplication = application;
    }
    public synchronized static AsyncUIProcessor getInstance(Application application)
    {
        if (inst == null)
        {
            inst = new AsyncUIProcessor(application);
        }
        return inst;
    }
    public void exportSettings(final String uuid, final String encryptionKey, final ExportListener listener)
    {
        threadPool.execute(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    // Do not store with application files.  Settings exports should *not* be
                    // deleted when the application is uninstalled
                    File dir = new File(Environment.getExternalStorageDirectory() + File.separator 
                            + mApplication.getPackageName());
                    dir.mkdirs();
                    File file = Utility.createUniqueFile(dir, "settings.k9s");
                    String fileName = file.getAbsolutePath();
                    StorageExporter.exportPreferences(mApplication, uuid, fileName, encryptionKey);
                    if (listener != null)
                    {
                        listener.exportSuccess(fileName);
                    }
                }
                catch (Exception e)
                {
                    listener.failure(e.getLocalizedMessage(), e);
                }
            }
        }
        );
        
    }
    public void importSettings(final String fileName, final String encryptionKey, final ImportListener listener)
    {
        threadPool.execute(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    int numAccounts = StorageImporter.importPreferences(mApplication, fileName, encryptionKey);
                    K9.setServicesEnabled(mApplication);
                    if (listener != null)
                    {
                        listener.importSuccess(numAccounts);
                    }
                }
                catch (Exception e)
                {
                    listener.failure(e.getLocalizedMessage(), e);
                }
            }
        }
        );
        
    }
    public void importSettings(final InputStream inputStream, final String encryptionKey, final ImportListener listener)
    {
        threadPool.execute(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    int numAccounts = StorageImporter.importPreferences(mApplication, inputStream, encryptionKey);
                    K9.setServicesEnabled(mApplication);
                    if (listener != null)
                    {
                        listener.importSuccess(numAccounts);
                    }
                }
                catch (Exception e)
                {
                    listener.failure(e.getLocalizedMessage(), e);
                }
            }
        }
        );
        
    }
    
}
