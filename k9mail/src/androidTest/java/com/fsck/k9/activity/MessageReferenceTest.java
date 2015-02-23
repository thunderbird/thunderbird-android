package com.fsck.k9.activity;


import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class MessageReferenceTest {
    /**
     * Typically happens during forwards.  (You have a reference, but no flag since we don't currently consider
     * FORWARDED a flag.)
     */
    @Test
    public void testIdentityStringNoFlag() {
        MessageReference mr = new MessageReference();
        mr.accountUuid = "o hai!";
        mr.folderName = "folder";
        mr.uid = "10101010";

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=", mr.toIdentityString());
    }

    /**
     * Typically happens during replies.
     */
    @Test
    public void testIdentityString() {
        MessageReference mr = new MessageReference();
        mr.accountUuid = "o hai!";
        mr.folderName = "folder";
        mr.uid = "10101010";
        mr.flag = Flag.ANSWERED;

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED", mr.toIdentityString());
    }

    @Test
    public void testParseIdentityStringNoFlag() throws MessagingException {
        MessageReference mr = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=");
        assertEquals("o hai!", mr.accountUuid);
        assertEquals("folder", mr.folderName);
        assertEquals("10101010", mr.uid);
        assertNull(mr.flag);
    }

    @Test
    public void testParseIdentityString() throws MessagingException {
        MessageReference mr = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");
        assertEquals("o hai!", mr.accountUuid);
        assertEquals("folder", mr.folderName);
        assertEquals("10101010", mr.uid);
        assertEquals(Flag.ANSWERED, mr.flag);
    }

    @Test
    public void testBadVersion() throws MessagingException {
        MessageReference mr = new MessageReference("@:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");
        assertNull(mr.accountUuid);
    }

    @Test(expected = MessagingException.class)
    public void testNull() throws MessagingException {
        new MessageReference(null);
    }

    @Test(expected = MessagingException.class)
    public void testCorruption() throws MessagingException {
        MessageReference mr = new MessageReference("!:%^&%^*$&$by&(BYWkh:Zm9%^@sZGVy:MT-35#$AxMDEwMTA=:ANSWERED");
        // No idea what this is going to generate, but it should be non-null.
        assertNotNull(mr.accountUuid);
        assertNotNull(mr.folderName);
        assertNotNull(mr.uid);

        // Corruption in the Flag should throw MessagingException.
        new MessageReference("!:%^&%^*$&$by&(BYWkh:Zm9%^@sZGVy:MT-35#$AxMDEwMTA=:ANSWE!RED");
    }

    @Test
    public void notAnInstanceOfMessageReferenceCantEquals() {
        // A MessageReference :
        MessageReference m = new MessageReference();
        // And another Object :
        Object o = new Object();
        // Asserting it is not equals, as not an instance of MessageReference:
        assertFalse(m.equals(o));
    }

    @Test
    public void sameMessageReferenceObjectsAreEquals() {
        // First MessageReference :
        MessageReference m1 = new MessageReference();
        m1.accountUuid = "acc1";
        m1.folderName = "folder1";
        m1.uid = "uid1";
        // Same MessageReference than m1 :
        MessageReference m2 = new MessageReference();
        m2.accountUuid = "acc1";
        m2.folderName = "folder1";
        m2.uid = "uid1";

        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
    }

    @Test
    public void messageReferenceWithAnotherAccountUuidDontEquals() {
        // First MessageReference :
        MessageReference m1 = new MessageReference();
        m1.accountUuid = "acc1";
        // A MessageReference with another accountUuid :
        MessageReference m2 = new MessageReference();
        m2.accountUuid = "acc2";

        assertFalse(m1.equals(m2));
        assertFalse(m2.equals(m1));
    }

    @Test
    public void messageReferenceWithAnotherFolderNameDontEquals() {
        // First MessageReference :
        MessageReference m1 = new MessageReference();
        m1.folderName = "folder1";
        // A MessageReference with another folderName :
        MessageReference m2 = new MessageReference();
        m2.folderName = "folder2";

        assertFalse(m1.equals(m2));
        assertFalse(m2.equals(m1));
    }

    @Test
    public void messageReferenceWithAnotherUidDontEquals() {
        // First MessageReference :
        MessageReference m1 = new MessageReference();
        m1.uid = "uid1";
        // A MessageReference with another uid :
        MessageReference m2 = new MessageReference();
        m2.uid = "uid2";

        assertFalse(m1.equals(m2));
        assertFalse(m2.equals(m1));
    }
}
