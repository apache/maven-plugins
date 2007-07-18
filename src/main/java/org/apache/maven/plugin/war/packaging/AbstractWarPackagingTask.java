package org.apache.maven.plugin.war.packaging;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.AbstractWarMojo;
import org.apache.maven.plugin.war.util.MappingUtils;
import org.apache.maven.plugin.war.util.PathSet;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractWarPackagingTask
    implements WarPackagingTask
{
    public static final String[] DEFAULT_INCLUDES = {"**/**"};

    public static final String CLASSES_PATH = "WEB-INF/classes";

    public static final String LIB_PATH = "WEB-INF/lib";

    /**
     * Copies the files if possible.
     * <p/>
     * Copy uses a first-win strategy: files that have already been copied by previous
     * tasks are ignored. This method makes sure to update the list of protected files
     * which gives the list of files that have already been copied.
     *
     * @param context        the context to use
     * @param sourceBaseDir  the base directory from which the <tt>sourceFilesSet</tt> will be copied
     * @param sourceFilesSet the files to be copied
     * @throws IOException if an error occured while copying the files
     */
    protected void copyFiles( WarPackagingContext context, File sourceBaseDir, PathSet sourceFilesSet )
        throws IOException
    {
        for ( Iterator iter = sourceFilesSet.iterator(); iter.hasNext(); )
        {
            final String fileToCopyName = (String) iter.next();
            final File sourceFile = new File( sourceBaseDir, fileToCopyName );
            copyFile( context, sourceFile, fileToCopyName );
        }
    }

    /**
     * Copy the specified file if the target location has not yet already been used.
     * <p/>
     * The <tt>targetFileName</tt> is the relative path according to the root of
     * the generated web application.
     *
     * @param context        the context to use
     * @param file           the file to copy
     * @param targetFilename the relative path according to the root of the webapp
     * @return true if the file has been copied, false otherwise
     * @throws IOException if an error occured while copying
     */
    protected boolean copyFile( WarPackagingContext context, File file, String targetFilename )
        throws IOException
    {
        if ( !context.getProtectedFiles().contains( targetFilename ) )
        {
            File targetFile = new File( context.getWebAppDirectory(), targetFilename );
            copyFile( file, targetFile );

            // Add the file to the protected list
            context.getProtectedFiles().add( targetFilename );
            context.getLogger().debug( " + " + targetFilename + " has been copied." );
            return true;
        }
        else
        {
            context.getLogger().debug(
                " - " + targetFilename + " wasn't copied because it has already been packaged." );
            return false;
        }
    }

    /**
     * Copy the specified file if the target location has not yet already been
     * used and filter its content with the configureed filter properties.
     * <p/>
     * The <tt>targetFileName</tt> is the relative path according to the root of
     * the generated web application.
     *
     * @param context        the context to use
     * @param file           the file to copy
     * @param targetFilename the relative path according to the root of the webapp
     * @return true if the file has been copied, false otherwise
     * @throws IOException            if an error occured while copying
     * @throws MojoExecutionException if an error occured while retrieving the filter properties
     */
    protected boolean copyFilteredFile( WarPackagingContext context, File file, String targetFilename )
        throws IOException, MojoExecutionException
    {

        if ( !context.getProtectedFiles().contains( targetFilename ) )
        {
            final File targetFile = new File( context.getWebAppDirectory(), targetFilename );
            // buffer so it isn't reading a byte at a time!
            Reader fileReader = null;
            Writer fileWriter = null;
            try
            {
                // fix for MWAR-36, ensures that the parent dir are created first
                targetFile.getParentFile().mkdirs();

                fileReader = new BufferedReader( new FileReader( file ) );
                fileWriter = new FileWriter( targetFile );

                Reader reader = fileReader;
                for ( int i = 0; i < getFilterWrappers().length; i++ )
                {
                    FilterWrapper wrapper = getFilterWrappers()[i];
                    reader = wrapper.getReader( reader, context.getFilterProperties() );
                }

                IOUtil.copy( reader, fileWriter );
            }
            finally
            {
                IOUtil.close( fileReader );
                IOUtil.close( fileWriter );
            }
            // Add the file to the protected list
            context.getProtectedFiles().add( targetFilename );
            context.getLogger().debug( " + " + targetFilename + " has been copied." );
            return true;
        }
        else
        {
            context.getLogger().debug(
                " - " + targetFilename + " wasn't copied because it has already been packaged." );
            return false;
        }
    }


    /**
     * Unpacks the specified file to the specified directory.
     *
     * @param context         the packaging context
     * @param file            the file to unpack
     * @param unpackDirectory the directory to use for th unpacked file
     * @throws MojoExecutionException if an error occured while unpacking the file
     */
    protected void doUnpack( WarPackagingContext context, File file, File unpackDirectory )
        throws MojoExecutionException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() ).toLowerCase();

        try
        {
            UnArchiver unArchiver = context.getArchiverManager().getUnArchiver( archiveExt );
            unArchiver.setSourceFile( file );
            unArchiver.setDestDirectory( unpackDirectory );
            unArchiver.setOverwrite( true );
            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file[" + file.getAbsolutePath() + "]" + "to[" +
                unpackDirectory.getAbsolutePath() + "]", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file[" + file.getAbsolutePath() + "]" + "to[" +
                unpackDirectory.getAbsolutePath() + "]", e );
        }
        catch ( NoSuchArchiverException e )
        {
            context.getLogger().warn( "Skip unpacking dependency file[" + file.getAbsolutePath() +
                " with unknown extension[" + archiveExt + "]" );
        }
    }

    /**
     * Copy file from source to destination. The directories up to <code>destination</code>
     * will be created if they don't already exist. <code>destination</code> will be
     * overwritten if it already exists.
     * <p/>
     * TODO: Remove this method when Maven moves to plexus-util version 1.4
     *
     * @param source      an existing non-directory <code>File</code> to copy bytes from
     * @param destination a non-directory <code>File</code> to write bytes to (possibly overwriting).
     * @throws IOException                   if <code>source</code> does not exist, <code>destination</code> cannot
     *                                       be written to, or an IO error occurs during copying
     * @throws java.io.FileNotFoundException if <code>destination</code> is a directory
     */
    private void copyFile( File source, File destination )
        throws IOException
    {
        FileUtils.copyFile( source.getCanonicalFile(), destination );
        // preserve timestamp
        destination.setLastModified( source.lastModified() );
    }

    /**
     * Returns the file to copy. If the includes are <tt>null</tt> or empty, the
     * default includes are used.
     *
     * @param baseDir  the base directory to start from
     * @param includes the includes
     * @param excludes the excludes
     * @return the files to copy
     */
    protected PathSet getFilesToIncludes( File baseDir, String[] includes, String[] excludes )
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

        return new PathSet( scanner.getIncludedFiles() );

    }

    /**
     * Returns the final name of the specified artifact.
     * <p/>
     * If the <tt>outputFileNameMapping</tt> is set, it is used, otherwise
     * the standard naming scheme is used.
     *
     * @param context  the packaging context
     * @param artifact the artifact
     * @return the converted filename of the artifact
     */
    protected String getArtifactFinalName( WarPackagingContext context, Artifact artifact )
    {
        if ( context.getOutputFileNameMapping() != null )
        {
            return MappingUtils.evaluateFileNameMapping( context.getOutputFileNameMapping(), artifact );
        }

        String classifier = artifact.getClassifier();
        if ( ( classifier != null ) && !( "".equals( classifier.trim() ) ) )
        {
            return MappingUtils.evaluateFileNameMapping( AbstractWarMojo.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER,
                                                         artifact );
        }
        else
        {
            return MappingUtils.evaluateFileNameMapping( AbstractWarMojo.DEFAULT_FILE_NAME_MAPPING, artifact );
        }

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
     * TO DO: Remove this interface when Maven moves to plexus-util version 1.4
     */
    private interface FilterWrapper
    {
        Reader getReader( Reader fileReader, Map filterProperties );
    }

}
