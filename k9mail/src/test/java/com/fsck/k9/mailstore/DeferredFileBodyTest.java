package com.fsck.k9.mailstore;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mailstore.util.FileFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DeferredFileBodyTest {
    public static final String TEST_ENCODING = "test-encoding";
    public static final byte[] TEST_DATA_SHORT = "test data".getBytes();
    public static final byte[] TEST_DATA_LONG = "test data long enough to be file backed".getBytes();
    public static final int TEST_THRESHOLD = 15;


    private File createdFile;
    private DeferredFileBody deferredFileBody;


    @Before
    public void setUp() throws Exception {
        FileFactory fileFactory = new FileFactory() {
            @Override
            public File createFile() throws IOException {
                assertNull("only a single file should be created", createdFile);
                createdFile = File.createTempFile("test", "tmp");
                createdFile.deleteOnExit();
                return createdFile;
            }
        };

        deferredFileBody = new DeferredFileBody(TEST_THRESHOLD, fileFactory, TEST_ENCODING);
    }

    @Test
    public void withShortData__getLength__shouldReturnWrittenLength() throws Exception {
        writeShortTestData();

        assertNull(createdFile);
        assertEquals(TEST_DATA_SHORT.length, deferredFileBody.getSize());
    }

    @Test
    public void withLongData__getLength__shouldReturnWrittenLength() throws Exception {
        writeLongTestData();

        assertNotNull(createdFile);
        assertEquals(TEST_DATA_LONG.length, deferredFileBody.getSize());
    }

    @Test
    public void withShortData__shouldReturnData() throws Exception {
        writeShortTestData();

        InputStream inputStream = deferredFileBody.getInputStream();
        byte[] data = IOUtils.toByteArray(inputStream);

        assertNull(createdFile);
        assertArrayEquals(TEST_DATA_SHORT, data);
    }

    @Test
    public void withLongData__shouldReturnData() throws Exception {
        writeLongTestData();

        InputStream inputStream = deferredFileBody.getInputStream();
        byte[] data = IOUtils.toByteArray(inputStream);
        InputStream fileInputStream = new FileInputStream(createdFile);
        byte[] dataFromFile = IOUtils.toByteArray(fileInputStream);

        assertArrayEquals(TEST_DATA_LONG, data);
        assertArrayEquals(TEST_DATA_LONG, dataFromFile);
    }

    @Test
    public void withShortData__getFile__shouldWriteDataToFile() throws Exception {
        writeShortTestData();

        File returnedFile = deferredFileBody.getFile();
        InputStream fileInputStream = new FileInputStream(returnedFile);
        byte[] dataFromFile = IOUtils.toByteArray(fileInputStream);

        assertSame(createdFile, returnedFile);
        assertArrayEquals(TEST_DATA_SHORT, dataFromFile);
    }

    @Test
    public void withLongData__getFile__shouldReturnCreatedFile() throws Exception {
        writeLongTestData();

        File returnedFile = deferredFileBody.getFile();

        assertSame(createdFile, returnedFile);
    }

    @Test
    public void withShortData__writeTo__shouldWriteData() throws Exception {
        writeShortTestData();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        deferredFileBody.writeTo(baos);

        assertArrayEquals(TEST_DATA_SHORT, baos.toByteArray());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setEncoding__shouldThrow() throws Exception {
        deferredFileBody.setEncoding("anything");
    }

    @Test
    public void getEncoding__shouldReturnEncoding() throws Exception {
        assertEquals(TEST_ENCODING, deferredFileBody.getEncoding());
    }

    private void writeShortTestData() throws IOException {
        OutputStream outputStream = deferredFileBody.getOutputStream();
        outputStream.write(TEST_DATA_SHORT);
        outputStream.close();
    }

    private void writeLongTestData() throws IOException {
        OutputStream outputStream = deferredFileBody.getOutputStream();
        outputStream.write(TEST_DATA_LONG);
        outputStream.close();
    }
}