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

package org.apache.commons.codec;

/**
 * Defines common decoding methods for byte array decoders.
 *
 * @author Apache Software Foundation
 * @version $Id: BinaryDecoder.java,v 1.10 2004/06/15 18:14:15 ggregory Exp $
 */
public interface BinaryDecoder extends Decoder {

    /**
     * Decodes a byte array and returns the results as a byte array. 
     *
     * @param pArray A byte array which has been encoded with the
     *      appropriate encoder
     * 
     * @return a byte array that contains decoded content
     * 
     * @throws DecoderException A decoder exception is thrown
     *          if a Decoder encounters a failure condition during
     *          the decode process.
     */
    byte[] decode(byte[] pArray) throws DecoderException;
}  

