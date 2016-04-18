#!/bin/bash

APP_DIR=../k9mail/src/main
MDPI_DIR=$APP_DIR/res/drawable-mdpi
HDPI_DIR=$APP_DIR/res/drawable-hdpi
XDPI_DIR=$APP_DIR/res/drawable-xhdpi
XXDPI_DIR=$APP_DIR/res/drawable-xxhdpi
XXXDPI_DIR=$APP_DIR/res/drawable-xxxhdpi
SRC_DIR=./drawables-pgp/


for NAME in "bullet_point_positive" "bullet_point_negative" "compatibility" "status_lock" "status_lock_closed" "status_lock_error" "status_lock_open" "status_lock_disabled" "status_lock_opportunistic" "status_signature_expired_cutout" "status_signature_invalid_cutout" "status_signature_revoked_cutout" "status_signature_unknown_cutout" "status_signature_unverified_cutout" "status_signature_verified_cutout"
do
echo $NAME
inkscape -w 24 -h 24 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 32 -h 32 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 48 -h 48 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 64 -h 64 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done

for NAME in "status_none_dots_1" "status_none_dots_2" "status_none_dots_3" "status_check_dots_1" "status_check_dots_1" "status_check_dots_3" "status_dots" "status_lock_none_dots_1" "status_lock_disabled_dots_1" "status_lock_dots_1" "status_lock_error_dots_1" "status_lock_dots_2" "status_lock_dots_3"
do
echo $NAME
inkscape -w 36 -h 24 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 48 -h 32 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 72 -h 48 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 96 -h 64 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done

for NAME in "status_dots_1" "status_dots_2" "status_dots_3"
do
echo $NAME
inkscape -w 12 -h 24 -e "$MDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 16 -h 32 -e "$HDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 24 -h 48 -e "$XDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
inkscape -w 32 -h 64 -e "$XXDPI_DIR/$NAME.png" "$SRC_DIR/$NAME.svg"
done
