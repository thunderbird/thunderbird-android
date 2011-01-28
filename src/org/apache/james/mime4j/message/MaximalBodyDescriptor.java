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

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.datetime.DateTime;
import org.apache.james.mime4j.field.datetime.parser.DateTimeParser;
import org.apache.james.mime4j.field.datetime.parser.ParseException;
import org.apache.james.mime4j.field.language.parser.ContentLanguageParser;
import org.apache.james.mime4j.field.mimeversion.parser.MimeVersionParser;
import org.apache.james.mime4j.field.structured.parser.StructuredFieldParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.DefaultBodyDescriptor;
import org.apache.james.mime4j.stream.MutableBodyDescriptor;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * Parses and stores values for standard MIME header values.
 *
 */
public class MaximalBodyDescriptor extends DefaultBodyDescriptor {

    private static final int DEFAULT_MINOR_VERSION = 0;
    private static final int DEFAULT_MAJOR_VERSION = 1;
    private boolean isMimeVersionSet;
    private int mimeMinorVersion;
    private int mimeMajorVersion;
    private MimeException mimeVersionException;
    private String contentId;
    private boolean isContentIdSet;
    private String contentDescription;
    private boolean isContentDescriptionSet;
    private String contentDispositionType;
    private Map<String, String> contentDispositionParameters;
    private DateTime contentDispositionModificationDate;
    private MimeException contentDispositionModificationDateParseException;
    private DateTime contentDispositionCreationDate;
    private MimeException contentDispositionCreationDateParseException;
    private DateTime contentDispositionReadDate;
    private MimeException contentDispositionReadDateParseException;
    private long contentDispositionSize;
    private MimeException contentDispositionSizeParseException;
    private boolean isContentDispositionSet;
    private List<String> contentLanguage;
    private MimeException contentLanguageParseException;
    private boolean isContentLanguageSet;
    private MimeException contentLocationParseException;
    private String contentLocation;
    private boolean isContentLocationSet;
    private String contentMD5Raw;
    private boolean isContentMD5Set;

    protected MaximalBodyDescriptor() {
        this(null, null);
    }

    public MaximalBodyDescriptor(final BodyDescriptor parent, final DecodeMonitor monitor) {
        super(parent, monitor);
        isMimeVersionSet = false;
        mimeMajorVersion = DEFAULT_MAJOR_VERSION;
        mimeMinorVersion = DEFAULT_MINOR_VERSION;
        this.contentId = null;
        this.isContentIdSet = false;
        this.contentDescription = null;
        this.isContentDescriptionSet = false;
        this.contentDispositionType = null;
        this.contentDispositionParameters = Collections.emptyMap();
        this.contentDispositionModificationDate = null;
        this.contentDispositionModificationDateParseException = null;
        this.contentDispositionCreationDate = null;
        this.contentDispositionCreationDateParseException = null;
        this.contentDispositionReadDate = null;
        this.contentDispositionReadDateParseException = null;
        this.contentDispositionSize = -1;
        this.contentDispositionSizeParseException = null;
        this.isContentDispositionSet = false;
        this.contentLanguage = null;
        this.contentLanguageParseException = null;
        this.isContentIdSet = false;
        this.contentLocation = null;
        this.contentLocationParseException = null;
        this.isContentLocationSet = false;
        this.contentMD5Raw = null;
        this.isContentMD5Set = false;
    }

    public MutableBodyDescriptor newChild() {
        return new MaximalBodyDescriptor(this, getDecodeMonitor());
    }

    @Override
    public void addField(RawField field) throws MimeException {
        String name = field.getName();
        String value = field.getBody();
        name = name.trim().toLowerCase();
        if (MimeUtil.MIME_HEADER_MIME_VERSION.equals(name) && !isMimeVersionSet) {
            parseMimeVersion(value);
        } else if (MimeUtil.MIME_HEADER_CONTENT_ID.equals(name) && !isContentIdSet) {
            parseContentId(value);
        } else if (MimeUtil.MIME_HEADER_CONTENT_DESCRIPTION.equals(name) && !isContentDescriptionSet) {
            parseContentDescription(value);
        } else if (MimeUtil.MIME_HEADER_CONTENT_DISPOSITION.equals(name) && !isContentDispositionSet) {
            parseContentDisposition(value);
        } else if (MimeUtil.MIME_HEADER_LANGAUGE.equals(name) && !isContentLanguageSet) {
            parseLanguage(value);
        } else if (MimeUtil.MIME_HEADER_LOCATION.equals(name) && !isContentLocationSet) {
            parseLocation(value);
        } else if (MimeUtil.MIME_HEADER_MD5.equals(name) && !isContentMD5Set) {
            parseMD5(value);
        } else {
            super.addField(field);
        }
    }

    private void parseMD5(String value) {
        isContentMD5Set = true;
        if (value != null) {
            contentMD5Raw = value.trim();
        }
    }

