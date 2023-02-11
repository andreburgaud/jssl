#!/usr/bin/env bash

export JAVA_OPTS="$JAVA_OPTS -Djava.security.properties==./java.security"
groovy sslversion.groovy $*