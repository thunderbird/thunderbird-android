package com.fsck.k9.activity;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fsck.k9.R;
import com.fsck.k9.activity.RecipientSelectView.Recipient;
import com.fsck.k9.helper.ContactPicture;


public class RecipientAdapter extends CursorAdapter {

    /** Indexes of the fields in the projection. This must match the order in
     * {@link #PROJECTION}. */
    protected static final int INDEX_NAME = 1;
    protected static final int INDEX_EMAIL = 2;
    protected static final int INDEX_CONTACT_ID = 3;
    protected static final int INDEX_PHOTO_URI = 4;

    protected static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.DATA,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
    };

    protected static final String SORT_ORDER =
            ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED + " DESC, " +
                    ContactsContract.Contacts.DISPLAY_NAME + ", " +
                    ContactsContract.CommonDataKinds.Email._ID;

    public RecipientAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public Recipient getCurrentItem() {
        Cursor cursor = getCursor();
        if (cursor == null || cursor.isClosed()) {
            return null;
        }

        String name = cursor.getString(INDEX_NAME);
        String email = cursor.getString(INDEX_EMAIL);
        long contactId = cursor.getLong(INDEX_CONTACT_ID);

        Uri uri = cursor.isNull(INDEX_PHOTO_URI) ? null : Uri.parse(cursor.getString(INDEX_PHOTO_URI));
        Recipient recipient = new Recipient(name, email, contactId);
        recipient.photoThumbnailUri = uri;
        return recipient;
    }

    @Override
    public Recipient getItem(int position) {
        Cursor cursor = getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            return null;
        }

        return getCurrentItem();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.recipient_dropdown_item, parent, false);
        RecipientTokenHolder holder = new RecipientTokenHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        Recipient recipient = getCurrentItem();

        holder.name.setText(recipient.address.getPersonal());
        holder.email.setText(recipient.address.getAddress());

        setContactPhotoOrPlaceholder(context, holder.photo, recipient);

    }

    public static void setContactPhotoOrPlaceholder(Context context, ImageView imageView, Recipient recipient) {
        if (recipient.photoThumbnailUri != null) {
            Glide.with(context).load(recipient.photoThumbnailUri).into(imageView);
        } else {
            ContactPicture.getContactPictureLoader(context).loadContactPicture(recipient.address, imageView);
        }
    }

    static class RecipientTokenHolder {
        TextView name, email;
        ImageView photo;

        public RecipientTokenHolder(View view) {
            name = (TextView) view.findViewById(R.id.text1);
            email = (TextView) view.findViewById(R.id.text2);
            photo = (ImageView) view.findViewById(R.id.contact_photo);
        }
    }

}
