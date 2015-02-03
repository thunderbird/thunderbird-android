package com.fsck.k9.activity;

import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class MessageReferenceTest
{
    /**
     * Typically happens during forwards.  (You have a reference, but no flag since we don't currently consider FORWARDED a flag.)
     */
    @Test
    public void testIdentityStringNoFlag()
    {
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
    public void testIdentityString()
    {
        MessageReference mr = new MessageReference();
        mr.accountUuid = "o hai!";
        mr.folderName = "folder";
        mr.uid = "10101010";
        mr.flag = Flag.ANSWERED;

        assertEquals("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED", mr.toIdentityString());
    }

    @Test
    public void testParseIdentityStringNoFlag() throws MessagingException
    {
        MessageReference mr = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=");
        assertEquals("o hai!", mr.accountUuid);
        assertEquals("folder", mr.folderName);
        assertEquals("10101010", mr.uid);
        assertNull(mr.flag);
    }

    @Test
    public void testParseIdentityString() throws MessagingException
    {
        MessageReference mr = new MessageReference("!:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");
        assertEquals("o hai!", mr.accountUuid);
        assertEquals("folder", mr.folderName);
        assertEquals("10101010", mr.uid);
        assertEquals(Flag.ANSWERED, mr.flag);
    }

    @Test
    public void testBadVersion() throws MessagingException
    {
        MessageReference mr = new MessageReference("@:byBoYWkh:Zm9sZGVy:MTAxMDEwMTA=:ANSWERED");
        assertNull(mr.accountUuid);
    }

    @Test(expected = MessagingException.class)
    public void testNull() throws MessagingException
    {
        new MessageReference(null);
    }

    @Test(expected = MessagingException.class)
    public void testCorruption() throws MessagingException
    {
        MessageReference mr = new MessageReference("!:%^&%^*$&$by&(BYWkh:Zm9%^@sZGVy:MT-35#$AxMDEwMTA=:ANSWERED");
        // No idea what this is going to generate, but it should be non-null.
        assertNotNull(mr.accountUuid);
        assertNotNull(mr.folderName);
        assertNotNull(mr.uid);

        // Corruption in the Flag should throw MessagingException.
        new MessageReference("!:%^&%^*$&$by&(BYWkh:Zm9%^@sZGVy:MT-35#$AxMDEwMTA=:ANSWE!RED");
    }
}
