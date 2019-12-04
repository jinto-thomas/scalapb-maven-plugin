package io.ttj.maven.plugins;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;


@Mojo(name = "compile")
public class ScalaCompileMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter(defaultValue = "false")
    private boolean skip;

    /***
     * Default protoc version is v3.8.0
     */
    @Parameter(defaultValue = "v380")
    private String protocVersion;

    /**
     * Input directory containing *.proto files.
     * Defaults to <code>${project.basedir}/src/main/proto</code>.
     *
     * @parameter property="inputDirectory"
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/proto")
    private File inputDirectory;

    /**
     * Additional include directories.
     *
     * @parameter property="includeDirectories"
     */
    @Parameter
    private File[] includeDirectories;

    /**
     * Add proto sources as resources.
     * Defaults to <code>false</code>.
     *
     * @parameter property="addProtoSources"
     */
    @Parameter(defaultValue = "false")
    private boolean addProtoSources;

    /**
     * Output directory for Scala generated classes.
     * Defaults to <code>${project.build.directory}/generated-sources/protobuf</code>.
     *
     * @parameter property="outputDirectory"
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/protobuf")
    private File outputDirectory;

    /**
     * Set to true if output packages need to be flatten.
     * Defaults to <code>false</code>.
     *
     * @parameter property="flatPackage"
     */
    @Parameter(defaultValue = "false")
    private boolean flatPackage;

    /**
     * Set to true if Java classes needs to be generated.
     * Defaults to <code>false</code>.
     *
     * @parameter property="javaOutput"
     */
    @Parameter(defaultValue = "false")
    private boolean javaOutput;

    /**
     * Set to true if Java conversions are needed.
     * Defaults to <code>false</code>.
     *
     * @parameter property="javaConversions"
     */
    @Parameter(defaultValue = "false")
    private boolean javaConversions;

    /**
     * Output directory for Java generated classes.
     * Defaults to <code>${project.build.directory}/generated-sources/protobuf</code>.
     *
     * @parameter property="javaOutputDirectory"
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/protobuf")
    private File javaOutputDirectory;

    /**
     * Set to true if grpc related classes are needed
     * Defaults to <code>false</code>.
     *
     * @parameter property="grpc"
     */
    @Parameter(defaultValue = "false")
    private boolean grpc;


    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skip flag set, skipping protobuf compilation.");
            return;
        }

        Path protoPath = Paths.get(inputDirectory.toURI());
        getLog().info("Reading proto files in '" + protoPath + "'.");

        Path[] includeDirectoriesPaths = ArrayUtils.toArray();
        if (includeDirectories != null) {
            includeDirectoriesPaths = Arrays.stream(includeDirectories).map(f -> {
                getLog().info("Including proto files from '" + protoPath + "'.");
                return Paths.get(f.toURI());
            }).toArray(Path[]::new);
        }

        Path scalaOutPath = Paths.get(outputDirectory.toURI());
        getLog().info("Writing Scala files in '" + scalaOutPath + "'.");
        project.addCompileSourceRoot(scalaOutPath.toString());

        if (addProtoSources) {
            projectHelper.addResource(
                    project,
                    inputDirectory.getAbsolutePath(),
                    Collections.singletonList("**/*.proto"),
                    Collections.emptyList()
            );
        }

        Path javaOutPath = null;
        if (javaOutput) {
            javaOutPath = Paths.get(javaOutputDirectory.toURI());
            getLog().info("Writing Java files in '" + javaOutPath + "'.");
            project.addCompileSourceRoot(javaOutPath.toString());
        }

        try {
            Files.createDirectories(scalaOutPath);
            if (javaOutput && javaOutPath != null) {
                Files.createDirectories(javaOutPath);
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Error creating output directories", ioe);
        }


        try {
            ProtoCompiler.compile(
                    protocVersion,
                    protoPath,
                    includeDirectoriesPaths,
                    scalaOutPath,
                    flatPackage, grpc,
                    javaConversions,
                    javaOutput,
                    javaOutPath
            );
        } catch (Exception e) {
            throw new MojoExecutionException("Error compiling protobuf files", e);
        }
    }
}
