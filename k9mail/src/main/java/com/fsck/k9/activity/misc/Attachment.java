package com.fsck.k9.activity.misc;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Container class for information about an attachment.
 *
 * This is used by {@link com.fsck.k9.activity.MessageCompose} to fetch and manage attachments.
 */
public class Attachment implements Parcelable {
    /**
     * The URI pointing to the source of the attachment.
     *
     * In most cases this will be a {@code content://}-URI.
     */
    public Uri uri;

    /**
     * The current loading state.
     */
    public LoadingState state;

    /**
     * The ID of the loader that is used to load the metadata or contents.
     */
    public int loaderId;

    /**
     * The content type of the attachment.
     *
     * Only valid when {@link #state} is {@link LoadingState#METADATA} or
     * {@link LoadingState#COMPLETE}.
     */
    public String contentType;

    /**
     * The (file)name of the attachment.
     *
     * Only valid when {@link #state} is {@link LoadingState#METADATA} or
     * {@link LoadingState#COMPLETE}.
     */
    public String name;

    /**
     * The size of the attachment.
     *
     * Only valid when {@link #state} is {@link LoadingState#METADATA} or
     * {@link LoadingState#COMPLETE}.
     */
    public long size;

    /**
     * The name of the temporary file containing the local copy of the attachment.
     *
     * Only valid when {@link #state} is {@link LoadingState#COMPLETE}.
     */
    public String filename;


    public Attachment() {}

    public static enum LoadingState {
        /**
         * The only thing we know about this attachment is {@link #uri}.
         */
        URI_ONLY,

        /**
         * The metadata of this attachment have been loaded.
         *
         * {@link #contentType}, {@link #name}, and {@link #size} should contain usable values.
         */
        METADATA,

        /**
         * The contents of the attachments have been copied to the temporary file {@link #filename}.
         */
        COMPLETE,

        /**
         * Something went wrong while trying to fetch the attachment's contents.
         */
        CANCELLED
    }


    // === Parcelable ===

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeSerializable(state);
        dest.writeInt(loaderId);
        dest.writeString(contentType);
        dest.writeString(name);
        dest.writeLong(size);
        dest.writeString(filename);
    }

    public static final Parcelable.Creator<Attachment> CREATOR =
            new Parcelable.Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    public Attachment(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        state = (LoadingState) in.readSerializable();
        loaderId = in.readInt();
        contentType = in.readString();
        name = in.readString();
        size = in.readLong();
        filename = in.readString();
    }
}
