#!/bin/sh
cd "$(dirname "$0")" || exit 1
sh ./app/run.sh start
echo
printf 'Press Enter to close this window...'
read -r _
