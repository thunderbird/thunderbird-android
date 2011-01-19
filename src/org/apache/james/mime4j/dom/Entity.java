/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.dom;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.Field;
import org.apache.james.mime4j.dom.field.FieldName;

/**
 * MIME entity. An entity has a header and a body (see RFC 2045).
 */
public abstract class Entity implements Disposable {
    private Header header = null;
    private Body body = null;
    private Entity parent = null;

    /**
     * Creates a new <code>Entity</code>. Typically invoked implicitly by a
     * subclass constructor.
     */
    protected Entity() {
    }

    /**
     * Gets the parent entity of this entity.
     * Returns <code>null</code> if this is the root entity.
     *
     * @return the parent or <code>null</code>.
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * Sets the parent entity of this entity.
     *
     * @param parent the parent entity or <code>null</code> if
     *        this will be the root entity.
     */
    public void setParent(Entity parent) {
        this.parent = parent;
    }

    /**
     * Gets the entity header.
     *
     * @return the header.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Sets the entity header.
     *
     * @param header the header.
     */
    public void setHeader(Header header) {
        this.header = header;
    }

    /**
     * Gets the body of this entity.
     *
     * @return the body,
     */
    public Body getBody() {
        return body;
    }

    /**
     * Sets the body of this entity.
     *
     * @param body the body.
     * @throws IllegalStateException if the body has already been set.
     */
    public void setBody(Body body) {
        if (this.body != null)
            throw new IllegalStateException("body already set");

        this.body = body;
        body.setParent(this);
    }

    /**
     * Removes and returns the body of this entity. The removed body may be
     * attached to another entity. If it is no longer needed it should be
     * {@link Disposable#dispose() disposed} of.
     *
     * @return the removed body or <code>null</code> if no body was set.
     */
    public Body removeBody() {
        if (body == null)
            return null;

        Body body = this.body;
        this.body = null;
        body.setParent(null);

        return body;
    }

    /**
     * Sets the specified message as body of this entity and the content type to
     * &quot;message/rfc822&quot;. A <code>Header</code> is created if this
     * entity does not already have one.
     *
     * @param message
     *            the message to set as body.
     */
    public void setMessage(Message message) {
        setBody(message, "message/rfc822", null);
    }

    /**
     * Sets the specified multipart as body of this entity. Also sets the
     * content type accordingly and creates a message boundary string. A
     * <code>Header</code> is created if this entity does not already have
     * one.
     *
     * @param multipart
     *            the multipart to set as body.
     */
    public void setMultipart(Multipart multipart) {
        String mimeType = "multipart/" + multipart.getSubType();
        Map<String, String> parameters = Collections.singletonMap("boundary",
                newUniqueBoundary());

        setBody(multipart, mimeType, parameters);
    }

    /**
     * Sets the specified multipart as body of this entity. Also sets the
     * content type accordingly and creates a message boundary string. A
     * <code>Header</code> is created if this entity does not already have
     * one.
     *
     * @param multipart
     *            the multipart to set as body.
     * @param parameters
     *            additional parameters for the Content-Type header field.
     */
    public void setMultipart(Multipart multipart, Map<String, String> parameters) {
        String mimeType = "multipart/" + multipart.getSubType();
        if (!parameters.containsKey("boundary")) {
            parameters = new HashMap<String, String>(parameters);
            parameters.put("boundary", newUniqueBoundary());
        }

        setBody(multipart, mimeType, parameters);
    }

    /**
     * Sets the specified <code>TextBody</code> as body of this entity and the
     * content type to &quot;text/plain&quot;. A <code>Header</code> is
     * created if this entity does not already have one.
     *
     * @param textBody
     *            the <code>TextBody</code> to set as body.
     * @see org.apache.james.mime4j.message.BodyFactory#textBody(String)
     */
    public void setText(TextBody textBody) {
        setText(textBody, "plain");
    }

    /**
     * Sets the specified <code>TextBody</code> as body of this entity. Also
     * sets the content type according to the specified sub-type. A
     * <code>Header</code> is created if this entity does not already have
     * one.
     *
     * @param textBody
     *            the <code>TextBody</code> to set as body.
     * @param subtype
     *            the text subtype (e.g. &quot;plain&quot;, &quot;html&quot; or
     *            &quot;xml&quot;).
     * @see org.apache.james.mime4j.message.BodyFactory#textBody(String)
     */
    public void setText(TextBody textBody, String subtype) {
        String mimeType = "text/" + subtype;

        Map<String, String> parameters = null;
        String mimeCharset = textBody.getMimeCharset();
        if (mimeCharset != null && !mimeCharset.equalsIgnoreCase("us-ascii")) {
            parameters = Collections.singletonMap("charset", mimeCharset);
        }

        setBody(textBody, mimeType, parameters);
    }

