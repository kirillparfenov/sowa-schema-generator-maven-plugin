package dev.parfenov.sowa.schema.plugin.classparser;

import org.apache.maven.project.MavenProject;

public class AllClassPathParser extends AbstractClassParser {

    public AllClassPathParser(MavenProject project) {
        super(project);
    }
}
