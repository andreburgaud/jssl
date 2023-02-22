package com.burgaud.jssl;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import java.util.ArrayList;
import java.util.List;

class Main {

    private static final String BANNER = """
                    _ ___ ___ _    
                 _ | / __/ __| |   
                | || \\__ \\__ \\ |__ 
                 \\__/|___/___/____|
                 """;
   

    private static final String ENABLED = "enabled";
    private static final String NOT_ENABLED = "not enabled";
    private Ssl ssl = null;

    @Parameter(names={"--debug", "-d"}, description = "Debug mode")
    private boolean debug = false;

    @Parameter
    private List<String> parameters = new ArrayList<>();

    Main() {
        this.ssl = new Ssl(debug);
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

    public void checkProtocol(String server) {
        System.out.println(ansi().fg(GREEN).a(server).reset());
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

    public void run() {
        System.out.printf("Debug: %b\n", debug);
        AnsiConsole.systemInstall();
        Cli.printBanner(BANNER);
        var server = "burgaud.com";
        checkProtocol(server);
        AnsiConsole.systemUninstall();
    }

    public static void main(String... args) {
        Main m = new Main();
        JCommander.newBuilder()
            .addObject(m)
            .build()
            .parse(args);
        m.run();
    }
}
