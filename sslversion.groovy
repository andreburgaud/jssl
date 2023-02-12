@Grab('info.picocli:picocli-groovy:4.7.1')
import static picocli.CommandLine.*
import groovy.transform.Field
import picocli.CommandLine.Help.Ansi

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import java.net.Socket

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

import javax.net.ssl.X509TrustManager
import javax.net.ssl.TrustManager
import javax.net.ssl.HostnameVerifier

// ================================================================================
// CLI
// ================================================================================
@Command(
    header = [
        $/@|bold,green  ___ ___ _                  _           |@/$,
        $/@|bold,green / __/ __| | __ _____ _ _ __(_)___ _ _   |@/$,
        $/@|bold,green \__ \__ \ |_\ V / -_) '_(_-< / _ \ ' \  |@/$,
        $/@|bold,green |___/___/____\_/\___|_| /__/_\___/_||_| |@/$,
        $/@|bold,green                                         |@/$
    ],
    name = 'sslversion.sh', mixinStandardHelpOptions = true, version = 'sslversion 0.2.0',
    description = 'Display the SSL/TLS protocol versions enabled on a server.')
@picocli.groovy.PicocliScript

@Parameters(arity="1", description = 'Server(s) or file(s) containing lists of servers.')
@Field String[] servers

@Option(names = [ '-d', '--debug'], description = 'Display debug information.')
@Field boolean debug = false

@Option(names = [ '-w', '--worker'], description = 'Number of workers (default: 1).')
@Field int workers = 1

ENABLED = "enabled"
NOT_ENABLED = "not enabled"

// ================================================================================
// Logic
// ================================================================================
boolean isProtocolEnabled(String server, String protocol, SSLSocketFactory sf, boolean debug) {
    def (hostname, port) =  server.tokenize(':')
    port = (port ?: "443") as int

    Socket socket = new Socket()
    socket.setSoTimeout(2000)

    socketAddr = new InetSocketAddress(hostname, port)
    socket.connect(socketAddr, 2000)

    SSLSocket sslSocket = (SSLSocket) sf.createSocket(socket, hostname, port, true)
    sslSocket.setEnabledProtocols((String[])["$protocol"])

    boolean res = false
    try {
        sslSocket.startHandshake()
        res = true
    }
    catch(Exception e) {
        if (debug) {
            println(">>> ${protocol}: ${e}")
        }
    }
    finally {
        sslSocket.close()
    }
    return res
}

def printInfo(String msg) {
    print Ansi.AUTO.string("@|green ${msg}|@")
}

def printWarning(String msg) {
    println Ansi.AUTO.string("@|bold,yellow ${msg}|@")
}

def printError(String msg) {
    println Ansi.AUTO.string("@|bold,red ${msg}|@")
}

def printSuccess(String msg) {
    println Ansi.AUTO.string("@|bold,green ${msg}|@")
}


def printProtocolSupport(String protocol, boolean enabled) {
    if (protocol == "SSLv2Hello") {
        protocol = "SSLv2"
    }

    print "  ${protocol}".padRight(12)

    switch(protocol) {
        case "SSLv2":
            if (enabled) {
                printError ENABLED
            }
            else {
                printSuccess NOT_ENABLED
            }
            break
        case "SSLv3":
            if (enabled) {
                printError ENABLED
            }
            else {
                printSuccess NOT_ENABLED
            }
            break
        case "TLSv1":
        case "TLSv1.1":
            if (enabled) {
                printWarning(ENABLED)
            }
            else {
                printSuccess(NOT_ENABLED)
            }
            break
        case "TLSv1.2":
        case "TLSv1.3":
            if (enabled) {
                printSuccess ENABLED
            }
            else {
                println NOT_ENABLED
            }
            break
        default:
            printWarning "unexpected protocol"
    }
}

def readServersFile(File file) {
    def lines = file.readLines().collect{ it.trim() }
    return lines.findAll { it.size() > 0 && !it.startsWith('#') }
}


def printSupportedProtocols() {
    s = SSLContext.default.supportedSSLParameters.protocols
    println s.join(' ')
}

SSLSocketFactory initSocketFactory() {
    // No validation
    def nullTrustManager = [
        checkClientTrusted: { chain, authType -> },
        checkServerTrusted: { chain, authType -> },
        getAcceptedIssuers: { null }
    ]

    def nullHostnameVerifier = [
        verify: { hostname, session -> true }
    ]
    // End of no validation
    
    def sc = SSLContext.getInstance('TLSv1.3')
    sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
    return sc.getSocketFactory()
}

if (debug) {
    printSupportedProtocols()
}

expandedServers = []

// An argument can be a server (hostname:port) or a file containing a list of servers
servers.each { server ->
    File file = new File(server)
    if (file.exists()) {
        expandedServers += readServersFile(file)
    }
    else {
        expandedServers << server
    }
}

sf = initSocketFactory()
protocols = ["SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"]

def callback(server) {
    def res = [server: server, protocols: [], error: ""]
    try {
        for (protocol in protocols) {
            supported = isProtocolEnabled(server, protocol, sf, debug)
            res.protocols << supported
        }
    }
    catch(java.net.SocketTimeoutException _) {
        res.error = "timeout"
    }
    catch(java.net.UnknownHostException _) {
        res.error = "unknown host"
    }
    catch(Exception e) {
        res.error = "${e}"
    }
    return res
}

ExecutorService executorService = Executors.newFixedThreadPool(workers)

List<Future> futures = expandedServers.collect { server ->
    if (debug) {
        println(">>> ${server}")
    }
    executorService.submit({ -> callback(server) } as Callable)
}

def spinner = "⣾⣽⣻⢿⡿⣟⣯⣷"
def results = [:]
futures.eachWithIndex { future, idx ->
    printInfo "${spinner[idx%8]}\r"
    result = future.get()
    results[result.server] = result
}

print(" \r")

executorService.shutdown()

if (debug) {
    println(">>> ${results}")
}
    
results.each { k, v ->
    println()
    printSuccess "${k}:"
    if (v.error.size() > 0) {
        print "  Error".padRight(12)
        printError("${v.error}")
    }
    else {
        protocols.eachWithIndex { enabled, idx ->
            printProtocolSupport(enabled, v.protocols[idx])
        }
    }
}
