package com.burgaud.jssl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.*;

public class Format {
    private static final String ENABLED = "enabled";
    private static final String NOT_ENABLED = "not enabled";

    public static void printProtocolSupport(String protocol, boolean enabled) {
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

    public static String[] boolToString(boolean[] bs) {
        String[] bsStr = new String[bs.length];
        int idx = 0;
        for(var b : bs) {
            bsStr[idx++] = String.valueOf(b);
        }
        return bsStr;
    }
    public static void csv(List<ServerProtocols> results) {
        System.out.println("hostname, sslv3, tlsv1, tlsv1.1, tlsv1.2, tlsv1.3, error");
        results.forEach((res) -> {
            System.out.printf("%s,", res.server());
            if (res.error().length() > 0) {
                System.out.printf(",,,,,%s\n", res.error());
            }
            else {
                System.out.printf(String.join(",", boolToString(res.supported())));
                System.out.println((","));
            }
        });
    }
}
