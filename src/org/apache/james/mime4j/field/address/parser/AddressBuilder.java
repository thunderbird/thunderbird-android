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

package org.apache.james.mime4j.field.address.parser;

import java.io.StringReader;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Group;
import org.apache.james.mime4j.dom.address.Mailbox;

public class AddressBuilder {

    /**
     * Parses the specified raw string into an address.
     *
     * @param rawAddressString
     *            string to parse.
     * @param monitor the DecodeMonitor to be used while parsing/decoding
     * @return an <code>Address</code> object for the specified string.
     * @throws ParseException if the raw string does not represent a single address.
     */
    public static Address parseAddress(String rawAddressString, DecodeMonitor monitor) throws ParseException {
        AddressListParser parser = new AddressListParser(new StringReader(
                rawAddressString));
        return Builder.getInstance().buildAddress(parser.parseAddress(), monitor);
    }

    public static Address parseAddress(String rawAddressString) throws ParseException {
        return parseAddress(rawAddressString, DecodeMonitor.STRICT);
    }

    /**
     * Parse the address list string, such as the value of a From, To, Cc, Bcc,
     * Sender, or Reply-To header.
     *
     * The string MUST be unfolded already.
     * @param monitor the DecodeMonitor to be used while parsing/decoding
     */
    public static AddressList parseAddressList(String rawAddressList, DecodeMonitor monitor)
            throws ParseException {
        AddressListParser parser = new AddressListParser(new StringReader(
                rawAddressList));
        try {
            return Builder.getInstance().buildAddressList(parser.parseAddressList(), monitor);
        } catch (RuntimeException e) {
            throw new ParseException(e.getMessage());
        }
    }

    public static AddressList parseAddressList(String rawAddressList) throws ParseException {
        return parseAddressList(rawAddressList, DecodeMonitor.STRICT);
    }

    /**
     * Parses the specified raw string into a mailbox address.
     *
     * @param rawMailboxString
     *            string to parse.
     * @param monitor the DecodeMonitor to be used while parsing/decoding.
     * @return a <code>Mailbox</code> object for the specified string.
     * @throws ParseException
     *             if the raw string does not represent a single mailbox
     *             address.
     */
    public static Mailbox parseMailbox(String rawMailboxString, DecodeMonitor monitor) throws ParseException {
        AddressListParser parser = new AddressListParser(new StringReader(
                rawMailboxString));
        return Builder.getInstance().buildMailbox(parser.parseMailbox(), monitor);
    }

    public static Mailbox parseMailbox(String rawMailboxString) throws ParseException {
        return parseMailbox(rawMailboxString, DecodeMonitor.STRICT);
    }

    /**
     * Parses the specified raw string into a group address.
     *
     * @param rawGroupString
     *            string to parse.
     * @return a <code>Group</code> object for the specified string.
     * @throws ParseException
     *             if the raw string does not represent a single group address.
     */
    public static Group parseGroup(String rawGroupString, DecodeMonitor monitor) throws ParseException {
        Address address = parseAddress(rawGroupString, monitor);
        if (!(address instanceof Group))
            throw new ParseException("Not a group address");

        return (Group) address;
    }

    public static Group parseGroup(String rawGroupString) throws ParseException {
        return parseGroup(rawGroupString, DecodeMonitor.STRICT);
    }

}
