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

package org.apache.james.mime4j.stream;

import java.util.BitSet;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.util.ByteSequence;
import org.apache.james.mime4j.util.ContentUtil;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * The basic immutable MIME field.
 */
public class RawField {

    private static final BitSet fieldChars = new BitSet();

    static {
        for (int i = 0x21; i <= 0x39; i++) {
            fieldChars.set(i);
        }
        for (int i = 0x3b; i <= 0x7e; i++) {
            fieldChars.set(i);
        }
    }


    private ByteSequence raw;
    private int colonIdx;
    private int headerNameEndIdx;

    private String name;
    private String body;
	private final boolean obsoleteSyntax;

    public RawField(String name, String body) {
	this.name = name;
	this.body = body;
	this.raw = null;
	this.obsoleteSyntax = false;
    }

    /**
     * @param raw bytes
     * @throws MimeException on malformed data
     */
    public RawField(ByteSequence raw) throws MimeException {
        this.raw = raw;

        colonIdx = -1;
        boolean obsolete = false;
        for (int i = 0; i < raw.length(); i++) {
            if (!fieldChars.get(raw.byteAt(i) & 0xff)) {
		headerNameEndIdx = i;
		for (; i < raw.length(); i++) {
			int j = raw.byteAt(i) & 0xff;
			if (j == ':') {
				colonIdx = i;
				break;
			} else if (j != 0x20 && j != 0x09) {
				throw new MimeException("Invalid header");
			} else {
						obsolete = true;
					}
		}
		break;
            }
        }
        if (colonIdx == -1) throw new MimeException("Invalid header. No colon found.");
        obsoleteSyntax = obsolete;
    }

    public String getName() {
        if (name == null) {
            name = parseName();
        }

        return name;
    }

    public String getBody() {
        if (body == null) {
            body = parseBody();
        }

        return body;
    }

    public ByteSequence getRaw() {
	if (raw == null) {
		raw = ContentUtil.encode(MimeUtil.fold(name+": "+body, 0));
	}
        return raw;
    }

    @Override
    public String toString() {
        return getName() + ": " + getBody();
    }

    private String parseName() {
	// make sure we ignore ending WSP (obsolete rfc822 syntax)
        return ContentUtil.decode(raw, 0, headerNameEndIdx);
    }

    private String parseBody() {
        int offset = colonIdx + 1;
        // if the header body starts with a space we remove it.
        if (raw.length() > offset + 1 && (raw.byteAt(offset) & 0xff) == 0x20) offset++;
        int length = raw.length() - offset;
        return MimeUtil.unfold(ContentUtil.decode(raw, offset, length));
    }

	public boolean isObsoleteSyntax() {
		return obsoleteSyntax;
	}

}
