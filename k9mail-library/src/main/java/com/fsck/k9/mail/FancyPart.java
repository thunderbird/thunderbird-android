
package com.fsck.k9.mail;


import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.fsck.k9.mail.internet.MimeUtility;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.FieldParser;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentIdField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.field.ContentDispositionFieldImpl;
import org.apache.james.mime4j.field.ContentIdFieldImpl;
import org.apache.james.mime4j.field.ContentTransferEncodingFieldImpl;
import org.apache.james.mime4j.field.ContentTypeFieldImpl;
import org.apache.james.mime4j.stream.RawField;


public class FancyPart {
    private static final String PROTOCOL_PARAMETER = "protocol";


    private final Part wrappedPart;
    private final ContentTypeField contentTypeField;
    private final ContentTransferEncodingField contentTransferEncodingField;
    private final ContentDispositionField contentDispositionField;
    private final ContentIdField contentIdField;


    private FancyPart(Part wrappedPart, ContentTypeField contentTypeField,
            ContentTransferEncodingField contentTransferEncodingField, ContentDispositionField contentDispositionField,
            ContentIdField contentIdField) {
        this.wrappedPart = wrappedPart;
        this.contentTypeField = contentTypeField;
        this.contentTransferEncodingField = contentTransferEncodingField;
        this.contentDispositionField = contentDispositionField;
        this.contentIdField = contentIdField;
    }

    public static FancyPart from(Part part) {
        ContentTypeField contentTypeField = parseHeaderToField(
                part, FieldName.CONTENT_TYPE, ContentTypeFieldImpl.PARSER);

        ContentTransferEncodingField contentTransferEncodingField = parseHeaderToField(
                part, FieldName.CONTENT_TRANSFER_ENCODING, ContentTransferEncodingFieldImpl.PARSER);

        ContentDispositionField contentDispositionField = parseHeaderToField(
                part, FieldName.CONTENT_DISPOSITION, ContentDispositionFieldImpl.PARSER);

        ContentIdField contentIdField = parseHeaderToField(
                part, FieldName.CONTENT_ID, ContentIdFieldImpl.PARSER);

        return new FancyPart(part, contentTypeField, contentTransferEncodingField, contentDispositionField, contentIdField);
    }


    public String getMimeType() {
        if (contentTypeField == null) {
            return null;
        }
        return contentTypeField.getMimeType();
    }

    public String getBoundary() {
        if (contentTypeField == null) {
            return null;
        }
        return contentTypeField.getBoundary();
    }

    public String getContentTypeName() {
        if (contentTypeField == null) {
            return null;
        }
        return contentTypeField.getParameter("name");
    }

    public boolean isMultipart() {
        return contentTypeField != null && contentTypeField.isMultipart();
    }

    public boolean isDispositionInline() {
        return contentDispositionField == null || contentDispositionField.isInline();
    }

    public boolean isDispositionAttachment() {
        return !isDispositionInline();
    }

    public String getDispositionFilename() {
        if (contentDispositionField == null) {
            return null;
        }
        return contentDispositionField.getFilename();
    }

    public String getContentTransferEncoding() {
        if (contentTransferEncodingField == null) {
            return null;
        }
        return contentTransferEncodingField.getEncoding();
    }

    public String getContentId() {
        if (contentIdField == null) {
            return null;
        }
        return contentIdField.getId();
    }

    @Nullable
    public Long getDispositionSize() {
        if (contentDispositionField == null) {
            return null;
        }
        long size = contentDispositionField.getSize();
        if (size < 0) {
            return null;
        }
        return size;
    }

    public boolean isMimeType(String mimeType) {
        return MimeUtility.isSameMimeType(getMimeType(), mimeType);
    }

    public boolean isMatchingMimeType(String mimeTypePattern) {
        String mimeType = getMimeType();
        return mimeType != null && MimeUtility.mimeTypeMatches(mimeType, mimeTypePattern);
    }

    public String getCharset() {
        if (contentTypeField == null) {
            return null;
        }
        return contentTypeField.getCharset();
    }

    public String getPartName() {
        String partName = getContentTypeName();
        if (TextUtils.isEmpty(partName)) {
            partName = getDispositionFilename();
        }
        return partName;
    }

    public String getContentTypeProtocol() {
        return contentTypeField.getParameter(PROTOCOL_PARAMETER);
    }

    public boolean isMimeTypeAnyOf(String... wantedMimeTypes) {
        for (String wantedMimeType : wantedMimeTypes) {
            if (isMimeType(wantedMimeType)) {
                return true;
            }
        }

        return false;
    }

    public Part getWrappedPart() {
        return wrappedPart;
    }


    @Nullable
    /** @see org.apache.james.mime4j.field.Fields */
    private static <T extends ParsedField> T parseHeaderToField(Part part, String fieldName, FieldParser<T> parser) {
        String headerContent = MimeUtility.unfoldAndDecode(part.getRawFirstHeader(fieldName));
        if (headerContent == null) {
            return null;
        }
        RawField rawField = new RawField(fieldName, headerContent);
        return parser.parse(rawField, DecodeMonitor.SILENT);
    }
}
