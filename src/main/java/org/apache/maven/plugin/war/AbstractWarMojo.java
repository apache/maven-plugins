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
    private File webXml;

    /**
     * The path to the context.xml file to use.
     *
     * @parameter expression="${maven.war.containerConfigXML}"
     */
    private String containerConfigXML;

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

    public File getWebXml()
    {
        return webXml;
    }

    public void setWebXml( File webXml )
    {
        this.webXml = webXml;
    }

    public String getContainerConfigXML()
    {
        return containerConfigXML;
    }

    public void setContainerConfigXML( String containerConfigXML )
    {
        this.containerConfigXML = containerConfigXML;
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
        if ( StringUtils.isNotEmpty( warSourceExcludes ) )
        {
            excludeList.addAll( Arrays.asList( StringUtils.split( warSourceExcludes, "," ) ) );
        }

        // if webXML is specified, omit the one in the source directory
        if ( getWebXml() != null && !"".trim().equals( getWebXml() ) )
        {
            excludeList.add( "**/" + WEB_INF + "/web.xml" );
        }

        // if contextXML is specified, omit the one in the source directory
        if ( StringUtils.isNotEmpty( getContainerConfigXML() ) )
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
    protected String[] getDependentWarExcludes()
    {
        if ( StringUtils.isNotEmpty( dependentWarExcludes ) )
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
    protected String[] getDependentWarIncludes()
    {
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
            copyResources( getWarSourceDirectory(), webappDirectory, getWebXml(), getContainerConfigXML() );

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
     * @param webXml          the abstract path to the web.xml
     * @throws java.io.IOException if an error occured while copying resources
     */
    public void copyResources( File sourceDirectory, File webappDirectory, File webXml, String containerConfigXML )
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
                    copyFileIfModified( new File( sourceDirectory, fileNames[i] ),
                                                  new File( webappDirectory, fileNames[i] ) );
                }
            }

            if ( webXml != null )
            {
                //rename to web.xml
                File webinfDir = new File( webappDirectory, WEB_INF );
                copyFileIfModified( webXml, new File( webinfDir, "/web.xml" ) );
            }

            if ( StringUtils.isNotEmpty( containerConfigXML ) )
            {
                File metainfDir = new File( webappDirectory, META_INF );
                String xmlFileName = new File( containerConfigXML ).getName();
                copyFileIfModified( new File( containerConfigXML ), new File( metainfDir, xmlFileName ) );
            }
        }
    }

    /**
     * Builds the webapp for the specified project.
     * <p/>
     * Classes, libraries and tld files are copied to
     * the <tt>webappDirectory</tt> during this phase.
     *
     * @param project         the maven project
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

        if ( getClassesDirectory().exists() && ( !getClassesDirectory().equals( webappClassesDirectory ) ) )
        {
            copyDirectoryStructureIfModified( getClassesDirectory(), webappClassesDirectory );
        }

        Set artifacts = project.getArtifacts();

        List duplicates = findDuplicates( artifacts );

        List dependentWarDirectories = new ArrayList();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            String targetFileName = getM2Filename( artifact );

            getLog().debug( "Processing: " + targetFileName );

            if ( duplicates.contains( targetFileName ) )
            {
                getLog().debug( "Duplicate found: " + targetFileName );
                targetFileName = artifact.getGroupId() + "-" + targetFileName;
                getLog().debug( "Renamed to: " + targetFileName );
            }

            // TODO: utilise appropriate methods from project builder
            ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
            if ( !artifact.isOptional() && filter.include( artifact ) )
            {
                String type = artifact.getType();
                if ( "tld".equals( type ) )
                {
                    copyFileIfModified( artifact.getFile(), new File( tldDirectory, targetFileName ) );
                }
                else if ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) )
                {
                    copyFileIfModified( artifact.getFile(), new File( libDirectory, targetFileName ) );
                }
                else if ( "par".equals( type ) )
                {
                    targetFileName = targetFileName.substring( 0, targetFileName.lastIndexOf( '.' ) ) + ".jar";

                    getLog().debug( "Copying " + artifact.getFile() + " to " + new File( libDirectory, targetFileName ) );

                    copyFileIfModified( artifact.getFile(), new File( libDirectory, targetFileName ) );
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
     * Searches a set of artifacts for duplicate filenames and returns a list of duplicates.
     *
     * @param artifacts set of artifacts
     * @return List of duplicated artifacts
     */
    private List findDuplicates( Set artifacts )
    {
        List duplicates = new ArrayList();
        List identifiers = new ArrayList();
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            String candidate = getM2Filename( artifact );
            if ( identifiers.contains( candidate ) )
            {
                duplicates.add( candidate );
            }
            else
            {
                identifiers.add( candidate );
            }
        }
        return duplicates;
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
     * @param file     File to be unpacked.
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
     * @param srcDir    Directory containing unpacked dependent war contents
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
            if ( !targetFile.exists() )
            {
                try
                {
                    targetFile.getParentFile().mkdirs();
                    copyFileIfModified( new File( srcDir, files[j] ), targetFile );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error copying file '" + files[j] + "' to '" + targetFile + "'",
                                                      e );
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

    /**
     * Copy file from source to destination only if source is newer than the target file.
     * If <code>destinationDirectory</code> does not exist, it
     * (and any parent directories) will be created. If a file <code>source</code> in
     * <code>destinationDirectory</code> exists, it will be overwritten.
     *
     * @param source An existing <code>File</code> to copy.
     * @param destinationDirectory A directory to copy <code>source</code> into.
     *
     * @throws java.io.FileNotFoundException if <code>source</code> isn't a normal file.
     * @throws IllegalArgumentException if <code>destinationDirectory</code> isn't a directory.
     * @throws IOException if <code>source</code> does not exist, the file in
     * <code>destinationDirectory</code> cannot be written to, or an IO error occurs during copying.
     */
    private static void copyFileToDirectoryIfModified( final String source,
                                            final String destinationDirectory )
        throws IOException
    {
        // TO DO: Remove this method and use the method in FileUtils when Maven 2 changes
        // to plexus-utils 1.2.
        copyFileToDirectoryIfModified( new File( source ),
                             new File( destinationDirectory ) );
    }

    /**
     * Copy file from source to destination only if source is newer than the target file.
     * If <code>destinationDirectory</code> does not exist, it
     * (and any parent directories) will be created. If a file <code>source</code> in
     * <code>destinationDirectory</code> exists, it will be overwritten.
     *
     * @param source An existing <code>File</code> to copy.
     * @param destinationDirectory A directory to copy <code>source</code> into.
     *
     * @throws java.io.FileNotFoundException if <code>source</code> isn't a normal file.
     * @throws IllegalArgumentException if <code>destinationDirectory</code> isn't a directory.
     * @throws IOException if <code>source</code> does not exist, the file in
     * <code>destinationDirectory</code> cannot be written to, or an IO error occurs during copying.
     */
    public static void copyFileToDirectoryIfModified( final File source,
                                            final File destinationDirectory )
        throws IOException
    {
        // TO DO: Remove this method and use the method in FileUtils when Maven 2 changes
        // to plexus-utils 1.2.
        if ( destinationDirectory.exists() && !destinationDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "Destination is not a directory" );
        }

        copyFileIfModified( source, new File( destinationDirectory, source.getName() ) );
    }

    /**
     * Copy file from source to destination only if source timestamp is later than the destination timestamp.
     * The directories up to <code>destination</code> will be created if they don't already exist.
     * <code>destination</code> will be overwritten if it already exists.
     *
     * @param source An existing non-directory <code>File</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     * overwriting).
     *
     * @throws IOException if <code>source</code> does not exist, <code>destination</code> cannot be
     * written to, or an IO error occurs during copying.
     *
     * @throws java.io.FileNotFoundException if <code>destination</code> is a directory
     */
    public static void copyFileIfModified( final File source, final File destination )
        throws IOException
    {
        // TO DO: Remove this method and use the method in FileUtils when Maven 2 changes
        // to plexus-utils 1.2.
        if ( destination.lastModified() < source.lastModified() )
        {
            FileUtils.copyFile( source, destination );
        }
    }

    /**
     * Copies a entire directory structure but only source files with timestamp later than the destinations'.
     *
     * Note:
     * <ul>
     * <li>It will include empty directories.
     * <li>The <code>sourceDirectory</code> must exists.
     * </ul>
     *
     * @param sourceDirectory
     * @param destinationDirectory
     * @throws IOException
     */
    public static void copyDirectoryStructureIfModified( File sourceDirectory, File destinationDirectory )
        throws IOException
    {
       if ( !sourceDirectory.exists() )
       {
           throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
       }

       File[] files = sourceDirectory.listFiles();

       String sourcePath = sourceDirectory.getAbsolutePath();

       for ( int i = 0; i < files.length; i++ )
       {
           File file = files[i];

           String dest = file.getAbsolutePath();

           dest = dest.substring( sourcePath.length() + 1 );

           File destination = new File( destinationDirectory, dest );

           if ( file.isFile() )
           {
               destination = destination.getParentFile();

               copyFileToDirectoryIfModified( file, destination );
           }
           else if ( file.isDirectory() )
           {
               if ( !destination.exists() && !destination.mkdirs() )
               {
                   throw new IOException( "Could not create destination directory '" + destination.getAbsolutePath() + "'." );
               }

               copyDirectoryStructureIfModified( file, destination );
           }
           else
           {
               throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
           }
       }
    }

    /**
     * Converts the filename of an artifact to artifactId-version.type format.
     *
     * @param artifact
     * @return converted filename of the artifact
     */
    private String getM2Filename( Artifact artifact )
    {
        String filename;
        if ( !artifact.getFile().getName().equals(
            artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType() ) )
        {
            filename = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType();
        }
        else
        {
            filename = artifact.getFile().getName();
        }
        return filename;
    }

}
