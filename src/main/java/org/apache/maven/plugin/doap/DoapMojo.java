package org.apache.maven.plugin.doap;

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

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * A Maven2 plugin to generate a Description of a Project (DOAP) file from the information found
 * in a POM.
 *
 * @author Jason van Zyl
 * @version $Id$
 * @goal generate
 */
public class DoapMojo
    extends AbstractMojo
{
    public static final String RDF_RESOURCE = "rdf:resource";

    /**
     * The POM from which information will be extracted to create a DOAP file.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The name of the DOAP file that will be generated.
     *
     * @parameter expression="${basedir}/doap_${project.artifactId}.rdf"
     */
    private File doapFile;

    /**
     * The category which should be displayed in the DOAP file. The POM doesn't have any
     * notions of category yet.
     *
     * @parameter expression="${category}"
     */
    private String category;

    /**
     * The language which should be displayed in the DOAP file. The POM doesn't have any
     * notions of language yet.
     *
     * @parameter expression="${language}" default-value="Java"
     */
    private String language;

    private XMLWriter w;

    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // setup pretty print xml writer
        // ----------------------------------------------------------------------------

        Writer writer;

        try
        {
            writer = new FileWriter( doapFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating DOAP file.", e );
        }

        PrintWriter pw = new PrintWriter( writer );

        w = new PrettyPrintXMLWriter( pw );

        // ----------------------------------------------------------------------------
        // Convert POM to DOAP
        // ----------------------------------------------------------------------------

        // Grab year for the license.
        SimpleDateFormat format = new SimpleDateFormat( "yyyy" );
        String year = format.format( new Date() );

        pw.println( "<!--" );
        pw.println( "  ~ Copyright " + year + " The Apache Software Foundation." );
        pw.println( "  ~" );
        pw.println( "  ~ Licensed under the Apache License, Version 2.0 (the \"License\");" );
        pw.println( "  ~ you may not use this file except in compliance with the License." );
        pw.println( "  ~ You may obtain a copy of the License at" );
        pw.println( "  ~" );
        pw.println( "  ~      http://www.apache.org/licenses/LICENSE-2.0" );
        pw.println( "  ~" );
        pw.println( "  ~ Unless required by applicable law or agreed to in writing, software" );
        pw.println( "  ~ distributed under the License is distributed on an \"AS IS\" BASIS," );
        pw.println( "  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied." );
        pw.println( "  ~ See the License for the specific language governing permissions and" );
        pw.println( "  ~ limitations under the License." );
        pw.println( "--> " );

        // Heading
        w.startElement( "rdf:RDF" );
        w.addAttribute( "xml:lang", "en" );
        w.addAttribute( "xmlns", "http://usefulinc.com/ns/doap#" );
        w.addAttribute( "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
        w.addAttribute( "xmlns:asfext", "http://projects.apache.org/ns/asfext#" );
        w.addAttribute( "xmlns:foaf", "http://xmlns.com/foaf/0.1/" );

        // Project
        w.startElement( "Project" );
        w.addAttribute( "rdf:about", "http://Maven.rdf.apache.org/" );
        element( "created", project.getInceptionYear() );

        if ( project.getLicenses().size() > 0 )
        {
            //TODO: how to map to usefulinc site, or if this is necessary, the OSI page might
            //      be more appropriate.
            rdfResourceElement( "license", ((License) project.getLicenses().get( 0 )).getUrl() );
        }

        element( "name", project.getName() );
        rdfResourceElement( "homepage", project.getUrl() );
        rdfResourceElement( "asfext:pmc", project.getUrl() );
        element( "shortdesc", project.getDescription() );
        element( "description", project.getDescription() );

        if ( project.getIssueManagement() != null )
        {
            rdfResourceElement( "bug-database", project.getIssueManagement().getUrl() );
        }
        rdfResourceElement( "mailing-list", composeUrl( project.getUrl() , "/mail-lists.html" ) );
        rdfResourceElement( "download-page", composeUrl( project.getUrl() , "/download.html" ) );
        element( "programming-language", language );
        //TODO: how to lookup category, map it, or just declare it.
        rdfResourceElement( "category", "http://projects.apache.org/category/" + category );

        // Releases
        publishReleases();

        // SCM
        publishSourceRepository();

        // Developers
        publishMaintainers();

        w.endElement();
        w.endElement();

        pw.close();
    }

    private void element( String name,
                          String value )
    {
        if ( value != null )
        {
            w.startElement( name );
            w.writeText( value );
            w.endElement();
        }
    }

    private void rdfResourceElement( String name,
                                     String value )
    {
        if ( value != null )
        {
            w.startElement( name );
            w.addAttribute( RDF_RESOURCE, value );
            w.endElement();
        }
    }

    //TODO: we will actually have to pull all the metadata from the repository
    private void publishReleases()
    {
    }

    private void publishSourceRepository()
    {
        //<repository>
        //  <SVNRepository>
        //    <location rdf:resource="http://svn.apache.org/repos/asf/maven/components/trunk/"/>
        //    <browse rdf:resource="http://svn.apache.org/viewcvs.cgi/maven/components/trunk/"/>
        //  </SVNRepository>
        //</repository>

        if ( project.getScm() == null )
        {
            return;
        }

        w.startElement( "repository" );
        w.startElement( "SVNRepository" );
        rdfResourceElement( "location", project.getScm().getConnection().substring( 8 ) );
        rdfResourceElement( "browse", project.getScm().getUrl() );
        w.endElement();
        w.endElement();

    }

    private void publishMaintainers()
    {
        //<maintainer>
        //  <foaf:Person>
        //    <foaf:name>Emmanuel Venisse</foaf:name>
        //    <foaf:mbox rdf:resource="mailto:evenisse@apache.org"/>
        //  </foaf:Person>
        //</maintainer>

        if ( project.getDevelopers() == null )
        {
            return;
        }

        for ( Iterator i = project.getDevelopers().iterator(); i.hasNext(); )
        {
            Developer d = (Developer) i.next();

            w.startElement( "maintainer" );
            w.startElement( "foaf:Person" );
            w.startElement( "foaf:name" );
            w.writeText( d.getName() );
            w.endElement();
            rdfResourceElement( "foaf:mbox", "mailto:" + d.getEmail() );
            w.endElement();
            w.endElement();
        }
    }

    /**
     * Compose a URL from two parts: a base URL and a file path. This method
     * makes sure that there will not be two slash '/' characters after each
     * other.
     *
     * @param base The base URL
     * @param path The file
     */
    private String composeUrl( String base, String path )
    {
        if ( base.endsWith( "/" ) && path.startsWith( "/" ) )
        {
            return base + path.substring( 1 );
        }
        else
        {
            return base + path;
        }
    }
}
