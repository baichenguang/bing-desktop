#!/bin/sh
# 调用Finder应用切换桌面壁纸
osascript -e "tell application \"Finder\" to set desktop picture to POSIX file \"$1\""