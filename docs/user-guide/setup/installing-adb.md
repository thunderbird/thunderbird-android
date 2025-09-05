# Installing ADB (Android Debug Bridge)

[Android Debug Bridge (**adb**)](https://developer.android.com/tools/adb) is a versatile command-line tool that lets you communicate with an Android-powered
device connected to a computer via USB. One of its useful features is the ability to capture a debug log (`logcat`).

See: [Collect and Share Debug Logs → Step 3](../troubleshooting/collecting-debug-logs.md#step-3-collect-the-debug-log).

## Download and Install

You don’t need the full Android SDK to use adb. Instead, download the smaller [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) which include **adb** and other essential utilities.

1. Go to the [Android Platform Tools download page](https://developer.android.com/tools/releases/platform-tools).
2. Download the package for your operating system (Windows, macOS, or Linux).
3. Extract the contents of the downloaded archive to a location on your computer.
4. Run `adb` directly from that folder.

If you plan to use **adb** often, add the `platform-tools` folder to your system **PATH** so you can run adb
from any terminal or command prompt.

## Enable Debugging on Your Device

Before adb can communicate with your Android device, you must enable **USB debugging**:

1. On your device, go to **Settings** → **About phone**.
2. Tap **Build number** seven times to enable **Developer options**. You may need to enter your device PIN or password to confirm.
3. Go back to **Settings** and select **System** (or directly **Developer options** on some devices).
4. Enable **USB debugging**.

For more detailed instructions, refer to the [official guide](https://developer.android.com/studio/debug/dev-options).

## Connect Your Device

1. Connect your Android device to your computer via USB.
2. The first time you connect, unlock your device and accept the **RSA key prompt** to authorize the connection.
3. You may choose to always allow connections from this computer.

**Windows**

You may need to install a **USB driver** for your device if it’s not recognized, see the [OEM USB Drivers](https://developer.android.com/studio/run/oem-usb) page for instructions.

**Linux**

You may need to add a **udev rules** file. See [here](https://developer.android.com/studio/run/device#setting-up) for details.

## Verify Connection

Run the following from inside the `platform-tools` folder (or anywhere, if adb is in your PATH):

```bash
adb devices
```

If connected, you should see your device listed under List of devices attached.
- If the device is listed as **unauthorized**, check your device for the **RSA prompt** and accept it.
- If the device is listed as **offline**:
1. Try killing the adb server with `adb kill-server`
2. Disable and enable USB debugging on the device.
3. Start the adb server again with `adb start-server`d
4. Reconnect the device and check `adb devices` again.

## When Finished

When you’re done using adb, you can simply disconnect your device. If you want to stop the adb server, run:

```bash
adb kill-server
```

