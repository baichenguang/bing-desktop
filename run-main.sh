#!/bin/bash
_pwd=`pwd`
cd $(cd "$(dirname "$0")"; pwd)
mvn exec:java -Dexec.mainClass="baichenguang.bing.BingDesktop"
