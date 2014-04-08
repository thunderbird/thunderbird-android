#!/bin/bash

# What we get from Transifex is unusable and needs some fixing before we're able to use
# the translations.

FILE=$1

# Fix xliff tags
perl -i -pe 's/&lt;xliff:g id=\\?"(.*?)?\\?"&gt;/<xliff:g id="\1">/g' $FILE
perl -i -pe 's/&lt;\/xliff:g&gt;/<\/xliff:g>/g' $FILE

# Escape single and double quotes before and after xliff tags
perl -i -pe 's/([^\\])(["'\''])<xliff/\1\\\2<xliff/g' $FILE
perl -i -pe 's/xliff:g>(["'\''])/xliff:g>\\\1/g' $FILE

# Restore "&lt;" and "&gt;"
perl -i -pe 's/&amp;(lt|gt);/&\1;/g' $FILE

# <string ...></string> -> <string ... />
perl -i -pe 's/"><\/string>/"\/>/g' $FILE

# Escape single and double quotes (but not in comments or the xml tag)
perl -i -pe 's/([^\\])'\''/\1\\'\''/g unless /(<!--|xml)/' $FILE
