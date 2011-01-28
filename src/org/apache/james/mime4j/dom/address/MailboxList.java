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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable, random-access list of Mailbox objects.
 */
public class MailboxList extends AbstractList<Mailbox> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Mailbox> mailboxes;

    /**
     * @param mailboxes
     *            A List that contains only Mailbox objects.
     * @param dontCopy
     *            true iff it is not possible for the mailboxes list to be
     *            modified by someone else.
     */
    public MailboxList(List<Mailbox> mailboxes, boolean dontCopy) {
        if (mailboxes != null)
            this.mailboxes = dontCopy ? mailboxes : new ArrayList<Mailbox>(
                    mailboxes);
        else
            this.mailboxes = Collections.emptyList();
    }

    /**
     * The number of elements in this list.
     */
    @Override
    public int size() {
        return mailboxes.size();
    }

    /**
     * Gets an address.
     */
    @Override
    public Mailbox get(int index) {
        return mailboxes.get(index);
    }

}
