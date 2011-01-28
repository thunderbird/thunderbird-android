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

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.io.LineReaderInputStream;
import org.apache.james.mime4j.io.MaxHeaderLengthLimitException;
import org.apache.james.mime4j.io.MaxHeaderLimitException;
import org.apache.james.mime4j.io.MaxLineLimitException;
import org.apache.james.mime4j.util.ByteArrayBuffer;
import org.apache.james.mime4j.util.CharsetUtil;

/**
 * Abstract MIME entity.
 */
public abstract class AbstractEntity implements EntityStateMachine {

    protected final int startState;
    protected final int endState;
    protected final MimeEntityConfig config;
    protected final MutableBodyDescriptor body;

    protected int state;

    private final ByteArrayBuffer linebuf;

    private int lineCount;
    private RawField field;
    private boolean endOfHeader;
    private int headerCount;
    protected final DecodeMonitor monitor;

    /**
     * Internal state, not exposed.
     */
    private static final int T_IN_BODYPART = -2;
    /**
     * Internal state, not exposed.
     */
    private static final int T_IN_MESSAGE = -3;

    AbstractEntity(
            MutableBodyDescriptor body,
            int startState,
            int endState,
            MimeEntityConfig config,
            DecodeMonitor monitor) {
        this.state = startState;
        this.startState = startState;
        this.endState = endState;
        this.config = config;
        this.body = body;
        this.linebuf = new ByteArrayBuffer(64);
        this.lineCount = 0;
        this.endOfHeader = false;
        this.headerCount = 0;
        this.monitor = monitor;
    }

    public int getState() {
        return state;
    }

    /**
     * Returns the current line number or <code>-1</code> if line number
     * information is not available.
     */
    protected abstract int getLineNumber();

    protected abstract LineReaderInputStream getDataStream();

    private ByteArrayBuffer fillFieldBuffer() throws IOException, MimeException {
        if (endOfHeader)
            throw new IllegalStateException();

        int maxHeaderLen = config.getMaxHeaderLen();
        LineReaderInputStream instream = getDataStream();
        ByteArrayBuffer fieldbuf = new ByteArrayBuffer(64);

        try {
            for (;;) {
                // If there's still data stuck in the line buffer
                // copy it to the field buffer
                int len = linebuf.length();
                if (maxHeaderLen > 0 && fieldbuf.length() + len >= maxHeaderLen) {
                    throw new MaxHeaderLengthLimitException("Maximum header length limit exceeded");
                }
                if (len > 0) {
                    fieldbuf.append(linebuf.buffer(), 0, len);
                }
                linebuf.clear();
                if (instream.readLine(linebuf) == -1) {
                    monitor(Event.HEADERS_PREMATURE_END);
                    endOfHeader = true;
                    break;
                }
                len = linebuf.length();
                if (len > 0 && linebuf.byteAt(len - 1) == '\n') {
                    len--;
                }
                if (len > 0 && linebuf.byteAt(len - 1) == '\r') {
                    len--;
                }
                if (len == 0) {
                    // empty line detected
                    endOfHeader = true;
                    break;
                }
                lineCount++;
                if (lineCount > 1) {
                    int ch = linebuf.byteAt(0);
                    if (ch != CharsetUtil.SP && ch != CharsetUtil.HT) {
                        // new header detected
                        break;
                    }
                }
            }
        } catch (MaxLineLimitException e) {
            throw new MimeException(e);
        }

        return fieldbuf;
    }

    protected boolean parseField() throws MimeException, IOException {
        int maxHeaderCount = config.getMaxHeaderCount();
        // the loop is here to transparently skip invalid headers
        for (;;) {
            if (endOfHeader) {
                return false;
            }
            if (maxHeaderCount > 0 && headerCount >= maxHeaderCount) {
                throw new MaxHeaderLimitException("Maximum header limit exceeded");
            }

            ByteArrayBuffer fieldbuf = fillFieldBuffer();
            headerCount++;

            // Strip away line delimiter
            int origLen = fieldbuf.length();
            int len = fieldbuf.length();
            if (len > 0 && fieldbuf.byteAt(len - 1) == '\n') {
                len--;
            }
            if (len > 0 && fieldbuf.byteAt(len - 1) == '\r') {
                len--;
            }
            fieldbuf.setLength(len);

            try {
		field = new RawField(fieldbuf);
		if (field.isObsoleteSyntax()) {
			monitor(Event.OBSOLETE_HEADER);
		}
                body.addField(field);
                return true;
            } catch (MimeException e) {
                monitor(Event.INVALID_HEADER);
                if (config.isMalformedHeaderStartsBody()) {
	                fieldbuf.setLength(origLen);
	                LineReaderInputStream instream = getDataStream();
	                if (!instream.unread(fieldbuf)) throw new MimeParseEventException(Event.INVALID_HEADER);
	                return false;
                }
            }
        }
    }

