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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.doap.options.DoapOptions;
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
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven SCM Manager.
     *
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
     * @since 1.0
     */
    protected ScmManager scmManager;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

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
     * @deprecated Since 1.0. Instead of, configure <code>category</code> property in <code>doapOptions</code> parameter.
     */
    private String category;

    /**
     * The language which should be displayed in the DOAP file. The POM doesn't have any
     * notions of language yet.
     *
     * @parameter expression="${language}"
     * @deprecated Since 1.0. Instead of, configure <code>programmingLanguage</code> property in <code>doapOptions</code> parameter.
     */
    private String language;

    /**
     * Specific DOAP parameters, i.e. options that POM doesn't have any notions.
     *
     * @parameter expression="${doapOptions}"
     * @since 1.0
     */
    private DoapOptions doapOptions;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

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

        // name
        writeName( writer );

        // description
        writeDescription( writer );

        // created
        writeCreated( writer );

        // homepage and old-homepage
        writeHomepage( writer );

        // licenses
        writeLicenses( writer );

        DoapUtil.writeRdfResourceElement( writer, "asfext:pmc", project.getUrl() );

        // programming-language
        writeProgrammingLanguage( writer );

        // category
        writeCategory( writer );

        // os
        writeOS( writer );

        // SCM
        writeSourceRepositories( writer );

        // bug-database
        writeBugDatabase( writer );

        // mailing list
        writeMailingList( writer );

        // download-page and download-mirror
        writeDownloadPage( writer );

        // screenshots
        writeScreenshots( writer );

        // Releases
        publishReleases();

        // Developers
        writeDevelopersOrContributors( writer, project.getDevelopers() );

        // Contributors
        writeDevelopersOrContributors( writer, project.getContributors() );

        writer.endElement(); // Project
        writer.endElement(); // rdf:RDF

        try
        {
            w.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error when closing the writer.", e );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Write DOAP name.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#name">http://usefulinc.com/ns/doap#name</a>
     */
    private void writeName( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( project.getName() ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "A name of something.", 2 );
        // http://usefulinc.com/ns/doap#name
        DoapUtil.writeElement( writer, "name", project.getName() );
    }

    /**
     * Write DOAP description.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#description">http://usefulinc.com/ns/doap#description</a>
     * @see <a href="http://usefulinc.com/ns/doap#shortdesc">http://usefulinc.com/ns/doap#shortdesc</a>
     */
    private void writeDescription( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( project.getDescription() ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Plain text description of a project, of 2-4 sentences in length.", 2 );
        // http://usefulinc.com/ns/doap#description
        DoapUtil.writeElement( writer, "description", project.getDescription() );
        // http://usefulinc.com/ns/doap#shortdesc
        DoapUtil.writeElement( writer, "shortdesc", project.getDescription() );
    }

    /**
     * Write DOAP created.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#created">http://usefulinc.com/ns/doap#created</a>
     */
    private void writeCreated( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( project.getInceptionYear() ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Date when something was created, in YYYY-MM-DD form. e.g. 2004-04-05",
                                        2 );
        // http://usefulinc.com/ns/doap#created
        DoapUtil.writeElement( writer, "created", project.getInceptionYear() + "-01-01" );
    }

    /**
     * Write DOAP homepage and old-homepage.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#homepage">http://usefulinc.com/ns/doap#homepage</a>
     * @see <a href="http://usefulinc.com/ns/doap#old-homepage">http://usefulinc.com/ns/doap#old-homepage</a>
     */
    private void writeHomepage( XMLWriter writer )
    {
        if ( StringUtils.isNotEmpty( project.getUrl() ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer,
                                            "URL of a project's homepage, associated with exactly one project.", 2 );
            // http://usefulinc.com/ns/doap#homepage
            DoapUtil.writeRdfResourceElement( writer, "homepage", project.getUrl() );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getOldHomepage() ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil
                .writeCommentText( writer, "URL of a project's past homepage, associated with exactly one project.", 2 );
            // http://usefulinc.com/ns/doap#old-homepage
            DoapUtil.writeRdfResourceElement( writer, "old-homepage", doapOptions.getOldHomepage() );
        }
    }

    /**
     * Write DOAP programming-language.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#programming-language">http://usefulinc.com/ns/doap#programming-language</a>
     */
    private void writeProgrammingLanguage( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getProgrammingLanguage() ) && StringUtils.isEmpty( language ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Programming language.", 2 );

        if ( StringUtils.isNotEmpty( language ) ) // backward compatible
        {
            // http://usefulinc.com/ns/doap#programming-language
            DoapUtil.writeRdfResourceElement( writer, "programming-language", language );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getProgrammingLanguage() ) )
        {
            String[] languages = StringUtils.split( doapOptions.getProgrammingLanguage(), "," );
            for ( int i = 0; i < languages.length; i++ )
            {
                // http://usefulinc.com/ns/doap#programming-language
                DoapUtil.writeRdfResourceElement( writer, "programming-language", languages[i].trim() );
            }
        }
    }

    /**
     * Write DOAP category.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#category">http://usefulinc.com/ns/doap#category</a>
     */
    private void writeCategory( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getCategory() ) && StringUtils.isEmpty( category ) )
        {
            return;
        }

        //TODO: how to lookup category, map it, or just declare it.
        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "A category of project.", 2 );

        if ( StringUtils.isNotEmpty( category ) ) // backward compatible
        {
            // http://usefulinc.com/ns/doap#category
            DoapUtil.writeRdfResourceElement( writer, "category", "http://projects.apache.org/category/" + category );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getCategory() ) )
        {
            String[] categories = StringUtils.split( doapOptions.getCategory(), "," );
            for ( int i = 0; i < categories.length; i++ )
            {
                // http://usefulinc.com/ns/doap#category
                DoapUtil.writeRdfResourceElement( writer, "category", "http://projects.apache.org/category/"
                    + categories[i].trim() );
            }
        }
    }

    /**
     * Write DOAP download-page and download-mirror.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#download-page">http://usefulinc.com/ns/doap#download-page</a>
     * @see <a href="http://usefulinc.com/ns/doap#download-mirror">http://usefulinc.com/ns/doap#download-mirror</a>
     */
    private void writeDownloadPage( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getDownloadPage() ) )
        {
            if ( StringUtils.isNotEmpty( project.getUrl() ) )
            {
                doapOptions.setDownloadPage( composeUrl( project.getUrl(), "/download.html" ) );
            }
        }

        if ( StringUtils.isNotEmpty( doapOptions.getDownloadPage() ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Download page.", 2 );
            // http://usefulinc.com/ns/doap#download-page
            DoapUtil.writeRdfResourceElement( writer, "download-page", doapOptions.getDownloadPage() );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getDownloadMirror() ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Mirror of software download web page.", 2 );
            String[] downloadMirrors = StringUtils.split( doapOptions.getDownloadMirror(), "," );
            for ( int i = 0; i < downloadMirrors.length; i++ )
            {
                // http://usefulinc.com/ns/doap#download-mirror
                DoapUtil.writeRdfResourceElement( writer, "download-mirror", downloadMirrors[i].trim() );
            }
        }
    }

    /**
     * Write DOAP OS.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#os">http://usefulinc.com/ns/doap#os</a>
     */
    private void writeOS( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getOs() ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Operating system that a project is limited to.", 2 );

        String[] oses = StringUtils.split( doapOptions.getOs(), "," );
        for ( int i = 0; i < oses.length; i++ )
        {
            // http://usefulinc.com/ns/doap#os
            DoapUtil.writeRdfResourceElement( writer, "os", oses[i].trim() );
        }
    }

    /**
     * Write DOAP screenshots.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#screenshots">http://usefulinc.com/ns/doap#screenshots</a>
     */
    private void writeScreenshots( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getScreenshots() ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Web page with screenshots of project.", 2 );
        // http://usefulinc.com/ns/doap#screenshots
        DoapUtil.writeRdfResourceElement( writer, "screenshots", doapOptions.getScreenshots() );
    }

    /**
     * Write DOAP licenses.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#license">http://usefulinc.com/ns/doap#license</a>
     */
    private void writeLicenses( XMLWriter writer )
    {
        if ( project.getLicenses() == null || project.getLicenses().size() == 0 )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "The URI of the license the software is distributed under.", 2 );
        //TODO: how to map to usefulinc site, or if this is necessary, the OSI page might
        //      be more appropriate.
        for ( Iterator it = project.getLicenses().iterator(); it.hasNext(); )
        {
            License license = (License) it.next();

            if ( StringUtils.isNotEmpty( license.getUrl() ) )
            {
                // http://usefulinc.com/ns/doap#license
                DoapUtil.writeRdfResourceElement( writer, "license", license.getUrl() );
            }
            else
            {
                getLog().warn( "No URL was specified for license " + license.getName() );
            }
        }
    }

    /**
     * Write DOAP bug-database.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#bug-database">http://usefulinc.com/ns/doap#bug-database</a>
     */
    private void writeBugDatabase( XMLWriter writer )
    {
        if ( project.getIssueManagement() == null )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "bug database.", 2 );
        if ( StringUtils.isNotEmpty( project.getIssueManagement().getUrl() ) )
        {
            // http://usefulinc.com/ns/doap#bug-database
            DoapUtil.writeRdfResourceElement( writer, "bug-database", project.getIssueManagement().getUrl() );
        }
        else
        {
            getLog().warn( "No URL was specified for issue management" );
        }
    }

    /**
     * Write DOAP mailing-list.
     *
     * @param writer
     * @see <a href="http://usefulinc.com/ns/doap#mailing-list">http://usefulinc.com/ns/doap#mailing-list</a>
     */
    private void writeMailingList( XMLWriter writer )
    {
        if ( project.getMailingLists() == null || project.getMailingLists().size() == 0 )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "mailing list.", 2 );
        for ( Iterator it = project.getMailingLists().iterator(); it.hasNext(); )
        {
            MailingList mailingList = (MailingList) it.next();

            if ( StringUtils.isNotEmpty( mailingList.getArchive() ) )
            {
                // http://usefulinc.com/ns/doap#mailing-list
                DoapUtil.writeRdfResourceElement( writer, "mailing-list", mailingList.getArchive() );
            }
            else
            {
                getLog().warn( "No archive was specified for mailing list " + mailingList.getName() );
            }

            if ( mailingList.getOtherArchives() != null )
            {
                for ( Iterator it2 = mailingList.getOtherArchives().iterator(); it2.hasNext(); )
                {
                    String otherArchive = (String) it2.next();

                    if ( StringUtils.isNotEmpty( otherArchive ) )
                    {
                        // http://usefulinc.com/ns/doap#mailing-list
                        DoapUtil.writeRdfResourceElement( writer, "mailing-list", otherArchive );
                    }
                    else
                    {
                        getLog().warn( "No other archive was specified for mailing list " + mailingList.getName() );
                    }
                }
            }
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
     * @see <a href="http://usefulinc.com/ns/doap#Repository">http://usefulinc.com/ns/doap#Repository</a>
     * @see <a href="http://usefulinc.com/ns/doap#CVSRepository">http://usefulinc.com/ns/doap#CVSRepository</a>
     * @see <a href="http://usefulinc.com/ns/doap#SVNRepository">http://usefulinc.com/ns/doap#SVNRepository</a>
     */
    private void writeSourceRepositories( XMLWriter writer )
    {
        Scm scm = project.getScm();
        if ( scm == null )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Anonymous Source Repository", 2 );
        String anonymousConnection = scm.getConnection();
        writeSourceRepository( writer, anonymousConnection );

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Developer Source Repository", 2 );
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

            CvsScmProviderRepository cvsRepo = (CvsScmProviderRepository) repository.getProviderRepository();

            // http://usefulinc.com/ns/doap#anon-root
            DoapUtil.writeElement( writer, "anon-root", cvsRepo.getCvsRoot() );
            // http://usefulinc.com/ns/doap#module
            DoapUtil.writeElement( writer, "module", cvsRepo.getModule() );
        }
        else if ( isScmSystem( repository, "svn" ) )
        {
            // http://usefulinc.com/ns/doap#SVNRepository
            writer.startElement( "SVNRepository" );

            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

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

    /**
     * Write all DOAP persons.
     *
     * @param writer not null
     * @param developersOrContributors list of developers or contributors
     */
    private void writeDevelopersOrContributors( XMLWriter writer, List developersOrContributors )
    {
        if ( developersOrContributors == null || developersOrContributors.size() == 0 )
        {
            return;
        }

        boolean isDeveloper = Developer.class.isAssignableFrom( developersOrContributors.get( 0 ).getClass() );
        if ( isDeveloper )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Main committers", 2 );
        }
        else
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Contributed persons", 2 );
        }

        List maintainers = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "maintainers" );
        List developers = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "developers" );
        List documenters = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "documenters" );
        List translators = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "translators" );
        List testers = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "testers" );
        List helpers = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "helpers" );
        List unknowns = (List) DoapUtil.filterDevelopersOrContributorsByDoapRoles( developersOrContributors )
            .get( "unknowns" );

        // By default, all developers are maintainers and contributors are helpers
        if ( isDeveloper )
        {
            maintainers.addAll( unknowns );
        }
        else
        {
            helpers.addAll( unknowns );
        }

        // all alphabetical
        if ( developers.size() != 0 )
        {
            writeDeveloperOrContributor( writer, developers, "developer" );
        }
        if ( documenters.size() != 0 )
        {
            writeDeveloperOrContributor( writer, documenters, "documenter" );
        }
        if ( helpers.size() != 0 )
        {
            writeDeveloperOrContributor( writer, helpers, "helper" );
        }
        if ( maintainers.size() != 0 )
        {
            writeDeveloperOrContributor( writer, maintainers, "maintainer" );
        }
        if ( testers.size() != 0 )
        {
            writeDeveloperOrContributor( writer, testers, "tester" );
        }
        if ( translators.size() != 0 )
        {
            writeDeveloperOrContributor( writer, translators, "translator" );
        }
    }

    /**
     * Write a DOAP maintainer or developer or documenter or translator or tester or helper, for instance:
     * <pre>
     *   &lt;maintainer&gt;
     *     &lt;foaf:Person&gt;
     *       &lt;foaf:name&gt;Emmanuel Venisse&lt;/foaf:name&gt;
     *       &lt;foaf:mbox rdf:resource="mailto:evenisse@apache.org"/&gt;
     *     &lt;/foaf:Person&gt;
     *   &lt;/maintainer&gt;
     * </pre>
     *
     * @param writer not null
     * @param developersOrContributors not null
     * @param doapType not null
     * @see <a href="http://usefulinc.com/ns/doap#maintainer">http://usefulinc.com/ns/doap#maintainer</a>
     * @see <a href="http://usefulinc.com/ns/doap#developer">http://usefulinc.com/ns/doap#developer</a>
     * @see <a href="http://usefulinc.com/ns/doap#documenter">http://usefulinc.com/ns/doap#documenter</a>
     * @see <a href="http://usefulinc.com/ns/doap#translator">http://usefulinc.com/ns/doap#translator</a>
     * @see <a href="http://usefulinc.com/ns/doap#tester">http://usefulinc.com/ns/doap#tester</a>
     * @see <a href="http://usefulinc.com/ns/doap#helper">http://usefulinc.com/ns/doap#helper</a>
     */
    private void writeDeveloperOrContributor( XMLWriter writer, List developersOrContributors, String doapType )
    {
        if ( developersOrContributors == null || developersOrContributors.size() == 0 )
        {
            return;
        }

        // Sort list by names
        Collections.sort( developersOrContributors, new Comparator()
        {
            /**
             * {@inheritDoc}
             */
            public int compare( Object arg0, Object arg1 )
            {
                if ( Developer.class.isAssignableFrom( arg0.getClass() ) )
                {
                    Developer developer0 = (Developer) arg0;
                    Developer developer1 = (Developer) arg1;

                    if ( developer0.getName() == null )
                    {
                        return -1;
                    }
                    if ( developer1.getName() == null )
                    {
                        return +1;
                    }

                    return developer0.getName().compareTo( developer1.getName() );
                }

                Contributor contributor0 = (Contributor) arg0;
                Contributor contributor1 = (Contributor) arg1;

                if ( contributor0.getName() == null )
                {
                    return -1;
                }
                if ( contributor1.getName() == null )
                {
                    return +1;
                }

                return contributor0.getName().compareTo( contributor1.getName() );
            }
        } );

        for ( Iterator it = developersOrContributors.iterator(); it.hasNext(); )
        {
            Object obj = it.next();

            String name;
            String email;
            String organization;
            String homepage;

            if ( Developer.class.isAssignableFrom( obj.getClass() ) )
            {
                Developer d = (Developer) obj;
                name = d.getName();
                email = d.getEmail();
                organization = d.getOrganization();
                homepage = d.getUrl();
            }
            else
            {
                Contributor c = (Contributor) obj;
                name = c.getName();
                email = c.getEmail();
                organization = c.getOrganization();
                homepage = c.getUrl();
            }

            // Name is required to write doap
            if ( StringUtils.isEmpty( name ) )
            {
                continue;
            }

            // http://usefulinc.com/ns/doap#maintainer
            // http://usefulinc.com/ns/doap#developer
            // http://usefulinc.com/ns/doap#documenter
            // http://usefulinc.com/ns/doap#translator
            // http://usefulinc.com/ns/doap#tester
            // http://usefulinc.com/ns/doap#helper
            writer.startElement( doapType );
            // http://xmlns.com/foaf/0.1/Person
            writer.startElement( "foaf:Person" );
            // http://xmlns.com/foaf/0.1/name
            writer.startElement( "foaf:name" );
            writer.writeText( name );
            writer.endElement(); // foaf:name
            if ( StringUtils.isNotEmpty( email ) )
            {
                // http://xmlns.com/foaf/0.1/mbox
                DoapUtil.writeRdfResourceElement( writer, "foaf:mbox", "mailto:" + email );
            }
            if ( StringUtils.isNotEmpty( organization ) )
            {
                // http://xmlns.com/foaf/0.1/Organization
                DoapUtil.writeRdfResourceElement( writer, "foaf:Organization", organization );
            }
            if ( StringUtils.isNotEmpty( homepage ) )
            {
                // http://xmlns.com/foaf/0.1/homepage
                DoapUtil.writeRdfResourceElement( writer, "foaf:homepage", homepage );
            }
            writer.endElement(); // foaf:Person
            writer.endElement(); // doapType
        }
    }

    /**
     * Return a <code>SCM repository</code> defined by a given url
     *
     * @param scmUrl an SCM URL
     * @return a valid SCM repository or null
     */
    private ScmRepository getScmRepository( String scmUrl )
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

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

    /**
     * Compose a URL from two parts: a base URL and a file path. This method
     * makes sure that there will not be two slash '/' characters after each
     * other.
     *
     * @param base The base URL
     * @param path The file
     */
    private static String composeUrl( String base, String path )
    {
        if ( base.endsWith( "/" ) && path.startsWith( "/" ) )
        {
            return base + path.substring( 1 );
        }

        return base + path;
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
