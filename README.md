# JSSL

SSL Client which checks SSL/TLS protocols enabled on a server (from SSLv3 to TLS v1.3)

![JSSL 0.8.1 Console Output](https://github.com/andreburgaud/jssl/assets/6396088/67b8a736-32ef-4cae-adea-e8b0af6aebdc)

## Usage

The recommended usage is via Docker. For example, to check the SSL/TLS protocol versions enabled on the main Google server:

```
$ docker run --rm -it andreburgaud/jssl google.com

google.com
  SSLv3           not enabled
  TLSv1           enabled
  TLSv1.1         enabled
  TLSv1.2         enabled
  TLSv1.3         enabled
```

For other JSSL options, execute the following command:

```
$ docker run --rm -it andreburgaud/jssl --help
    __  ____  ____  __
  _(  )/ ___)/ ___)(  )
 / \) \\___ \\___ \/ (_/\
 \____/(____/(____/\____/

Usage: jssl [-dhV] [--java-version] [-f=<file>] [--format=<format>]
            [-w=<workers>] [SERVER...]
Validate health of servers TLS configuration.
      [SERVER...]           One or more servers to analyze.
  -d, --debug               Display debug information.
  -f, --file=<file>         Servers are listed in a file.
      --format=<format>     Output format: csv or console (default).
  -h, --help                Show this help message and exit.
      --java-version        Show Java Version.
  -V, --version             Print version information and exit.
  -w, --workers=<workers>   Number of concurrent workers.
```

## Java Compilation

To execute a local non-native version, create a first a distribution:


```
$ ./gradlew installDist
```

Then you can execute JSSL by invoking the generate bootstrap script:

```
$ build/install/jssl/bin/jssl google.com
```

### Warning

By default, the Java compilation will generate a version only able to validate SSL protocols enabled in the file `${JAVA_HOME}/conf/security`. For JSSL to validate SSLv3, TLSv1 and TLSv1.1, you need to enable these protocols for the Java runtime. You can bypass the existing security limitations by commenting the following lines in `${JAVA_HOME}/conf/security`:

```
#jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA, \
#    DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL, \
#    include jdk.disabled.namedCurves
```
This will affect any Java application running in this Java environment, hence we recommend that you instead use the [Docker image](https://hub.docker.com/r/andreburgaud/jssl) built with the proper settings.

Another option is to consider a native compilation via GraalVM (see the next section).

## Native Compilation

* As of 7/1/2023, the Linux native compilation is successful via a container. You can see the `justfile` task `native-linux` to perform a Linux native build.
* Static executable images are not supported on Mac Os (Darwin)

# License

[MIT License](./LICENSE)