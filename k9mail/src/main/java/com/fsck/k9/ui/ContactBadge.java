/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * modified by madrat- for customize its look
 */

package com.fsck.k9.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.RawContacts;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * ContactBadge replaces the android ContactBadge for custom drawing.
 */
public class ContactBadge extends ImageView implements OnClickListener {

	private Uri mContactUri;
	private String mContactEmail;
	private String mContactPhone;
	private QueryHandler mQueryHandler;
	private Bundle mExtras = null;

	protected String[] mExcludeMimes = null;

	static final private int TOKEN_EMAIL_LOOKUP = 0;
	static final private int TOKEN_PHONE_LOOKUP = 1;
	static final private int TOKEN_EMAIL_LOOKUP_AND_TRIGGER = 2;
	static final private int TOKEN_PHONE_LOOKUP_AND_TRIGGER = 3;

	static final private String EXTRA_URI_CONTENT = "uri_content";

	static final String[] EMAIL_LOOKUP_PROJECTION = new String[] {
			RawContacts.CONTACT_ID,
			Contacts.LOOKUP_KEY,
	};
	static final int EMAIL_ID_COLUMN_INDEX = 0;
	static final int EMAIL_LOOKUP_STRING_COLUMN_INDEX = 1;

	static final String[] PHONE_LOOKUP_PROJECTION = new String[] {
			PhoneLookup._ID,
			PhoneLookup.LOOKUP_KEY,
	};
	static final int PHONE_ID_COLUMN_INDEX = 0;
	static final int PHONE_LOOKUP_STRING_COLUMN_INDEX = 1;


	public ContactBadge(Context context) {
		this(context, null);
	}
	public ContactBadge(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public ContactBadge(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mQueryHandler = new QueryHandler(context.getContentResolver());
		setOnClickListener(this);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
	}

	/** This call has no effect anymore, as there is only one QuickContact mode */
	@SuppressWarnings("unused")
	public void setMode(int size) {
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	/** True if a contact, an email address or a phone number has been assigned */
	private boolean isAssigned() {
		return mContactUri != null || mContactEmail != null || mContactPhone != null;
	}

	/**
	 * Assign the contact uri that this ContactBadge should be associated
	 * with. Note that this is only used for displaying the QuickContact window and
	 * won't bind the contact's photo for you. Call {@link #setImageDrawable(Drawable)} to set the
	 * photo.
	 *
	 * @param contactUri Either a {@link Contacts#CONTENT_URI} or
	 *            {@link Contacts#CONTENT_LOOKUP_URI} style URI.
	 */
	public void assignContactUri(Uri contactUri) {
		mContactUri = contactUri;
		mContactEmail = null;
		mContactPhone = null;
		onContactUriChanged();
	}

	/**
	 * Assign a contact based on an email address. This should only be used when
	 * the contact's URI is not available, as an extra query will have to be
	 * performed to lookup the URI based on the email.
	 *
	 * @param emailAddress The email address of the contact.
	 * @param lazyLookup If this is true, the lookup query will not be performed
	 * until this view is clicked.
	 */
	public void assignContactFromEmail(String emailAddress, boolean lazyLookup) {
		assignContactFromEmail(emailAddress, lazyLookup, null);
	}

	/**
	 * Assign a contact based on an email address. This should only be used when
	 * the contact's URI is not available, as an extra query will have to be
	 * performed to lookup the URI based on the email.

	 @param emailAddress The email address of the contact.
	 @param lazyLookup If this is true, the lookup query will not be performed
	 until this view is clicked.
	 @param extras A bundle of extras to populate the contact edit page with if the contact
	 is not found and the user chooses to add the email address to an existing contact or
	 create a new contact. Uses the same string constants as those found in
	 {@link android.provider.ContactsContract.Intents.Insert}
	 */

	public void assignContactFromEmail(String emailAddress, boolean lazyLookup, Bundle extras) {
		mContactEmail = emailAddress;
		mExtras = extras;
		if (!lazyLookup) {
			mQueryHandler.startQuery(TOKEN_EMAIL_LOOKUP, null,
					Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(mContactEmail)),
					EMAIL_LOOKUP_PROJECTION, null, null, null);
		} else {
			mContactUri = null;
			onContactUriChanged();
		}
	}


	/**
	 * Assign a contact based on a phone number. This should only be used when
	 * the contact's URI is not available, as an extra query will have to be
	 * performed to lookup the URI based on the phone number.
	 *
	 * @param phoneNumber The phone number of the contact.
	 * @param lazyLookup If this is true, the lookup query will not be performed
	 * until this view is clicked.
	 */

	public void assignContactFromPhone(String phoneNumber, boolean lazyLookup) {
		assignContactFromPhone(phoneNumber, lazyLookup, new Bundle());
	}

	/**
	 * Assign a contact based on a phone number. This should only be used when
	 * the contact's URI is not available, as an extra query will have to be
	 * performed to lookup the URI based on the phone number.
	 *
	 * @param phoneNumber The phone number of the contact.
	 * @param lazyLookup If this is true, the lookup query will not be performed
	 * until this view is clicked.
	 * @param extras A bundle of extras to populate the contact edit page with if the contact
	 * is not found and the user chooses to add the phone number to an existing contact or
	 * create a new contact. Uses the same string constants as those found in
	 * {@link android.provider.ContactsContract.Intents.Insert}
	 */
	public void assignContactFromPhone(String phoneNumber, boolean lazyLookup, Bundle extras) {
		mContactPhone = phoneNumber;
		mExtras = extras;
		if (!lazyLookup) {
			mQueryHandler.startQuery(TOKEN_PHONE_LOOKUP, null,
					Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, mContactPhone),
					PHONE_LOOKUP_PROJECTION, null, null, null);
		} else {
			mContactUri = null;
			onContactUriChanged();
		}
	}