    /**
     * <p>Gets a descriptor for the current entity.
     * This method is valid if {@link #getState()} returns:</p>
     * <ul>
     * <li>{@link EntityStates#T_BODY}</li>
     * <li>{@link EntityStates#T_START_MULTIPART}</li>
     * <li>{@link EntityStates#T_EPILOGUE}</li>
     * <li>{@link EntityStates#T_PREAMBLE}</li>
     * </ul>
     * @return <code>BodyDescriptor</code>, not nulls
     */
    public BodyDescriptor getBodyDescriptor() {
        switch (getState()) {
        case EntityStates.T_BODY:
        case EntityStates.T_START_MULTIPART:
        case EntityStates.T_PREAMBLE:
        case EntityStates.T_EPILOGUE:
        case EntityStates.T_END_OF_STREAM:
            return body;
        default:
            throw new IllegalStateException("Invalid state :" + stateToString(state));
        }
    }

    /**
     * This method is valid, if {@link #getState()} returns {@link EntityStates#T_FIELD}.
     * @return String with the fields raw contents.
     * @throws IllegalStateException {@link #getState()} returns another
     *   value than {@link EntityStates#T_FIELD}.
     */
    public RawField getField() {
        switch (getState()) {
        case EntityStates.T_FIELD:
            return field;
        default:
            throw new IllegalStateException("Invalid state :" + stateToString(state));
        }
    }

    /**
     * Monitors the given event.
     * Subclasses may override to perform actions upon events.
     * Base implementation logs at warn.
     * @param event <code>Event</code>, not null
     * @throws MimeException subclasses may elect to throw this exception upon
     * invalid content
     * @throws IOException subclasses may elect to throw this exception
     */
    protected void monitor(Event event) throws MimeException, IOException {
        if (monitor.isListening()) {
            String message = message(event);
            if (monitor.warn(message, "ignoring")) {
                throw new MimeParseEventException(event);
            }
        }
    }

    /**
     * Creates an indicative message suitable for display
     * based on the given event and the current state of the system.
     * @param event <code>Event</code>, not null
     * @return message suitable for use as a message in an exception
     * or for logging
     */
    protected String message(Event event) {
        final String message;
        if (event == null) {
            message = "Event is unexpectedly null.";
        } else {
            message = event.toString();
        }

        int lineNumber = getLineNumber();
        if (lineNumber <= 0)
            return message;
        else
            return "Line " + lineNumber + ": " + message;
    }

    @Override
    public String toString() {
        return getClass().getName() + " [" + stateToString(state)
        + "][" + body.getMimeType() + "][" + body.getBoundary() + "]";
    }

    /**
     * Renders a state as a string suitable for logging.
     * @param state
     * @return rendered as string, not null
     */
    public static final String stateToString(int state) {
        final String result;
        switch (state) {
            case EntityStates.T_END_OF_STREAM:
                result = "End of stream";
                break;
            case EntityStates.T_START_MESSAGE:
                result = "Start message";
                break;
            case EntityStates.T_END_MESSAGE:
                result = "End message";
                break;
            case EntityStates.T_RAW_ENTITY:
                result = "Raw entity";
                break;
            case EntityStates.T_START_HEADER:
                result = "Start header";
                break;
            case EntityStates.T_FIELD:
                result = "Field";
                break;
            case EntityStates.T_END_HEADER:
                result = "End header";
                break;
            case EntityStates.T_START_MULTIPART:
                result = "Start multipart";
                break;
            case EntityStates.T_END_MULTIPART:
                result = "End multipart";
                break;
            case EntityStates.T_PREAMBLE:
                result = "Preamble";
                break;
            case EntityStates.T_EPILOGUE:
                result = "Epilogue";
                break;
            case EntityStates.T_START_BODYPART:
                result = "Start bodypart";
                break;
            case EntityStates.T_END_BODYPART:
                result = "End bodypart";
                break;
            case EntityStates.T_BODY:
                result = "Body";
                break;
            case T_IN_BODYPART:
                result = "Bodypart";
                break;
            case T_IN_MESSAGE:
                result = "In message";
                break;
            default:
                result = "Unknown";
                break;
        }
        return result;
    }

}
