# 🌐 Translations

This document explains how you can help translate Thunderbird for Android into your language.

- All translations for Thunderbird for Android are managed in [Thunderbird for Android Weblate project](https://hosted.weblate.org/projects/tb-android/).
- The Source language is **English** (American English, represented as `en`).
- Translations are done only in Weblate, not in this repository.
- The Thunderbird team regularly syncs Weblate with the repository to pull in translation updates.

> [!NOTE]
> If you are a developer and need to add or manage strings or languages in the codebase, see [managing strings](managing-strings.md).

## 🚀 Getting started with Weblate

Before contributing, familiarize yourself with [documentation](https://docs.weblate.org/en/latest/).

To start translating Thunderbird for Android:

1. Create a [Weblate account](https://hosted.weblate.org/accounts/signup/).
2. Go to the [Thunderbird for Android Weblate project](https://hosted.weblate.org/projects/tb-android/).
3. Select your language from the list of languages.
4. Start translating strings through the Weblate web interface.

## 🔑 Translation Rules

- **Translate only on Weblate** - never edit translation directly in Git.
- **Preserve technical placeholders and formatting:**
  - Keep placeholders like `%1$s` or `%1$d` unchanged.
  - Match punctuation with the source string unless language rules require otherwise.
- **Don’t change meaning** - if the English source string changes meaning, developers will create a new string key.

## 💡 Tips for Translators

- Use the **Comments** section in Weblate to ask questions or provide context.
- Check regularly for new strings to translate.
- Join the [Matrix channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org) to connect with other translators and developers.

## 📦 Adding or Removing Languages

Languages included in app builds are decided by the development team based on **translation coverage**.

- A language must reach at least **70% coverage across all project components** before being shipped.
- If coverage falls below **60% for an extended period**, the language may be removed from builds.
- These thresholds are evaluated at each release cycle.

You can still contribute translations below the 70% threshold in Weblate. Once coverage improves, the language will be
added to future builds.

## ✅ Becoming a Reviewer

Reviewers help maintain translation quality by approving or correcting contributions.

To propose yourself as a reviewer:

1. Make consistent contributions for your language in Weblate.
2. Go to the [Matrix channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org)
3. Post a short message like:

   ```text
   Hi, I would like to become a reviewer for [Language Name].
   My Weblate username is [Your Username].  
   Here is a link to my contributions: [Your Weblate Profile or Component Link].
   ```
4. The Thunderbird team will review your request and grant reviewer rights for that language if appropriate.

## 🌍 Requesting a New Language

If your language is not available in Weblate yet, you can request it be added.

To propose a new language:

1. Go to the [Matrix channel](https://matrix.to/#/#tb-mobile-dev:mozilla.org)
2. Post a short message like:

   ```text
   Hi, I would like to request adding a new language to Thunderbird for Android.  
   Language: [Language Name]  
   Code: [e.g., fr, pt_BR]
   Any special notes: [Optional]
   ```
3. A team member will create the language in Weblate and confirm in the channel.
4. Once it’s available, you can start translating immediately.

Inclusion into the app follows our translation coverage policy, see [Adding or Removing Languages](#-adding-or-removing-languages).

## 🙏 Thank You!

Every translation improves Thunderbird for Android for users worldwide.
We greatly appreciate your help in making the app accessible in more languages!
