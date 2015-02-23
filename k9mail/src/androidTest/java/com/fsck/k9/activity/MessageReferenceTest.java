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
        MessageReference mr = new MessageReference();
        mr.accountUuid = "o hai!";
        mr.folderName = "folder";
        mr.uid = "10101010";

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=", mr.toIdentityString());
    }

    @Test
    public void checkIdentityStringFromMessageReferenceWithFlag() {
        MessageReference mr = new MessageReference();
        mr.accountUuid = "o hai!";
        mr.folderName = "folder";
        mr.uid = "10101010";
        mr.flag = Flag.ANSWERED;

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED", mr.toIdentityString());
    }

    @Test
    public void parseIdentityStringWithoutFlag() throws MessagingException {
        MessageReference mr = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=");
        assertEquals("o hai!", mr.accountUuid);
        assertEquals("folder", mr.folderName);
        assertEquals("10101010", mr.uid);
        assertNull(mr.flag);
    }

    @Test
    public void parseIdentityStringWithFlag() throws MessagingException {
        MessageReference mr = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");
        assertEquals("o hai!", mr.accountUuid);
        assertEquals("folder", mr.folderName);
        assertEquals("10101010", mr.uid);
        assertEquals(Flag.ANSWERED, mr.flag);
    }

    @Test
    public void parseIdentityStringContainingBadVersionNumber() throws MessagingException {
        MessageReference mr = new MessageReference("@:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");
        assertNull(mr.accountUuid);
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
        MessageReference m = new MessageReference();
        Object o = new Object();
        assertFalse(m.equals(o));
    }

    @Test
    public void equalsWithMessageReferenceContainingSameDataShouldReturnTrue() {
        MessageReference m1 = new MessageReference();
        m1.accountUuid = "acc1";
        m1.folderName = "folder1";
        m1.uid = "uid1";

        MessageReference m2 = new MessageReference();
        m2.accountUuid = "acc1";
        m2.folderName = "folder1";
        m2.uid = "uid1";

        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentAccountUuidShouldReturnFalse() {
        MessageReference m1 = new MessageReference();
        m1.accountUuid = "acc1";

        MessageReference m2 = new MessageReference();
        m2.accountUuid = "acc2";

        assertFalse(m1.equals(m2));
        assertFalse(m2.equals(m1));
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentFolderNameShouldReturnFalse() {
        MessageReference m1 = new MessageReference();
        m1.folderName = "folder1";

        MessageReference m2 = new MessageReference();
        m2.folderName = "folder2";

        assertFalse(m1.equals(m2));
        assertFalse(m2.equals(m1));
    }

    @Test
    public void equalsWithMessageReferenceContainingDifferentUidShouldReturnFalse() {
        MessageReference m1 = new MessageReference();
        m1.uid = "uid1";

        MessageReference m2 = new MessageReference();
        m2.uid = "uid2";

        assertFalse(m1.equals(m2));
        assertFalse(m2.equals(m1));
    }
}