    private void parseLocation(final String value) {
        isContentLocationSet = true;
        if (value != null) {
            final StringReader stringReader = new StringReader(value);
            final StructuredFieldParser parser = new StructuredFieldParser(stringReader);
            try {
                // From RFC2017 3.1
                /*
                 * Extraction of the URL string from the URL-parameter is even simpler:
                 * The enclosing quotes and any linear whitespace are removed and the
                 * remaining material is the URL string.
                 * Read more: http://www.faqs.org/rfcs/rfc2017.html#ixzz0aufO9nRL
                 */
                contentLocation = parser.parse().replaceAll("\\s", "");
            } catch (MimeException e) {
                contentLocationParseException = e;
            }
        }
    }

    private void parseLanguage(final String value) {
        isContentLanguageSet = true;
        if (value != null) {
            try {
                final ContentLanguageParser parser = new ContentLanguageParser(new StringReader(value));
                contentLanguage = parser.parse();
            } catch (MimeException e) {
                contentLanguageParseException = e;
            }
        }
    }

    private void parseContentDisposition(final String value) throws MimeException {
        isContentDispositionSet = true;
        contentDispositionParameters = DefaultBodyDescriptor.getHeaderParams(value, getDecodeMonitor());
        contentDispositionType = contentDispositionParameters.get("");

        final String contentDispositionModificationDate
            = contentDispositionParameters.get(MimeUtil.PARAM_MODIFICATION_DATE);
        if (contentDispositionModificationDate != null) {
            try {
                this.contentDispositionModificationDate = parseDate(contentDispositionModificationDate);
            } catch (ParseException e) {
                this.contentDispositionModificationDateParseException = e;
            }
        }

        final String contentDispositionCreationDate
            = contentDispositionParameters.get(MimeUtil.PARAM_CREATION_DATE);
        if (contentDispositionCreationDate != null) {
            try {
                this.contentDispositionCreationDate = parseDate(contentDispositionCreationDate);
            } catch (ParseException e) {
                this.contentDispositionCreationDateParseException = e;
            }
        }

        final String contentDispositionReadDate
            = contentDispositionParameters.get(MimeUtil.PARAM_READ_DATE);
        if (contentDispositionReadDate != null) {
            try {
                this.contentDispositionReadDate = parseDate(contentDispositionReadDate);
            } catch (ParseException e) {
                this.contentDispositionReadDateParseException = e;
            }
        }

        final String size = contentDispositionParameters.get(MimeUtil.PARAM_SIZE);
        if (size != null) {
            try {
                contentDispositionSize = Long.parseLong(size);
            } catch (NumberFormatException e) {
                this.contentDispositionSizeParseException = (MimeException) new MimeException(e.getMessage(), e).fillInStackTrace();
            }
        }
        contentDispositionParameters.remove("");
    }

    private DateTime parseDate(final String date) throws ParseException {
        final StringReader stringReader = new StringReader(date);
        final DateTimeParser parser = new DateTimeParser(stringReader);
        DateTime result = parser.date_time();
        return result;
    }

    private void parseContentDescription(String value) {
        if (value == null) {
            contentDescription = "";
        } else {
            contentDescription = value.trim();
        }
        isContentDescriptionSet = true;
    }

    private void parseContentId(final String value) {
        if (value == null) {
            contentId = "";
        } else {
            contentId = value.trim();
        }
        isContentIdSet = true;
    }

    private void parseMimeVersion(String value) {
        final StringReader reader = new StringReader(value);
        final MimeVersionParser parser = new MimeVersionParser(reader);
        try {
            parser.parse();
            final int major = parser.getMajorVersion();
            if (major != MimeVersionParser.INITIAL_VERSION_VALUE) {
                mimeMajorVersion = major;
            }
            final int minor = parser.getMinorVersion();
            if (minor != MimeVersionParser.INITIAL_VERSION_VALUE) {
                mimeMinorVersion = minor;
            }
        } catch (MimeException e) {
            this.mimeVersionException = e;
        }
        isMimeVersionSet = true;
    }

    /**
     * Gets the MIME major version
     * as specified by the <code>MIME-Version</code>
     * header.
     * Defaults to one.
     * @return positive integer
     */
    public int getMimeMajorVersion() {
        return mimeMajorVersion;
    }

    /**
     * Gets the MIME minor version
     * as specified by the <code>MIME-Version</code>
     * header.
     * Defaults to zero.
     * @return positive integer
     */
    public int getMimeMinorVersion() {
        return mimeMinorVersion;
    }


    /**
     * When the MIME version header exists but cannot be parsed
     * this field will be contain the exception.
     * @return <code>MimeException</code> if the mime header cannot
     * be parsed, null otherwise
     */
    public MimeException getMimeVersionParseException() {
        return mimeVersionException;
    }

    /**
     * Gets the value of the <a href='http://www.faqs.org/rfcs/rfc2045'>RFC</a>
     * <code>Content-Description</code> header.
     * @return value of the <code>Content-Description</code> when present,
     * null otherwise
     */
    public String getContentDescription() {
        return contentDescription;
    }

