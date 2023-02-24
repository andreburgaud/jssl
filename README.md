# README



## Native Compilation

Having issues generating the native executable via Gradle. To workaround this issue:

* Generate the Distribution

```
$ ./gradlew distZip
```

* Create a temporary folder

```
$ mkdir -p temp/bin
```

* Extract the distribution into the temporary folder

```
$ unzip build/distributions/jssl.zip -d temp
```

* Compile native executagle

```
$ native-image -cp temp/jssl/lib/picocli-4.7.1.jar -jar temp/jssl/lib/jssl.jar --static --no-fallback -o temp/bin/jssl
```

