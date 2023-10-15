package com.burgaud.jssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
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
         version = "@|green,bold jssl version 0.12.0|@",
         header = {
            "@|green,bold     __  ____  ____  __           |@",
            "@|green,bold   _(  )/ ___)/ ___)(  )          |@",
            "@|green,bold  / \\) \\\\___ \\\\___ \\/ (_/\\ |@",
            "@|green,bold  \\____/(____/(____/\\____/      |@",
         ""},
         description = "Validate health of servers TLS configuration.")
public class Jssl implements Callable<Integer> {
    @Parameters(paramLabel = "SERVER", arity = "0..*", description = "One or more servers to analyze.")
    String[] servers;

    @Option(names = {"-d", "--debug"}, description = "Display debug information.")
    private boolean debug = false;

    @Option(names = {"-f", "--file"}, description = "Servers are listed in a file.")
    private Path file;

    @Option(names = {"--format"}, description = "Output format: csv or console (default).")
    private String format = "";
    
    @Option(names = {"-w", "--workers"}, description = "Number of concurrent workers.")
    private int workers = 1;

    @Option(names = {"--java-version"}, description = "Show Java Version.")
    private boolean javaVersion;

    private Ssl ssl = null;

    @Override public Integer call() {
        if (javaVersion) {
            Cli.printInfo(String.format("Java version %s", System.getProperty("java.version")));
            return 0;
        }

        List<String> serverList;
        this.ssl = new Ssl(debug);
        if (null != file) {
            try {
                serverList = expandServers(file);
            }
            catch(IOException e) {
                Cli.printError(String.format("Error opening %s", e.getMessage()));
                return 1;
            }
        }
        else if (null == servers) {
            Cli.printError("At least a server or a file expected as argument");
            return 1;
        }
        else {
            serverList = Arrays.asList(servers);
        }

        var results = process(serverList);
        return switch (format) {
            case "csv" -> csvFormat(results);
            //case "json" -> 0;
            default -> consoleFormat(results);
        };
    }

    private int consoleFormat(List<ServerProtocols> results) {
        results.forEach((res) -> {
            System.out.println();
            Cli.printBanner(res.server());
            if (res.error().length() > 0) {
                System.out.printf("  %-15s ", "Error");
                Cli.printError(res.error());
            }
            else {
                int index = 0;
                for (var protocol : ProtocolCallable.protocols) {
                    Format.printProtocolSupport(protocol, res.supported()[index]);
                    index++;
                }
            }
        });
        return 0;
    }

    private int csvFormat(List<ServerProtocols> results) {
        System.err.println();
        Format.csv(results);
        return 0;
    }

    private List<ServerProtocols> process(List<String> servers) {
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        List<Future<ServerProtocols>> futures = new ArrayList<>();
        
        int index = 0;
        String[] spinner = new String[]{"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};
        
        for(var server : servers) {
            System.err.printf("%s\r", spinner[index%8]);
            futures.add(pool.submit(new ProtocolCallable(server, ssl)));
            index++;
        }

        List<ServerProtocols> results = new ArrayList<>();
        for(var f : futures) {
            System.err.printf("%s\r", spinner[index%8]);
            try {
                results.add(f.get());
            }
            catch(Exception e) {
                results.add(new ServerProtocols("", null, e.getMessage()));
            }
            index++;
        }
        pool.shutdown();
        System.err.print("\r ");
        return results;
    }

    private List<String> expandServers(Path file) throws IOException {
        List<String> servers;
        try (Stream<String> stream = Files.lines(file)) {
            servers = stream.filter(s -> s.length() > 0)
            .filter(s -> !s.startsWith("#"))
            .collect(Collectors.toList());
        }
        return servers;
    }
}
