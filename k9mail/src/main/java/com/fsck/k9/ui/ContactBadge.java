package com.fsck.k9.ui;


import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.RawContacts;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;


/**
 * ContactBadge replaces the android ContactBadge for custom drawing.
 * <p>
 * Based on QuickContactBadge:
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/QuickContactBadge.java
 */
public class ContactBadge extends ImageView implements OnClickListener {
    private static final int TOKEN_EMAIL_LOOKUP = 0;
    private static final int TOKEN_EMAIL_LOOKUP_AND_TRIGGER = 1;

    private static final String EXTRA_URI_CONTENT = "uri_content";

    private static final String[] EMAIL_LOOKUP_PROJECTION = new String[] {
            RawContacts.CONTACT_ID,
            Contacts.LOOKUP_KEY,
    };
    private static final int EMAIL_ID_COLUMN_INDEX = 0;
    private static final int EMAIL_LOOKUP_STRING_COLUMN_INDEX = 1;


    private Uri contactUri;
    private String contactEmail;
    private QueryHandler queryHandler;
    private Bundle extras = null;


    public ContactBadge(Context context) {
        this(context, null);
    }

    public ContactBadge(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        queryHandler = new QueryHandler(context.getContentResolver());
        setOnClickListener(this);
    }

    /**
     * True if a contact, an email address or a phone number has been assigned
     */
    private boolean isAssigned() {
        return contactUri != null || contactEmail != null;
    }

    /**
     * Assign the contact uri that this ContactBadge should be associated
     * with. Note that this is only used for displaying the QuickContact window and
     * won't bind the contact's photo for you. Call {@link #setImageDrawable(Drawable)} to set the
     * photo.
     *
     * @param contactUri
     *         Either a {@link Contacts#CONTENT_URI} or
     *         {@link Contacts#CONTENT_LOOKUP_URI} style URI.
     */
    public void assignContactUri(Uri contactUri) {
        this.contactUri = contactUri;
        contactEmail = null;
        onContactUriChanged();
    }

    /**
     * Assign a contact based on an email address. This should only be used when
     * the contact's URI is not available, as an extra query will have to be
     * performed to lookup the URI based on the email.
     *
     * @param emailAddress
     *         The email address of the contact.
     * @param lazyLookup
     *         If this is true, the lookup query will not be performed
     *         until this view is clicked.
     */
    public void assignContactFromEmail(String emailAddress, boolean lazyLookup) {
        assignContactFromEmail(emailAddress, lazyLookup, null);
    }

    /**
     * Assign a contact based on an email address. This should only be used when
     * the contact's URI is not available, as an extra query will have to be
     * performed to lookup the URI based on the email.
     *
     * @param emailAddress
     *         The email address of the contact.
     * @param lazyLookup
     *         If this is true, the lookup query will not be performed
     *         until this view is clicked.
     * @param extras
     *         A bundle of extras to populate the contact edit page with if the contact
     *         is not found and the user chooses to add the email address to an existing contact or
     *         create a new contact. Uses the same string constants as those found in
     *         {@link android.provider.ContactsContract.Intents.Insert}
     */

    public void assignContactFromEmail(String emailAddress, boolean lazyLookup, Bundle extras) {
        contactEmail = emailAddress;
        this.extras = extras;
        if (!lazyLookup) {
            queryHandler.startQuery(TOKEN_EMAIL_LOOKUP, null,
                    Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(contactEmail)),
                    EMAIL_LOOKUP_PROJECTION, null, null, null);
        } else {
            contactUri = null;
            onContactUriChanged();
        }
    }

    private void onContactUriChanged() {
        setEnabled(isAssigned());
    }

    @Override
    public void onClick(View v) {
        // If contact has been assigned, extras should no longer be null, but do a null check
        // anyway just in case assignContactFromPhone or Email was called with a null bundle or
        // wasn't assigned previously.
        final Bundle extras = (this.extras == null) ? new Bundle() : this.extras;
        if (contactUri != null) {
            QuickContact.showQuickContact(getContext(), ContactBadge.this, contactUri,
                    QuickContact.MODE_LARGE, null);
        } else if (contactEmail != null) {
            extras.putString(EXTRA_URI_CONTENT, contactEmail);
            queryHandler.startQuery(TOKEN_EMAIL_LOOKUP_AND_TRIGGER, extras,
                    Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(contactEmail)),
                    EMAIL_LOOKUP_PROJECTION, null, null, null);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ContactBadge.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ContactBadge.class.getName());
    }

    private class QueryHandler extends AsyncQueryHandler {

        QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            Uri lookupUri = null;
            Uri createUri = null;
            boolean trigger = false;
            Bundle extras = (cookie != null) ? (Bundle) cookie : new Bundle();
            try {
                switch (token) {
                    case TOKEN_EMAIL_LOOKUP_AND_TRIGGER:
                        trigger = true;
                        createUri = Uri.fromParts("mailto",
                                extras.getString(EXTRA_URI_CONTENT), null);

                        //$FALL-THROUGH$
                    case TOKEN_EMAIL_LOOKUP: {
                        if (cursor != null && cursor.moveToFirst()) {
                            long contactId = cursor.getLong(EMAIL_ID_COLUMN_INDEX);
                            String lookupKey = cursor.getString(EMAIL_LOOKUP_STRING_COLUMN_INDEX);
                            lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                        }
                        break;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            contactUri = lookupUri;
            onContactUriChanged();

            if (trigger && lookupUri != null) {
                // Found contact, so trigger QuickContact
                QuickContact.showQuickContact(
                        getContext(), ContactBadge.this, lookupUri, QuickContact.MODE_LARGE, null);
            } else if (createUri != null) {
                // Prompt user to add this person to contacts
                final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, createUri);
                extras.remove(EXTRA_URI_CONTENT);
                intent.putExtras(extras);
                getContext().startActivity(intent);
            }
        }
    }
}

