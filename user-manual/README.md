# User Manual Tooling

This directory contains scripts to automate creating screenshots for the [user manual](https://docs.k9mail.app/) whose
source can be found in the [k9mail-docs](https://github.com/k9mail/k9mail-docs) repository.

## Requirements

- [Bash](https://www.gnu.org/software/bash/)
- [Maestro](https://maestro.mobile.dev/)
- [ImageMagick](https://imagemagick.org/)'s `convert` command line tool

## Usage

1. Start an emulator with the following configuration:

   - Device: Pixel 2
   - Size: 1080x1920
   - Density: 420 dpi
   - API 33 "Android 13.0 (Google APIs)" - not "… (Google Play)"

2. Run `./build_images.sh`

   This will enable [System UI Demo Mode](https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/docs/demo_mode.md)
   on the emulator, run the Maestro flows `ui-flows/screenshots/user_manual*` to record screenshots, then run a
   post-processing step on the screenshots.

3. The final images can be found in the `output` directory. Copy as necessary to the
   [k9mail-docs](https://github.com/k9mail/k9mail-docs) repository.
