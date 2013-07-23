#!/usr/bin/env python
# Rewrite localized strings files to match the formatting of the master strings file in an Android project
#
# Written for use with K-9 Mail (https://github.com/k9mail/k-9)
# Licensed under the WTFPL (http://www.wtfpl.net/about/)

import sys
import re
from copy import deepcopy
from lxml import etree

INDENTATION = " " * 4


def new_tail(element):
    newline_count = element.tail.count("\n")
    if newline_count == 0:
        return element.tail
    else:
        return "%s%s" % ("\n" * newline_count, INDENTATION)


def remove_namespace_attribute(text):
    return re.sub(' xmlns:xliff="[^"]+"', "", text)


def fix_children_tail(element):
    for item in element.getchildren():
        if item.tail is not None and item.tail.count("\n") > 0:
            item.tail = "\n%s" % (INDENTATION * 2)
    element.getchildren()[-1].tail = "\n%s" % INDENTATION


if len(sys.argv) < 3:
    print "Usage: sync_translation.py <master> <translation>"
    print "Example: sync_translation.py res/values/strings.xml res/values-zh-rTW/strings.xml"
    exit(1)

master_file = sys.argv[1]
translation_file = sys.argv[2]

# Parse source files
parser = etree.XMLParser(strip_cdata=False)
master = etree.parse(master_file, parser=parser)
translation = etree.parse(translation_file, parser=parser)

# Create new XML object for the output
output = etree.Element("resources", nsmap={'xliff': "urn:oasis:names:tc:xliff:document:1.2"})
output.text = "\n%s" % INDENTATION

# Copy top level comments from translation
previous = output
elem = translation.getroot()
while elem.getprevious() is not None:
    elem = elem.getprevious()
    new_elem = deepcopy(elem)
    previous.addprevious(new_elem)
    previous = new_elem


for element in master.getroot().getchildren():

    if not isinstance(element.tag, basestring):
        # This is a comment; copy it to 'output'

        new_element = deepcopy(element)
        new_element.tail = new_tail(element)
        output.append(new_element)
    else:
        tag = element.tag
        name = element.get("name")

        if tag == "string":
            # Find string element in translation
            translated = translation.find('.//string[@name="%s"]' % name)

            if translated is None:
                # No translation found; copy original string and make it a comment

                temp_element = deepcopy(element)
                temp_element.tail = ""
                text = " NEW: %s" % etree.tostring(temp_element, encoding="UNICODE")
                text = remove_namespace_attribute(text)
                new_element = etree.Comment(text)
            else:
                # Translation found; use it

                new_element = deepcopy(translated)

            new_element.tail = new_tail(element)
            output.append(new_element)

        elif tag == "plurals":
            # Find plurals element in translation
            translated = translation.find('.//plurals[@name="%s"]' % name)

            if translated is None:
                # No translation found; copy original element and wrap it in a comment

                temp_element = deepcopy(element)
                temp_element.tail = ""

                fix_children_tail(temp_element)

                text = " NEW:\n%s%s\n%s" % (INDENTATION, etree.tostring(temp_element, encoding="UNICODE"), INDENTATION)
                text = remove_namespace_attribute(text)
                new_element = etree.Comment(text)
            else:
                # Translation found; use it

                new_element = deepcopy(translated)
                fix_children_tail(new_element)

            new_element.tail = new_tail(element)
            output.append(new_element)
        else:
            sys.stderr.write("Unknown element: %s\n" % tag)


output.getchildren()[-1].tail = "\n"

tree = etree.ElementTree(output)
tree.write(sys.argv[2], xml_declaration=True, encoding="UTF-8", pretty_print=True)
