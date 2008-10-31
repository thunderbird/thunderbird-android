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
 * A named group of zero or more mailboxes.  
 *
 * 
 */
public class Group extends Address {
	private String name;
	private MailboxList mailboxList;
	
	/**
	 * @param name The group name.
	 * @param mailboxes The mailboxes in this group.
	 */
	public Group(String name, MailboxList mailboxes) {
		this.name = name;
		this.mailboxList = mailboxes;
	}

	/**
	 * Returns the group name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the mailboxes in this group.
	 */
	public MailboxList getMailboxes() {
		return mailboxList;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(name);
		buf.append(":");
		for (int i = 0; i < mailboxList.size(); i++) {
			buf.append(mailboxList.get(i).toString());
			if (i + 1 < mailboxList.size())
				buf.append(",");
		}
		buf.append(";");
		return buf.toString();
	}

	protected void doAddMailboxesTo(ArrayList results) {
		for (int i = 0; i < mailboxList.size(); i++)
			results.add(mailboxList.get(i));
	}
}
