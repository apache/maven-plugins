package org.apache.maven.plugin.ant;

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

/**
 * A Maven2 plugin to generate an Ant build file.
 *
 * @author Jason van Zyl
 * @version $Id:$
 * @goal generate
 */
public class DoapMojo
    extends AbstractMojo
{
    /**
     * The project to create a build for.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter expression="${basedir}/doap_${project.artifactId}.rdf"
     */
    private File doapFile;

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

        w.startElement( "rdf:RDF");
        w.addAttribute( "xml:lang", "en" );
        w.addAttribute( "xmlns", "http://usefulinc.com/ns/doap#" );
        w.addAttribute( "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
        w.addAttribute( "xmlns:asfext", "http://projects.apache.org/ns/asfext#" );
        w.addAttribute( "xmlns:foaf", "http://xmlns.com/foaf/0.1/" );

        w.startElement( "Project");
        w.addAttribute( "rdf:about", "http://Maven.rdf.apache.org/" );
        element( "created", project.getInceptionYear() );
        //element( "license", (String) project.getLicenses().get(0) );
        element( "name", project.getName() );
        //element( "homepage")


        w.endElement();
        w.endElement();

        pw.close();
    }

    private void element( String name, String value )
    {
        if ( value != null )
        {
            w.startElement( name );
            w.writeText( value );
            w.endElement();
        }
    }
}
