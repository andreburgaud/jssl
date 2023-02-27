#!/usr/bin/env just --justfile

APP := "jssl"
VERSION := "0.6.1"

alias db := docker-build
alias ghp := github-push
alias gj := gradle-jar
alias c := clean

# Default recipe (this list)
default:
    @just --list

# Build a docker image
docker-build:
    ./gradlew clean
    ./gradlew distZip
    docker build -t andreburgaud/{{APP}}:latest .
    docker tag andreburgaud/{{APP}}:latest andreburgaud/{{APP}}:{{VERSION}}

# Generate the jar file
gradle-jar:
    ./gradlew jar

# Generate the jar file
gradle-native: gradle-jar
    ./gradlew nativeCompile

# Clean build and release artifacts
clean:
    ./gradlew clean
    -rm -rf temp-native

# Direct native compile (no Gradle)
native: clean
    ./gradlew distZip
    mkdir -p temp-native/bin
    unzip build/distributions/jssl.zip -d temp-native
    native-image -cp temp-native/jssl/lib/picocli-4.7.1.jar -jar temp-native/jssl/lib/jssl.jar --static --no-fallback -o temp-native/bin/jssl

# Push and tag changes to github
github-push:
    git push
    git tag -a {{VERSION}} -m 'Version {{VERSION}}'
    git push origin --tags
