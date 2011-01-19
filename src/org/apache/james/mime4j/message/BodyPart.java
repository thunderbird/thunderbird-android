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

package org.apache.james.mime4j.message;

import java.util.Date;
import java.util.Map;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.field.ContentTransferEncodingFieldImpl;
import org.apache.james.mime4j.field.ContentTypeFieldImpl;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * Represents a MIME body part  (see RFC 2045).
 */
public class BodyPart extends Entity {

    /**
     * Creates a new empty <code>BodyPart</code>.
     */
    public BodyPart() {
    }

    /**
     * Creates a new <code>BodyPart</code> from the specified
     * <code>BodyPart</code>. The <code>BodyPart</code> instance is initialized
     * with copies of header and body of the specified <code>BodyPart</code>.
     * The parent entity of the new body part is <code>null</code>.
     * 
     * @param other
     *            body part to copy.
     * @throws UnsupportedOperationException
     *             if <code>other</code> contains a {@link SingleBody} that
     *             does not support the {@link SingleBody#copy() copy()}
     *             operation.
     * @throws IllegalArgumentException
     *             if <code>other</code> contains a <code>Body</code> that
     *             is neither a {@link Message}, {@link Multipart} or
     *             {@link SingleBody}.
     */
    public BodyPart(Entity other) {
        if (other.getHeader() != null) {
            setHeader(new Header(other.getHeader()));
        }

        if (other.getBody() != null) {
            Body bodyCopy = BodyCopier.copy(other.getBody());
            setBody(bodyCopy);
        }
    }

	@Override
	protected String newUniqueBoundary() {
		return MimeUtil.createUniqueBoundary();
	}

    protected ContentDispositionField newContentDisposition(
            String dispositionType, String filename, long size,
            Date creationDate, Date modificationDate, Date readDate) {
        return Fields.contentDisposition(dispositionType, filename, size,
                creationDate, modificationDate, readDate);
    }

    protected ContentDispositionField newContentDisposition(
            String dispositionType, Map<String, String> parameters) {
        return Fields.contentDisposition(dispositionType, parameters);
    }

    protected ContentTypeField newContentType(String mimeType,
            Map<String, String> parameters) {
        return Fields.contentType(mimeType, parameters);
    }

    protected ContentTransferEncodingField newContentTransferEncoding(
            String contentTransferEncoding) {
        return Fields.contentTransferEncoding(contentTransferEncoding);
    }

    protected String calcTransferEncoding(ContentTransferEncodingField f) {
        return ContentTransferEncodingFieldImpl.getEncoding(f);
    }

    protected String calcMimeType(ContentTypeField child, ContentTypeField parent) {
        return ContentTypeFieldImpl.getMimeType(child, parent);
    }

    protected String calcCharset(ContentTypeField contentType) {
        return ContentTypeFieldImpl.getCharset(contentType);
    }

}
