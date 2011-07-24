package com.fsck.k9.activity.setup;

/*
    After gathering the necessary information one way or another this activity displays it to the user, eventually warns
    him it could be false ( no https ), asks for confirmation, allows selection of protocol,... and then goes on to test
    the final settings.
 */

import com.fsck.k9.helper.configxmlparser.AutoconfigInfo;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.ServerType;
import java.util.List;

public class AccountSetupConfirmIncoming extends AbstractSetupConfirmActivity{
    @Override
    protected List<? extends AutoconfigInfo.Server> getServers() {
        return mConfigInfo.incomingServer;
    }

    @Override
    protected List<ServerType> getAvailableServerTypes() {
        return mConfigInfo.getAvailableIncomingServerTypes();
    }

    @Override
    protected void finishAction() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
