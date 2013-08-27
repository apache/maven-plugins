package org.apache.maven.plugin.assembly.filter;

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

import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @version $Id$
 */
public class SimpleAggregatingDescriptorHandler
    implements ContainerDescriptorHandler, LogEnabled
{

    // component configuration.

    private String filePattern;

    private String outputPath;

    private final String commentChars = "#";

    // calculated, temporary values.

    private boolean overrideFilterAction;

    private final StringWriter aggregateWriter = new StringWriter();

    private final List<String> filenames = new ArrayList<String>();

    // injected by the container.

    private Logger logger;

    public void finalizeArchiveCreation( final Archiver archiver )
        throws ArchiverException
    {
        checkConfig();

        if ( outputPath.endsWith( "/" ) )
        {
            throw new ArchiverException(
                "Cannot write aggregated properties to a directory. You must specify a file name in the outputPath configuration for this handler. (handler: "
                    + getClass().getName() );
        }

        if ( outputPath.startsWith( "/" ) )
        {
            outputPath = outputPath.substring( 1 );
        }

        final File temp = writePropertiesFile();

        overrideFilterAction = true;

        archiver.addFile( temp, outputPath );

        overrideFilterAction = false;
    }

    private File writePropertiesFile()
        throws ArchiverException
    {
        File f;

        Writer writer = null;
        try
        {
            f = File.createTempFile( "maven-assembly-plugin", "tmp" );
            f.deleteOnExit();

            // FIXME if it is a properties file, encoding should be ISO-8859-1
            writer = new FileWriter( f ); // platform encoding

            writer.write( commentChars + " Aggregated on " + new Date() + " from: " );

            for ( final String filename : filenames )
            {
                writer.write( "\n" + commentChars + " " + filename );
            }

            writer.write( "\n\n" );

            writer.write( aggregateWriter.toString() );
        }
        catch ( final IOException e )
        {
            throw new ArchiverException(
                "Error adding aggregated properties to finalize archive creation. Reason: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }

        return f;
    }

    public void finalizeArchiveExtraction( final UnArchiver unarchiver )
        throws ArchiverException
    {
    }

    public List<String> getVirtualFiles()
    {
        checkConfig();

        return Collections.singletonList( outputPath );
    }

    public boolean isSelected( final FileInfo fileInfo )
        throws IOException
    {
        checkConfig();

        if ( overrideFilterAction )
        {
            System.out.println( "Filtering overridden. Returning true." );
            return true;
        }

        String name = fileInfo.getName();
        name = AssemblyFileUtils.normalizePath( name );

        name = name.replace( File.separatorChar, '/' );

        if ( fileInfo.isFile() && name.matches( filePattern ) )
        {
            readProperties( fileInfo );
            filenames.add( name );

            return false;
        }

        return true;
    }

    private void checkConfig()
    {
        if ( filePattern == null || outputPath == null )
        {
            throw new IllegalStateException(
                "You must configure filePattern and outputPath in your containerDescriptorHandler declaration." );
        }
    }

    private void readProperties( final FileInfo fileInfo )
        throws IOException
    {
        final StringWriter writer = new StringWriter();
        Reader reader = null;
        try
        {
            // FIXME if it is a properties file, encoding should be ISO-8859-1
            reader = new InputStreamReader( fileInfo.getContents() ); // platform encoding

            IOUtil.copy( reader, writer );
        }
        finally
        {
            IOUtil.close( reader );
        }

        final String content = writer.toString();

        aggregateWriter.write( "\n" );
        aggregateWriter.write( content );
    }

    protected final Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_INFO, "" );
        }

        return logger;
    }

    public void enableLogging( final Logger logger )
    {
        this.logger = logger;
    }

    public String getFilePattern()
    {
        return filePattern;
    }

    public void setFilePattern( final String filePattern )
    {
        this.filePattern = filePattern;
    }

    public String getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath( final String outputPath )
    {
        this.outputPath = outputPath;
    }

}
