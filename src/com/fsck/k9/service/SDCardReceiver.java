package com.fsck.k9.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SDCardReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO implement this as per http://code.google.com/p/k9mail/issues/detail?id=888

		/*
		 * <intent-filter>
                <action android:name="android.intent.action.MEDIA_MOUNTED"/> restart accounts that use SD-storage
                <action android:name="android.intent.action.MEDIA_EJECT"/> close databases on SD-storage and stop their accounts
                <action android:name="android.intent.action.MEDIA_UNMOUNTED"/> stop accounts that use SD-storage
            </intent-filter>
		 */
	}

}