    /**
     * Sets the body of this entity and sets the content-type to the specified
     * value. A <code>Header</code> is created if this entity does not already
     * have one.
     *
     * @param body
     *            the body.
     * @param mimeType
     *            the MIME media type of the specified body
     *            (&quot;type/subtype&quot;).
     */
    public void setBody(Body body, String mimeType) {
        setBody(body, mimeType, null);
    }

    /**
     * Sets the body of this entity and sets the content-type to the specified
     * value. A <code>Header</code> is created if this entity does not already
     * have one.
     *
     * @param body
     *            the body.
     * @param mimeType
     *            the MIME media type of the specified body
     *            (&quot;type/subtype&quot;).
     * @param parameters
     *            additional parameters for the Content-Type header field.
     */
    public void setBody(Body body, String mimeType,
            Map<String, String> parameters) {
        setBody(body);

        Header header = obtainHeader();
        header.setField(newContentType(mimeType, parameters));
    }

    /**
     * Determines the MIME type of this <code>Entity</code>. The MIME type
     * is derived by looking at the parent's Content-Type field if no
     * Content-Type field is set for this <code>Entity</code>.
     *
     * @return the MIME type.
     */
    public String getMimeType() {
        ContentTypeField child =
            getContentTypeField();
        ContentTypeField parent = getParent() != null
            ? (ContentTypeField) getParent().getHeader().
                                                getField(FieldName.CONTENT_TYPE)
            : null;

        return calcMimeType(child, parent);
    }

    private ContentTypeField getContentTypeField() {
        return (ContentTypeField) getHeader().getField(FieldName.CONTENT_TYPE);
    }

    /**
     * Determines the MIME character set encoding of this <code>Entity</code>.
     *
     * @return the MIME character set encoding.
     */
    public String getCharset() {
        return calcCharset((ContentTypeField) getHeader().getField(FieldName.CONTENT_TYPE));
    }

    /**
     * Determines the transfer encoding of this <code>Entity</code>.
     *
     * @return the transfer encoding.
     */
    public String getContentTransferEncoding() {
        ContentTransferEncodingField f = (ContentTransferEncodingField)
                        getHeader().getField(FieldName.CONTENT_TRANSFER_ENCODING);

        return calcTransferEncoding(f);
    }

    /**
     * Sets the transfer encoding of this <code>Entity</code> to the specified
     * value.
     *
     * @param contentTransferEncoding
     *            transfer encoding to use.
     */
    public void setContentTransferEncoding(String contentTransferEncoding) {
        Header header = obtainHeader();
        header.setField(newContentTransferEncoding(contentTransferEncoding));
    }

    /**
     * Return the disposition type of the content disposition of this
     * <code>Entity</code>.
     *
     * @return the disposition type or <code>null</code> if no disposition
     *         type has been set.
     */
    public String getDispositionType() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        if (field == null)
            return null;

