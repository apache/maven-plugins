package org.apache.maven.plugins.war.packaging;

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

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.war.util.PathSet;
import org.apache.maven.plugins.war.util.WebappStructure;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.mapping.MappingUtils;
import org.codehaus.plexus.archiver.ArchiveEntryDateProvider;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author Stephane Nicoll
 * @version $Id$
 */
public abstract class AbstractWarPackagingTask
    implements WarPackagingTask
{
    /**
     * The default list of includes.
     */
    public static final String[] DEFAULT_INCLUDES = { "**/**" };

    /**
     * The {@code WEB-INF} path.
     */
    public static final String WEB_INF_PATH = "WEB-INF";

    /**
     * The {@code META-INF} path.
     */
    public static final String META_INF_PATH = "META-INF";

    /**
     * The {@code classes} path.
     */
    public static final String CLASSES_PATH = "WEB-INF/classes/";

    /**
     * The {@code lib} path.
     */
    public static final String LIB_PATH = "WEB-INF/lib/";

    /**
     * Copies the files if possible with an optional target prefix.
     * 
     * Copy uses a first-win strategy: files that have already been copied by previous tasks are ignored. This method
     * makes sure to update the list of protected files which gives the list of files that have already been copied.
     * 
     * If the structure of the source directory is not the same as the root of the webapp, use the <tt>targetPrefix</tt>
     * parameter to specify in which particular directory the files should be copied. Use <tt>null</tt> to copy the
     * files with the same structure
     *
     * @param sourceId the source id
     * @param context the context to use
     * @param sourceBaseDir the base directory from which the <tt>sourceFilesSet</tt> will be copied
     * @param sourceFilesSet the files to be copied
     * @param targetPrefix the prefix to add to the target file name
     * @param filtered filter or not.
     * @throws IOException if an error occurred while copying the files
     * @throws MojoExecutionException if an error occurs.
     */
    protected void copyFiles( String sourceId, WarPackagingContext context, File sourceBaseDir, PathSet sourceFilesSet,
                              String targetPrefix, boolean filtered )
        throws IOException, MojoExecutionException
    {
        for ( String fileToCopyName : sourceFilesSet.paths() )
        {
            final File sourceFile = new File( sourceBaseDir, fileToCopyName );

            String destinationFileName;
            if ( targetPrefix == null )
            {
                destinationFileName = fileToCopyName;
            }
            else
            {
                destinationFileName = targetPrefix + fileToCopyName;
            }

            if ( filtered && !context.isNonFilteredExtension( sourceFile.getName() ) )
            {
                copyFilteredFile( sourceId, context, sourceFile, destinationFileName );
            }
            else
            {
                copyFile( sourceId, context, sourceFile, destinationFileName );
            }
        }
    }

    /**
     * Copies the files if possible as is.
     * 
     * Copy uses a first-win strategy: files that have already been copied by previous tasks are ignored. This method
     * makes sure to update the list of protected files which gives the list of files that have already been copied.
     *
     * @param sourceId the source id
     * @param context the context to use
     * @param sourceBaseDir the base directory from which the <tt>sourceFilesSet</tt> will be copied
     * @param sourceFilesSet the files to be copied
     * @param filtered filter or not.
     * @throws IOException if an error occurred while copying the files
     * @throws MojoExecutionException break the build.
     */
    protected void copyFiles( String sourceId, WarPackagingContext context, File sourceBaseDir, PathSet sourceFilesSet,
                              boolean filtered )
        throws IOException, MojoExecutionException
    {
        copyFiles( sourceId, context, sourceBaseDir, sourceFilesSet, null, filtered );
    }

    /**
     * Copy the specified file if the target location has not yet already been used.
     * 
     * The <tt>targetFileName</tt> is the relative path according to the root of the generated web application.
     *
     * @param sourceId the source id
     * @param context the context to use
     * @param file the file to copy
     * @param targetFilename the relative path according to the root of the webapp
     * @throws IOException if an error occurred while copying
     */
    // CHECKSTYLE_OFF: LineLength
    protected void copyFile( String sourceId, final WarPackagingContext context, final File file, String targetFilename )
        throws IOException
    // CHECKSTYLE_ON: LineLength
    {
        final File targetFile = new File( context.getWebappDirectory(), targetFilename );

        if ( file.isFile() )
        {
            context.getWebappStructure().registerFile( sourceId, targetFilename,
           new WebappStructure.RegistrationCallback()
           {
               public void registered( String ownerId, String targetFilename )
                   throws IOException
               {
                   copyFile( context, file, targetFile, targetFilename,
                             false );
               }
    
               public void alreadyRegistered( String ownerId,
                                              String targetFilename )
                   throws IOException
               {
                   copyFile( context, file, targetFile, targetFilename,
                             true );
               }
    
               public void refused( String ownerId, String targetFilename,
                                    String actualOwnerId )
                   throws IOException
               {
                   context.getLog().debug( " - "
                                               + targetFilename
                                               + " wasn't copied because it has "
                                               + "already been packaged for overlay ["
                                               + actualOwnerId + "]." );
               }
    
               public void superseded( String ownerId,
                                       String targetFilename,
                                       String deprecatedOwnerId )
                   throws IOException
               {
                   context.getLog().info( "File ["
                                              + targetFilename
                                              + "] belonged to overlay ["
                                              + deprecatedOwnerId
                                              + "] so it will be overwritten." );
                   copyFile( context, file, targetFile, targetFilename,
                             false );
               }
    
               public void supersededUnknownOwner( String ownerId,
                                                   String targetFilename,
                                                   String unknownOwnerId )
                   throws IOException
               {
                   // CHECKSTYLE_OFF: LineLength
                   context.getLog().warn( "File ["
                                              + targetFilename
                                              + "] belonged to overlay ["
                                              + unknownOwnerId
                                              + "] which does not exist anymore in the current project. It is recommended to invoke "
                                              + "clean if the dependencies of the project changed." );
                   // CHECKSTYLE_ON: LineLength
                   copyFile( context, file, targetFile, targetFilename,
                             false );
               }
           } );
        }
        else if ( !targetFile.exists() && !targetFile.mkdirs() )
        {
            context.getLog().info( "Failed to create directory " + targetFile.getAbsolutePath() );
        }
    }

    /**
     * Copy the specified file if the target location has not yet already been used and filter its content with the
     * configured filter properties.
     * 
     * The <tt>targetFileName</tt> is the relative path according to the root of the generated web application.
     *
     * @param sourceId the source id
     * @param context the context to use
     * @param file the file to copy
     * @param targetFilename the relative path according to the root of the webapp
     * @return true if the file has been copied, false otherwise
     * @throws IOException if an error occurred while copying
     * @throws MojoExecutionException if an error occurred while retrieving the filter properties
     */
    protected boolean copyFilteredFile( String sourceId, final WarPackagingContext context, File file,
                                        String targetFilename )
        throws IOException, MojoExecutionException
    {

        if ( context.getWebappStructure().registerFile( sourceId, targetFilename ) )
        {
            final File targetFile = new File( context.getWebappDirectory(), targetFilename );
            final String encoding;
            try
            {
                if ( isXmlFile( file ) )
                {
                    // For xml-files we extract the encoding from the files
                    encoding = getEncoding( file );
                }
                else
                {
                    // For all others we use the configured encoding
                    encoding = context.getResourceEncoding();
                }
                // fix for MWAR-36, ensures that the parent dir are created first
                targetFile.getParentFile().mkdirs();

                context.getMavenFileFilter().copyFile( file, targetFile, true, context.getFilterWrappers(), encoding );
            }
            catch ( MavenFilteringException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            // CHECKSTYLE_OFF: LineLength
            // Add the file to the protected list
            context.getLog().debug( " + " + targetFilename + " has been copied (filtered encoding='" + encoding + "')." );
            // CHECKSTYLE_ON: LineLength
            return true;
        }
        else
        {
            context.getLog().debug( " - " + targetFilename
                                        + " wasn't copied because it has already been packaged (filtered)." );
            return false;
        }
    }

    /**
     * Unpacks the specified file to the specified directory.
     *
     * @param context the packaging context
     * @param file the file to unpack
     * @param unpackDirectory the directory to use for th unpacked file
     * @throws MojoExecutionException if an error occurred while unpacking the file
     */
    protected void doUnpack( WarPackagingContext context, File file, File unpackDirectory )
        throws MojoExecutionException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() ).toLowerCase();

        try
        {
            UnArchiver unArchiver = context.getArchiverManager().getUnArchiver( archiveExt );
            unArchiver.setSourceFile( file );
            unArchiver.setUseJvmChmod( context.isUseJvmChmod() );
            unArchiver.setDestDirectory( unpackDirectory );
            unArchiver.setOverwrite( true );
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file [" + file.getAbsolutePath() + "]" + " to ["
                + unpackDirectory.getAbsolutePath() + "]", e );
        }
        catch ( NoSuchArchiverException e )
        {
            context.getLog().warn( "Skip unpacking dependency file [" + file.getAbsolutePath()
                                       + " with unknown extension [" + archiveExt + "]" );
        }
    }

    /**
     * Copy file from source to destination. The directories up to <code>destination</code> will be created if they
     * don't already exist. if the <code>onlyIfModified</code> flag is <tt>false</tt>, <code>destination</code> will be
     * overwritten if it already exists. If the flag is <tt>true</tt> destination will be overwritten if it's not up to
     * date.
     *
     * @param context the packaging context
     * @param source an existing non-directory <code>File</code> to copy bytes from
     * @param destination a non-directory <code>File</code> to write bytes to (possibly overwriting).
     * @param targetFilename the relative path of the file from the webapp root directory
     * @param onlyIfModified if true, copy the file only if the source has changed, always copy otherwise
     * @return true if the file has been copied/updated, false otherwise
     * @throws IOException if <code>source</code> does not exist, <code>destination</code> cannot be written to, or an
     *             IO error occurs during copying
     */
    protected boolean copyFile( WarPackagingContext context, File source, File destination, String targetFilename,
                                boolean onlyIfModified )
        throws IOException
    {
        if ( onlyIfModified && destination.lastModified() >= source.lastModified() )
        {
            context.getLog().debug( " * " + targetFilename + " is up to date." );
            return false;
        }
        else
        {
            if ( source.isDirectory() )
            {
                context.getLog().warn( " + " + targetFilename + " is packaged from the source folder" );

                try
                {
                    JarArchiver archiver = context.getJarArchiver();
                    archiver.addDirectory( source );
                    archiver.setDestFile( destination );
                    archiver.createArchive();
                }
                catch ( ArchiverException e )
                {
                    String msg = "Failed to create " + targetFilename;
                    context.getLog().error( msg, e );
                    IOException ioe = new IOException( msg );
                    ioe.initCause( e );
                    throw ioe;
                }
            }
            else
            {
                FileUtils.copyFile( source.getCanonicalFile(), destination );
                // preserve timestamp
                destination.setLastModified( source.lastModified() );
                context.getLog().debug( " + " + targetFilename + " has been copied." );
            }
            return true;
        }
    }

    /**
     * Get the encoding from an XML-file.
     *
     * @param webXml the XML-file
     * @return The encoding of the XML-file, or UTF-8 if it's not specified in the file
     * @throws java.io.IOException if an error occurred while reading the file
     */
    protected String getEncoding( File webXml )
        throws IOException
    {
        XmlStreamReader xmlReader = new XmlStreamReader( webXml );
        try
        {
            return xmlReader.getEncoding();
        }
        finally
        {
            IOUtil.close( xmlReader );
        }
    }

    /**
     * Returns the file to copy. If the includes are <tt>null</tt> or empty, the default includes are used.
     *
     * @param baseDir the base directory to start from
     * @param includes the includes
     * @param excludes the excludes
     * @return the files to copy
     */
    protected PathSet getFilesToIncludes( File baseDir, String[] includes, String[] excludes )
    {
        return getFilesToIncludes( baseDir, includes, excludes, false );
    }

    /**
     * Returns the file to copy. If the includes are <tt>null</tt> or empty, the default includes are used.
     *
     * @param baseDir the base directory to start from
     * @param includes the includes
     * @param excludes the excludes
     * @param includeDirectories include directories yes or not.
     * @return the files to copy
     */
    // CHECKSTYLE_OFF: LineLength
    protected PathSet getFilesToIncludes( File baseDir, String[] includes, String[] excludes, boolean includeDirectories )
    // CHECKSTYLE_ON: LineLength
    {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( baseDir );

        if ( excludes != null )
        {
            scanner.setExcludes( excludes );
        }
        scanner.addDefaultExcludes();

        if ( includes != null && includes.length > 0 )
        {
            scanner.setIncludes( includes );
        }
        else
        {
            scanner.setIncludes( DEFAULT_INCLUDES );
        }

        scanner.scan();

        PathSet pathSet = new PathSet( scanner.getIncludedFiles() );

        if ( includeDirectories )
        {
            pathSet.addAll( scanner.getIncludedDirectories() );
        }

        return pathSet;
    }

    /**
     * Returns the final name of the specified artifact.
     * 
     * If the <tt>outputFileNameMapping</tt> is set, it is used, otherwise the standard naming scheme is used.
     *
     * @param context the packaging context
     * @param artifact the artifact
     * @return the converted filename of the artifact
     * @throws InterpolationException in case of interpolation problem.
     */
    protected String getArtifactFinalName( WarPackagingContext context, Artifact artifact )
        throws InterpolationException
    {
        if ( context.getOutputFileNameMapping() != null )
        {
            return MappingUtils.evaluateFileNameMapping( context.getOutputFileNameMapping(), artifact );
        }

        String classifier = artifact.getClassifier();
        if ( ( classifier != null ) && !( "".equals( classifier.trim() ) ) )
        {
            return MappingUtils.evaluateFileNameMapping( MappingUtils.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, artifact );
        }
        else
        {
            return MappingUtils.evaluateFileNameMapping( MappingUtils.DEFAULT_FILE_NAME_MAPPING, artifact );
        }

    }

    /**
     * Returns <code>true</code> if the <code>File</code>-object is a file (not a directory) that is not
     * <code>null</code> and has a file name that ends in ".xml".
     *
     * @param file The file to check
     * @return <code>true</code> if the file is an xml-file, otherwise <code>false</code>
     * @since 2.3
     */
    private boolean isXmlFile( File file )
    {
        return file != null && file.isFile() && file.getName().endsWith( ".xml" );
    }
}
