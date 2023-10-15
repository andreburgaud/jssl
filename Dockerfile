FROM container-registry.oracle.com/graalvm/native-image:21-muslib-ol9 as build

ENV LANG=C.UTF-8

WORKDIR /jssl

RUN useradd -u 10001 jssluser

RUN microdnf -y install findutils xz

RUN curl --location --output upx-4.1.0-amd64_linux.tar.xz "https://github.com/upx/upx/releases/download/v4.1.0/upx-4.1.0-amd64_linux.tar.xz" && \
    tar -xJf "upx-4.1.0-amd64_linux.tar.xz" && \
    cp upx-4.1.0-amd64_linux/upx /bin/

RUN mkdir -p ./native/bin && \
    mkdir /files

WORKDIR /jssl

ADD . .

COPY java.security ${JAVA_HOME}/conf/security

RUN ./gradlew installDist --no-daemon

RUN native-image -march=compatibility -cp /jssl/build/install/jssl/lib/picocli-4.7.5.jar --static --no-fallback --libc=musl -jar /jssl/build/install/jssl/lib/jssl.jar -o /jssl/native/bin/jssl && \
    strip /jssl/native/bin/jssl && \
    upx --best /jssl/native/bin/jssl

FROM scratch
COPY --from=build /jssl/native/bin/jssl /jssl
COPY --from=build /files /files
COPY --from=build /etc/passwd /etc/passwd

ENV LANG=C.UTF-8

USER jssluser

ENTRYPOINT [ "/jssl" ]
