package com.fsck.k9.mail.filter;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

public class NewlineAtEOBOutputStreamTest {
    private NewlineAtEOBOutputStream subject;
    private ByteArrayOutputStream out;

    @Before
    public void setUp() throws Exception {
        out = new ByteArrayOutputStream();
        subject = new NewlineAtEOBOutputStream(out);
    }

    @Test
    public void testDoNothing() throws Exception {
        subject.write("blahblah".getBytes());
        subject.flush();
        assertEquals("blahblah", out.toString());
    }

    @Test
    public void testWriteEOBatCR() throws Exception {
        subject.write("blahblah\r".getBytes());
        subject.writeEOB();
        assertEquals("blahblah\r\n", out.toString());
    }

    @Test
    public void testWriteEOBatLF() throws Exception {
        subject.write("blahblah\n".getBytes());
        subject.writeEOB();
        assertEquals("blahblah\n", out.toString());
    }

    @Test
    public void testWriteEOBatNoNewline() throws Exception {
        subject.write("blahblah".getBytes());
        subject.writeEOB();
        assertEquals("blahblah\r\n", out.toString());
    }

    @Test
    public void testWriteEOBatEmpty() throws Exception {
        subject.writeEOB();
        assertEquals("\r\n", out.toString());
    }
}