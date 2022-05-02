package com.fsck.k9.helper;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.net.Uri;

import com.fsck.k9.RobolectricTest;
import com.fsck.k9.helper.MailTo.CaseInsensitiveParamWrapper;
import com.fsck.k9.mail.Address;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class MailToTest extends RobolectricTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testIsMailTo_validMailToURI() {
        Uri uri = Uri.parse("mailto:nobody");

        boolean result = MailTo.isMailTo(uri);

        assertTrue(result);
    }

    @Test
    public void testIsMailTo_invalidMailToUri() {
        Uri uri = Uri.parse("http://example.org/");

        boolean result = MailTo.isMailTo(uri);

        assertFalse(result);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testIsMailTo_nullArgument() {
        Uri uri = null;

        boolean result = MailTo.isMailTo(uri);

        assertFalse(result);
    }

    @Test
    public void parse_withNullArgument_shouldThrow() throws Exception {
        exception.expect(NullPointerException.class);
        exception.expectMessage("Argument 'uri' must not be null");

        MailTo.parse(null);
    }

    @Test
    public void parse_withoutMailtoUri_shouldThrow() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Not a mailto scheme");

        Uri uri = Uri.parse("http://example.org/");

        MailTo.parse(uri);
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
        Uri uri = Uri.parse("mailto:test1@abc.com?cc=test3@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getCc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
    }

    @Test
    public void testGetCc_multipleEmailAddress() {
        Uri uri = Uri.parse("mailto:test1@abc.com?cc=test3@abc.com,test4@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getCc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
        assertEquals(emailAddressList[1].getAddress(), "test4@abc.com");
    }

    @Test
    public void testGetBcc_singleEmailAddress() {
        Uri uri = Uri.parse("mailto:?bcc=test3@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getBcc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
    }

    @Test
    public void testGetBcc_multipleEmailAddress() {
        Uri uri = Uri.parse("mailto:?bcc=test3@abc.com&bcc=test4@abc.com");
        MailTo mailToHelper = MailTo.parse(uri);

        Address[] emailAddressList = mailToHelper.getBcc();

        assertEquals(emailAddressList[0].getAddress(), "test3@abc.com");
        assertEquals(emailAddressList[1].getAddress(), "test4@abc.com");
    }

    @Test
    public void testGetSubject() {
        Uri uri = Uri.parse("mailto:?subject=Hello");
        MailTo mailToHelper = MailTo.parse(uri);

        String subject = mailToHelper.getSubject();

        assertEquals(subject, "Hello");
    }

    @Test
    public void testGetBody() {
        Uri uri = Uri.parse("mailto:?body=Test%20Body&something=else");
        MailTo mailToHelper = MailTo.parse(uri);

        String subject = mailToHelper.getBody();

        assertEquals(subject, "Test Body");
    }

    @Test
    public void testCaseInsensitiveParamWrapper() {
        Uri uri = Uri.parse("scheme://authority?a=one&b=two&c=three");
        CaseInsensitiveParamWrapper caseInsensitiveParamWrapper = new CaseInsensitiveParamWrapper(uri);

        List<String> result = caseInsensitiveParamWrapper.getQueryParameters("b");

        assertThat(Collections.singletonList("two"), is(result));
    }

    @Test
    public void testCaseInsensitiveParamWrapper_multipleMatchingQueryParameters() {
        Uri uri = Uri.parse("scheme://authority?xname=one&name=two&Name=Three&NAME=FOUR");
        CaseInsensitiveParamWrapper caseInsensitiveParamWrapper = new CaseInsensitiveParamWrapper(uri);

        List<String> result = caseInsensitiveParamWrapper.getQueryParameters("name");

        assertThat(Arrays.asList("two", "Three", "FOUR"), is(result));
    }

    @Test
    public void testCaseInsensitiveParamWrapper_withoutQueryParameters() {
        Uri uri = Uri.parse("scheme://authority");
        CaseInsensitiveParamWrapper caseInsensitiveParamWrapper = new CaseInsensitiveParamWrapper(uri);

        List<String> result = caseInsensitiveParamWrapper.getQueryParameters("name");

        assertThat(Collections.<String>emptyList(), is(result));
    }

    @Test
    public void testGetInReplyTo_singleMessageId() {
        Uri uri = Uri.parse("mailto:?in-reply-to=%3C7C72B202-73F3@somewhere%3E");

        MailTo mailToHelper = MailTo.parse(uri);

        assertEquals("<7C72B202-73F3@somewhere>", mailToHelper.getInReplyTo());
    }

    @Test
    public void testGetInReplyTo_multipleMessageIds() {
        Uri uri = Uri.parse("mailto:?in-reply-to=%3C7C72B202-73F3@somewhere%3E%3C8A39-1A87CB40C114@somewhereelse%3E");

        MailTo mailToHelper = MailTo.parse(uri);

        assertEquals("<7C72B202-73F3@somewhere>", mailToHelper.getInReplyTo());
    }


    @Test
    public void testGetInReplyTo_RFC6068Example() {
        Uri uri = Uri.parse("mailto:list@example.org?In-Reply-To=%3C3469A91.D10AF4C@example.com%3E");

        MailTo mailToHelper = MailTo.parse(uri);

        assertEquals("<3469A91.D10AF4C@example.com>", mailToHelper.getInReplyTo());
    }

    @Test
    public void testGetInReplyTo_invalid() {
        Uri uri = Uri.parse("mailto:?in-reply-to=7C72B202-73F3somewhere");

        MailTo mailToHelper = MailTo.parse(uri);

        assertEquals(null, mailToHelper.getInReplyTo());
    }

    @Test
    public void testGetInReplyTo_empty() {
        Uri uri = Uri.parse("mailto:?in-reply-to=");

        MailTo mailToHelper = MailTo.parse(uri);

        assertEquals(null, mailToHelper.getInReplyTo());

    }

}
