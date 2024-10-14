#!/usr/bin/env python3

import argparse
import os
import requests
import yaml

from jinja2 import Template


def render_notes(version, versioncode, application, applicationid):
    """Update changelog files based on release notes from thunderbird-notes."""
    tb_notes_filename = f"{version}.yml"
    tb_notes_directory = "android_release"
    if "0b" in version:
        tb_notes_filename = f"{version[0:-1]}eta.yml"
        tb_notes_directory = "android_beta"
    tb_notes_url = os.path.join(
        "https://raw.githubusercontent.com/coreycb/thunderbird-notes/",
        "refs/heads/master/",
        tb_notes_directory,
        tb_notes_filename,
    )

    response = requests.get(tb_notes_url)
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
        render_data["releases"][vers]["changes"] = []
        for note in yaml_content["notes"]:
            if "0b" in version:
                if note["group"] == int(vers[-1]):
                    render_data["releases"][vers]["changes"].append(note["note"])
            else:
                render_data["releases"][vers]["changes"].append(note["note"])

    render_files = {
        "changelog_master": {
            "template": "./scripts/templates/changelog_master.xml",
            "outfile": f"./app-{application}/src/main/res/raw/changelog_master.xml",
            "render_data": render_data["releases"][version],
        },
        "changelog.txt": {
            "template": "./scripts/templates/changelog.txt",
            "outfile": f"./app-metadata/{applicationid}/en-US/changelogs/{versioncode}.txt",
            "render_data": render_data["releases"][version],
        },
    }

    for render_file in render_files:
        with open(render_files[render_file]["template"], "r") as file:
            template = file.read()
        template = Template(template)
        rendered = template.render(render_files[render_file]["render_data"])
        if render_file == "changelog_master":
            with open(render_files[render_file]["outfile"], "r") as file:
                lines = file.readlines()
                for index, line in enumerate(lines):
                    if "<changelog>" in line:
                        if version in lines[index + 1]:
                            break
                        lines.insert(index + 1, rendered)
                        break
            with open(render_files[render_file]["outfile"], "w") as file:
                file.writelines(lines)
        elif render_file == "changelog.txt":
            stripped = rendered.lstrip()
            with open(render_files[render_file]["outfile"], "w") as file:
                file.write(stripped)
            print(stripped)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("applicationid", type=str, choices=['net.thunderbird.android', 'net.thunderbird.android.beta', 'com.fsck.k9'], help="thunderbird or k9mail")
    parser.add_argument("version", type=str, help="Version name for this release")
    parser.add_argument("versioncode", type=str, help="Version code for this release")
    args = parser.parse_args()

    if args.applicationid == 'com.fsck.k9':
      application = "k9mail"
    else:
      application = "thunderbird"

    render_notes(args.version, args.versioncode, application, args.applicationid)


if __name__ == "__main__":
    main()