        return field.getDispositionType();
    }

    /**
     * Sets the content disposition of this <code>Entity</code> to the
     * specified disposition type. No filename, size or date parameters
     * are included in the content disposition.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     */
    public void setContentDisposition(String dispositionType) {
        Header header = obtainHeader();
        header.setField(newContentDisposition(dispositionType, null, -1, null,
                null, null));
    }

    /**
     * Sets the content disposition of this <code>Entity</code> to the
     * specified disposition type and filename. No size or date parameters are
     * included in the content disposition.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     */
    public void setContentDisposition(String dispositionType, String filename) {
        Header header = obtainHeader();
        header.setField(newContentDisposition(dispositionType, filename, -1,
                null, null, null));
    }

    /**
     * Sets the content disposition of this <code>Entity</code> to the
     * specified values. No date parameters are included in the content
     * disposition.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param size
     *            size parameter value or <code>-1</code> if the parameter
     *            should not be included.
     */
    public void setContentDisposition(String dispositionType, String filename,
            long size) {
        Header header = obtainHeader();
        header.setField(newContentDisposition(dispositionType, filename, size,
                null, null, null));
    }

    /**
     * Sets the content disposition of this <code>Entity</code> to the
     * specified values.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param size
     *            size parameter value or <code>-1</code> if the parameter
     *            should not be included.
     * @param creationDate
     *            creation-date parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param modificationDate
     *            modification-date parameter value or <code>null</code> if
     *            the parameter should not be included.
     * @param readDate
     *            read-date parameter value or <code>null</code> if the
     *            parameter should not be included.
     */
    public void setContentDisposition(String dispositionType, String filename,
            long size, Date creationDate, Date modificationDate, Date readDate) {
        Header header = obtainHeader();
        header.setField(newContentDisposition(dispositionType, filename, size,
                creationDate, modificationDate, readDate));
    }

    /**
     * Returns the filename parameter of the content disposition of this
     * <code>Entity</code>.
     *
     * @return the filename parameter of the content disposition or
     *         <code>null</code> if the filename has not been set.
     */
    public String getFilename() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        if (field == null)
            return null;

        return field.getFilename();
    }

    /**
     * Sets the filename parameter of the content disposition of this
     * <code>Entity</code> to the specified value. If this entity does not
     * have a content disposition header field a new one with disposition type
     * <code>attachment</code> is created.
     *
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should be removed.
     */
    public void setFilename(String filename) {
        Header header = obtainHeader();
        ContentDispositionField field = (ContentDispositionField) header
                .getField(FieldName.CONTENT_DISPOSITION);
        if (field == null) {
            if (filename != null) {
                header.setField(newContentDisposition(
                        ContentDispositionField.DISPOSITION_TYPE_ATTACHMENT,
                        filename, -1, null, null, null));
            }
        } else {
            String dispositionType = field.getDispositionType();
            Map<String, String> parameters = new HashMap<String, String>(field
                    .getParameters());
            if (filename == null) {
                parameters.remove(ContentDispositionField.PARAM_FILENAME);
            } else {
                parameters
                        .put(ContentDispositionField.PARAM_FILENAME, filename);
            }
            header.setField(newContentDisposition(dispositionType, parameters));
        }
    }

    /**
     * Determines if the MIME type of this <code>Entity</code> matches the
     * given one. MIME types are case-insensitive.
     *
     * @param type the MIME type to match against.
     * @return <code>true</code> on match, <code>false</code> otherwise.
     */
    public boolean isMimeType(String type) {
        return getMimeType().equalsIgnoreCase(type);
    }

    /**
     * Determines if the MIME type of this <code>Entity</code> is
     * <code>multipart/*</code>. Since multipart-entities must have
     * a boundary parameter in the <code>Content-Type</code> field this
     * method returns <code>false</code> if no boundary exists.
     *
     * @return <code>true</code> on match, <code>false</code> otherwise.
     */
    public boolean isMultipart() {
        ContentTypeField f = getContentTypeField();
        return f != null
                && f.getBoundary() != null
                && getMimeType().startsWith(
                        ContentTypeField.TYPE_MULTIPART_PREFIX);
    }

    /**
     * Disposes of the body of this entity. Note that the dispose call does not
     * get forwarded to the parent entity of this Entity.
     *
     * Subclasses that need to free resources should override this method and
     * invoke super.dispose().
     *
     * @see org.apache.james.mime4j.dom.Disposable#dispose()
     */
    public void dispose() {
        if (body != null) {
            body.dispose();
        }
    }

    /**
     * Obtains the header of this entity. Creates and sets a new header if this
     * entity's header is currently <code>null</code>.
     *
     * @return the header of this entity; never <code>null</code>.
     */
    Header obtainHeader() {
        if (header == null) {
            header = new Header();
        }
        return header;
    }

    /**
     * Obtains the header field with the specified name.
     *
     * @param <F>
     *            concrete field type.
     * @param fieldName
     *            name of the field to retrieve.
     * @return the header field or <code>null</code> if this entity has no
     *         header or the header contains no such field.
     */
    <F extends Field> F obtainField(String fieldName) {
        Header header = getHeader();
        if (header == null)
            return null;

        @SuppressWarnings("unchecked")
        F field = (F) header.getField(fieldName);
        return field;
    }

    protected abstract String newUniqueBoundary();

    protected abstract ContentDispositionField newContentDisposition(
            String dispositionType, String filename, long size,
            Date creationDate, Date modificationDate, Date readDate);

    protected abstract ContentDispositionField newContentDisposition(
            String dispositionType, Map<String, String> parameters);

    protected abstract ContentTypeField newContentType(String mimeType,
            Map<String, String> parameters);

    protected abstract ContentTransferEncodingField newContentTransferEncoding(
            String contentTransferEncoding);

    protected abstract String calcMimeType(ContentTypeField child, ContentTypeField parent);

    protected abstract String calcTransferEncoding(ContentTransferEncodingField f);

    protected abstract String calcCharset(ContentTypeField contentType);
}
