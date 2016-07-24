package com.fsck.k9;


import android.content.Context;
import android.support.annotation.VisibleForTesting;


public class Globals {
    private static Context context;

    static void setContext(Context context) {
        Globals.context = context;
    }

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("No context provided");
        }

        return context;
    }
}
