FROM ghcr.io/graalvm/native-image:22.3.1 as build

RUN useradd -u 10001 jssluser

ADD . .

COPY java.security ${JAVA_HOME}/conf/security

RUN microdnf -y install gzip zlib-static unzip findutils tree

RUN mkdir -p /native/bin
RUN mkdir /files

RUN ./gradlew distZip --no-daemon && \
    unzip ./build/distributions/app.zip -d /native

ARG RESULT_LIB="/musl"
RUN mkdir ${RESULT_LIB} && \
    curl -L -o musl.tar.gz https://more.musl.cc/10.2.1/x86_64-linux-musl/x86_64-linux-musl-native.tgz && \
    tar -xvzf musl.tar.gz -C ${RESULT_LIB} --strip-components 1

ENV CC=/musl/bin/gcc

RUN curl -L -o zlib.tar.gz https://zlib.net/zlib-1.2.13.tar.gz && \
    mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && \
    cd zlib && ./configure --static --prefix=/musl && \
    make && make install && \
    cd / && rm -rf /zlib && rm -f /zlib.tar.gz

ENV PATH="$PATH:/musl:/musl/bin"

RUN native-image -cp /native/app/lib/picocli-4.7.1.jar --static --no-fallback --libc=musl -jar /native/app/lib/app.jar -o /native/bin/jssl

FROM scratch
COPY --from=build /native/bin/jssl /jssl
COPY --from=build /files /files
COPY --from=build /etc/passwd /etc/passwd

USER jssluser

ENTRYPOINT [ "/jssl" ]
