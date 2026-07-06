#!/usr/bin/env python3
# Converts Thunderbird/K-9 Mail release notes into JSON changelog resources.
#
# The script:
# - Loads release notes from https://github.com/thunderbird/thunderbird-notes.
# - Extracts changelog data for a specific release version.
# - Generates version-specific JSON changelog files.
# - Updates the changelog index.
# - Validates all generated files against the project's JSON schemas.
#
# Supported application IDs:
#   - com.fsck.k9
#   - net.thunderbird.android
#   - net.thunderbird.android.beta
#
# Usage:
#   python scripts/changelog/migrate_changelog_to_json.py <applicationid> <version>
#
# Example:
#   python scripts/changelog/migrate_changelog_to_json.py net.thunderbird.android 11.0

import argparse
import json
import re
from pathlib import Path

import requests
import yaml
from jsonschema import Draft202012Validator

APP_CONFIG = {
    "com.fsck.k9": {"app_dir": "app-k9mail", "build_type": "release"},
    "net.thunderbird.android": {"app_dir": "app-thunderbird", "build_type": "release"},
    "net.thunderbird.android.beta": {"app_dir": "app-thunderbird", "build_type": "beta"},
}

def to_resource_name(version: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", "_", version.lower()).strip("_")
    return f"changelog_release_{normalized}"

def load_schema(schema_path: Path) -> dict:
    with schema_path.open("r", encoding="utf-8") as file:
        return json.load(file)

def validate_json(
    data: dict,
    schema: dict,
    description: str,
) -> None:
    validator = Draft202012Validator(
        schema
    )

    errors = sorted(
        validator.iter_errors(data),
        key=lambda error: list(
            error.absolute_path
        ),
    )

    if not errors:
        return

    messages = []

    for error in errors:
        location = (
            "/".join(
                str(part)
                for part in error.absolute_path
            )
            or "<root>"
        )

        messages.append(
            f"{location}: "
            f"{error.message}"
        )

    raise ValueError(
        f"{description} failed schema "
        f"validation:\n"
        + "\n".join(messages)
    )

def load_release_notes(version: str, notesrepo: str, notesbranch: str) -> dict:
    filename = f"{version}.yml"
    directory = "android_release"
    if "0b" in version:
        filename = f"{version[:-1]}eta.yml"
        directory = "android_beta"

    repo_path = Path(notesrepo).expanduser()
    if repo_path.is_dir():
        yaml_file = repo_path / directory / filename
        with yaml_file.open("r", encoding="utf-8") as f:
            return yaml.safe_load(f)

    url = (
        f"https://api.github.com/repos/{notesrepo}/contents/"
        f"{directory}/{filename}?ref={notesbranch}"
    )
    response = requests.get(
        url,
        headers={"Accept": "application/vnd.github.v3.raw"},
        timeout=30,
    )
    response.raise_for_status()
    return yaml.safe_load(response.text)

def extract_release(version: str, application: str, yaml_content: dict) -> dict:
    release_info = next(
        (r for r in yaml_content["release"]["releases"] if r["version"] == version),
        None,
    )
    if release_info is None:
        raise ValueError(f"Version '{version}' not found")

    beta_group = None
    match = re.search(r"0b(\d+)$", version)
    if match:
        beta_group = int(match.group(1))

    notes = []
    for note in yaml_content["notes"]:
        if beta_group is not None and note.get("group") != beta_group:
            continue
        if note.get("thunderbird_only", False) and application == "k9mail":
            continue
        if note.get("k9mail_only", False) and application == "thunderbird":
            continue

        text = note.get("note", "").strip()
        if not text:
            continue

        change_type = determine_change_type(note.get("tag", "changed").lower())
        entry = {
            "type": change_type,
            "text": text,
            "source": "thunderbird-notes",
        }
        if "issues" in note:
            entry["issues"] = note["issues"]
        notes.append(entry)

    return {
        "schemaVersion": 1,
        "version": version,
        "date": release_info["release_date"],
        "notes": notes,
    }

def determine_change_type(
    text: str,
) -> str:
    normalized = text.lower()

    if normalized.startswith(
        "fixed"
    ):
        return "fixed"

    if (
        normalized.startswith(
            "added"
        )
        or normalized.startswith(
        "new"
    )
        or normalized.startswith(
        "support"
    )
    ):
        return "new"

    return "changed"

def write_release_file(release_data: dict, output_dir: Path) -> str:
    resource_name = to_resource_name(release_data["version"])
    output_file = output_dir / f"{resource_name}.json"
    with output_file.open("w", encoding="utf-8") as f:
        json.dump(release_data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    return resource_name

def create_index_entry(release_data: dict, resource_name: str) -> dict:
    return {
        "version": release_data["version"],
        "date": release_data["date"],
        "resourceName": resource_name,
    }

def load_existing_index(output_dir: Path) -> dict:
    index_file = output_dir / "changelog_index.json"
    if not index_file.exists():
        return {"schemaVersion": 1, "releases": []}
    with index_file.open("r", encoding="utf-8") as f:
        return json.load(f)

def update_index(index: dict, release_data: dict, resource_name: str) -> dict:
    releases = [
        r for r in index["releases"]
        if r["version"] != release_data["version"]
    ]
    releases.append(create_index_entry(release_data, resource_name))
    releases.sort(key=lambda r: r["date"], reverse=True)
    index["releases"] = releases
    return index

def write_index_file(index: dict, output_dir: Path) -> None:
    with (output_dir / "changelog_index.json").open("w", encoding="utf-8") as f:
        json.dump(index, f, indent=2, ensure_ascii=False)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "applicationid",
        choices=[
            "net.thunderbird.android",
            "net.thunderbird.android.beta",
            "com.fsck.k9",
        ],
    )
    parser.add_argument("version")
    parser.add_argument("versioncode", nargs="?", default="0")
    parser.add_argument(
        "--repository", "-r",
        default="thunderbird/thunderbird-notes",
    )
    parser.add_argument("--branch", "-b", default="master")
    args = parser.parse_args()

    config = APP_CONFIG[args.applicationid]
    application = "k9mail" if args.applicationid == "com.fsck.k9" else "thunderbird"

    repo_root = Path(__file__).resolve().parents[2]

    output_dir = (
        repo_root / config["app_dir"] / "src" / config["build_type"]
        / "res" / "raw"
    )

    schema_dir = repo_root / "schemas"
    release_schema = load_schema(schema_dir / "changelog-release.schema.json")
    index_schema = load_schema(schema_dir / "changelog-index.schema.json")

    output_dir.mkdir(parents=True, exist_ok=True)

    yaml_content = load_release_notes(
        args.version,
        args.repository,
        args.branch,
    )

    release_data = extract_release(
        args.version,
        application,
        yaml_content,
    )
    print(release_data)

    validate_json(
        release_data,
        release_schema,
        f"Release {args.version}",
    )

    resource_name = write_release_file(release_data, output_dir)

    index = load_existing_index(output_dir)
    index = update_index(index, release_data, resource_name)

    validate_json(index, index_schema, "Changelog index")

    write_index_file(index, output_dir)

    print(
        f"Generated {resource_name}.json and updated changelog_index.json"
    )

if __name__ == "__main__":
    main()
