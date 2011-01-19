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

import org.apache.james.mime4j.MimeException;

/**
 * A MessageBuilderFactory is used to create EntityBuilder instances.
 *
 * MessageBuilderFactory.newInstance() is used to get access to an implementation
 * of MessageBuilderFactory.
 * Then the method newMessageBuilder is used to create a new EntityBuilder object.
 */
public abstract class MessageBuilderFactory {

    public abstract MessageBuilder newMessageBuilder() throws MimeException;

    public static MessageBuilderFactory newInstance() throws MimeException {
        return ServiceLoader.load(MessageBuilderFactory.class);
    }

    public abstract void setAttribute(String name, Object value) throws IllegalArgumentException;

}
