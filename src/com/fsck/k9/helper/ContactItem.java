package com.fsck.k9.helper;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ContactItem implements Serializable {
    private static final long serialVersionUID = 4893328130147843375L;

    public final String displayName;
    public final List<String> emailAddresses;

    public ContactItem(String displayName, List<String> emailAddresses) {
        this.displayName = displayName;
        this.emailAddresses = Collections.unmodifiableList(emailAddresses);
    }
}
