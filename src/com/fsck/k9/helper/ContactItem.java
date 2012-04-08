package com.fsck.k9.helper;

import java.io.Serializable;
import java.util.ArrayList;

public class ContactItem implements Serializable {
    private static final long serialVersionUID = 4893328130147843375L;

    private String displayName = null;
    private ArrayList<String> emailAddresses = null;

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public ArrayList<String> getEmailAddresses() {
        return emailAddresses;
    }
    public void setEmailAddresses(ArrayList<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }
}
