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

package org.apache.james.mime4j.stream;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.io.BufferedLineReaderInputStream;
import org.apache.james.mime4j.io.LimitedInputStream;
import org.apache.james.mime4j.io.LineNumberSource;
import org.apache.james.mime4j.io.LineReaderInputStream;
import org.apache.james.mime4j.io.LineReaderInputStreamAdaptor;
import org.apache.james.mime4j.io.MimeBoundaryInputStream;
import org.apache.james.mime4j.util.MimeUtil;

public class MimeEntity extends AbstractEntity {

    private final LineNumberSource lineSource;
    private final BufferedLineReaderInputStream inbuffer;

    private int recursionMode;
    private MimeBoundaryInputStream currentMimePartStream;
    private LineReaderInputStreamAdaptor dataStream;

    private byte[] tmpbuf;

    public MimeEntity(
            LineNumberSource lineSource,
            InputStream instream,
            MutableBodyDescriptor body,
            int startState,
            int endState,
            MimeEntityConfig config,
            DecodeMonitor monitor) {
        super(body, startState, endState, config, monitor);
        this.lineSource = lineSource;
        this.inbuffer = new BufferedLineReaderInputStream(
                instream,
                4 * 1024,
                config.getMaxLineLen());
        this.dataStream = new LineReaderInputStreamAdaptor(
                inbuffer,
                config.getMaxLineLen());
    }

    public MimeEntity(
            LineNumberSource lineSource,
            InputStream instream,
            MutableBodyDescriptor body,
            int startState,
            int endState,
            MimeEntityConfig config) {
        this(lineSource, instream, body, startState, endState, config, config.isStrictParsing() ? DecodeMonitor.STRICT : DecodeMonitor.SILENT);
    }

    public MimeEntity(
            LineNumberSource lineSource,
            InputStream instream,
            MutableBodyDescriptor body) {
        this(lineSource, instream, body, EntityStates.T_START_MESSAGE, EntityStates.T_END_MESSAGE,
                new MimeEntityConfig(), DecodeMonitor.SILENT);
    }

    public int getRecursionMode() {
        return recursionMode;
    }

    public void setRecursionMode(int recursionMode) {
        this.recursionMode = recursionMode;
    }

    public void stop() {
	this.inbuffer.truncate();
    }

    @Override
    protected int getLineNumber() {
        if (lineSource == null)
            return -1;
        else
            return lineSource.getLineNumber();
    }

    @Override
    protected LineReaderInputStream getDataStream() {
        return dataStream;
    }

    public EntityStateMachine advance() throws IOException, MimeException {
        switch (state) {
        case EntityStates.T_START_MESSAGE:
            state = EntityStates.T_START_HEADER;
            break;
        case EntityStates.T_START_BODYPART:
            state = EntityStates.T_START_HEADER;
            break;
        case EntityStates.T_START_HEADER:
        case EntityStates.T_FIELD:
            state = parseField() ? EntityStates.T_FIELD : EntityStates.T_END_HEADER;
            break;
        case EntityStates.T_END_HEADER:
            String mimeType = body.getMimeType();
            if (recursionMode == RecursionMode.M_FLAT) {
                state = EntityStates.T_BODY;
            } else if (MimeUtil.isMultipart(mimeType)) {
                state = EntityStates.T_START_MULTIPART;
                clearMimePartStream();
            } else if (recursionMode != RecursionMode.M_NO_RECURSE
                    && MimeUtil.isMessage(mimeType)) {
                state = EntityStates.T_BODY;
                return nextMessage();
            } else {
                state = EntityStates.T_BODY;
            }
            break;
        case EntityStates.T_START_MULTIPART:
            if (dataStream.isUsed()) {
                advanceToBoundary();
                state = EntityStates.T_END_MULTIPART;
                break;
            } else {
                createMimePartStream();
                state = EntityStates.T_PREAMBLE;

                if (!currentMimePartStream.isEmptyStream()) break;
                // continue to next state
            }
        case EntityStates.T_PREAMBLE:
		// removed specific code. Fallback to T_IN_BODYPART that
		// better handle missing parts.
		// Removed the T_IN_BODYPART state (always use T_PREAMBLE)
            advanceToBoundary();
            if (currentMimePartStream.eof() && !currentMimePartStream.isLastPart()) {
                monitor(Event.MIME_BODY_PREMATURE_END);
            } else {
                if (!currentMimePartStream.isLastPart()) {
                    clearMimePartStream();
                    createMimePartStream();
                    return nextMimeEntity();
                }
            }
            clearMimePartStream();
            state = EntityStates.T_EPILOGUE;
            break;
        case EntityStates.T_EPILOGUE:
            state = EntityStates.T_END_MULTIPART;
            break;
        case EntityStates.T_BODY:
        case EntityStates.T_END_MULTIPART:
            state = endState;
            break;
        default:
            if (state == endState) {
                state = EntityStates.T_END_OF_STREAM;
                break;
            }
            throw new IllegalStateException("Invalid state: " + stateToString(state));
        }
        return null;
    }

