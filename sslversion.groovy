@Grab('info.picocli:picocli-groovy:4.7.1')
import static picocli.CommandLine.*
import groovy.transform.Field
import picocli.CommandLine.Help.Ansi

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
    name = 'sslversion.sh', mixinStandardHelpOptions = true, version = 'sslversion 0.1.0',
    description = 'Display the SSL/TLS protocol versions enabled on a server')
@picocli.groovy.PicocliScript

@Parameters(arity="1", description = 'Server(s) or file(s) containing lists of servers')
@Field String[] servers

@Option(names = [ '-v', '--verbose'], description = 'Display more information.')
@Field boolean verbose = false

@Option(names = [ '-w', '--worker'], description = 'Number of workers (default: 1).')
@Field int workers = 1

ENABLED = "enabled"
NOT_ENABLED = "not enabled"

// ================================================================================
// Logic
// ================================================================================
boolean isProtocolEnabled(String server, String protocol, SSLSocketFactory sf, boolean verbose) {
    def (hostname, port) =  server.tokenize(':')
    port = (port ?: "443") as int

    Socket socket = new Socket()
    socket.setSoTimeout(2000)

    socketAddr = new InetSocketAddress(hostname, port)
    socket.connect(socketAddr, 2000)

    SSLSocket sslSocket = (SSLSocket) sf.createSocket(socket, hostname, port, true)
    sslSocket.setEnabledProtocols((String[])["$protocol"])

    try {
        sslSocket.startHandshake()
        return true
    }
    catch(Exception e) {
        if (verbose) {
            println("  >>> ${protocol}: ${e}")
        }
        return false
    }
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
                printError(ENABLED)
            }
            else {
                printSuccess(NOT_ENABLED)
            }
            break
        case "SSLv3":
            if (enabled) {
                printError(ENABLED)
            }
            else {
                printSuccess(NOT_ENABLED)
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
                printSuccess(ENABLED)
            }
            else {
                println(NOT_ENABLED)
            }
            break
        default:
            printWarning("unexpected protocol")
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
    // Code needed to discard validation with self signed certificates
    def nullTrustManager = [
        checkClientTrusted: { chain, authType -> },
        checkServerTrusted: { chain, authType -> },
        getAcceptedIssuers: { null }
    ]

    def nullHostnameVerifier = [
        verify: { hostname, session -> true }
    ]
    // End of code self-signed certificates

    def sc = SSLContext.getInstance('TLSv1.3')
    sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
    return sc.getSocketFactory()
}

if (verbose) {
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
    def r = [server: server, error: ""]
    try {
        for (protocol in protocols) {
            supported = isProtocolEnabled(server, protocol, sf, verbose)
            switch (protocol) {
                case "SSLv2Hello":
                    r.sslv2 = supported
                    break
                case "SSLv3":
                    r.sslv3 = supported
                    break
                case "TLSv1":
                    r.tlsv1 = supported
                    break
                case "TLSv1.1":
                    r.tlsv11 = supported
                    break
                case "TLSv1.2":
                    r.tlsv12 = supported
                    break
                case "TLSv1.3":
                    r.tlsv13 = supported
                    break
            }
        }
    }
    catch(java.net.SocketTimeoutException _) {
        r.error = "timeout"
    }
    catch(java.net.UnknownHostException _) {
        r.error = "unknown host"
    }
    catch(Exception e) {
        r.error = "${e}"
    }
    return r
}

ExecutorService executorService = Executors.newFixedThreadPool(workers)

// RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardOldestPolicy()

// ExecutorService executorService = new ThreadPoolExecutor(workers, workers,
//     0L, TimeUnit.MILLISECONDS,
//     new LinkedBlockingQueue<>(expandedServers.size()),
//     handler)

List<Future> futures = expandedServers.collect { server ->
    if (verbose) {
        println(">>> ${server}")
    }
    executorService.submit({ -> callback(server) } as Callable)
}

//def spinner = '|/-\\'
def spinner = "⣾⣽⣻⢿⡿⣟⣯⣷"


def results = []
def i = 0
futures.each {
    print "Processing ${spinner[i%8]}\r"
    results << it.get()
    i += 1
}

println()

executorService.shutdown()

results.each { r ->
    println()
    printSuccess "${r.server}:"
    if (r.error.size() > 0) {
        printError("  Error: ${r.error}")
    }
    else {
        printProtocolSupport("SSLv2Hello", r.sslv2)
        printProtocolSupport("SSLv3", r.sslv3)
        printProtocolSupport("TLSv1", r.tlsv1)
        printProtocolSupport("TLSv1.1", r.tlsv11)
        printProtocolSupport("TLSv1.2", r.tlsv12)
        printProtocolSupport("TLSv1.3", r.tlsv13)
    }
}
