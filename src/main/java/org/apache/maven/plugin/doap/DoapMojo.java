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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.doap.options.ASFExtOptions;
import org.apache.maven.plugin.doap.options.DoapOptions;
import org.apache.maven.plugin.doap.options.Standard;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.cvslib.repository.CvsScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;

/**
 * Generate a <a href="http://usefulinc.com/ns/doap">Description of a Project (DOAP)</a> file from the main information
 * found in a POM. <br/>
 * <b>Note</b>: The generated file is tailored for use by projects at <a
 * href="http://projects.apache.org/doap.html">Apache</a>.
 *
 * @author Jason van Zyl
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 1.0-beta-1
 * @goal generate
 */
public class DoapMojo
    extends AbstractMojo
{
    /** UTC Time Zone */
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    /** Date format for <lastUpdated/> tag in the repository metadata, i.e.: yyyyMMddHHmmss */
    private static final DateFormat REPOSITORY_DATE_FORMAT;

    /** Date format for DOAP file, i.e. ISO-8601 YYYY-MM-DD */
    private static final DateFormat DOAP_DATE_FORMAT;

    static
    {
        REPOSITORY_DATE_FORMAT = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.ENGLISH );
        REPOSITORY_DATE_FORMAT.setTimeZone( UTC_TIME_ZONE );

        DOAP_DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd", Locale.ENGLISH );
        DOAP_DATE_FORMAT.setTimeZone( UTC_TIME_ZONE );
    }

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven SCM Manager.
     *
     * @component
     * @since 1.0
     */
    private ScmManager scmManager;

    /**
     * Artifact factory.
     *
     * @component
     * @since 1.0
     */
    private ArtifactFactory artifactFactory;

    /**
     * Used to resolve artifacts.
     *
     * @component
     * @since 1.0
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * Internationalization component.
     *
     * @component
     * @since 1.0
     */
    private I18N i18n;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The POM from which information will be extracted to create a DOAP file.
     *
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * The name of the DOAP file that will be generated.
     *
     * @parameter expression="${doapFile}"
     *            default-value="${project.reporting.outputDirectory}/doap_${project.artifactId}.rdf"
     * @required
     */
    private File doapFile;

    /**
     * The local repository where the artifacts are located.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     * @since 1.0
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where the artifacts are located.
     *
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     * @since 1.0
     */
    private List<ArtifactRepository> remoteRepositories;

    // ----------------------------------------------------------------------
    // Doap options
    // ----------------------------------------------------------------------

    /**
     * The category which should be displayed in the DOAP file.
     *
     * @parameter expression="${category}"
     * @deprecated Since 1.0. Instead of, configure
     *             <code>&lt;doapOptions&gt;&lt;category/&gt;&lt;/doapOptions&gt;</code> parameter.
     */
    private String category;

    /**
     * The programming language which should be displayed in the DOAP file.
     *
     * @parameter expression="${language}"
     * @deprecated Since 1.0. Instead of, configure
     *             <code>&lt;doapOptions&gt;&lt;programmingLanguage/&gt;&lt;/doapOptions&gt;</code> parameter.
     */
    private String language;

    /**
     * Specific DOAP parameters, i.e. options that POM doesn't have any notions. <br/>
     * Example:
     *
     * <pre>
     * &lt;doapOptions&gt;
     * &nbsp;&nbsp;&lt;programmingLanguage&gt;java&lt;/programmingLanguage&gt;
     * &lt;/doapOptions&gt;
     * </pre>
     *
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/doap/options/DoapOptions.html">Javadoc</a> <br/>
     *
     * @parameter expression="${doapOptions}"
     * @since 1.0
     * @see <a href="http://usefulinc.com/ns/doap#">http://usefulinc.com/ns/doap#</a>
     */
    private DoapOptions doapOptions;

    /**
     * Specific ASF extensions parameters, i.e. options that POM doesn't have any notions but required by ASF DOAP
     * requirements. <br/>
     * Example:
     *
     * <pre>
     * &lt;asfExtOptions&gt;
     * &nbsp;&nbsp;&lt;included&gt;true&lt;/included&gt;
     * &nbsp;&nbsp;&lt;charter&gt;The mission of the Apache XXX project is to create and maintain software
     * &nbsp;&nbsp;libraries that provide ...&lt;/charter&gt;
     * &nbsp;&nbsp;...
     * &lt;/asfExtOptions&gt;
     * </pre>
     *
     * <b>Note</b>: By default, <code>&lt;asfExtOptions&gt;&lt;included/&gt;&lt;/asfExtOptions&gt;</code> will be
     * automatically set to <code>true</code> if the project is hosted at ASF. <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/doap/options/ASFExtOptions.html">Javadoc</a> <br/>
     *
     * @parameter expression="${asfExtOptions}"
     * @since 1.0
     * @see <a href="http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext">
     *      http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext</a>
     * @see <a href="http://projects.apache.org/docs/pmc.html">http://projects.apache.org/docs/pmc.html</a>
     * @see <a href="http://projects.apache.org/docs/standards.html">http://projects.apache.org/docs/standards.html</a>
     * @see ASFExtOptions#isASFProject(MavenProject)
     */
    private ASFExtOptions asfExtOptions;

    /**
     * The value for the <code>xml:lang</code> attribute used by the <code>&lt;rdf:RDF/&gt;<code>,
     * <code>&lt;description/&gt;</code> and <code>&lt;shortdesc/&gt;</code> elements. <br/>
     * POM doesn't have any notions about language. <br/>
     * See <a href="http://www.w3.org/TR/REC-xml/#sec-lang-tag">http://www.w3.org/TR/REC-xml/#sec-lang-tag</a> <br/>
     *
     * @parameter expression="${lang}" default-value="en"
     * @required
     * @since 1.0
     */
    private String lang;

    /**
     * The <code>about</code> URI-reference which should be displayed in the DOAP file. Example:
     *
     * <pre>
     * &lt;rdf:RDF&gt;
     * &nbsp;&nbsp;&lt;Project rdf:about="http://maven.apache.org/"&gt;
     * &nbsp;&nbsp;...
     * &nbsp;&nbsp;&lt;/Project&gt;
     * &lt;/rdf:RDF&gt;
     * </pre>
     *
     * See <a href="http://www.w3.org/TR/1999/REC-rdf-syntax-19990222/#aboutAttr">
     * http://www.w3.org/TR/1999/REC-rdf-syntax-19990222/#aboutAttr</a> <br/>
     *
     * @parameter expression="${about}" default-value="${project.url}"
     * @since 1.0
     */
    private String about;

    /**
     * Flag to validate the generated DOAP.
     *
     * @parameter default-value="true"
     * @since 1.1
     */
    private boolean validate;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    @SuppressWarnings( "unchecked" )
    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // Includes ASF extensions
        // ----------------------------------------------------------------------------

        if ( !asfExtOptions.isIncluded() && ASFExtOptions.isASFProject( project ) )
        {
            getLog().info( "This project is an ASF project, ASF Extensions to DOAP will be added." );
            asfExtOptions.setIncluded( true );
        }

        // ----------------------------------------------------------------------------
        // setup pretty print xml writer
        // ----------------------------------------------------------------------------

        Writer w;
        try
        {
            if ( !doapFile.getParentFile().exists() )
            {
                FileUtils.mkdir( doapFile.getParentFile().getAbsolutePath() );
            }

            w = WriterFactory.newXmlWriter( doapFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating DOAP file.", e );
        }

        if ( asfExtOptions.isIncluded() )
        {
            getLog().info( "Generating an ASF DOAP file..." );
        }
        else
        {
            getLog().info( "Generating a pure DOAP file..." );
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w, project.getModel().getModelEncoding(), null );

        // ----------------------------------------------------------------------------
        // Convert POM to DOAP
        // ----------------------------------------------------------------------------

        DoapUtil.writeHeader( writer );

        // Heading
        writer.startElement( "rdf:RDF" );
        writer.addAttribute( "xml:lang", lang );
        writer.addAttribute( "xmlns", "http://usefulinc.com/ns/doap#" );
        writer.addAttribute( "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
        writer.addAttribute( "xmlns:foaf", "http://xmlns.com/foaf/0.1/" );
        if ( asfExtOptions.isIncluded() )
        {
            writer.addAttribute( "xmlns:asfext", ASFExtOptions.ASFEXT_NAMESPACE );
        }

        // Project
        writer.startElement( "Project" );
        if ( StringUtils.isNotEmpty( about ) )
        {
            writer.addAttribute( "rdf:about", about );
        }
        else
        {
            getLog().warn( "rdf:about should be required" );
        }

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

        // wiki
        writeWiki( writer );

        // Releases
        writeReleases( writer );

        // Developers
        writeContributors( writer, project.getDevelopers() );

        // Contributors
        writeContributors( writer, project.getContributors() );

        // ASFext
        if ( asfExtOptions.isIncluded() )
        {
            writeASFext( writer );
        }

        writer.endElement(); // Project

        writeOrganizations( writer );

        writer.endElement(); // rdf:RDF

        try
        {
            w.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error when closing the writer.", e );
        }

        if ( validate )
        {
            List<String> errors = DoapUtil.validate( doapFile );
            if ( !errors.isEmpty() )
            {
                for ( int i = 0; i < errors.size(); i++ )
                {
                    getLog().error( errors.get( i ).toString() );
                }

                throw new MojoExecutionException( "Error parsing the generated doap file, see above." );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Write DOAP name.
     *
     * @param writer not null
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

        if ( asfExtOptions.isIncluded()
            && !project.getName().toLowerCase( Locale.ENGLISH ).trim().startsWith( "apache" ) )
        {
            DoapUtil.writeElement( writer, "name", "Apache " + project.getName() );
        }
        else
        {
            DoapUtil.writeElement( writer, "name", project.getName() );
        }
    }

    /**
     * Write DOAP description.
     *
     * @param writer not null
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
        DoapUtil.writeElement( writer, "description", project.getDescription(), lang );
        if ( StringUtils.isNotEmpty( doapOptions.getShortdesc() ) )
        {
            DoapUtil.writeElement( writer, "shortdesc", doapOptions.getShortdesc(), lang );
        }
        else
        {
            DoapUtil.writeElement( writer, "shortdesc", project.getDescription(), lang );
        }
    }

    /**
     * Write DOAP created.
     *
     * @param writer not null
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
        DoapUtil.writeElement( writer, "created", project.getInceptionYear() + "-01-01" );
    }

    /**
     * Write DOAP homepage and old-homepage.
     *
     * @param writer not null
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
            DoapUtil.writeRdfResourceElement( writer, "homepage", project.getUrl() );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getOldHomepage() ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer,
                                            "URL of a project's past homepage, associated with exactly one project.", 2 );
            DoapUtil.writeRdfResourceElement( writer, "old-homepage", doapOptions.getOldHomepage() );
        }
    }

    /**
     * Write DOAP programming-language.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#programming-language">
     *      http://usefulinc.com/ns/doap#programming-language</a>
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
            if ( asfExtOptions.isIncluded() && !ASFExtOptions.isProgrammingLanguageSupportedByASF( language ) )
            {
                getLog().warn( "The programming language '" + language + "' is not supported by ASF. "
                                   + "Refer you to http://projects.apache.org/languages.html" );
            }

            DoapUtil.writeElement( writer, "programming-language", language );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getProgrammingLanguage() ) )
        {
            String[] languages = StringUtils.split( doapOptions.getProgrammingLanguage(), "," );
            for ( int i = 0; i < languages.length; i++ )
            {
                if ( asfExtOptions.isIncluded()
                    && !ASFExtOptions.isProgrammingLanguageSupportedByASF( languages[i].trim() ) )
                {
                    getLog().warn( "The programming language '" + languages[i].trim() + "' is not supported by ASF. "
                                       + "Refer you to http://projects.apache.org/languages.html" );
                }

                DoapUtil.writeElement( writer, "programming-language", languages[i].trim() );
            }
        }
    }

    /**
     * Write DOAP category.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#category">http://usefulinc.com/ns/doap#category</a>
     */
    private void writeCategory( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getCategory() ) && StringUtils.isEmpty( category ) )
        {
            return;
        }

        // TODO: how to lookup category, map it, or just declare it.
        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "A category of project.", 2 );

        if ( StringUtils.isNotEmpty( category ) ) // backward compatible
        {
            if ( asfExtOptions.isIncluded() && !ASFExtOptions.isCategorySupportedByASF( category ) )
            {
                getLog().warn( "The given category '" + category + "' is not supported by ASF. "
                                   + "Refer you to http://projects.apache.org/categories.html" );
            }

            if ( asfExtOptions.isIncluded() )
            {
                DoapUtil.writeRdfResourceElement( writer, "category", "http://projects.apache.org/category/" + category );
            }
            else
            {
                DoapUtil.writeRdfResourceElement( writer, "category", category );
            }
        }

        if ( StringUtils.isNotEmpty( doapOptions.getCategory() ) )
        {
            String[] categories = StringUtils.split( doapOptions.getCategory(), "," );
            for ( int i = 0; i < categories.length; i++ )
            {
                if ( asfExtOptions.isIncluded() && !ASFExtOptions.isCategorySupportedByASF( categories[i] ) )
                {
                    getLog().warn( "The given category '" + categories[i] + "' is not supported by ASF. "
                                       + "Refer you to http://projects.apache.org/categories.html" );
                }

                if ( asfExtOptions.isIncluded() )
                {
                    DoapUtil.writeRdfResourceElement( writer, "category", "http://projects.apache.org/category/"
                        + categories[i].trim() );
                }
                else
                {
                    DoapUtil.writeRdfResourceElement( writer, "category", categories[i].trim() );
                }
            }
        }
    }

    /**
     * Write DOAP download-page and download-mirror.
     *
     * @param writer not null
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
            DoapUtil.writeRdfResourceElement( writer, "download-page", doapOptions.getDownloadPage() );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getDownloadMirror() ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Mirror of software download web page.", 2 );
            String[] downloadMirrors = StringUtils.split( doapOptions.getDownloadMirror(), "," );
            for ( int i = 0; i < downloadMirrors.length; i++ )
            {
                DoapUtil.writeRdfResourceElement( writer, "download-mirror", downloadMirrors[i].trim() );
            }
        }
    }

    /**
     * Write DOAP OS.
     *
     * @param writer not null
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
            DoapUtil.writeElement( writer, "os", oses[i].trim() );
        }
    }

    /**
     * Write DOAP screenshots.
     *
     * @param writer not null
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
        DoapUtil.writeRdfResourceElement( writer, "screenshots", doapOptions.getScreenshots() );
    }

    /**
     * Write DOAP wiki.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#wiki">http://usefulinc.com/ns/doap#wiki</a>
     */
    private void writeWiki( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getWiki() ) )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "URL of Wiki for collaborative discussion of project.", 2 );
        DoapUtil.writeRdfResourceElement( writer, "wiki", doapOptions.getWiki() );
    }

    /**
     * Write DOAP licenses.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#license">http://usefulinc.com/ns/doap#license</a>
     */
    private void writeLicenses( XMLWriter writer )
    {
        if ( project.getLicenses() == null || project.getLicenses().isEmpty() )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "The URI of the license the software is distributed under.", 2 );
        // TODO: how to map to usefulinc site, or if this is necessary, the OSI page might
        // be more appropriate.
        @SuppressWarnings( "unchecked" )
        List<License> licenses = project.getLicenses();
        for ( License license : licenses )
        {
            if ( StringUtils.isNotEmpty( license.getUrl() ) )
            {
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
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#bug-database">http://usefulinc.com/ns/doap#bug-database</a>
     */
    private void writeBugDatabase( XMLWriter writer )
    {
        if ( project.getIssueManagement() == null )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Bug database.", 2 );
        if ( StringUtils.isNotEmpty( project.getIssueManagement().getUrl() ) )
        {
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
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#mailing-list">http://usefulinc.com/ns/doap#mailing-list</a>
     * @see DoapOptions#getMailingList()
     * @see MavenProject#getMailingLists()
     */
    private void writeMailingList( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getMailingList() ) || project.getMailingLists() == null || project.getMailingLists().isEmpty() )
        {
            return;
        }

        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "Mailing lists.", 2 );
        if ( StringUtils.isNotEmpty( doapOptions.getMailingList() ) )
        {
            DoapUtil.writeRdfResourceElement( writer, "mailing-list", doapOptions.getMailingList() );
        }
        else
        {
            @SuppressWarnings( "unchecked" )
            List<MailingList> mailingLists = project.getMailingLists();
            for ( MailingList mailingList : mailingLists )
            {
                if ( StringUtils.isNotEmpty( mailingList.getArchive() ) )
                {
                    DoapUtil.writeRdfResourceElement( writer, "mailing-list", mailingList.getArchive() );
                }
                else
                {
                    getLog().warn( "No archive was specified for mailing list " + mailingList.getName() );

                    if ( mailingList.getOtherArchives() != null )
                    {
                        for ( String otherArchive : mailingList.getOtherArchives() )
                        {
                            if ( StringUtils.isNotEmpty( otherArchive ) )
                            {
                                DoapUtil.writeRdfResourceElement( writer, "mailing-list", otherArchive );
                            }
                            else
                            {
                                getLog().warn( "No other archive was specified for mailing list "
                                                   + mailingList.getName() );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Write all DOAP releases.
     *
     * @param writer not null
     * @throws MojoExecutionException if any
     * @see <a href="http://usefulinc.com/ns/doap#release">http://usefulinc.com/ns/doap#release</a>
     * @see <a href="http://usefulinc.com/ns/doap#Version">http://usefulinc.com/ns/doap#Version</a>
     */
    private void writeReleases( XMLWriter writer )
        throws MojoExecutionException
    {
        Artifact artifact =
            artifactFactory.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null,
                                            project.getPackaging() );
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );

        for ( ArtifactRepository repo : remoteRepositories )
        {
            if ( repo.isBlacklisted() )
            {
                continue;
            }
            if ( repo.getSnapshots().isEnabled() )
            {
                continue;
            }
            if ( repo.getReleases().isEnabled() )
            {
                try
                {
                    repositoryMetadataManager.resolveAlways( metadata, localRepository, repo );
                    break;
                }
                catch ( RepositoryMetadataResolutionException e )
                {
                    throw new MojoExecutionException( metadata
                        + " could not be retrieved from repositories due to an error: " + e.getMessage(), e );
                }
            }
        }

        if ( metadata.getMetadata().getVersioning() == null )
        {
            getLog().info( "No versioning was found - ignored writing <release/> tag." );
            return;
        }

        List<String> versions = metadata.getMetadata().getVersioning().getVersions();

        // Recent releases in first
        Collections.reverse( versions );
        boolean addComment = false;
        int i = 0;
        for ( String version : versions )
        {
            if ( !addComment )
            {
                XmlWriterUtil.writeLineBreak( writer );
                XmlWriterUtil.writeCommentText( writer, "Project releases.", 2 );
                addComment = true;
            }

            writer.startElement( "release" );
            writer.startElement( "Version" );

            writer.startElement( "name" );
            if ( version.equals( metadata.getMetadata().getVersioning().getRelease() ) )
            {
                writer.writeText( "Latest stable release" );
            }
            else
            {
                writer.writeText( project.getName() + " - " + version );
            }
            writer.endElement(); // name

            writer.startElement( "revision" );
            writer.writeText( version );
            writer.endElement(); // revision

            // list all file release from all remote repos
            for ( ArtifactRepository repo : remoteRepositories )
            {
                Artifact artifactRelease =
                    artifactFactory.createArtifact( project.getGroupId(), project.getArtifactId(), version, null,
                                                    project.getPackaging() );

                if ( artifactRelease == null )
                {
                    continue;
                }

                String fileRelease = repo.getUrl() + "/" + repo.pathOf( artifactRelease );
                // try to ping the url
                try
                {
                    URL urlRelease = new URL( fileRelease );
                    urlRelease.openStream();
                }
                catch ( MalformedURLException e )
                {
                    getLog().debug( e.getMessage(), e );
                    continue;
                }
                catch ( IOException e )
                {
                    // Not found, ignored
                    getLog().debug( e.getMessage(), e );
                    continue;
                }

                writer.startElement( "file-release" );
                writer.writeText( fileRelease );
                writer.endElement(); // file-release

                Date releaseDate = null;
                try
                {
                    releaseDate =
                        REPOSITORY_DATE_FORMAT.parse( metadata.getMetadata().getVersioning().getLastUpdated() );
                }
                catch ( ParseException e )
                {
                    getLog().error( "Unable to parse date '" + metadata.getMetadata().getVersioning().getLastUpdated()
                                        + "'" );
                    continue;
                }

                // See MDOAP-11
                if ( i == 0 )
                {
                    writer.startElement( "created" );
                    writer.writeText( DOAP_DATE_FORMAT.format( releaseDate ) );
                    writer.endElement(); // created
                }
            }

            writer.endElement(); // Version
            writer.endElement(); // release

            i++;
        }
    }

    /**
     * Write all DOAP repositories.
     *
     * @param writer not null
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

        String anonymousConnection = scm.getConnection();
        if ( StringUtils.isNotEmpty( anonymousConnection ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Anonymous Source Repository", 2 );
            writeSourceRepository( writer, anonymousConnection );
        }

        String developerConnection = scm.getDeveloperConnection();
        if ( StringUtils.isNotEmpty( developerConnection ) )
        {
            XmlWriterUtil.writeLineBreak( writer );
            XmlWriterUtil.writeCommentText( writer, "Developer Source Repository", 2 );
            writeSourceRepository( writer, developerConnection );
        }
    }

    /**
     * Write a DOAP repository, for instance:
     *
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
            writer.startElement( "CVSRepository" );

            CvsScmProviderRepository cvsRepo = (CvsScmProviderRepository) repository.getProviderRepository();

            DoapUtil.writeElement( writer, "anon-root", cvsRepo.getCvsRoot() );
            DoapUtil.writeElement( writer, "module", cvsRepo.getModule() );
        }
        else if ( isScmSystem( repository, "svn" ) )
        {
            writer.startElement( "SVNRepository" );

            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

            DoapUtil.writeRdfResourceElement( writer, "location", svnRepo.getUrl() );
        }
        else
        {
            /*
             * Supported DOAP repositories actually unsupported by SCM: BitKeeper
             * (http://usefulinc.com/ns/doap#BKRepository) Arch (http://usefulinc.com/ns/doap#ArchRepository) Other SCM
             * repos are unsupported by DOAP.
             */
            writer.startElement( "Repository" );

            if ( connection.length() < 4 )
            {
                throw new IllegalArgumentException( "The source repository connection is too short." );
            }

            DoapUtil.writeRdfResourceElement( writer, "location", connection.substring( 4 ) );
        }

        DoapUtil.writeRdfResourceElement( writer, "browse", project.getScm().getUrl() );

        writer.endElement(); // CVSRepository || SVNRepository || Repository
        writer.endElement(); // repository
    }

    /**
     * Write all DOAP persons.
     *
     * @param writer not null
     * @param contributors list of developers or contributors
     */
    private void writeContributors( XMLWriter writer, List<Contributor> contributors )
    {
        if ( contributors == null || contributors.isEmpty() )
        {
            return;
        }

        boolean isDeveloper = Developer.class.isAssignableFrom( contributors.get( 0 ).getClass() );
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

        List<Contributor> maintainers = DoapUtil.getContributorsWithMaintainerRole( i18n, contributors );
        List<Contributor> developers = DoapUtil.getContributorsWithDeveloperRole( i18n, contributors );
        List<Contributor> documenters = DoapUtil.getContributorsWithDocumenterRole( i18n, contributors );
        List<Contributor> translators = DoapUtil.getContributorsWithTranslatorRole( i18n, contributors );
        List<Contributor> testers = DoapUtil.getContributorsWithTesterRole( i18n, contributors );
        List<Contributor> helpers = DoapUtil.getContributorsWithHelperRole( i18n, contributors );
        List<Contributor> unknowns = DoapUtil.getContributorsWithUnknownRole( i18n, contributors );

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
            writeContributor( writer, developers, "developer" );
        }
        if ( documenters.size() != 0 )
        {
            writeContributor( writer, documenters, "documenter" );
        }
        if ( helpers.size() != 0 )
        {
            writeContributor( writer, helpers, "helper" );
        }
        if ( maintainers.size() != 0 )
        {
            writeContributor( writer, maintainers, "maintainer" );
        }
        if ( testers.size() != 0 )
        {
            writeContributor( writer, testers, "tester" );
        }
        if ( translators.size() != 0 )
        {
            writeContributor( writer, translators, "translator" );
        }
    }

    /**
     * Write a DOAP maintainer or developer or documenter or translator or tester or helper, for instance:
     *
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
     * @param developersOrContributors list of <code>{@link Developer}/{@link Contributor}</code>
     * @param doapType not null
     * @see #writeContributor(XMLWriter, Object, String)
     */
    private void writeContributor( XMLWriter writer, List<Contributor> developersOrContributors, String doapType )
    {
        if ( developersOrContributors == null || developersOrContributors.isEmpty() )
        {
            return;
        }

        // Sort list by names
        Collections.sort( developersOrContributors, new Comparator<Contributor>()
        {
            /** {@inheritDoc} */
            public int compare( Contributor arg0, Contributor arg1 )
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

                Contributor contributor0 = arg0;
                Contributor contributor1 = arg1;

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

        for ( Contributor developersOrContributor : developersOrContributors )
        {
            writeContributor( writer, developersOrContributor, doapType );
        }
    }

    /**
     * Writer a single developer or contributor
     *
     * @param writer not null
     * @param developerOrContributor not null, instance of <code>{@link Developer}/{@link Contributor}</code>
     * @param doapType not null
     * @see <a href="http://usefulinc.com/ns/doap#maintainer">http://usefulinc.com/ns/doap#maintainer</a>
     * @see <a href="http://usefulinc.com/ns/doap#developer">http://usefulinc.com/ns/doap#developer</a>
     * @see <a href="http://usefulinc.com/ns/doap#documenter">http://usefulinc.com/ns/doap#documenter</a>
     * @see <a href="http://usefulinc.com/ns/doap#translator">http://usefulinc.com/ns/doap#translator</a>
     * @see <a href="http://usefulinc.com/ns/doap#tester">http://usefulinc.com/ns/doap#tester</a>
     * @see <a href="http://usefulinc.com/ns/doap#helper">http://usefulinc.com/ns/doap#helper</a>
     * @see <a href="http://xmlns.com/foaf/0.1/Person">http://xmlns.com/foaf/0.1/Person</a>
     * @see <a href="http://xmlns.com/foaf/0.1/name">http://xmlns.com/foaf/0.1/name</a>
     * @see <a href="http://xmlns.com/foaf/0.1/mbox">http://xmlns.com/foaf/0.1/mbox</a>
     * @see <a href="http://xmlns.com/foaf/0.1/Organization">http://xmlns.com/foaf/0.1/Organization</a>
     * @see <a href="http://xmlns.com/foaf/0.1/homepage">http://xmlns.com/foaf/0.1/homepage</a>
     */
    private void writeContributor( XMLWriter writer, Contributor developerOrContributor, String doapType )
    {
        if ( developerOrContributor == null )
        {
            return;
        }

        if ( StringUtils.isEmpty( doapType ) )
        {
            throw new IllegalArgumentException( "doapType is required." );
        }

        String name = developerOrContributor.getName();
        String email = developerOrContributor.getEmail();
        String organization = developerOrContributor.getOrganization();
        String organizationUrl = developerOrContributor.getOrganizationUrl();
        String homepage = developerOrContributor.getUrl();
        String nodeId = null;

        // Name is required to write doap
        if ( StringUtils.isEmpty( name ) )
        {
            return;
        }

        if ( !StringUtils.isEmpty( organization ) || !StringUtils.isEmpty( organizationUrl ) )
        {
            DoapUtil.Organization doapOrganization = DoapUtil.addOrganization( organization, organizationUrl );
            nodeId = DoapUtil.getNodeId();
            doapOrganization.addMember( nodeId );
        }

        writer.startElement( doapType );
        writer.startElement( "foaf:Person" );
        if ( StringUtils.isNotEmpty( nodeId ) )
        {
            writer.addAttribute( "rdf:nodeID", nodeId );
        }
        writer.startElement( "foaf:name" );
        writer.writeText( name );
        writer.endElement(); // foaf:name
        if ( StringUtils.isNotEmpty( email ) )
        {
            DoapUtil.writeRdfResourceElement( writer, "foaf:mbox", "mailto:" + email );
        }
        if ( StringUtils.isNotEmpty( organization ) || StringUtils.isNotEmpty( organizationUrl ) )
        {
            DoapUtil.addOrganization( organization, organizationUrl );
        }
        if ( StringUtils.isNotEmpty( homepage ) )
        {
            DoapUtil.writeRdfResourceElement( writer, "foaf:homepage", homepage );
        }
        writer.endElement(); // foaf:Person
        writer.endElement(); // doapType
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

    /**
     * Write the ASF extensions
     *
     * @param writer not null
     * @see <a href="http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext">
     *      http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext</a>
     * @see <a href="http://projects.apache.org/docs/pmc.html">http://projects.apache.org/docs/pmc.html</a>
     */
    private void writeASFext( XMLWriter writer )
    {
        XmlWriterUtil.writeLineBreak( writer );
        XmlWriterUtil.writeCommentText( writer, "ASF extension", 2 );

        // asfext:pmc
        if ( StringUtils.isNotEmpty( asfExtOptions.getPmc() ) )
        {
            DoapUtil.writeRdfResourceElement( writer, "asfext:pmc", asfExtOptions.getPmc() );
        }
        else
        {
            if ( StringUtils.isEmpty( project.getUrl() ) )
            {
                getLog().warn( "No project url discovered! According http://projects.apache.org/docs/pmc.html, "
                                   + "asfext:pmc is required" );
            }
            else
            {
                DoapUtil.writeRdfResourceElement( writer, "asfext:pmc", project.getUrl() );
            }
        }

        // asfext:name
        if ( StringUtils.isNotEmpty( asfExtOptions.getName() ) )
        {
            DoapUtil.writeElement( writer, "asfext:name", asfExtOptions.getName() );
        }
        else
        {
            if ( StringUtils.isEmpty( project.getName() ) )
            {
                getLog().warn( "No project name discovered! According http://projects.apache.org/docs/pmc.html, "
                                   + "asfext:name is required" );
            }
            else
            {
                // Respect ASF rule
                if ( !project.getName().trim().startsWith( "Apache" ) )
                {
                    DoapUtil.writeElement( writer, "asfext:name", "Apache " + project.getName().trim() );
                }
                else
                {
                    DoapUtil.writeElement( writer, "asfext:name", project.getName().trim() );
                }
            }
        }

        // asfext:charter
        if ( StringUtils.isEmpty( asfExtOptions.getCharter() ) )
        {
            getLog().warn( "No charter specified! According http://projects.apache.org/docs/pmc.html, "
                               + "asfext:charter is required" );
        }
        else
        {
            DoapUtil.writeRdfResourceElement( writer, "asfext:charter", asfExtOptions.getCharter() );
        }

        // asfext:chair
        @SuppressWarnings( "unchecked" )
        List<Developer> developers = project.getDevelopers();

        if ( StringUtils.isNotEmpty( asfExtOptions.getChair() ) )
        {
            writer.startElement( "asfext:chair" );
            writer.startElement( "foaf:Person" );
            writer.startElement( "foaf:name" );
            writer.writeText( asfExtOptions.getChair() );
            writer.endElement(); // foaf:name
            writer.endElement(); // foaf:Person
            writer.endElement(); // asfext:chair
        }
        else
        {
            Developer chair = ASFExtOptions.findChair( developers );
            if ( chair != null )
            {
                writeContributor( writer, chair, "asfext:chair" );
            }
            else
            {
                getLog().warn( "No chair man discovered! According http://projects.apache.org/docs/pmc.html, "
                                   + "asfext:chair is required" );
            }
        }

        // asfext:member
        if ( developers != null && developers.size() > 0 )
        {
            List<Developer> pmcMembers = ASFExtOptions.findPMCMembers( developers );
            for ( Developer pmcMember : pmcMembers )
            {
                writeContributor( writer, pmcMember, "asfext:member" );
            }
        }

        writeASFImplements( writer );
    }

    /**
     * Write the ASF implements.
     *
     * @param writer not null
     * @see <a href="http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext">
     *      http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext</a>
     * @see <a href="http://projects.apache.org/docs/standards.html">http://projects.apache.org/docs/standards.html</a>
     */
    private void writeASFImplements( XMLWriter writer )
    {
        if ( asfExtOptions.getStandards() == null || asfExtOptions.getStandards().isEmpty() )
        {
            return;
        }

        for ( Standard standard : asfExtOptions.getStandards() )
        {
            writer.startElement( "asfext:implements" );
            writer.startElement( "asfext:Standard" );

            if ( StringUtils.isEmpty( standard.getTitle() ) )
            {
                getLog().warn( "No title specified! According http://projects.apache.org/docs/standards.html, "
                                   + "asfext:title is required" );
            }
            else
            {
                writer.startElement( "asfext:title" );
                writer.writeText( standard.getTitle() );
                writer.endElement(); // asfext:title
            }

            if ( StringUtils.isEmpty( standard.getBody() ) )
            {
                getLog().warn( "No body specified! According http://projects.apache.org/docs/standards.html, "
                                   + "asfext:body is required" );
            }
            else
            {
                writer.startElement( "asfext:body" );
                writer.writeText( standard.getBody() );
                writer.endElement(); // asfext:body
            }

            if ( StringUtils.isEmpty( standard.getId() ) )
            {
                getLog().warn( "No id specified! According http://projects.apache.org/docs/standards.html, "
                                   + "asfext:id is required" );
            }
            else
            {
                writer.startElement( "asfext:id" );
                writer.writeText( standard.getId() );
                writer.endElement(); // asfext:id
            }

            if ( StringUtils.isNotEmpty( standard.getUrl() ) )
            {
                writer.startElement( "asfext:url" );
                writer.writeText( standard.getUrl() );
                writer.endElement(); // asfext:url
            }

            writer.endElement(); // asfext:Standard
            writer.endElement(); // asfext:implements
        }
    }

    /**
     * Write a Foaf Organization, for instance:
     *
     * <pre>
     *   &lt;<foaf:Organization&gt;
     *     &lt;foaf:name&gt;YoyoDyne&lt;/foaf:name&gt;
     *     &lt;foaf:homepage rdf:resource="http://yoyodyne.example.org"/&gt;
     *     &lt;foaf:member rdf:nodeID="benny_profane"&gt;
     *   &lt;/foaf:Organization&gt;
     * </pre>
     *
     * @param writer not null
     * @param developersOrContributors list of <code>{@link Developer}/{@link Contributor}</code>
     * @param doapType not null
     * @see #writeContributor(XMLWriter, Object, String)
     */
    private void writeOrganizations( XMLWriter writer )
    {
        Set<Entry<String, DoapUtil.Organization>> organizations = DoapUtil.getOrganizations();

        for (Entry<String, DoapUtil.Organization> organizationEntry : organizations )
        {
            DoapUtil.Organization organization = organizationEntry.getValue();

            writer.startElement( "foaf:Organization" );
            if ( !StringUtils.isEmpty( organization.getName() ) )
            {
                DoapUtil.writeElement( writer, "foaf:name", organization.getName() );
            }
            if ( !StringUtils.isEmpty( organization.getUrl() ) )
            {
                DoapUtil.writeRdfResourceElement( writer, "foaf:homepage", organization.getUrl() );
            }
            List<String> members = organization.getMembers();
            for ( String member : members )
            {
                DoapUtil.writeRdfNodeIdElement( writer, "foaf:member", member );
            }
            writer.endElement(); // foaf:Organization
        }
    }

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

    /**
     * Compose a URL from two parts: a base URL and a file path. This method makes sure that there will not be two slash
     * '/' characters after each other.
     *
     * @param base The base URL
     * @param path The file
     * @return the url with base and path
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
