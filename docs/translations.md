# Managing translations

Right now we're using the `resourceConfigurations` mechanism provided by the Android Gradle Plugin to limit which
languages are included in builds of the app.
See e.g. https://github.com/thunderbird/thunderbird-android/blob/176a520e86bfe6875ad409a7565d122406dc7550/app-k9mail/build.gradle.kts#L40-L48

This list needs to be kept in sync with the string array `supported_languages`, so the in-app language picker offers
exactly the languages that are included in the app.

## Removing a language

1. Remove the language code from the `resourceConfigurations` list in `app-k9mail/build.gradle.kts`.
2. Remove the entry from `supported_languages` in `app/core/src/main/res/values/arrays_general_settings_values.xml`.

## Adding a language

1. Add the language code to the `resourceConfigurations` list in `app-k9mail/build.gradle.kts`.
2. Add an entry to `supported_languages` in `app/core/src/main/res/values/arrays_general_settings_values.xml`.
3. Make sure that `language_values` in `app/core/src/main/res/values/arrays_general_settings_values.xml` contains an
   entry for the language code you just added. If not:
   1. Add the language name (in its native script) to `language_entries` in
      `app/ui/legacy/src/main/res/values/arrays_general_settings_strings.xml`. Please note that this list should be
      ordered using the Unicode default collation order.
   2. Add the language code to `language_values` in `app/core/src/main/res/values/arrays_general_settings_values.xml`
      so that the index in the list matches that of the newly added entry in `language_entries`.

## Adding a component on Weblate

When adding a new code module that is including translatable strings, a new components needs to be added to Weblate.

1. Go the the Weblate page to [add a component](https://hosted.weblate.org/create/component/?project=3696).
2. Switch to the "From existing component" tab.
3. Enter a name for the component.
4. For "Component", select "K-9 Mail/Thunderbird/ui-legacy".
5. Press the "Continue" button.
6. Under "Choose translation files to import", select "Specify configuration manually".
7. Press the "Continue" button.
8. For "File format", select "Android String Resource".
9. Under "File mask", enter the path to the string resource files with a wildcard,
   e.g. `feature/account/common/src/main/res/values-*/strings.xml`.
10. Under "Monolingual base language file", enter the path to the string source file,
    e.g. `feature/account/common/src/main/res/values/strings.xml`.
11. Uncheck "Edit base file".
12. For "Translation license", select "Apache License 2.0".
13. Press the "Save" button.

## Things to note

For some languages Android uses different language codes than typical translation tools, e.g. Hebrew's code is _he_ on
Weblate, but _iw_ on Android. When writing automation tools, there needs to be a mapping step involved.

See [translation-cli](https://github.com/thunderbird/thunderbird-android/blob/ed07da8be5513ac74aabb1c934a4545aaae4f5a3/cli/translation-cli/src/main/kotlin/net/thunderbird/cli/translation/LanguageCodeLoader.kt#L12-L13)
for an example.
