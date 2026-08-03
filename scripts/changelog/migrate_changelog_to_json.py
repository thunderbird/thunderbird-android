#!/usr/bin/env python3
# This script converts changelog_master.xml into JSON changelog resources.
# It extracts release information from the XML changelog, validates the
# generated JSON files against the project's schemas, and creates an index
# file for changelog lookups.
#
# Supported applications:
#   - com.fsck.k9
#   - net.thunderbird.android
#   - net.thunderbird.android.beta
#
# Usage:
#   python scripts\changelog\migrate_changelog_to_json.py <applicationid>
#
# Example:
#   python scripts\changelog\migrate_changelog_to_json.py net.thunderbird.android

import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET
import re

from jsonschema import Draft202012Validator


APP_CONFIG = {
    "com.fsck.k9": {
        "app_dir": "app-k9mail",
        "build_type": "main",
    },
    "net.thunderbird.android": {
        "app_dir": "app-thunderbird",
        "build_type": "release",
    },
    "net.thunderbird.android.beta": {
        "app_dir": "app-thunderbird",
        "build_type": "beta",
    },
}


def to_resource_name(version: str) -> str:
    normalized = re.sub(
        r"[^a-z0-9]+",
        "_",
        version.lower(),
    ).strip("_")

    return f"changelog_release_{normalized}"


def parse_changelog(
    xml_path: Path,
) -> ET.Element:
    tree = ET.parse(xml_path)
    return tree.getroot()


def load_schema(
    schema_path: Path,
) -> dict:
    with schema_path.open(
        "r",
        encoding="utf-8",
    ) as file:
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


def extract_release(
    release_element: ET.Element,
) -> dict:
    version = release_element.get(
        "version"
    )

    date = release_element.get(
        "date"
    )

    if not version:
        raise ValueError(
            "Release is missing "
            "'version' attribute"
        )

    if not date:
        raise ValueError(
            f"Release {version} "
            "is missing "
            "'date' attribute"
        )

    notes = []

    for change in release_element.findall(
        "change"
    ):
        text = (
            change.text or ""
        ).strip()

        if not text:
            continue

        notes.append(
            {
                "type":
                    determine_change_type(
                        text
                    ),
                "text": text,
                "source":
                    "ckchangelog-xml",
            }
        )

    release = {
        "schemaVersion": 1,
        "version": version,
        "date": date,
        "notes": notes,
    }

    return release


def write_release_file(
    release_data: dict,
    output_dir: Path,
) -> str:
    resource_name = (
        to_resource_name(
            release_data["version"]
        )
    )

    output_file = (
        output_dir /
        f"{resource_name}.json"
    )

    with output_file.open(
        "w",
        encoding="utf-8",
    ) as file:
        json.dump(
            release_data,
            file,
            indent=2,
            ensure_ascii=False,
        )

    return resource_name


def create_index_entry(
    release_data: dict,
    resource_name: str,
) -> dict:
    return {
        "version":
            release_data["version"],
        "date":
            release_data["date"],
        "resourceName":
            resource_name,
    }


def write_index_file(
    index: dict,
    output_dir: Path,
) -> None:
    output_file = (
        output_dir /
        "changelog_index.json"
    )

    with output_file.open(
        "w",
        encoding="utf-8",
    ) as file:
        json.dump(
            index,
            file,
            indent=2,
            ensure_ascii=False,
        )


def migrate_changelog(
    root: ET.Element,
    output_dir: Path,
    release_schema: dict,
) -> dict:
    index = {
        "schemaVersion": 1,
        "releases": [],
    }

    seen_versions = set()

    for release_element in root.findall(
        "release"
    ):
        release_data = extract_release(
            release_element
        )

        version = release_data[
            "version"
        ]

        if version in seen_versions:
            raise ValueError(
                f"Duplicate release "
                f"version: {version}"
            )

        seen_versions.add(version)

        validate_json(
            data=release_data,
            schema=release_schema,
            description=(
                f"Release "
                f"{version}"
            ),
        )

        resource_name = (
            write_release_file(
                release_data,
                output_dir,
            )
        )

        index["releases"].append(
            create_index_entry(
                release_data,
                resource_name,
            )
        )

    return index


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Convert "
            "changelog_master.xml "
            "to JSON changelog files"
        )
    )

    parser.add_argument(
        "applicationid",
        choices=list(
            APP_CONFIG.keys()
        ),
        help="Application ID",
    )

    args = parser.parse_args()

    config = APP_CONFIG.get(
        args.applicationid
    )

    if config is None:
        raise ValueError(
            "Unsupported "
            f"application ID: "
            f"{args.applicationid}"
        )

    app_dir = config["app_dir"]
    build_type = config[
        "build_type"
    ]

    repo_root = (
        Path(__file__)
        .resolve()
        .parents[2]
    )

    input_xml = (
        repo_root /
        app_dir /
        "src" /
        build_type /
        "res" /
        "raw" /
        "changelog_master.xml"
    )

    output_dir = (
        repo_root /
        app_dir /
        "src" /
        build_type /
        "res" /
        "raw" /
        "changelog"
    )

    schema_dir = (
        repo_root /
        "schemas"
    )

    release_schema_file = (
        schema_dir /
        "changelog-release.schema.json"
    )

    index_schema_file = (
        schema_dir /
        "changelog-index.schema.json"
    )

    if not input_xml.exists():
        raise FileNotFoundError(
            f"Input changelog file "
            f"not found: "
            f"{input_xml}"
        )

    if not release_schema_file.exists():
        raise FileNotFoundError(
            f"Release schema not found: "
            f"{release_schema_file}"
        )

    if not index_schema_file.exists():
        raise FileNotFoundError(
            f"Index schema not found: "
            f"{index_schema_file}"
        )

    output_dir.mkdir(
        parents=True,
        exist_ok=True,
    )

    release_schema = (
        load_schema(
            release_schema_file
        )
    )

    index_schema = (
        load_schema(
            index_schema_file
        )
    )

    root = parse_changelog(
        input_xml
    )

    index = migrate_changelog(
        root=root,
        output_dir=output_dir,
        release_schema=
        release_schema,
    )

    validate_json(
        data=index,
        schema=index_schema,
        description=
        "Changelog index",
    )

    write_index_file(
        index=index,
        output_dir=output_dir,
    )

    print(
        f"Generated "
        f"{len(index['releases'])} "
        f"release files and "
        f"validated all JSON "
        f"against schemas."
    )


if __name__ == "__main__":
    main()
