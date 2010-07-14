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

package org.apache.james.mime4j.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @version $Id: TempStorage.java,v 1.2 2004/10/02 12:41:11 ntherning Exp $
 */
public abstract class TempStorage {
    private static Log log = LogFactory.getLog(TempStorage.class);
    private static TempStorage inst = null;
    
    static {
        
        String clazz = System.getProperty("org.apache.james.mime4j.tempStorage");
        try {
            
            if (inst != null) {
                inst = (TempStorage) Class.forName(clazz).newInstance();
            }
            
        } catch (Throwable t) {
            log.warn("Unable to create or instantiate TempStorage class '" 
                      + clazz + "' using SimpleTempStorage instead", t);
        }

        if (inst == null) {
            inst = new SimpleTempStorage();            
        }
    }
    
    /**
     * Gets the root temporary path which should be used to 
     * create new temporary paths or files.
     * 
     * @return the root temporary path.
     */
    public abstract TempPath getRootTempPath();
    
    public static TempStorage getInstance() {
        return inst;
    }
    
    public static void setInstance(TempStorage inst) {
        if (inst == null) {
            throw new NullPointerException("inst");
        }
        TempStorage.inst = inst;
    }
}
