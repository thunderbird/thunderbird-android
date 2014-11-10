package com.fsck.k9.helper;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import java.io.File;


public class MediaScannerNotifier implements MediaScannerConnectionClient {
    private MediaScannerConnection mConnection;
    private File mFile;

    public MediaScannerNotifier(Context context, File file) {
        mFile = file;
        mConnection = new MediaScannerConnection(context, this);
        mConnection.connect();
    }

    public void onMediaScannerConnected() {
        mConnection.scanFile(mFile.getAbsolutePath(), null);
    }

    public void onScanCompleted(String path, Uri uri) {
        mConnection.disconnect();
    }
}
