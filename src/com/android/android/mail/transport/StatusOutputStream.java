package com.fsck.k9.mail.transport;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.fsck.k9.k9;

import android.util.Config;
import android.util.Log;

public class StatusOutputStream extends FilterOutputStream {
    private long mCount = 0;
    
    public StatusOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int oneByte) throws IOException {
        super.write(oneByte);
        mCount++;
        if (Config.LOGV) {
            if (mCount % 1024 == 0) {
                Log.v(k9.LOG_TAG, "# " + mCount);
            }
        }
    }
}
