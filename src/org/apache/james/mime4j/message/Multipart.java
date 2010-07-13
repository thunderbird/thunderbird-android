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

package org.apache.james.mime4j.message;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.james.mime4j.field.ContentTypeField;
import org.apache.james.mime4j.field.Field;
import org.apache.james.mime4j.util.CharsetUtil;

/**
 * Represents a MIME multipart body (see RFC 2045).A multipart body has a 
 * ordered list of body parts. The multipart body also has a preamble and
 * epilogue. The preamble consists of whatever characters appear before the 
 * first body part while the epilogue consists of whatever characters come
 * after the last body part.
 *
 * 
 * @version $Id: Multipart.java,v 1.3 2004/10/02 12:41:11 ntherning Exp $
 */
public class Multipart implements Body {
    private String preamble = "";
    private String epilogue = "";
    private List bodyParts = new LinkedList();
    private Entity parent = null;
    private String subType = "alternative";

    /**
     * Creates a new empty <code>Multipart</code> instance.
     */
    public Multipart() {
    }

    /**
     * Gets the multipart sub-type. E.g. <code>alternative</code> (the default)
     * or <code>parallel</code>. See RFC 2045 for common sub-types and their
     * meaning.
     * 
     * @return the multipart sub-type.
     */
    public String getSubType() {
        return subType;
    }
    
    /**
     * Sets the multipart sub-type. E.g. <code>alternative</code>
     * or <code>parallel</code>. See RFC 2045 for common sub-types and their
     * meaning.
     * 
     * @param subType the sub-type.
     */
    public void setSubType(String subType) {
        this.subType = subType;
    }
    
    /**
     * @see org.apache.james.mime4j.message.Body#getParent()
     */
    public Entity getParent() {
        return parent;
    }
    
    /**
     * @see org.apache.james.mime4j.message.Body#setParent(org.apache.james.mime4j.message.Entity)
     */
    public void setParent(Entity parent) {
        this.parent = parent;
        for (Iterator it = bodyParts.iterator(); it.hasNext();) {
            ((BodyPart) it.next()).setParent(parent);
        }
    }

    /**
     * Gets the epilogue.
     * 
     * @return the epilogue.
     */
    public String getEpilogue() {
        return epilogue;
    }
    
    /**
     * Sets the epilogue.
     * 
     * @param epilogue the epilogue.
     */
    public void setEpilogue(String epilogue) {
        this.epilogue = epilogue;
    }
    
    /**
     * Gets the list of body parts. The list is immutable.
     * 
     * @return the list of <code>BodyPart</code> objects.
     */
    public List getBodyParts() {
        return Collections.unmodifiableList(bodyParts);
    }
    
    /**
     * Sets the list of body parts.
     * 
     * @param bodyParts the new list of <code>BodyPart</code> objects.
     */
    public void setBodyParts(List bodyParts) {
        this.bodyParts = bodyParts;
        for (Iterator it = bodyParts.iterator(); it.hasNext();) {
            ((BodyPart) it.next()).setParent(parent);
        }
    }
    
    /**
     * Adds a body part to the end of the list of body parts.
     * 
     * @param bodyPart the body part.
     */
    public void addBodyPart(BodyPart bodyPart) {
        bodyParts.add(bodyPart);
        bodyPart.setParent(parent);
    }
    
    /**
     * Gets the preamble.
     * 
     * @return the preamble.
     */
    public String getPreamble() {
        return preamble;
    }
    
    /**
     * Sets the preamble.
     * 
     * @param preamble the preamble.
     */
    public void setPreamble(String preamble) {
        this.preamble = preamble;
    }

    /**
     * 
     * @see org.apache.james.mime4j.message.Body#writeTo(java.io.OutputStream)
     */
    public void writeTo(OutputStream out) throws IOException {
        String boundary = getBoundary();
        List bodyParts = getBodyParts();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, CharsetUtil.getCharset(getCharset())),8192);
        
        writer.write(getPreamble() + "\r\n");

        for (int i = 0; i < bodyParts.size(); i++) {
            writer.write(boundary + "\r\n");
            ((BodyPart) bodyParts.get(i)).writeTo(out);
        }

        writer.write(getEpilogue() + "\r\n");
        writer.write(boundary + "--" + "\r\n");

    }

    /**
     * Return the boundory of the parent Entity
     * 
     * @return boundery
     */
    private String getBoundary() {
        Entity e = getParent();
        ContentTypeField cField = (ContentTypeField) e.getHeader().getField(
                Field.CONTENT_TYPE);
        return cField.getBoundary();
    }
    
    private String getCharset() {
        Entity e = getParent();
        String charString = ((ContentTypeField) e.getHeader().getField(Field.CONTENT_TYPE)).getCharset();
        return charString;
    }
}
