package com.burgaud.jssl;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

class Main {

    private static final String ENABLED = "enabled";
    private static final String NOT_ENABLED = "not enabled";
    private boolean debug = false;
    private SSLSocketFactory factory = null;

    Main(boolean debug) {
        this.debug = debug;
        this.factory = initSocketFactory();

    }

    public static void printWarning(String msg) {
         System.out.println(ansi().fg(YELLOW).a(msg).reset());
    }

    public static void printError(String msg) {
        System.out.println(ansi().fg(RED).a(msg).reset());

    }
    public static void printInfo(String msg) {
        System.out.println(ansi().fg(GREEN).a(msg).reset());

    }
    public static void printSuccess(String msg) {
        System.out.println(ansi().fg(GREEN).a(msg).reset());
    }

    private boolean isProtocolEnabled(String server, String protocol) throws Exception {
        String hostname;
        int port = 443;

        String[] hostnamePort = server.split(":");
        if (hostnamePort.length > 1) {
            port = Integer.parseInt(hostnamePort[1]);
        }
        hostname = hostnamePort[0];

        Socket socket = new Socket();
        socket.setSoTimeout(2000);

        var socketAddr = new InetSocketAddress(hostname, port);
        socket.connect(socketAddr, 2000);

        SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, hostname, port, true);
        String[] protocols = new String[] {protocol};
        sslSocket.setEnabledProtocols(protocols);

        boolean res = false;
        try {
            sslSocket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    SSLSession session = event.getSession();
                    var p = session.getProtocol();
                    if (debug) {
                        System.out.printf(">>> expected protocol: %s -> negotiated protocol: %s\n", protocol, p);
                    }
                }
            });
            sslSocket.startHandshake();
            res = true;
        }
        catch(Exception e) {
            if (debug) {
                System.out.printf(">>> %s: %s", protocol, e);
            }
        }
        finally {
            try {
                sslSocket.close();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return res;
    }

    private SSLSocketFactory initSocketFactory() {
        SSLSocketFactory factory = null;

        try {
            TrustManager[] trustAllCerts = { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};

            SSLContext sc = SSLContext.getInstance("TLSv1.3");
            sc.init(null, trustAllCerts, new SecureRandom());
            factory = (SSLSocketFactory) sc.getSocketFactory();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return factory;
    }

    private void printProtocolSupport(String protocol, boolean enabled) {
        System.out.printf("  %s ", protocol);

        switch(protocol) {
            case "SSLv3":
                if (enabled) {
                    printError(ENABLED);
                }
                else {
                    printSuccess(NOT_ENABLED);
                }
                break;
            case "TLSv1":
            case "TLSv1.1":
                if (enabled) {
                    printWarning(ENABLED);
                }
                else {
                    printSuccess(NOT_ENABLED);
                }
                break;
            case "TLSv1.2":
            case "TLSv1.3":
                if (enabled) {
                    printSuccess(ENABLED);
                }
                else {
                    System.out.println(NOT_ENABLED);
                }
                break;
            default:
                printWarning("unexpected protocol");
        }
    }

    public void checkProtocol(String server) {
        String[] protocols = {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
        try {
            for (var protocol : protocols) {
                printProtocolSupport(protocol, isProtocolEnabled(server, protocol));
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

    public static void main(String... args) {
        AnsiConsole.systemInstall();
        var server = "burgaud.com";

        Main m = new Main(false);
        System.out.println(ansi().fg(GREEN).a(server).reset());
        m.checkProtocol(server);

        AnsiConsole.systemUninstall();
    }
}
