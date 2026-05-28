# Enabling S/MIME via CipherMail

Thunderbird for Android supports **S/MIME** (signing and encrypting mail with
X.509 certificates) by binding to a companion app that owns the keystore and
certificates. The reference companion is **CipherMail for Android**. This
guide walks you through the one-time setup.

If you only need OpenPGP, see the existing OpenKeychain integration instead —
S/MIME and OpenPGP coexist in Thunderbird and can be enabled per account.

## What you'll need

- Thunderbird for Android installed (this app).
- CipherMail for Android installed.
- Your personal S/MIME certificate as a `.p12` / `.pfx` file (PKCS#12), along
  with the passphrase that protects it. Typically issued by your organisation
  or by a public CA. If you don't have one yet, your IT department or
  certificate provider can issue one.
- The certificates of the people you want to **send encrypted mail to** —
  usually obtained from a previous signed mail they sent you, or from a
  directory service.

## Step 1 — Install CipherMail

CipherMail is a separate Android app. It can be installed from:

- **Google Play**.
- The CipherMail download page at <https://www.ciphermail.com>.

CipherMail is planned for F-Droid once this S/MIME companion API is accepted
upstream; until then, F-Droid users should use the direct download.

After installing, open CipherMail at least once so it can complete its
initial setup. The first launch will start a short wizard.

## Step 2 — Set a keystore passphrase

The first time you open CipherMail, the **Start Wizard** prompts you to
create a keystore passphrase. This passphrase protects your private key on
the device; it is **not** the same as your `.p12` import passphrase.

1. Tap **Next** through the welcome screen.
2. Enter a strong passphrase and confirm. Choose something you can remember
   — you will be asked to re-enter it whenever the keystore has been locked
   (e.g. after a device reboot or after the cache is cleared).
3. Tap **Next** to continue to account setup.

You can later re-run the wizard from CipherMail's menu (**⋮ → Setup**)
without clearing app data.

## Step 3 — Import or generate your S/MIME certificate

Inside the wizard, after the account step:

1. Choose **Import certificate**.
2. Browse to your `.p12` / `.pfx` file (or open it from Files / Downloads).
3. Enter the **PKCS#12 passphrase** that was set when the file was exported.
   (Again: this is the import passphrase, distinct from the keystore
   passphrase from Step 2.)
4. CipherMail will list the certificate(s) it found. Confirm to import.

The certificate is now stored in CipherMail's keystore. CipherMail will use
it for signing outgoing mail from any matching email address.

## Step 4 — Enable S/MIME on your Thunderbird account

1. In Thunderbird, open **Settings**.
2. Tap the account you want to protect with S/MIME.
3. Scroll to **S/MIME** and tap **Enable S/MIME support**.
4. If CipherMail is the only S/MIME provider installed, it's selected
   automatically. If more than one is installed (rare today), pick
   **CipherMail** from the list.
5. The summary line under "Enable S/MIME support" should now read
   **Connected to CipherMail**.

If you see **No S/MIME app found**, CipherMail isn't installed — go back to
Step 1. If you see **Missing S/MIME app**, the previously selected provider
was uninstalled; tap the row and pick a new one.

## Step 5 — Send your first signed and encrypted mail

1. Compose a new message to a recipient whose certificate CipherMail has.
2. Look at the **lock icon** in the compose toolbar:
   - **Green**: certificates available for every recipient — the message
     will be signed and encrypted.
   - **Red**: at least one recipient is missing a certificate — the message
     will be signed but not encrypted to the missing recipient(s). Tap the
     icon for details.
3. Tap **Send**.

The first time you send after a reboot (or after CipherMail's keystore
cache has expired), Thunderbird hands you off to CipherMail's **passphrase
prompt**. Enter the keystore passphrase you set in Step 2. After OK,
Thunderbird automatically retries the send and CipherMail signs and encrypts
without prompting again until the cache clears.

## Step 6 — Read an encrypted or signed mail

When an S/MIME-encrypted mail arrives:

1. Open the message. Thunderbird routes it to CipherMail for decryption.
2. If the keystore is locked, you'll see the passphrase prompt again —
   same dialog as in Step 5.
3. After unlock, the message is displayed in plaintext with an indicator
   showing whether the signature was valid and trusted.

Signature indicators in the message view:

- **Valid, trusted** — the signature verified and the signer's certificate
  chains to a trusted root.
- **Valid, untrusted** — the signature verified but the signer's certificate
  is self-signed or chains to a root your device doesn't trust. The message
  is genuine but the signer's identity isn't confirmed.
- **Invalid** — the signature did not verify; the message may have been
  tampered with.
- **Certificate missing / expired / revoked** — the signature can't be
  evaluated for the reason shown.

## Understanding the compose lock icon

While composing, the lock icon next to the recipient field reflects what
will happen when you tap **Send**. CipherMail tells Thunderbird which
recipients have usable certificates; Thunderbird translates that into one
of four states:

| State                  | Meaning                                                                                                | When you see it                                                                  |
|------------------------|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| **Hidden**             | S/MIME is not configured on this account.                                                              | The account's S/MIME provider preference is empty. Enable it (Step 4) to show.   |
| **Gray (disabled)**    | S/MIME is enabled but there are no recipients to check yet.                                            | After enabling S/MIME, before you add anyone to To / Cc / Bcc.                   |
| **Green (trusted)**    | Every recipient has a valid certificate. The message will be signed **and** encrypted on send.         | Once every address in To / Cc / Bcc resolves to a usable certificate.            |
| **Red (error)**        | At least one recipient is missing a valid certificate, **or** Thunderbird could not reach CipherMail.  | Most common cause: you don't have that recipient's cert yet. Tap for details.    |

**Tapping the icon** shows a short message:

- Green → *"S/MIME: signing and encrypting"*
- Red → *"S/MIME: missing certificates for some recipients"*

The state refreshes each time you add or remove a recipient. CipherMail's
certificate lookup is asynchronous, so on slow devices you may briefly see
the previous state before it updates.

### What to do when the icon is red

1. Tap the icon to confirm it's a missing-certificate issue (and not a
   provider error).
