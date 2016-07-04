/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *               2013 Florian Schmaus <flo@geekplace.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.openpgp.util;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;

import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSink;
import org.openintents.openpgp.util.OpenPgpApi.OpenPgpDataSource;


public class ParcelFileDescriptorUtil {

    public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
            throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        new TransferThread(inputStream, new ParcelFileDescriptor.AutoCloseOutputStream(writeSide))
                .start();

        return readSide;
    }

    public static TransferThread pipeTo(OutputStream outputStream, ParcelFileDescriptor output)
            throws IOException {

        AutoCloseInputStream InputStream = new AutoCloseInputStream(output);
        TransferThread t = new TransferThread(InputStream, outputStream);

        t.start();
        return t;
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("IPC Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buf = new byte[4096];
            int len;

            try {
                while ((len = mIn.read(buf)) > 0) {
                    mOut.write(buf, 0, len);
                }
            } catch (IOException e) {
                Log.e(OpenPgpApi.TAG, "IOException when writing to out", e);
            } finally {
                try {
                    mIn.close();
                } catch (IOException ignored) {
                }
                try {
                    mOut.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static <T> DataSinkTransferThread<T> asyncPipeToDataSink(
            OpenPgpDataSink<T> dataSink, ParcelFileDescriptor output) throws IOException {
        InputStream inputStream = new BufferedInputStream(new AutoCloseInputStream(output));
        DataSinkTransferThread<T> dataSinkTransferThread =
                new DataSinkTransferThread<T>(dataSink, inputStream);
        dataSinkTransferThread.start();
        return dataSinkTransferThread;
    }

    static class DataSourceTransferThread extends Thread {
        final OpenPgpDataSource dataSource;
        final OutputStream outputStream;

        DataSourceTransferThread(OpenPgpDataSource dataSource, OutputStream outputStream) {
            super("IPC Transfer Thread (TO service)");
            this.dataSource = dataSource;
            this.outputStream = outputStream;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                dataSource.writeTo(outputStream);
            } catch (IOException e) {
                if (dataSource.isCancelled()) {
                    Log.d(OpenPgpApi.TAG, "Stopped writing because operation was cancelled.");
                } else if (isIOExceptionCausedByEPIPE(e)) {
                    Log.d(OpenPgpApi.TAG, "Stopped writing due to broken pipe (other end closed pipe?)");
                } else {
                    Log.e(OpenPgpApi.TAG, "IOException when writing to out", e);
                }
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static boolean isIOExceptionCausedByEPIPE(IOException e) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return e.getMessage().contains("EPIPE");
        }

        Throwable cause = e.getCause();
        return cause instanceof ErrnoException && ((ErrnoException) cause).errno == OsConstants.EPIPE;
    }

    static class DataSinkTransferThread<T> extends Thread {
        final OpenPgpDataSink<T> dataSink;
        final InputStream inputStream;
        T sinkResult;

        DataSinkTransferThread(OpenPgpDataSink<T> dataSink, InputStream inputStream) {
            super("IPC Transfer Thread (FROM service)");
            this.dataSink = dataSink;
            this.inputStream = inputStream;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                sinkResult = dataSink.processData(inputStream);
            } catch (IOException e) {
                if (isIOExceptionCausedByEPIPE(e)) {
                    Log.e(OpenPgpApi.TAG, "Stopped read due to broken pipe (other end closed pipe?)");
                } else {
                    Log.e(OpenPgpApi.TAG, "IOException while reading from in", e);
                }
                sinkResult = null;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }

        T getResult() {
            if (isAlive()) {
                throw new IllegalStateException("result must be accessed only *after* the thread finished execution!");
            }
            return sinkResult;
        }
    }

}
