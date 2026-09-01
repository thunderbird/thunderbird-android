package com.fsck.k9.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

import com.fsck.k9.mail.internet.BinaryTempFileBody;
import net.thunderbird.core.android.testing.RobolectricTest;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ImageAttachmentCompressorTest extends RobolectricTest {
    private static final int JPEG_QUALITY = 85;
    private static final int LONGEST_EDGE = 1920;

    @Before
    public void setUp() {
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.getApplication().getCacheDir());
    }

    @Test
    public void compress_shouldResizeAndEncodeJpeg() throws Exception {
        Attachment attachment = createImageAttachment(Bitmap.CompressFormat.JPEG, "image/jpeg", "photo.jpeg");

        ImageAttachmentCompressor.CompressedAttachment result = new ImageAttachmentCompressor(
                JPEG_QUALITY, LONGEST_EDGE).compress(attachment);

        assertNotNull(result);
        assertEquals("photo.jpg", result.name);
        Bitmap compressedBitmap = BitmapFactory.decodeFile(result.file.getAbsolutePath());
        assertEquals(LONGEST_EDGE, compressedBitmap.getWidth());
        assertEquals(960, compressedBitmap.getHeight());
        compressedBitmap.recycle();
    }

    @Test
    public void compress_shouldNotTouchPng() throws Exception {
        Attachment attachment = createImageAttachment(Bitmap.CompressFormat.PNG, "image/png", "photo.png");

        ImageAttachmentCompressor.CompressedAttachment result = new ImageAttachmentCompressor(
                JPEG_QUALITY, LONGEST_EDGE).compress(attachment);

        assertNull(result);
    }

    private Attachment createImageAttachment(
            Bitmap.CompressFormat format,
            String contentType,
            String name
    ) throws Exception {
        File file = File.createTempFile("image-", ".tmp", RuntimeEnvironment.getApplication().getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(3000, 1500, Bitmap.Config.ARGB_8888);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(format, 100, outputStream);
        } finally {
            bitmap.recycle();
        }

        return new Attachment() {
            @Override
            public LoadingState getState() {
                return LoadingState.COMPLETE;
            }

            @Override
            public String getFileName() {
                return file.getAbsolutePath();
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Long getSize() {
                return file.length();
            }

            @Override
            public boolean isInternalAttachment() {
                return false;
            }
        };
    }
}
