#!/usr/bin/env just --justfile

APP := "jssl"
VERSION := "0.6.2"
NATIVE_DIR := "native"

alias db := docker-build
alias ghp := github-push
alias gj := gradle-jar
alias c := clean

# Default recipe (this list)
default:
    @just --list

# Build a docker image
docker-build: clean
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
    -rm -rf {{NATIVE_DIR}}

# Direct native compile (no Gradle)
native: clean
    ./gradlew distZip
    mkdir -p {{NATIVE_DIR}}/bin
    unzip build/distributions/jssl.zip -d {{NATIVE_DIR}}
    native-image -cp temp-native/jssl/lib/picocli-4.7.1.jar -jar temp-native/jssl/lib/jssl.jar --static --no-fallback -o {{NATIVE_DIR}}/bin/jssl

# Push and tag changes to github
github-push:
    git push
    git tag -a {{VERSION}} -m 'Version {{VERSION}}'
    git push origin --tags
