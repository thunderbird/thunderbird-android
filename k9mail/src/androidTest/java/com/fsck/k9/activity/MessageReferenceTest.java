package com.fsck.k9.activity;


import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class MessageReferenceTest {

    @Test
    public void checkIdentityStringFromMessageReferenceWithoutFlag() {
        MessageReference messageReference = createMessageReference("o hai!", "folder", "10101010");

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=", messageReference.toIdentityString());
    }

    @Test
    public void checkIdentityStringFromMessageReferenceWithFlag() {
        MessageReference messageReference =
                createMessageReferenceWithFlag("o hai!", "folder", "10101010", Flag.ANSWERED);

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED", messageReference.toIdentityString());
    }

    @Test
    public void parseIdentityStringWithoutFlag() throws MessagingException {
        MessageReference messageReference = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=");

        assertEquals("o hai!", messageReference.accountUuid);
        assertEquals("folder", messageReference.folderName);
        assertEquals("10101010", messageReference.uid);
        assertNull(messageReference.flag);
    }

    @Test
    public void parseIdentityStringWithFlag() throws MessagingException {
        MessageReference messageReference = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");

        assertEquals("o hai!", messageReference.accountUuid);
        assertEquals("folder", messageReference.folderName);
        assertEquals("10101010", messageReference.uid);
        assertEquals(Flag.ANSWERED, messageReference.flag);
    }

    @Test
    public void parseIdentityStringContainingBadVersionNumber() throws MessagingException {
        MessageReference messageReference = new MessageReference("@:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");

        assertNull(messageReference.accountUuid);
    }

    @Test(expected = MessagingException.class)
    public void parseNullIdentityString() throws MessagingException {
        new MessageReference(null);
    }

    @Test(expected = MessagingException.class)
    public void parseIdentityStringWithCorruptFlag() throws MessagingException {
        new MessageReference("!:%^&%^*$&$by&(BYWkh:Zm9%^@sZGVy:MT-35#$AxMDEwMTA=:ANSWE!RED");
    }

    @Test
    public void equalsWithAnObjectShouldReturnFalse() {
        MessageReference messageReference = new MessageReference();
        Object object = new Object();

        assertFalse(messageReference.equals(object));
    }

    @SuppressWarnings("ObjectEqualsNull")
    @Test
    public void equalsWithNullShouldReturnFalse() {
        MessageReference messageReference = createMessageReference("account", "folder", "uid");

        assertFalse(messageReference.equals(null));
    }

    @Test
    public void equalsWithSameMessageReferenceShouldReturnTrue() {
        MessageReference messageReference = createMessageReference("account", "folder", "uid");

        assertTrue(messageReference.equals(messageReference));
    }

    @Test
    public void equalsWithMessageReferenceContainingSameDataShouldReturnTrue() {
        MessageReference messageReferenceOne = createMessageReference("account", "folder", "uid");
        MessageReference messageReferenceTwo = createMessageReference("account", "folder", "uid");

        assertEqualsReturnsTrueSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentAccountUuidShouldReturnFalse() {
        MessageReference messageReferenceOne = createMessageReference("account", "folder", "uid");
        MessageReference messageReferenceTwo = createMessageReference("-------", "folder", "uid");

        assertEqualsReturnsFalseSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentFolderNameShouldReturnFalse() {
        MessageReference messageReferenceOne = createMessageReference("account", "folder", "uid");
        MessageReference messageReferenceTwo = createMessageReference("account", "------", "uid");

        assertEqualsReturnsFalseSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentUidShouldReturnFalse() {
        MessageReference messageReferenceOne = createMessageReference("account", "folder", "uid");
        MessageReference messageReferenceTwo = createMessageReference("account", "folder", "---");

        assertEqualsReturnsFalseSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    private MessageReference createMessageReference(String accountUuid, String folderName, String uid) {
        MessageReference messageReference = new MessageReference();
        messageReference.accountUuid = accountUuid;
        messageReference.folderName = folderName;
        messageReference.uid = uid;

        return messageReference;
    }

    private MessageReference createMessageReferenceWithFlag(String accountUuid, String folderName, String uid,
            Flag flag) {
        MessageReference messageReference = new MessageReference();
        messageReference.accountUuid = accountUuid;
        messageReference.folderName = folderName;
        messageReference.uid = uid;
        messageReference.flag = flag;

        return messageReference;
    }

    private void assertEqualsReturnsTrueSymmetrically(MessageReference referenceOne, MessageReference referenceTwo) {
        assertTrue(referenceOne.equals(referenceTwo));
        assertTrue(referenceTwo.equals(referenceOne));
    }

    private void assertEqualsReturnsFalseSymmetrically(MessageReference referenceOne, MessageReference referenceTwo) {
        assertFalse(referenceOne.equals(referenceTwo));
        assertFalse(referenceTwo.equals(referenceOne));
    }
}
