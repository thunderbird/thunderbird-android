package com.fsck.k9.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.fsck.k9.helper.MimeTypeUtil;
import com.fsck.k9.mail.internet.BinaryTempFileBody;

final class ImageAttachmentCompressor {
    private final int jpegQuality;
    private final int longestEdge;

    ImageAttachmentCompressor(int jpegQuality, int longestEdge) {
        this.jpegQuality = jpegQuality;
        this.longestEdge = longestEdge;
    }

    CompressedAttachment compress(Attachment attachment) {
        if (!isCompressible(attachment)) {
            return null;
        }

        Bitmap bitmap = decodeBitmap(attachment.getFileName());
        if (bitmap == null) {
            return null;
        }

        Bitmap scaledBitmap = scaleBitmap(bitmap);
        if (scaledBitmap != bitmap) {
            bitmap.recycle();
        }

        File file = null;
        try {
            file = File.createTempFile("compressed-", ".jpg", BinaryTempFileBody.getTempDirectory());
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                if (!scaledBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)) {
                    file.delete();
                    return null;
                }
            }
            return new CompressedAttachment(file, getJpegFileName(attachment.getName()));
        } catch (IOException e) {
            if (file != null) {
                file.delete();
            }
            return null;
        } finally {
            scaledBitmap.recycle();
        }
    }

    private boolean isCompressible(Attachment attachment) {
        String contentType = attachment.getContentType();
        return contentType != null &&
                !MimeTypeUtil.isSameMimeType(contentType, "image/png") &&
                MimeTypeUtil.isSupportedImageType(contentType) &&
                attachment.getFileName() != null;
    }

    private Bitmap decodeBitmap(String fileName) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight);
        return BitmapFactory.decodeFile(fileName, options);
    }

    private int calculateInSampleSize(int width, int height) {
        int sampleSize = 1;
        int largestDimension = Math.max(width, height);
        while (largestDimension / (sampleSize * 2) >= longestEdge) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int largestDimension = Math.max(width, height);
        if (largestDimension <= longestEdge) {
            return bitmap;
        }

        float scale = (float) longestEdge / largestDimension;
        return Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
    }

    private String getJpegFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "image.jpg";
        }

        int extensionStart = fileName.lastIndexOf('.');
        String baseName = extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
        return baseName + ".jpg";
    }

    static final class CompressedAttachment {
        final File file;
        final String name;

        CompressedAttachment(File file, String name) {
            this.file = file;
            this.name = name;
        }
    }
}
