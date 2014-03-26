/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.fsck.k9.mail.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;

import android.util.Log;

/**
 * This is a mock Transport that is used to test protocols that use MailTransport.
 */
public class MockTransport implements BaseTransport {

    private String getSourcePosition(int n) {
        StackTraceElement caller = new Throwable().getStackTrace()[n];

        StringBuilder sb = new StringBuilder();
        sb.append(caller.getClassName())
                .append(".").append(caller.getMethodName())
                .append("(")
                .append(caller.getFileName())
                .append(":").append(caller.getLineNumber())
                .append(")");
        return sb.toString();
    }

    // All flags defining debug or development code settings must be FALSE
    // when code is checked in or released.
    private static boolean DEBUG_LOG_STREAMS = true;

    private static String LOG_TAG = "MockTransport";

    private static final String SPECIAL_RESPONSE_IOEXCEPTION = "!!!IOEXCEPTION!!!";

    private boolean mTlsStarted = false;

    private MockOutputStream mOut;
    private boolean mOpen;
    private boolean mInputOpen;
    private InetAddress mLocalAddress;

    private boolean mSecure = false;

    private ArrayList<String> mQueuedInput = new ArrayList<String>();

    private static class Transaction {
        public static final int ACTION_INJECT_TEXT = 0;
        public static final int ACTION_CLIENT_CLOSE = 1;
        public static final int ACTION_IO_EXCEPTION = 2;
        public static final int ACTION_START_TLS = 3;

        int mAction;
        String mPattern;
        String[] mResponses;
        String mCaller;

        Transaction(String caller, String pattern, String[] responses) {
            mAction = ACTION_INJECT_TEXT;
            mPattern = pattern;
            mResponses = responses;
            mCaller = caller;
        }

        Transaction(String caller, int otherType) {
            mAction = otherType;
            mPattern = null;
            mResponses = null;
            mCaller = caller;
        }

        @Override
        public String toString() {
            switch (mAction) {
                case ACTION_INJECT_TEXT:
                    return mPattern + ": " + Arrays.toString(mResponses);
                case ACTION_CLIENT_CLOSE:
                    return "Expect the client to close";
                case ACTION_IO_EXCEPTION:
                    return "Expect IOException";
                case ACTION_START_TLS:
                    return "Expect StartTls";
                default:
                    return "(Hmm.  Unknown action.)";
            }
        }
    }

    private ArrayList<Transaction> mPairs = new ArrayList<Transaction>();

    /**
     * Give the mock a pattern to wait for.  No response will be sent.
     * @param pattern Java RegEx to wait for
     */
    public void expect(String pattern) {
        expect(getSourcePosition(2), pattern, (String[])null);
    }

    /**
     * Give the mock a pattern to wait for and a response to send back.
     * @param pattern Java RegEx to wait for
     * @param response String to reply with, or null to acccept string but not respond to it
     */
    public void expect(String pattern, String response) {
        expect(getSourcePosition(2), pattern, (response == null) ? null : new String[] { response });
    }

    /**
     * Give the mock a pattern to wait for and a multi-line response to send back.
     * @param pattern Java RegEx to wait for
     * @param responses Strings to reply with
     */
    public void expect(String pattern, String[] response) {
        expect(getSourcePosition(2), pattern, response);
    }

    private void expect(String caller, String pattern, String[] responses) {
        Transaction pair = new Transaction(caller, pattern, responses);
        mPairs.add(pair);
    }

    /**
     * Same as {@link #expect(String, String[])}, but the first arg is taken literally, rather than
     * as a regexp.
     */
    public void expectLiterally(String literal, String[] responses) {
        expect(getSourcePosition(2), "^" + Pattern.quote(literal) + "$", responses);
    }

    /**
     * Tell the Mock Transport that we expect it to be closed.  This will preserve
     * the remaining entries in the expect() stream and allow us to "ride over" the close (which
     * would normally reset everything).
     */
    public void expectClose() {
        mPairs.add(new Transaction(getSourcePosition(2), Transaction.ACTION_CLIENT_CLOSE));
    }

