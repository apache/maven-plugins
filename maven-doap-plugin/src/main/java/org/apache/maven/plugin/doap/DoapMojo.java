package org.apache.maven.plugin.doap;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.cvslib.repository.CvsScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;

/**
 * Generate a Description of a Project (DOAP) file from the information found in a POM.
 *
 * @author Jason van Zyl
 * @version $Id$
 * @since 1.0
 * @goal generate
 */
public class DoapMojo
    extends AbstractMojo
{
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

    /**
     * Maven SCM Manager.
     *
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
     * @since 1.0
     */
    protected ScmManager scmManager;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // setup pretty print xml writer
        // ----------------------------------------------------------------------------

        Writer w;
        try
        {
            w = WriterFactory.newXmlWriter( doapFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating DOAP file.", e );
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w, project.getModel().getModelEncoding(), null );

        // ----------------------------------------------------------------------------
        // Convert POM to DOAP
        // ----------------------------------------------------------------------------

        DoapUtil.writeHeader( writer );

        // Heading
        writer.startElement( "rdf:RDF" );
        writer.addAttribute( "xml:lang", "en" );
        writer.addAttribute( "xmlns", "http://usefulinc.com/ns/doap#" );
        writer.addAttribute( "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
        writer.addAttribute( "xmlns:asfext", "http://projects.apache.org/ns/asfext#" );
        writer.addAttribute( "xmlns:foaf", "http://xmlns.com/foaf/0.1/" );

        // Project
        writer.startElement( "Project" );
        writer.addAttribute( "rdf:about", "http://Maven.rdf.apache.org/" );
        DoapUtil.writeElement( writer, "created", project.getInceptionYear() );

        if ( project.getLicenses().size() > 0 )
        {
            //TODO: how to map to usefulinc site, or if this is necessary, the OSI page might
            //      be more appropriate.
            DoapUtil.writeRdfResourceElement( writer, "license", ((License) project.getLicenses().get( 0 )).getUrl() );
        }

        DoapUtil.writeElement( writer, "name", project.getName() );
        DoapUtil.writeRdfResourceElement( writer, "homepage", project.getUrl() );
        DoapUtil.writeRdfResourceElement( writer, "asfext:pmc", project.getUrl() );
        DoapUtil.writeElement( writer, "shortdesc", project.getDescription() );
        DoapUtil.writeElement( writer, "description", project.getDescription() );

        if ( project.getIssueManagement() != null )
        {
            DoapUtil.writeRdfResourceElement( writer, "bug-database", project.getIssueManagement().getUrl() );
        }
        DoapUtil.writeRdfResourceElement( writer, "mailing-list", composeUrl( project.getUrl() , "/mail-lists.html" ) );
        DoapUtil.writeRdfResourceElement( writer, "download-page", composeUrl( project.getUrl() , "/download.html" ) );
        DoapUtil.writeElement( writer, "programming-language", language );
        //TODO: how to lookup category, map it, or just declare it.
        DoapUtil.writeRdfResourceElement( writer, "category", "http://projects.apache.org/category/" + category );

        // Releases
        publishReleases();

        // SCM
        writeSourceRepositories( writer );

        // Developers
        publishMaintainers( writer );

        writer.endElement();
        writer.endElement();

        try
        {
            w.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error when closing the writer.", e );
        }
    }

    //TODO: we will actually have to pull all the metadata from the repository
    private void publishReleases()
    {
    }

    /**
     * Write all DOAP repositories.
     *
     * @param writer
     */
    private void writeSourceRepositories( XMLWriter writer )
    {
        Scm scm = project.getScm();
        if ( scm == null )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Anonymous Connection", 2 );
        String anonymousConnection = scm.getConnection();
        writeSourceRepository( writer, anonymousConnection );

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Developer Connection", 2 );
        String developerConnection = scm.getDeveloperConnection();
        writeSourceRepository( writer, developerConnection );
    }

    /**
     * Write a DOAP repository, for instance:
     * <pre>
     *   &lt;repository&gt;
     *     &lt;SVNRepository&gt;
     *       &lt;location rdf:resource="http://svn.apache.org/repos/asf/maven/components/trunk/"/&gt;
     *       &lt;browse rdf:resource="http://svn.apache.org/viewcvs.cgi/maven/components/trunk/"/&gt;
     *     &lt;/SVNRepository&gt;
     *   &lt;/repository&gt;
     * </pre>
     *
     * @param writer not null
     * @param connection not null
     * @see <a href="http://usefulinc.com/ns/doap#Repository">http://usefulinc.com/ns/doap#Repository</a>
     * @see <a href="http://usefulinc.com/ns/doap#CVSRepository">http://usefulinc.com/ns/doap#CVSRepository</a>
     * @see <a href="http://usefulinc.com/ns/doap#SVNRepository">http://usefulinc.com/ns/doap#SVNRepository</a>
     */
    private void writeSourceRepository( XMLWriter writer, String connection )
    {
        ScmRepository repository = getScmRepository( connection );

        writer.startElement( "repository" );

        if ( isScmSystem( repository, "cvs" ) )
        {
            // http://usefulinc.com/ns/doap#CVSRepository
            writer.startElement( "CVSRepository" );

            CvsScmProviderRepository cvsRepo =
                (CvsScmProviderRepository) repository.getProviderRepository();

            // http://usefulinc.com/ns/doap#anon-root
            DoapUtil.writeElement( writer, "anon-root", cvsRepo.getCvsRoot() );
            // http://usefulinc.com/ns/doap#module
            DoapUtil.writeElement( writer, "module", cvsRepo.getModule() );
        }
        else if ( isScmSystem( repository, "svn" ) )
        {
            // http://usefulinc.com/ns/doap#SVNRepository
            writer.startElement( "SVNRepository" );

            SvnScmProviderRepository svnRepo =
                (SvnScmProviderRepository) repository.getProviderRepository();

            // http://usefulinc.com/ns/doap#location
            DoapUtil.writeRdfResourceElement( writer, "location", svnRepo.getUrl() );
        }
        else
        {
            /*
             * Supported DOAP repositories actually unsupported by SCM:
             *   BitKeeper (http://usefulinc.com/ns/doap#BKRepository)
             *   Arch (http://usefulinc.com/ns/doap#ArchRepository)
             * Other SCM repos are unsupported by DOAP.
             */
            writer.startElement( "Repository" );

            if ( connection.length() < 4 )
            {
                throw new IllegalArgumentException( "The source repository connection is too short." );
            }

            // http://usefulinc.com/ns/doap#location
            DoapUtil.writeRdfResourceElement( writer, "location", connection.substring( 4 ) );
        }

        // http://usefulinc.com/ns/doap#browse
        DoapUtil.writeRdfResourceElement( writer, "browse", project.getScm().getUrl() );

        writer.endElement(); // CVSRepository || SVNRepository || Repository
        writer.endElement(); // repository
    }

    private void publishMaintainers( XMLWriter w )
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
            DoapUtil.writeRdfResourceElement( w, "foaf:mbox", "mailto:" + d.getEmail() );
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

        return base + path;
    }

    /**
     * Return a <code>SCM repository</code> defined by a given url
     *
     * @param scmUrl an SCM URL
     * @return a valid SCM repository or null
     */
    public ScmRepository getScmRepository( String scmUrl )
    {
        ScmRepository repo = null;
        if ( !StringUtils.isEmpty( scmUrl ) )
        {
            try
            {
                repo = scmManager.makeScmRepository( scmUrl );
            }
            catch ( NoSuchScmProviderException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( e.getMessage(), e );
                }
            }
            catch ( ScmRepositoryException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( e.getMessage(), e );
                }
            }
        }
        return repo;
    }

    /**
     * Convenience method that return true is the defined <code>SCM repository</code> is a known provider.
     * <p>
     * Actually, we fully support Clearcase, CVS, Perforce, Starteam, SVN by the maven-scm-providers component.
     * </p>
     *
     * @param scmRepository a SCM repository
     * @param scmProvider a SCM provider name
     * @return true if the provider of the given SCM repository is equal to the given scm provider.
     * @see <a href="http://svn.apache.org/repos/asf/maven/scm/trunk/maven-scm-providers/">maven-scm-providers</a>
     */
    private static boolean isScmSystem( ScmRepository scmRepository, String scmProvider )
    {
        if ( StringUtils.isEmpty( scmProvider ) )
        {
            return false;
        }

        if ( scmRepository != null && scmProvider.equalsIgnoreCase( scmRepository.getProvider() ) )
        {
            return true;
        }

        return false;
    }
}
