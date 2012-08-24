package com.fsck.k9.helper;

import java.util.Iterator;

public final class StringUtils {

    public static boolean isNullOrEmpty(String string){
        return string == null || string.length() == 0;
    }

    public static String join(Iterable<String> elements, String delimiter) {
    	StringBuffer buffer = new StringBuffer();

    	Iterator<String> iter = elements.iterator();
    	if (iter.hasNext()) {
    		buffer.append(iter.next());
    		while (iter.hasNext()) {
    			buffer.append(delimiter);
    			buffer.append(iter.next());
    		}
    	}
    	return buffer.toString();
    }
}
