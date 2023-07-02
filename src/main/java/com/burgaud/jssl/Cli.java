package com.burgaud.jssl;

import picocli.CommandLine.Help.Ansi;

public class Cli {

    public static void printBanner(String msg) { 
        System.out.println(Ansi.AUTO.string(String.format("@|green,bold %s|@", msg)));
    }

    public static void printWarning(String msg) {
        System.out.println(Ansi.AUTO.string(String.format("@|yellow ⚠️  %s|@", msg)));
    }

    public static void printError(String msg) {
        System.out.println(Ansi.AUTO.string(String.format("@|red ☢️  %s|@", msg)));
    }
    
    public static void printInfo(String msg) {
        System.out.println(Ansi.AUTO.string(String.format("@|green %s|@", msg)));
    }
    
    public static void printSuccess(String msg) {
        System.out.println(Ansi.AUTO.string(String.format("@|green ✅ %s|@", msg)));
    }
}

