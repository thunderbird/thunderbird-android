# 📝 Managing Strings & Languages

This document explains how developers manage our english **source strings** and add/remove **languages** in the
Thunderbird for Android project.

> [!NOTE]
> Translators: If you want to contribute translations, see [Translations](translations.md).
> This document is developer-focused.

## 📖 Approach

* We use Android’s [resource system](https://developer.android.com/guide/topics/resources/localization) for localizing strings and Compose Multiplatform Resources for localizing strings in common code (Kotlin Multiplatform).
* **Source language** is **English** (American English, represented as `en`).
* **Source strings** are modified only in this repository (via pull requests).
* **Translations** are managed exclusively in [Weblate](https://hosted.weblate.org/projects/tb-android/) and merged into the repository by the Thunderbird team.
* **Languages** are added/removed when they reach 70% translation or fall below 60%.

## 🔄 Changing Source Strings

Source strings are always stored in `res/values/strings.xml` or (**English**, `en`).

They must be managed carefully to avoid breaking existing translations.

- **Do not edit translation files directly in Git.**
  Translations should always be updated in Weblate.

### 🔧 Mechanical/Global Changes

If a **mechanical or global change** to translations is required (for example, renaming placeholders or fixing
formatting across all languages):

1. Lock components in Weblate ([maintenance page](https://hosted.weblate.org/projects/tb-android/#repository)).
2. Commit all outstanding changes.
3. Push Weblate changes (creates a PR).
4. Merge the Weblate PR.
5. Apply your mechanical change in a separate PR.
6. Wait for Weblate sync to propagate your merged PR.
7. Unlock components in Weblate.

This ensures translators do not work on outdated strings and avoids merge conflicts.

### ➕ Adding a String

1. Add the new string in the appropriate `res/values/strings.xml` file.
2. **Do not** add translations.
   * After merge, Weblate will pull the new string.
   * Translators can then add translations in Weblate.

### ✏️ Changing a String

There are two kinds of changes to source strings:

#### 🔤 Typos or Grammar Fixes

Correcting minor errors (spelling, capitalization, punctuation, grammar) in the English source is allowed:

- **Keep the same key** — translations will remain valid.

Example:

- Changing "Recieve" to "Receive" or "email" to "Email".

#### 🧭 Changing Meaning

> [!CAUTION]
> Never reuse an existing key for a changed meaning — this would cause translators’ work to become misleading or incorrect.

If the meaning of the string changes (new wording, different context, updated functionality):

1. **Add a new key** with the new string.
2. **Update all references** in the source code to use the new key.
3. **Delete the old key** from `res/values/strings.xml`.
4. **Delete the old key’s translations** from all `res/values-*/strings.xml` files.
5. **Build the project** to ensure there are no references to the old key remaining.

This ensures there are no stale or misleading translations left behind.

Example:

- Old: "Check mail now" (`action_check_mail`)
- New: "Sync mail" (`action_sync_mail`)

Steps:

- Add new key `action_sync_mail` with value "Sync mail" to `res/values/strings.xml`.
- Update all code references from `R.string.action_check_mail` to `R.string.action_sync_mail`.
- Remove `action_check_mail` from `res/values/strings.xml` and all `res/values-*/strings.xml`
- Build the project to ensure no references to `action_check_mail` remain.
- After the next sync, Weblate will prompt translators to provide translations for action_sync_mail.

### ❌ Removing a String

1. **Delete the key** from `res/values/strings.xml`.
2. **Delete the key’s translations** from all `res/values-*/strings.xml` files.
3. **Build the project** to ensure there are no references to the removed key remaining.

## 🔀 Merging Weblate PRs

When merging Weblate-generated PRs:

* Check plural forms for **cs, lt, sk** locales. Weblate does not handle these correctly ([issue](https://github.com/WeblateOrg/weblate/issues/7520)).
* Ensure both `many` and `other` forms are present.
  * If unsure, reusing values from `many` or `other` is acceptable.

## 🌍 Managing Languages

We use Gradle’s [`androidResources.localeFilters`](https://developer.android.com/reference/tools/gradle-api/8.8/com/android/build/api/dsl/ApplicationAndroidResources#localeFilters%28%29) to control which languages are bundled.

This must stay in sync with the string array `supported_languages` so the in-app picker shows only available locales.

### 🔎 Checking Translation Coverage

Before adding a language, we require that it is at least **70% translated** in Weblate.

We provide a **Translation CLI** script to check translation coverage:

```bash
./scripts/translation --token <weblate-token>

# Specify the low 60% threshold
./scripts/translation --token <weblate-token> --threshold 60
```

- Requires a [Weblate API token](https://hosted.weblate.org/accounts/profile/#api)
- Default threshold is 70% (can be changed with `--threshold <N>`)

For example code integration, run with --print-all:

```bash
./scripts/translation --token <weblate-token> --print-all
```

This output can be used to update:

- `resourceConfigurations` in `app-k9mail/build.gradle.kts` and `app-thunderbird/build.gradle.kts`
- `supported_languages` in `legacy/core/src/res/values/arrays_general_settings_values.xml`

### ➖ Removing a Language

1. Remove language code from `androidResources.localeFilters` in:
   * `app-thunderbird/build.gradle.kts`
   * `app-k9mail/build.gradle.kts`
2. Remove entry from `supported_languages` in:
   * `app/core/src/main/res/values/arrays_general_settings_values.xml`

### ➕ Adding a Language

1. Add the code to `androidResources.localeFilters` in both app modules.
2. Add entry to `supported_languages` in:
   * `app/core/src/main/res/values/arrays_general_settings_values.xml`
3. Add corresponding display name in:
   * `app/ui/legacy/src/main/res/values/arrays_general_settings_strings.xml` (sorted by Unicode default collation order).
4. Ensure indexes match between `language_entries` and `language_values`.

> [!IMPORTANT]
> The order of entries in language_entries and language_values must match exactly. Incorrect ordering will cause mismatches in the language picker.

## 🧩 Adding a Component to Weblate

When a new module contains translatable strings, a new Weblate component must be created.

Steps:

1. Go to [Add Component](https://hosted.weblate.org/create/component/?project=3696).
2. Choose **From existing component**.
3. Name your component.
4. For **Component**, select **Thunderbird for Android / K-9 Mail/ui-legacy**.
5. Continue → Select **Specify configuration manually**.
6. Set file format to **Android String Resource**.
7. File mask: `path/to/module/src/main/res/values-*/strings.xml`
8. Base file: `path/to/module/src/main/res/values/strings.xml`
9. Uncheck **Edit base file**.
10. License: **Apache License 2.0**.
11. Save.

## ⚠️ Language Code Differences

Android sometimes uses codes that differ from Weblate (e.g. Hebrew = `iw` in Android but `he` in Weblate).

Automation tools must map between systems.
See [LanguageCodeLoader.kt](https://github.com/thunderbird/thunderbird-android/blob/main/cli/translation-cli/src/main/kotlin/net/thunderbird/cli/translation/LanguageCodeLoader.kt#L12-L13) for an example.

You could find a more complete list of differences in the [Android documentation](https://developer.android.com/guide/topics/resources/localization#LocaleCodes) and [Unicode and internationalization support](https://developer.android.com/guide/topics/resources/internationalization)
