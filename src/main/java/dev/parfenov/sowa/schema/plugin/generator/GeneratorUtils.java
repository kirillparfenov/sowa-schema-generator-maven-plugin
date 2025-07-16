package dev.parfenov.sowa.schema.plugin.generator;

public class GeneratorUtils {

    private static final String PREFIX = "./";
    private static final String SUFFIX = ".json";

    private GeneratorUtils() {}

    public static String changeRefPath(String refValue) {
        return PREFIX
                .concat(refValue.substring(refValue.lastIndexOf("/") + 1))
                .concat(SUFFIX);
    }
}
