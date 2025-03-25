# Thunderbird for Android

<a href="https://play.google.com/store/apps/details?id=net.thunderbird.android&referrer=utm_campaign%3Dandroid_metadata%26utm_medium%3Dweb%26utm_source%3Dgithub.com%26utm_content%3Dbadge" target="_blank"><img src="./docs/assets/get-it-on-play.png" alt="Get it on Google Play" height="28"></a>
<a href="https://f-droid.org/packages/net.thunderbird.android"><img src="./docs/assets/get-it-on-fdroid.png" alt="Get it on F-Droid" height="28"></a>
[![Latest release](https://img.shields.io/github/release/thunderbird/thunderbird-android.svg?style=for-the-badge&filter=THUNDERBIRD_*&logo=thunderbird)](https://github.com/thunderbird/thunderbird-android/releases/latest)
[![Latest beta release](https://img.shields.io/github/release/thunderbird/thunderbird-android.svg?include_prereleases&style=for-the-badge&label=beta&filter=THUNDERBIRD_*b*&logo=thunderbird)](https://github.com/thunderbird/thunderbird-android/releases)

Thunderbird for Android is a powerful, privacy-focused email app. Effortlessly manage multiple email accounts from one app, with a Unified Inbox option for maximum productivity. Built on open-source technology and supported by a dedicated team of developers alongside a global community of volunteers, Thunderbird never treats your private data as a product.

Thunderbird for Android is based on K-9 Mail, which comes with a rich history of success and functionality in open source email.

## Download

Thunderbird for Android can be downloaded from a couple of sources:

- Thunderbird on [Google Play](https://play.google.com/store/apps/details?id=net.thunderbird.android&referrer=utm_campaign%3Dandroid_metadata%26utm_medium%3Dweb%26utm_source%3Dgithub.com%26utm_content%3Dlink) or [F-Droid](https://f-droid.org/packages/net.thunderbird.android)
- Thunderbird Beta on [Google Play](https://play.google.com/store/apps/details?id=net.thunderbird.android.beta&referrer=utm_campaign%3Dandroid_metadata%26utm_medium%3Dweb%26utm_source%3Dgithub.com%26utm_content%3Dlink) or [F-Droid](https://f-droid.org/packages/net.thunderbird.android.beta)
- [Github Releases](https://github.com/thunderbird/thunderbird-android/releases)
- [FFUpdater](https://f-droid.org/packages/de.marmaro.krt.ffupdater/) allows installing the latest versions from ftp.mozilla.org

By using Thunderbird for Android Beta, you have early access to current development and are able to try new features earlier.

Check out the [Release Notes](https://github.com/thunderbird/thunderbird-android/releases) to find out what changed in each version of Thunderbird for Android.

## Need Help? Found a bug? Have an idea? Want to chat?

If the app is not behaving like it should, or you are not sure if you've encountered a bug:

- Check out our [knowledge base](https://support.mozilla.org/products/thunderbird-android) and [frequently asked questions](https://support.mozilla.org/kb/thunderbird-android-8-faq)
- Ask a question on our [support forum](https://support.mozilla.org/en-US/questions/new/thunderbird-android)

If you are certain you've identified a bug in Thunderbird for Android and would like to help fix it:

- File an issue on [our GitHub issue tracker](https://github.com/thunderbird/thunderbird-android/issues)

If you have an idea how to improve Thunderbird for Android:

- Tell us about and vote on your feature ideas on [connect.mozilla.org](https://connect.mozilla.org/t5/ideas/idb-p/ideas/label-name/thunderbird%20android).
- Join the discussion about the latest changes in the [Thunderbird Android Beta Topicbox](https://thunderbird.topicbox.com/groups/android-beta).

The Thunderbird Community uses Matrix to communicate:

- General chat about Thunderbird for Android and K-9 Mail: [#tb-android:mozilla.org](https://matrix.to/#/#tb-android:mozilla.org)
- Development and other ways to contribute: [#tb-android-dev:mozilla.org](https://matrix.to/#/#tb-android-dev:mozilla.org)
- Reach the broader Thunderbird Community in the [community space](https://matrix.to/#/#thunderbird-community:mozilla.org)

## Contributing

We welcome contributions from everyone.

- Development: Have you done a little bit of Kotlin? The [CONTRIBUTING](docs/CONTRIBUTING.md) guide will help you get started
- Translations: Do you speak a language aside from English? [Translating is easy](https://hosted.weblate.org/projects/tb-android/) and just takes a few minutes for your first success.
- We have [a number of other contribution opportunities](https://blog.thunderbird.net/2024/09/contribute-to-thunderbird-for-android/) available.
- Thunderbird is supported solely by financial contributions from users like you. [Make a financial contribution today](https://www.thunderbird.net/donate/mobile/?form=tfa)!
- Make sure to check out the [Mozilla Community Participation Guidelines](https://www.mozilla.org/about/governance/policies/participation/).

### Architecture Decision Records (ADR)

We use [Architecture Decision Records](https://adr.github.io/) to document the architectural decisions made in the
development of Thunderbird for Android. You can find them in the [`docs/architecture/adr`](docs/architecture/adr) directory.

For more information about our ADRs, please see the [ADRs README](docs/architecture/adr/README.md).

We encourage team members and contributors to read through our ADRs to understand the architectural decisions that
have shaped this project so far. Feel free to propose new ADRs or suggest modifications to existing ones as needed.

## K-9 Mail

In June 2022, [K-9 Mail joined the Thunderbird family](https://k9mail.app/2022/06/13/K-9-Mail-and-Thunderbird.html)
as the foundation for Thunderbird on Android. Since then, we’ve been updating both apps to give
users the same solid experience, so it’s normal to notice that K-9 Mail and Thunderbird look and
feel nearly identical. They’re built on the same code, and that’s intentional. You'll notice some
features are selectively enabled for Thunderbird as opposed to K-9 Mail, usually when they are
simply a better fit for Thunderbird (like the import from K-9 functionality).

If you prefer the robot dog and would like to keep K-9 Mail around, you can find it here:

- [K-9 Mail on Google Play](https://play.google.com/store/apps/details?id=com.fsck.k9&utm_source=thunderbird-android-github&utm_campaign=download-section)
- [K-9 Mail on F-Droid](https://f-droid.org/packages/com.fsck.k9/)

## Forking

If you want to use a fork of this project please ensure that you replace the OAuth client setup in the `app-k9mail/src/{debug,release}/kotlin/app/k9mail/auth/K9OAuthConfigurationFactory.kt` and `app-thunderbird/src/{debug,daily,beta,release}/kotlin/net/thunderbird/android/auth/TbOAuthConfigurationFactory.kt` with your own OAuth client setup and ensure that the `redirectUri` is different to the one used in the main project. This is to prevent conflicts with the main app when both are installed on the same device.

## License

Thunderbird for Android is licensed under the [Apache License, Version 2.0](LICENSE).
