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

package org.apache.james.mime4j.field.address;

import org.apache.james.mime4j.field.address.parser.AddressListParser;
import org.apache.james.mime4j.field.address.parser.ParseException;

import java.io.StringReader;
import java.util.ArrayList;

/**
 * An immutable, random-access list of Address objects.
 *
 * 
 */
public class AddressList {
	
	private ArrayList addresses;

	/**
	 * @param addresses An ArrayList that contains only Address objects. 
	 * @param dontCopy true iff it is not possible for the addresses ArrayList to be modified by someone else.
	 */
	public AddressList(ArrayList addresses, boolean dontCopy) {
		if (addresses != null)
			this.addresses = (dontCopy ? addresses : (ArrayList) addresses.clone());
		else
			this.addresses = new ArrayList(0);
	}

	/**
	 * The number of elements in this list.
	 */
	public int size() {
		return addresses.size();
	}

	/**
	 * Gets an address. 
	 */
	public Address get(int index) {
		if (0 > index || size() <= index)
			throw new IndexOutOfBoundsException();
		return (Address) addresses.get(index);
	}

	/**
	 * Returns a flat list of all mailboxes represented
	 * in this address list. Use this if you don't care
	 * about grouping. 
	 */
	public MailboxList flatten() {
		// in the common case, all addresses are mailboxes
		boolean groupDetected = false;
		for (int i = 0; i < size(); i++) {
			if (!(get(i) instanceof Mailbox)) {
				groupDetected = true;
				break;
			}
		}
		
		if (!groupDetected)
			return new MailboxList(addresses, true);
		
		ArrayList results = new ArrayList();
		for (int i = 0; i < size(); i++) {
			Address addr = get(i);
			addr.addMailboxesTo(results);
		}
		
		// copy-on-construct this time, because subclasses
		// could have held onto a reference to the results
		return new MailboxList(results, false);
	}
	
	/**
	 * Dumps a representation of this address list to
	 * stdout, for debugging purposes.
	 */
	public void print() {
		for (int i = 0; i < size(); i++) {
			Address addr = get(i);
			System.out.println(addr.toString());
		}
	}



	/**
	 * Parse the address list string, such as the value 
	 * of a From, To, Cc, Bcc, Sender, or Reply-To
	 * header.
	 * 
	 * The string MUST be unfolded already.
	 */
	public static AddressList parse(String rawAddressList) throws ParseException {
		AddressListParser parser = new AddressListParser(new StringReader(rawAddressList));
		return Builder.getInstance().buildAddressList(parser.parse());
	}
	
	/**
	 * Test console.
	 */
	public static void main(String[] args) throws Exception {
		java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
		while (true) {
			try {
				System.out.print("> ");
				String line = reader.readLine();
				if (line.length() == 0 || line.toLowerCase().equals("exit") || line.toLowerCase().equals("quit")) {
					System.out.println("Goodbye.");
					return;
				}
				AddressList list = parse(line);
				list.print();
			}
			catch(Exception e) {
				e.printStackTrace();
				Thread.sleep(300);
			}
		}
	}
}
