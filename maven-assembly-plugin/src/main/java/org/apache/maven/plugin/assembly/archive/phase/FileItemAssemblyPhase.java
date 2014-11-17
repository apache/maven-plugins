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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.format.ReaderFormatter;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugin.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.ArchiverAttributeUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.components.io.resources.PlexusIoSymlink;
import org.codehaus.plexus.components.io.resources.proxy.PlexusIoProxyResource;
import org.codehaus.plexus.components.io.resources.proxy.PlexusIoProxySymlinkResource;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

            final PlexusIoFileResource res =
                new PlexusIoFileResource( source, ArchiverAttributeUtils.getFileAttributes( source ) );
            PlexusIoResource restoUse = res;
            try
            {
                final InputStreamTransformer fileSetTransformers =
                    ReaderFormatter.getFileSetTransformers( configSource, fileItem.isFiltered(),
                                                            fileItem.getLineEnding() );

                if ( fileSetTransformers != null )
                {
                    restoUse = new Deferred( res )
                    {
                        @Override
                        protected InputStream getInputStream()
                            throws IOException
                        {
                            return fileSetTransformers.transform( res, res.getContents() );
                        }

                        @Override
                        public String getName()
                        {
                            return res.getName();
                        }
                    } .asResource();
                }

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

    // Nicked from archiver until I can get a better solution. I am the author :)
    abstract class Deferred
    {
        final DeferredFileOutputStream dfos;

        final PlexusIoResource resource;

        public Deferred( final PlexusIoResource resource )
            throws IOException
        {
            this.resource = resource;
            // CHECKSTYLE_OFF: MagicNumber
            dfos = new DeferredFileOutputStream( 1000000, "m-assembly-archiver", null, null );
            // CHECKSTYLE_ON: MagicNumber
            InputStream inputStream = getInputStream();
            IOUtils.copy( inputStream, dfos );
            IOUtils.closeQuietly( inputStream );
        }

        protected abstract InputStream getInputStream()
            throws IOException;

        @Nonnull
        public InputStream getContents()
            throws IOException
        {
            if ( dfos.isInMemory() )
            {
                return new ByteArrayInputStream( dfos.getData() );
            }
            else
            {
                return new FileInputStream( dfos.getFile() )
                {
                    @Override
                    public void close()
                        throws IOException
                    {
                        super.close();
                        dfos.getFile().delete();
                    }
                };
            }
        }

        public long getSize()
        {
            if ( dfos == null )
            {
                return resource.getSize();
            }
            if ( dfos.isInMemory() )
            {
                return dfos.getByteCount();
            }
            else
            {
                return dfos.getFile().length();
            }
        }

        public abstract String getName();

        private PlexusIoResource asSymlinkResource()
        {
            return new PlexusIoProxySymlinkResource( resource )
            {
                @Override
                public String getName()
                {
                    return Deferred.this.getName();
                }

                @Nonnull
                @Override
                public InputStream getContents()
                    throws IOException
                {
                    return Deferred.this.getContents();
                }

                @Override
                public long getSize()
                {
                    return Deferred.this.getSize();
                }
            };
        }

        public PlexusIoResource asResource()
        {
            if ( resource instanceof PlexusIoSymlink )
            {
                return asSymlinkResource();
            }

            return new PlexusIoProxyResource( resource )
            {
                @Override
                public String getName()
                {
                    return Deferred.this.getName();
                }

                @Nonnull
                @Override
                public InputStream getContents()
                    throws IOException
                {
                    return Deferred.this.getContents();
                }

                @Override
                public long getSize()
                {
                    return Deferred.this.getSize();
                }
            };
        }

    }

    public int order()
    {
        return 10;
    }
}
