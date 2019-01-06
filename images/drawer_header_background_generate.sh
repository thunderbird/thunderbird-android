#!/bin/bash

function generate() {
    inkscape "drawer_header_background.svg" -export-area-page --without-gui -e "../app/ui/src/main/res/drawable-$1/drawer_header_background.png" -w $2 -h $3
}

generate mdpi     384 216
generate hdpi     576 324
generate xhdpi    768 432
generate xxhdpi  1152 648
#generate xxxhdpi 1728 972
