# Hardware keypad navigation

Hardware keypad navigation is an optional mode for Android phones with a directional pad and physical keys.
It is disabled by default. Enable it under **Settings → General settings → Interaction → Hardware keypad navigation**.

![Keypad navigation and independently configured phone-key actions](assets/hardware-keypad-settings.png)

The keypad changes use Android APIs available on Android 8.0 (API 26) and newer. They do not require root
or accessibility-service permissions. Device firmware must deliver the keys to the app.

| Key | Action when the mode is enabled |
| --- | --- |
| Directional pad | Move focus using the screen's normal Android navigation |
| Center / Enter | Activate the focused control, or use the text editor's normal Enter behavior |
| Left soft key / F1 | Open the activity's options menu, when available |
| Right soft key / F2 | Go back |
| `*` outside a text editor | Move to the previous control (Shift+Tab) |
| `#` outside a text editor | Move to the next control (Tab) |

Numbers and characters, including `*` and `#` in text editors, remain available to the input method.
Modified keys such as Ctrl+F1 retain their normal behavior. In the message reader, Left and Right move focus
instead of switching messages. Backspace no longer invokes the delete-message shortcut in this mode.
Use the options menu for message actions such as reply, refresh, or delete.

## Call and end-call shortcuts

In the same settings screen, **Call key action** and **End-call key action** can independently be set to
**None** (the default), **Delete**, **Archive**, or **Toggle star**. These actions apply only to the currently
focused message in the message list. They do not act on a previously focused message, an open message,
or a multiple selection. Delete retains the existing delete-confirmation setting.

These two shortcuts require the optional **Phone** (`READ_PHONE_STATE`) permission to check whether a call
is ringing, dialing, active, or on hold. No telephone numbers or call history are read. If permission is
denied or revoked, the phone keys retain their normal handling. The shortcuts also remain inactive while
the system reports a call or communication audio in use. Call state, the configured action, and the focused
message are checked again on release, and a held key executes at most once.

Android and device firmware may handle a phone key before an application receives it, especially the
end-call key. This mode only handles `KEYCODE_CALL` and `KEYCODE_ENDCALL` events delivered to Thunderbird.
It does not remap Home or Power keys, change the default phone application, or intercept system calls.
An unavailable physical key therefore cannot be enabled by this setting alone.

The message list displays a focus outline when a message or one of its controls has keyboard focus.
Focus visibility also works with a full hardware keyboard without enabling keypad mode.

![Directional focus on a complete message row](assets/hardware-keypad-message-focus.png)

## Scope

The soft-key mappings apply to activity windows. Independent dialog and popup windows retain their existing
Android key handling. Screens without an activity options menu do not acquire one by enabling this mode.
The mode does not replace an input method or add predictive text entry.

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
