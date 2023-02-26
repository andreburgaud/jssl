package com.burgaud.jssl;

import picocli.CommandLine;

class Main {
    public static void main(String... args) {
        int exitCode = new CommandLine(new Jssl()).execute(args);
        System.exit(exitCode);
    }
}
