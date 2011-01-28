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

package org.apache.james.mime4j.dom.address;

import java.io.Serializable;
import java.util.List;

/**
 * The abstract base for classes that represent RFC2822 addresses. This includes
 * groups and mailboxes.
 */
public abstract class Address implements Serializable {

    private static final long serialVersionUID = 634090661990433426L;

    /**
     * Adds any mailboxes represented by this address into the given List. Note
     * that this method has default (package) access, so a doAddMailboxesTo
     * method is needed to allow the behavior to be overridden by subclasses.
     */
    final void addMailboxesTo(List<Mailbox> results) {
        doAddMailboxesTo(results);
    }

    /**
     * Adds any mailboxes represented by this address into the given List. Must
     * be overridden by concrete subclasses.
     */
    protected abstract void doAddMailboxesTo(List<Mailbox> results);

}