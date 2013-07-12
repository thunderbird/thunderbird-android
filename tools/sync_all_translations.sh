#!/bin/bash

SCRIPT=$(readlink -f $0)
SCRIPTPATH=`dirname $SCRIPT`
PROJECTROOT=`dirname $SCRIPTPATH`

cd $PROJECTROOT

find res/values-* -name "strings.xml" -type f ! -wholename "res/values-fr-rCA/strings.xml" -exec ./tools/sync_translation.py res/values/strings.xml {} \;

cd -
