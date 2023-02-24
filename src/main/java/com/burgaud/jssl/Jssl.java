package com.burgaud.jssl;

import java.util.concurrent.Callable;

import static picocli.CommandLine.*;

@Command(name = "jssl", 
         mixinStandardHelpOptions = true, 
         version = "0.3.0",
         header = {
            "@|green,bold     __  ____  ____  __           |@",
            "@|green,bold   _(  )/ ___)/ ___)(  )          |@",
            "@|green,bold  / \\) \\\\___ \\\\___ \\/ (_/\\ |@",
            "@|green,bold  \\____/(____/(____/\\____/      |@",
         ""},
         description = "Validate health of servers TLS configuration.",
         subcommands = Protocol.class)
public class Jssl implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}

@Command(name = "protocol", description = "Check SSL/TLS procols a server supports.")
class Protocol implements Callable<Integer> {

    @Option(names = {"-d", "--debug"}, description = "Display debug information.")
    private boolean debug = false;

    @Parameters(paramLabel = "SERVER", description = "One or more servers to analyze.")
    String[] servers;

    private static final String ENABLED = "enabled";
    private static final String NOT_ENABLED = "not enabled";
    private Ssl ssl = null;

    @Override public Integer call() {
        this.ssl = new Ssl(debug);
        if (null == servers) {
            Cli.printError("At least one server expected as argument");
            return 1;
        }
        else {
            for(var server : servers) {
                checkProtocol(server);
            }
            return 0;
        }
    }

    private void printProtocolSupport(String protocol, boolean enabled) {
        System.out.printf("  %s ", protocol);

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

    public void checkProtocol(String server) {;
        Cli.printBanner(server);
        String[] protocols = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
        try {
            for (var protocol : protocols) {
                printProtocolSupport(protocol, ssl.isProtocolEnabled(server, protocol));
            }
        }
        catch(java.net.SocketTimeoutException ste) {
            ste.printStackTrace();
        }
        catch(java.net.UnknownHostException ue) {
            ue.printStackTrace();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
