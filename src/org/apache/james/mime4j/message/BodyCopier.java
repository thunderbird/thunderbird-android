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

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Disposable;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;

/**
 * Utility class for copying message bodies.
 */
public class BodyCopier {

    private BodyCopier() {
    }

    /**
     * Returns a copy of the given {@link Body} that can be used (and modified)
     * independently of the original. The copy should be
     * {@link Disposable#dispose() disposed of} when it is no longer needed.
     * <p>
     * The {@link Body#getParent() parent} of the returned copy is
     * <code>null</code>, that is, the copy is detached from the parent
     * entity of the original.
     *
     * @param body
     *            body to copy.
     * @return a copy of the given body.
     * @throws UnsupportedOperationException
     *             if <code>body</code> is an instance of {@link SingleBody}
     *             that does not support the {@link SingleBody#copy() copy()}
     *             operation (or contains such a <code>SingleBody</code>).
     * @throws IllegalArgumentException
     *             if <code>body</code> is <code>null</code> or
     *             <code>body</code> is a <code>Body</code> that is neither
     *             a {@link MessageImpl}, {@link Multipart} or {@link SingleBody}
     *             (or contains such a <code>Body</code>).
     */
    public static Body copy(Body body) {
        if (body == null)
            throw new IllegalArgumentException("Body is null");

        if (body instanceof Message)
            return new MessageImpl((Message) body);

        if (body instanceof Multipart)
            return new MultipartImpl((Multipart) body);

        if (body instanceof SingleBody)
            return ((SingleBody) body).copy();

        throw new IllegalArgumentException("Unsupported body class");
    }

}
