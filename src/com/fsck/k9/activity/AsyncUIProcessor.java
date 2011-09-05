package com.fsck.k9.activity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Application;

/**
 * The class should be used to run long-running processes invoked from the UI that
 * do not affect the Stores.  There are probably pieces of MessagingController
 * that can be moved here.  There is no wakelock used here.  Any network activity, or 
 * true background activity, that is invoked from here should wakelock itself.  UI-centric
 * activity does not need to be wakelocked, as it will simply continue when the phone wakes
 * without disruption.
 *
 */
public class AsyncUIProcessor {

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static AsyncUIProcessor inst = null;
    private AsyncUIProcessor() {
    }
    
   public synchronized static AsyncUIProcessor getInstance(Application application) {
        if (inst == null) {
            inst = new AsyncUIProcessor();
        }
        return inst;
    }
    public void execute(Runnable runnable) {
        threadPool.execute(runnable);
    }
}
