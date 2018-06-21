package com.fsck.k9.message.extractors;


import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Part;
import org.junit.Before;
import org.junit.Test;

import static com.fsck.k9.message.MessageCreationHelper.createEmptyPart;
import static com.fsck.k9.message.MessageCreationHelper.createMultipart;
import static com.fsck.k9.message.MessageCreationHelper.createPart;
import static com.fsck.k9.message.MessageCreationHelper.createTextPart;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class TextPartFinderTest {
    private TextPartFinder textPartFinder;


    @Before
    public void setUp() throws Exception {
        textPartFinder = new TextPartFinder();
    }

    @Test
    public void findFirstTextPart_withTextPlainPart() throws Exception {
        Part part = createTextPart("text/plain");

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(part, result);
    }

    @Test
    public void findFirstTextPart_withTextHtmlPart() throws Exception {
        Part part = createTextPart("text/html");

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(part, result);
    }

    @Test
    public void findFirstTextPart_withoutTextPart() throws Exception {
        Part part = createPart("image/jpeg");

        Part result = textPartFinder.findFirstTextPart(part);

        assertNull(result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternative() throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/alternative", expected, createTextPart("text/html"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternativeHtmlPartFirst() throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/alternative", createTextPart("text/html"), expected);

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternativeContainingOnlyTextHtmlPart() throws Exception {
        BodyPart expected = createTextPart("text/html");
        Part part = createMultipart("multipart/alternative",
                createPart("image/gif"),
                expected,
                createTextPart("text/html"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternativeNotContainingTextPart() throws Exception {
        Part part = createMultipart("multipart/alternative",
                createPart("image/gif"),
                createPart("application/pdf"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertNull(result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternativeContainingMultipartRelatedContainingTextPlain()
            throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/alternative",
                createMultipart("multipart/related", expected, createPart("image/jpeg")),
                createTextPart("text/html"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternativeContainingMultipartRelatedContainingTextHtmlFirst()
            throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/alternative",
                createMultipart("multipart/related", createTextPart("text/html"), createPart("image/jpeg")),
                expected);

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartMixedContainingTextPlain() throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/mixed", createPart("image/jpeg"), expected);

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartMixedContainingTextHtmlFirst() throws Exception {
        BodyPart expected = createTextPart("text/html");
        Part part = createMultipart("multipart/mixed", expected, createTextPart("text/plain"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartMixedNotContainingTextPart() throws Exception {
        Part part = createMultipart("multipart/mixed", createPart("image/jpeg"), createPart("image/gif"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertNull(result);
    }

    @Test
    public void findFirstTextPart_withMultipartMixedContainingMultipartAlternative() throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/mixed",
                createPart("image/jpeg"),
                createMultipart("multipart/alternative", expected, createTextPart("text/html")),
                createTextPart("text/plain"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartMixedContainingMultipartAlternativeWithTextPlainPartLast()
            throws Exception {
        BodyPart expected = createTextPart("text/plain");
        Part part = createMultipart("multipart/mixed",
                createMultipart("multipart/alternative", createTextPart("text/html"), expected));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartAlternativeContainingEmptyTextPlainPart()
            throws Exception {
        BodyPart expected = createEmptyPart("text/plain");
        Part part = createMultipart("multipart/alternative", expected, createTextPart("text/html"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }

    @Test
    public void findFirstTextPart_withMultipartMixedContainingEmptyTextHtmlPart()
            throws Exception {
        BodyPart expected = createEmptyPart("text/html");
        Part part = createMultipart("multipart/mixed", expected, createTextPart("text/plain"));

        Part result = textPartFinder.findFirstTextPart(part);

        assertEquals(expected, result);
    }
}
