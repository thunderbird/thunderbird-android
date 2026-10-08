# Weblate CLI

This is a command line interface that inspects Weblate project components and applies a
"default" component configuration. It's intended for maintainers to review component configuration
consistency and, when appropriate, patch components to match the component config.

## Usage

You need a Weblate API token (available from your Weblate account profile). A convenience wrapper script
is provided at `./scripts/weblate` which builds and runs the CLI.

The CLI uses a subcommand pattern: `weblate [OPTIONS] COMMAND [ARGS]...`

Available commands:
- `update`: Update managed components with the standard configuration.
- `create`: Create missing components on Weblate based on local modules.
- `delete`: Delete a component from Weblate.

Basic examples:

```bash
# Dry-run using the default configuration and managed components file
./scripts/weblate --token YOUR_WEBLATE_TOKEN --dry-run update

# Apply changes to managed components
./scripts/weblate --token YOUR_WEBLATE_TOKEN update

# Create missing components
./scripts/weblate --token YOUR_WEBLATE_TOKEN create

# Delete a component by slug
./scripts/weblate --token YOUR_WEBLATE_TOKEN delete --slug component-slug-to-delete

# Use a custom managed components file, component config, and set log level to ALL
./scripts/weblate --token YOUR_WEBLATE_TOKEN --managed-components-file ./cli/weblate-cli/managed-components.txt --component-config-file ./cli/weblate-cli/default-component-config.json --log-level ALL --dry-run update
```

## Available options

- `--token`: Weblate API token (required).
- `--component-config-file`: Path to component config JSON (default: `./cli/weblate-cli/default-component-config.json`).
- `--managed-components-file`: Path to file with managed component slugs (default: `./cli/weblate-cli/managed-components.txt`).
- `--dry-run`: Dry run the command without making any changes.
- `--log-level`: Log level for the Weblate API client (`NONE`, `INFO`, `HEADERS`, `BODY`, `ALL`). Default: `NONE`.

## Defaults

- Component config: `./cli/weblate-cli/default-component-config.json`
- Managed components file: `./cli/weblate-cli/managed-components.txt`

## Managed components file format

- One component slug per non-empty line. Inline comments are allowed after `#` and full-line comments that
  start with `#` are ignored.
- Matching is exact and case-sensitive against the component slug returned by the Weblate API.

Example:

```
# legacy
app-strings # ID: 17093 (main)
designsystem # ID: 25913
app-common
```

## Safety notes

- Always run with `--dry-run` first to verify diffs before applying changes to the live Weblate instance.
- The `update` command only processes components listed in the managed components file.
- Use `./scripts/weblate --help` to see all available commands and options.

