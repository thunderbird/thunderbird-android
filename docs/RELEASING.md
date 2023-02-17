# Create K-9 Mail releases

## One-time setup

1. Download `tb-android keystore` from 1Password and place it somewhere outside the root of the Git repository.
2. Add the following to `~/.gradle/gradle.properties` (create the file if necessary)
   ```
   k9mail.storeFile=<path to keystore>
   k9mail.storePassword=<password 'tb-android keystore' in 1Password>
   k9mail.keyAlias=k9mail
   k9mail.keyPassword=<password 'k9mail@tb-android' in 1Password>
   ```

## Release a beta version

1. Update versionCode and versionName in `app/k9mail/build.gradle`
2. Create change log entries in
   - `app/ui/legacy/src/main/res/raw/changelog_master.xml`
   - `fastlane/metadata/android/en-US/changelogs/${versionCode}.txt`
     Use past tense. Try to keep them high level. Focus on the user (experience).
3. Commit the changes. Message: "Version $versionName"
4. Run `gradlew clean :app:k9mail:assembleRelease --no-build-cache`
5. Update an existing installation to make sure the app is signed with the proper key and runs on a real device.
   ```
   adb install -r app/k9mail/build/outputs/apk/release/k9mail-release.apk
   ```
6. Tag as $versionName, e.g. `6.508`
7. Copy `app/k9mail/build/outputs/apk/release/k9mail-release.apk` as `k9-${versionName}.apk` to Google Drive (MZLA Team > K9 > APKs)
8. Change versionName in `app/k9mail/build.gradle` to next version name followed by `-SNAPSHOT`
9. Commit the changes. Message: "Prepare for version $newVersionName"
10. Update `gh-pages` branch with the new change log
11. Push `main` branch
12. Push tags
13. Push `gh-pages` branch

### Create release on GitHub

1. Go to https://github.com/thundernest/k-9/tags and select the appropriate tag
2. Click "Create release from tag"
3. Fill out the form
   - Click "Generate release notes"
   - Replace contents under "What's changed" with change log entries
   - Add GitHub handles in parentheses to change log entries
   - If necessary, add another entry "Internal changes" (or similar) so people who contributed changes outside of the
     entries mentioned in the change log can be mentioned via GitHub handle.
   - Attach the APK
   - Select "Set as a pre-release"
   - Click "Publish release"

### Create release on F-Droid

TODO

### Create release on Google Play

TODO