    public void expectIOException() {
        mPairs.add(new Transaction(getSourcePosition(2), Transaction.ACTION_IO_EXCEPTION));
    }

    public void expectStartTls() {
        mPairs.add(new Transaction(getSourcePosition(2), Transaction.ACTION_START_TLS));
    }

    private void sendResponse(Transaction pair) {
        switch (pair.mAction) {
            case Transaction.ACTION_INJECT_TEXT:
                for (String s : pair.mResponses) {
                    mQueuedInput.add(s);
                }
                break;
            case Transaction.ACTION_IO_EXCEPTION:
                mQueuedInput.add(SPECIAL_RESPONSE_IOEXCEPTION);
                break;
            default:
                Assert.fail("Invalid action for sendResponse: " + pair.mAction);
        }
    }

    /**
     * This simulates a condition where the server has closed its side, causing
     * reads to fail.
     */
    public void closeInputStream() {
        mInputOpen = false;
    }

    @Override
    public void close() {
        mOut = null;
        mOpen = false;
        mInputOpen = false;
        // unless it was expected as part of a test, reset the stream
        if (mPairs.size() > 0) {
            Transaction expect = mPairs.remove(0);
            if (expect.mAction == Transaction.ACTION_CLIENT_CLOSE) {
                return;
            }
        }
        mQueuedInput.clear();
        mPairs.clear();
    }

    public void setMockLocalAddress(InetAddress address) {
        mLocalAddress = address;
    }

    @Override
    public InputStream getInputStream() {
        Assert.assertTrue(mOpen);
        return new MockInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        Assert.assertTrue(mOpen);
        return mOut;
    }

    @Override
    public boolean isOpen() {
        return mOpen;
    }

    @Override
    public void open() /* throws MessagingException, CertificateValidationException */ {
        mOpen = true;
        mInputOpen = true;
        mOut = new MockOutputStream();
    }

    /**
     * This returns one string (if available) to the caller. Usually this simply
     * pulls strings
     * from the mQueuedInput list, but if the list is empty, we also peek the
     * expect list. This
     * supports banners, multi-line responses, and any other cases where we
     * respond without
     * a specific expect pattern.
     *
     * If no response text is available, we assert (failing our test) as an
     * underflow.
     *
     * Logs the read text if DEBUG_LOG_STREAMS is true.
     */
    @Override
    public String readLine() throws IOException {
        SmtpTransportUnitTests.assertTrue(mOpen);
        if (!mInputOpen) {
            throw new IOException("Reading from MockTransport with closed input");
        }
        // if there's nothing to read, see if we can find a null-pattern response
        if ((mQueuedInput.size() == 0) && (mPairs.size() > 0)) {
            Transaction pair = mPairs.get(0);
            if (pair.mPattern == null) {
                mPairs.remove(0);
                sendResponse(pair);
            }
        }
        if (mQueuedInput.size() == 0) {
            // MailTransport returns "" at EOS.
            Log.w(LOG_TAG, "Underflow reading from MockTransport");
            throw new IOException("Reading from MockTransport with closed input");
        }
        String line = mQueuedInput.remove(0);
        if (DEBUG_LOG_STREAMS) {
            Log.d(LOG_TAG, "<<< " + line);
        }
        if (SPECIAL_RESPONSE_IOEXCEPTION.equals(line)) {
            throw new IOException("Expected IOException.");
        }
        return line;
    }

    @Override
    public void setSoTimeout(int timeoutMilliseconds) /* throws SocketException */{
    }

    /**
     * Accepts a single string (command or text) that was written by the code under test.
     * Because we are essentially mocking a server, we check to see if this string was expected.
     * If the string was expected, we push the corresponding responses into the mQueuedInput
     * list, for subsequent calls to readLine().  If the string does not match, we assert
     * the mismatch.  If no string was expected, we assert it as an overflow.
     *
     * Logs the written text if DEBUG_LOG_STREAMS is true.
     */
    @Override
    public void writeLine(byte[] bytes) throws IOException {
        mOut.writeLine(bytes);
    }

