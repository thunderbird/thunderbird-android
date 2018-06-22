package org.openintents.openpgp.util;


import java.util.concurrent.Executor;

import android.content.Intent;
import android.os.AsyncTask;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowAsyncTask;


@Implements(OpenPgpApi.OpenPgpAsyncTask.class)
public class ShadowOpenPgpAsyncTask extends ShadowAsyncTask<Void, Integer, Intent> {

    @RealObject
    private OpenPgpApi.OpenPgpAsyncTask realAsyncTask;

    @Implementation
    public AsyncTask<Void, Integer, Intent> executeOnExecutor(Executor executor, Void... params) {
        return super.execute(params);
    }
}