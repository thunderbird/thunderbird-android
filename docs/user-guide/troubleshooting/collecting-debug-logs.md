# Collecting Debug Logs

Thunderbird for Android can produce debug logs to help diagnose problems and errors. This guide explains how to enable
logging, reproduce the issue, collect logs, and share them with the team.

## Before you start

- If the app is **crashing on startup**, jump directly
  to [Method B: Using a PC with ADB](#method-b-using-a-pc-with-adb).
- Logs may include sensitive information (e.g., email addresses, server hostnames). You should redact **passwords**.
- When possible, share the **complete log** to maximize debugging value.

## Step 1: Enable debug logging

1. Open Thunderbird for Android.
2. Go to: **Settings** → **General settings** → **Debugging**.
3. Check: **Enable debug logging**.

If Thunderbird crashes before you can reach Settings, jump directly
to [Method B: Using a PC with ADB](#method-b-using-a-pc-with-adb).

## Step 2: Reproduce the problem

Perform the actions that lead to the error or crash. This ensures the relevant events are captured in the log.

Take note of:

- The **steps you performed** (e.g. “Opened folder → tapped compose → app crashed”).
- The **exact time** the issue occurred (to help locate it in the log).

This information makes it easier to match the log entries to the problem when reviewing your report.

## Step 3: Collect the debug log

Choose one of the following methods:

### Method A: From within the app

Use this if the app is **not** crashing during startup.

- Go to: **Settings** → **General settings** → **Debugging**.
- Tap the menu and select: **Export logs**.
- Choose a location to save the log file.

### Method B: Using a PC with ADB

Use this if the app **crashes on startup** or you prefer collecting logs via a computer.

**Requirements:**

- A computer with the [Android Platform Tools (ADB)](https://developer.android.com/tools/adb) installed. See
  also [Installing ADB](../setup/installing-adb.md).
- USB debugging enabled on your Android device.

**Steps:**

1. Connect your device to the computer and verify ADB sees it:

   ```bash
   adb devices
   ```

   You should see your device listed. If not, ensure drivers are installed (Windows) and USB debugging is enabled.

2. Find Thunderbird’s **process ID (PID)**:

   - On Linux / macOS:

     ```bash
     adb shell ps | grep net.thunderbird.android
     # For K-9 Mail:
     adb shell ps | grep com.fsck.k9
     ```
   - On Windows (Command Prompt):

     ```bash
     adb shell ps -A | findstr net.thunderbird.android
     # For K-9 Mail:
     adb shell ps -A | findstr com.fsck.k9
     ```

   Example output:

   ```plaintext
   u0_a153       5191   587 4468612 112380 SyS_epoll_wait      0 S net.thunderbird.android
   ```

   In this example, the PID is **5191**.

3. Capture the debug log to a file:

   ```bash
   adb logcat -d --pid=<PID> > thunderbird-log.txt
   ```

   Replace `<PID>` (including brackets) with the actual number.
   - If you see an ADB error like `> was unexpected at this time.`, it usually means you forgot to replace `<PID>` with
   the actual number.
   To capture **ongoing logs** while reproducing the issue:

   ```bash
   adb logcat --pid=<PID> > thunderbird-log.txt
   ```

   Stop the command with **Ctrl+C** (Windows/Linux) or **Command+C** (macOS).

**Tips:**

- If the app restarts and gets a new PID, repeat step 2 to obtain the current PID and run the capture command again.
- If `pgrep` is unavailable, see [Troubleshooting ADB](#troubleshooting-adb)

## Step 4: Check Logs for Sensitive Information

Debug logs may include details about your account or device. While most of this is safe to share, here are some things
you may want to remove before attaching logs:

- **Passwords**: Look for lines with `AUTH`, `LOGIN`, or `PASSWORD`. Replace with password with `redacted-password`.
- **Personal Identifiable Information (PII)**: Email addresses, phone numbers, or real names. Consider replacing with
  placeholders like `redacted-pii`.
- **Server Hostnames/IPs**: If you’re concerned about privacy, replace with `redacted-mail-server`.
- **OAuth Tokens**: Look for lines containing `oauth=` or `token=`. Replace with `redacted-oauth-token`.

### How to Quickly Search Logs

- On Windows: Open the file in Notepad or another text editor and use **Ctrl+F** to search.
- On macOS/Linux: Use `grep` in the terminal. For example, to find passwords:

  ```bash
  grep -iE 'auth|login|password' thunderbird-log.txt
  ```

## Step 5: Report the Issue and Attach Logs

1. Create a new issue in our [bug tracker](https://github.com/thunderbird/thunderbird-android/issues/new/choose)
2. Include the following:
   - Thunderbird for Android **version number** (see [Find out version number](find-your-app-version.md)).
   - A clear description of the problem and ideally steps to reproduce it.
   - The collected log file as an **attachment**.
   - Any relevant **screenshots** or **screen recordings**.

## Troubleshooting ADB

If `adb devices` shows **unauthorized**, accept the RSA key prompt on your device. If it doesn’t appear:

```bash
adb kill-server
adb start-server
```

On _Windows_, install OEM USB drivers if your device isn’t detected.