    private void createMimePartStream() throws MimeException, IOException {
        String boundary = body.getBoundary();
	// TODO move the following lines inside the MimeBoundaryInputStream constructor
        int bufferSize = 2 * boundary.length();
        if (bufferSize < 4096) {
            bufferSize = 4096;
        }
        try {
            inbuffer.ensureCapacity(bufferSize);
            currentMimePartStream = new MimeBoundaryInputStream(inbuffer, boundary);
        } catch (IllegalArgumentException e) {
            // thrown when boundary is too long
            throw new MimeException(e.getMessage(), e);
        }
        dataStream = new LineReaderInputStreamAdaptor(
                currentMimePartStream,
                config.getMaxLineLen());
    }

    private void clearMimePartStream() {
        currentMimePartStream = null;
        dataStream = new LineReaderInputStreamAdaptor(
                inbuffer,
                config.getMaxLineLen());
    }

    private void advanceToBoundary() throws IOException {
        if (!dataStream.eof()) {
            if (tmpbuf == null) {
                tmpbuf = new byte[2048];
            }
            InputStream instream = getLimitedContentStream();
            while (instream.read(tmpbuf)!= -1) {
            }
        }
    }

    private EntityStateMachine nextMessage() {
        // optimize nesting of streams returning the "lower" stream instead of
        // always return dataStream (that would add a LineReaderInputStreamAdaptor in the chain)
        InputStream instream = currentMimePartStream != null ? currentMimePartStream : inbuffer;
        instream = decodedStream(instream);
        return nextMimeEntity(EntityStates.T_START_MESSAGE, EntityStates.T_END_MESSAGE, instream);
    }

    private InputStream decodedStream(InputStream instream) {
        String transferEncoding = body.getTransferEncoding();
        if (MimeUtil.isBase64Encoding(transferEncoding)) {
            instream = new Base64InputStream(instream, monitor);
        } else if (MimeUtil.isQuotedPrintableEncoded(transferEncoding)) {
            instream = new QuotedPrintableInputStream(instream, monitor);
        }
        return instream;
    }

    private EntityStateMachine nextMimeEntity() {
	return nextMimeEntity(EntityStates.T_START_BODYPART, EntityStates.T_END_BODYPART, currentMimePartStream);
    }

    private EntityStateMachine nextMimeEntity(int startState, int endState, InputStream instream) {
        if (recursionMode == RecursionMode.M_RAW) {
            RawEntity message = new RawEntity(instream);
            return message;
        } else {
            MimeEntity mimeentity = new MimeEntity(
                    lineSource,
                    instream,
                    body.newChild(),
                    startState,
                    endState,
                    config,
                    monitor);
            mimeentity.setRecursionMode(recursionMode);
            return mimeentity;
        }
    }

    private InputStream getLimitedContentStream() {
        long maxContentLimit = config.getMaxContentLen();
        if (maxContentLimit >= 0) {
            return new LimitedInputStream(dataStream, maxContentLimit);
        } else {
            return dataStream;
        }
    }

    /**
     * @see org.apache.james.mime4j.stream.EntityStateMachine#getContentStream()
     */
    public InputStream getContentStream() {
        switch (state) {
        case EntityStates.T_START_MULTIPART:
        case EntityStates.T_PREAMBLE:
        case EntityStates.T_EPILOGUE:
        case EntityStates.T_BODY:
            return getLimitedContentStream();
        default:
            throw new IllegalStateException("Invalid state: " + stateToString(state));
        }
    }

    /**
     * @see org.apache.james.mime4j.stream.EntityStateMachine#getDecodedContentStream()
     */
    public InputStream getDecodedContentStream() throws IllegalStateException {
        return decodedStream(getContentStream());
    }

}
