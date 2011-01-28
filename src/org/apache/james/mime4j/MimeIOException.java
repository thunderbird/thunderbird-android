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

package org.apache.james.mime4j;

import java.io.IOException;

/**
 * A wrapper class based on {@link IOException} for MIME protocol exceptions.
 * <p>
 * This exception is used to signal a <code>MimeException</code> in methods
 * that only permit <code>IOException</code> to be thrown.
 * <p>
 * The cause of a <code>MimeIOException</code> is always a
 * <code>MimeException</code> therefore.
 */
public class MimeIOException extends IOException {

    private static final long serialVersionUID = 5393613459533735409L;

    /**
     * Constructs an IO exception based on {@link MimeException}.
     *
     * @param cause the cause.
     */
    public MimeIOException(MimeException cause) {
        super(cause == null ? null : cause.getMessage());
        initCause(cause);
    }

    /**
     * Returns the <code>MimeException</code> that caused this
     * <code>MimeIOException</code>.
     *
     * @return the cause of this <code>MimeIOException</code>.
     */
    @Override
    public MimeException getCause() {
        return (MimeException) super.getCause();
    }

}
