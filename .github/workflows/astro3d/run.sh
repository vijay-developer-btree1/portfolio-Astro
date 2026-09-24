#!/usr/bin/env bash
cd "$(dirname "$0")"
mkdir -p bin
javac -encoding UTF-8 -d bin \
  src/astro3d/*.java src/astro3d/core/*.java src/astro3d/model/*.java \
  src/astro3d/scene/*.java src/astro3d/ui/*.java
java -Dfile.encoding=UTF-8 -cp bin astro3d.Main
