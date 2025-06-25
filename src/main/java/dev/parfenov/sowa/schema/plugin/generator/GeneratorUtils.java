package dev.parfenov.sowa.schema.plugin.generator;

public class GeneratorUtils {

    private static final String PREFIX = "./";
    private static final String SUFFIX = ".json";

    private GeneratorUtils() {}

    public static String changeRefPath(String refValue) {
        var lastSlash = refValue.lastIndexOf("/");
        return PREFIX
                .concat(refValue.substring(lastSlash + 1))
                .concat(SUFFIX);
    }
}
