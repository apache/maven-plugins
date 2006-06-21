package org.apache.maven.plugin.docck;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.docck.reports.DocumentationReporter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.scanner.MojoScanner;

import java.util.Iterator;
import java.util.List;

/**
 * Checks a plugin's documentation for the standard minimums.
 *
 * @author jdcasey
 * @goal plugin
 * @aggregator
 * @phase validate
 */
public class CheckPluginDocumentationMojo
    extends AbstractCheckDocumentationMojo
{

    /**
     * Plexus component that searches for Mojos
     *
     * @component
     */
    protected MojoScanner mojoScanner;

    protected void checkPackagingSpecificDocumentation( MavenProject project, DocumentationReporter reporter )
    {
        PluginDescriptor descriptor = new PluginDescriptor();

        try
        {
            mojoScanner.populatePluginDescriptor( project, descriptor );
        }
        catch ( InvalidPluginDescriptorException e )
        {
            reporter.error( "Failed to parse mojo descriptors.\nError: " + e.getMessage() );
            descriptor = null;
        }
        catch ( ExtractionException e )
        {
            reporter.error( "Failed to parse mojo descriptors.\nError: " + e.getMessage() );
            descriptor = null;
        }

        if ( descriptor != null )
        {
            List mojos = descriptor.getMojos();

            // ensure that all mojo classes are documented
            if ( mojos != null && !mojos.isEmpty() )
            {
                for ( Iterator it = mojos.iterator(); it.hasNext(); )
                {
                    MojoDescriptor mojo = (MojoDescriptor) it.next();

                    String mojoDescription = mojo.getDescription();

                    // TODO: really a description of length 1 isn't all that helpful...
                    if ( mojoDescription == null || mojoDescription.trim().length() < 1 )
                    {
                        reporter.error( "Mojo: \'" + mojo.getGoal() + "\' is missing a description." );
                    }

                    List params = mojo.getParameters();

                    // ensure that all parameters are documented
                    if ( params != null && !params.isEmpty() )
                    {
                        for ( Iterator paramIterator = params.iterator(); paramIterator.hasNext(); )
                        {
                            Parameter param = (Parameter) paramIterator.next();

                            if ( param.isEditable() )
                            {
                                String paramDescription = param.getDescription();

                                if ( paramDescription == null || paramDescription.trim().length() < 1 )
                                {
                                    reporter.error( "Parameter: \'" + param.getName() + "\' in mojo: \'" + mojo.getGoal() +
                                        "\' is missing a description." );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean approveProjectPackaging( String packaging )
    {
        return "maven-plugin".equals( packaging );
    }
}
