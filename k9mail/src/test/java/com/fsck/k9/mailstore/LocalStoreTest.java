package com.fsck.k9.mailstore;


import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import org.junit.Test;

import static org.junit.Assert.*;


public class LocalStoreTest {

    @Test
    public void findPartById__withRootLocalBodyPart() throws Exception {
        LocalBodyPart searchRoot = new LocalBodyPart(null, null, 123L, -1L);

        Part part = LocalStore.findPartById(searchRoot, 123L);

        assertSame(searchRoot, part);
    }

    @Test
    public void findPartById__withRootLocalMessage() throws Exception {
        LocalMessage searchRoot = new LocalMessage(null, "uid", null);
        searchRoot.setMessagePartId(123L);

        Part part = LocalStore.findPartById(searchRoot, 123L);

        assertSame(searchRoot, part);
    }

    @Test
    public void findPartById__withNestedLocalBodyPart() throws Exception {
        LocalBodyPart searchRoot = new LocalBodyPart(null, null, 1L, -1L);

        LocalBodyPart needlePart = new LocalBodyPart(null, null, 123L, -1L);
        MimeMultipart mimeMultipart = new MimeMultipart("boundary");
        mimeMultipart.addBodyPart(needlePart);
        searchRoot.setBody(mimeMultipart);


        Part part = LocalStore.findPartById(searchRoot, 123L);


        assertSame(needlePart, part);
    }

    @Test
    public void findPartById__withNestedLocalMessagePart() throws Exception {
        LocalBodyPart searchRoot = new LocalBodyPart(null, null, 1L, -1L);

        LocalMimeMessage needlePart = new LocalMimeMessage(null, null, 123L);
        MimeMultipart mimeMultipart = new MimeMultipart("boundary");
        mimeMultipart.addBodyPart(new MimeBodyPart(needlePart));
        searchRoot.setBody(mimeMultipart);


        Part part = LocalStore.findPartById(searchRoot, 123L);


        assertSame(needlePart, part);
    }

    @Test
    public void findPartById__withTwoTimesNestedLocalMessagePart() throws Exception {
        LocalBodyPart searchRoot = new LocalBodyPart(null, null, 1L, -1L);

        LocalMimeMessage needlePart = new LocalMimeMessage(null, null, 123L);
        MimeMultipart mimeMultipartInner = new MimeMultipart("boundary");
        mimeMultipartInner.addBodyPart(new MimeBodyPart(needlePart));
        MimeMultipart mimeMultipart = new MimeMultipart("boundary");
        mimeMultipart.addBodyPart(new MimeBodyPart(mimeMultipartInner));
        searchRoot.setBody(mimeMultipart);


        Part part = LocalStore.findPartById(searchRoot, 123L);


        assertSame(needlePart, part);
    }
}