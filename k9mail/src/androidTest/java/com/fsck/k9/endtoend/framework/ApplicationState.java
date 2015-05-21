package com.fsck.k9.endtoend.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the state of the application from the point of view of end-to-end tests.
 */
public class ApplicationState {

    private static final ApplicationState state = new ApplicationState();

    public final List<AccountForTest> accounts = new ArrayList<AccountForTest>();

    public StubMailServer stubMailServer;

    public static ApplicationState getInstance() {
        return state;
    }


}
