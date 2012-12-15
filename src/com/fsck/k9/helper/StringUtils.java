package com.fsck.k9.helper;

import java.util.List;

public final class StringUtils {

    public static boolean isNullOrEmpty(String string){
        return string == null || string.length() == 0;
    }

    public static boolean containsAny(String haystack, String[] needles) {
        if (haystack == null) {
            return false;
        }

        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }

        return false;
    }
    
    static public String join(List<String> list, String conjunction) { 
    	StringBuilder sb = new StringBuilder(); 
    	final int size = list.size(); 
    	if (size > 0) sb.append(list.get(0)); 
    	for (int i = 1; i < size; i++) { 
    		sb.append(conjunction); 
    		sb.append(list.get(i)); 
		} 
    	return sb.toString(); 
	}    
}
