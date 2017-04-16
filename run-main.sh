#!/bin/bash
_pwd=`pwd`
cd $(cd "$(dirname "$0")"; pwd)
mvn clean install exec:java -Dexec.mainClass="baichenguang.bing.BingDesktop"
