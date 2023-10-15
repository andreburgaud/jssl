# JSSL

SSL Client which checks SSL/TLS protocols enabled on a server (from SSLv3 to TLS v1.3)

![JSSL 0.12.0 Console Output](https://github.com/andreburgaud/jssl/assets/6396088/301e179b-35f7-473d-a007-0517961f08f7)

## Usage

The recommended usage is via [Docker](https://hub.docker.com/r/andreburgaud/jssl). For example, to check the SSL/TLS protocol versions enabled on the main Google server:

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

### List of Servers in a File

To avoid typing many hosts at the command line, you can create a file with one host per line and use this file as an argument to option `--file`. For example you can prepare a file with the follwoing entries:

```
mailserv.baehal.com
google.com
microsoft.com
```

Assuming the file is named `servers.txt` and exists in the current directory, you can run the JSSL container as follows:

```
$ docker run --rm -it -v $PWD:/files andreburgaud/jssl --file /files/servers.txt
...
```

The image includes a `/files` folder that you can map via the docker `--volume` option (or `-v`).


### Workers

If you pass a long list of servers to JSSL, especially via an input file, you may want to set a number of workers. By default there is only one worker. JSSL creates a thread pool based on the number of workers set at the command line via the option `--workers` or `-w`.

If you have a list of servers in `servers.txt`, in the current working directory, and you want to execute the command with 10 workers, the docker command line would be the following:

```
$ docker run --rm -it -v $PWD:/files andreburgaud/jssl --workers 10 --file /files/servers.txt
...

```


### Server Ports

The default SSL port for each server is `443`. If SSL is enabled on a port other than `443` for a given server, you can append the port to a server name, separating the name of the server and the port number with the colon (`:`) character:

```
<server_name>:<port>
```

If you enable SSL on port `4433` for host `example.com`, the JSSL input will be the following:

```
example.com:4433
```

This works at the JSSL command line or in an input file with option `--file`.


## Java Compilation

To execute a local non-native version, first, create a distribution:


```
$ ./gradlew installDist
```

Then you can execute JSSL by invoking the generate bootstrap script:

```
$ build/install/jssl/bin/jssl google.com
```

You can also use `java -jar`, as follows:

```
$ java -Djava.security.properties==./java.security -jar build/install/jssl/lib/jssl.jar google.com
```

Setting `java.security.properties` to `./java.security` enables SSL protocol below `TLSv1.2`.

### Warning

By default, the Java compilation will generate a version only able to validate SSL protocols enabled in the file `${JAVA_HOME}/conf/security`. For JSSL to validate SSLv3, TLSv1 and TLSv1.1, you need to enable these protocols for the Java runtime. You can bypass the existing security limitations by commenting the following lines in `${JAVA_HOME}/conf/security`:

```
#jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA, \
#    DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL, \
#    include jdk.disabled.namedCurves
```

This would affect any Java applications running in this Java environment, hence we recommend that you instead use the [Docker image](https://hub.docker.com/r/andreburgaud/jssl) built with the proper settings.

Another option is to consider a native compilation via GraalVM (see the next section).


## Native Compilation

* As of 7/1/2023, the Linux native compilation is successful via a container. You can see the `justfile` task `native-linux` to perform a Linux native build.
* Linux x86_64 native executables are available in the [GitHub releases section](https://github.com/andreburgaud/jssl/releases)
* Static executable images are not supported on Mac Os (Darwin)


### Native Image via Docker

If you are on Linux x86-64, you can build the Docker image and then extract the files needed to run JSSL Server locally:

```
$ docker build -t jssl .
$ docker create --name jssl-copy jssl:latest
$ docker cp jssl-copy:/jssl .
$ docker rm -f jssl-copy
```

To run the application:

```
$ ./jssl google.com
```

### Native Image

To build a native image on your machine - not via Docker - you need to [install GraalVM](https://www.graalvm.org/latest/docs/getting-started/). You can also use [SDKMAN](https://sdkman.io/) to manage different JVMs, including GraalVM.

You can build a local native image, executing the following command:

```
$ just native-image
```

To test and execute the application:

```
$ native/bin/jssl google.com
```


# License

[MIT License](./LICENSE)