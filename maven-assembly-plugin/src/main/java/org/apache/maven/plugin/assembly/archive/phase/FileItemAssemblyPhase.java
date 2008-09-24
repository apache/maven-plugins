package org.apache.maven.plugin.assembly.archive.phase;

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
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.AssemblyContext;
import org.apache.maven.plugin.assembly.DefaultAssemblyContext;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.FileFormatter;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.AbstractLogEnabled;


/**
 * Handles the top-level &lt;files/&gt; section of the assembly descriptor.
 * 
 * @version $Id$
 * @plexus.component role="org.apache.maven.plugin.assembly.archive.phase.AssemblyArchiverPhase"
 *                   role-hint="file-items"
 */
public class FileItemAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase
{

    /**
     * {@inheritDoc}
     */
    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        execute( assembly, archiver, configSource, new DefaultAssemblyContext() );
    }
    
    /**
     * {@inheritDoc}
     */
    public void execute( Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource,
                         AssemblyContext context )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        List fileList = assembly.getFiles();
        File basedir = configSource.getBasedir();

        FileFormatter fileFormatter = new FileFormatter( configSource, getLogger() );
        for ( Iterator i = fileList.iterator(); i.hasNext(); )
        {
            FileItem fileItem = (FileItem) i.next();

            String sourcePath = fileItem.getSource();

            // ensure source file is in absolute path for reactor build to work
            File source = new File( sourcePath );

            // save the original sourcefile's name, because filtration may
            // create a temp file with a different name.
            String sourceName = source.getName();

            if ( !source.isAbsolute() )
            {
                source = new File( basedir, sourcePath );
            }

            source = fileFormatter.format( source, fileItem.isFiltered(), fileItem.getLineEnding() );

            String destName = fileItem.getDestName();

            if ( destName == null )
            {
                destName = sourceName;
            }

            String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( fileItem.getOutputDirectory(), configSource.getProject(), null,
                                                        configSource.getFinalName(), configSource );

            String target;

            // omit the last char if ends with / or \\
            if ( outputDirectory.endsWith( "/" ) || outputDirectory.endsWith( "\\" ) )
            {
                target = outputDirectory + destName;
            }
            else if ( outputDirectory.length() < 1 )
            {
                target = destName;
            }
            else
            {
                target = outputDirectory + "/" + destName;
            }

            try
            {
                archiver.addFile( source, target, TypeConversionUtils.modeToInt( fileItem.getFileMode(), getLogger() ) );
            }
            catch ( ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
            }
        }
    }

}
