#!/bin/bash

APP_DIR=../k9mail/src/main
MDPI_DIR=$APP_DIR/res/drawable-mdpi
HDPI_DIR=$APP_DIR/res/drawable-hdpi
XDPI_DIR=$APP_DIR/res/drawable-xhdpi
XXDPI_DIR=$APP_DIR/res/drawable-xxhdpi
XXXDPI_DIR=$APP_DIR/res/drawable-xxxhdpi
SRC_DIR=./drawables-pgp/


for NAME in "status_lock_closed" "status_lock_error" "status_lock_open" "status_signature_expired_cutout" "status_signature_invalid_cutout" "status_signature_revoked_cutout" "status_signature_unknown_cutout" "status_signature_unverified_cutout" "status_signature_verified_cutout"
do
echo $NAME
inkscape -w 24 -h 24 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 32 -h 32 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 48 -h 48 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 64 -h 64 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done
