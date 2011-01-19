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
 * An immutable, random-access list of Address objects.
 */
public class AddressList extends AbstractList<Address> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<? extends Address> addresses;

    /**
     * @param addresses
     *            A List that contains only Address objects.
     * @param dontCopy
     *            true iff it is not possible for the addresses list to be
     *            modified by someone else.
     */
    public AddressList(List<? extends Address> addresses, boolean dontCopy) {
        if (addresses != null)
            this.addresses = dontCopy ? addresses : new ArrayList<Address>(
                    addresses);
        else
            this.addresses = Collections.emptyList();
    }

    /**
     * The number of elements in this list.
     */
    @Override
    public int size() {
        return addresses.size();
    }

    /**
     * Gets an address.
     */
    @Override
    public Address get(int index) {
        return addresses.get(index);
    }

    /**
     * Returns a flat list of all mailboxes represented in this address list.
     * Use this if you don't care about grouping.
     */
    public MailboxList flatten() {
        // in the common case, all addresses are mailboxes
        boolean groupDetected = false;
        for (Address addr : addresses) {
            if (!(addr instanceof Mailbox)) {
                groupDetected = true;
                break;
            }
        }

        if (!groupDetected) {
            @SuppressWarnings("unchecked")
            final List<Mailbox> mailboxes = (List<Mailbox>) addresses;
            return new MailboxList(mailboxes, true);
        }

        List<Mailbox> results = new ArrayList<Mailbox>();
        for (Address addr : addresses) {
            addr.addMailboxesTo(results);
        }

        // copy-on-construct this time, because subclasses
        // could have held onto a reference to the results
        return new MailboxList(results, false);
    }

}
