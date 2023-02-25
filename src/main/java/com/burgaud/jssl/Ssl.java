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

public class Ssl {
    private SSLSocketFactory factory = null;
    boolean debug = false;

    Ssl(boolean debug) {
        this.debug = debug;
        this.factory = initSocketFactory();
    }

    public boolean isProtocolEnabled(String server, String protocol) throws Exception {
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
        String[] cf = sslSocket.getSupportedCipherSuites();
        sslSocket.setEnabledCipherSuites(cf);

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
}
