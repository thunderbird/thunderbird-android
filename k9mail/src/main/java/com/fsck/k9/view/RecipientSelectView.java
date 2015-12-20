package com.fsck.k9.view;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.AlternateRecipientAdapter;
import com.fsck.k9.activity.AlternateRecipientAdapter.AlternateRecipientListener;
import com.fsck.k9.activity.RecipientAdapter;
import com.fsck.k9.activity.RecipientLoader;
import com.fsck.k9.mail.Address;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.tokenautocomplete.TokenCompleteTextView;


public class RecipientSelectView extends TokenCompleteTextView<Recipient>
        implements LoaderCallbacks<List<Recipient>>, AlternateRecipientListener {

    public static final String ARG_QUERY = "query";

    private RecipientAdapter adapter;
    private String cryptoProvider;
    private LoaderManager loaderManager;

    private ListPopupWindow alternatesPopup;
    private AlternateRecipientAdapter alternatesAdapter;
    private Recipient alternatesPopupRecipient;
    private boolean attachedToWindow = true;
    private TokenListener<Recipient> listener;

    public RecipientSelectView(Context context) {
        super(context);
        initView(context);
    }

    public RecipientSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RecipientSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        // TODO: validator?

        alternatesPopup = new ListPopupWindow(context);
        alternatesAdapter = new AlternateRecipientAdapter(context, this);
        alternatesPopup.setAdapter(alternatesAdapter);

        // don't allow duplicates, based on equality of recipient objects, which is e-mail addresses
        allowDuplicates(false);

        // if a token is completed, pick an entry based on best guess.
        // Note that we override performCompletion, so this doesn't actually do anything
        performBestGuess(true);

        adapter = new RecipientAdapter(context);
        setAdapter(adapter);
    }

    @Override
    protected View getViewForObject(final Recipient recipient) {
        LayoutInflater l = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams") // root will be attached by caller
        View view = l.inflate(R.layout.recipient_token_item, null, false);

        RecipientTokenViewHolder holder = new RecipientTokenViewHolder(view);
        view.setTag(holder);

        bindObjectView(recipient, view);

        return view;
    }

    private void bindObjectView(Recipient recipient, View view) {
        RecipientTokenViewHolder holder = (RecipientTokenViewHolder) view.getTag();

        holder.vName.setText(recipient.getDisplayNameOrAddress());

        RecipientAdapter.setContactPhotoOrPlaceholder(getContext(), holder.vContactPhoto, recipient);

        boolean hasCryptoProvider = cryptoProvider != null;
        if (!hasCryptoProvider) {
            holder.cryptoStatusRed.setVisibility(View.GONE);
            holder.cryptoStatusOrange.setVisibility(View.GONE);
            holder.cryptoStatusGreen.setVisibility(View.GONE);
        } else if (recipient.cryptoStatus == RecipientCryptoStatus.UNAVAILABLE) {
            holder.cryptoStatusRed.setVisibility(View.VISIBLE);
            holder.cryptoStatusOrange.setVisibility(View.GONE);
            holder.cryptoStatusGreen.setVisibility(View.GONE);
        } else if (recipient.cryptoStatus == RecipientCryptoStatus.AVAILABLE_UNTRUSTED) {
            holder.cryptoStatusRed.setVisibility(View.GONE);
            holder.cryptoStatusOrange.setVisibility(View.VISIBLE);
            holder.cryptoStatusGreen.setVisibility(View.GONE);
        } else if (recipient.cryptoStatus == RecipientCryptoStatus.AVAILABLE_TRUSTED) {
            holder.cryptoStatusRed.setVisibility(View.GONE);
            holder.cryptoStatusOrange.setVisibility(View.GONE);
            holder.cryptoStatusGreen.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        Editable text = getText();

        if (text != null && action == MotionEvent.ACTION_UP) {

            int offset = getOffsetForPosition(event.getX(), event.getY());

            if (offset != -1) {
                TokenImageSpan[] links = text.getSpans(offset, offset, RecipientTokenSpan.class);
                if (links.length > 0) {
                    showAlternates(links[0].getToken());
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected Recipient defaultObject(String completionText) {
        Address[] parsedAddresses = Address.parse(completionText);
        if (parsedAddresses.length == 0 || parsedAddresses[0].getAddress() == null) {
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

        attachedToWindow = true;

        if (getContext() instanceof Activity) {
            loaderManager = ((Activity) getContext()).getLoaderManager();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attachedToWindow = false;
    }

    @Override
    public void showDropDown() {
        boolean cursorIsValid = adapter != null;
        if (!cursorIsValid) {
            return;
        }
        super.showDropDown();
    }

    @Override
    public void performCompletion() {
        if (getListSelection() == ListView.INVALID_POSITION && enoughToFilter()) {
            Object bestGuess;
            if (getAdapter().getCount() > 0) {
                bestGuess = getAdapter().getItem(0);
            } else {
                bestGuess = defaultObject(currentCompletionText());
            }
            if (bestGuess != null) {
                replaceText(convertSelectionToString(bestGuess));
            }
        } else {
            super.performCompletion();
        }
    }

    @Override
    protected void performFiltering(@NonNull CharSequence text, int start, int end, int keyCode) {
        String query = text.subSequence(start, end).toString();

        if (TextUtils.isEmpty(query) || query.length() < 2) {
            loaderManager.destroyLoader(0);
            return;
        }

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        loaderManager.restartLoader(0, args, this);
    }

    public void setCryptoProvider(String cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public void addRecipients(Recipient... recipients) {
        for (Recipient recipient : recipients) {
            addObject(recipient);
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


    private void showAlternates(Recipient recipient) {
        if (!attachedToWindow) {
            return;
        }

        alternatesPopupRecipient = recipient;
        loaderManager.restartLoader(1, null, this);
    }

    public void showAlternatesPopup(List<Recipient> data) {
        if (!attachedToWindow) {
            return;
        }

        // Copy anchor settings from the autocomplete dropdown
        View anchorView = getRootView().findViewById(getDropDownAnchor());
        alternatesPopup.setAnchorView(anchorView);
        alternatesPopup.setWidth(getDropDownWidth());

        alternatesAdapter.setCurrentRecipient(alternatesPopupRecipient);
        alternatesAdapter.setAlternateRecipientInfo(data);

        // Clear the checked item.
        alternatesPopup.show();
        ListView listView = alternatesPopup.getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    @Override
    public Loader<List<Recipient>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case 0: {
                String query = args != null && args.containsKey(ARG_QUERY) ? args.getString(ARG_QUERY) : "";
                // mAdapter.setSearchQuery(query);
                return new RecipientLoader(getContext(), cryptoProvider, query);
            }
            case 1: {
                Uri contactLookupUri = alternatesPopupRecipient.getContactLookupUri();
                if (contactLookupUri != null) {
                    return new RecipientLoader(getContext(), cryptoProvider, contactLookupUri);
                } else {
                    String address = alternatesPopupRecipient.address.getAddress();
                    return new RecipientLoader(getContext(), cryptoProvider, address);
                }
            }
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Recipient>> loader, List<Recipient> data) {
        switch (loader.getId()) {
            case 0:
                adapter.setRecipients(data);
                break;
            case 1:
                showAlternatesPopup(data);
                loaderManager.destroyLoader(1);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Recipient>> loader) {
        if (loader.getId() == 0) {
            adapter.setRecipients(null);
        }
    }

    public boolean hasUncompletedText() {
        return !TextUtils.isEmpty(currentCompletionText());
    }

    public enum RecipientCryptoStatus {
        UNDEFINED, UNAVAILABLE, AVAILABLE_UNTRUSTED, AVAILABLE_TRUSTED;

        public boolean isAvailable() {
            return this == AVAILABLE_TRUSTED || this == AVAILABLE_UNTRUSTED;
        }
    }

    private static class RecipientTokenViewHolder {

        private final TextView vName;
        private final ImageView vContactPhoto;
        private final View cryptoStatusRed;
        private final View cryptoStatusOrange;
        private final View cryptoStatusGreen;

        RecipientTokenViewHolder(View view) {
            vName = (TextView) view.findViewById(android.R.id.text1);

            vContactPhoto = (ImageView) view.findViewById(R.id.contact_photo);

            cryptoStatusRed = view.findViewById(R.id.contact_crypto_status_red);
            cryptoStatusOrange = view.findViewById(R.id.contact_crypto_status_orange);
            cryptoStatusGreen = view.findViewById(R.id.contact_crypto_status_green);
        }
    }

    public static class Recipient implements Serializable {
        @Nullable // null means the address is not associated with a contact
        public final Long contactId;
        public final String contactLookupKey;

        @NonNull
        public Address address;

        public String addressLabel;

        @Nullable // null if the contact has no photo. transient because we serialize this manually, see below.
        public transient Uri photoThumbnailUri;

        @NonNull
        private RecipientCryptoStatus cryptoStatus;

        public Recipient(@NonNull Address address) {
            this.address = address;
            this.contactId = null;
            this.cryptoStatus = RecipientCryptoStatus.UNDEFINED;
            this.contactLookupKey = null;
        }

        public Recipient(String name, String email, String addressLabel, long contactId, String lookupKey) {
            this.address = new Address(email, name);
            this.contactId = contactId;
            this.addressLabel = addressLabel;
            this.cryptoStatus = RecipientCryptoStatus.UNDEFINED;
            this.contactLookupKey = lookupKey;
        }

        public String getDisplayNameOrAddress() {
            String displayName = getDisplayName();
            if (displayName != null) {
                return displayName;
            }
            return address.getAddress();
        }

        public String getDisplayNameOrUnknown(Context context) {
            String displayName = getDisplayName();
            if (displayName != null) {
                return displayName;
            }
            return context.getString(R.string.unknown_recipient);
        }

        private String getDisplayName() {
            if (TextUtils.isEmpty(address.getPersonal())) {
                return null;
            }
            String displayName = address.getPersonal();
            if (addressLabel != null) {
                displayName += " (" + addressLabel + ")";
            }
            return displayName;
        }

        @NonNull
        public RecipientCryptoStatus getCryptoStatus() {
            return cryptoStatus;
        }

        public void setCryptoStatus(@NonNull RecipientCryptoStatus cryptoStatus) {
            this.cryptoStatus = cryptoStatus;
        }

        @Nullable
        public Uri getContactLookupUri() {
            if (contactId == null) {
                return null;
            }
            return Contacts.getLookupUri(contactId, contactLookupKey);
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

    @Override
    public void onRecipientRemove(Recipient currentRecipient) {
        alternatesPopup.dismiss();
        removeObject(currentRecipient);
    }

    @Override
    public void onRecipientChange(Recipient currentRecipient, Recipient alternateAddress) {
        alternatesPopup.dismiss();
        currentRecipient.address = alternateAddress.address;
        currentRecipient.addressLabel = alternateAddress.addressLabel;
        currentRecipient.cryptoStatus = alternateAddress.cryptoStatus;

        View recipientTokenView = getTokenViewForRecipient(currentRecipient);
        if (recipientTokenView == null) {
            Log.e(K9.LOG_TAG, "Tried to refresh invalid view token!");
            return;
        }
        bindObjectView(currentRecipient, recipientTokenView);
        if (listener != null) {
            listener.onTokenChanged(currentRecipient);
        }
        invalidate();
    }
    @Override
    /** This method builds the span given a recipient object. We override it with identical
     * functionality, but using the custom RecipientTokenSpan class which allows us to
     * retrieve the view for redrawing at a later point.
     */
    protected TokenImageSpan buildSpanForObject(Recipient obj) {
        if (obj == null) {
            return null;
        }
        View tokenView = getViewForObject(obj);
        return new RecipientTokenSpan(tokenView, obj, (int)maxTextWidth());
    }

    /** Find the token view tied to a given recipient. This method relies on spans to
     * be of the RecipientTokenSpan class, as created by the buildSpanForObject method.
     */
    private View getTokenViewForRecipient(Recipient currentRecipient) {
        Editable text = getText();
        if (text == null) {
            return null;
        }
        RecipientTokenSpan[] recipientSpans = text.getSpans(0, text.length(), RecipientTokenSpan.class);
        for (RecipientTokenSpan recipientSpan : recipientSpans) {
            if (recipientSpan.getToken() == currentRecipient) {
                return recipientSpan.view;
            }
        }
        return null;
    }

    private class RecipientTokenSpan extends TokenImageSpan {
        private final View view;

        public RecipientTokenSpan(View view, Recipient d, int token) {
            super(view, d, token);
            this.view = view;
        }
    }

    /** We use a specialized version of TokenCompleteTextView.TokenListener as well,
     * adding a callback for onTokenChanged.
     */
    public void setTokenListener(TokenListener<Recipient> l) {
        super.setTokenListener(l);
        listener = l;
    }

    public interface TokenListener<T> extends TokenCompleteTextView.TokenListener<T> {
        void onTokenChanged(T token);
    }

}
