#!/usr/bin/env python3

import re
import shutil
import subprocess
import sys

ours = sys.argv[1]
theirs = sys.argv[2]


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
    if is_k9:
        search_term = r"versionNameSuffix = \"a1\""
    else:
        search_term = r"versionNameSuffix = \"b[1-9]\""
    found_line = find_matching_line(theirs, search_term)
    if found_line:
        if "b1" not in found_line:
            new_line = "{}{}\n".format(found_line.split("=")[0], '= "b1"')
            replace_matching_line(ours, search_term, new_line)
    else:
        raise SystemExit(f"Search term '{search_term}' not found in theirs file.")
elif branch == "release":
    search_term = r"versionNameSuffix = \"b[1-9]\""
    found_line = find_matching_line(theirs, search_term)
    if found_line:
        replace_matching_line(ours, search_term, "")
    else:
        raise SystemExit(f"Search term '{search_term}' not found in theirs file.")
