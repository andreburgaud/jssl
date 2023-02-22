package com.burgaud.jssl;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class Cli {
    public static void printBanner(String msg) { 
        System.out.println(ansi().fg(GREEN).a(msg).reset());
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
}

