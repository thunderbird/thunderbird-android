# Badging CLI

A small Kotlin CLI to generate normalized Android badging files using `aapt2 dump badging` on an APK derived from the AAB (bundletool universal APK), and to compare/update against the checked-in golden badging. The derived APK will be saved under build/outputs/apk_from_bundle/<flavor><BuildType>/ with a name that ends in `-badging.apk`.

Notes:
- bundletool is not part of the Android SDK. If you do not provide `--bundletool-path`, the CLI will automatically download `bundletool-all-1.18.1.jar` from the official GitHub releases and cache it at `~/.cache/thunderbird-cli/bundletool-all-1.18.1.jar`.

Usage examples:

- Generate and validate (default):

```bash
./scripts/badging --module app-thunderbird --flavor full --build-type release
```

- Update golden badging (skips validation for this run):

```bash
./scripts/badging --module app-thunderbird --flavor full --build-type release --update"
```

- Increase log verbosity:

```bash
./scripts/badging --module app-thunderbird --flavor full --build-type release --log-level=debug"
```

- Directly:

```bash
./gradlew :cli:badging-cli:run --args="--module app-thunderbird --flavor full --build-type release"
```

Options:
- --module=<text>                              Gradle module, e.g., app-thunderbird
- --flavor=(full|foss)                         Product flavor (e.g., full or foss)
- --build-type=(release|beta|daily|debug)      Build type (e.g release, beta, daily, debug)
- --aapt2path=<text>                           Optional path to aapt2; otherwise auto-detected from Android SDK
- --bundletool-path=<text>                     Path to bundletool (binary or .jar) used to extract APKs from AAB
- --sdk-root=<text>                            Override SDK root detection (ANDROID_HOME/ANDROID_SDK_ROOT)
- --build                                      If set, will run Gradle to build the apk
- --output-dir=<text>                          Output directory for golden badging (default: <module>/badging)
- --update                                     Overwrite the golden badging with current badging (disables validation for this run)
- --log-level=(verbose|debug|info|warn|error)  Log level (e.g. verbose, debug, info, warn, error) (default: info)
- -h, --help                                   Show this message and exit

Behavior:
- The current badging is saved to <module>/build/outputs/badging/<flavor><BuildType>-badging.txt
- Golden badging is saved in <module>/badging by default (or the directory given via --output-dir)
- If --update is not provided, the CLI validates the current badging against the golden file and prints a human-readable diff to stdout, with +/- lines colorized when colors are enabled.
- Exit code 0 when badging matches (or after successful update); non-zero when validation fails or required tools are missing.
