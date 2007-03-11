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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
     * Whether a JAR file will be created for the classes in the webapp. Using this optional configuration
     * parameter will make the generated classes to be archived into a jar file
     * and the classes directory will then be excluded from the webapp.
     *
     * @parameter expression="${archiveClasses}" default-value="false"
     */
    private boolean archiveClasses;

    /**
     * The Jar archiver needed for archiving classes directory into jar file under WEB-INF/lib.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     */
    private JarArchiver jarArchiver;

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
     * The list of webResources we want to transfer.
     *
     * @parameter
     */
    private Resource[] webResources;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     *
     * @parameter expression="${project.build.filters}"
     */
    private List filters;

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
    private File containerConfigXML;

    /**
     * Directory to unpack dependent WARs into if needed
     *
     * @parameter expression="${project.build.directory}/war/work"
     * @required
     */
    private File workDirectory;

    /**
     * The file name mapping to use to copy libraries and tlds. If no file mapping is
     * set (default) the file is copied with its standard name.
     *
     * @parameter
     */
    private String outputFileNameMapping;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */
    protected ArchiverManager archiverManager;

    private static final String WEB_INF = "WEB-INF";

    private static final String META_INF = "META-INF";

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    private static final String DEFAULT_FILE_NAME_MAPPING_CLASSIFIER =
        "${artifactId}-${version}-${classifier}.${extension}";

    private static final String DEFAULT_FILE_NAME_MAPPING =
        "${artifactId}-${version}.${extension}";

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
     * a war overlay.
     *
     * @parameter
     */
    private String dependentWarExcludes;

    /**
     * The maven archive configuration to use.
     *
     * @parameter
     */
    protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    private static final String[] EMPTY_STRING_ARRAY = {};


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

    public File getContainerConfigXML()
    {
        return containerConfigXML;
    }

    public void setContainerConfigXML( File containerConfigXML )
    {
        this.containerConfigXML = containerConfigXML;
    }

    public String getOutputFileNameMapping()
    {
        return outputFileNameMapping;
    }

    public void setOutputFileNameMapping( String outputFileNameMapping )
    {
        this.outputFileNameMapping = outputFileNameMapping;
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
        if ( webXml != null && StringUtils.isNotEmpty( webXml.getName() ) )
        {
            excludeList.add( "**/" + WEB_INF + "/web.xml" );
        }

        // if contextXML is specified, omit the one in the source directory
        if ( containerConfigXML != null && StringUtils.isNotEmpty( containerConfigXML.getName() ) )
        {
            excludeList.add( "**/" + META_INF + "/" + containerConfigXML.getName() );
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
        String[] excludes;
        if ( StringUtils.isNotEmpty( dependentWarExcludes ) )
        {
            excludes = StringUtils.split( dependentWarExcludes, "," );
        }
        else
        {
            excludes = EMPTY_STRING_ARRAY;
        }
        return excludes;
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
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "Exploding webapp..." );

        webappDirectory.mkdirs();

        try
        {
            buildWebapp( project, webappDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode webapp...", e );
        }
    }

    private Map getBuildFilterProperties()
        throws MojoExecutionException
    {

        Map filterProperties = new Properties();

        // System properties
        filterProperties.putAll( System.getProperties() );

        // Project properties
        filterProperties.putAll( project.getProperties() );

        for ( Iterator i = filters.iterator(); i.hasNext(); )
        {
            String filtersfile = (String) i.next();

            try
            {
                Properties properties = PropertyUtils.loadPropertyFile( new File( filtersfile ), true, true );

                filterProperties.putAll( properties );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error loading property file '" + filtersfile + "'", e );
            }
        }

        // can't putAll, as ReflectionProperties doesn't enumerate - so we make a composite map with the project variables as dominant
        return new CompositeMap( new ReflectionProperties( project ), filterProperties );
    }

    /**
     * Copies webapp webResources from the specified directory.
     * <p/>
     * Note that the <tt>webXml</tt> parameter could be null and may
     * specify a file which is not named <tt>web.xml<tt>. If the file
     * exists, it will be copied to the <tt>META-INF</tt> directory and
     * renamed accordingly.
     *
     * @param resource         the resource to copy
     * @param webappDirectory  the target directory
     * @param filterProperties
     * @throws java.io.IOException if an error occured while copying webResources
     */
    public void copyResources( Resource resource, File webappDirectory, Map filterProperties )
        throws IOException
    {
        if ( !resource.getDirectory().equals( webappDirectory.getPath() ) )
        {
            getLog().info( "Copy webapp webResources to " + webappDirectory.getAbsolutePath() );
            if ( webappDirectory.exists() )
            {
                String[] fileNames = getWarFiles( resource );
                String targetPath = ( resource.getTargetPath() == null ) ? "" : resource.getTargetPath();
                File destination = new File( webappDirectory, targetPath );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    if ( resource.isFiltering() )
                    {
                        copyFilteredFile( new File( resource.getDirectory(), fileNames[i] ),
                                          new File( destination, fileNames[i] ), null, getFilterWrappers(),
                                          filterProperties );
                    }
                    else
                    {
                        copyFileIfModified( new File( resource.getDirectory(), fileNames[i] ),
                                            new File( destination, fileNames[i] ) );
                    }
                }
            }
        }
    }

    /**
     * Copies webapp webResources from the specified directory.
     * <p/>
     * Note that the <tt>webXml</tt> parameter could be null and may
     * specify a file which is not named <tt>web.xml<tt>. If the file
     * exists, it will be copied to the <tt>META-INF</tt> directory and
     * renamed accordingly.
     *
     * @param sourceDirectory the source directory
     * @param webappDirectory the target directory
     * @throws java.io.IOException if an error occured while copying webResources
     */
    public void copyResources( File sourceDirectory, File webappDirectory )
        throws IOException
    {
        if ( !sourceDirectory.equals( webappDirectory ) )
        {
            getLog().info( "Copy webapp webResources to " + webappDirectory.getAbsolutePath() );
            if ( warSourceDirectory.exists() )
            {
                String[] fileNames = getWarFiles( sourceDirectory );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    copyFileIfModified( new File( sourceDirectory, fileNames[i] ),
                                        new File( webappDirectory, fileNames[i] ) );
                }
            }
        }
    }

    /**
     * Generates the JAR.
     *
     * @todo Add license files in META-INF directory.
     */
    public void createJarArchive( File libDirectory )
        throws MojoExecutionException
    {
        String archiveName = project.getBuild().getFinalName() + ".jar";

        File jarFile = new File( libDirectory, archiveName );

        MavenArchiver archiver = new MavenArchiver();

        archiver.setArchiver( jarArchiver );

        archiver.setOutputFile( jarFile );

        try
        {
            archiver.getArchiver().addDirectory( classesDirectory, getIncludes(), getExcludes() );

            archiver.createArchive( project, archive );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error assembling JAR", e );
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
        throws MojoExecutionException, IOException, MojoFailureException
    {
        getLog().info( "Assembling webapp " + project.getArtifactId() + " in " + webappDirectory );

        File webinfDir = new File( webappDirectory, WEB_INF );
        webinfDir.mkdirs();

        File metainfDir = new File( webappDirectory, META_INF );
        metainfDir.mkdirs();

        List webResources = this.webResources != null ? Arrays.asList( this.webResources ) : null;
        if ( webResources != null && webResources.size() > 0 )
        {
            Map filterProperties = getBuildFilterProperties();
            for ( Iterator it = webResources.iterator(); it.hasNext(); )
            {
                Resource resource = (Resource) it.next();
                copyResources( resource, webappDirectory, filterProperties );
            }
        }

        copyResources( warSourceDirectory, webappDirectory );

        if ( webXml != null && StringUtils.isNotEmpty( webXml.getName() ) )
        {
            if ( !webXml.exists() )
            {
                throw new MojoFailureException( "The specified web.xml file '" + webXml + "' does not exist" );
            }

            //rename to web.xml
            copyFileIfModified( webXml, new File( webinfDir, "/web.xml" ) );
        }

        if ( containerConfigXML != null && StringUtils.isNotEmpty( containerConfigXML.getName() ) )
        {
            metainfDir = new File( webappDirectory, META_INF );
            String xmlFileName = containerConfigXML.getName();
            copyFileIfModified( containerConfigXML, new File( metainfDir, xmlFileName ) );
        }

        File libDirectory = new File( webinfDir, "lib" );

        File tldDirectory = new File( webinfDir, "tld" );

        File webappClassesDirectory = new File( webappDirectory, WEB_INF + "/classes" );

        if ( classesDirectory.exists() && !classesDirectory.equals( webappClassesDirectory ) )
        {
            if ( archiveClasses )
            {
                createJarArchive( libDirectory );
            }
            else
            {
                copyDirectoryStructureIfModified( classesDirectory, webappClassesDirectory );
            }
        }

        Set artifacts = project.getArtifacts();

        List duplicates = findDuplicates( artifacts );

        List dependentWarDirectories = new ArrayList();

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            String targetFileName = getFinalName( artifact );

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
                else
                {
                    if ( "jar".equals( type ) || "ejb".equals( type ) || "ejb-client".equals( type ) )
                    {
                        copyFileIfModified( artifact.getFile(), new File( libDirectory, targetFileName ) );
                    }
                    else
                    {
                        if ( "par".equals( type ) )
                        {
                            targetFileName = targetFileName.substring( 0, targetFileName.lastIndexOf( '.' ) ) + ".jar";

                            getLog().debug(
                                "Copying " + artifact.getFile() + " to " + new File( libDirectory, targetFileName ) );

                            copyFileIfModified( artifact.getFile(), new File( libDirectory, targetFileName ) );
                        }
                        else
                        {
                            if ( "war".equals( type ) )
                            {
                                dependentWarDirectories.add( unpackWarToTempDirectory( artifact ) );
                            }
                            else
                            {
                                getLog().debug( "Skipping artifact of type " + type + " for WEB-INF/lib" );
                            }
                        }
                    }
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
            String candidate = getFinalName( artifact );
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
            UnArchiver unArchiver = archiverManager.getUnArchiver( archiveExt );
            unArchiver.setSourceFile( file );
            unArchiver.setDestDirectory( location );
            unArchiver.setOverwrite( true );
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

            try
            {
                // Don't copy if it is in the source directory
                if ( !new File( warSourceDirectory, files[j] ).exists() )
                {
                    targetFile.getParentFile().mkdirs();
                    copyFileIfModified( new File( srcDir, files[j] ), targetFile );
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error copying file '" + files[j] + "' to '" + targetFile + "'", e );
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
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param resource the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getWarFiles( Resource resource )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( resource.getDirectory() );
        if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
        {
            scanner.setIncludes( (String[]) resource.getIncludes().toArray( EMPTY_STRING_ARRAY ) );
        }
        else
        {
            scanner.setIncludes( DEFAULT_INCLUDES );
        }
        if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
        {
            scanner.setExcludes( (String[]) resource.getExcludes().toArray( EMPTY_STRING_ARRAY ) );
        }

        scanner.addDefaultExcludes();

        scanner.scan();

        return scanner.getIncludedFiles();
    }

    /**
     * Copy file from source to destination only if source is newer than the target file.
     * If <code>destinationDirectory</code> does not exist, it
     * (and any parent directories) will be created. If a file <code>source</code> in
     * <code>destinationDirectory</code> exists, it will be overwritten.
     *
     * @param source               An existing <code>File</code> to copy.
     * @param destinationDirectory A directory to copy <code>source</code> into.
     * @throws java.io.FileNotFoundException if <code>source</code> isn't a normal file.
     * @throws IllegalArgumentException      if <code>destinationDirectory</code> isn't a directory.
     * @throws java.io.IOException           if <code>source</code> does not exist, the file in
     *                                       <code>destinationDirectory</code> cannot be written to, or an IO error occurs during copying.
     *                                       <p/>
     *                                       TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyFileToDirectoryIfModified( File source, File destinationDirectory )
        throws IOException
    {
        // TO DO: Remove this method and use the method in WarFileUtils when Maven 2 changes
        // to plexus-utils 1.2.
        if ( destinationDirectory.exists() && !destinationDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "Destination is not a directory" );
        }

        copyFileIfModified( source, new File( destinationDirectory, source.getName() ) );
    }

    private FilterWrapper[] getFilterWrappers()
    {
        return new FilterWrapper[]{
            // support ${token}
            new FilterWrapper()
            {
                public Reader getReader( Reader fileReader, Map filterProperties )
                {
                    return new InterpolationFilterReader( fileReader, filterProperties, "${", "}" );
                }
            },
            // support @token@
            new FilterWrapper()
            {
                public Reader getReader( Reader fileReader, Map filterProperties )
                {
                    return new InterpolationFilterReader( fileReader, filterProperties, "@", "@" );
                }
            }};
    }

    /**
     * @param from
     * @param to
     * @param encoding
     * @param wrappers
     * @param filterProperties
     * @throws IOException TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyFilteredFile( File from, File to, String encoding, FilterWrapper[] wrappers,
                                          Map filterProperties )
        throws IOException
    {
        // buffer so it isn't reading a byte at a time!
        Reader fileReader = null;
        Writer fileWriter = null;
        try
        {
            // fix for MWAR-36, ensures that the parent dir are created first
            to.getParentFile().mkdirs();

            if ( encoding == null || encoding.length() < 1 )
            {
                fileReader = new BufferedReader( new FileReader( from ) );
                fileWriter = new FileWriter( to );
            }
            else
            {
                FileInputStream instream = new FileInputStream( from );

                FileOutputStream outstream = new FileOutputStream( to );

                fileReader = new BufferedReader( new InputStreamReader( instream, encoding ) );

                fileWriter = new OutputStreamWriter( outstream, encoding );
            }

            Reader reader = fileReader;
            for ( int i = 0; i < wrappers.length; i++ )
            {
                FilterWrapper wrapper = wrappers[i];
                reader = wrapper.getReader( reader, filterProperties );
            }

            IOUtil.copy( reader, fileWriter );
        }
        finally
        {
            IOUtil.close( fileReader );
            IOUtil.close( fileWriter );
        }
    }

    /**
     * Copy file from source to destination only if source timestamp is later than the destination timestamp.
     * The directories up to <code>destination</code> will be created if they don't already exist.
     * <code>destination</code> will be overwritten if it already exists.
     *
     * @param source      An existing non-directory <code>File</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     *                    overwriting).
     * @throws IOException                   if <code>source</code> does not exist, <code>destination</code> cannot be
     *                                       written to, or an IO error occurs during copying.
     * @throws java.io.FileNotFoundException if <code>destination</code> is a directory
     *                                       <p/>
     *                                       TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyFileIfModified( File source, File destination )
        throws IOException
    {
        // TO DO: Remove this method and use the method in WarFileUtils when Maven 2 changes
        // to plexus-utils 1.2.
        if ( destination.lastModified() < source.lastModified() )
        {
            FileUtils.copyFile( source.getCanonicalFile(), destination );
            // preserve timestamp
            destination.setLastModified( source.lastModified() );
        }
    }

    /**
     * Copies a entire directory structure but only source files with timestamp later than the destinations'.
     * <p/>
     * Note:
     * <ul>
     * <li>It will include empty directories.
     * <li>The <code>sourceDirectory</code> must exists.
     * </ul>
     *
     * @param sourceDirectory
     * @param destinationDirectory
     * @throws IOException TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private static void copyDirectoryStructureIfModified( File sourceDirectory, File destinationDirectory )
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
                    throw new IOException(
                        "Could not create destination directory '" + destination.getAbsolutePath() + "'." );
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
     * TO DO: Remove this interface when Maven moves to plexus-utils version 1.4
     */
    private interface FilterWrapper
    {
        Reader getReader( Reader fileReader, Map filterProperties );
    }

    /**
     * Returns the final name of the specified artifact.
     * <p/>
     * If the <tt>outputFileNameMapping</tt> is set, it is used, otherwise
     * the standard naming scheme is used.
     *
     * @param artifact the artifact
     * @return the converted filename of the artifact
     */
    private String getFinalName( Artifact artifact )
    {
        if (outputFileNameMapping != null) {
            return MappingUtils.evaluateFileNameMapping( outputFileNameMapping, artifact);
        }

        String classifier = artifact.getClassifier();
        if ( ( classifier != null ) && !( "".equals( classifier.trim() ) ) )
        {
            return MappingUtils.evaluateFileNameMapping( DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, artifact);
        }
        else {
            return MappingUtils.evaluateFileNameMapping( DEFAULT_FILE_NAME_MAPPING, artifact);
        }

    }

}
