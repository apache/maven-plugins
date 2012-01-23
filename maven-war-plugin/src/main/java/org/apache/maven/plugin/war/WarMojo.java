package org.apache.maven.plugin.war;

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

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.war.util.ClassesPackager;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Build a WAR file.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal war
 * @phase package
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class WarMojo
    extends AbstractWarMojo
{
    /**
     * The directory for the generated WAR.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the generated WAR.
     *
     * @parameter default-value="${project.build.finalName}"
     * @required
     */
    private String warName;

    /**
     * Classifier to add to the generated WAR. If given, the artifact will be an attachment instead.
     * The classifier will not be applied to the JAR file of the project - only to the WAR file.
     *
     * @parameter
     */
    private String classifier;

    /**
     * The comma separated list of tokens to exclude from the WAR before
     * packaging. This option may be used to implement the skinny WAR use
     * case. Note that you can use the Java Regular Expressions engine to
     * include and exclude specific pattern using the expression %regex[].
     * Hint: read the about (?!Pattern).
     *
     * @parameter
     * @since 2.1-alpha-2
     */
    private String packagingExcludes;

    /**
     * The comma separated list of tokens to include in the WAR before
     * packaging. By default everything is included. This option may be used
     * to implement the skinny WAR use case. Note that you can use the
     * Java Regular Expressions engine to include and exclude specific pattern
     * using the expression %regex[].
     *
     * @parameter
     * @since 2.1-beta-1
     */
    private String packagingIncludes;

    /**
     * The WAR archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="war"
     */
    private WarArchiver warArchiver;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to install or
     * deploy it to the local repository instead of the default one in an execution.
     *
     * @parameter expression="${primaryArtifact}" default-value="true"
     */
    private boolean primaryArtifact = true;

    /**
     * Whether or not to fail the build if the <code>web.xml</code> file is missing. Set to <code>false</code>
     * if you want you WAR built without a <code>web.xml</code> file.
     * This may be useful if you are building an overlay that has no web.xml file.
     *
     * @parameter expression="${failOnMissingWebXml}" default-value="true"
     * @since 2.1-alpha-2
     */
    private boolean failOnMissingWebXml = true;

    /**
     * Whether classes (that is the content of the WEB-INF/classes directory) should be attached to the
     * project as an additional artifact.
     * <p>By default the
     * classifier for the additional artifact is 'classes'. 
     * You can change it with the
     * <code><![CDATA[<classesClassifier>someclassifier</classesClassifier>]]></code>
     * parameter.
     * </p><p>
     * If this parameter true, another project can depend on the classes
     * by writing something like:
     * <pre><![CDATA[<dependency>
     *   <groupId>myGroup</groupId>
     *   <artifactId>myArtifact</artifactId>
     *   <version>myVersion</myVersion>
     *   <classifier>classes</classifier>
     * </dependency>]]></pre></p>
     * @parameter default-value="false"
     * @since 2.1-alpha-2
     */
    private boolean attachClasses = false;

    /**
     * The classifier to use for the attached classes artifact.
     *
     * @parameter default-value="classes"
     * @since 2.1-alpha-2
     */
    private String classesClassifier = "classes";

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------


    /**
     * Executes the WarMojo on the current project.
     *
     * @throws MojoExecutionException if an error occurred while building the webapp
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
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
     * @throws IOException            if an error occurred while copying files
     * @throws ArchiverException      if the archive could not be created
     * @throws ManifestException      if the manifest could not be created
     * @throws DependencyResolutionRequiredException
     *                                if an error occurred while resolving the dependencies
     * @throws MojoExecutionException if the execution failed
     * @throws MojoFailureException   if a fatal exception occurred
     */
    private void performPackaging( File warFile )
        throws IOException, ArchiverException, ManifestException, DependencyResolutionRequiredException,
        MojoExecutionException, MojoFailureException
    {
        getLog().info( "Packaging webapp" );

        buildExplodedWebapp( getWebappDirectory() );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( warArchiver );

        archiver.setOutputFile( warFile );

        getLog().debug(
            "Excluding " + Arrays.asList( getPackagingExcludes() ) + " from the generated webapp archive." );
        getLog().debug(
            "Including " + Arrays.asList( getPackagingIncludes() ) + " in the generated webapp archive." );

        warArchiver.addDirectory( getWebappDirectory(), getPackagingIncludes(), getPackagingExcludes() );

        final File webXmlFile = new File( getWebappDirectory(), "WEB-INF/web.xml" );
        if ( webXmlFile.exists() )
        {
            warArchiver.setWebxml( webXmlFile );
        }
        if ( !failOnMissingWebXml )
        {
            getLog().debug( "Build won't fail if web.xml file is missing." );
            // The flag is wrong in plexus-archiver so it will need to be fixed at some point
            warArchiver.setIgnoreWebxml( false );
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
                FileUtils.copyFile(getJarArchiver().getDestFile(), targetClassesFile);
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

        String classifier = this.classifier;
        if ( classifier != null )
        {
            projectHelper.attachArtifact( getProject(), "war", classifier, warFile );
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


    protected File getTargetWarFile()
    {
        return getTargetFile( new File( getOutputDirectory() ), getWarName(), getClassifier(), "war" );

    }

    protected File getTargetClassesFile()
    {
        return getTargetFile( new File( getOutputDirectory() ), getWarName(), getClassesClassifier(), "jar" );
    }

    // Getters and Setters

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

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

    public void setPackagingExcludes( String packagingExcludes )
    {
        this.packagingExcludes = packagingExcludes;
    }

    public String[] getPackagingIncludes()
    {
        if ( StringUtils.isEmpty( packagingIncludes ) )
        {
            return new String[]{"**"};
        }
        else
        {
            return StringUtils.split( packagingIncludes, "," );
        }
    }

    public void setPackagingIncludes( String packagingIncludes )
    {
        this.packagingIncludes = packagingIncludes;
    }

    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public String getWarName()
    {
        return warName;
    }

    public void setWarName( String warName )
    {
        this.warName = warName;
    }

    public WarArchiver getWarArchiver()
    {
        return warArchiver;
    }

    public void setWarArchiver( WarArchiver warArchiver )
    {
        this.warArchiver = warArchiver;
    }

    public MavenProjectHelper getProjectHelper()
    {
        return projectHelper;
    }

    public void setProjectHelper( MavenProjectHelper projectHelper )
    {
        this.projectHelper = projectHelper;
    }

    public boolean isPrimaryArtifact()
    {
        return primaryArtifact;
    }

    public void setPrimaryArtifact( boolean primaryArtifact )
    {
        this.primaryArtifact = primaryArtifact;
    }

    public boolean isAttachClasses()
    {
        return attachClasses;
    }

    public void setAttachClasses( boolean attachClasses )
    {
        this.attachClasses = attachClasses;
    }

    public String getClassesClassifier()
    {
        return classesClassifier;
    }

    public void setClassesClassifier( String classesClassifier )
    {
        this.classesClassifier = classesClassifier;
    }

    public boolean isFailOnMissingWebXml()
    {
        return failOnMissingWebXml;
    }

    public void setFailOnMissingWebXml( boolean failOnMissingWebXml )
    {
        this.failOnMissingWebXml = failOnMissingWebXml;
    }
}
