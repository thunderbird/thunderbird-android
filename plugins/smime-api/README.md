# S/MIME API library

The S/MIME API provides methods to execute S/MIME operations — sign, encrypt,
decrypt, verify, and certificate lookup — without user interaction from
background threads. This is done by binding your mail client to a remote
service provided by [CipherMail](https://www.ciphermail.com) or another
S/MIME provider.

The design parallels the [OpenPGP API](../openpgp-api-lib/README.md): a single
AIDL service with an `execute(Intent, ParcelFileDescriptor, int)` entry point,
operations selected by Intent action, bulk MIME data streamed over pipes
rather than passed as Intent extras.

### News

#### Version 1
  * Initial release. See `CHANGELOG.md` for the full action / extra / result
    inventory.

### License

The reference provider (CipherMail) is GPL; this API library is licensed under
[Apache License v2](LICENSE) so it can be embedded in any S/MIME-aware mail
client regardless of the client's license.

### Add the API library to your project

The library is consumed as a Gradle module within this repository:

```kotlin
dependencies {
    implementation(projects.plugins.smimeApi.smimeApi)
}
```

The `<queries>` element required for service discovery on Android 11+ is
already declared in `plugins/smime-api/smime-api/src/main/AndroidManifest.xml`
and is merged into the host app's manifest automatically.

### API

[`SmimeApi`](smime-api/src/main/java/com/ciphermail/smime/api/util/SmimeApi.java)
defines every Intent action, extra, and result constant. The AIDL contract is
defined in
[`ISmimeService.aidl`](smime-api/src/main/aidl/com/ciphermail/smime/api/ISmimeService.aidl).

### Short tutorial

The API is **not** driven by `startActivityForResult`; operations run as
background calls and only surface UI (via a `PendingIntent`) when the provider
needs user input — typically to unlock its keystore.

#### 1. Bind to the provider

```java
SmimeServiceConnection serviceConnection;

@Override
protected void onCreate(Bundle state) {
    super.onCreate(state);
    serviceConnection = new SmimeServiceConnection(
            this,
            "com.ciphermail.android",   // provider package
            new SmimeServiceConnection.OnBound() {
                @Override public void onBound(ISmimeService service) { /* ready */ }
                @Override public void onError(Exception e) { /* handle */ }
            });
    serviceConnection.bindToService();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (serviceConnection != null) serviceConnection.unbindFromService();
}
```

#### 2. Build the request Intent and run the operation

```java
Intent request = new Intent(SmimeApi.ACTION_SIGN_AND_ENCRYPT);
request.putExtra(SmimeApi.EXTRA_API_VERSION, SmimeApi.API_VERSION);
request.putExtra(SmimeApi.EXTRA_USER_IDS,
        new String[] { "alice@example.com", "bob@example.com" });

InputStream  mimeBody    = new ByteArrayInputStream(rawMimeBytes);
ByteArrayOutputStream smimeOutput = new ByteArrayOutputStream();

SmimeApi api = new SmimeApi(serviceConnection.getService());
api.executeApiAsync(request, mimeBody, smimeOutput, this::onSmimeResult);
```

`executeApiAsync` runs `executeApi` on a worker thread and posts the result
back via `SmimeCallback`. If you are already on a background thread you can
call `executeApi(...)` directly — it is blocking. Calling it on the main
thread will fail Android's strict-mode network/disk-on-main checks for any
non-trivial message.

#### 3. Handle the result

```java
private void onSmimeResult(Intent result) {
    int code = result.getIntExtra(SmimeApi.RESULT_CODE, SmimeApi.RESULT_CODE_ERROR);
    switch (code) {
        case SmimeApi.RESULT_CODE_SUCCESS: {
            // smimeOutput now contains the wrapped MIME bytes.
            SmimeSignatureResult sig =
                    result.getParcelableExtra(SmimeApi.RESULT_SIGNATURE);
            // ...
            break;
        }
        case SmimeApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
            PendingIntent pi = result.getParcelableExtra(SmimeApi.RESULT_INTENT);
            try {
                startIntentSenderForResult(pi.getIntentSender(), REQ_UNLOCK,
                        null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // ...
            }
            break;
        }
        case SmimeApi.RESULT_CODE_ERROR: {
            SmimeError err = result.getParcelableExtra(SmimeApi.RESULT_ERROR);
            // err.getErrorId(): see SmimeError.* constants
            break;
        }
    }
}
```

#### 4. Retry after user interaction

If the provider returned `RESULT_CODE_USER_INTERACTION_REQUIRED`, its
`PendingIntent` will launch a provider-owned activity (for CipherMail, the
keystore passphrase dialog). On `RESULT_OK` simply rebuild the request and
call `executeApiAsync` again — the second call will find the cached
credentials and return `RESULT_CODE_SUCCESS`.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQ_UNLOCK && resultCode == RESULT_OK) {
        retrySignAndEncrypt(); // same code as step 2
    }
}
```

### Actions at a glance

| Action                       | Input stream             | Output stream      | Notable result extras                    |
|------------------------------|--------------------------|--------------------|------------------------------------------|
| `ACTION_CHECK_PERMISSION`    | —                        | —                  | `RESULT_INTENT` (consent dialog)         |
| `ACTION_DECRYPT_VERIFY`      | encrypted MIME bytes     | decrypted MIME     | `RESULT_DECRYPTION`, `RESULT_SIGNATURE`  |
| `ACTION_SIGN_AND_ENCRYPT`    | plain MIME bytes         | wrapped S/MIME     | (uses output stream only)                |
| `ACTION_GET_CERTIFICATES`    | —                        | —                  | `RESULT_CERTIFICATES`                    |
| `ACTION_IMPORT_CERTIFICATE`  | DER- or PEM-encoded cert | —                  | (uses RESULT_CODE only)                  |

### Tips

* `api.executeApi(data, is, os);` is blocking. Use `executeApiAsync` for a
  fire-and-callback variant.
* To bind to CipherMail's **debug** build during development, use
  `com.ciphermail.android.debug` as the provider package — release and debug
  are installable side-by-side with distinct package IDs.
* To let the user choose between S/MIME providers (when more than one is
  installed), use Thunderbird's `SmimeAppSelectDialog` (legacy/ui/legacy).
  It enumerates all packages that declare an `ISmimeService` service in their
  manifest.
* Bulk message data never travels as an Intent extra — always through the
  `ParcelFileDescriptor` pipes set up by `executeApi`. This keeps Binder
  transactions small even for multi-megabyte attachments.
* The provider may take several seconds for the first call after process
  start (keystore initialisation, certificate cache warm-up). Show a
  progress indicator in compose / message-view UI rather than blocking the
  user thread.

### Cross-process passphrase unlock

CipherMail's keystore is locked by default. When a sign/encrypt or
decrypt/verify call lands while the keystore is locked, the service returns
`RESULT_CODE_USER_INTERACTION_REQUIRED` immediately (no IPC timeout) with a
`PendingIntent` for its `KeyStorePassphraseDialog`. The dialog broadcasts the
passphrase back to a singleton `CachingPasswordProvider` on success, which
caches it for subsequent calls. The client's only job is to launch the
`PendingIntent` via `startIntentSenderForResult` and retry on `RESULT_OK`.
