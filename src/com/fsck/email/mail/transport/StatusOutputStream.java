package com.android.email.mail.transport;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.android.email.Email;

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
                Log.v(Email.LOG_TAG, "# " + mCount);
            }
        }
    }
}
