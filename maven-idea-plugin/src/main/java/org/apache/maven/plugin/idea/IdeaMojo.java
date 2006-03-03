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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.manager.WagonManager;

import java.util.List;

/**
 * Goal for generating IDEA files from a POM.
 * This plug-in provides the ability to generate IDEA project files (.ipr and .iws files) for IDEA
 *
 * @goal idea
 * @execute phase="generate-sources"
 * @todo use dom4j or something. Xpp3Dom can't cope properly with entities and so on
 */
public class IdeaMojo
    extends AbstractIdeaMojo
{
    /**
     * The Maven Project.
     *
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * The reactor projects in a multi-module build.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.manager.WagonManager}"
     * @required
     * @readonly
     */
    private WagonManager wagonManager;

    /**
     * Whether to link the reactor projects as dependency modules or as libraries.
     *
     * @parameter expression="${linkModules}" default-value="true"
     */
    private boolean linkModules;

    /**
     * Whether to use full artifact names when referencing libraries.
     *
     * @parameter expression="${useFullNames}" default-value="false"
     */
    private boolean useFullNames;

    /**
     * Switch to enable or disable the inclusion of sources and javadoc references to the project's library
     *
     * @parameter expression="${useClassifiers}" default-value="false"
     */
    private boolean useClassifiers;

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

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doDependencyResolution( project, artifactFactory, artifactResolver, localRepo,
                                    artifactMetadataSource );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        rewriteModule();

        if ( project.isExecutionRoot() )
        {
            rewriteProject();

            rewriteWorkspace();
        }
    }

    private void rewriteModule()
        throws MojoExecutionException
    {
        IdeaModuleMojo mojo = new IdeaModuleMojo();

        mojo.initParam( project, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, getLog(), overwrite, executedProject, reactorProjects, wagonManager, linkModules, useFullNames, useClassifiers, sourceClassifier, javadocClassifier );

        mojo.execute();
    }

    private void rewriteProject()
        throws MojoExecutionException
    {
        IdeaProjectMojo mojo = new IdeaProjectMojo();

        mojo.initParam( project, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, getLog(), overwrite, jdkName, jdkLevel, wildcardResourcePatterns, ideaVersion );

        mojo.execute();
    }

    private void rewriteWorkspace()
        throws MojoExecutionException
    {
        IdeaWorkspaceMojo mojo = new IdeaWorkspaceMojo();

        mojo.initParam( project, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, getLog(), overwrite );

        mojo.execute();
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }
}
