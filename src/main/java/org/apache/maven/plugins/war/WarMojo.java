package org.apache.maven.plugins.war;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.war.util.ClassesPackager;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Build a WAR file.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "war", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
// CHECKSTYLE_ON: LineLength
public class WarMojo
    extends AbstractWarMojo
{
    /**
     * The directory for the generated WAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private String outputDirectory;

    /**
     * The name of the generated WAR.
     */
    @Parameter( defaultValue = "${project.build.finalName}", required = true, readonly = true )
    private String warName;

    /**
     * Classifier to add to the generated WAR. If given, the artifact will be an attachment instead. The classifier will
     * not be applied to the JAR file of the project - only to the WAR file.
     */
    @Parameter
    private String classifier;

    /**
     * The comma separated list of tokens to exclude from the WAR before packaging. This option may be used to implement
     * the skinny WAR use case. Note that you can use the Java Regular Expressions engine to include and exclude
     * specific pattern using the expression %regex[]. Hint: read the about (?!Pattern).
     *
     * @since 2.1-alpha-2
     */
    @Parameter
    private String packagingExcludes;

    /**
     * The comma separated list of tokens to include in the WAR before packaging. By default everything is included.
     * This option may be used to implement the skinny WAR use case. Note that you can use the Java Regular Expressions
     * engine to include and exclude specific pattern using the expression %regex[].
     *
     * @since 2.1-beta-1
     */
    @Parameter
    private String packagingIncludes;

    /**
     * The WAR archiver.
     */
    @Component( role = Archiver.class, hint = "war" )
    private WarArchiver warArchiver;

    /**
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or deploy
     * it to the local repository instead of the default one in an execution.
     */
    @Parameter( defaultValue = "true" )
    private boolean primaryArtifact;

    /**
     * Whether or not to fail the build if the <code>web.xml</code> file is missing. Set to <code>false</code> if you
     * want you WAR built without a <code>web.xml</code> file. This may be useful if you are building an overlay that
     * has no web.xml file.
     *
     * @since 2.1-alpha-2
     */
    @Parameter( defaultValue = "true" )
    private boolean failOnMissingWebXml;

    /**
     * Whether classes (that is the content of the WEB-INF/classes directory) should be attached to the project as an
     * additional artifact.
     * <p>
     * By default the classifier for the additional artifact is 'classes'. You can change it with the
     * <code><![CDATA[<classesClassifier>someclassifier</classesClassifier>]]></code> parameter.
     * </p>
     * <p>
     * If this parameter true, another project can depend on the classes by writing something like:
     * 
     * <pre>
     * <![CDATA[<dependency>
     *   <groupId>myGroup</groupId>
     *   <artifactId>myArtifact</artifactId>
     *   <version>myVersion</myVersion>
     *   <classifier>classes</classifier>
     * </dependency>]]>
     * </pre>
     * </p>
     *
     * @since 2.1-alpha-2
     */
    @Parameter( defaultValue = "false" )
    private boolean attachClasses;

    /**
     * The classifier to use for the attached classes artifact.
     *
     * @since 2.1-alpha-2
     */
    @Parameter( defaultValue = "classes" )
    private String classesClassifier;

    /**
     * You can skip the execution of the plugin if you need to. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     * 
     * @since 3.0.0
     */
    @Parameter( property = "maven.war.skip", defaultValue = "false" )
    private boolean skip;

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    /**
     * Executes the WarMojo on the current project.
     *
     * @throws MojoExecutionException if an error occurred while building the webapp
     * @throws MojoFailureException if an error.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( isSkip() )
        {
            getLog().info( "Skipping the execution." );
            return;
        }

        File warFile = getTargetWarFile();

        try
        {
            performPackaging( warFile );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "Error assembling WAR: " + e.getMessage(), e );
        }
        catch ( ManifestException e )
        {
            throw new MojoExecutionException( "Error assembling WAR", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error assembling WAR", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error assembling WAR: " + e.getMessage(), e );
        }
    }

    /**
     * Generates the webapp according to the <tt>mode</tt> attribute.
     *
     * @param warFile the target WAR file
     * @throws IOException if an error occurred while copying files
     * @throws ArchiverException if the archive could not be created
     * @throws ManifestException if the manifest could not be created
     * @throws DependencyResolutionRequiredException if an error occurred while resolving the dependencies
     * @throws MojoExecutionException if the execution failed
     * @throws MojoFailureException if a fatal exception occurred
     */
    private void performPackaging( File warFile )
        throws IOException, ManifestException, DependencyResolutionRequiredException, MojoExecutionException,
        MojoFailureException
    {
        getLog().info( "Packaging webapp" );

        buildExplodedWebapp( getWebappDirectory() );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( warArchiver );

        archiver.setOutputFile( warFile );

        // CHECKSTYLE_OFF: LineLength
        getLog().debug( "Excluding " + Arrays.asList( getPackagingExcludes() )
            + " from the generated webapp archive." );
        getLog().debug( "Including " + Arrays.asList( getPackagingIncludes() ) + " in the generated webapp archive." );
        // CHECKSTYLE_ON: LineLength

        warArchiver.addDirectory( getWebappDirectory(), getPackagingIncludes(), getPackagingExcludes() );

        final File webXmlFile = new File( getWebappDirectory(), "WEB-INF/web.xml" );
        if ( webXmlFile.exists() )
        {
            warArchiver.setWebxml( webXmlFile );
        }

        warArchiver.setRecompressAddedZips( isRecompressZippedFiles() );

        warArchiver.setIncludeEmptyDirs( isIncludeEmptyDirectories() );

        if ( !failOnMissingWebXml )
        {
            getLog().debug( "Build won't fail if web.xml file is missing." );
            warArchiver.setExpectWebXml( false );
        }

        // create archive
        archiver.createArchive( getSession(), getProject(), getArchive() );

        // create the classes to be attached if necessary
        if ( isAttachClasses() )
        {
            if ( isArchiveClasses() && getJarArchiver().getDestFile() != null )
            {
                // special handling in case of archived classes: MWAR-240
                File targetClassesFile = getTargetClassesFile();
                FileUtils.copyFile( getJarArchiver().getDestFile(), targetClassesFile );
                projectHelper.attachArtifact( getProject(), "jar", getClassesClassifier(), targetClassesFile );
            }
            else
            {
                ClassesPackager packager = new ClassesPackager();
                final File classesDirectory = packager.getClassesDirectory( getWebappDirectory() );
                if ( classesDirectory.exists() )
                {
                    getLog().info( "Packaging classes" );
                    packager.packageClasses( classesDirectory, getTargetClassesFile(), getJarArchiver(), getSession(),
                                             getProject(), getArchive() );
                    projectHelper.attachArtifact( getProject(), "jar", getClassesClassifier(), getTargetClassesFile() );
                }
            }
        }

        if ( this.classifier != null )
        {
            projectHelper.attachArtifact( getProject(), "war", this.classifier, warFile );
        }
        else
        {
            Artifact artifact = getProject().getArtifact();
            if ( primaryArtifact )
            {
                artifact.setFile( warFile );
            }
            else if ( artifact.getFile() == null || artifact.getFile().isDirectory() )
            {
                artifact.setFile( warFile );
            }
        }
    }

    /**
     * @param basedir The basedir
     * @param finalName The finalName
     * @param classifier The classifier.
     * @param type The type.
     * @return {@link File}
     */
    protected static File getTargetFile( File basedir, String finalName, String classifier, String type )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + "." + type );
    }

    /**
     * @return The war {@link File}
     */
    protected File getTargetWarFile()
    {
        return getTargetFile( new File( getOutputDirectory() ), getWarName(), getClassifier(), "war" );

    }

    /**
     * @return The target class {@link File}
     */
    protected File getTargetClassesFile()
    {
        return getTargetFile( new File( getOutputDirectory() ), getWarName(), getClassesClassifier(), "jar" );
    }

    // Getters and Setters

    /**
     * @return {@link #classifier}
     */
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * @param classifier {@link #classifier}
     */
    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    /**
     * @return The package excludes.
     */
    public String[] getPackagingExcludes()
    {
        if ( StringUtils.isEmpty( packagingExcludes ) )
        {
            return new String[0];
        }
        else
        {
            return StringUtils.split( packagingExcludes, "," );
        }
    }

    /**
     * @param packagingExcludes {@link #packagingExcludes}
     */
    public void setPackagingExcludes( String packagingExcludes )
    {
        this.packagingExcludes = packagingExcludes;
    }

    /**
     * @return The packaging includes.
     */
    public String[] getPackagingIncludes()
    {
        if ( StringUtils.isEmpty( packagingIncludes ) )
        {
            return new String[] { "**" };
        }
        else
        {
            return StringUtils.split( packagingIncludes, "," );
        }
    }

    /**
     * @param packagingIncludes {@link #packagingIncludes}
     */
    public void setPackagingIncludes( String packagingIncludes )
    {
        this.packagingIncludes = packagingIncludes;
    }

    /**
     * @return {@link #outputDirectory}
     */
    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @param outputDirectory {@link #outputDirectory}
     */
    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return {@link #warName}
     */
    public String getWarName()
    {
        return warName;
    }

    /**
     * @param warName {@link #warName}
     */
    public void setWarName( String warName )
    {
        this.warName = warName;
    }

    /**
     * @return {@link #warArchiver}
     */
    public WarArchiver getWarArchiver()
    {
        return warArchiver;
    }

    /**
     * @param warArchiver {@link #warArchiver}
     */
    public void setWarArchiver( WarArchiver warArchiver )
    {
        this.warArchiver = warArchiver;
    }

    /**
     * @return {@link #projectHelper}
     */
    public MavenProjectHelper getProjectHelper()
    {
        return projectHelper;
    }

    /**
     * @param projectHelper {@link #projectHelper}
     */
    public void setProjectHelper( MavenProjectHelper projectHelper )
    {
        this.projectHelper = projectHelper;
    }

    /**
     * @return {@link #primaryArtifact}
     */
    public boolean isPrimaryArtifact()
    {
        return primaryArtifact;
    }

    /**
     * @param primaryArtifact {@link #primaryArtifact}
     */
    public void setPrimaryArtifact( boolean primaryArtifact )
    {
        this.primaryArtifact = primaryArtifact;
    }

    /**
     * @return {@link #attachClasses}
     */
    public boolean isAttachClasses()
    {
        return attachClasses;
    }

    /**
     * @param attachClasses {@link #attachClasses}
     */
    public void setAttachClasses( boolean attachClasses )
    {
        this.attachClasses = attachClasses;
    }

    /**
     * @return {@link #classesClassifier}
     */
    public String getClassesClassifier()
    {
        return classesClassifier;
    }

    /**
     * @param classesClassifier {@link #classesClassifier}
     */
    public void setClassesClassifier( String classesClassifier )
    {
        this.classesClassifier = classesClassifier;
    }

    /**
     * @return {@link #failOnMissingWebXml}
     */
    public boolean isFailOnMissingWebXml()
    {
        return failOnMissingWebXml;
    }

    /**
     * @param failOnMissingWebXml {@link #failOnMissingWebXml}
     */
    public void setFailOnMissingWebXml( boolean failOnMissingWebXml )
    {
        this.failOnMissingWebXml = failOnMissingWebXml;
    }

    public boolean isSkip()
    {
        return skip;
    }
}
