**Thank you for your contribution!**

### Prior to submitting a pull request, please familiarize yourself with...

- Our [Architecture docs](https://github.com/thunderbird/thunderbird-android/tree/main/docs/architecture)
  - Including the [Architecture Design Records](https://github.com/thunderbird/thunderbird-android/tree/main/docs/architecture/adr)
- Read [Mozilla’s Community Participation Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/)
- Check out our [contribution code quality guides](https://github.com/thunderbird/thunderbird-android/tree/main/docs/contributing), especially our [git commit guide](https://github.com/thunderbird/thunderbird-android/blob/main/docs/contributing/git-commit-guide.md), which can also help you write your pull request title

> [!IMPORTANT]
> Pull requests may take a few days to weeks to review. We’re a small team and prioritize contributions aligned with our [current roadmap](https://developer.thunderbird.net/planning/android-roadmap). 
> You can help us by categorizing your pull request with labels. 
> We appreciate you working with us and will get to reviewing your contribution as soon as we can!

### Please ensure that your pull request meets the following requirements - thanks!

- Does not contain merge commits. Rebase instead.
- Contains commits with descriptive titles.
- New code is written in Kotlin whenever possible.
- Follows our existing codestyle (`gradlew spotlessCheck` to check and `gradlew spotlessApply` to format your source code; will be checked by CI).
- Does not break any unit tests (`gradlew testDebugUnitTest`; will be checked by CI).
- Uses a descriptive title; don't put issue numbers in there.
- Contains a reference to the issue that it fixes (e.g. _Closes #XXX_ or _Fixes #XXX_) in the body text.
- For cosmetic changes add one or multiple images, if possible.
- Disclose if and how AI was used in the creation of this content


\* Finally, please replace this template text with a description of the change and additional context if necessary.
You may format it however you’d like. Consider including screenshots, descriptions of the problem it solves and how it goes about solving those issues, and any other information you consider relevant. 

As always, thank you for the contribution!