    @Override
    public void writeLine(byte[] data, boolean appendCrLf) throws IOException {
        mOut.writeLine(data);
        if (appendCrLf) {
            mOut.write('\r');
            mOut.write('\n');
        }
    }

    @Override
    public void write(int oneByte) throws IOException {
        mOut.write(oneByte);
    }

    /**
     * This is an InputStream that satisfies the needs of getInputStream()
     */
    private class MockInputStream extends InputStream {

        private byte[] mNextLine = null;
        private int mNextIndex = 0;

        /**
         * Reads from the same input buffer as readLine()
         */
        @Override
        public int read() throws IOException {
            if (!mInputOpen) {
                throw new IOException();
            }

            if (mNextLine != null && mNextIndex < mNextLine.length) {
                return mNextLine[mNextIndex++];
            }

            // previous line was exhausted so try to get another one
            String next = readLine();
            if (next == null) {
                throw new IOException("Reading from MockTransport with closed input");
            }
            mNextLine = (next + "\r\n").getBytes();
            mNextIndex = 0;

            if (mNextLine != null && mNextIndex < mNextLine.length) {
                return mNextLine[mNextIndex++];
            }

            // no joy - throw an exception
            throw new IOException();
        }
    }

    /**
     * This is an OutputStream that satisfies the needs of getOutputStream()
     */
    private class MockOutputStream extends OutputStream {

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private boolean mIsAlreadyAssertionFailed = false;

        private void writeLineInternal(byte[] bytes) throws IOException {
            if (mIsAlreadyAssertionFailed) {
                // prevent the method being called again
                return;
            }

            try {
                String s = new String(bytes, "ISO-8859-1");
                if (DEBUG_LOG_STREAMS) {
                    Log.d(LOG_TAG, ">>> " + s);
                }
                SmtpTransportUnitTests.assertTrue(mOpen);
                SmtpTransportUnitTests.assertTrue("Overflow writing to MockTransport: Getting " + s,
                        0 != mPairs.size());
                Transaction pair = mPairs.remove(0);
                if (pair.mAction == Transaction.ACTION_IO_EXCEPTION) {
                    throw new IOException("Expected IOException.");
                }
                SmtpTransportUnitTests.assertTrue("Unexpected string written to MockTransport: Actual=[" + s + "]"
                        + "  Expected=[" + pair.mPattern + "]"
                        + "\nexpected at " + pair.mCaller,
                        pair.mPattern != null && s.matches(pair.mPattern));
                if (pair.mResponses != null) {
                    sendResponse(pair);
                }
            }
            catch (AssertionFailedError e) {
                mIsAlreadyAssertionFailed = true;
                throw e;
            }
        }

        public void writeLine(byte[] bytes) throws IOException {
            for (int c : bytes) {
                write(c);
            }
        }

        @Override
        public void write(int oneByte) throws IOException {
            // CR or CRLF will immediately dump previous line (w/o CRLF)
            if (oneByte == '\r') {
                byte[] out = baos.toByteArray();
                baos.reset();

                writeLineInternal(out);
            } else if (oneByte == '\n') {
                // swallow it
            } else {
                baos.write(oneByte);
            }
        }
    }

    @Override
    public String getLocalHost() {
        return mLocalAddress.toString();
    }

    @Override
    public void connect(String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException, MessagingException {
    }

    @Override
    public void connect2(String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException {
    }

    @Override
    public void reopenTls(String host, int port) throws IOException {
        mSecure = true;
    }

    @Override
    public void useCompression() throws IOException {
    }

    @Override
    public MockTransport clone() {
        return new MockTransport();
    }

    @Override
    public boolean isSecure() {
        return mSecure;
    }
}
