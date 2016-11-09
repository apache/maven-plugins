package org.apache.maven.plugins.assembly.archive.phase;

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

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.format.ReaderFormatter;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.codehaus.plexus.components.io.resources.ResourceFactory.createResource;

/**
 * Handles the top-level &lt;files/&gt; section of the assembly descriptor.
 *
 * @version $Id$
 */
@Component( role = AssemblyArchiverPhase.class, hint = "file-items" )
public class FileItemAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase, PhaseOrder
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        final List<FileItem> fileList = assembly.getFiles();
        final File basedir = configSource.getBasedir();

        for ( final FileItem fileItem : fileList )
        {
            final String sourcePath = fileItem.getSource();

            // ensure source file is in absolute path for reactor build to work
            File source = new File( sourcePath );

            // save the original sourcefile's name, because filtration may
            // create a temp file with a different name.
            final String sourceName = source.getName();

            if ( !source.isAbsolute() )
            {
                source = new File( basedir, sourcePath );
            }

            String destName = fileItem.getDestName();

            if ( destName == null )
            {
                destName = sourceName;
            }

            final String outputDirectory1 = fileItem.getOutputDirectory();

            AssemblyFormatUtils.warnForPlatformSpecifics( getLogger(), outputDirectory1 );

            final String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( outputDirectory1, configSource.getFinalName(), configSource,
                                                        AssemblyFormatUtils.moduleProjectInterpolator(
                                                            configSource.getProject() ),
                                                        AssemblyFormatUtils.artifactProjectInterpolator( null ) );

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
                final InputStreamTransformer fileSetTransformers =
                    ReaderFormatter.getFileSetTransformers( configSource, fileItem.isFiltered(),
                                                            fileItem.getLineEnding() );

                final PlexusIoResource restoUse = createResource( source, fileSetTransformers );

                int mode = TypeConversionUtils.modeToInt( fileItem.getFileMode(), getLogger() );
                archiver.addResource( restoUse, target, mode );
            }
            catch ( final ArchiverException e )
            {
                throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
            }
            catch ( IOException e )
            {
                throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
            }
        }
    }

    @Override
    public int order()
    {
        return 10;
    }
}
