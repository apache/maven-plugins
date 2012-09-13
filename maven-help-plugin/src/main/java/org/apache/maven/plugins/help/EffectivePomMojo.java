package org.apache.maven.plugins.help;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Displays the effective POM as an XML for this build, with the active profiles factored in.
 *
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "effective-pom", aggregator = true )
public class EffectivePomMojo
    extends AbstractEffectiveMojo
{
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven project.
     *
     * @since 2.0.2
     */
    @Component
    private MavenProject project;

    /**
     * The projects in the current build. The effective-POM for
     * each of these projects will written.
     */
    @Parameter( property = "reactorProjects", required = true, readonly = true )
    private List projects;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        StringWriter w = new StringWriter();
        XMLWriter writer =
            new PrettyPrintXMLWriter( w, StringUtils.repeat( " ", XmlWriterUtil.DEFAULT_INDENTATION_SIZE ),
                                      project.getModel().getModelEncoding(), null );

        writeHeader( writer );

        String effectivePom;
        if ( projects.get( 0 ).equals( project ) && projects.size() > 1 )
        {
            // outer root element
            writer.startElement( "projects" );
            for ( Iterator it = projects.iterator(); it.hasNext(); )
            {
                MavenProject subProject = (MavenProject) it.next();

                writeEffectivePom( subProject, writer );
            }
            writer.endElement();

            effectivePom = w.toString();
            effectivePom = prettyFormat( effectivePom );
        }
        else
        {
            writeEffectivePom( project, writer );

            effectivePom = w.toString();
        }

        if ( output != null )
        {
            try
            {
                writeXmlFile( output, effectivePom, project.getModel().getModelEncoding() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write effective-POM to output: " + output, e );
            }

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Effective-POM written to: " + output );
            }
        }
        else
        {
            StringBuilder message = new StringBuilder();

            message.append( "\nEffective POMs, after inheritance, interpolation, and profiles are applied:\n\n" );
            message.append( effectivePom );
            message.append( "\n" );

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( message.toString() );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Method for writing the effective pom informations of the current build.
     *
     * @param project the project of the current build, not null.
     * @param writer the XML writer , not null, not null.
     * @throws MojoExecutionException if any
     */
    private static void writeEffectivePom( MavenProject project, XMLWriter writer )
        throws MojoExecutionException
    {
        Model pom = project.getModel();
        cleanModel( pom );

        String effectivePom;

        StringWriter sWriter = new StringWriter();
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        try
        {
            pomWriter.write( sWriter, pom );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot serialize POM to XML.", e );
        }

        effectivePom = addMavenNamespace( sWriter.toString(), true );

        writeComment( writer, "Effective POM for project \'" + project.getId() + "\'" );

        writer.writeMarkup( effectivePom );
    }

    /**
     * Apply some logic to clean the model before writing it.
     *
     * @param pom not null
     */
    private static void cleanModel( Model pom )
    {
        Properties properties = new SortedProperties();
        properties.putAll( pom.getProperties() );
        pom.setProperties( properties );
    }

    /**
     * @param effectivePom not null
     * @return pretty format of the xml  or the original <code>effectivePom</code> if an error occurred.
     */
    private static String prettyFormat( String effectivePom )
    {
        SAXBuilder builder = new SAXBuilder();

        try
        {
            Document effectiveDocument = builder.build( new StringReader( effectivePom ) );

            StringWriter w = new StringWriter();
            Format format = Format.getPrettyFormat();
            XMLOutputter out = new XMLOutputter( format );
            out.output( effectiveDocument, w );

            return w.toString();
        }
        catch ( JDOMException e )
        {
            return effectivePom;
        }
        catch ( IOException e )
        {
            return effectivePom;
        }
    }
}
