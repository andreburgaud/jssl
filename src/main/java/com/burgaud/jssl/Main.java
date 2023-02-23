package com.burgaud.jssl;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

class Main {
    public static void main(String... args) {
        int exitCode = new CommandLine(new Jssl()).execute(args);
        System.exit(exitCode);
    }
}
