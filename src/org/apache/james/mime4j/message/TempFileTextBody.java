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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.util.CharsetUtil;
import org.apache.james.mime4j.util.TempFile;
import org.apache.james.mime4j.util.TempPath;
import org.apache.james.mime4j.util.TempStorage;


/**
 * Text body backed by a {@link org.apache.james.mime4j.util.TempFile}.
 *
 * 
 * @version $Id: TempFileTextBody.java,v 1.3 2004/10/25 07:26:46 ntherning Exp $
 */
class TempFileTextBody extends AbstractBody implements TextBody {
    private static Log log = LogFactory.getLog(TempFileTextBody.class);
    
    private String mimeCharset = null;
    private TempFile tempFile = null;

    public TempFileTextBody(InputStream is) throws IOException {
        this(is, null);
    }
    
    public TempFileTextBody(InputStream is, String mimeCharset) 
            throws IOException {
        
        this.mimeCharset = mimeCharset;
        
        TempPath tempPath = TempStorage.getInstance().getRootTempPath();
        tempFile = tempPath.createTempFile("attachment", ".txt");
        
        OutputStream out = tempFile.getOutputStream();
        IOUtils.copy(is, out);
        out.close();
    }
    
    /**
     * @see org.apache.james.mime4j.message.TextBody#getReader()
     */
    public Reader getReader() throws UnsupportedEncodingException, IOException {
        String javaCharset = null;
        if (mimeCharset != null) {
            javaCharset = CharsetUtil.toJavaCharset(mimeCharset);
        }
        
        if (javaCharset == null) {
            javaCharset = "ISO-8859-1";
            
            if (log.isWarnEnabled()) {
                if (mimeCharset == null) {
                    log.warn("No MIME charset specified. Using " + javaCharset
                            + " instead.");
                } else {
                    log.warn("MIME charset '" + mimeCharset + "' has no "
                            + "corresponding Java charset. Using " + javaCharset
                            + " instead.");
                }
            }
        }
        /*
            if (log.isWarnEnabled()) {
                if (mimeCharset == null) {
                    log.warn("No MIME charset specified. Using the "
                           + "platform's default charset.");
                } else {
                    log.warn("MIME charset '" + mimeCharset + "' has no "
                            + "corresponding Java charset. Using the "
                            + "platform's default charset.");
                }
            }
            
            return new InputStreamReader(tempFile.getInputStream());
        }*/
        
        return new InputStreamReader(tempFile.getInputStream(), javaCharset);
    }
    
    
    /**
     * @see org.apache.james.mime4j.message.Body#writeTo(java.io.OutputStream)
     */
    public void writeTo(OutputStream out) throws IOException {
	IOUtils.copy(tempFile.getInputStream(), out);	
    }
}
