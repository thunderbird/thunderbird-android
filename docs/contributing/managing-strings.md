# 📝 Managing Strings & Languages

This document explains how developers manage our English **source strings** and add/remove **languages** in the
Thunderbird for Android project.

> [!NOTE]
> Translators: If you want to contribute translations, see [Translations](translations.md).
> This document is developer-focused.

## 📖 Approach

* We use Android’s [resource system](https://developer.android.com/guide/topics/resources/localization) for localizing strings in Android-only modules.
* We use [Compose Multiplatform Resources](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html) for localizing strings in Kotlin Multiplatform (KMP) modules.
* **Source language** is **English** (American English, represented as `en`).
* **Source strings** are modified only in this repository (via pull requests).
* **Translations** are managed exclusively in [Weblate](https://hosted.weblate.org/projects/thunderbird/thunderbird-android/), stored in the [Thunderbird for Android l10n repository](https://github.com/thunderbird/thunderbird-android-l10n), and imported via the [Translation - Update](https://github.com/thunderbird/thunderbird-android/actions/workflows/l10n-translation-update.yml) workflow.
* **Languages** are added/removed when they reach 70% translation or fall below 60%.

## 🔄 Changing Source Strings

Source strings are always stored in English (`en`). They must be managed carefully to avoid breaking
existing translations.

- **Do not edit translation files directly in Git.**
- Translations should always be updated in Weblate.

### Android Resource Files

Stored in `res/values/strings.xml` (and `plurals.xml` if applicable).

### Compose Multiplatform Resources

Stored in `src/commonMain/composeResources/values/strings.xml`.

### 🏗️ Gradle Setup

To use Compose Multiplatform Resources in a module, follow these steps in your `build.gradle.kts`:

1. **Apply the plugin:**
   Use `ThunderbirdPlugins.Library.kmpCompose` for a KMP library module with Compose support.

   ```kotlin
   plugins {
       id(ThunderbirdPlugins.Library.kmpCompose)
   }
   ```
2. **Configure `compose.resources` block:**
   Set `publicResClass` to `false` and provide a unique `packageOfResClass`.

   ```kotlin
   compose.resources {
       publicResClass = false
       packageOfResClass = "net.thunderbird.feature.yourfeature.resources"
   }
   ```
3. **Ensure `android` namespace is set:**
   The Android target requires a namespace in the `kotlin` block.

   ```kotlin
   kotlin {
       android {
           namespace = "net.thunderbird.feature.yourfeature.api"
       }
   }
   ```

### 🔧 Mechanical/Global Changes

If a **mechanical or global change** to translations is required (for example, renaming placeholders or fixing formatting across all languages), follow this workflow:

1. **Lock components in Weblate:**
   Go to the [maintenance page](https://hosted.weblate.org/projects/thunderbird/thunderbird-android/#repository) and lock all components to prevent new translations during the change.
2. **Push outstanding changes:**
   Ensure all pending translations in Weblate are committed and pushed to the l10n repository.
3. **Pull latest translations:**
   Trigger the [Translation - Update](https://github.com/thunderbird/thunderbird-android/actions/workflows/l10n-translation-update.yml) GitHub workflow manually using `workflow_dispatch`.
4. **Merge the pull request:**
   Review and merge the resulting PR to ensure your local `main` branch is in sync with Weblate.
5. **Apply your change:**
   Coordinate source changes in this repository with translation changes in the l10n repository, using reviewed pull requests.
6. **Update the l10n mirror and Weblate configuration:**
   Import the updated source files into the l10n repository. If your change involved moving files or changing directory structures, run the mirror's `scripts/l10n-weblate update` command to update Weblate configuration.
7. **Unlock components:**
   Once Weblate has pulled the changes from the l10n repository, unlock the components.

See the [Weblate CLI section](#-weblate-cli) for more details on the tooling.

### ➕ Adding a String

1. Add the new string in the appropriate source file:
   - Android: `res/values/strings.xml`
   - Compose: `src/commonMain/composeResources/values/strings.xml`
2. **Do not** add translations.
   * After merge, the source-import workflow creates a pull request in the l10n repository. Once that pull request is merged, Weblate can pull the new string.
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
3. **Delete the old key** from the source file.
4. **Delete the old key’s translations** from all translation files (e.g. `values-*/strings.xml`).
5. **Build the project** to ensure there are no references to the old key remaining.

This ensures there are no stale or misleading translations left behind.

Example:

- Old: "Check mail now" (`action_check_mail`)
- New: "Sync mail" (`action_sync_mail`)

### ❌ Removing a String

1. **Delete the key** from the source file.
2. **Delete the key’s translations** from all translation files.
3. **Build the project** to ensure there are no references to the removed key remaining.

## 🛠️ Using Strings in Code

### Android Resources

Used in Android-only modules or Android-specific source sets.

```kotlin
// In a Context-aware class
val title = context.getString(R.string.my_string_key)

// With arguments
val message = context.getString(R.string.welcome_message, userName)
```

### Compose Multiplatform Resources

Used in KMP modules, primarily in `commonMain`.

```kotlin
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getPluralString
import your.module.package.resources.Res
import your.module.package.resources.my_string_key

// In a Composable function (using the @Composable stringResource)
val title = stringResource(Res.string.my_string_key)

// In a non-Composable (suspend) function
val title = getString(Res.string.my_string_key)

// With arguments
val message = getString(Res.string.welcome_message, userName)

// Plurals
val messagesCount = getPluralString(Res.plurals.new_messages, count, count)
```

> [!NOTE]
> The `Res` class is generated by the Compose Multiplatform Gradle plugin. You may need to build the project to
> generate it after adding new strings.

## 🔀 Merging Translations

Weblate commits translations to the dedicated [l10n repository](https://github.com/thunderbird/thunderbird-android-l10n). An automated [GitHub workflow](https://github.com/thunderbird/thunderbird-android/actions/workflows/l10n-translation-update.yml):
1. Checks out the l10n repository's `main` branch and uses its `scripts/l10n-sync export --branch main` command to select translations belonging to this repository's `main` branch.
2. Creates a pull request with the updated translation files against this repository's `main` branch, without modifying source-language files.
3. Links the exact l10n revision in the pull request and commit metadata and preserves contributor attribution via `Co-authored-by` trailers.

The current rollout imports translations into `main` only; automated imports into `beta` and `release` are not enabled.

Source changes reach Weblate through the l10n repository's [Import from Source workflow](https://github.com/thunderbird/thunderbird-android-l10n/actions/workflows/l10n-sync-from-source.yml).
Review and merge its source-import pull request using the Weblate lock/push/unlock checklist in the pull request body.
The resulting `l10n-sync-manifest.json` records which files and keys belong to each source branch for later export.

When reviewing and merging these PRs:

* Check plural forms for **cs, lt, sk** locales. Weblate does not handle these correctly ([issue](https://github.com/WeblateOrg/weblate/issues/7520)).
* Ensure both `many` and `other` forms are present.
  * If unsure, reusing values from `many` or `other` is acceptable.

## 🌍 Managing Languages

We use Gradle’s [`androidResources.localeFilters`](https://developer.android.com/reference/tools/gradle-api/8.8/com/android/build/api/dsl/ApplicationAndroidResources#localeFilters%28%29) to control which languages are bundled.

This must stay in sync with the string array `supported_languages` so the in-app picker shows only available locales.

### 🛠️ Automation Tools

We provide several CLI tools to assist with translation management.

#### 🔎 Translation CLI

Used to check translation coverage before adding or removing languages.

```bash
./scripts/translation --token <weblate-token>

# Specify the low 60% threshold and verbose logging
./scripts/translation --token <weblate-token> --threshold 60 --log-level BODY
```

- Requires a [Weblate API token](https://hosted.weblate.org/accounts/profile/#api)
- Default threshold is 70% (can be changed with `--threshold <N>`)
- Default log level is `NONE` (can be changed with `--log-level <LEVEL>`)

For example code integration, run with `--print-all`:

```bash
./scripts/translation --token <weblate-token> --print-all
```

This output can be used to update:

- `resourceConfigurations` in `app-k9mail/build.gradle.kts` and `app-thunderbird/build.gradle.kts`
- `supported_languages` in `legacy/core/src/res/values/arrays_general_settings_values.xml`

#### 🔧 Weblate CLI

Component management now runs from a checkout of the [l10n repository](https://github.com/thunderbird/thunderbird-android-l10n), not this source repository.
Its `l10n-config.json` and `l10n-component-config.json` define component discovery and configuration.
The launcher requires Java 21 and downloads and checksum-verifies the pinned tooling release.

Run these commands from the l10n repository root. Commands are dry runs unless `--apply` is supplied.

```bash
# Preview component configuration updates
./scripts/l10n-weblate update --token "$WEBLATE_TOKEN"

# Apply reviewed configuration updates
./scripts/l10n-weblate update --token "$WEBLATE_TOKEN" --apply

# Discover missing components; add --apply after reviewing the dry run
./scripts/l10n-weblate create --token "$WEBLATE_TOKEN"
```

Maintainers can also use the mirror's [Weblate Management workflow](https://github.com/thunderbird/thunderbird-android-l10n/actions/workflows/l10n-weblate-management.yml), which defaults to a dry run.

##### ➕ Adding a Component

1. Ensure your module follows the standard directory structure for strings.
2. Import and merge its source files into the l10n repository.
3. From the l10n repository root, run `scripts/l10n-weblate create --token "$WEBLATE_TOKEN"` to preview missing components.
4. Review the discovered components, then rerun with `--apply` to create them.

For more details, see the [l10n repository README](https://github.com/thunderbird/thunderbird-android-l10n#readme) and [shared tooling documentation](https://github.com/thunderbird/thunderbird-mobile-weblate#readme).

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

### 🧩 Adding a Component to Weblate

When a new module contains translatable strings, a new Weblate component must be created. We provide a **Weblate CLI** to automate this process. See the [Weblate CLI section](#-weblate-cli) for more details.

## ⚠️ Language Code Differences

Android sometimes uses codes that differ from Weblate (e.g. Hebrew = `iw` in Android but `he` in Weblate).

Automation tools must map between systems.
See [LanguageCodeLoader.kt](https://github.com/thunderbird/thunderbird-android/blob/main/cli/translation-cli/src/main/kotlin/net/thunderbird/cli/translation/LanguageCodeLoader.kt#L12-L13) for an example.

You could find a more complete list of differences in the [Android documentation](https://developer.android.com/guide/topics/resources/localization#LocaleCodes) and [Unicode and internationalization support](https://developer.android.com/guide/topics/resources/internationalization)
