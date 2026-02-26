# Scripts Directory

Various scripts for CI/CD, release automation, and development tasks.

## Python Scripts

### Setup

Install direct dependencies (hashed, no transitive deps):

```bash
python3 -m pip install --require-hashes --no-deps -r scripts/requirements.txt
```

### Available Scripts

**CI Scripts** (`ci/`)
- `render-notes.py` - Fetches and renders release notes from thunderbird-notes
- `setup_release_automation` - Sets up GitHub release automation environments
- `merges/merge_gradle.py` - Custom git merge driver for Gradle files

**CLI Wrappers**
- `autodiscovery`, `html-cleaner`, `resource-mover`, `translation` - Gradle CLI tool wrappers

## Virtual Environment

It's recommended to use a virtual environment:

```bash
python3 -m venv venv
source venv/bin/activate  # On macOS/Linux
python3 -m pip install --require-hashes --no-deps -r scripts/requirements.txt
```

To deactivate: `deactivate`

## Testing

To verify everything works:

```bash
./scripts/test_python_scripts.sh
```

This creates a temporary environment, installs dependencies, runs tests, and cleans up automatically.

## Dependencies

The `requirements.txt` includes **4 direct dependencies** (all pinned to specific versions):

- **PyNaCl** - Cryptography for GitHub secret encryption
- **PyYAML** - YAML parsing for release notes
- **Jinja2** - Template rendering for changelog files
- **requests** - HTTP client for GitHub API interactions

## Updating requirements.txt hashes

If `./scripts/test_python_scripts.sh` fails with a `--require-hashes` error, regenerate hashes using a temporary no-hash file:

```bash
cp scripts/requirements.txt /tmp/requirements-no-hash.txt
python3 - <<'PY'
import re, pathlib
path = pathlib.Path('/tmp/requirements-no-hash.txt')
text = path.read_text()
text = re.sub(r"\s+--hash=sha256:[a-f0-9]+", "", text)
path.write_text(text)
print("Wrote", path)
PY

mkdir -p /tmp/pip-hashes
python3 -m pip download --no-deps -r /tmp/requirements-no-hash.txt -d /tmp/pip-hashes --quiet
python3 -m pip hash /tmp/pip-hashes/* | sed 's/^.*--hash=/--hash=/'
rm -rf /tmp/pip-hashes
rm /tmp/requirements-no-hash.txt
```

Add the pinned versions and hashes from the output to `scripts/requirements.txt` (direct dependencies only).
