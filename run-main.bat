@echo off
call setenv.bat
mvn clean install exec:java -Dexec.mainClass="baichenguang.bing.BingDesktop"