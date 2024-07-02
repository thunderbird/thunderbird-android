package com.fsck.k9.controller;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.fsck.k9.mail.DefaultBodyFactory;
import org.apache.commons.io.output.CountingOutputStream;


class ProgressBodyFactory extends DefaultBodyFactory {
    private final ProgressListener progressListener;


    ProgressBodyFactory(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    protected void copyData(InputStream inputStream, OutputStream outputStream) throws IOException {
        Timer timer = new Timer();
        try (CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream)) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    progressListener.updateProgress(countingOutputStream.getCount());
                }
            }, 0, 50);

            super.copyData(inputStream, countingOutputStream);
        } finally {
            timer.cancel();
        }
    }

    interface ProgressListener {
        void updateProgress(int progress);
    }
}
