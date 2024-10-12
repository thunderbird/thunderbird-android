#!/usr/bin/env python3

import argparse
import os
import requests
import yaml

from jinja2 import Template


def build_script_updated(filename, version, application):
    """Check if build.gradle.kts file has been updated for this version."""
    with open(filename, "r") as file:
        content = file.read()
        if application == "thunderbird":
            if f"versionNameSuffix = \"b{version.split('b')[1]}\"" in content:
                return True
        else:
            if f'versionName = "{version}"' in content:
                return True
    return False


def new_versioncode(filename, version, application):
    """Get the new versioncode based on incrementing the previous one."""
    with open(filename, "r") as file:
        lines = file.readlines()
    for index, line in enumerate(lines):
        if "versionCode = " in line:
            current_versioncode = int(line.split(" = ")[1])
            new_versioncode = current_versioncode + 1
    if build_script_updated(filename, version, application):
        return current_versioncode
    return new_versioncode


def update_build_script(
    filename, version, versioncode, application, applicationid
):
    """Update build.gradle.kts files with new versions."""
    if build_script_updated(filename, version, application):
        return
    with open(filename, "r") as file:
        lines = file.readlines()
    for index, line in enumerate(lines):
        if "versionCode = " in line:
            lines[index] = f"{line.split(' = ')[0]} = {versioncode}\n"
        if "versionName = " in line:
            if application == "thunderbird":
                lines[index] = f"{line.split(' = ')[0]} = \"{version.split('b')[0]}\"\n"
            else:
                lines[index] = f"{line.split(' = ')[0]} = \"{version}\"\n"
        if "0b" in version:
            if 'versionNameSuffix = "b' in line:
                lines[index] = (
                    f"{line.split(' = ')[0]} = \"b{version.split('0b')[1]}\"\n"
                )
    with open(filename, "w") as file:
        file.writelines(lines)


def render_notes(version, versioncode, application, applicationid):
    """Update changelog files based on release notes from thunderbird-notes."""
    tb_notes_filename = f"{version}.yml"
    if "0b" in version:
        tb_notes_filename = f"{version[0:-1]}eta.yml"
    tb_notes_url = os.path.join(
        "https://raw.githubusercontent.com/coreycb/thunderbird-notes/"
        "refs/heads/master/android_beta/",
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
            if note["group"] == int(vers[-1]):
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
            if not os.path.exists(render_files[render_file]["outfile"]):
                with open(render_files[render_file]["outfile"], "w") as file:
                    file.write(rendered.lstrip())


def symlink_metadata(applicationid):
    """Set up the metadata symlink depending on the application."""
    if os.path.islink("./metadata"):
        os.remove("./metadata")
        os.symlink(f"app-metadata/{applicationid}", "./metadata")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("version", type=str, help="Version for this release")
    parser.add_argument("application", type=str, help="thunderbird or k9mail")
    args = parser.parse_args()

    applicationid = "com.fsck.k9"
    if args.application == "thunderbird":
        applicationid = f"net.{args.application}.android"
        if "0b" in args.version:
            applicationid = f"net.{args.application}.android.beta"

    filename = f"./app-{args.application}/build.gradle.kts"

    versioncode = new_versioncode(filename, args.version, args.application)
    update_build_script(
        filename, args.version, versioncode, args.application, applicationid
    )
    render_notes(args.version, versioncode, args.application, applicationid)
    symlink_metadata(applicationid)


if __name__ == "__main__":
    main()
