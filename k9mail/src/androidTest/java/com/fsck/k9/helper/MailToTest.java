package com.fsck.k9.helper;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import com.fsck.k9.mail.Address;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MailToTest {
    @Test
    public void testIsMailTo_validMailToURI() {
        Uri uri = Uri.parse("mailto:nobody");

        boolean result = MailTo.isMailTo(uri);

        assertTrue(result);
    }

    @Test
    public void testIsMailTo_invalidMailToUri() {
        Uri uri = Uri.parse("mailto1:nobody");

        boolean result = MailTo.isMailTo(uri);

        assertFalse(result);
    }

    @Test
    public void testGetTo_singleEmailAddress() {
        Uri uri = Uri.parse("mailto:test@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getTo();

        assertEquals(emailAddressList[0].getAddress(), "test@abc.com");
    }

    @Test
    public void testGetTo_multipleEmailAddress() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getTo();

        assertEquals(emailAddressList[0].getAddress(), "test1@abc.com");
        assertEquals(emailAddressList[1].getAddress(), "test2@abc.com");
    }

    @Test
    public void testGetCc_singleEmailAddress() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&cc=test3@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getCc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
    }

    @Test
    public void testGetCc_multipleEmailAddress() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&cc=test3@abc.com,test4@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getCc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
        assertEquals(emailAddressList[1].getAddress(), "test4@abc.com");
    }

    @Test
    public void testGetBcc_singleEmailAddress() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&bcc=test3@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getBcc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
    }

    @Test
    public void testGetBcc_multipleEmailAddress() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&bcc=test3@abc.com,test4@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getBcc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
        assertEquals(emailAddressList[1].getAddress(), "test4@abc.com");
    }

    @Test
    public void testGetSubject() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&cc=test3@abc.com&subject=Hello");
        MailTo mailToHelper = MailTo.parse(uri);

        String subject = mailToHelper.getSubject();

        assertEquals(subject, "Hello");
    }

    @Test
    public void testGetBody() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&cc=test3@abc.com&subject=Hello&body=Test Body");
        MailTo mailToHelper = MailTo.parse(uri);

        String subject = mailToHelper.getBody();

        assertEquals(subject, "Test Body");
    }

    @Test
    public void testCaseInsensitiveParamWrapper() {
        Uri uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com&cc=test3@abc.com&subject=Hello&body=Test Body");
        MailTo.CaseInsensitiveParamWrapper caseInsensitiveParamWrapper = new MailTo.CaseInsensitiveParamWrapper(
                Uri.parse("foo://bar?" + uri.getEncodedQuery()));

        List<String> actualTo = caseInsensitiveParamWrapper.getQueryParameters("to");
        List<String> expectedTo = Arrays.asList(new String[]{"test2@abc.com"});
    }
}
