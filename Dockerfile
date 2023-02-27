FROM ghcr.io/graalvm/native-image:22.3.1 as build

ADD . .

COPY java.security ${JAVA_HOME}/conf/security

RUN microdnf -y install gzip zlib-static unzip

RUN mkdir /build

COPY build/distributions/jssl.zip /build

RUN mkdir /native
RUN mkdir /files
RUN unzip /build/jssl.zip -d /native

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

RUN native-image -cp /native/jssl/lib/picocli-4.7.1.jar --static --no-fallback --libc=musl -jar /native/jssl/lib/jssl.jar -o /jssl

FROM scratch
COPY --from=build /jssl /jssl
COPY --from=build /files /files

ENTRYPOINT [ "/jssl" ]
