/* Copyright (C) 2010 The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.mail.store.exchange.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.admin.DevicePolicyManager;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.store.EasStore;

/**
 * Parse the result of the Provision command
 *
 * Assuming a successful parse, we store the PolicySet and the policy key
 */
public class ProvisionParser extends Parser {
    PolicySet mPolicySet = null;
    String mPolicyKey = null;
    boolean mRemoteWipe = false;
    boolean mIsSupportable = true;

    public ProvisionParser(InputStream in) throws IOException {
        super(in);
    }

    public PolicySet getPolicySet() {
        return mPolicySet;
    }

    public String getPolicyKey() {
        return mPolicyKey;
    }

    public boolean getRemoteWipe() {
        return mRemoteWipe;
    }

    public boolean hasSupportablePolicySet() {
        return (mPolicySet != null) && mIsSupportable;
    }

    private void parseProvisionDocWbxml() throws IOException {
        int minPasswordLength = 0;
        int passwordMode = PolicySet.PASSWORD_MODE_NONE;
        int maxPasswordFails = 0;
        int maxScreenLockTime = 0;
        boolean supported = true;

        while (nextTag(Tags.PROVISION_EAS_PROVISION_DOC) != END) {
            switch (tag) {
                case Tags.PROVISION_DEVICE_PASSWORD_ENABLED:
                    if (getValueInt() == 1) {
                        if (passwordMode == PolicySet.PASSWORD_MODE_NONE) {
                            passwordMode = PolicySet.PASSWORD_MODE_SIMPLE;
                        }
                    }
                    break;
                case Tags.PROVISION_MIN_DEVICE_PASSWORD_LENGTH:
                    minPasswordLength = getValueInt();
                    break;
                case Tags.PROVISION_ALPHA_DEVICE_PASSWORD_ENABLED:
                    if (getValueInt() == 1) {
                        passwordMode = PolicySet.PASSWORD_MODE_STRONG;
                    }
                    break;
                case Tags.PROVISION_MAX_INACTIVITY_TIME_DEVICE_LOCK:
                    // EAS gives us seconds, which is, happily, what the PolicySet requires
                    maxScreenLockTime = getValueInt();
                    break;
                case Tags.PROVISION_MAX_DEVICE_PASSWORD_FAILED_ATTEMPTS:
                    maxPasswordFails = getValueInt();
                    break;
                case Tags.PROVISION_ALLOW_SIMPLE_DEVICE_PASSWORD:
                    // Ignore this unless there's any MSFT documentation for what this means
                    // Hint: I haven't seen any that's more specific than "simple"
                    getValue();
                    break;
                // The following policy, if false, can't be supported at the moment
                case Tags.PROVISION_ATTACHMENTS_ENABLED:
                    if (getValueInt() == 0) {
                       supported = false;
                    }
                    break;
                // The following policies, if true, can't be supported at the moment
                case Tags.PROVISION_DEVICE_ENCRYPTION_ENABLED:
                case Tags.PROVISION_PASSWORD_RECOVERY_ENABLED:
                case Tags.PROVISION_DEVICE_PASSWORD_EXPIRATION:
                case Tags.PROVISION_DEVICE_PASSWORD_HISTORY:
                case Tags.PROVISION_MAX_ATTACHMENT_SIZE:
                    if (getValueInt() == 1) {
                        supported = false;
                    }
                    break;
                default:
                    skipTag();
            }

            if (!supported) {
                log("Policy not supported: " + tag);
                mIsSupportable = false;
            }
        }

        mPolicySet = new PolicySet(minPasswordLength, passwordMode,
                    maxPasswordFails, maxScreenLockTime, true);
    }

    class ShadowPolicySet {
        int mMinPasswordLength = 0;
        int mPasswordMode = PolicySet.PASSWORD_MODE_NONE;
        int mMaxPasswordFails = 0;
        int mMaxScreenLockTime = 0;
    }

