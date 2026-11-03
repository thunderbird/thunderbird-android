# Hardware keypad navigation

Hardware keypad navigation is an optional mode for Android phones with a directional pad and physical keys.
It is disabled by default. Enable it under **Settings → General settings → Interaction → Hardware keypad navigation**.
On the initial setup screen, press **Left soft key / F1**, select **Enable hardware keypad navigation**, and
press **Center / Enter**. Opening this menu alone does not enable the mode.

![Keypad navigation and independently configured phone-key actions](assets/hardware-keypad-settings.png)

The keypad changes use Android APIs available on Android 8.0 (API 26) and newer. They do not require root
or accessibility-service permissions. Device firmware must deliver the keys to the app.

## Navigation and menus

| Key | Action when the mode is enabled |
| --- | --- |
| Directional pad | Move focus using the screen's normal Android navigation |
| Center / Enter | Activate the focused control, or use the text editor's normal Enter behavior |
| Left soft key / F1 | Open the current screen's actions as a keypad menu |
| Right soft key / F2 | Hide the on-screen keyboard first when it is visible; otherwise go back |
| `*` outside a text editor | Move to the previous control (Shift+Tab) |
| `#` outside a text editor | Move to the next control (Tab) |

In a text editor, digits, `*`, and `#` remain available to the input method. The numeric shortcuts below apply
outside text editors, with no search field, folder drawer, or dialog covering the content. Modified keys such
as Ctrl+F1 retain their normal behavior. Backspace does not invoke the delete-message shortcut in this mode.

Use the directional pad and Center / Enter to choose a menu action. F2 returns from a submenu or closes the
menu. Screens without an actions menu offer **Next control**, **Previous control**, and **Back** instead.

## Message list

| Key | Action |
| --- | --- |
| Up / Down | Focus a message row |
| Center / Enter | Open the focused message, or toggle its selection if selection mode is already active |
| `0` | Open the account and folder drawer |
| `5` | Refresh the message list |
| `7` | Compose a message |
| `9` | Focus and expand search |
| Left soft key / F1 | Open the list's current actions |

In keypad mode, the traditional message list gives keyboard focus to the complete row, with a visible outline.
Touching its avatar or star still performs the existing selection or star action. Closing the drawer restores
the previous list focus when that control is still available.

![Directional focus on a complete message row](assets/hardware-keypad-message-focus.png)

## Reading a message

The first completed body load focuses the message body once, preserving the restored scroll position.
It does not take focus back after you have begun navigating or editing another control.

| Key | Action |
| --- | --- |
| Up / Down while the body has focus | Scroll the message document; holding the key continues scrolling |
| `1` / Home navigation key | Scroll to the top of the document |
| `3` / End navigation key | Scroll to the bottom, including the attachment area |
| `2` / Page Up | Scroll up one page with overlap |
| `8` / Page Down | Scroll down one page with overlap |
| `4` / `6` while the body has focus | Pan left / right when HTML content is wider than the viewport |
| `5` | Focus the body; reveal it if it has scrolled off-screen |
| `7` | Focus the header's reply action, when available |
| `9` | Focus the attachment-list entry point, when attachments are available |

Press Center / Enter after `7` or `9` to activate the focused control. These shortcuts do not send a reply or
open an attachment automatically. A fresh Up / Down press at the document boundary can move focus out of the
body to nearby controls. On other buttons, arrows retain their normal focus behavior. Use `*` / `#` to move
between controls or HTML links, and F2 to return to the message list. The physical system Home key is unchanged;
the table's Home navigation key means a keyboard's `MOVE_HOME` event.

Scrolling uses the outer message document for both plain text and HTML bodies. Page overlap and line steps
account for the viewport and system font size. Horizontal panning is available only when the HTML body can
actually scroll horizontally; a message fitted to the screen may not need it.

## Composing a message

F1 opens the composer's actual actions menu, including its existing send, save, and attachment actions.
The following keypad actions appear first:

| Menu action | Result |
| --- | --- |
| Next field / Previous field | Move between text fields without inserting a tab or taking digits from the input method |
| Message body | Focus and reveal the message editor |
| Attachments | Focus the first attachment's remove control; available when attachments exist |

Field navigation preserves the editor's cursor or selection. Removing an attachment restores focus to the
next attachment, the previous attachment, or the message body. Sending, saving a draft, and discarding changes
retain their existing confirmation behavior.

## Dialogs and choice lists

Thunderbird's confirmation dialogs, choice lists, compose dialogs, and keypad menus use their own window
mappings. They never activate shortcuts on the screen behind the dialog.

| Key | Action in the dialog |
| --- | --- |
| Left soft key / F1 | Move to the next control, including leaving a text field |
| Right soft key / F2 | Hide the on-screen keyboard first; otherwise use the dialog's normal Back behavior |
| `*` / `#` outside a text editor | Move to the previous / next control |
| Directional pad and Center / Enter | Navigate and activate choices using the dialog's normal behavior |

`*` and `#` inside a text editor still belong to the input method. F1 provides a way to leave that editor
without using those characters as navigation keys.

## Call and end-call shortcuts

In the same settings screen, **Call key action** and **End-call key action** can independently be set to
**None** (the default), **Delete**, **Archive**, or **Toggle star**. These actions apply only to the currently
focused message in the message list. They do not act on a previously focused message, an open message,
or a multiple selection. Delete retains the existing delete-confirmation setting.

These two shortcuts require the optional **Phone** (`READ_PHONE_STATE`) permission to check whether a call
is ringing, dialing, active, or on hold. No telephone numbers or call history are read. If permission is
denied or revoked, the phone keys retain their normal handling. The shortcuts also remain inactive while
Telecom or cellular state reports a call, or communication audio is in use. Call state, the configured action,
and the focused message are checked again on release, and a held key executes at most once. These checks
depend on the states reported by Android; they are not an atomic guarantee against a late or incorrect
firmware report.

Android and device firmware may handle a phone key before an application receives it, especially the
end-call key. This mode only handles `KEYCODE_CALL` and `KEYCODE_ENDCALL` events delivered to Thunderbird.
It does not remap Home or Power keys, change the default phone application, or intercept system calls.
An unavailable physical key therefore cannot be enabled by this setting alone.

When a configured Call key is delivered to a focused idle message list, its normal idle shortcut is replaced
by the selected message action, including a held press. Leave the action as **None** to retain that idle behavior.
Physical incoming-call and end-call behavior must be verified on the target phone; this guide does not claim
that such a real-device call test has been completed for this build.

## Scope

The mappings cover Thunderbird activity and dialog windows, including its keypad menus.
System file pickers, external browsers or sign-in applications, and external
attachment viewers keep their own key handling. The mode does not replace an input method or add predictive
text entry. It does not require changing system key layouts, the default dialer, Home, or Power behavior.

## Source patch and test builds

This is an application source change, not an installable add-on for an existing Thunderbird APK.
A source patch can be applied to the documented base revision and rebuilt. A debug test build uses the
separate `net.thunderbird.android.debug` application ID and can be installed alongside the official app.
It has its own account data; installing it does not migrate accounts automatically.

For a source checkout containing the patch:

```shell
./gradlew :app-thunderbird:assembleFossDebug
```

The upstream minimum Android version is unchanged. Supporting Android 8 and newer does not imply that
every manufacturer key layout, authentication flow, attachment viewer, or embedded HTML control has been tested.