    /**
     * Gets the value of the <a href='http://www.faqs.org/rfcs/rfc2045'>RFC</a>
     * <code>Content-ID</code> header.
     * @return value of the <code>Content-ID</code> when present,
     * null otherwise
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Gets the disposition type of the <code>content-disposition</code> field.
     * The value is case insensitive and will be converted to lower case.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return content disposition type,
     * or null when this has not been set
     */
    public String getContentDispositionType() {
        return contentDispositionType;
    }

    /**
     * Gets the parameters of the <code>content-disposition</code> field.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return parameter value strings indexed by parameter name strings,
     * not null
     */
    public Map<String, String> getContentDispositionParameters() {
        return contentDispositionParameters;
    }

    /**
     * Gets the <code>filename</code> parameter value of the <code>content-disposition</code> field.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return filename parameter value,
     * or null when it is not present
     */
    public String getContentDispositionFilename() {
        return contentDispositionParameters.get(MimeUtil.PARAM_FILENAME);
    }

    /**
     * Gets the <code>modification-date</code> parameter value of the <code>content-disposition</code> field.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return modification-date parameter value,
     * or null when this is not present
     */
    public DateTime getContentDispositionModificationDate() {
        return contentDispositionModificationDate;
    }

    /**
     * Gets any exception thrown during the parsing of {@link #getContentDispositionModificationDate()}
     * @return <code>ParseException</code> when the modification-date parse fails,
     * null otherwise
     */
    public MimeException getContentDispositionModificationDateParseException() {
        return contentDispositionModificationDateParseException;
    }

    /**
     * Gets the <code>creation-date</code> parameter value of the <code>content-disposition</code> field.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return creation-date parameter value,
     * or null when this is not present
     */
    public DateTime getContentDispositionCreationDate() {
        return contentDispositionCreationDate;
    }

    /**
     * Gets any exception thrown during the parsing of {@link #getContentDispositionCreationDate()}
     * @return <code>ParseException</code> when the creation-date parse fails,
     * null otherwise
     */
    public MimeException getContentDispositionCreationDateParseException() {
        return contentDispositionCreationDateParseException;
    }

    /**
     * Gets the <code>read-date</code> parameter value of the <code>content-disposition</code> field.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return read-date parameter value,
     * or null when this is not present
     */
    public DateTime getContentDispositionReadDate() {
        return contentDispositionReadDate;
    }

    /**
     * Gets any exception thrown during the parsing of {@link #getContentDispositionReadDate()}
     * @return <code>ParseException</code> when the read-date parse fails,
     * null otherwise
     */
    public MimeException getContentDispositionReadDateParseException() {
        return contentDispositionReadDateParseException;
    }

    /**
     * Gets the <code>size</code> parameter value of the <code>content-disposition</code> field.
     * See <a href='http://www.faqs.org/rfcs/rfc2183.html'>RFC2183</a>.
     * @return size parameter value,
     * or -1 if this size has not been set
     */
    public long getContentDispositionSize() {
        return contentDispositionSize;
    }

    /**
     * Gets any exception thrown during the parsing of {@link #getContentDispositionSize()}
     * @return <code>ParseException</code> when the read-date parse fails,
     * null otherwise
     */
    public MimeException getContentDispositionSizeParseException() {
        return contentDispositionSizeParseException;
    }

    /**
     * Get the <code>content-language</code> header values.
     * Each applicable language tag will be returned in order.
     * See <a href='http://tools.ietf.org/html/rfc4646'>RFC4646</a>
     * <cite>http://tools.ietf.org/html/rfc4646</cite>.
     * @return list of language tag Strings,
     * or null if no header exists
     */
    public List<String> getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Gets any exception thrown during the parsing of {@link #getContentLanguage()}
     * @return <code>ParseException</code> when the content-language parse fails,
     * null otherwise
     */
    public MimeException getContentLanguageParseException() {
        return contentLanguageParseException;
    }


    /**
     * Get the <code>content-location</code> header value.
     * See <a href='http://tools.ietf.org/html/rfc2557'>RFC2557</a>
     * @return the URL content-location
     * or null if no header exists
     */
    public String getContentLocation() {
        return contentLocation;
    }

    /**
     * Gets any exception thrown during the parsing of {@link #getContentLocation()}
     * @return <code>ParseException</code> when the content-language parse fails,
     * null otherwise
     */
    public MimeException getContentLocationParseException() {
        return contentLocationParseException;
    }

    /**
     * Gets the raw, Base64 encoded value of the
     * <code>Content-MD5</code> field.
     * See <a href='http://tools.ietf.org/html/rfc1864'>RFC1864</a>.
     * @return raw encoded content-md5
     * or null if no header exists
     */
    public String getContentMD5Raw() {
        return contentMD5Raw;
    }


}
