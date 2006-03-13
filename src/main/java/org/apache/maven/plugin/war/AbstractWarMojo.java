package org.apache.maven.plugin.war;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AbstractWarMojo
    extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDirectory;

    /**
     * The directory where the webapp is built.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File webappDirectory;

    /**
     * Single directory for extra files to include in the WAR.
     *
     * @parameter expression="${basedir}/src/main/webapp"
     * @required
     */
    private File warSourceDirectory;

    /**
     * The path to the web.xml file to use.
     *
     * @parameter expression="${maven.war.webxml}"
     */
    private String webXml;

    /**
     * The path to the context.xml file to use.
     *
     * @parameter expression="${maven.war.contextxml}"
     */
    private String contextXml;

    /**
     * Directory to unpack dependent WARs into if needed
     *
     * @parameter expression="${project.build.directory}/war/work"
     * @required
     */
    private File workDirectory;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */
    protected ArchiverManager archiverManager;

    public static final String WEB_INF = "WEB-INF";

    public static final String META_INF = "META-INF";

    /**
     * The comma separated list of tokens to include in the WAR.
     * Default is '**'.
     *
     * @parameter alias="includes"
     */
    private String warSourceIncludes = "**";

    /**
     * The comma separated list of tokens to exclude from the WAR.
     *
     * @parameter alias="excludes"
     */
    private String warSourceExcludes;

    /**
     * The comma separated list of tokens to include when doing
     * a war overlay.
     * Default is '**'
     *
     * @parameter
     */
    private String dependentWarIncludes = "**";

    /**
     * The comma separated list of tokens to exclude when doing
     * a way overlay.
     *
     * @parameter
     */
    private String dependentWarExcludes;

    private static final String[] EMPTY_STRING_ARRAY = {};

    public abstract void execute()
        throws MojoExecutionException;

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public File getClassesDirectory()
    {
        return classesDirectory;
    }

    public void setClassesDirectory( File classesDirectory )
    {
        this.classesDirectory = classesDirectory;
    }

    public File getWebappDirectory()
    {
        return webappDirectory;
    }

    public void setWebappDirectory( File webappDirectory )
    {
        this.webappDirectory = webappDirectory;
    }

    public File getWarSourceDirectory()
    {
        return warSourceDirectory;
    }

    public void setWarSourceDirectory( File warSourceDirectory )
    {
        this.warSourceDirectory = warSourceDirectory;
    }

    public String getWebXml()
    {
        return webXml;
    }

    public void setWebXml( String webXml )
    {
        this.webXml = webXml;
    }

    public String getContextXml()
    {
        return contextXml;
    }

    public void setContextXml( String contextXml )
    {
        this.contextXml = contextXml;
    }

    /**
     * Returns a string array of the excludes to be used
     * when assembling/copying the war.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List excludeList = new ArrayList();
        if ( StringUtils.isNotEmpty(warSourceExcludes) )
        {
            excludeList.addAll( Arrays.asList( StringUtils.split( warSourceExcludes, "," ) ) );
        }

        // if webXML is specified, omit the one in the source directory
        if ( getWebXml() != null && !"".trim().equals( getWebXml() ) )
        {
            excludeList.add( "**/" + WEB_INF + "/web.xml" );
        }

        // if contextXML is specified, omit the one in the source directory
        if ( StringUtils.isNotEmpty( getContextXml() ) )
        {
            excludeList.add( "**/" + META_INF + "/context.xml" );
        }


        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the war.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( warSourceIncludes ), "," );
    }

    /**
     * Returns a string array of the excludes to be used
     * when adding dependent wars as an overlay onto this war.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getDependentWarExcludes() {
        if ( StringUtils.isNotEmpty(dependentWarExcludes) )
        {
            return StringUtils.split( dependentWarExcludes, "," );
        }

        return (String[]) EMPTY_STRING_ARRAY.clone();
    }

    /**
     * Returns a string array of the includes to be used
     * when adding dependent wars as an overlay onto this war.
     *
     * @return an array of tokens to include
     */
    protected String[] getDependentWarIncludes() {
        return StringUtils.split( StringUtils.defaultString( dependentWarIncludes ), "," );
    }

    public void buildExplodedWebapp( File webappDirectory )
        throws MojoExecutionException
    {
        getLog().info( "Exploding webapp..." );

        webappDirectory.mkdirs();

        File webinfDir = new File( webappDirectory, WEB_INF );
        webinfDir.mkdirs();

        File metainfDir = new File( webappDirectory, META_INF );
        metainfDir.mkdirs();

        try
        {
            copyResources( getWarSourceDirectory(), webappDirectory, getWebXml(), getContextXml() );

            buildWebapp( getProject(), webappDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode webapp...", e );
        }
    }

    /**
     * Copies webapp resources from the specified directory.
     * <p/>
     * Note that the <tt>webXml</tt> parameter could be null and may
     * specify a file which is not named <tt>web.xml<tt>. If the file
     * exists, it will be copied to the <tt>META-INF</tt> directory and
     * renamed accordingly.
     *
     * @param sourceDirectory the source directory
     * @param webappDirectory the target directory
     * @param webXml the path to a custom web.xml
     * @throws java.io.IOException if an error occured while copying resources
     */
    public void copyResources( File sourceDirectory, File webappDirectory, String webXml, String contextXml )
        throws IOException
    {
        if ( !sourceDirectory.equals( webappDirectory ) )
        {
            getLog().info( "Copy webapp resources to " + webappDirectory.getAbsolutePath() );
            if ( getWarSourceDirectory().exists() )
            {
                String[] fileNames = getWarFiles( sourceDirectory );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    FileUtils.copyFile( new File( sourceDirectory, fileNames[i] ),
                                        new File( webappDirectory, fileNames[i] ) );
                }
            }

            if ( StringUtils.isNotEmpty( webXml ) )
            {
                //rename to web.xml
                File webinfDir = new File( webappDirectory, WEB_INF );
                FileUtils.copyFile( new File( webXml ), new File( webinfDir, "/web.xml" ) );
            }

            if ( StringUtils.isNotEmpty( contextXml ) )
            {
                //rename to web.xml
                File metainfDir = new File( webappDirectory, META_INF );
                FileUtils.copyFile( new File( contextXml ), new File( metainfDir, "/context.xml" ) );
            }
        }
    }

    /**
     * Builds the webapp for the specified project.
     * <p/>
     * Classes, libraries and tld files are copied to
     * the <tt>webappDirectory</tt> during this phase.
     *
     * @param project the maven project
     * @param webappDirectory
     * @throws java.io.IOException if an error occured while building the webapp
     */
    public void buildWebapp( MavenProject project, File webappDirectory )
        throws MojoExecutionException, IOException
    {
        getLog().info( "Assembling webapp " + project.getArtifactId() + " in " + webappDirectory );

        File libDirectory = new File( webappDirectory, WEB_INF + "/lib" );

        File tldDirectory = new File( webappDirectory, WEB_INF + "/tld" );

        File webappClassesDirectory = new File( webappDirectory, WEB_INF + "/classes" );

        if ( getClassesDirectory().exists() && (!getClassesDirectory().equals(webappClassesDirectory)))
        {
            FileUtils.copyDirectoryStructure( getClassesDirectory(), webappClassesDirectory );
        }

        Set artifacts = project.getArtifacts();

        List dependentWarDirectories = new ArrayList();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();

            // TODO: utilise appropriate methods from project builder
            ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
            if ( !artifact.isOptional() && filter.include( artifact ) )
            {
                String type = artifact.getType();
                if ( "tld".equals( type ) )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), tldDirectory );
                }
                else if ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) )
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), libDirectory );
                }
                else if ( "par".equals( type ) )
                {
                    String newName = artifact.getFile().getName();
                    newName = newName.substring( 0, newName.lastIndexOf('.') ) + ".jar";

                    getLog().debug( "Copying " + artifact.getFile() + " to " + new File( libDirectory, newName ) );

                    FileUtils.copyFile( artifact.getFile(), new File( libDirectory, newName ) );
                }
                else if ( "war".equals( type ) )
                {
                    dependentWarDirectories.add( unpackWarToTempDirectory( artifact ) );
                }
                else
                {
                    getLog().debug( "Skipping artifact of type " + type + " for WEB-INF/lib" );
                }
            }
        }

        if ( dependentWarDirectories.size() > 0 )
        {
            getLog().info( "Overlaying " + dependentWarDirectories.size() + " war(s)." );

            // overlay dependent wars
            for ( Iterator iter = dependentWarDirectories.iterator(); iter.hasNext(); )
            {
                copyDependentWarContents( (File) iter.next(), webappDirectory );
            }
        }
    }

    /**
     * Unpacks war artifacts into a temporary directory inside <tt>workDirectory</tt>
     * named with the name of the war.
     *
     * @param artifact War artifact to unpack.
     * @return Directory containing the unpacked war.
     * @throws MojoExecutionException
     */
    private File unpackWarToTempDirectory( Artifact artifact )
        throws MojoExecutionException
    {
        String name = artifact.getFile().getName();
        File tempLocation = new File( workDirectory, name.substring( 0, name.length() - 4 ) );

        boolean process = false;
        if ( !tempLocation.exists() )
        {
            tempLocation.mkdirs();
            process = true;
        }
        else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
        {
            process = true;
        }

        if ( process )
        {
            File file = artifact.getFile();
            try
            {
                unpack( file, tempLocation );
            }
            catch ( NoSuchArchiverException e )
            {
                this.getLog().info( "Skip unpacking dependency file with unknown extension: " + file.getPath() );
            }
        }

        return tempLocation;
    }

    /**
     * Unpacks the archive file.
     *
     * @param file File to be unpacked.
     * @param location Location where to put the unpacked files.
     */
    private void unpack( File file, File location )
        throws MojoExecutionException, NoSuchArchiverException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() ).toLowerCase();

        try
        {
            UnArchiver unArchiver;
            unArchiver = this.archiverManager.getUnArchiver( archiveExt );
            unArchiver.setSourceFile( file );
            unArchiver.setDestDirectory( location );
            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

    /**
     * Recursively copies contents of <tt>srcDir</tt> into <tt>targetDir</tt>.
     * This will not overwrite any existing files.
     *
     * @param srcDir Directory containing unpacked dependent war contents
     * @param targetDir Directory to overlay srcDir into
     */
    private void copyDependentWarContents( File srcDir, File targetDir )
        throws MojoExecutionException
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( srcDir );
        scanner.setExcludes( getDependentWarExcludes() );
        scanner.addDefaultExcludes();

        scanner.setIncludes( getDependentWarIncludes() );

        scanner.scan();

        String[] dirs = scanner.getIncludedDirectories();
        for ( int j = 0; j < dirs.length; j++ )
        {
            new File( targetDir, dirs[j] ).mkdirs();
        }

        String[] files = scanner.getIncludedFiles();

        for ( int j = 0; j < files.length; j++ )
        {
            File targetFile = new File( targetDir, files[j] );

            // Do not overwrite existing files.
            if (!targetFile.exists())
            {
                try
                {
                    targetFile.getParentFile().mkdirs();
                    FileUtils.copyFile( new File( srcDir, files[j] ), targetFile );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error copying file '" + files[j] + "' to '" + targetFile + "'", e );
                }
            }
        }
    }

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param sourceDir the directory to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getWarFiles( File sourceDir )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( sourceDir );
        scanner.setExcludes( getExcludes() );
        scanner.addDefaultExcludes();

        scanner.setIncludes( getIncludes() );

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
