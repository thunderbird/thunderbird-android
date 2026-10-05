#!/usr/bin/env python3

import argparse
import os
import re
import requests
import yaml
import sys

from jinja2 import Template

from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "changelog"))
from generate_changelog_json import (  # noqa: E402
    extract_release,
    load_existing_index,
    load_schema,
    to_resource_name,
    update_index,
    validate_json,
    write_index_file,
    write_release_file,
)


def render_notes(
    version,
    versioncode,
    application,
    applicationid,
    longform_file,
    print_only=False,
    overwrite=False,
    notesrepo="thunderbird/thunderbird-notes",
    notesbranch="master",
):
    """Render release notes from thunderbird-notes

    Update changelog files based on short release notes from thunderbird-notes.
    Render long-form notes to specified file,
    """
    tb_notes_filename = f"{version}.yml"
    tb_notes_directory = "android_release"
    if re.search(r"0b\d+$", version):
        tb_notes_filename = f"{re.sub(r'b\d+$', 'beta', version)}.yml"
        tb_notes_directory = "android_beta"

    if application == "k9mail":
        build_type = "release"
    else:
        if applicationid == "net.thunderbird.android":
            build_type = "release"
        elif applicationid == "net.thunderbird.android.beta":
            build_type = "beta"
        else:
            # // throw error
            print("Error: Unsupported applicationid")
            sys.exit(1)

    if os.path.isdir(os.path.expanduser(notesrepo)):
        notes_path = os.path.join(
            os.path.expanduser(notesrepo), tb_notes_directory, tb_notes_filename
        )
        with open(notes_path) as fp:
            yaml_content = yaml.safe_load(fp.read())
    else:
        tb_notes_url = (
            os.path.join(
                f"https://api.github.com/repos/{notesrepo}/",
                f"contents/{tb_notes_directory}/{tb_notes_filename}?ref={notesbranch}",
            )
        )

        headers = {
            "Accept": "application/vnd.github.v3.raw"
        }

        response = requests.get(tb_notes_url, headers=headers)
        response.raise_for_status()
        yaml_content = yaml.safe_load(response.text)

    render_data = {"releases": {}}
    for release in reversed(yaml_content["release"]["releases"]):
        vers = release["version"]
        render_data["releases"][vers] = {}
        render_data["releases"][vers]["version"] = vers
        render_data["releases"][vers]["versioncode"] = int(versioncode)
        render_data["releases"][vers]["application"] = application
        render_data["releases"][vers]["date"] = release["release_date"]
        render_data["releases"][vers]["short_notes"] = {}
        render_data["releases"][vers]["notes"] = {}
        render_data["releases"][vers]["long_notes"] = []
        for note in yaml_content["notes"]:
            if ("0b" not in version) or (
                "0b" in version and note["group"] == int(vers[-1])
            ):
                if (
                    note.get("thunderbird_only", False) and application == "k9mail"
                ) or (note.get("k9mail_only", False) and application == "thunderbird"):
                    continue
                if "note" in note:
                    tag = note["tag"].lower().capitalize()
                    if tag not in render_data["releases"][vers]["notes"]:
                        render_data["releases"][vers]["notes"][tag] = []
                    note_entry = {
                        "text": note["note"].strip(),
                        "issues": note.get("issues", []),
                    }
                    render_data["releases"][vers]["notes"][tag].append(note_entry)
                    render_data["releases"][vers]["long_notes"].append(note["note"].strip())
                if "short_note" in note:
                    tag = note["tag"].lower().capitalize()
                    if tag not in render_data["releases"][vers]["short_notes"]:
                        render_data["releases"][vers]["short_notes"][tag] = []
                    render_data["releases"][vers]["short_notes"][tag].append(
                        note["short_note"].strip()
                    )

    render_files = {
        "changelog": {
            "template": "changelog.txt",
            "outfile": f"./app-metadata/{applicationid}/en-US/changelogs/{versioncode}.txt",
            "render_data": render_data["releases"][version],
            "max_length": 500,
        },
        "changelog_long": {
            "template": "changelog_long.txt",
            "outfile": longform_file,
            "render_data": render_data["releases"][version],
            "autoescape": True,
        },
    }

    if not print_only and not overwrite:
        for config in render_files.values():
            if os.path.exists(config["outfile"]):
                print(
                    f"Error: Release notes already exist: {config['outfile']}. "
                    "Use --overwrite to replace them, or --print to preview without writing.",
                    file=sys.stderr,
                )
                return 1

    template_base = os.path.join(os.path.dirname(sys.argv[0]), "templates")

    for render_file, config in render_files.items():
        with open(os.path.join(template_base, config["template"]), "r") as file:
            template = file.read()
        template = Template(template, autoescape=config.get("autoescape", False))
        rendered = template.render(config["render_data"])
        if render_file == "changelog" or render_file == "changelog_long":
            stripped = rendered.lstrip()
            maxlen = config.get("max_length", float("inf"))
            if print_only:
                print(f"\n==={config['outfile']}===")
                print(stripped)

            if len(stripped) > maxlen:
                print(
                    f"Error: Maximum length of {maxlen} exceeded, {render_file} is {len(stripped)} characters"
                )
                sys.exit(1)

            if not print_only:
                try:
                    with open(config["outfile"], "w" if overwrite else "x") as file:
                        file.write(stripped)
                except FileExistsError:
                    print(
                        f"Error: Release notes already exist: {config['outfile']}. "
                        "Use --overwrite to replace them, or --print to preview without writing.",
                        file=sys.stderr,
                    )
                    return 1

    output_dir = Path(f"app-{application}/src/{build_type}/res/raw")
    schema_dir = Path(__file__).resolve().parents[2] / "schemas"
    release_schema = load_schema(schema_dir / "changelog-release.schema.json")
    index_schema = load_schema(schema_dir / "changelog-index.schema.json")
    index = load_existing_index(output_dir)
    releases = []
    for release_info in yaml_content["release"]["releases"]:
        release = extract_release(release_info["version"], int(versioncode), application, yaml_content)
        validate_json(release, release_schema, f"Release {release['version']}")
        index = update_index(index, release, to_resource_name(release["version"]))
        releases.append(release)
    validate_json(index, index_schema, "Changelog index")

    if print_only:
        for release in releases:
            print(f"\n==={output_dir / (to_resource_name(release['version']) + '.json')}===")
            print(release)
        print(f"\n==={output_dir / 'changelog_index.json'}===")
        print(index)
    else:
        output_dir.mkdir(parents=True, exist_ok=True)
        for release in releases:
            write_release_file(release, output_dir)
        write_index_file(index, output_dir)

    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--print",
        "-p",
        action="store_true",
        help="Only print the processed release notes",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace existing store and long-form note files instead of failing",
    )
    parser.add_argument(
        "--repository",
        "-r",
        default="thunderbird/thunderbird-notes",
        help="Repository or directory to retrieve thunderbird-notes from",
    )
    parser.add_argument(
        "--branch",
        "-b",
        default="master",
        help="Branch to retrieve thunderbird-notes from",
    )
    parser.add_argument(
        "applicationid",
        type=str,
        choices=[
            "net.thunderbird.android",
            "net.thunderbird.android.beta",
            "com.fsck.k9",
        ],
        help="thunderbird or k9mail",
    )
    parser.add_argument("version", type=str, help="Version name for this release")
    parser.add_argument(
        "versioncode",
        nargs="?",
        default="0",
        type=str,
        help="Version code for this release",
    )
    parser.add_argument(
        "longform_file",
        type=str,
        nargs="?",
        default="github_notes",
        help="File to render long-form notes to",
    )
    args = parser.parse_args()

    if args.applicationid == "com.fsck.k9":
        application = "k9mail"
    else:
        application = "thunderbird"

    return render_notes(
        args.version,
        args.versioncode,
        application,
        args.applicationid,
        args.longform_file,
        print_only=args.print,
        overwrite=args.overwrite,
        notesrepo=args.repository,
        notesbranch=args.branch,
    )


if __name__ == "__main__":
    sys.exit(main())
