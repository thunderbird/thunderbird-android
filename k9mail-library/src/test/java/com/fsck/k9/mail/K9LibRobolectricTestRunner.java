package com.fsck.k9.mail;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

public class K9LibRobolectricTestRunner extends RobolectricTestRunner {

    public K9LibRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Config buildGlobalConfig() {
        return new Config.Builder()
                .setSdk(22)
                .setManifest(Config.NONE)
                .build();
    }
}