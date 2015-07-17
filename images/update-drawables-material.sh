#!/bin/bash

# Clone this repository: https://github.com/sebkur/android-res-utils
# and put the directory android-res-utils on your path, so that the
# python script can be found, e.g. execute:
#
# export PATH=$PATH:~/android-res-utils

DIR=$(dirname $0)
CONVERT="pngs_from_svg.py"
SVGS="$DIR/drawables-material"
OUTPUT="$DIR/../k9mail/src/main/res"
SIZE=24

function convert {
	NAME=$1
	SUFFIX1=$2
	SUFFIX2=$3
	IN="$SVGS/$NAME"
	#                         size (dp)             color     opacity
	$CONVERT "$IN" "$OUTPUT" "$SIZE" -s "$SUFFIX1" -c "#000" -o "1.0"
	$CONVERT "$IN" "$OUTPUT" "$SIZE" -s "$SUFFIX2" -c "#fff" -o "1.0"
}

# Cope with the inconsistent naming of the drawables. Eventually fix the
# drawable names instead of using this workaround. Four functions for
# four different suffixes currently used.
function convert1 {
	convert $1 "_material_light" "_material_dark"
}

function convert2 {
	convert $1 "_light_material" "_dark_material"
}

function convert3 {
	convert $1 "_black_material" "_white_material"
}

function convert4 {
	convert $1 "_light" "_dark"
}

for file in \
"ic_action_archive.svg" \
"ic_action_search.svg" \
"ic_action_refresh.svg" \
"ic_action_copy.svg" \
"ic_action_delete.svg" \
"ic_action_add_cc_bcc.svg" \
; do
	convert1 "$file"
done

convert2 "folder.svg"
convert3 "ic_attach_file.svg"

for file in \
"action_search_folder.svg" \
"ic_action_compose.svg" \
"ic_action_send.svg" \
"ic_action_settings.svg" \
"ic_button_add_contact.svg" \
"ic_action_mark_as_read.svg" \
"ic_action_mark_as_unread.svg" \
; do
	convert4 "$file"
done
