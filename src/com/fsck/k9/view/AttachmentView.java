package com.fsck.k9.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.MediaScannerNotifier;
import com.fsck.k9.helper.SizeFormatter;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.fsck.k9.provider.AttachmentProvider;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class AttachmentView extends FrameLayout
{

    private Context mContext;
    public Button viewButton;
    public Button downloadButton;
    public LocalAttachmentBodyPart part;
    private Message mMessage;
    private Account mAccount;
    private MessagingController mController;
    private MessagingListener mListener;
    public String name;
    public String contentType;
    public long size;
    public ImageView iconView;

    public AttachmentView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mContext = context;
    }
    public AttachmentView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
    }
    public AttachmentView(Context context)
    {
        super(context);
        mContext = context;
    }



    public boolean populateFromPart(Part inputPart, Message message, Account account, MessagingController controller, MessagingListener listener  )
    {
        try
        {
            part = (LocalAttachmentBodyPart) inputPart;

            contentType = MimeUtility.unfoldAndDecode(part.getContentType());
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());

            name = MimeUtility.getHeaderParameter(contentType, "name");
            if (name == null)
            {
                name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
            }
            if (name == null)
            {
                return false;
            }

            mAccount = account;
            mMessage = message;
            mController = controller;
            mListener = listener;

            size = Integer.parseInt(MimeUtility.getHeaderParameter(contentDisposition, "size"));
            contentType = part.getMimeType();
            if (MimeUtility.DEFAULT_ATTACHMENT_MIME_TYPE.equals(contentType))
            {
                contentType = MimeUtility.getMimeTypeByExtension(name);
            }
            TextView attachmentName = (TextView) findViewById(R.id.attachment_name);
            TextView attachmentInfo = (TextView) findViewById(R.id.attachment_info);
            ImageView attachmentIcon = (ImageView) findViewById(R.id.attachment_icon);
            viewButton = (Button) findViewById(R.id.view);
            downloadButton = (Button) findViewById(R.id.download);
            if ((!MimeUtility.mimeTypeMatches(contentType, K9.ACCEPTABLE_ATTACHMENT_VIEW_TYPES))
                    || (MimeUtility.mimeTypeMatches(contentType, K9.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES)))
            {
                viewButton.setVisibility(View.GONE);
            }
            if ((!MimeUtility.mimeTypeMatches(contentType, K9.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))
                    || (MimeUtility.mimeTypeMatches(contentType, K9.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES)))
            {
                downloadButton.setVisibility(View.GONE);
            }
            if (size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE)
            {
                viewButton.setVisibility(View.GONE);
                downloadButton.setVisibility(View.GONE);
            }

            viewButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onViewButtonClicked();
                    return;
                }
            });


            downloadButton.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onSaveButtonClicked();
                    return;
                }
            });

            attachmentName.setText(name);
            attachmentInfo.setText(SizeFormatter.formatSize(mContext, size));
            Bitmap previewIcon = getPreviewIcon();
            if (previewIcon != null)
            {
                attachmentIcon.setImageBitmap(previewIcon);
            }
            else
            {
                attachmentIcon.setImageResource(R.drawable.attached_image_placeholder);
            }
        }

        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "error ",e);
        }

        return true;
    }

    private Bitmap getPreviewIcon()
    {
        try
        {
            return BitmapFactory.decodeStream(
                       mContext.getContentResolver().openInputStream(
                           AttachmentProvider.getAttachmentThumbnailUri(mAccount,
                                   part.getAttachmentId(),
                                   62,
                                   62)));
        }
        catch (Exception e)
        {
            /*
             * We don't care what happened, we just return null for the preview icon.
             */
            return null;
        }
    }

    private void onViewButtonClicked()
    {
        if (mMessage != null)
        {
            mController.loadAttachment( mAccount, mMessage, part, new Object[] { false, this }, mListener);
        }
    }


    private void onSaveButtonClicked()
    {
        saveFile();
    }

    public void writeFile ()
    {
        try
        {
            File file = Utility.createUniqueFile(Environment.getExternalStorageDirectory(), name);
            Uri uri = AttachmentProvider.getAttachmentUri( mAccount, part.getAttachmentId());
            InputStream in = mContext.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            out.flush();
            out.close();
            in.close();
            attachmentSaved(file.getName());
            new MediaScannerNotifier(mContext, file);
        }
        catch (IOException ioe)
        {
            attachmentNotSaved();
        }
    }

    public void saveFile()
    {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            /*
             * Abort early if there's no place to save the attachment. We don't want to spend
             * the time downloading it and then abort.
             */
            Toast.makeText(mContext,
                           mContext.getString(R.string.message_view_status_attachment_not_saved),
                           Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMessage != null)
        {
            mController.loadAttachment( mAccount, mMessage, part, new Object[] {true, this}, mListener);
        }
    }


    public void showFile()
    {
        Uri uri = AttachmentProvider.getAttachmentUri( mAccount, part.getAttachmentId());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try
        {
            mContext.startActivity(intent);
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Could not display attachment of type " + contentType, e);
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string.message_view_no_viewer, contentType), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void attachmentSaved(final String filename)
    {
        Toast.makeText(mContext, String.format(
                           mContext.getString(R.string.message_view_status_attachment_saved), filename),
                       Toast.LENGTH_LONG).show();
    }

    public void attachmentNotSaved()
    {
        Toast.makeText(mContext,
                       mContext.getString(R.string.message_view_status_attachment_not_saved),
                       Toast.LENGTH_LONG).show();
    }
}
