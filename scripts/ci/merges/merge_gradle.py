#!/usr/bin/env python3

import re
import shutil
import subprocess
import sys

ours = sys.argv[1]
theirs = sys.argv[2]

BETA_SUFFIX = r"versionNameSuffix = \"b\d+\""
MAIN_SUFFIX = r"versionNameSuffix = \"a1\""


def get_current_branch():
    result = subprocess.run(
        ["git", "rev-parse", "--abbrev-ref", "HEAD"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if result.returncode:
        raise SystemExit(f"Git error: {result.stderr.strip()}")
    return result.stdout.strip()


def find_matching_line(file_path, search_term):
    """Finds and returns the first line containing search term in file."""
    with open(file_path, "r") as file:
        for line in file:
            if re.search(search_term, line):
                return line
    return None


def replace_matching_line(file_path, search_term, new_line):
    """Finds matching line in file and replaces it with new_line."""
    with open(file_path, "r") as file:
        lines = file.readlines()

    with open(file_path, "w") as file:
        for line in lines:
            if re.search(search_term, line):
                file.write(new_line)
            else:
                file.write(line)


def set_version_name_suffix(file_path, search_term, suffix):
    """Rewrites the versionNameSuffix line matching search_term.

    Sets the suffix to `suffix`, or drops the line entirely when `suffix` is
    None. Raises if the line is missing.
    """
    found_line = find_matching_line(file_path, search_term)
    if not found_line:
        raise SystemExit(f"Search term '{search_term}' not found in merge result.")
    if suffix is None:
        replace_matching_line(file_path, search_term, "")
        return
    if f'"{suffix}"' in found_line:
        return
    new_line = '{}= "{}"\n'.format(found_line.split("=")[0], suffix)
    replace_matching_line(file_path, search_term, new_line)


branch = get_current_branch()

search_term = "com.fsck.k9"
is_k9 = find_matching_line(ours, search_term)

search_term = "net.thunderbird.android"
is_thunderbird = find_matching_line(ours, search_term)

search_term = r"versionCode = "
found_line = find_matching_line(ours, search_term)

shutil.copyfile(theirs, ours)

if found_line:
    replace_matching_line(ours, search_term, found_line)
else:
    raise SystemExit(f"Search term '{search_term}' not found in ours file.")

if branch == "beta":
    # beta always starts at "b0"; the shippable build workflow bumps the suffix
    # per beta release. k9 has no beta build, so main carries its suffix in
    # defaultConfig as "a1" rather than "b0". The suffix is dropped on release
    # for both apps below.
    set_version_name_suffix(ours, MAIN_SUFFIX if is_k9 else BETA_SUFFIX, "b0")
elif branch == "release":
    # release ships without a suffix, so drop the line beta was carrying.
    set_version_name_suffix(ours, BETA_SUFFIX, suffix=None)
