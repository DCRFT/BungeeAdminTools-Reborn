#!/bin/bash

set -e

export REPOSILITE-ALIAS="$1"
export REPOSILITE-TOKEN="$2"

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