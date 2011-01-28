package com.fsck.k9.helper;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import java.io.File;


public class MediaScannerNotifier implements MediaScannerConnectionClient
{
    private MediaScannerConnection mConnection;
    private File mFile;
    private Context mContext;

    public MediaScannerNotifier(Context context, File file)
    {
        mFile = file;
        mConnection = new MediaScannerConnection(context, this);
        mConnection.connect();
        mContext = context;

    }

    public void onMediaScannerConnected()
    {
        mConnection.scanFile(mFile.getAbsolutePath(), null);
    }

    public void onScanCompleted(String path, Uri uri)
    {
        try
        {
            if (uri != null)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                mContext.startActivity(intent);
            }
        }
        finally
        {
            mConnection.disconnect();
        }
    }
}
