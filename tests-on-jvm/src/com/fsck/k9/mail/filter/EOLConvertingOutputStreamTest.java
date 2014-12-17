package com.fsck.k9.mail.filter;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

public class EOLConvertingOutputStreamTest extends TestCase {
    private EOLConvertingOutputStream subject;
    private ByteArrayOutputStream out;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        out = new ByteArrayOutputStream();
        subject = new EOLConvertingOutputStream(out);
    }

    public void testFilterWithoutCRorLF() throws Exception {
        subject.write("Unchanged".getBytes());
        subject.flush();
        assertEquals("Unchanged", out.toString());
    }

    public void testFilterWithCRLF() throws Exception {
        subject.write("Filter\r\nNext Line".getBytes());
        subject.flush();
        assertEquals("Filter\r\nNext Line", out.toString());
    }

    public void testFilterWithJustCR() throws Exception {
        subject.write("\n\n\n".getBytes());
        subject.flush();
        assertEquals("\r\n\r\n\r\n", out.toString());
    }

    public void testFilterWithCR() throws Exception {
        subject.write("Filter\rNext Line".getBytes());
        subject.flush();
        assertEquals("Filter\r\nNext Line", out.toString());
    }

    public void testFilterWithLF() throws Exception {
        subject.write("Filter\nNext Line".getBytes());
        subject.flush();
        assertEquals("Filter\r\nNext Line", out.toString());
    }

    public void testFlushWithCR() throws Exception {
        subject.write("Flush\r".getBytes());
        subject.flush();
        assertEquals("Flush\r\n", out.toString());
        subject.write("\n\n\n".getBytes());
        assertEquals("Flush\r\n\r\n\r\n", out.toString());
    }

    public void testFlushWithCRNotFollowedByLF() throws Exception {
        subject.write("Flush\r".getBytes());
        subject.flush();
        subject.write("Next line".getBytes());
        assertEquals("Flush\r\nNext line", out.toString());
    }

    public void testFlushWithLF() throws Exception {
        subject.write("Flush\n".getBytes());
        subject.flush();
        subject.write("\n".getBytes());
        assertEquals("Flush\r\n\r\n", out.toString());
    }
}
