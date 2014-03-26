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

package com.fsck.k9.mail.store;

import java.util.List;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.transport.MockTransport;

/**
 * This is a series of unit tests for the POP3 Store class.  These tests must be locally
 * complete - no server(s) required.
 */
@SmallTest
public class Pop3StoreUnitTests extends AndroidTestCase {
    final String UNIQUE_ID_1 = "20080909002219r1800rrjo9e00";

    final static int PER_MESSAGE_SIZE = 100;

    /* These values are provided by setUp() */
    private Pop3Store mStore = null;
    private Pop3Store.Pop3Folder mFolder = null;

    /**
     * Setup code.  We generate a lightweight Pop3Store and Pop3Store.Pop3Folder.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();

        Account testAccount = new Account(mContext);
        testAccount.setStoreUri("pop3://user:password@server:999");

        mStore = new Pop3Store(testAccount);
        mFolder = (Pop3Store.Pop3Folder) mStore.getFolder("INBOX");
    }

    /**
     * Confirms simple non-SSL non-TLS login
     */
    public void testSimpleLogin() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        // try to open it
        setupOpenFolder(mockTransport, 0, null);
        mFolder.open(Folder.OPEN_MODE_RO);
    }

    /**
     * TODO: Test with SSL negotiation (faked)
     * TODO: Test with SSL required but not supported
     * TODO: Test with TLS negotiation (faked)
     * TODO: Test with TLS required but not supported
     * TODO: Test calling getMessageCount(), getMessages(), etc.
     */

    /**
     * Test the operation of checkSettings(), which requires (a) a good open and (b) UIDL support.
     */
    public void testCheckSettings() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        // scenario 1:  CAPA returns -ERR, so we try UIDL explicitly
        setupOpenFolder(mockTransport, 0, null);
        mockTransport.expect("UIDL", "+OK sending UIDL list");
        mockTransport.expect("QUIT", "");
        mStore.checkSettings();

        // scenario 2:  CAPA indicates UIDL, so we don't try UIDL
        setupOpenFolder(mockTransport, 0, "UIDL");
        mockTransport.expect("QUIT", "");
        mStore.checkSettings();

        // scenario 3:  CAPA returns -ERR, and UIDL fails
        try {
            setupOpenFolder(mockTransport, 0, null);
            mockTransport.expect("UIDL", "-ERR unsupported");
            mockTransport.expect("QUIT", "");
            mStore.checkSettings();
            fail("MessagingException was expected due to UIDL unsupported.");
        } catch (MessagingException me) {
            // this is expected, so eat it
        }
    }

    /**
     * Test a strange case that causes open to proceed without mCapabilities
     *  open - fail with "-" error code
     *  then check capabilities
     */
    public void testCheckSettingsCapabilities() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        // First, preload an open that fails for some reason
        mockTransport.expect(null, "-ERR from the Mock Transport.");

        // And watch it fail
        try {
            Pop3Store.Pop3Folder folder = mStore.new Pop3Folder("INBOX");
            folder.open(Folder.OPEN_MODE_RW);
            fail("Should have thrown exception");
        } catch (MessagingException me) {
            // Expected - continue.
        }

        // Now try again (assuming a slightly different connection setup - successful)
        // Note, checkSettings is going to try to close the connection again, so we expect
        // one extra QUIT before we spin it up again
        mockTransport.expect("QUIT", "");
        mockTransport.expectClose();
        setupOpenFolder(mockTransport, 0, "UIDL");
        mockTransport.expect("QUIT", "");
        mStore.checkSettings();
    }

    /**
     * Test small Store & Folder functions that manage folders & namespace
     *
     * @throws MessagingException
     */
    public void testStoreFoldersFunctions() throws MessagingException {

        // getPersonalNamespaces() always returns INBOX folder
        List<? extends Folder> folders = mStore.getPersonalNamespaces(true);
        assertEquals(1, folders.size());
        assertSame(mFolder, folders.get(0));

        // getName() returns the name we were created with.  If "inbox", converts to INBOX
        assertEquals("INBOX", mFolder.getName());
        Pop3Store.Pop3Folder folderMixedCaseInbox = mStore.new Pop3Folder("iNbOx");
        assertEquals("INBOX", folderMixedCaseInbox.getName());
        Pop3Store.Pop3Folder folderNotInbox = mStore.new Pop3Folder("NOT-INBOX");
        assertEquals("NOT-INBOX", folderNotInbox.getName());

        // exists() true if name is INBOX
        assertTrue(mFolder.exists());
        assertTrue(folderMixedCaseInbox.exists());
        assertFalse(folderNotInbox.exists());
    }

    /**
     * Test small Folder functions that don't really do anything in Pop3
     *
     * @throws MessagingException
     */
    public void testSmallFolderFunctions() throws MessagingException {

        // getMode() returns OpenMode.READ_WRITE
        assertEquals(Folder.OPEN_MODE_RW, mFolder.getMode());

        assertFalse(mFolder.create(FolderType.HOLDS_FOLDERS));
        assertFalse(mFolder.create(FolderType.HOLDS_MESSAGES));

        // getUnreadMessageCount() always returns -1
        assertEquals(-1, mFolder.getUnreadMessageCount());

        // appendMessages(Message[] messages) does nothing
        mFolder.appendMessages(null);

        // delete(boolean recurse) does nothing
        // TODO - it should!
        mFolder.delete(false);

        // expunge() no returns
        mFolder.expunge();

        // copyMessages() is unsupported
        assertNull(mFolder.copyMessages(null, null));
    }

    /**
     * Test the process of opening and indexing a mailbox with one unread message in it.
     *
     * TODO should create an instrumented listener to confirm all expected callbacks.  Then use
     * it everywhere we could have passed a message listener.
     */
    public void testOneUnread() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        checkOneUnread(mockTransport);
    }

    /**
     * Test the process of opening and getting message by uid.
     */
    public void testGetMessageByUid() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        setupOpenFolder(mockTransport, 2, null);
        mFolder.open(Folder.OPEN_MODE_RW);
        // check message count
        assertEquals(2, mFolder.getMessageCount());

        // setup 2 messages
        setupUidlSequence(mockTransport, 2);
        String uid1 = getSingleMessageUID(1);
        String uid2 = getSingleMessageUID(2);
        String uid3 = getSingleMessageUID(3);

        Message msg1 = mFolder.getMessage(uid1);
        assertTrue("message with uid1", msg1 != null);

        // uid3 does not exist. return new Pop3Message()
        Message msg3 = mFolder.getMessage(uid3);
        assertTrue("message with uid3", msg3 != null);

        Message msg2 = mFolder.getMessage(uid2);
        assertTrue("message with uid2", msg2 != null);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we are simulating the steps of
     * MessagingController.synchronizeMailboxSyncronous() and we will inject the failure a bit
     * further along in each case, to test various recovery points.
     *
     * This test confirms that Pop3Store needs to call close() in the IOExceptionHandler in
     * Pop3Folder.getMessages(), due to a closure before the UIDL command completes.
     */
    public void testCatchClosed1a() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        openFolderWithMessage(mockTransport);

        // cause the next sequence to fail on the readLine() calls
        mockTransport.closeInputStream();

        // index the message(s) - it should fail, because our stream is broken
        try {
            setupUidlSequence(mockTransport, 1);
            Message[] messages = mFolder.getMessages(1, 1, null, null);
            assertEquals(1, messages.length);
            assertEquals(getSingleMessageUID(1), messages[0].getUid());
            fail("Broken stream should cause getMessages() to throw.");
        } catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we are simulating the steps of
     * MessagingController.synchronizeMailboxSyncronous() and we will inject the failure a bit
     * further along in each case, to test various recovery points.
     *
     * This test confirms that Pop3Store needs to call close() in the IOExceptionHandler in
     * Pop3Folder.getMessages(), due to non-numeric data in a multi-line UIDL.
     */
    public void testCatchClosed1b() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        openFolderWithMessage(mockTransport);

        // index the message(s) - it should fail, because our stream is broken
        try {
            // setupUidlSequence(mockTransport, 1);
            mockTransport.expect("UIDL", "+OK sending UIDL list");
            mockTransport.expect(null, "bad-data" + " " + "THE-UIDL");
            mockTransport.expect(null, ".");

            Message[] messages = mFolder.getMessages(1, 1, null, null);
            fail("Bad UIDL should cause getMessages() to throw.");
        } catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // TODO: k9 do not close yet
        mockTransport.close();

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we are simulating the steps of
     * MessagingController.synchronizeMailboxSyncronous() and we will inject the failure a bit
     * further along in each case, to test various recovery points.
     *
     * This test confirms that Pop3Store needs to call close() in the IOExceptionHandler in
     * Pop3Folder.getMessages(), due to non-numeric data in a single-line UIDL.
     */
    public void testCatchClosed1c() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        // openFolderWithMessage(mockTransport);
        setupOpenFolder(mockTransport, 6000, null);
        mFolder.open(Folder.OPEN_MODE_RO);
        assertEquals(6000, mFolder.getMessageCount());

        // index the message(s) - it should fail, because our stream is broken
        try {
            // setupUidlSequence(mockTransport, 1);
            mockTransport.expect("UIDL 1", "+OK " + "bad-data" + " " + "THE-UIDL");

            Message[] messages = mFolder.getMessages(1, 1, null, null);
            fail("Bad UIDL should cause getMessages() to throw.");
        } catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // TODO: k9 do not close yet
        mockTransport.close();

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we are simulating the steps of
     * MessagingController.synchronizeMailboxSyncronous() and we will inject the failure a bit
     * further along in each case, to test various recovery points.
     *
     * This test confirms that Pop3Store needs to call close() in the first IOExceptionHandler in
     * Pop3Folder.fetch(), for a failure in the call to indexUids().
     */
    public void testCatchClosed2() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 1);
        Message[] messages = mFolder.getMessages(1, 1, null, null);
        assertEquals(1, messages.length);
        assertEquals(getSingleMessageUID(1), messages[0].getUid());

        // cause the next sequence to fail on the readLine() calls
        mockTransport.closeInputStream();

        try {
            // try the basic fetch of flags & envelope
            setupListSequence(mockTransport, 1);
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(FetchProfile.Item.ENVELOPE);
            mFolder.fetch(messages, fp, null);
            assertEquals(PER_MESSAGE_SIZE, messages[0].getSize());
            fail("Broken stream should cause fetch() to throw.");
        }
        catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we have to check additional places where
     * Pop3Store and/or Pop3Folder should be dealing with IOErrors.
     *
     * This test confirms that Pop3Store needs to call close() in the first IOExceptionHandler in
     * Pop3Folder.fetch(), for a failure in the call to fetchEnvelope().
     */
    public void testCatchClosed2a() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 1);
        Message[] messages = mFolder.getMessages(1, 1, null, null);
        assertEquals(1, messages.length);
        assertEquals(getSingleMessageUID(1), messages[0].getUid());

        // try the basic fetch of flags & envelope, but the LIST command fails
        setupBrokenListSequence(mockTransport, 1);
        try {
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(FetchProfile.Item.ENVELOPE);
            mFolder.fetch(messages, fp, null);
            assertEquals(PER_MESSAGE_SIZE, messages[0].getSize());
            fail("Broken stream should cause fetch() to throw.");
        } catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // TODO: k9 do not close yet
        mockTransport.close();

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we are simulating the steps of
     * MessagingController.synchronizeMailboxSyncronous() and we will inject the failure a bit
     * further along in each case, to test various recovery points.
     *
     * This test confirms that Pop3Store needs to call close() in the second IOExceptionHandler in
     * Pop3Folder.fetch().
     */
    public void testCatchClosed3() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 1);
        Message[] messages = mFolder.getMessages(1, 1, null, null);
        assertEquals(1, messages.length);
        assertEquals(getSingleMessageUID(1), messages[0].getUid());

        // try the basic fetch of flags & envelope
        setupListSequence(mockTransport, 1);
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.FLAGS);
        fp.add(FetchProfile.Item.ENVELOPE);
        mFolder.fetch(messages, fp, null);
        assertEquals(PER_MESSAGE_SIZE, messages[0].getSize());

        // cause the next sequence to fail on the readLine() calls
        mockTransport.closeInputStream();

        try {
            // now try fetching the message
            setupSingleMessage(mockTransport, 1, false);
            fp = new FetchProfile();
            fp.add(FetchProfile.Item.BODY);
            mFolder.fetch(messages, fp, null);
            checkFetchedMessage(messages[0], 1, false);
            fail("Broken stream should cause fetch() to throw.");
        }
        catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we have to check additional places where
     * Pop3Store and/or Pop3Folder should be dealing with IOErrors.
     *
     * This test confirms that Pop3Store needs to call close() in the IOExceptionHandler in
     * Pop3Folder.setFlags().
     */
    public void testCatchClosed4() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 1);
        Message[] messages = mFolder.getMessages(1, 1, null, null);
        assertEquals(1, messages.length);
        assertEquals(getSingleMessageUID(1), messages[0].getUid());

        // cause the next sequence to fail on the readLine() calls
        mockTransport.closeInputStream();

        // delete 'em all - should fail because of broken stream
        try {
            mockTransport.expect("DELE 1", "+OK message deleted");
            mFolder.setFlags(messages, new Flag[] { Flag.DELETED }, true);
            fail("Broken stream should cause fetch() to throw.");
        }
        catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll just repeat the above steps, without the failure.
        // NOTE: everything from here down is copied from testOneUnread() and should be consolidated

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we have to check additional places where
     * Pop3Store and/or Pop3Folder should be dealing with IOErrors.
     *
     * This test confirms that Pop3Store needs to call close() in the first IOExceptionHandler in
     * Pop3Folder.open().
     */
    public void testCatchClosed5() {
        // TODO cannot write this test until we can inject stream closures mid-sequence
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we have to check additional places where
     * Pop3Store and/or Pop3Folder should be dealing with IOErrors.
     *
     * This test confirms that Pop3Store needs to call close() in the second IOExceptionHandler in
     * Pop3Folder.open() (when it calls STAT and the response is empty of garbagey).
     */
    public void testCatchClosed6a() throws MessagingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        // like openFolderWithMessage(mockTransport) but with a broken STAT report (empty response)
        setupOpenFolder(mockTransport, -1, null);
        try {
            mFolder.open(Folder.OPEN_MODE_RO);
            fail("Broken STAT should cause open() to throw.");
        } catch(MessagingException me) {
            // success
        }

        // At this point the UI would display connection error, which is fine.  Now, the real
        // test is, can we recover?  So I'll try a new connection, without the failure.

        // TODO: k9 do not close yet
        mockTransport.close();

        // confirm that we're closed at this point
        assertFalse("folder should be 'closed' after an IOError", mFolder.isOpen());

        // and confirm that the next connection will be OK
        checkOneUnread(mockTransport);
    }

    /**
     * Test the scenario where the transport is "open" but not really (e.g. server closed).  Two
     * things should happen:  We should see an intermediate failure that makes sense, and the next
     * operation should reopen properly.
     *
     * There are multiple versions of this test because we have to check additional places where
     * Pop3Store and/or Pop3Folder should be dealing with IOErrors.
     *
     * This test confirms that Pop3Store needs to call close() in the second IOExceptionHandler in
     * Pop3Folder.open() (when it calls STAT, and there is no response at all).
     */
    public void testCatchClosed6b() {
        // TODO cannot write this test until we can inject stream closures mid-sequence
    }

    /**
     * Given an initialized mock transport, open it and attempt to "read" one unread message from
     * it.  This can be used as a basic test of functionality and it should be possible to call this
     * repeatedly (if you close the folder between calls).
     *
     * @param mockTransport the mock transport we're using
     */
    private void checkOneUnread(MockTransport mockTransport) throws MessagingException {
        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 1);
        Message[] messages = mFolder.getMessages(1, 1, null, null);
        assertEquals(1, messages.length);
        assertEquals(getSingleMessageUID(1), messages[0].getUid());

        // try the basic fetch of flags & envelope
        setupListSequence(mockTransport, 1);
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.FLAGS);
        fp.add(FetchProfile.Item.ENVELOPE);
        mFolder.fetch(messages, fp, null);
        assertEquals(PER_MESSAGE_SIZE, messages[0].getSize());

        // A side effect of how messages work is that if you get fields that are empty,
        // then empty arrays are written back into the parsed header fields (e.g. mTo, mFrom).  The
        // standard message parser needs to clear these before parsing.  Make sure that this
        // is happening.  (This doesn't affect IMAP, which reads the headers directly via
        // IMAP evelopes.)
        MimeMessage message = (MimeMessage) messages[0];
        message.getRecipients(RecipientType.TO);
        message.getRecipients(RecipientType.CC);
        message.getRecipients(RecipientType.BCC);

        // now try fetching the message
        setupSingleMessage(mockTransport, 1, false);
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY);
        mFolder.fetch(messages, fp, null);
        checkFetchedMessage(messages[0], 1, false);
    }

    /**
     * A group of tests to confirm that we're properly juggling the RETR and TOP commands.
     * Some servers (hello, live.com) support TOP but don't support CAPA.  So we ignore CAPA
     * and just try TOP.
     */
    public void testRetrVariants() throws MessagingException {
        MockTransport mockTransport = openAndInjectMockTransport();
        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 2);
        Message[] messages = mFolder.getMessages(1, 2, null, null);
        assertEquals(2, messages.length);

        // basic fetch of flags & envelope
        setupListSequence(mockTransport, 2);
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.FLAGS);
        fp.add(FetchProfile.Item.ENVELOPE);
        mFolder.fetch(messages, fp, null);

        // A side effect of how messages work is that if you get fields that are empty,
        // then empty arrays are written back into the parsed header fields (e.g. mTo, mFrom).  The
        // standard message parser needs to clear these before parsing.  Make sure that this
        // is happening.  (This doesn't affect IMAP, which reads the headers directly via
        // IMAP envelopes.)
        for (Message message : messages) {
            message.getRecipients(RecipientType.TO);
            message.getRecipients(RecipientType.CC);
            message.getRecipients(RecipientType.BCC);
        }

        // In the cases below, we fetch BODY_SANE which tries to load the first chunk of the
        // message (not the entire thing) in order to quickly access the headers.
        // In the first test, TOP succeeds
        Message[] singleMessage = new Message[] { messages[0] };
        setupSingleMessageTop(mockTransport, 1, true, true);        // try TOP & succeed
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY_SANE);
        mFolder.fetch(singleMessage, fp, null);
        checkFetchedMessage(singleMessage[0], 1, false);

        singleMessage[0] = messages[1];
        setupSingleMessageTop(mockTransport, 2, true, true);        // try TOP & succeed
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY_SANE);
        mFolder.fetch(singleMessage, fp, null);
        checkFetchedMessage(singleMessage[0], 2, false);
    }

    /**
     * In the 2nd test, TOP fails, so we should fall back to RETR
     */
    public void testRetrVariantsFailTop() throws MessagingException {
        MockTransport mockTransport = openAndInjectMockTransport();
        openFolderWithMessage(mockTransport);

        // index the message(s)
        setupUidlSequence(mockTransport, 2);
        Message[] messages = mFolder.getMessages(1, 2, null, null);
        assertEquals(2, messages.length);

        // basic fetch of flags & envelope
        setupListSequence(mockTransport, 2);
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.FLAGS);
        fp.add(FetchProfile.Item.ENVELOPE);
        mFolder.fetch(messages, fp, null);

        // A side effect of how messages work is that if you get fields that are empty,
        // then empty arrays are written back into the parsed header fields (e.g. mTo, mFrom).  The
        // standard message parser needs to clear these before parsing.  Make sure that this
        // is happening.  (This doesn't affect IMAP, which reads the headers directly via
        // IMAP envelopes.)
        for (Message message : messages) {
            message.getRecipients(RecipientType.TO);
            message.getRecipients(RecipientType.CC);
            message.getRecipients(RecipientType.BCC);
        }

        // In the cases below, we fetch BODY_SANE which tries to load the first chunk of the
        // message (not the entire thing) in order to quickly access the headers.
        // In the first test, TOP succeeds
        Message[] singleMessage = new Message[] { messages[0] };

        // In the 2nd test, TOP fails, so we should fall back to RETR
        setupSingleMessageTop(mockTransport, 1, true, false);        // try TOP & fail
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY_SANE);
        mFolder.fetch(singleMessage, fp, null);
        checkFetchedMessage(singleMessage[0], 1, false);

        singleMessage[0] = messages[1];
        setupSingleMessageTop(mockTransport, 2, false, false);
        fp = new FetchProfile();
        fp.add(FetchProfile.Item.BODY_SANE);
        mFolder.fetch(singleMessage, fp, null); // already marked TOP is failed, so use RETR
        checkFetchedMessage(singleMessage[0], 2, false);
    }

    /**
     * Set up a basic MockTransport. open it, and inject it into mStore
     */
    private MockTransport openAndInjectMockTransport() {
        // Create mock transport and inject it into the POP3Store that's already set up
        MockTransport mockTransport = new MockTransport();
        mStore.setTransport(mockTransport);
        return mockTransport;
    }

    /**
     * Open a folder that's preloaded with one unread message.
     *
     * @param mockTransport the mock transport we're using
     */
    private void openFolderWithMessage(MockTransport mockTransport) throws MessagingException {
        // try to open it
        setupOpenFolder(mockTransport, 1, null);
        mFolder.open(Folder.OPEN_MODE_RO);

        // check message count
        assertEquals(1, mFolder.getMessageCount());
    }

    /**
     * Look at a fetched message and confirm that it is complete.
     *
     * TODO this needs to be more dynamic, not just hardcoded for empty message #1.
     *
     * @param message the fetched message to be checked
     * @param msgNum the message number
     */
    private void checkFetchedMessage(Message message, int msgNum, boolean body)
            throws MessagingException {
        // check To:
        Address[] to = message.getRecipients(RecipientType.TO);
        assertNotNull(to);
        assertEquals(1, to.length);
        assertEquals("Smith@Registry.Org", to[0].getAddress());
        assertNull(to[0].getPersonal());

        // check From:
        Address[] from = message.getFrom();
        assertNotNull(from);
        assertEquals(1, from.length);
        assertEquals("Jones@Registry.Org", from[0].getAddress());
        assertNull(from[0].getPersonal());

        // check Cc:
        Address[] cc = message.getRecipients(RecipientType.CC);
        assertNotNull(cc);
        assertEquals(1, cc.length);
        assertEquals("Chris@Registry.Org", cc[0].getAddress());
        assertNull(cc[0].getPersonal());

        // check Reply-To:
        Address[] replyto = message.getReplyTo();
        assertNotNull(replyto);
        assertEquals(1, replyto.length);
        assertEquals("Roger@Registry.Org", replyto[0].getAddress());
        assertNull(replyto[0].getPersonal());

        // TODO date

        // TODO check body (if applicable)
    }

    /**
     * Helper which stuffs the mock with enough strings to satisfy a call to Pop3Folder.open()
     *
     * @param mockTransport the mock transport we're using
     * @param statCount the number of messages to indicate in the STAT, or -1 for broken STAT
     * @param capabilities if non-null, comma-separated list of capabilities
     */
    private void setupOpenFolder(MockTransport mockTransport, int statCount, String capabilities) {
        mockTransport.expect(null, "+OK Hello there from the Mock Transport.");

        mockTransport.expect("AUTH", "+OK Listing of supported mechanisms follows");
        mockTransport.expect(null, "CRAM-MD5");
        mockTransport.expect(null, "LOGIN");
        mockTransport.expect(null, ".");

        if (capabilities == null) {
            mockTransport.expect("CAPA", "-ERR unimplemented");
        } else {
            mockTransport.expect("CAPA", "+OK capabilities follow");
            mockTransport.expect(null, capabilities.split(","));        // one capability per line
            mockTransport.expect(null, ".");                            // terminated by "."
        }

        mockTransport.expect("USER user", "+OK User name accepted");
        mockTransport.expect("PASS password", "+OK Logged in");

        if (statCount == -1) {
            mockTransport.expect("STAT", "");
        } else {
            String stat = "+OK " + Integer.toString(statCount) + " "
                    + Integer.toString(PER_MESSAGE_SIZE * statCount);
            mockTransport.expect("STAT", stat);
        }
    }

    /**
     * Setup expects for a UIDL on a mailbox with 0 or more messages in it.
     * @param transport The mock transport to preload
     * @param numMessages The number of messages to return from UIDL.
     */
    private static void setupUidlSequence(MockTransport transport, int numMessages) {
        transport.expect("UIDL", "+OK sending UIDL list");
        for (int msgNum = 1; msgNum <= numMessages; ++msgNum) {
            transport.expect(null, Integer.toString(msgNum) + " " + getSingleMessageUID(msgNum));
        }
        transport.expect(null, ".");
    }

    /**
     * Setup expects for a LIST on a mailbox with 0 or more messages in it.
     * @param transport The mock transport to preload
     * @param numMessages The number of messages to return from LIST.
     */
    private static void setupListSequence(MockTransport transport, int numMessages) {
        transport.expect("LIST", "+OK sending scan listing");
        for (int msgNum = 1; msgNum <= numMessages; ++msgNum) {
            transport.expect(null, Integer.toString(msgNum) + " " +
                    Integer.toString(PER_MESSAGE_SIZE * msgNum));
        }
        transport.expect(null, ".");
    }

    /**
     * Setup expects for a LIST on a mailbox with 0 or more messages in it, except that
     * this time the pipe fails, and we return empty lines.
     * @param transport The mock transport to preload
     * @param numMessages The number of messages to return from LIST.
     */
    private static void setupBrokenListSequence(MockTransport transport, int numMessages) {
        transport.expect("LIST", "");
        for (int msgNum = 1; msgNum <= numMessages; ++msgNum) {
            transport.expect(null, "");
        }
        transport.expect(null, "");
    }

    /**
     * Setup a single message to be retrieved.
     *
     * Per RFC822 here is a minimal message header:
     *     Date:     26 Aug 76 1429 EDT
     *     From:     Jones@Registry.Org
     *     To:       Smith@Registry.Org
     *
     * We'll add the following fields to support additional tests:
     *     Cc:       Chris@Registry.Org
     *     Reply-To: Roger@Registry.Org
     *
     * @param transport the mock transport to preload
     * @param msgNum the message number to expect and return
     * @param body if true, a non-empty body will be added
     */
    private static void setupSingleMessage(MockTransport transport, int msgNum, boolean body) {
        setupSingleMessageTop(transport, msgNum, false, false);
    }

    /**
     * Setup a single message to be retrieved (headers only).
     * This is very similar to setupSingleMessage() but is intended to test the BODY_SANE
     * fetch mode.
     * @param transport the mock transport
     * @param msgNum the message number to expect and return
     * @param topTry if true, the "client" is going to attempt the TOP command
     * @param topSupported if true, the "server" supports the TOP command
     */
    private static void setupSingleMessageTop(MockTransport transport, int msgNum,
            boolean topTry, boolean topSupported) {
        String msgNumString = Integer.toString(msgNum);
        String topCommand = "TOP " + msgNumString + " " + (32768 / 76);
        String retrCommand = "RETR " + msgNumString;

        if (topTry) {
            if (topSupported) {
                transport.expect(topCommand, "+OK message follows");
            } else {
                transport.expect(topCommand, "-ERR unsupported command");
                transport.expect(retrCommand, "+OK message follows");
            }
        } else {
            transport.expect(retrCommand, "+OK message follows");
        }

        transport.expect(null, "Date: 26 Aug 76 1429 EDT");
        transport.expect(null, "From: Jones@Registry.Org");
        transport.expect(null, "To:   Smith@Registry.Org");
        transport.expect(null, "CC:   Chris@Registry.Org");
        transport.expect(null, "Reply-To: Roger@Registry.Org");
        transport.expect(null, "");
        transport.expect(null, ".");
    }

    /**
     * Generates a simple unique code for each message.  Repeatable.
     * @param msgNum The message number
     * @return a string that can be used as the UID
     */
    private static String getSingleMessageUID(int msgNum) {
        final String UID_HEAD = "ABCDEF-";
        final String UID_TAIL = "";
        return UID_HEAD + Integer.toString(msgNum) + UID_TAIL;
    }
}
