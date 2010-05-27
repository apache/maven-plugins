package org.apache.maven.plugin.docck;

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

import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.docck.reports.DocumentationReporter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Checks a plugin's documentation for the standard minimums.
 *
 * @author jdcasey
 * @goal check
 * @aggregator
 * @phase validate
 * @threadSafe
 */
public class CheckPluginDocumentationMojo
    extends AbstractCheckDocumentationMojo
{

    /**
     * Plexus component that searches for Mojos.
     *
     * @component
     */
    protected MojoScanner mojoScanner;

    // TODO: really a description of length 1 isn't all that helpful...
    private static final int MIN_DESCRIPTION_LENGTH = 1;

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

                    if ( mojoDescription == null || mojoDescription.trim().length() < MIN_DESCRIPTION_LENGTH )
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

                            if ( param.getRequirement() == null && param.isEditable() )
                            {
                                String paramDescription = param.getDescription();

                                if ( paramDescription == null
                                    || paramDescription.trim().length() < MIN_DESCRIPTION_LENGTH )
                                {
                                    reporter.error( "Parameter: \'" + param.getName() + "\' in mojo: \'"
                                        + mojo.getGoal() + "\' is missing a description." );
                                }
                            }
                        }
                    }
                }
            }
        }

        checkConfiguredReportPlugins( project, reporter );

        checkProjectSite( project, reporter );
    }

    protected boolean approveProjectPackaging( String packaging )
    {
        return "maven-plugin".equals( packaging );
    }

    private void checkProjectSite( MavenProject project, DocumentationReporter reporter )
    {
        File projectSiteDirectory = new File( project.getBasedir(), siteDirectory );

        // check for site.xml
        File siteXml = new File( projectSiteDirectory, "site.xml" );

        if ( !siteXml.exists() )
        {
            reporter.error( "site.xml is missing." );
        }
        else
        {
            Reader streamReader = null;
            try
            {
                streamReader = ReaderFactory.newXmlReader( siteXml );

                String siteHtml = IOUtil.toString( streamReader );

                if ( siteHtml.indexOf( "href=\"index.html\"" ) < 0 )
                {
                    reporter.error( "site.xml is missing the link to: index.html \"Introduction\"." );
                }

                if ( siteHtml.indexOf( "href=\"usage.html\"" ) < 0 )
                {
                    reporter.error( "site.xml is missing the link to: usage.html \"Usage\"." );
                }

                if ( siteHtml.indexOf( "href=\"plugin-info.html\"" ) < 0 )
                {
                    reporter.error( "site.xml is missing the link to: plugin-info.html \"Goals\"." );
                }

                if ( siteHtml.indexOf( "href=\"faq.html\"" ) < 0 )
                {
                    reporter.error( "site.xml is missing the link to: faq.html \"FAQ\"." );
                }
            }
            catch ( IOException e )
            {
                reporter.error( "Unable to read site.xml file: \'" + siteXml.getAbsolutePath()
                    + "\'.\nError: " + e.getMessage() );
            }
            finally
            {
                IOUtil.close( streamReader );
            }
        }

        // check for index.(apt|html|xml)[.vm]
        if ( !findFiles( projectSiteDirectory, "index" ) )
        {
            reporter.error( "There is no \'index\' file in your site directory (in apt|html|xml[.vm] format)." );
        }

        // check for usage.(apt|html|xml)[.vm]
        if ( !findFiles( projectSiteDirectory, "usage" ) )
        {
            reporter.error( "There is no \'usage\' file in your site directory (in apt|html|xml[.vm] format)." );
        }

        // check for **/examples/**.(apt|html|xml)[.vm] or **/example*.(apt|html|xml)[.vm] 
        if ( !findFiles( projectSiteDirectory, "**/examples/*" )
             && !findFiles( projectSiteDirectory, "**/example*" ) )
        {
            reporter.error( "There are no example files in your site directory (in apt|html|xml[.vm] format)."
                + " They should either be called \'example*.(apt|html|xml)[.vm]\'"
                + " or they should be located in the \'examples\' directory." );
        }

        if ( !findFiles( projectSiteDirectory, "faq" ) )
        {
            reporter.error( "There is no \'faq\' file in your site directory (in apt|fml|html|xml[.vm] format)." );
        }
    }

    /**
     * Checks the project configured plugins if the required report plugins are present.
     *
     * @param project  MavenProject to check
     * @param reporter listener
     * @todo maybe this should be checked default for all project?
     */
    private void checkConfiguredReportPlugins( MavenProject project, DocumentationReporter reporter )
    {
        List expectedPlugins = getRequiredPlugins();

        List reportPlugins = project.getReportPlugins();
        if ( reportPlugins != null && reportPlugins.size() > 0 )
        {
            for ( Iterator plugins = reportPlugins.iterator(); plugins.hasNext(); )
            {
                ReportPlugin plugin = (ReportPlugin) plugins.next();

                expectedPlugins.remove( plugin.getArtifactId() );
            }
        }
        else
        {
            reporter.error( "pom.xml has no report plugins configured." );
        }

        for ( Iterator plugins = expectedPlugins.iterator(); plugins.hasNext(); )
        {
            reporter.error( "pom.xml is missing the report plugin: " + plugins.next().toString() + "." );
        }
    }

    /**
     * Returns a List of Strings of required report plugins.
     *
     * @return List of report plugin artifactIds
     */
    private List getRequiredPlugins()
    {
        List list = new ArrayList();

        list.add( "maven-javadoc-plugin" );
        list.add( "maven-jxr-plugin" );

        return list;
    }
}
