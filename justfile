#!/usr/bin/env just --justfile

APP := "jssl"
VERSION := "0.8.1"
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

# Run from an install distribution
run  *ARGS:
    ./gradlew installDist
    ./build/install/jssl/bin/jssl {{ARGS}}

# Generate the jar file
gradle-jar:
    ./gradlew jar

# Generate the jar file (not working as of 7/1/2023)
gradle-native: gradle-jar
    ./gradlew nativeCompile

# Clean build and release artifacts
clean:
    ./gradlew clean
    -rm -rf {{NATIVE_DIR}}

# Native compile via container (Linux only)
native-linux:
    docker create --name jssl-build andreburgaud/{{APP}}:{{VERSION}}
    docker cp jssl-build:/jssl ./bin
    docker rm -f jssl-build
    zip -j build/{{APP}}-{{VERSION}}_linux_{{arch()}}.zip bin/{{APP}}

# Direct native compile (not working as of 7/1/2023)
native: clean
    ./gradlew installDist
    mkdir -p {{NATIVE_DIR}}/bin
    native-image -cp ./build/install/jssl/lib/picocli-4.7.4.jar --static --no-fallback --libc=musl -jar ./build/install/jssl/lib/jssl.jar -o {{NATIVE_DIR}}/bin/jssl

# Push and tag changes to github
github-push:
    git push
    git tag -a {{VERSION}} -m 'Version {{VERSION}}'
    git push origin --tags
