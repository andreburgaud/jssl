package com.burgaud.jssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static picocli.CommandLine.*;

record ServerProtocols(String server, boolean[] supported, String error) {}

class ProtocolCallable implements Callable<ServerProtocols> {
    private final String server;
    private final Ssl ssl;
    public static final String[] protocols = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

    ProtocolCallable(String server, Ssl ssl) {
        this.server = server;
        this.ssl = ssl;
    }

    @Override
    public ServerProtocols call() throws Exception {
        return getSupportedProtocols();
    }

    public ServerProtocols getSupportedProtocols() {
        boolean[] supported = new boolean[5];
        String error = "";
        int i = 0;
        try {
            for (var protocol : protocols) {
                supported[i++] = ssl.isProtocolEnabled(server, protocol);
            }
        }
        catch(UnknownHostException uhe) {
            error = "Unknown host";
        }
        catch(SocketTimeoutException ste) {
            error = ste.getMessage();
        }
        catch(Exception e) {
            error = e.getMessage();
        }
        return new ServerProtocols(server, supported, error);
    }
}


@Command(name = "jssl", 
         mixinStandardHelpOptions = true, 
         version = "@|green,bold jssl version 0.4.0 |@",
         header = {
            "@|green,bold     __  ____  ____  __           |@",
            "@|green,bold   _(  )/ ___)/ ___)(  )          |@",
            "@|green,bold  / \\) \\\\___ \\\\___ \\/ (_/\\ |@",
            "@|green,bold  \\____/(____/(____/\\____/      |@",
         ""},
         description = "Validate health of servers TLS configuration.")
public class Jssl implements Callable<Integer> {
    @Parameters(paramLabel = "SERVER", description = "One or more servers to analyze.")
    String[] servers;

    @Option(names = {"-d", "--debug"}, description = "Display debug information.")
    private boolean debug = false;

    @Option(names = {"-f", "--file"}, description = "Servers are listed in a file.")
    private Path file;
    
    @Option(names = {"-w", "--workers"}, description = "Number of concurrent workers.")
    private int workers = 1;

    private static final String ENABLED = "enabled";
    private static final String NOT_ENABLED = "not enabled";
    private Ssl ssl = null;

    @Override public Integer call() {
        this.ssl = new Ssl(debug);
        if (null != file) {
            try {
                servers = expandServers(file);
            }
            catch(IOException e) {
                Cli.printError(e.getMessage());
                return 1;
            }
        }
        if (null == servers) {
            Cli.printError("At least one server expected as argument");
            return 1;
        }
        return process(servers);
    }

    private int process(String[] servers) {
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        List<Future<ServerProtocols>> futures = new ArrayList<Future<ServerProtocols>>(); 
        int index = 0;
        String[] spinner = new String[]{"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};
        for(var server : servers) {
            System.out.printf("%s\r", spinner[index%8]);
            var future = pool.submit(new ProtocolCallable(server, ssl));
            futures.add(future);
            index++;
        }
        for(var f : futures) {
            try {
                var res = f.get();
                index = 0;
                System.out.println();
                Cli.printBanner(res.server());
                if (res.error().length() > 0) {
                    System.out.printf("  %-15s ", "Error");
                    Cli.printError(res.error());
                }
                else {
                    for (var protocol : ProtocolCallable.protocols) {
                        printProtocolSupport(protocol, res.supported()[index]);
                        index++;
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        pool.shutdown();
        return 0;
    }

    private String[] expandServers(Path file) throws IOException {
        Stream<String> stream = Files.lines(file);
        String[] servers = stream.filter(s -> s.length() > 0)
                                 .filter(s -> !s.startsWith("#"))
                                 .toArray(String[]::new);
        stream.close();
        return servers;
    }

    private void printProtocolSupport(String protocol, boolean enabled) {
        System.out.printf("  %-15s ", protocol);

        switch(protocol) {
        case "SSLv3":
            if (enabled) {
                Cli.printError(ENABLED);
            }
            else {
                Cli.printSuccess(NOT_ENABLED);
            }
            break;
        case "TLSv1":
        case "TLSv1.1":
            if (enabled) {
                Cli.printWarning(ENABLED);
            }
            else {
                Cli.printSuccess(NOT_ENABLED);
            }
            break;
        case "TLSv1.2":
        case "TLSv1.3":
            if (enabled) {
                Cli.printSuccess(ENABLED);
            }
            else {
                System.out.println(NOT_ENABLED);
            }
            break;
        default:
            Cli.printWarning("unexpected protocol");
        }
    }
}
