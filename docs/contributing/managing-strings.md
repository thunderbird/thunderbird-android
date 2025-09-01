# 📝 Managing Strings & Translations

This document explains how developers manage **user-visible strings** and **translations** in the Thunderbird for Android project.

> [!NOTE]
> Translators: If you just want to contribute translations via Weblate, see [Translations](translations.md).
> This document is developer-focused.

## 📖 Approach

* We use Android’s [resource system](https://developer.android.com/guide/topics/resources/localization) for localizing strings.
* Source language: **English** (American English, represented as `en`).
* **Source strings** are modified only in this repository (via pull requests).
* **Translations** are managed exclusively in [Weblate](https://hosted.weblate.org/projects/tb-android/) and merged into the repo by the Thunderbird team.
* This avoids conflicts by keeping source in Git and translations in Weblate.
* **Languages** are added/removed when they reach 70% translation or fall below 60%.

## ➕ Adding a String

1. Add the new string in the appropriate `res/values/strings.xml` file.
2. **Do not** add translations.
   * After merge, Weblate will pull the new string.
   * Translators can then add translations in Weblate.

## ✏️ Changing a String

* Only acceptable for **typo or grammar fixes** in English.
* If you need to change the meaning of a string:
  * **Remove the old string**.
  * **Add a new one with a new key**.
  * This avoids breaking existing translations.

## ❌ Removing a String

1. Delete the string from `res/values/strings.xml`.
2. **Do not** touch `res/values-<lang>/strings.xml`.
   * The next Weblate sync will remove translations automatically.

## 🔄 Changing Translations in the Repo

Avoid direct edits to translation files in Git.

* Translations should be updated in Weblate.
* If a mechanical/global change is necessary:
  * Discuss with the core team.
  * Follow this procedure:
    1. Lock components in Weblate ([maintenance page](https://hosted.weblate.org/projects/tb-android/#repository)).
    2. Commit all outstanding changes.
    3. Push Weblate changes (creates a PR).
    4. Merge the Weblate PR.
    5. Apply your mechanical change in a PR.
    6. Wait for Weblate sync to propagate.
    7. Unlock components in Weblate.

## 🔀 Merging Weblate PRs

When merging Weblate-generated PRs:

* Check plural forms for **cs, lt, sk** locales.
  * Weblate does not handle these correctly ([issue](https://github.com/WeblateOrg/weblate/issues/7520)).
  * Ensure both `many` and `other` forms are present.
  * If unsure, duplicating values is acceptable.

## 🌍 Managing Languages

We use Gradle’s [`androidResources.localeFilters`](https://developer.android.com/reference/tools/gradle-api/8.8/com/android/build/api/dsl/ApplicationAndroidResources#localeFilters%28%29) to control which languages are bundled.

This must stay in sync with the string array `supported_languages` so the in-app picker shows only available locales.

### 🔎 Checking Translation Coverage

Before adding a language, we require that it is at least 70% translated in Weblate.

We provide a **Translation CLI** script to check translation coverage:

```bash
./scripts/translation --token <weblate-token>
```

- Requires a [Weblate API token](https://hosted.weblate.org/accounts/profile/#api)
- Default threshold is 70% (can be changed with `--threshold`)
- Change the threshold via --threshold <N>.

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

## 🧩 Adding a Component to Weblate

When a new module contains translatable strings, a new Weblate component is required.

Steps:

1. Go to [Add Component](https://hosted.weblate.org/create/component/?project=3696).
2. Choose **From existing component**.
3. Name your component.
4. For “Component”, select **K-9 Mail/Thunderbird/ui-legacy**.
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
