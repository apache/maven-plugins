package org.apache.maven.plugin.ear;

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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.ear.util.EarMavenArchiver;
import org.apache.maven.plugin.ear.util.JavaEEVersion;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;

/**
 * Builds J2EE Enterprise Archive (EAR) files.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 * @goal ear
 * @phase package
 * @threadSafe
 * @requiresDependencyResolution test
 */
public class EarMojo
    extends AbstractEarMojo
{
    private static final String[] EMPTY_STRING_ARRAY = { };


    /**
     * Single directory for extra files to include in the EAR.
     *
     * @parameter default-value="${basedir}/src/main/application"
     * @required
     */
    private File earSourceDirectory;

    /**
     * The comma separated list of tokens to include in the EAR.
     *
     * @parameter alias="includes" default-value="**"
     */
    private String earSourceIncludes;

    /**
     * The comma separated list of tokens to exclude from the EAR.
     *
     * @parameter alias="excludes"
     */
    private String earSourceExcludes;

    /**
     * Specify that the ear sources should be filtered.
     *
     * @parameter default-value="false"
     * @since 2.3.2
     */
    private boolean filtering;

    /**
     * Filters (property files) to include during the interpolation of the pom.xml.
     *
     * @parameter
     * @since 2.3.2
     */
    private List filters;

    /**
     * A list of file extensions that should not be filtered if
     * filtering is enabled.
     *
     * @parameter
     * @since 2.3.2
     */
    private List nonFilteredFileExtensions;

    /**
     * To escape interpolated value with windows path
     * c:\foo\bar will be replaced with c:\\foo\\bar
     *
     * @parameter expression="${maven.ear.escapedBackslashesInFilePath}" default-value="false"
     * @since 2.3.2
     */
    private boolean escapedBackslashesInFilePath;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     *
     * @parameter expression="${maven.ear.escapeString}"
     * @since 2.3.2
     */
    protected String escapeString;

    /**
     * The location of the manifest file to be used within the ear file. If
     * not value if specified, the default location in the workDirectory is
     * taken. If the file does not exist, a manifest will be generated
     * automatically.
     *
     * @parameter
     */
    private File manifestFile;

    /**
     * The location of a custom application.xml file to be used
     * within the ear file.
     *
     * @parameter
     */
    private String applicationXml;

    /**
     * The directory for the generated EAR.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the EAR file to generate.
     *
     * @parameter alias="earName" default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * The comma separated list of artifact's type(s) to unpack
     * by default.
     *
     * @parameter
     */
    private String unpackTypes;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will
     * be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    /**
     * A comma separated list of tokens to exclude when packaging the EAR.
     * By default nothing is excluded. Note that you can use the Java Regular
     * Expressions engine to include and exclude specific pattern using the
     * expression %regex[].
     * Hint: read the about (?!Pattern).
     *
     * @parameter
     * @since 2.7
     */
    private String packagingExcludes;

    /**
     * A comma separated list of tokens to include when packaging the EAR.
     * By default everything is included. Note that you can use the Java Regular
     * Expressions engine to include and exclude specific pattern using the
     * expression %regex[].
     *
     * @parameter
     * @since 2.7
     */
    private String packagingIncludes;

    /**
     * The directory to get the resources from.
     *
     * @parameter
     * @deprecated please use earSourceDirectory instead
     */
    private File resourcesDir;

    /**
     * Whether to create skinny WARs or not. A skinny WAR is a WAR that does not
     * have all of its dependencies in WEB-INF/lib. Instead those dependencies
     * are shared between the WARs through the EAR.
     *
     * @parameter expression="${maven.ear.skinnyWars}" default-value="false"
     * @since 2.7
     */
    private boolean skinnyWars;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * The Zip archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="zip"
     */
    private ZipArchiver zipArchiver;

    /**
     * The Zip Un archiver.
     *
     * @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip"
     */
    private ZipUnArchiver zipUnArchiver;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The archive manager.
     *
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter" role-hint="default"
     * @required
     */
    private MavenFileFilter mavenFileFilter;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */
    private MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     * @since 2.3.2
     */
    private MavenSession session;


    private List filterWrappers;


    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // Initializes ear modules
        super.execute();

        final JavaEEVersion javaEEVersion = JavaEEVersion.getJavaEEVersion( version );

        // Initializes unpack types
        List unpackTypesList = new ArrayList();
        if ( unpackTypes != null )
        {
            unpackTypesList = Arrays.asList( unpackTypes.split( "," ) );
            final Iterator it = unpackTypesList.iterator();
            while ( it.hasNext() )
            {
                String type = (String) it.next();
                if ( !EarModuleFactory.standardArtifactTypes.contains( type ) )
                {
                    throw new MojoExecutionException(
                        "Invalid type [" + type + "] supported types are " + EarModuleFactory.standardArtifactTypes );
                }
            }
            getLog().debug( "Initialized unpack types " + unpackTypesList );
        }

        // Copy modules
        try
        {
            for ( Iterator iter = getModules().iterator(); iter.hasNext(); )
            {
                EarModule module = (EarModule) iter.next();
                if ( module instanceof JavaModule )
                {
                    getLog().warn( "JavaModule is deprecated (" + module + "), please use JarModule instead." );
                }
                if ( module instanceof Ejb3Module )
                {
                    getLog().warn( "Ejb3Module is deprecated (" + module + "), please use EjbModule instead." );
                }
                final File sourceFile = module.getArtifact().getFile();
                final File destinationFile = buildDestinationFile( getWorkDirectory(), module.getUri() );
                if ( !sourceFile.isFile() )
                {
                    throw new MojoExecutionException(
                        "Cannot copy a directory: " + sourceFile.getAbsolutePath() + "; Did you package/install " +
                            module.getArtifact() + "?" );
                }

                if ( destinationFile.getCanonicalPath().equals( sourceFile.getCanonicalPath() ) )
                {
                    getLog().info(
                        "Skipping artifact [" + module + "], as it already exists at [" + module.getUri() + "]" );
                    continue;
                }

                // If the module is within the unpack list, make sure that no unpack wasn't forced (null or true)
                // If the module is not in the unpack list, it should be true
                if ( ( unpackTypesList.contains( module.getType() ) &&
                    ( module.shouldUnpack() == null || module.shouldUnpack().booleanValue() ) ) ||
                    ( module.shouldUnpack() != null && module.shouldUnpack().booleanValue() ) )
                {
                    getLog().info( "Copying artifact [" + module + "] to [" + module.getUri() + "] (unpacked)" );
                    // Make sure that the destination is a directory to avoid plexus nasty stuff :)
                    destinationFile.mkdirs();
                    unpack( sourceFile, destinationFile );

                    if ( skinnyWars && module.changeManifestClasspath() )
                    {
                        changeManifestClasspath( module, destinationFile );
                    }
                }
                else
                {
                    if ( sourceFile.lastModified() > destinationFile.lastModified() )
                    {
                        getLog().info( "Copying artifact [" + module + "] to [" + module.getUri() + "]" );
                        FileUtils.copyFile( sourceFile, destinationFile );

                        if ( skinnyWars && module.changeManifestClasspath() )
                        {
                            changeManifestClasspath( module, destinationFile );
                        }
                    }
                    else
                    {
                        getLog().debug(
                            "Skipping artifact [" + module + "], as it is already up to date at [" + module.getUri() +
                                "]" );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR modules", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking EAR modules", e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "No Archiver found for EAR modules", e );
        }

        // Copy source files
        try
        {
            File earSourceDir = earSourceDirectory;
            if ( earSourceDir.exists() )
            {
                getLog().info( "Copy ear sources to " + getWorkDirectory().getAbsolutePath() );
                String[] fileNames = getEarFiles( earSourceDir );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    copyFile( new File( earSourceDir, fileNames[i] ), new File( getWorkDirectory(), fileNames[i] ) );
                }
            }

            if ( applicationXml != null && !"".equals( applicationXml ) )
            {
                //rename to application.xml
                getLog().info( "Including custom application.xml[" + applicationXml + "]" );
                File metaInfDir = new File( getWorkDirectory(), META_INF );
                copyFile( new File( applicationXml ), new File( metaInfDir, "/application.xml" ) );
            }

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR sources", e );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( "Error filtering EAR sources", e );
        }

        // Copy resources files
        try
        {
            if ( resourcesDir != null && resourcesDir.exists() )
            {
                getLog().warn( "resourcesDir is deprecated. Please use the earSourceDirectory property instead." );
                getLog().info( "Copy ear resources to " + getWorkDirectory().getAbsolutePath() );
                String[] fileNames = getEarFiles( resourcesDir );
                for ( int i = 0; i < fileNames.length; i++ )
                {
                    FileUtils.copyFile( new File( resourcesDir, fileNames[i] ),
                                        new File( getWorkDirectory(), fileNames[i] ) );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying EAR resources", e );
        }

        // Check if deployment descriptor is there
        File ddFile = new File( getWorkDirectory(), APPLICATION_XML_URI );
        if ( !ddFile.exists() && ( javaEEVersion.lt( JavaEEVersion.Five ) ) )
        {
            throw new MojoExecutionException(
                "Deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist." );
        }

        try
        {
            File earFile = getEarFile( outputDirectory, finalName, classifier );
            final MavenArchiver archiver = new EarMavenArchiver( getModules() );
            final JarArchiver jarArchiver = getJarArchiver();
            getLog().debug( "Jar archiver implementation [" + jarArchiver.getClass().getName() + "]" );
            archiver.setArchiver( jarArchiver );
            archiver.setOutputFile( earFile );

            // Include custom manifest if necessary
            includeCustomManifestFile();

            getLog().debug(
                "Excluding " + Arrays.asList( getPackagingExcludes() ) + " from the generated EAR." );
            getLog().debug(
                "Including " + Arrays.asList( getPackagingIncludes() ) + " in the generated EAR." );

            archiver.getArchiver().addDirectory( getWorkDirectory(), getPackagingIncludes(), getPackagingExcludes() );
            archiver.createArchive( getProject(), archive );

            if ( classifier != null )
            {
                projectHelper.attachArtifact( getProject(), "ear", classifier, earFile );
            }
            else
            {
                getProject().getArtifact().setFile( earFile );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error assembling EAR", e );
        }
    }

    public String getApplicationXml()
    {
        return applicationXml;
    }

    public void setApplicationXml( String applicationXml )
    {
        this.applicationXml = applicationXml;
    }

    /**
     * Returns a string array of the excludes to be used
     * when assembling/copying the ear.
     *
     * @return an array of tokens to exclude
     */
    protected String[] getExcludes()
    {
        List excludeList = new ArrayList( FileUtils.getDefaultExcludesAsList() );
        if ( earSourceExcludes != null && !"".equals( earSourceExcludes ) )
        {
            excludeList.addAll( Arrays.asList( StringUtils.split( earSourceExcludes, "," ) ) );
        }

        // if applicationXml is specified, omit the one in the source directory
        if ( getApplicationXml() != null && !"".equals( getApplicationXml() ) )
        {
            excludeList.add( "**/" + META_INF + "/application.xml" );
        }

        return (String[]) excludeList.toArray( EMPTY_STRING_ARRAY );
    }

    /**
     * Returns a string array of the includes to be used
     * when assembling/copying the ear.
     *
     * @return an array of tokens to include
     */
    protected String[] getIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( earSourceIncludes ), "," );
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

    private static File buildDestinationFile( File buildDir, String uri )
    {
        return new File( buildDir, uri );
    }

    private void includeCustomManifestFile()
    {
        if ( manifestFile == null )
        {
            manifestFile = new File( getWorkDirectory(), "META-INF/MANIFEST.MF" );
        }

        if ( !manifestFile.exists() )
        {
            getLog().info( "Could not find manifest file: " + manifestFile + " - Generating one" );
        }
        else
        {
            getLog().info( "Including custom manifest file [" + manifestFile + "]" );
            archive.setManifestFile( manifestFile );
        }
    }

    /**
     * Returns the EAR file to generate, based on an optional classifier.
     *
     * @param basedir    the output directory
     * @param finalName  the name of the ear file
     * @param classifier an optional classifier
     * @return the EAR file to generate
     */
    private static File getEarFile( String basedir, String finalName, String classifier )
    {
        if ( classifier == null )
        {
            classifier = "";
        }
        else if ( classifier.trim().length() > 0 && !classifier.startsWith( "-" ) )
        {
            classifier = "-" + classifier;
        }

        return new File( basedir, finalName + classifier + ".ear" );
    }

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param sourceDir the directory to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getEarFiles( File sourceDir )
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
     * Unpacks the module into the EAR structure.
     *
     * @param source  File to be unpacked.
     * @param destDir Location where to put the unpacked files.
     */
    public void unpack( File source, File destDir )
        throws NoSuchArchiverException, IOException, ArchiverException
    {
        UnArchiver unArchiver = archiverManager.getUnArchiver( "zip" );
        unArchiver.setSourceFile( source );
        unArchiver.setDestDirectory( destDir );

        // Extract the module
        unArchiver.extract();
    }

    /**
     * Returns the {@link JarArchiver} implementation used
     * to package the EAR file.
     * <p/>
     * By default the archiver is obtained from the Plexus container.
     *
     * @return the archiver
     */
    protected JarArchiver getJarArchiver()
    {
        return jarArchiver;
    }

    private void copyFile( File source, File target )
        throws MavenFilteringException, IOException, MojoExecutionException
    {
        if ( filtering && !isNonFilteredExtension( source.getName() ) )
        {
            // Silly that we have to do this ourselves
            if ( target.getParentFile() != null && !target.getParentFile().exists() )
            {
                target.getParentFile().mkdirs();
            }
            mavenFileFilter.copyFile( source, target, true, getFilterWrappers(), null );
        }
        else
        {
            FileUtils.copyFile( source, target );
        }
    }

    public boolean isNonFilteredExtension( String fileName )
    {
        return !mavenResourcesFiltering.filteredFileExtension( fileName, nonFilteredFileExtensions );
    }

    private List getFilterWrappers()
        throws MojoExecutionException
    {
        if ( filterWrappers == null )
        {
            try
            {
                MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
                mavenResourcesExecution.setEscapeString( escapeString );
                filterWrappers =
                    mavenFileFilter.getDefaultFilterWrappers( project, filters, escapedBackslashesInFilePath,
                                                              this.session, mavenResourcesExecution );
            }
            catch ( MavenFilteringException e )
            {
                getLog().error( "Fail to build filtering wrappers " + e.getMessage() );
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
        return filterWrappers;
    }

    private void changeManifestClasspath( EarModule module, File original )
            throws MojoFailureException
    {
        try
        {
            File workDirectory;

            // Handle the case that the destination might be a directory (project-038)
            if( original.isFile() )
            {
                // Create a temporary work directory
                workDirectory = new File( new File(
                        generatedDescriptorLocation, "temp" ), module.getArtifact()
                        .getArtifactId() );
                workDirectory.mkdirs();
                getLog().debug( "Created a temporary work directory: " + workDirectory.getAbsolutePath() );

                // Unpack the archive to a temporary work directory
                zipUnArchiver.setSourceFile( original );
                zipUnArchiver.setDestDirectory( workDirectory );
                zipUnArchiver.extract();
            }
            else
            {
                workDirectory = original;
            }

            // Create a META-INF/MANIFEST.MF file if it doesn't exist (project-038)
            File metaInfDirectory = new File( workDirectory, "META-INF" );
            boolean newMetaInfCreated = metaInfDirectory.mkdirs();
            if( newMetaInfCreated )
            {
                getLog().debug( "This project did not have a META-INF directory before, so a new directory was created." );
            }
            File manifestFile = new File( metaInfDirectory, "MANIFEST.MF" );
            boolean newManifestCreated = manifestFile.createNewFile();
            if( newManifestCreated )
            {
                getLog().debug( "This project did not have a META-INF/MANIFEST.MF file before, so a new file was created." );
            }

            // Read the manifest from disk
            Manifest mf = new Manifest( new FileReader( manifestFile ) );
            Attribute classPath = mf.getMainSection()
                    .getAttribute( "Class-Path" );
            List classPathElements = new ArrayList();

            if ( classPath != null )
            {
                classPathElements.addAll( Arrays.asList( classPath.getValue()
                        .split( " " ) ) );
            } else
            {
                classPath = new Attribute( "Class-Path", "" );
                mf.getMainSection().addConfiguredAttribute( classPath );
            }

            // Modify the classpath entries in the manifest
            for ( Iterator iter = getModules().iterator(); iter.hasNext(); )
            {
                Object o = iter.next();

                if ( o instanceof JarModule )
                {
                    JarModule jm = ( JarModule ) o;

                    if ( module.getLibDir() != null )
                    {
                        File artifact = new File( new File(
                                workDirectory, module.getLibDir() ),
                                jm.getBundleFileName() );

                        if ( artifact.exists() )
                        {
                            if ( !artifact.delete() )
                            {
                                getLog().error(
                                        "Could not delete '" + artifact + "'" );
                            }
                        }
                    }

                    if ( classPathElements.contains( jm.getBundleFileName() ) )
                    {
                        classPathElements.set( classPathElements.indexOf( jm
                                .getBundleFileName() ), jm.getUri() );
                    }
                    else
                    {
                        classPathElements.add( jm.getUri() );
                    }
                }
            }
            classPath.setValue( StringUtils.join( classPathElements.iterator(), " " ) );

            // Write the manifest to disk
            PrintWriter pw = new PrintWriter( manifestFile );
            mf.write( pw );
            pw.close();

            if( original.isFile() )
            {
                // Pack up the archive again from the work directory
                if ( !original.delete() )
                {
                    getLog().error( "Could not delete original artifact file " + original );
                }

                getLog().debug( "Zipping module" );
                zipArchiver.setDestFile( original );
                zipArchiver.addDirectory( workDirectory );
                zipArchiver.createArchive();
            }
        }
        catch ( ManifestException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
        catch ( ZipException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
        catch ( ArchiverException e )
        {
            throw new MojoFailureException( e.getMessage() );
        }
    }
}
