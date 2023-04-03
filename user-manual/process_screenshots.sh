#!/bin/bash
set -e

SCRIPT_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

pushd $SCRIPT_PATH

mkdir -p output/setup/img/
cp screenshots/welcome_screen.png output/setup/img/

mkdir -p output/accounts/img/

cp screenshots/account_setup_* output/accounts/img/
cp screenshots/drawer_account_list.png output/accounts/img/

# Adding another account
convert screenshots/drawer_one_account.png -stroke red -strokewidth 5 -fill none -draw "rectangle 20,1750 820,1840" output/accounts/img/drawer_settings_highlight.png
convert screenshots/settings_one_account.png -stroke red -strokewidth 5 -fill none -draw "rectangle 20,580 1060,690" output/accounts/img/settings_add_account_highlight.png

# Switching between multiple accounts
convert screenshots/unified_inbox.png -crop 1080x660+0+0 -stroke red -strokewidth 5 -fill none -draw "rectangle 20,85 130,190" output/accounts/img/message_list_drawer_button_highlight.png
convert screenshots/drawer_two_accounts.png -crop 1080x650+0+0 -stroke red -strokewidth 5 -fill none -draw "rectangle 720,360 815,455" output/accounts/img/drawer_account_switcher_highlight.png

# Removing an account
convert screenshots/drawer_two_accounts.png -stroke red -strokewidth 5 -fill none -draw "rectangle 20,1750 820,1840" output/accounts/img/drawer_two_accounts_settings_highlight.png
convert screenshots/settings_two_accounts.png -stroke red -strokewidth 5 -fill none -draw "rectangle 20,580 1060,735" output/accounts/img/settings_second_account_highlight.png
convert screenshots/account_settings.png -crop 1080x485+0+0 -stroke red -strokewidth 5 -fill none -draw "rectangle 980,85 1060,190" output/accounts/img/account_settings_menu_highlight.png
convert screenshots/account_settings_menu_expanded.png -crop 1080x485+0+0 -stroke red -strokewidth 5 -fill none -draw "rectangle 570,110 1050,210" output/accounts/img/account_settings_remove_account_highlight.png

# Reading mail
mkdir -p output/reading/img/

cp screenshots/reading_folder_view.png output/reading/img/
cp screenshots/reading_email_view.png output/reading/img/

# Unified Inbox
convert screenshots/unified_inbox.png -crop 1080x882+0+0 -stroke red -strokewidth 5 -fill none -draw "rectangle 176,238 207,756" output/reading/img/unified_inbox_account_chip_highlight.png
convert screenshots/drawer_account_list.png -crop 1080x890+0+0 -stroke red -strokewidth 5 -fill none -draw "rectangle 25,515 163,864" output/reading/img/drawer_account_list_account_image_highlight.png

# Managing mail
convert screenshots/message_list_multi_select.png -crop 1080x1110+0+0 output/reading/img/managing_multiselect.png
convert screenshots/message_list_multi_select.png -crop 1080x139+0+64 -stroke red -strokewidth 5 -fill none -draw "rectangle 606,24 1062,116" output/reading/img/reading_actionbar_options.png

popd