    public void parseProvisionDocXml(String doc) throws IOException {
        ShadowPolicySet sps = new ShadowPolicySet();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new ByteArrayInputStream(doc.getBytes()), "UTF-8");
            int type = parser.getEventType();
            if (type == XmlPullParser.START_DOCUMENT) {
                type = parser.next();
                if (type == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("wap-provisioningdoc")) {
                        parseWapProvisioningDoc(parser, sps);
                    }
                }
            }
        } catch (XmlPullParserException e) {
           throw new IOException();
        }

        mPolicySet = new PolicySet(sps.mMinPasswordLength, sps.mPasswordMode, sps.mMaxPasswordFails,
                sps.mMaxScreenLockTime, true);
    }

    /**
     * Return true if password is required; otherwise false.
     */
    boolean parseSecurityPolicy(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
        boolean passwordRequired = true;
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("parm")) {
                    String name = parser.getAttributeValue(null, "name");
                    if (name.equals("4131")) {
                        String value = parser.getAttributeValue(null, "value");
                        if (value.equals("1")) {
                            passwordRequired = false;
                        }
                    }
                }
            }
        }
        return passwordRequired;
    }

    void parseCharacteristic(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
        boolean enforceInactivityTimer = true;
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                if (parser.getName().equals("parm")) {
                    String name = parser.getAttributeValue(null, "name");
                    String value = parser.getAttributeValue(null, "value");
                    if (name.equals("AEFrequencyValue")) {
                        if (enforceInactivityTimer) {
                            if (value.equals("0")) {
                                sps.mMaxScreenLockTime = 1;
                            } else {
                                sps.mMaxScreenLockTime = 60*Integer.parseInt(value);
                            }
                        }
                    } else if (name.equals("AEFrequencyType")) {
                        // "0" here means we don't enforce an inactivity timeout
                        if (value.equals("0")) {
                            enforceInactivityTimer = false;
                        }
                    } else if (name.equals("DeviceWipeThreshold")) {
                        sps.mMaxPasswordFails = Integer.parseInt(value);
                    } else if (name.equals("CodewordFrequency")) {
                        // Ignore; has no meaning for us
                    } else if (name.equals("MinimumPasswordLength")) {
                        sps.mMinPasswordLength = Integer.parseInt(value);
                    } else if (name.equals("PasswordComplexity")) {
                        if (value.equals("0")) {
                            sps.mPasswordMode = PolicySet.PASSWORD_MODE_STRONG;
                        } else {
                            sps.mPasswordMode = PolicySet.PASSWORD_MODE_SIMPLE;
                        }
                    }
                }
            }
        }
    }

    void parseRegistry(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
      while (true) {
          int type = parser.nextTag();
          if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
              break;
          } else if (type == XmlPullParser.START_TAG) {
              String name = parser.getName();
              if (name.equals("characteristic")) {
                  parseCharacteristic(parser, sps);
              }
          }
      }
    }

    void parseWapProvisioningDoc(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("wap-provisioningdoc")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.equals("characteristic")) {
                    String atype = parser.getAttributeValue(null, "type");
                    if (atype.equals("SecurityPolicy")) {
                        // If a password isn't required, stop here
                        if (!parseSecurityPolicy(parser, sps)) {
                            return;
                        }
                    } else if (atype.equals("Registry")) {
                        parseRegistry(parser, sps);
                        return;
                    }
                }
            }
        }
    }

    public void parseProvisionData() throws IOException {
        while (nextTag(Tags.PROVISION_DATA) != END) {
            if (tag == Tags.PROVISION_EAS_PROVISION_DOC) {
                parseProvisionDocWbxml();
            } else {
                skipTag();
            }
        }
    }

    public void parsePolicy() throws IOException {
        String policyType = null;
        while (nextTag(Tags.PROVISION_POLICY) != END) {
            switch (tag) {
                case Tags.PROVISION_POLICY_TYPE:
                    policyType = getValue();
                    Log.i(K9.LOG_TAG, "Policy type: " + policyType);
                    break;
                case Tags.PROVISION_POLICY_KEY:
                    mPolicyKey = getValue();
                    break;
                case Tags.PROVISION_STATUS:
                	Log.i(K9.LOG_TAG, "Policy status: " + getValue());
                    break;
                case Tags.PROVISION_DATA:
                    if (policyType.equalsIgnoreCase(EasStore.EAS_2_POLICY_TYPE)) {
                        // Parse the old style XML document
                        parseProvisionDocXml(getValue());
                    } else {
                        // Parse the newer WBXML data
                        parseProvisionData();
                    }
                    break;
                default:
                    skipTag();
            }
        }
    }

    public void parsePolicies() throws IOException {
        while (nextTag(Tags.PROVISION_POLICIES) != END) {
            if (tag == Tags.PROVISION_POLICY) {
                parsePolicy();
            } else {
                skipTag();
            }
        }
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.PROVISION_PROVISION) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            switch (tag) {
                case Tags.PROVISION_STATUS:
                    int status = getValueInt();
                    Log.i(K9.LOG_TAG, "Provision status: " + status);
                    res = (status == 1);
                    break;
                case Tags.PROVISION_POLICIES:
                    parsePolicies();
                    break;
                case Tags.PROVISION_REMOTE_WIPE:
                    // Indicate remote wipe command received
                    mRemoteWipe = true;
                    break;
                default:
                    skipTag();
            }
        }
        return res;
    }
    
    /**
     * Class for tracking policies and reading/writing into accounts
     */
    public static class PolicySet {

        // Security (provisioning) flags
            // bits 0..4: password length (0=no password required)
        private static final int PASSWORD_LENGTH_MASK = 31;
        private static final int PASSWORD_LENGTH_SHIFT = 0;
        public static final int PASSWORD_LENGTH_MAX = 30;
            // bits 5..8: password mode
        private static final int PASSWORD_MODE_SHIFT = 5;
        private static final int PASSWORD_MODE_MASK = 15 << PASSWORD_MODE_SHIFT;
        public static final int PASSWORD_MODE_NONE = 0 << PASSWORD_MODE_SHIFT;
        public static final int PASSWORD_MODE_SIMPLE = 1 << PASSWORD_MODE_SHIFT;
        public static final int PASSWORD_MODE_STRONG = 2 << PASSWORD_MODE_SHIFT;
            // bits 9..13: password failures -> wipe device (0=disabled)
        private static final int PASSWORD_MAX_FAILS_SHIFT = 9;
        private static final int PASSWORD_MAX_FAILS_MASK = 31 << PASSWORD_MAX_FAILS_SHIFT;
        public static final int PASSWORD_MAX_FAILS_MAX = 31;
            // bits 14..24: seconds to screen lock (0=not required)
        private static final int SCREEN_LOCK_TIME_SHIFT = 14;
        private static final int SCREEN_LOCK_TIME_MASK = 2047 << SCREEN_LOCK_TIME_SHIFT;
        public static final int SCREEN_LOCK_TIME_MAX = 2047;
            // bit 25: remote wipe capability required
        private static final int REQUIRE_REMOTE_WIPE = 1 << 25;

        /*package*/ final int mMinPasswordLength;
        /*package*/ final int mPasswordMode;
        /*package*/ final int mMaxPasswordFails;
        /*package*/ final int mMaxScreenLockTime;
        /*package*/ final boolean mRequireRemoteWipe;

        public int getMinPasswordLengthForTest() {
            return mMinPasswordLength;
        }

        public int getPasswordModeForTest() {
            return mPasswordMode;
        }

        public int getMaxPasswordFailsForTest() {
            return mMaxPasswordFails;
        }

        public int getMaxScreenLockTimeForTest() {
            return mMaxScreenLockTime;
        }

        public boolean isRequireRemoteWipeForTest() {
            return mRequireRemoteWipe;
        }

        /**
         * Create from raw values.
         * @param minPasswordLength (0=not enforced)
         * @param passwordMode
         * @param maxPasswordFails (0=not enforced)
         * @param maxScreenLockTime in seconds (0=not enforced)
         * @param requireRemoteWipe
         * @throws IllegalArgumentException for illegal arguments.
         */
        public PolicySet(int minPasswordLength, int passwordMode, int maxPasswordFails,
                int maxScreenLockTime, boolean requireRemoteWipe) throws IllegalArgumentException {
            // If we're not enforcing passwords, make sure we clean up related values, since EAS
            // can send non-zero values for any or all of these
            if (passwordMode == PASSWORD_MODE_NONE) {
                maxPasswordFails = 0;
                maxScreenLockTime = 0;
                minPasswordLength = 0;
            } else {
                if ((passwordMode != PASSWORD_MODE_SIMPLE) &&
                        (passwordMode != PASSWORD_MODE_STRONG)) {
                    throw new IllegalArgumentException("password mode");
                }
                // The next value has a hard limit which cannot be supported if exceeded.
                if (minPasswordLength > PASSWORD_LENGTH_MAX) {
                    throw new IllegalArgumentException("password length");
                }
                // This value can be reduced (which actually increases security) if necessary
                if (maxPasswordFails > PASSWORD_MAX_FAILS_MAX) {
                    maxPasswordFails = PASSWORD_MAX_FAILS_MAX;
                }
                // This value can be reduced (which actually increases security) if necessary
                if (maxScreenLockTime > SCREEN_LOCK_TIME_MAX) {
                    maxScreenLockTime = SCREEN_LOCK_TIME_MAX;
                }
            }
            mMinPasswordLength = minPasswordLength;
            mPasswordMode = passwordMode;
            mMaxPasswordFails = maxPasswordFails;
            mMaxScreenLockTime = maxScreenLockTime;
            mRequireRemoteWipe = requireRemoteWipe;
        }

//        /**
//         * Create from values encoded in an account
//         * @param account
//         */
//        public PolicySet(Account account) {
//            this(account.mSecurityFlags);
//        }

        /**
         * Create from values encoded in an account flags int
         */
        public PolicySet(int flags) {
            mMinPasswordLength =
                (flags & PASSWORD_LENGTH_MASK) >> PASSWORD_LENGTH_SHIFT;
            mPasswordMode =
                (flags & PASSWORD_MODE_MASK);
            mMaxPasswordFails =
                (flags & PASSWORD_MAX_FAILS_MASK) >> PASSWORD_MAX_FAILS_SHIFT;
            mMaxScreenLockTime =
                (flags & SCREEN_LOCK_TIME_MASK) >> SCREEN_LOCK_TIME_SHIFT;
            mRequireRemoteWipe = 0 != (flags & REQUIRE_REMOTE_WIPE);
        }

        /**
         * Helper to map our internal encoding to DevicePolicyManager password modes.
         */
        public int getDPManagerPasswordQuality() {
            switch (mPasswordMode) {
                case PASSWORD_MODE_SIMPLE:
                    return DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
                case PASSWORD_MODE_STRONG:
                    return DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
                default:
                    return DevicePolicyManager .PASSWORD_QUALITY_UNSPECIFIED;
            }
        }

//        /**
//         * Record flags (and a sync key for the flags) into an Account
//         * Note: the hash code is defined as the encoding used in Account
//         *
//         * @param account to write the values mSecurityFlags and mSecuritySyncKey
//         * @param syncKey the value to write into the account's mSecuritySyncKey
//         * @param update if true, also writes the account back to the provider (updating only
//         *  the fields changed by this API)
//         * @param context a context for writing to the provider
//         * @return true if the actual policies changed, false if no change (note, sync key
//         *  does not affect this)
//         */
//        public boolean writeAccount(Account account, String syncKey, boolean update,
//                Context context) {
//            int newFlags = hashCode();
//            boolean dirty = (newFlags != account.mSecurityFlags);
//            account.mSecurityFlags = newFlags;
//            account.mSecuritySyncKey = syncKey;
//            if (update) {
//                if (account.isSaved()) {
//                    ContentValues cv = new ContentValues();
//                    cv.put(AccountColumns.SECURITY_FLAGS, account.mSecurityFlags);
//                    cv.put(AccountColumns.SECURITY_SYNC_KEY, account.mSecuritySyncKey);
//                    account.update(context, cv);
//                } else {
//                    account.save(context);
//                }
//            }
//            return dirty;
//        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PolicySet) {
                PolicySet other = (PolicySet)o;
                return (this.mMinPasswordLength == other.mMinPasswordLength)
                        && (this.mPasswordMode == other.mPasswordMode)
                        && (this.mMaxPasswordFails == other.mMaxPasswordFails)
                        && (this.mMaxScreenLockTime == other.mMaxScreenLockTime)
                        && (this.mRequireRemoteWipe == other.mRequireRemoteWipe);
            }
            return false;
        }

        /**
         * Note: the hash code is defined as the encoding used in Account
         */
        @Override
        public int hashCode() {
            int flags = 0;
            flags = mMinPasswordLength << PASSWORD_LENGTH_SHIFT;
            flags |= mPasswordMode;
            flags |= mMaxPasswordFails << PASSWORD_MAX_FAILS_SHIFT;
            flags |= mMaxScreenLockTime << SCREEN_LOCK_TIME_SHIFT;
            if (mRequireRemoteWipe) {
                flags |= REQUIRE_REMOTE_WIPE;
            }
            return flags;
        }

        @Override
        public String toString() {
            return "{ " + "pw-len-min=" + mMinPasswordLength + " pw-mode=" + mPasswordMode
                    + " pw-fails-max=" + mMaxPasswordFails + " screenlock-max="
                    + mMaxScreenLockTime + " remote-wipe-req=" + mRequireRemoteWipe + "}";
        }
    }

}

