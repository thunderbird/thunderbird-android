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

package org.apache.james.mime4j.field.address.formatter;

import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Group;
import org.apache.james.mime4j.dom.address.Mailbox;

public class AddressFormatter {

    /**
     * Formats the address as a human readable string, not including the route.
     * The resulting string is intended for display purposes only and cannot be
     * used for transport purposes.
     *
     * For example, if the unparsed address was
     *
     * <"Joe Cheng"@joecheng.com>
     *
     * this method would return
     *
     * <Joe Cheng@joecheng.com>
     *
     * which is not valid for transport; the local part would need to be
     * re-quoted.
     *
     * @param includeRoute
     *            <code>true</code> if the route should be included if it
     *            exists, <code>false</code> otherwise.
     * @return a string representation of this address intended to be displayed.
     */
    public static void format(final StringBuilder sb, final Address address, boolean includeRoute) {
        if (address == null) {
            return;
        }
        if (address instanceof Mailbox) {
            format(sb, (Mailbox) address, includeRoute);
        } else if (address instanceof Group) {
            format(sb, (Group) address, includeRoute);
        } else {
            throw new IllegalArgumentException("Unsuppported Address class: " + address.getClass());
        }
    }

    /**
     * Returns a string representation of this address that can be used for
     * transport purposes. The route is never included in this representation
     * because routes are obsolete and RFC 5322 states that obsolete syntactic
     * forms MUST NOT be generated.
     *
     * @return a string representation of this address intended for transport
     *         purposes.
     */
    public static void encode(final StringBuilder sb, final Address address) {
        if (address == null) {
            return;
        }
        if (address instanceof Mailbox) {
            encode(sb, (Mailbox) address);
        } else if (address instanceof Group) {
            encode(sb, (Group) address);
        } else {
            throw new IllegalArgumentException("Unsuppported Address class: " + address.getClass());
        }
    }

    public static void format(final StringBuilder sb, final Mailbox mailbox, boolean includeRoute) {
        if (sb == null) {
            throw new IllegalArgumentException("StringBuilder may not be null");
        }
        if (mailbox == null) {
            throw new IllegalArgumentException("Mailbox may not be null");
        }
        includeRoute &= mailbox.getRoute() != null;
        boolean includeAngleBrackets = mailbox.getName() != null || includeRoute;
        if (mailbox.getName() != null) {
            sb.append(mailbox.getName());
            sb.append(' ');
        }
        if (includeAngleBrackets) {
            sb.append('<');
        }
        if (includeRoute) {
            sb.append(mailbox.getRoute().toRouteString());
            sb.append(':');
        }
        sb.append(mailbox.getLocalPart());
        if (mailbox.getDomain() != null) {
            sb.append('@');
            sb.append(mailbox.getDomain());
        }
        if (includeAngleBrackets) {
            sb.append('>');
        }
    }

    public static String format(final Mailbox mailbox, boolean includeRoute) {
        StringBuilder sb = new StringBuilder();
        format(sb, mailbox, includeRoute);
        return sb.toString();
    }

    public static void encode(final StringBuilder sb, final Mailbox mailbox) {
        if (sb == null) {
            throw new IllegalArgumentException("StringBuilder may not be null");
        }
        if (mailbox == null) {
            throw new IllegalArgumentException("Mailbox may not be null");
        }
        if (mailbox.getName() != null) {
            sb.append(EncoderUtil.encodeAddressDisplayName(mailbox.getName()));
            sb.append(" <");
        }
        sb.append(EncoderUtil.encodeAddressLocalPart(mailbox.getLocalPart()));
        // domain = dot-atom / domain-literal
        // domain-literal = [CFWS] "[" *([FWS] dtext) [FWS] "]" [CFWS]
        // dtext = %d33-90 / %d94-126
        if (mailbox.getDomain() != null) {
            sb.append('@');
            sb.append(mailbox.getDomain());
        }
        if (mailbox.getName() != null) {
            sb.append('>');
        }
    }

    public static String encode(final Mailbox mailbox) {
        StringBuilder sb = new StringBuilder();
        encode(sb, mailbox);
        return sb.toString();
    }

    public static void format(final StringBuilder sb, final Group group, boolean includeRoute) {
        if (sb == null) {
            throw new IllegalArgumentException("StringBuilder may not be null");
        }
        if (group == null) {
            throw new IllegalArgumentException("Group may not be null");
        }
        sb.append(group.getName());
        sb.append(':');

        boolean first = true;
        for (Mailbox mailbox : group.getMailboxes()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(' ');
            format(sb, mailbox, includeRoute);
        }
        sb.append(";");
    }

    public static String format(final Group group, boolean includeRoute) {
        StringBuilder sb = new StringBuilder();
        format(sb, group, includeRoute);
        return sb.toString();
    }

    public static void encode(final StringBuilder sb, final Group group) {
        if (sb == null) {
            throw new IllegalArgumentException("StringBuilder may not be null");
        }
        if (group == null) {
            throw new IllegalArgumentException("Group may not be null");
        }
        sb.append(EncoderUtil.encodeAddressDisplayName(group.getName()));
        sb.append(':');
        boolean first = true;
        for (Mailbox mailbox : group.getMailboxes()) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }

            sb.append(' ');
            encode(sb, mailbox);
        }
        sb.append(';');
    }

    public static String encode(final Group group) {
        StringBuilder sb = new StringBuilder();
        encode(sb, group);
        return sb.toString();
    }

}