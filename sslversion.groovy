@Grab('info.picocli:picocli-groovy:4.7.1')
import static picocli.CommandLine.*
import groovy.transform.Field
import picocli.CommandLine.Help.Ansi

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

ENABLED = "enabled"
NOT_ENABLED = "not enabled"

// ================================================================================
// Logic
// ================================================================================
boolean isProtocolEnabled(String server, String protocol, SSLSocketFactory sf, boolean verbose) {
    def (hostname, port) =  server.tokenize(':')
    port = (port ?: "443") as int

    Socket socket = new Socket()

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
    def msg
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

def sf = initSocketFactory()
def protocols = ["SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"]

expandedServers.each { server ->
    printSuccess "\n${server}:"
    try {
        protocols.each { protocol ->
            supported = isProtocolEnabled(server, protocol, sf, verbose)
            printProtocolSupport(protocol, supported)
        }
    }
    catch(java.net.SocketTimeoutException _) {
        printError("  timeout")
    }
    catch(java.net.UnknownHostException _) {
        printError("  unknown host")
    }
    catch(Exception e) {
        printError("  ${e}")
    }
}