	private void onContactUriChanged() {
		setEnabled(isAssigned());
	}

	@Override
	public void onClick(View v) {
		// If contact has been assigned, mExtras should no longer be null, but do a null check
		// anyway just in case assignContactFromPhone or Email was called with a null bundle or
		// wasn't assigned previously.
		final Bundle extras = (mExtras == null) ? new Bundle() : mExtras;
		if (mContactUri != null) {
			QuickContact.showQuickContact(getContext(), ContactBadge.this, mContactUri,
					QuickContact.MODE_LARGE, mExcludeMimes);
		} else if (mContactEmail != null) {
			extras.putString(EXTRA_URI_CONTENT, mContactEmail);
			mQueryHandler.startQuery(TOKEN_EMAIL_LOOKUP_AND_TRIGGER, extras,
					Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(mContactEmail)),
					EMAIL_LOOKUP_PROJECTION, null, null, null);
		} else if (mContactPhone != null) {
			extras.putString(EXTRA_URI_CONTENT, mContactPhone);
			mQueryHandler.startQuery(TOKEN_PHONE_LOOKUP_AND_TRIGGER, extras,
					Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, mContactPhone),
					PHONE_LOOKUP_PROJECTION, null, null, null);
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

	/**
	 * Set a list of specific MIME-types to exclude and not display. For
	 * example, this can be used to hide the {@link Contacts#CONTENT_ITEM_TYPE}
	 * profile icon.
	 */
	public void setExcludeMimes(String[] excludeMimes) {
		mExcludeMimes = excludeMimes;
	}

	private class QueryHandler extends AsyncQueryHandler {

		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			Uri lookupUri = null;
			Uri createUri = null;
			boolean trigger = false;
			final Bundle extras = (cookie != null) ? (Bundle) cookie : new Bundle();
			try {
				switch(token) {
					case TOKEN_PHONE_LOOKUP_AND_TRIGGER:
						trigger = true;
						createUri = Uri.fromParts("tel", extras.getString(EXTRA_URI_CONTENT), null);

						//$FALL-THROUGH$
					case TOKEN_PHONE_LOOKUP: {
						if (cursor != null && cursor.moveToFirst()) {
							long contactId = cursor.getLong(PHONE_ID_COLUMN_INDEX);
							String lookupKey = cursor.getString(PHONE_LOOKUP_STRING_COLUMN_INDEX);
							lookupUri = Contacts.getLookupUri(contactId, lookupKey);
						}

						break;
					}
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

			mContactUri = lookupUri;
			onContactUriChanged();

			if (trigger && lookupUri != null) {
				// Found contact, so trigger QuickContact
				QuickContact.showQuickContact(getContext(), ContactBadge.this, lookupUri,
						QuickContact.MODE_LARGE, ContactBadge.this.mExcludeMimes);
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

