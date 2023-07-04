FROM ghcr.io/graalvm/native-image:muslib-ol9-java17-22.3.2 as build

ENV LANG=C.UTF-8

RUN useradd -u 10001 jssluser

RUN microdnf -y install findutils

RUN mkdir -p /work/native/bin
RUN mkdir -p /work/native/lib
RUN mkdir /files

WORKDIR /jssl

ADD . .

COPY java.security ${JAVA_HOME}/conf/security

RUN ./gradlew installDist --no-daemon

RUN native-image -cp /jssl/build/install/jssl/lib/picocli-4.7.4.jar --static --no-fallback --libc=musl -jar /jssl/build/install/jssl/lib/jssl.jar -o /jssl/native/bin/jssl

FROM scratch
COPY --from=build /jssl/native/bin/jssl /jssl
COPY --from=build /files /files
COPY --from=build /etc/passwd /etc/passwd

ENV LANG=C.UTF-8

USER jssluser

ENTRYPOINT [ "/jssl" ]
