package com.burgaud.jssl;

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
}
