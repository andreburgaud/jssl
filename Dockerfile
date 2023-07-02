FROM ghcr.io/graalvm/native-image:muslib-ol9-java17-22.3.2 as build

ENV LANG=C.UTF-8

RUN useradd -u 10001 jssluser

RUN microdnf -y install findutils

ADD . .

COPY java.security ${JAVA_HOME}/conf/security

RUN mkdir -p /native/bin
RUN mkdir -p /native/lib

RUN mkdir /files

RUN ./gradlew installDist --no-daemon

RUN native-image -cp ./build/install/jssl/lib/picocli-4.7.1.jar --static --no-fallback --libc=musl -jar ./build/install/jssl/lib/jssl.jar -o /native/bin/jssl

FROM scratch
COPY --from=build /native/bin/jssl /jssl
COPY --from=build /files /files
COPY --from=build /etc/passwd /etc/passwd

ENV LANG=C.UTF-8

USER jssluser

ENTRYPOINT [ "/jssl" ]
