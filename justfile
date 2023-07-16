#!/usr/bin/env just --justfile

APP := "jssl"
VERSION := "0.10.0"
NATIVE_DIR := "native"

alias db := docker-build
alias ghp := github-push
alias gj := gradle-jar
alias c := clean
alias nl := native-linux

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

# Generate native image
gradle-native:
    ./gradlew nativeCompile

# Clean build and release artifacts
clean:
    ./gradlew clean
    -rm -rf {{NATIVE_DIR}}
    -rm -rf ./bin

# Native compile via container (Linux only)
native-linux: docker-build
    mkdir ./bin
    docker create --name jssl-build andreburgaud/{{APP}}:{{VERSION}}
    docker cp jssl-build:/jssl ./bin
    docker rm -f jssl-build
    zip -j bin/{{APP}}-{{VERSION}}_linux_{{arch()}}.zip bin/{{APP}}

# Direct native compile
native-image: clean
    ./gradlew installDist
    mkdir -p {{NATIVE_DIR}}/bin
    native-image --initialize-at-build-time={{APP}} -Djava.security.properties==./java.security -cp ./build/install/jssl/lib/picocli-4.7.4.jar --no-fallback -jar ./build/install/jssl/lib/jssl.jar -o {{NATIVE_DIR}}/bin/{{APP}}
    upx --best {{NATIVE_DIR}}/bin/{{APP}}
    zip -j {{NATIVE_DIR}}/{{APP}}-{{VERSION}}_{{os()}}_{{arch()}}.zip {{NATIVE_DIR}}/bin/{{APP}}

# Push and tag changes to github
github-push:
    git push
    git tag -a {{VERSION}} -m 'Version {{VERSION}}'
    git push origin --tags
