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

import org.apache.james.mime4j.MimeException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents the interal state of a MIME entity, which is being retrieved
 * from an input stream by a MIME parser.
 */
public interface EntityStateMachine {

    /**
     * Return the current state of the entity.
     *
     * @see EntityStates
     *
     * @return current state
     */
    int getState();

    /**
     * Sets the current recursion mode.
     * The recursion mode specifies the approach taken to parsing parts.
     * {@link RecursionMode#M_RAW} mode does not parse the part at all.
     * {@link RecursionMode#M_RECURSE} mode recursively parses each mail
     * when an <code>message/rfc822</code> part is encounted;
     * {@link RecursionMode#M_NO_RECURSE} does not.
     *
     * @see RecursionMode
     *
     * @param recursionMode
     */
    void setRecursionMode(int recursionMode);

    /**
     * Advances the state machine to the next state in the
     * process of the MIME stream parsing. This method
     * may return an new state machine that represents an embedded
     * entity, which must be parsed before the parsing process of
     * the current entity can proceed.
     *
     * @return a state machine of an embedded entity, if encountered,
     * <code>null</code> otherwise.
     *
     * @throws IOException if an I/O error occurs.
     * @throws MimeException if the message can not be processed due
     *  to the MIME specification violation.
     */
    EntityStateMachine advance() throws IOException, MimeException;

    /**
     * Returns description of the entity body.
     *
     * @return body description
     *
     * @throws IllegalStateException if the body description cannot be
     *  obtained at the current stage of the parsing process.
     */
    BodyDescriptor getBodyDescriptor() throws IllegalStateException;

    /**
     * Returns content stream of the entity body.
     *
     * @return input stream
     *
     * @throws IllegalStateException if the content stream cannot be
     *  obtained at the current stage of the parsing process.
     */
    InputStream getContentStream() throws IllegalStateException;

    /**
     * Returns the decoded content stream of the entity body.
     *
     * @return input stream
     *
     * @throws IllegalStateException if the content stream cannot be
     *  obtained at the current stage of the parsing process.
     */
    InputStream getDecodedContentStream() throws IllegalStateException;

    /**
     * Returns current header field.
     *
     * @return header field
     *
     * @throws IllegalStateException if a header field cannot be
     *  obtained at the current stage of the parsing process.
     */
    RawField getField() throws IllegalStateException;

}
