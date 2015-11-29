package com.fsck.k9.activity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.activity.RecipientSelectView.Recipient;
import com.fsck.k9.mail.Address;
import com.tokenautocomplete.TokenCompleteTextView;


public class RecipientSelectView extends TokenCompleteTextView<Recipient>
        implements LoaderCallbacks<Cursor> {

    public static final String ARG_QUERY = "query";

    private RecipientAdapter mAdapter;
    private android.app.LoaderManager mLoaderManager;

    public RecipientSelectView(Context context) {
        super(context);
        initView();
    }

    public RecipientSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RecipientSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        // TODO: validator?

        // don't allow duplicates, based on equality of recipient objects, which is e-mail addresses
        allowDuplicates(false);

        // if a token is completed, pick an entry based on best guess
        performBestGuess(true);

        mAdapter = new RecipientAdapter(getContext(), null, 0);
        setAdapter(mAdapter);
    }

    @Override
    protected View getViewForObject(Recipient recipient) {
        LayoutInflater l = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") // root will be attached by caller
        View view = l.inflate(R.layout.recipient_token_item, null, false);

        // A name is preferred, but show if nothing is available
        TextView vName = (TextView) view.findViewById(android.R.id.text1);
        String personal = recipient.address.getPersonal();
        if ( ! TextUtils.isEmpty(personal)) {
            vName.setText(personal);
        } else {
            vName.setText(recipient.address.getAddress());
        }

        ImageView contactView = (ImageView) view.findViewById(R.id.contact_photo);
        RecipientAdapter.setContactPhotoOrPlaceholder(getContext(), contactView, recipient);

        return view;
    }

    @Override
    protected Recipient defaultObject(String completionText) {
        Address[] parsedAddresses = Address.parseUnencoded(completionText);
        if (parsedAddresses.length == 0) {
            return null;
        }
        return new Recipient(parsedAddresses[0]);
    }

    public boolean isEmpty() {
        return getObjects().isEmpty();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (getContext() instanceof Activity) {
            mLoaderManager = ((Activity) getContext()).getLoaderManager();
            mLoaderManager.initLoader(0, null, this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLoaderManager = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String query = args != null && args.containsKey(ARG_QUERY) ? args.getString(ARG_QUERY) : "";
//        mAdapter.setSearchQuery(query);
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(query));

        return new CursorLoader(getContext(), uri, RecipientAdapter.PROJECTION, null, null, RecipientAdapter.SORT_ORDER);

        // new String[]{"%" + query + "%"}, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void showDropDown() {
        boolean cursorIsValid = mAdapter != null && mAdapter.getCursor() != null && !mAdapter.getCursor().isClosed();
        if (!cursorIsValid) {
            return;
        }
        super.showDropDown();
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (hasFocus) {
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    protected void performFiltering(@NonNull CharSequence text, int start, int end, int keyCode) {
        super.performFiltering(text, start, end, keyCode);
        String query = text.subSequence(start, end).toString();
        if (TextUtils.isEmpty(query) || query.length() < 2) {
            mLoaderManager.destroyLoader(0);
            return;
        }
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        mLoaderManager.restartLoader(0, args, this);
    }

    public void addAddress(Address... addresses) {
        for (Address address : addresses) {
            addObject(new Recipient(address));
        }
    }

    public Address[] getAddresses() {
        List<Recipient> recipients = getObjects();
        Address[] address = new Address[recipients.size()];
        for (int i = 0; i < address.length; i++) {
            address[i] = recipients.get(i).address;
        }
        return address;
    }

    static class Recipient implements Serializable {
        @NonNull
        public final Address address;

        @Nullable // null means the address is not associated with a contact
        public final Long contactId;

        @Nullable // null if the contact has no photo
        transient Uri photoThumbnailUri;

        Recipient(@NonNull Address address) {
            this.address = address;
            this.contactId = null;
        }

        Recipient(String name, String email, long contactId) {
            this.address = new Address(email, name);
            this.contactId = contactId;
        }

        @Override
        public boolean equals(Object o) {
            // Equality is entirely up to the address
            return o instanceof Recipient && address.equals(((Recipient) o).address);
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();

            // custom serialization, Android's Uri class is not serializable
            if (photoThumbnailUri != null) {
                oos.writeInt(1);
                oos.writeUTF(photoThumbnailUri.toString());
            } else {
                oos.writeInt(0);
            }
        }

        private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
            ois.defaultReadObject();

            // custom deserialization, Android's Uri class is not serializable
            if (ois.readInt() != 0) {
                String uriString = ois.readUTF();
                photoThumbnailUri = Uri.parse(uriString);
            }
        }

    }

}