2. Ask the recipient to send you any signed mail — CipherMail will extract
   their certificate from the signature automatically when you read it.
3. Alternatively, import their certificate manually via CipherMail's
   **Certificates** screen if you have it as a `.cer` / `.crt` file.
4. The icon will turn green automatically the next time you re-focus the
   recipient field (or you can briefly toggle a recipient to force a
   refresh).

If you send while the icon is red, the message is **not** silently dropped
or sent in plaintext to the missing recipient — it fails with an explicit
error. To send in plain text on purpose, disable S/MIME on the account
first.

### Notes for translators

The strings the user sees here all live in
`legacy/ui/legacy/src/main/res/values/strings.xml`:

| Key                                       | English source                                                                                  |
|-------------------------------------------|-------------------------------------------------------------------------------------------------|
| `smime_status_active`                     | S/MIME: signing and encrypting                                                                  |
| `smime_status_missing_certs`              | S/MIME: missing certificates for some recipients                                                |
| `account_settings_smime`                  | S/MIME                                                                                          |
| `account_settings_smime_app`              | Enable S/MIME support                                                                           |
| `account_settings_smime_app_select_title` | Select S/MIME app                                                                               |
| `account_settings_smime_summary_off`      | No S/MIME app configured                                                                        |
| `account_settings_smime_summary_on`       | Connected to %s                                                                                 |
| `account_settings_smime_summary_config`   | Configuring…                                                                                    |
| `account_settings_smime_missing`          | Missing S/MIME app - was it uninstalled?                                                        |
| `account_settings_smime_no_provider_title`| No S/MIME app found                                                                             |
| `account_settings_smime_no_provider_msg`  | No S/MIME provider app is installed. Please install CipherMail to enable S/MIME support.        |

When translating, keep the "S/MIME" trademark intact — it's an industry
term, not a brand we're free to localise. The provider product name
(*"CipherMail"*) is also a proper noun and should not be translated.

## Troubleshooting

**"Connected to CipherMail" never appears.** Re-open CipherMail and make
sure the wizard completed (a certificate is imported, a passphrase is set).
Then return to Thunderbird's account settings and toggle **Enable S/MIME
support** off and on again.

**Sending fails with "missing certificate for recipient".** You don't have
an S/MIME certificate for at least one recipient. Either:

- Ask them to send you a signed mail first — CipherMail extracts their
  certificate from the signature automatically.
- Or, if you have their certificate file, import it via CipherMail's
  **Certificates** screen.

**Passphrase prompt keeps reappearing.** That's expected after a reboot or
after the cache has been cleared. The prompt appears once per "unlock
session" — subsequent sends and receives reuse the cached passphrase.

**After reinstalling CipherMail, the keystore is gone.** Reinstall wipes
the device-bound key that protects CipherMail's keystore at rest. You'll
need to set a new passphrase and re-import your certificate. Your sent
messages and counterparties' certificates that lived only inside
CipherMail's local store are lost; ones cached in Thunderbird-side
sources are not affected.

**I uninstalled CipherMail.** Thunderbird will show **Missing S/MIME app**
on the account. Re-install CipherMail, or change the account's S/MIME
provider to a different one in **Settings → account → S/MIME**.

## See also

- [ADR 0009 — Companion App + AIDL Service for S/MIME](../../architecture/adr/0009-smime-companion-app-architecture.md) — why Thunderbird uses a separate app for S/MIME at all.
- [`plugins/smime-api/README.md`](../../../plugins/smime-api/README.md) — for developers integrating against the same API.
