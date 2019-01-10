package com.fsck.k9.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.fsck.k9.Account;
import com.fsck.k9.message.Attachment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ImageResizer {
    /**
     * The path of the temporary directory that is used to store resized image attachments.
     * The directory is cleaned as soon as message is sent.
     */
    private static final String RESIZED_IMAGES_TEMPORARY_DIRECTORY = "/tempAttachments/";

    private final Context context;


    public ImageResizer(Context context) {
        this.context = context;
    }

    public ArrayList<Attachment> createAttachmentListWithResizedImages(List<? extends Attachment> attachments) {
        ArrayList<Attachment> result = new ArrayList<>();
        for (Attachment attachment : attachments) {
            int resizeCircumference;
            int resizeQuality;

            if (attachment.getResizeImagesEnabled() && isImage(context, attachment.getUri())) {
                    resizeCircumference = attachment.getResizeImageCircumference();
                    resizeQuality = attachment.getResizeImageQuality();
            } else{
                resizeCircumference = 0;
                resizeQuality = 0;
            }

            if (resizeCircumference != 0) {
                try {
                    resizeImageFile(attachment, resizeCircumference, resizeQuality);
                } catch (IOException ioe) {
                    Timber.i("image resizing failed for " + attachment.getFileName() + " circumference=" + resizeCircumference + ", quality=" + resizeQuality);
                }
            }

            result.add(attachment);
        }
        return result;
    }


    private void resizeImageFile(Attachment attachment, int circumference, int quality) throws IOException {
        File tempAttachmentsDirectory = getTempAttachmentsDirectory();
        tempAttachmentsDirectory.mkdirs();

        Bitmap bitmap = BitmapFactory.decodeFile(attachment.getFileName());

        int newWidth;
        int newHeight;
        float factor = (bitmap.getWidth() + bitmap.getHeight() + 0f) / circumference;
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

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        FileOutputStream out = null;
        File tempFile = null;
        try {
            tempFile = File.createTempFile("TempResizedAttachment", null, tempAttachmentsDirectory);
            out = new FileOutputStream(tempFile);
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out);
            FileUtils.copyFile(tempFile, new File(attachment.getFileName()));
            attachment.setSize(tempFile.length());
        } finally {
            IOUtils.closeQuietly(out);
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @NonNull
    private File getTempAttachmentsDirectory() {
        File cacheDir = context.getCacheDir();
        return new File(cacheDir.getPath(), RESIZED_IMAGES_TEMPORARY_DIRECTORY);
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
