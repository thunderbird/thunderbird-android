package com.fsck.k9.controller;


import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


public class MessageReferenceTest {

    @Test
    public void checkIdentityStringFromMessageReference() {
        MessageReference messageReference = createMessageReference("o hai!", 2, "10101010");

        assertEquals("#:byBoYWkh:Mg==:MTAxMDEwMTA=", messageReference.toIdentityString());
    }

    @Test
    public void parseIdentityString() {
        MessageReference messageReference = MessageReference.parse("#:byBoYWkh:Mg==:MTAxMDEwMTA=");

        assertNotNull(messageReference);
        assertEquals("o hai!", messageReference.getAccountUuid());
        assertEquals(2, messageReference.getFolderId());
        assertEquals("10101010", messageReference.getUid());
    }

    @Test
    public void checkMessageReferenceWithChangedUid() {
        MessageReference messageReferenceOne = createMessageReference("account", 1, "uid");
        
        MessageReference messageReferenceTwo = messageReferenceOne.withModifiedUid("---");

        assertEquals("account", messageReferenceTwo.getAccountUuid());
        assertEquals(1, messageReferenceTwo.getFolderId());
        assertEquals("---", messageReferenceTwo.getUid());
    }

    @Test
    public void parseIdentityStringContainingBadVersionNumber() {
        MessageReference messageReference = MessageReference.parse("@:byBoYWkh:MTAxMDEwMTA=");

        assertNull(messageReference);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void parseNullIdentityString() {
        MessageReference messageReference = MessageReference.parse(null);

        assertNull(messageReference);
    }

    @Test
    public void equalsWithAnObjectShouldReturnFalse() {
        MessageReference messageReference = new MessageReference("a", 1, "c");
        Object object = new Object();

        assertFalse(messageReference.equals(object));
    }

    @SuppressWarnings({"ObjectEqualsNull", "ConstantConditions"})
    @Test
    public void equalsWithNullShouldReturnFalse() {
        MessageReference messageReference = createMessageReference("account", 1, "uid");

        assertFalse(messageReference.equals(null));
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    public void equalsWithSameMessageReferenceShouldReturnTrue() {
        MessageReference messageReference = createMessageReference("account", 1, "uid");

        assertTrue(messageReference.equals(messageReference));
    }

    @Test
    public void equalsWithMessageReferenceContainingSameDataShouldReturnTrue() {
        MessageReference messageReferenceOne = createMessageReference("account", 1, "uid");
        MessageReference messageReferenceTwo = createMessageReference("account", 1, "uid");

        assertEqualsReturnsTrueSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentAccountUuidShouldReturnFalse() {
        MessageReference messageReferenceOne = createMessageReference("account", 1, "uid");
        MessageReference messageReferenceTwo = createMessageReference("-------", 1, "uid");

        assertEqualsReturnsFalseSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentFolderNameShouldReturnFalse() {
        MessageReference messageReferenceOne = createMessageReference("account", 1, "uid");
        MessageReference messageReferenceTwo = createMessageReference("account", 8, "uid");

        assertEqualsReturnsFalseSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentUidShouldReturnFalse() {
        MessageReference messageReferenceOne = createMessageReference("account", 1, "uid");
        MessageReference messageReferenceTwo = createMessageReference("account", 1, "---");

        assertEqualsReturnsFalseSymmetrically(messageReferenceOne, messageReferenceTwo);
    }

    @Test
    public void alternativeEquals() {
        MessageReference messageReference = createMessageReference("account", 1, "uid");

        boolean equalsResult = messageReference.equals("account", 1, "uid");

        assertTrue(equalsResult);
    }

    @Test
    public void equals_withNullAccount_shouldReturnFalse() {
        MessageReference messageReference = createMessageReference("account", 1, "uid");

        boolean equalsResult = messageReference.equals(null, 1, "uid");

        assertFalse(equalsResult);
    }

    @Test
    public void equals_withNullUid_shouldReturnFalse() {
        MessageReference messageReference = createMessageReference("account", 1, "uid");

        boolean equalsResult = messageReference.equals("account", 1, null);

        assertFalse(equalsResult);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_withNullAccount_shouldThrow() {
        createMessageReference(null, 1, "uid");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_withNullUid_shouldThrow() {
        createMessageReference("account", 1, null);
    }

    private MessageReference createMessageReference(String accountUuid, long folderId, String uid) {
        return new MessageReference(accountUuid, folderId, uid);
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
