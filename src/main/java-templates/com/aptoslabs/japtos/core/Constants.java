package com.aptoslabs.japtos.core;

public class Constants {
    public static final String VERSION = "${project.version}";
    public static final String GITHUB_REPO = "https://github.com/aptos-labs/japtos";

    private Constants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}
