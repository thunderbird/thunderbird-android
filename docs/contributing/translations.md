# 🌐 Translations

This document explains how you can help translate Thunderbird for Android into your language.

- All translations for Thunderbird for Android are managed in [Weblate project](https://hosted.weblate.org/projects/tb-android/).
- The source language is English (en).
- Translations happen only in Weblate, not in this repository.
- Weblate is synced regularly with the repository by the Thunderbird team to pull in translation updates.

> [!NOTE]
> If you are a developer and need to add or manage strings in the codebase, see [managing strings](managing-strings.md).

## 🚀 Getting started with Weblate

Familiarize yourself with Weblate by reading their [documentation](https://docs.weblate.org/en/latest/).

To start translating Thunderbird for Android:

1. Create a [Weblate account](https://hosted.weblate.org/accounts/signup/).
2. Go to the [Thunderbird for Android project](https://hosted.weblate.org/projects/tb-android/).
3. Select your language from the list of components.
4. Start translating strings through the Weblate's web interface.

## 🔑 Translation Rules

- **Translate only on Weblate** - never edit translation files directly in Git.
- **Follow string context:**
  - Placeholders like `%s` or `%1$d` must remain unchanged.
  - Xliff formatting (e.g. `<xliff:g id="...">...</xliff:g>`) must be preserved.
  - Keep punctuation consistent with the source.
- **Don’t change meaning** - if the English source string changes meaning, developers will create a new string key.

## 💡 Tips for Translators

- Use the "Comments" section in Weblate to ask questions or provide context.
- Regularly check for new strings to translate.
- Join the [Matrix channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org) to connect with other translators and developers.
- If your language is missing and you want to translate, please contact the Thunderbird team via the Matrix channel.

## 📦 Adding/Removing Languages

Languages included in app builds are decided by the development team based on **translation coverage**.

- Currently, a language must be at least **70% translated** before it is shipped.

If your language is below 70%, you can still contribute in Weblate - once coverage improves, it will be added to future builds.

## 🙏 Thank You!

Every translation improves Thunderbird for Android for users worldwide.
We greatly appreciate your help in making the app accessible in more languages!
