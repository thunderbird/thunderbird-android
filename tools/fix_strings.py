#!/usr/bin/env python
# Rewrite a strings file to get rid of line break/whitespace combinations that get stripped when building with Gradle.
#
# Example:
#   <string name="account_size_changed">
#   Account \"<xliff:g id="account">%s</xliff:g>\" shrunk from
#   <xliff:g id="oldSize">%s</xliff:g>
#   to
#   <xliff:g id="newSize">%s</xliff:g>
#   </string>
#
# will be rendered as
#
#   Account "account" shrunk from10MB to1MB
#
# when built with Gradle, but displays fine when built with Ant.
#
#
# Written for use with K-9 Mail (https://github.com/k9mail/k-9)
# Licensed under the WTFPL (http://www.wtfpl.net/about/)

import sys
import re
from lxml import etree


def fix_text(element):
    if element.text is not None:
        element.text = re.sub(r'^\n\s*([^\s])', "\\1", element.text)
        element.text = re.sub(r'\n\s*$', " ", element.text)


def fix_tail(element, is_last):
    if element.tail is not None:
        if is_last:
            replacement = ""
        else:
            replacement = " "
        element.tail = re.sub(r'^\n\s*([^\s])', " \\1", element.tail)
        element.tail = re.sub(r'\n\s*$', replacement, element.tail)


def cleanup_string_elements(elements):
    for element in elements:
        if element.tag is None:
            continue

        tag = element.tag
        children = element.getchildren()

        if tag in ["string", "item"]:
            if len(children) > 0:
                fix_text(element)

                for child in children:
                    if isinstance(child.tag, basestring):
                        fix_text(child)
                        fix_tail(child, child == children[-1])

        elif tag == "plurals":
            cleanup_string_elements(children)


if len(sys.argv) < 2:
    print "Usage: fix_strings.py <strings file>"
    print "Example: fix_strings.py res/values/strings.xml"
    exit(1)

strings_file = sys.argv[1]

parser = etree.XMLParser(strip_cdata=False)
strings = etree.parse(strings_file, parser=parser)

cleanup_string_elements(strings.getroot().getchildren())

strings.write(strings_file, xml_declaration=True, encoding="UTF-8", pretty_print=True)
