#!/bin/bash

set -e

echo ""
echo " ... Running build"

cd bungeeadmintools-git

chmod +x ./gradlew

java --version
# gradle build
./gradlew clean shadowJar

# create target folder
mkdir -f ../build-output

# move the output
cp build/libs/*.jar ../build-output/