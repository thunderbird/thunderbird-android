# Scripts Directory

Various scripts for CI/CD, release automation, and development tasks.

## Python Scripts

### Setup

Install dependencies (all pinned and CVE-free):

```bash
pip install -r requirements.txt
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
pip install -r requirements.txt
```

To deactivate: `deactivate`

## Testing

To verify everything works:

```bash
./test_python_scripts.sh
```

This creates a temporary environment, installs dependencies, runs tests, and cleans up automatically.

## Dependencies

The `requirements.txt` includes **4 direct dependencies** (all pinned to specific versions):

- **PyNaCl** - Cryptography for GitHub secret encryption
- **PyYAML** - YAML parsing for release notes
- **Jinja2** - Template rendering for changelog files
- **requests** - HTTP client for GitHub API interactions
