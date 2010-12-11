
package com.fsck.k9;

import com.fsck.k9.mail.Flag;

public interface SearchSpecification
{

    public Flag[] getRequiredFlags();

    public Flag[] getForbiddenFlags();

    public boolean isIntegrate();

    public String getQuery();

    public String[] getAccountUuids();

    public String[] getFolderNames();
}