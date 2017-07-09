package com.fsck.k9.helper;


public final class EmailHelper {
    public static String getDomainFromEmailAddress(String email) {
        int separatorIndex = email.lastIndexOf('@');
        if (separatorIndex == -1 || separatorIndex + 1 == email.length()) {
            return null;
        }

        return email.substring(separatorIndex + 1);
    }

    public String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }
}
