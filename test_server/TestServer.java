import java.io.*;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpsServer;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import com.sun.net.httpserver.*;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;

public class TestServer {

    public static class DummyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange x) throws IOException {
            String resp = "Hello!";
            HttpsExchange exchange = (HttpsExchange) x;
            x.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            x.sendResponseHeaders(200, resp.getBytes().length);
            OutputStream os = x.getResponseBody();
            os.write(resp.getBytes());
            os.close();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 9999;
        var ksFile = "testkey.jks";
        try {
            InetSocketAddress address = new InetSocketAddress(port);
            HttpsServer server = HttpsServer.create(address, 0);
            SSLContext context = SSLContext.getInstance("SSL");

            // Keystore
            char[] passwd = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream is = new FileInputStream(ksFile);
            ks.load(is, passwd);

            // Key manager
            KeyManagerFactory km = KeyManagerFactory.getInstance("SunX509");
            km.init(ks, passwd);

            // Trust manager
            TrustManagerFactory tm = TrustManagerFactory.getInstance("SunX509");
            tm.init(ks);

            // setup the HTTPS context and parameters
            context.init(km.getKeyManagers(), tm.getTrustManagers(), null);
            server.setHttpsConfigurator(new HttpsConfigurator(context) {
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext context = getSSLContext();
                        SSLEngine engine = context.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        String[] protocols = params.getProtocols();
                        for (String p : protocols) {
                          System.out.printf("%s is enabled\n", p);
                        }

                        // Set the SSL parameters
                        SSLParameters sslParams = context.getSupportedSSLParameters();
                        params.setSSLParameters(sslParams);
                        System.out.println("The HTTPS server is connected");

                    } catch (Exception ex) {
                        System.out.println("Failed to create the HTTPS port");
                    }
                }
            });
            server.createContext("/hello", new DummyHandler());
            server.setExecutor(null); // creates a default executor
            System.out.printf("Starting HTTPS server at localhost:%s\n", port);
            server.start();

        } catch (Exception exception) {
            System.out.printf("Failed to start HTTPS server at localhost:%s\n", port);
            exception.printStackTrace();
        }
    }

}