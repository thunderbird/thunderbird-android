# Releases

Thunderbird for Android follows a release train model to ensure timely and predictable releases. This model allows for regular feature rollouts, stability improvements, and bug fixes.

## Branches in the Release Train Model

### Daily

Daily builds are used for initial testing of new features and changes. Feature flags are used to work on features that are not yet ready for consumption.

- **Branch:** `main`
- **Purpose:** Active development of new features and improvements
- **Release Cadence:** Daily
- **Audience:** Developers and highly technical users who want to test the bleeding edge of Thunderbird. Daily builds are unstable and not recommended for production use.
- **Availability:** Daily builds are available on the Play Store internal channel. APKs are available on [ftp.mozilla.org](https://ftp.mozilla.org/pub/thunderbird-mobile/).

### Beta

After features are stabilized in Daily, they are merged into the Beta branch for broader testing. Uplifts are limited to high-impact bug fixes only. The Beta branch serves as a preview of what will be included in the next stable release, allowing for user feedback and final adjustments before general availability.

- **Branch:** `beta`
- **Purpose:** Pre-release testing
- **Release Cadence:** Weekly with the option to skip if not needed
- **Merge Cadence:** Every 4 weeks
- **Audience:** Early adopters and testers. Testers are encouraged to provide error logs and help reproduce issues filed.
- **Availability:** Beta builds are available from the [Play Store](https://play.google.com/store/apps/details?id=net.thunderbird.android.beta) and [F-Droid](https://f-droid.org/packages/net.thunderbird.android.beta).

### Release

This branch represents the stable version of Thunderbird. It is tested and suitable for general use. Uplifts are limited to high-impact bug fixes only.

- **Branch:** `release`
- **Purpose:** Stable releases
- **Release Cadence:** Major releases every 4 weeks. Minor release 2 weeks after a major release with the option to skip if not needed.
- **Merge Cadence:** Every 4 weeks
- **Audience:** General users. Users may be filing bug reports or leaving reviews to express their level of satisfaction.
- **Availability:** Release builds are available from the [Play Store](https://play.google.com/store/apps/details?id=net.thunderbird.android) and [F-Droid](https://f-droid.org/packages/net.thunderbird.android).

## Sample Release Timeline

|           Milestone           |  Details  |  Date  |
|-------------------------------|-----------|--------|
| TfA 14.0a1 starts             |           | Aug 28 |
| TfA 12.0                      |           | Sep 1  |
| TfA 13.0b1                    |           | Sep 1  |
| TfA 13.0bX                    | If needed | Sep 8  |
| TfA 12.1                      | If needed | Sep 15 |
| TfA 13.0bX                    | If needed | Sep 15 |
| TfA 14.0a1 soft freeze starts |           | Sep 18 |
| TfA 13.0bX                    | If needed | Sep 22 |
| TfA merge 13.0 beta->release  |           | Sep 22 |
| TfA merge 14.0 main->beta     |           | Sep 25 |
| TfA 15.0a1 starts             |           | Sep 25 |
| TfA 13.0                      |           | Sep 29 |
| TfA 14.0b1                    |           | Sep 29 |

## Soft Freeze

A week long soft freeze occurs for the `main` branch prior to merging into the `beta` branch. During this time:

- Risky code should not land
- Disabled feature flags should not be enabled

## Feature Flags

Thunderbird for Android uses Feature Flags to disable features not yet ready for consumption.

- On `main`, feature flags are enabled as soon as developers have completed all pull requests related to the feature.
- On `beta`, feature flags remain enabled unless the feature has not been fully completed and the developers would like to pause the feature.
- On `release`, feature flags are disabled until an explicit decision has been made to enable the feature for all users.

## Uplifts

Uplifts should be avoided if possible and fixes should ride the train. There are cases, however, where a bug is severe enough to warrant an uplift.
If the urgency of a fix requires it to uplifted to the Beta or Release channel before the next merge, the uplift process must be followed.

### Uplift Criteria

Uplifts to Beta and Release should:

- Be limited to stability, security, or high-impact fixes only. Features must ride the train.
- For Beta uplifts: Have landed in main, tested, and stabilized on the daily channel.
- For Release uplifts: Have landed in beta, tested, and stabilized on the beta channel.
- Have tests, or a strong statement of what can be done in the absence of tests.
- Not change any localizable strings.
- Have a comment in the GitHub issue assessing the reasons the patch is needed and risks involved in taking the patch.

Uplifts can include:
- Major crash fixes.
- High volume startup crash fixes.
- Security fixes.
- Dataloss fixes.
- Fixes for high-impact regressions with broad impact.
- Fixes for high-impact bugs in a major feature.

### Uplift Process

1. The requestor creates a pull request against the target uplift branch.
2. The requestor adds a comment to the pull request with the Approval Request template filled out.
3. The release driver reviews the uplift request, merging if approved, or closing with a comment if rejected.

## Versioning System

### Version Names

Thunderbird for Android stable release versions follow the `X.Y` format, where:

- **X (Major version):** Incremented for each new release cycle.
- **Y (Patch version):** Incremented when changes are added to an existing major version.

For beta builds, the suffix `b1` is appended, where the number increments for each beta. For daily builds, the suffix `a1` is appended, which remains constant.

### Version Codes

The version code is an internal version number for Android that helps determine whether one version is more recent than another.

The version code for beta and release is an integer value that increments for each new release.

The version code for daily is calculated based on the date and has the format `yDDDHHmm`:

- **y**: The number of years since a base year, with 2023 as the starting point (e.g., 2024 is 1)
- **DDD**: The day of the year in 3 digits, zero-padded
- **HH**: The hour of the day in 2 digits (00–23)
- **mm**: The minute of the hour in 2 digits

For example:

- `2024-02-09 16:45` → `1 | 040 | 16 | 45` → `10401645`
- `2025-10-12 09:23` → `2 | 285 | 09 | 23` → `22850923`
- `2122-02-09 16:45` → `99 | 040 | 16 | 45` → `990401645`

## Merge Days

Active development occurs on the `main` branch and becomes part of the daily build. Every 4 weeks:

1. `main` is merged into `beta`, for testing.
2. `beta` is merged into `release`, making it publicly available.

On the former, `main` carries over to `beta`, where the community can test the changes as part of “Thunderbird Beta for Testers” (`net.thunderbird.android.beta`) until the next merge day.
On the latter, code that was in beta goes to release, where the general population receives product updates (`net.thunderbird.android`).

When a merge occurs, the version name is carried forward to the next branch, and the alpha/beta suffixes are removed/reset accordingly. For example, let’s say we are shortly before the Thunderbird 9.0 release. The latest releases were Thunderbird 8.1, Thunderbird Beta 9.0b4, and Thunderbird Daily 10.0a1. Here is what happens:

- The `beta` branch is merged to `release`. The resulting version on release changes from 8.1 to 9.0.
- The `main` branch is merged to `beta`. The resulting version on beta changes from 9.0b4 to 10.0b1
- The `main` branch version number is changed from 10.0a1 to 11.0a1

While the version name changes, it must be ensured that the version code remains on the same sequence for each branch. For example:

- If the version code on the beta branch is 20 at 9.0b4, it will be 21 at 10.0b1.
- If the version code on the release branch is 12 at 8.1, it will be 13 at 9.0.

Our application IDs are specific to the branch they are on. For example:

- Beta always uses `net.thunderbird.android.beta` as the app ID for TfA.
- Release always uses `net.thunderbird.android` as the app ID for TfA.
- Release always uses `com.fsck.k9` as the app ID for K-9.

## Milestones

We use GitHub Milestones to track work for each major release. There is only one milestone for the whole major release, so work going into 9.0 and 9.1 would both be in the "Thunderbird 9" milestone. Each milestone has the due date set to the anticipated release date.

There are exactly three open milestones at any given time, some of our automation depends on this being the case. The milestone with the date furthest into the future is the target for the `main` branch, the one closest is the target for the `release` branch. When an uplift occurs, the milestone is changed to the respective next target.

Learn more on the [milestones page](https://github.com/thunderbird/thunderbird-android/milestones)

## Merge Process

The merge process enables various benefits, including:

- Carrying forward main branch history to beta, and beta branch history to release.
- No branch history is lost.
- Git tags are retained in the git log.
- Files/code that is unique per branch can remain that way (e.g. notes files such as changelog_master.xml, version codes).

The following steps are taken when merging main into beta:
1. Lock the main branch with the 'CLOSED TREE (main)' ruleset
2. Send a message to the #tb-mobile-dev:mozilla.org matrix channel to let them know:
- You will be performing the merge from main into beta
- The main branch is locked and cannot be changed during the merge
- You will let them know when the merge is complete and main is re-opened
3. Review merge results and ensure correctness
4. Ensure feature flags are following the rules
5. Push the merge
6. Submit a pull request that increments the version in main
7. Open a new milestone for the new version on github
8. Once the version increment is merged into main, unlock the branch
9. Send a message to the #tb-mobile-dev:mozilla.org channel to notify of merge completion and that main is re-opened

The following steps are taken when merging beta into release:
1. Send a message to the #tb-mobile-dev:mozilla.org matrix channel to let them know:
- You will be performing the merge from beta into release
- You will let them know when the merge is complete
2. Review merge results and ensure correctness
3. Ensure feature flags are following the rules
4. Push the merge
5. Close the milestone for the version that was previously in release
6. Send a message to the #tb-mobile-dev:mozilla.org channel to notify of merge completion

Merges are performed with the `do_merge.sh` script.

The following will merge main into beta:
`scripts/ci/merges/do_merge.sh beta`

And the following will merge beta into release:
`scripts/ci/merges/do_merge.sh release`

Be sure to review merge results and ensure correctness before pushing to the repository.

Files of particular importance are:

- app-k9mail/build.gradle.kts
- app-thunderbird/build.gradle.kts
- app-k9mail/src/main/res/raw/changelog_master.xml

These build.gradle.kts files must be handled as described in "Merge Days" section above. This is part of the do_merge.sh automation.
The app-k9mail/src/main/res/raw/changelog_master.xml should not include any beta notes in the release branch.

## Releases

Releases for both K-9 and Thunderbird for Android are automated with github actions.
Daily builds are scheduled with the [Daily Builds](https://github.com/thunderbird/thunderbird-android/actions/workflows/daily_builds.yml) action and all builds are performed by the [Shippable Build & Signing](https://github.com/thunderbird/thunderbird-android/actions/workflows/shippable_builds.yml) action.

For the historical manual release process, see [Releasing](HISTORICAL_RELEASE.md).

### Release Process

These are the general steps for a release:

1. Perform merge or uplifts. Each release is the result of either a merge or uplift.
2. Draft release notes at [thunderbird-notes](https://github.com/thunderbird/thunderbird-notes).
3. Trigger build via the [Shippable Build & Signing](https://github.com/thunderbird/thunderbird-android/actions/workflows/shippable_builds.yml) action.
4. Review the build results by reviewing the action summary and the git commits resulting from the build.
   - Make sure the version code is incremented properly and not wildly off
   - Ensure the commits are correct
   - Ensure the symlink `app-metadata` points to the right product at this commit
5. Test the build in the internal testing track
   - Release versions should be thoroughly tested with the test plan in Testrail
   - Beta versions only require a basic smoke test to ensure it installs
6. Promote TfA and K-9 releases to production track in Play Store.
   - Set rollout to a low rate (generally 10-30%).
   - Betas are only released for TfA. K-9 beta users are advised to use Thunderbird.
7. Wait for Play Store review to complete.
   - Release versions of TfA and K-9 have managed publishing enabled. Once the review has completed you need to publish the release
   - Beta versions of TfA do not have managed publishing enabled. It will be available once Google has reviewed, even on a weekend.
8. Update F-Droid to new TfA and K-9 releases by sending a pull request to [fdroiddata](https://gitlab.com/fdroid/fdroiddata)
9. Send community updates to Matrix channels, and beta or planning mailing lists as needed.
10. Approximately 24 hours after initial release to production, assess the following before updating rollout to a higher rate:
    - Crash rates, GitHub issues, install base, and reviews.

