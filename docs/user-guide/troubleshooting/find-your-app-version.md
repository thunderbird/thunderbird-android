# Find Your App Version

For [bug reports](https://github.com/k9mail/k-9/issues/new/choose) it's important for the developers to know which
version of Thunderbird you're using. This is especially true if you attach debug logs, since they show exactly which
part of the code was active when the error occurred. Because the apps are updated frequently, version numbers
(and code line references) can change quickly.

Follow the instructions below to find the exact version number you’re running.

## Methods

There are several ways to check the version number: inside the app, through Android’s system settings, or with ADB.

### In the App (About screen)

1. Start **Thunderbird**.
2. Go to the **Settings**.
3. Select **About**.
4. The version string is shown under **Version**.

### From Android: App Info

If the app won’t start, Android usually displays the version number at the bottom of its **App info** screen.

1. Open the system **Settings** app.
2. Navigate to **Apps** (or **Apps & notifications**).
3. Find and select **Thunderbird** from the list of installed apps.
4. Scroll down to the bottom of the screen to find the version number.

Alternatively, you can long-press the Thunderbird icon in your app drawer or home screen, then tap the "App info" (i) icon that appears.

### Using ADB (Advanced)

If you have [ADB](../setup/installing-adb.md) set up, you can retrieve the version number via the command line:

```bash
adb shell dumpsys package net.thunderbird.android | grep versionName
adb shell dumpsys package net.thunderbird.android.beta | grep versionCode
adb shell dumpsys package net.thunderbird.android.daily | grep versionCode

# For K-9 Mail:
adb shell dumpsys package com.fsck.k9 | grep versionName
```

This will return a line like:

```plaintext
versionName=12.1
```

## Special Note for Custom Builds

If you’ve built your own version of the app from the source repository, please include the commit hash in your bug
report along with the version number.
