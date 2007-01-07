package org.apache.maven.plugin.idea;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Goal for generating IDEA files from a POM.
 * This plug-in provides the ability to generate IDEA project files (.ipr, .iml and .iws files) for IDEA
 *
 * @goal idea
 * @execute phase="generate-sources"
 */
public class IdeaMojo
    extends AbstractIdeaMojo
{
    /**
     * The reactor projects in a multi-module build.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @component
     */
    private WagonManager wagonManager;

    /**
     * Whether to link the reactor projects as dependency modules or as libraries.
     *
     * @parameter expression="${linkModules}" default-value="true"
     */
    private boolean linkModules;

    /**
     * Specify the location of the deployment descriptor file, if one is provided
     *
     * @parameter expression="${deploymentDescriptorFile}"
     */
    private String deploymentDescriptorFile;

    /**
     * Whether to use full artifact names when referencing libraries.
     *
     * @parameter expression="${useFullNames}" default-value="false"
     */
    private boolean useFullNames;

    /**
     * Enables/disables the downloading of source attachments. Defaults to false.
     *
     * @parameter expression="${downloadSources}" default-value="false"
     */
    private boolean downloadSources;

    /**
     * Enables/disables the downloading of javadoc attachements. Defaults to false.
     *
     * @parameter expression="${downloadJavadocs}" default-value="false"
     */
    private boolean downloadJavadocs;

    /**
     * Sets the classifier string attached to an artifact source archive name
     *
     * @parameter expression="${sourceClassifier}" default-value="sources"
     */
    private String sourceClassifier;

    /**
     * Sets the classifier string attached to an artifact javadoc archive name
     *
     * @parameter expression="${javadocClassifier}" default-value="javadoc"
     */
    private String javadocClassifier;

    /**
     * Specify the name of the registered IDEA JDK to use
     * for the project.
     *
     * @parameter expression="${jdkName}"
     */
    private String jdkName;

    /**
     * Specify the version of the JDK to use for the project for the purpose of enabled assertions and 5.0 language features.
     * The default value is the specification version of the executing JVM.
     *
     * @parameter expression="${jdkLevel}"
     * @todo would be good to use the compilation source if possible
     */
    private String jdkLevel;

    /**
     * An optional set of Library objects that allow you to specify a comma separated list of source dirs, class dirs,
     * or to indicate that the library should be excluded from the module. For example:
     * <p/>
     * <pre>
     * &lt;libraries&gt;
     *  &lt;library&gt;
     *      &lt;name&gt;webwork&lt;/name&gt;
     *      &lt;sources&gt;file://$webwork$/src/java&lt;/sources&gt;
     *      &lt;!--
     *      &lt;classes&gt;...&lt;/classes&gt;
     *      &lt;exclude&gt;true&lt;/exclude&gt;
     *      --&gt;
     *  &lt;/library&gt;
     * &lt;/libraries&gt;
     * </pre>
     *
     * @parameter
     */
    private Library[] libraries;

    /**
     * A comma-separated list of directories that should be excluded. These directories are in addition to those
     * already excluded, such as target/classes. A common use of this is to exclude the entire target directory.
     *
     * @parameter
     */
    private String exclude;

    /**
     * Specify the resource pattern in wildcard format, for example "?*.xml;?*.properties".
     * Currently supports 4.x and 5.x.
     * The default value is any file without a java extension ("!?*.java").
     * Because IDEA doesn't distinguish between source and resources directories, this is needed.
     * Please note that the default value includes package.html files as it's not possible to exclude those.
     *
     * @parameter expression="${wildcardResourcePatterns}" default-value="!?*.java"
     */
    private String wildcardResourcePatterns;

    /**
     * Specify the version of idea to use.  This is needed to identify the default formatting of
     * project-jdk-name used by idea.  Currently supports 4.x and 5.x.
     * <p/>
     * This will only be used when parameter jdkName is not set.
     *
     * @parameter expression="${ideaVersion}"
     * default-value="5.x"
     */
    private String ideaVersion;

    /**
     * Causes the module libraries to use a short name for all dependencies. This is very convenient but has been
     * reported to cause problems with IDEA.
     *
     * @parameter default-value="false"
     */
    private boolean dependenciesAsLibraries;

    /**
     * Tell IntelliJ IDEA that this module is an IntelliJ IDEA Plugin
     *
     * @parameter default-value="false"
     */
    private boolean ideaPlugin;


    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doDependencyResolution( executedProject, localRepo );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        Set macros = new HashSet();
        rewriteModule( macros );

        if ( executedProject.isExecutionRoot() )
        {
            rewriteProject( macros );

            rewriteWorkspace();
        }
    }

    private void rewriteModule( Set macros )
        throws MojoExecutionException
    {
        IdeaModuleMojo mojo = new IdeaModuleMojo();

        mojo.initParam( executedProject, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, getLog(),
                        overwrite, executedProject, reactorProjects, wagonManager, linkModules, useFullNames,
                        downloadSources, sourceClassifier, downloadJavadocs, javadocClassifier, libraries, macros,
                        exclude, dependenciesAsLibraries, deploymentDescriptorFile, ideaPlugin, ideaVersion );

        mojo.rewriteModule();
    }

    private void rewriteProject( Set macros )
        throws MojoExecutionException
    {
        IdeaProjectMojo mojo = new IdeaProjectMojo();

        mojo.initParam( executedProject, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, getLog(),
                        overwrite, jdkName, jdkLevel, wildcardResourcePatterns, ideaVersion, macros );

        mojo.rewriteProject();
    }

    private void rewriteWorkspace()
        throws MojoExecutionException
    {
        IdeaWorkspaceMojo mojo = new IdeaWorkspaceMojo();

        mojo.initParam( executedProject, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, getLog(),
                        overwrite );

        mojo.rewriteWorkspace();
    }

    public void setProject( MavenProject project )
    {
        this.executedProject = project;
    }
}
