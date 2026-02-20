package com.fsck.k9.activity.misc

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.fsck.k9.helper.MimeTypeUtil
import com.fsck.k9.message.Attachment.LoadingState

/**
 * Container class for information about an attachment.
 *
 * This is used by [com.fsck.k9.activity.MessageCompose] to fetch and manage attachments.
 *
 * @property uri The URI pointing to the source of the attachment.
 *   In most cases this will be a `content://`-URI.
 * @property state The current loading state.
 * @property loaderId The ID of the loader that is used to load the metadata or contents.
 * @property contentType The content type of the attachment.
 *   Valid iff [state] is [LoadingState.METADATA] or [LoadingState.COMPLETE].
 * @property allowMessageType `true` if we allow MIME types of `message/ *`, e.g. `message/rfc822`.
 * @property name The (file)name of the attachment.
 *   Valid iff [state] is [LoadingState.METADATA] or [LoadingState.COMPLETE].
 * @property size The size of the attachment. Valid iff [state] is
 *   [LoadingState.METADATA] or [LoadingState.COMPLETE].
 * @property fileName The name of the temporary file containing the local copy of the attachment.
 *   Valid iff [state] is [LoadingState.COMPLETE].
 * @property isInternalAttachment
 */
@Suppress("LongParameterList")
class Attachment(
    @JvmField val uri: Uri,
    override val state: LoadingState,
    @JvmField val loaderId: Int,
    override val contentType: String?,
    @JvmField val allowMessageType: Boolean,
    override val name: String?,
    override val size: Long?,
    override val fileName: String?,
    override val isInternalAttachment: Boolean,
) : Parcelable, com.fsck.k9.message.Attachment {

    val isSupportedImage: Boolean
        get() {
            if (contentType == null) return false

            return MimeTypeUtil.isSupportedImageType(contentType) ||
                (
                    MimeTypeUtil.isSameMimeType(MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE, contentType)
                        && fileName != null && MimeTypeUtil.isSupportedImageExtension(fileName)
                    )
        }

    private constructor(parcel: Parcel) : this(
        uri = requireNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                parcel.readParcelable(Uri::class.java.classLoader, Uri::class.java)
            } else {
                parcel.readParcelable(Uri::class.java.classLoader)
            },
        ),
        state = requireNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                parcel.readSerializable(
                    LoadingState::class.java.classLoader,
                    LoadingState::class.java,
                )
            } else {
                parcel.readSerializable() as LoadingState
            },
        ),
        loaderId = parcel.readInt(),
        contentType = parcel.readString(),
        allowMessageType = parcel.readInt() != 0,
        name = parcel.readString(),
        size = if (parcel.readInt() != 0) parcel.readLong() else null,
        fileName = parcel.readString(),
        isInternalAttachment = parcel.readInt() != 0,
    )

    fun deriveWithMetadataLoaded(
        usableContentType: String?,
        name: String?,
        size: Long,
    ): Attachment {
        check(state == LoadingState.URI_ONLY) {
            "deriveWithMetadataLoaded can only be called on a URI_ONLY attachment!"
        }
        return Attachment(
            uri = uri,
            state = LoadingState.METADATA,
            loaderId = loaderId,
            contentType = usableContentType,
            allowMessageType = allowMessageType,
            name = name,
            size = size,
            fileName = null,
            isInternalAttachment = isInternalAttachment,
        )
    }

    fun deriveWithLoadCancelled(): Attachment =
        Attachment(
            uri = uri,
            state = LoadingState.CANCELLED,
            loaderId = loaderId,
            contentType = contentType,
            allowMessageType = allowMessageType,
            name = name,
            size = size,
            fileName = null,
            isInternalAttachment = isInternalAttachment,
        )

    fun deriveWithLoadComplete(absolutePath: String?): Attachment {
        check(state == LoadingState.METADATA) {
            "deriveWithLoadComplete can only be called on a METADATA attachment!"
        }

        return Attachment(
            uri = uri,
            state = LoadingState.COMPLETE,
            loaderId = loaderId,
            contentType = contentType,
            allowMessageType = allowMessageType,
            name = name,
            size = size,
            fileName = absolutePath,
            isInternalAttachment = isInternalAttachment,
        )
    }

    // === Parcelable ===
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(uri, flags)
        dest.writeSerializable(state)
        dest.writeInt(loaderId)
        dest.writeString(contentType)
        dest.writeInt(if (allowMessageType) 1 else 0)
        dest.writeString(name)
        if (size != null) {
            dest.writeInt(1)
            dest.writeLong(size)
        } else {
            dest.writeInt(0)
        }
        dest.writeString(fileName)
        dest.writeInt(if (isInternalAttachment) 1 else 0)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<Attachment> =
            object : Parcelable.Creator<Attachment> {
                override fun createFromParcel(parcel: Parcel): Attachment =
                    Attachment(parcel)

                override fun newArray(size: Int): Array<Attachment?> {
                    return arrayOfNulls(size)
                }
            }

        @JvmStatic
        fun createAttachment(
            uri: Uri,
            loaderId: Int,
            contentType: String?,
            allowMessageType: Boolean,
            internalAttachment: Boolean,
        ): Attachment =
            Attachment(
                uri = uri,
                state = LoadingState.URI_ONLY,
                loaderId = loaderId,
                contentType = contentType,
                allowMessageType = allowMessageType,
                name = null,
                size = null,
                fileName = null,
                isInternalAttachment = internalAttachment,
            )
    }
}
