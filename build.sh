#!/bin/bash

set -e

echo "---- Build Started ----"
echo ""

cd bungeeadmintools-git

chmod +x ./gradlew

echo "---- Java Version ----"
java -version
echo ""

# gradle build
./gradlew clean shadowJar publish

echo ""
echo "---- Build Finished ----"