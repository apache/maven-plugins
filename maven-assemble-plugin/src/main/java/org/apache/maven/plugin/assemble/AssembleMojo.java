package org.apache.maven.plugin.assemble;

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

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugins.assemble.model.Assembly;
import org.apache.maven.plugins.assemble.model.FileSet;
import org.apache.maven.plugins.assemble.model.io.xpp3.AssemblyXpp3Reader;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal assemble
 * @description assemble an application bundle or distribution
 * @parameter name="outputDirectory" type="String" required="true" validator="" expression="#project.build.directory" description=""
 * @parameter name="descriptor" type="String" required="true" validator="" expression="#maven.assemble.descriptor" description=""
 * @parameter name="finalName" type="String" required="true" validator="" expression="#project.build.finalName" description=""
 */
public class AssembleMojo
    extends AbstractPlugin
{
    private static final String[] EMPTY_STRING_ARRAY = {};

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // TODO: align all to basedir
        String outputDirectory = (String) request.getParameter( "outputDirectory" );
        String descriptor = (String) request.getParameter( "descriptor" );
        String finalName = (String) request.getParameter( "finalName" );

        AssemblyXpp3Reader reader = new AssemblyXpp3Reader();
        Assembly assembly = reader.read( new FileReader( new File( descriptor ) ) );

        // TODO: include dependencies marked for distribution under certain formats
        // TODO: have a default set of descriptors that can be used instead of the file

        String fullName = finalName + "-" + assembly.getId();

        for ( Iterator i = assembly.getFormats().iterator(); i.hasNext(); )
        {
            String format = (String) i.next();

            String filename = fullName + "." + format;

            // TODO: use component roles? Can we do that in a mojo?
            Archiver archiver;
            if ( format.startsWith( "tar" ) )
            {
                TarArchiver tarArchiver = new TarArchiver();
                archiver = tarArchiver;
                int index = format.indexOf( '.' );
                if ( index >= 0 )
                {
                    // TODO: this needs a cleanup in plexus archiver - use a real typesafe enum
                    TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
                    tarCompressionMethod.setValue( format.substring( index + 1 ) );
                    tarArchiver.setCompression( tarCompressionMethod );
                }
            }
            else
            {
                // TODO: better handling
                throw new IllegalArgumentException( "Unknown format: " + format );
            }

            for ( Iterator j = assembly.getFilesets().iterator(); j.hasNext(); )
            {
                FileSet fileset = (FileSet) j.next();
                String directory = fileset.getDirectory();
                String output = fileset.getOutputDirectory();
                if ( output == null )
                {
                    output = directory;
                }

                String[] includes = (String[]) fileset.getIncludes().toArray( EMPTY_STRING_ARRAY );
                String[] excludes = (String[]) fileset.getExcludes().toArray( EMPTY_STRING_ARRAY );
                archiver.addDirectory( new File( directory ), output, includes, excludes );
            }

            archiver.setDestFile( new File( outputDirectory, filename ) );
            archiver.createArchive();
        }
    }
}
