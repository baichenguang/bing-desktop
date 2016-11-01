@echo off
cd /d %~dp0
call setenv.bat
mvn exec:java -Dexec.mainClass="baichenguang.bing.BingDesktop" -q