package com.fsck.k9.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.fsck.k9.Account;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import timber.log.Timber;

public class ImageResizer {
    /**
     * The path of the temporary directory that is used to store resized image attachments.
     * The directory is cleaned as soon as message is sent.
     */
    private static final String RESIZED_IMAGES_TEMPORARY_DIRECTORY = "/tempAttachments/";

    public String getResizedImageFile(Context context, Uri uri, int circumference, int quality) throws IOException {
        File tempAttachmentsDirectory = getTempAttachmentsDirectory(context);
        tempAttachmentsDirectory.mkdirs();

        File tempFile;
        Bitmap bitmap = null;
        Bitmap resized;
        FileOutputStream out = null;

        float factor = 0;

        try {
            tempFile = File.createTempFile("TempResizedAttachment", null, tempAttachmentsDirectory);
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            int newWidth;
            int newHeight;

            factor = (bitmap.getWidth() + bitmap.getHeight() + 0f) / circumference;
            if (factor <= 1.0f) {
                newWidth = bitmap.getWidth();
                newHeight = bitmap.getHeight();
            } else {
                newWidth = (int) (bitmap.getWidth() / factor);
                newHeight = (int) (bitmap.getHeight() / factor);

                while (newWidth + newHeight < circumference) {
                    if ((0f + bitmap.getWidth()) / newWidth >= (0f + bitmap.getHeight()) / newHeight)
                        ++newWidth;
                    else
                        ++newHeight;
                }
            }

            resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            out = new FileOutputStream(tempFile);
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out);

            return tempFile.getAbsolutePath();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public void clearTemporaryAttachmentsCache(Context context) {
        File tempAttachmentsDirectory = getTempAttachmentsDirectory(context);
        if (tempAttachmentsDirectory.exists()) {
            try {
                FileUtils.cleanDirectory(tempAttachmentsDirectory);
            } catch (IOException e) {
                Timber.e(e, "Error occurred while cleaning temporary directory for resized attachments");
            }
        }
    }

    @NonNull
    private File getTempAttachmentsDirectory(Context context) {
        File cacheDir = context.getCacheDir();
        return new File(cacheDir.getPath() + RESIZED_IMAGES_TEMPORARY_DIRECTORY);
    }

    /* simple helper methods. */

    public static boolean isImage(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    public static int convertResizeImageCircumference(String resizeCircumferenceS) {
        int resizeCircumference = Account.DEFAULT_RESIZE_IMAGE_CIRCUMFERENCE;
        try {
            resizeCircumference = Integer.parseInt(resizeCircumferenceS);
            if (resizeCircumference < 520)
                resizeCircumference = 520;
        } catch (NumberFormatException ex){
            // ignore
        }
        return resizeCircumference;
    }

    public static int convertResizeImageQuality(String resizeQualityS) {
        int resizeQuality = Account.DEFAULT_RESIZE_IMAGE_QUALITY;
        try {
            resizeQuality = Integer.parseInt(resizeQualityS);
            if (resizeQuality < 10)
                resizeQuality = 10;
            else if (resizeQuality > 100)
                resizeQuality = 100;
        } catch (NumberFormatException ex){
            // ignore
        }
        return resizeQuality;
    }

}
