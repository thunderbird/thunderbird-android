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

import java.util.ArrayList;

/**
 * An immutable, random-access list of Mailbox objects.
 *
 * 
 */
public class MailboxList {

	private ArrayList mailboxes;
	
	/**
	 * @param mailboxes An ArrayList that contains only Mailbox objects. 
	 * @param dontCopy true iff it is not possible for the mailboxes ArrayList to be modified by someone else.
	 */
	public MailboxList(ArrayList mailboxes, boolean dontCopy) {
		if (mailboxes != null)
			this.mailboxes = (dontCopy ? mailboxes : (ArrayList) mailboxes.clone());
		else
			this.mailboxes = new ArrayList(0);
	}
	
	/**
	 * The number of elements in this list.
	 */
	public int size() {
		return mailboxes.size();
	}
	
	/**
	 * Gets an address. 
	 */
	public Mailbox get(int index) {
		if (0 > index || size() <= index)
			throw new IndexOutOfBoundsException();
		return (Mailbox) mailboxes.get(index);
	}
	
	/**
	 * Dumps a representation of this mailbox list to
	 * stdout, for debugging purposes.
	 */
	public void print() {
		for (int i = 0; i < size(); i++) {
			Mailbox mailbox = get(i);
			System.out.println(mailbox.toString());
		}
	}

}
