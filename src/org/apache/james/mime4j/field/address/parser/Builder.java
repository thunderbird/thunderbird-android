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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.DomainList;
import org.apache.james.mime4j.dom.address.Group;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;

/**
 * Transforms the JJTree-generated abstract syntax tree into a graph of
 * org.apache.james.mime4j.dom.address objects.
 */
class Builder {

    private static Builder singleton = new Builder();

    public static Builder getInstance() {
        return singleton;
    }

    public AddressList buildAddressList(ASTaddress_list node, DecodeMonitor monitor) throws ParseException {
        List<Address> list = new ArrayList<Address>();
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            ASTaddress childNode = (ASTaddress) node.jjtGetChild(i);
            Address address = buildAddress(childNode, monitor);
            list.add(address);
        }
        return new AddressList(list, true);
    }

    public Address buildAddress(ASTaddress node, DecodeMonitor monitor) throws ParseException {
        ChildNodeIterator it = new ChildNodeIterator(node);
        Node n = it.next();
        if (n instanceof ASTaddr_spec) {
            return buildAddrSpec((ASTaddr_spec) n);
        } else if (n instanceof ASTangle_addr) {
            return buildAngleAddr((ASTangle_addr) n);
        } else if (n instanceof ASTphrase) {
            String name = buildString((ASTphrase) n, false);
            Node n2 = it.next();
            if (n2 instanceof ASTgroup_body) {
                return new Group(name, buildGroupBody((ASTgroup_body) n2, monitor));
            } else if (n2 instanceof ASTangle_addr) {
                try {
                    name = DecoderUtil.decodeEncodedWords(name, monitor);
                } catch (IllegalArgumentException e) {
                    throw new ParseException(e.getMessage());
                }
                Mailbox mb = buildAngleAddr((ASTangle_addr) n2);
                return new Mailbox(name, mb.getRoute(), mb.getLocalPart(),
                        mb.getDomain());
            } else {
                throw new ParseException();
            }
        } else {
            throw new ParseException();
        }
    }

    private MailboxList buildGroupBody(ASTgroup_body node, DecodeMonitor monitor) throws ParseException {
        List<Mailbox> results = new ArrayList<Mailbox>();
        ChildNodeIterator it = new ChildNodeIterator(node);
        while (it.hasNext()) {
            Node n = it.next();
            if (n instanceof ASTmailbox)
                results.add(buildMailbox((ASTmailbox) n, monitor));
            else
                throw new ParseException();
        }
        return new MailboxList(results, true);
    }

    public Mailbox buildMailbox(ASTmailbox node, DecodeMonitor monitor) throws ParseException {
        ChildNodeIterator it = new ChildNodeIterator(node);
        Node n = it.next();
        if (n instanceof ASTaddr_spec) {
            return buildAddrSpec((ASTaddr_spec) n);
        } else if (n instanceof ASTangle_addr) {
            return buildAngleAddr((ASTangle_addr) n);
        } else if (n instanceof ASTname_addr) {
            return buildNameAddr((ASTname_addr) n, monitor);
        } else {
            throw new ParseException();
        }
    }

    private Mailbox buildNameAddr(ASTname_addr node, DecodeMonitor monitor) throws ParseException {
        ChildNodeIterator it = new ChildNodeIterator(node);
        Node n = it.next();
        String name;
        if (n instanceof ASTphrase) {
            name = buildString((ASTphrase) n, false);
        } else {
            throw new ParseException();
        }

        n = it.next();
        if (n instanceof ASTangle_addr) {
            try {
                name = DecoderUtil.decodeEncodedWords(name, monitor);
            } catch (IllegalArgumentException e) {
                throw new ParseException(e.getMessage());
            }
            Mailbox mb = buildAngleAddr((ASTangle_addr) n);
            return new Mailbox(name, mb.getRoute(), mb.getLocalPart(),
                    mb.getDomain());
        } else {
            throw new ParseException();
        }
    }

    private Mailbox buildAngleAddr(ASTangle_addr node) throws ParseException {
        ChildNodeIterator it = new ChildNodeIterator(node);
        DomainList route = null;
        Node n = it.next();
        if (n instanceof ASTroute) {
            route = buildRoute((ASTroute) n);
            n = it.next();
        } else if (n instanceof ASTaddr_spec) {
            // do nothing
        }
        else
            throw new ParseException();

        if (n instanceof ASTaddr_spec)
            return buildAddrSpec(route, (ASTaddr_spec) n);
        else
            throw new ParseException();
    }

    private DomainList buildRoute(ASTroute node) throws ParseException {
        List<String> results = new ArrayList<String>(node.jjtGetNumChildren());
        ChildNodeIterator it = new ChildNodeIterator(node);
        while (it.hasNext()) {
            Node n = it.next();
            if (n instanceof ASTdomain)
                results.add(buildString((ASTdomain) n, true));
            else
                throw new ParseException();
        }
        return new DomainList(results, true);
    }

    private Mailbox buildAddrSpec(ASTaddr_spec node) {
        return buildAddrSpec(null, node);
    }

    private Mailbox buildAddrSpec(DomainList route, ASTaddr_spec node) {
        ChildNodeIterator it = new ChildNodeIterator(node);
        String localPart = buildString((ASTlocal_part) it.next(), true);
        String domain = buildString((ASTdomain) it.next(), true);
        return new Mailbox(route, localPart, domain);
    }

    private String buildString(SimpleNode node, boolean stripSpaces) {
        Token head = node.firstToken;
        Token tail = node.lastToken;
        StringBuilder out = new StringBuilder();

        while (head != tail) {
            out.append(head.image);
            head = head.next;
            if (!stripSpaces)
                addSpecials(out, head.specialToken);
        }
        out.append(tail.image);

        return out.toString();
    }

    private void addSpecials(StringBuilder out, Token specialToken) {
        if (specialToken != null) {
            addSpecials(out, specialToken.specialToken);
            out.append(specialToken.image);
        }
    }

    private static class ChildNodeIterator implements Iterator<Node> {

        private SimpleNode simpleNode;
        private int index;
        private int len;

        public ChildNodeIterator(SimpleNode simpleNode) {
            this.simpleNode = simpleNode;
            this.len = simpleNode.jjtGetNumChildren();
            this.index = 0;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean hasNext() {
            return index < len;
        }

        public Node next() {
            return simpleNode.jjtGetChild(index++);
        }

    }
}
