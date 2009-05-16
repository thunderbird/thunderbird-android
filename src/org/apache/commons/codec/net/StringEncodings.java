/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.apache.commons.codec.net;

/**
 * String encodings used in this package.
 * 
 * @author Apache Software Foundation
 * @since 1.3
 * @version $Id: StringEncodings.java,v 1.2 2004/04/09 22:21:07 ggregory Exp $
 */
interface StringEncodings {
    /**
     * <p>
     * Seven-bit ASCII, also known as ISO646-US, also known as the Basic Latin block of the Unicode character set.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     * 
     * @see <a href="http://java.sun.com/j2se/1.3/docs/api/java/lang/package-summary.html#charenc">JRE character
     *          encoding names</a>
     */
    String US_ASCII = "US-ASCII";

    /**
     * <p>
     * Eight-bit Unicode Transformation Format.
     * </p>
     * <p>
     * Every implementation of the Java platform is required to support this character encoding.
     * </p>
     * 
     * @see <a href="http://java.sun.com/j2se/1.3/docs/api/java/lang/package-summary.html#charenc">JRE character
     *          encoding names</a>
     */
    String UTF8 = "UTF-8";
}