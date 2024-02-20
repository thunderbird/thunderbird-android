# Translation CLI

This is a command line interface that will check the [weblate](https://hosted.weblate.org/projects/tb-android/#languages) translation state for all languages and print out the ones that are above a certain threshold.

## Usage

To use this script you need to have a weblate token. You can get it by logging in to weblate and going to your profile settings.

You can run the script with the following command:

```bash
./scripts/translation --token <weblate-token> [--threshold 70]
```

It will print out the languages that are above the threshold. The default threshold is 70. You can change it by passing the `--threshold` argument.

If you want a code example, you can pass the `--print-all` argument. It will print out example code for easier integration into the project.

```bash
./scripts/translation --token <weblate-token> --print-all
```

You could use this output to update the `resourceConfigurations` variable in the `app-k9mail/build.gradle.kts` file and the `supported_languages` in the `arrays_general_settings_values.xml` file.
