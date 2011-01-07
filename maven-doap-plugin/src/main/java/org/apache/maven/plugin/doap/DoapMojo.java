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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.doap.options.ASFExtOptions;
import org.apache.maven.plugin.doap.options.ASFExtOptionsUtil;
import org.apache.maven.plugin.doap.options.DoapArtifact;
import org.apache.maven.plugin.doap.options.DoapOptions;
import org.apache.maven.plugin.doap.options.ExtOptions;
import org.apache.maven.plugin.doap.options.Standard;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.cvslib.repository.CvsScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

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
     * @parameter expression="${doapFile}" default-value="doap_${project.artifactId}.rdf"
     * @required
     */
    private String doapFile;

    /**
     * The output directory of the DOAP file that will be generated.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @required
     * @since 1.1
     */
    private String outputDirectory;

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

    /**
     * Factory for creating artifact objects
     *
     * @component
     * @since 1.1
     */
    private ArtifactFactory factory;

    /**
     * Project builder
     *
     * @component
     * @since 1.1
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Used for resolving artifacts
     *
     * @component
     * @since 1.1
     */
    private ArtifactResolver resolver;

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     * @since 1.1
     */
    protected Settings settings;

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
     * @see ASFExtOptionsUtil#isASFProject(MavenProject)
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

    /**
     * An artifact to generate the DOAP file against. <br/>
     * Example:
     *
     * <pre>
     * &lt;artifact&gt;
     * &nbsp;&nbsp;&lt;groupId&gt;given-artifact-groupId&lt;/groupId&gt;
     * &nbsp;&nbsp;&lt;artifactId&gt;given-artifact-artifactId&lt;/artifactId&gt;
     * &nbsp;&nbsp;&lt;version&gt;given-artifact-version&lt;/version&gt;
     * &lt;/artifact&gt;
     * </pre>
     *
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/doap/options/DaopArtifact.html">Javadoc</a> <br/>
     *
     * @parameter
     * @since 1.1
     */
    private DoapArtifact artifact;

    /**
     * Specifies whether the DOAP generation should be skipped.
     *
     * @parameter expression="${maven.doap.skip}" default-value="false"
     * @since 1.1
     */
    private boolean skip;

    /**
     * Extensions parameters. <br/>
     * Example:
     *
     * <pre>
     * &lt;extOptions&gt;
     * &nbsp;&lt;extOption&gt;
     * &nbsp;&nbsp;&nbsp;&lt;xmlnsPrefix&gt;labs&lt;/xmlnsPrefix&gt;
     * &nbsp;&nbsp;&nbsp;&lt;xmlnsNamespaceURI&gt;http://labs.apache.org/doap-ext/1.0#&lt;/xmlnsNamespaceURI&gt;
     * &nbsp;&nbsp;&nbsp;&lt;extensions&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;status&gt;active&lt;/status&gt;
     * &nbsp;&nbsp;&nbsp;&lt;/extensions&gt;
     * &nbsp;&lt;/extOption&gt;
     * &lt;/extOptions&gt;
     * </pre>
     *
     * See <a href="./apidocs/org/apache/maven/plugin/doap/options/ExtOptions.html">Javadoc</a> <br/>
     *
     * @parameter expression="${extOptions}"
     * @since 1.1
     */
    private ExtOptions[] extOptions;

    /**
     * All POM/DOAP error messages.
     *
     * @since 1.1
     */
    private List<String> errorMessages = new ArrayList<String>();

    /**
     * All POM/DOAP warn messages.
     *
     * @since 1.1
     */
    private List<String> warnMessages = new ArrayList<String>();

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Skipping DOAP generation" );
            return;
        }

        // single artifact
        if ( artifact != null )
        {
            MavenProject givenProject = getMavenProject( artifact );
            if ( givenProject != null )
            {
                File outDir = new File( outputDirectory );
                if ( !outDir.isAbsolute() )
                {
                    outDir = new File( project.getBasedir(), outputDirectory );
                }
                File outFile = new File( outDir, artifact.getDoapFileName() );
                writeDoapFile( givenProject, outFile );
                return;
            }
        }

        // current project
        File outFile = new File( doapFile );
        if ( !doapFile.replaceAll( "\\\\", "/" ).contains( "/" ) )
        {
            File outDir = new File( outputDirectory );
            if ( !outDir.isAbsolute() )
            {
                outDir = new File( project.getBasedir(), outputDirectory );
            }
            outFile = new File( outDir, doapFile );
        }
        writeDoapFile( project, outFile );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param artifact not null
     * @return the maven project for the given doap artifact
     * @since 1.1
     */
    private MavenProject getMavenProject( DoapArtifact artifact )
    {
        if ( artifact == null )
        {
            return null;
        }

        if ( StringUtils.isEmpty( artifact.getGroupId() ) || StringUtils.isEmpty( artifact.getArtifactId() )
            || StringUtils.isEmpty( artifact.getVersion() ) )
        {
            getLog().warn( "Missing groupId or artifactId or version in <artifact/> parameter, ignored it." );
            return null;
        }

        getLog().info( "Using artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                           + artifact.getVersion() );

        try
        {
            Artifact art =
                factory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                               Artifact.SCOPE_COMPILE );

            if ( art.getFile() == null )
            {
                MavenProject proj = mavenProjectBuilder.buildFromRepository( art, remoteRepositories, localRepository );
                art = proj.getArtifact();

                resolver.resolve( art, remoteRepositories, localRepository );

                return proj;
            }
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().error( "ArtifactResolutionException: " + e.getMessage() + "\nIgnored <artifact/> parameter." );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().error( "ArtifactNotFoundException: " + e.getMessage() + "\nIgnored <artifact/> parameter." );
        }
        catch ( ProjectBuildingException e )
        {
            getLog().error( "ProjectBuildingException: " + e.getMessage() + "\nIgnored <artifact/> parameter." );
        }

        return null;
    }

    /**
     * Write a doap file for the given project.
     *
     * @param project not null
     * @param outputFile not null
     * @since 1.1
     */
    private void writeDoapFile( MavenProject project, File outputFile )
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // Includes ASF extensions
        // ----------------------------------------------------------------------------

        if ( !asfExtOptions.isIncluded() && ASFExtOptionsUtil.isASFProject( project ) )
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
            if ( !outputFile.getParentFile().exists() )
            {
                FileUtils.mkdir( outputFile.getParentFile().getAbsolutePath() );
            }

            w = WriterFactory.newXmlWriter( outputFile );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating DOAP file " + outputFile.getAbsolutePath(), e );
        }

        if ( asfExtOptions.isIncluded() )
        {
            getLog().info( "Generating an ASF DOAP file " + outputFile.getAbsolutePath() );
        }
        else
        {
            getLog().info( "Generating a pure DOAP file " + outputFile.getAbsolutePath() );
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w, project.getModel().getModelEncoding(), null );

        // ----------------------------------------------------------------------------
        // Convert POM to DOAP
        // ----------------------------------------------------------------------------

        DoapUtil.writeHeader( writer );

        // Heading
        DoapUtil.writeStartElement( writer, "rdf", "RDF" );
        if ( Arrays.binarySearch( Locale.getISOLanguages(), lang ) < 0 )
        {
            throw new MojoExecutionException( "The <doapOptions><lang>" + lang
                + "</lang></doapOptions> parameter is not a valid ISO language." );
        }
        writer.addAttribute( "xml:lang", lang );
        if ( StringUtils.isEmpty( doapOptions.getXmlnsNamespaceURI() ) )
        {
            throw new MojoExecutionException( "The <doapOptions><xmlnsNamespaceURI/></doapOptions>"
                + " parameter is required." );
        }
        writer.addAttribute( "xmlns"
                                 + ( StringUtils.isEmpty( doapOptions.getXmlnsPrefix() ) ? "" : ":"
                                     + doapOptions.getXmlnsPrefix() ), doapOptions.getXmlnsNamespaceURI() );
        writer.addAttribute( "xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
        writer.addAttribute( "xmlns:foaf", "http://xmlns.com/foaf/0.1/" );
        if ( asfExtOptions.isIncluded() )
        {
            if ( StringUtils.isEmpty( asfExtOptions.getXmlnsPrefix() ) )
            {
                throw new MojoExecutionException( "The <asfExtOptions><xmlnsPrefix/></asfExtOptions>"
                    + " parameter is required." );
            }
            if ( StringUtils.isEmpty( asfExtOptions.getXmlnsNamespaceURI() ) )
            {
                throw new MojoExecutionException( "The <asfExtOptions><xmlnsNamespaceURI/></asfExtOptions>"
                    + " parameter is required." );
            }
            writer.addAttribute( "xmlns"
                                     + ( StringUtils.isEmpty( asfExtOptions.getXmlnsPrefix() ) ? "" : ":"
                                         + asfExtOptions.getXmlnsPrefix() ), asfExtOptions.getXmlnsNamespaceURI() );
        }
        if ( extOptions != null && extOptions.length > 0 && !extOptions[0].getExtensions().isEmpty() )
        {
            for ( ExtOptions extOption : extOptions )
            {
                if ( StringUtils.isEmpty( extOption.getXmlnsPrefix() ) )
                {
                    throw new MojoExecutionException( "The <extOptions><extOption><xmlnsPrefix/></extOption>"
                        + "</extOptions> parameter is required." );
                }
                if ( StringUtils.isEmpty( extOption.getXmlnsNamespaceURI() ) )
                {
                    throw new MojoExecutionException( "The <extOptions><extOption><xmlnsNamespaceURI/></extOption>"
                        + "</extOptions> parameter is required." );
                }
                writer.addAttribute( "xmlns"
                                         + ( StringUtils.isEmpty( extOption.getXmlnsPrefix() ) ? "" : ":"
                                             + extOption.getXmlnsPrefix() ),
                                     extOption.getXmlnsNamespaceURI() );
            }
        }

        // Project
        DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "Project" );
        boolean added = false;
        if ( artifact != null )
        {
            String about_ = project.getUrl();

            if ( StringUtils.isNotEmpty( about_ ) )
            {
                try
                {
                    new URL( about_ );

                    writer.addAttribute( "rdf:about", about_ );
                    added = true;
                }
                catch ( MalformedURLException e )
                {
                }
            }

            if ( !added )
            {
                warnMessages.add( "The project's url from " + artifact.toStringConfiguration()
                    + " is empty or not valid, using <about/> parameter." );
            }
        }

        if ( !added )
        {
            if ( StringUtils.isNotEmpty( about ) )
            {
                try
                {
                    new URL( about );

                    writer.addAttribute( "rdf:about", about );
                }
                catch ( MalformedURLException e )
                {
                    errorMessages.add( "The <about/> parameter is not a valid URL." );
                }
            }
            else
            {
                errorMessages.add( "The <about/> parameter is required." );
            }
        }

        // name
        writeName( writer, project );

        // description
        writeDescription( writer, project );

        // implements
        writeImplements( writer );

        // Audience
        writeAudience( writer );

        // Vendor
        writeVendor( writer, project );

        // created
        writeCreated( writer, project );

        // homepage and old-homepage
        writeHomepage( writer, project );

        // Blog
        writeBlog( writer );

        // licenses
        writeLicenses( writer, project );

        // programming-language
        writeProgrammingLanguage( writer, project );

        // category
        writeCategory( writer, project );

        // os
        writeOS( writer, project );

        // Plateform
        writePlateform( writer );

        // Language
        writeLanguage( writer );

        // SCM
        writeSourceRepositories( writer, project );

        // bug-database
        writeBugDatabase( writer, project );

        // mailing list
        writeMailingList( writer, project );

        // download-page and download-mirror
        writeDownloadPage( writer, project );

        // screenshots
        writeScreenshots( writer, project );

        // service-endpoint
        writeServiceEndpoint( writer );

        // wiki
        writeWiki( writer, project );

        // Releases
        writeReleases( writer, project );

        // Developers
        @SuppressWarnings( "unchecked" )
        List<Contributor> developers = project.getDevelopers();
        if ( developers == null || developers.isEmpty() )
        {
            addPomMessage( "developers" );
        }
        else
        {
            writeContributors( writer, developers );
        }

        // Contributors
        @SuppressWarnings( "unchecked" )
        List<Contributor> contributors = project.getContributors();
        writeContributors( writer, contributors );

        // Extra DOAP
        @SuppressWarnings( "unchecked" )
        Map<String, String> map = doapOptions.getExtra();
        writeExtra( writer, project, "Extra DOAP vocabulary.", map, doapOptions.getXmlnsPrefix() );

        // ASFext
        writeASFext( writer, project );

        // Extra extensions
        writeExtensions( writer );

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

        if ( !warnMessages.isEmpty() )
        {
            for ( String warn : warnMessages )
            {
                getLog().warn( warn );
            }
        }

        if ( !errorMessages.isEmpty() )
        {
            getLog().error( "" );
            for ( String error : errorMessages )
            {
                getLog().error( error );
            }
            getLog().error( "" );

            if ( ASFExtOptionsUtil.isASFProject( project ) )
            {
                getLog().error( "For more information about the errors and possible solutions, please read the plugin documentation:" );
                getLog().error( "http://maven.apache.org/plugins/maven-doap-plugin/usage.html#DOAP_ASF_Configuration" );
                throw new MojoExecutionException( "The generated DOAP doesn't respect ASF rules, see above." );
            }
        }

        if ( validate )
        {
            List<String> errors = DoapUtil.validate( outputFile );
            if ( !errors.isEmpty() )
            {
                getLog().error( "" );
                for ( String error : errors )
                {
                    getLog().error( error );
                }
                getLog().error( "" );

                throw new MojoExecutionException( "Error parsing the generated DOAP file, see above." );
            }
        }
    }

    /**
     * Write DOAP name.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#name">http://usefulinc.com/ns/doap#name</a>
     */
    private void writeName( XMLWriter writer, MavenProject project )
    {
        String name = interpolate( doapOptions.getName(), project, settings );
        if ( StringUtils.isEmpty( name ) )
        {
            addDoapMessage( "name" );
            return;
        }

        DoapUtil.writeComment( writer, "A name of something." );

        if ( asfExtOptions.isIncluded() && !name.toLowerCase( Locale.ENGLISH ).startsWith( "apache" ) )
        {
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "name", "Apache " + name );
        }
        else
        {
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "name", name );
        }
    }

    /**
     * Write DOAP description.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#description">http://usefulinc.com/ns/doap#description</a>
     * @see <a href="http://usefulinc.com/ns/doap#shortdesc">http://usefulinc.com/ns/doap#shortdesc</a>
     */
    private void writeDescription( XMLWriter writer, MavenProject project )
    {
        boolean addComment = false;
        String description = interpolate( doapOptions.getDescription(), project, settings );
        if ( StringUtils.isEmpty( description ) )
        {
            addDoapMessage( "description" );
        }
        else
        {
            DoapUtil.writeComment( writer, "Plain text description of a project, of 2-4 sentences in length." );
            addComment = true;
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "description", description, lang );
        }

        String comment = "Short plain text description of a project.";
        String shortdesc = interpolate( doapOptions.getShortdesc(), project, settings );
        if ( StringUtils.isEmpty( shortdesc ) )
        {
            addDoapMessage( "shortdesc" );
            return;
        }
        if ( description.equals( shortdesc ) )
        {
            // try to get the first 10 words of the description
            String sentence = StringUtils.split( shortdesc, "." )[0];
            if ( StringUtils.split( sentence, " " ).length > 10 )
            {
                addDoapMessage( "shortdesc" );
                return;
            }
            if ( !addComment )
            {
                DoapUtil.writeComment( writer, comment );
            }
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "shortdesc", sentence, lang );
            return;
        }
        if ( !addComment )
        {
            DoapUtil.writeComment( writer, comment );
        }
        DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "shortdesc", shortdesc, lang );
    }

    /**
     * Write DOAP created.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#created">http://usefulinc.com/ns/doap#created</a>
     */
    private void writeCreated( XMLWriter writer, MavenProject project )
    {
        String created = interpolate( doapOptions.getCreated(), project, settings );
        if ( StringUtils.isEmpty( created ) )
        {
            addDoapMessage( "created" );
            return;
        }

        try
        {
            DOAP_DATE_FORMAT.parse( created );
        }
        catch ( ParseException e )
        {
            errorMessages.add( "The <doapOptions><created>" + created
                + "</created></doapOptions> parameter should be in YYYY-MM-DD." );
            return;
        }

        DoapUtil.writeComment( writer, "Date when something was created, in YYYY-MM-DD form. e.g. 2004-04-05" );
        DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "created", created );
    }

    /**
     * Write DOAP homepage and old-homepage.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#homepage">http://usefulinc.com/ns/doap#homepage</a>
     * @see <a href="http://usefulinc.com/ns/doap#old-homepage">http://usefulinc.com/ns/doap#old-homepage</a>
     */
    private void writeHomepage( XMLWriter writer, MavenProject project )
    {
        String homepage = interpolate( doapOptions.getHomepage(), project, settings );
        if ( StringUtils.isEmpty( homepage ) )
        {
            addDoapMessage( "homepage" );
        }
        else
        {
            try
            {
                new URL( homepage );

                DoapUtil.writeComment( writer, "URL of a project's homepage, associated with exactly one project." );
                DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "homepage", homepage );
            }
            catch ( MalformedURLException e )
            {
                errorMessages.add( "The <doapOptions><homepage>" + homepage
                    + "</homepage></doapOptions> parameter is not a valid URL." );
            }
        }

        if ( StringUtils.isNotEmpty( doapOptions.getOldHomepage() ) )
        {
            String oldHomepage = interpolate( doapOptions.getOldHomepage(), project, settings );
            if ( StringUtils.isEmpty( oldHomepage ) )
            {
                return;
            }

            try
            {
                new URL( oldHomepage );

                DoapUtil.writeComment( writer, "URL of a project's past homepage, associated with exactly one project." );
                DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "old-homepage", oldHomepage );
            }
            catch ( MalformedURLException e )
            {
                errorMessages.add( "The <doapOptions><oldHomepage>" + oldHomepage
                    + "</oldHomepage></doapOptions> parameter is not a valid URL." );
            }
        }
    }

    /**
     * Write DOAP programming-language.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#programming-language">
     *      http://usefulinc.com/ns/doap#programming-language</a>
     */
    private void writeProgrammingLanguage( XMLWriter writer, MavenProject project )
    {
        if ( StringUtils.isEmpty( doapOptions.getProgrammingLanguage() ) && StringUtils.isEmpty( language ) )
        {
            addDoapMessage( "programmingLanguage" );
            return;
        }

        boolean addComment = false;
        String comment = "Programming language.";
        if ( StringUtils.isNotEmpty( language ) ) // backward compatible
        {
            getLog().warn( "The <language/> parameter is deprecated, please use <doapOptions><programmingLanguage/></doapOptions> parameter instead of." );

            language = language.trim();

            if ( asfExtOptions.isIncluded() )
            {
                String asfLanguage = ASFExtOptionsUtil.getProgrammingLanguageSupportedByASF( language );
                if ( asfLanguage == null )
                {
                    errorMessages.add( "The deprecated <language>" + language
                        + "</language> parameter is not supported by ASF. Should be one of "
                        + Arrays.toString( ASFExtOptionsUtil.PROGRAMMING_LANGUAGES ) );
                }
                else
                {
                    DoapUtil.writeComment( writer, comment );
                    addComment = true;
                    DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "programming-language",
                                           asfLanguage.trim() );
                }
            }
            else
            {
                DoapUtil.writeComment( writer, comment );
                addComment = true;
                DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "programming-language", language.trim() );
            }
        }

        if ( StringUtils.isNotEmpty( doapOptions.getProgrammingLanguage() ) )
        {
            String[] languages = StringUtils.split( doapOptions.getProgrammingLanguage(), "," );
            for ( String language : languages )
            {
                language = language.trim();

                if ( asfExtOptions.isIncluded() )
                {
                    String asfLanguage = ASFExtOptionsUtil.getProgrammingLanguageSupportedByASF( language );
                    if ( asfLanguage == null )
                    {
                        errorMessages.add( "The <doapOptions><programmingLanguage>" + language
                            + "</programmingLanguage></doapOptions> parameter is not supported by ASF. "
                            + "Should be one of " + Arrays.toString( ASFExtOptionsUtil.PROGRAMMING_LANGUAGES ) );
                    }
                    else
                    {
                        if ( !addComment )
                        {
                            DoapUtil.writeComment( writer, comment );
                            addComment = true;
                        }
                        DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "programming-language",
                                               asfLanguage );
                    }
                }
                else
                {
                    if ( !addComment )
                    {
                        DoapUtil.writeComment( writer, comment );
                        addComment = true;
                    }
                    DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "programming-language", language );
                }
            }
        }
    }

    /**
     * Write DOAP category.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#category">http://usefulinc.com/ns/doap#category</a>
     */
    private void writeCategory( XMLWriter writer, MavenProject project )
    {
        if ( StringUtils.isEmpty( doapOptions.getCategory() ) && StringUtils.isEmpty( category ) )
        {
            addDoapMessage( "category" );
            return;
        }

        // TODO: how to lookup category, map it, or just declare it.
        boolean addComment = false;
        String comment = "A category of project.";
        if ( StringUtils.isNotEmpty( category ) ) // backward compatible
        {
            getLog().warn( "The <category/> parameter is deprecated, please use <doapOptions><category/></doapOptions> parameter instead of." );

            category = category.trim();

            if ( asfExtOptions.isIncluded() )
            {
                String asfCategory = ASFExtOptionsUtil.getCategorySupportedByASF( category );
                if ( asfCategory == null )
                {
                    errorMessages.add( "The deprecated <category>" + category
                        + "</category> parameter is not supported by ASF. Should be one of "
                        + Arrays.toString( ASFExtOptionsUtil.CATEGORIES ) );
                }
                else
                {
                    DoapUtil.writeComment( writer, comment );
                    addComment = true;
                    DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "category",
                                                      ASFExtOptionsUtil.CATEGORY_RESOURCE + asfCategory );
                }
            }
            else
            {
                DoapUtil.writeComment( writer, comment );
                addComment = true;
                DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "category", category );
            }
        }

        if ( StringUtils.isNotEmpty( doapOptions.getCategory() ) )
        {
            String[] categories = StringUtils.split( doapOptions.getCategory(), "," );
            for ( String category : categories )
            {
                category = category.trim();

                if ( asfExtOptions.isIncluded() )
                {
                    String asfCategory = ASFExtOptionsUtil.getCategorySupportedByASF( category );
                    if ( asfCategory == null )
                    {
                        errorMessages.add( "The <doapOptions><category>" + category
                            + "</category></doapOptions> parameter is not supported by ASF. Should be one of "
                            + Arrays.toString( ASFExtOptionsUtil.CATEGORIES ) );
                    }
                    else
                    {
                        if ( !addComment )
                        {
                            DoapUtil.writeComment( writer, comment );
                            addComment = true;
                        }
                        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "category",
                                                          ASFExtOptionsUtil.CATEGORY_RESOURCE + asfCategory );
                    }
                }
                else
                {
                    if ( !addComment )
                    {
                        DoapUtil.writeComment( writer, comment );
                        addComment = true;
                    }
                    DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "category", category );
                }
            }
        }
    }

    /**
     * Write DOAP download-page and download-mirror.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#download-page">http://usefulinc.com/ns/doap#download-page</a>
     * @see <a href="http://usefulinc.com/ns/doap#download-mirror">http://usefulinc.com/ns/doap#download-mirror</a>
     */
    private void writeDownloadPage( XMLWriter writer, MavenProject project )
    {
        String downloadPage = interpolate( doapOptions.getDownloadPage(), project, settings );
        if ( StringUtils.isEmpty( downloadPage ) )
        {
            addDoapMessage( "downloadPage" );
            return;
        }

        try
        {
            new URL( downloadPage );

            DoapUtil.writeComment( writer, "Download page." );
            DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "download-page", downloadPage );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The <doapOptions><downloadPage>" + downloadPage
                + "</downloadPage></doapOptions> parameter is not a valid URL." );
        }

        if ( StringUtils.isNotEmpty( doapOptions.getDownloadMirror() ) )
        {
            boolean addComment = false;
            String[] downloadMirrors = StringUtils.split( doapOptions.getDownloadMirror(), "," );
            for ( String downloadMirror : downloadMirrors )
            {
                downloadMirror = downloadMirror.trim();

                try
                {
                    new URL( downloadMirror );

                    if ( !addComment )
                    {
                        DoapUtil.writeComment( writer, "Mirror of software download web page." );
                        addComment = true;
                    }
                    DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "download-mirror",
                                                      downloadMirror );
                }
                catch ( MalformedURLException e )
                {
                    errorMessages.add( "The <doapOptions><downloadMirror>" + downloadMirror
                        + "</downloadMirror></doapOptions> parameter is not a valid URL." );
                }
            }
        }
    }

    /**
     * Write DOAP OS.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#os">http://usefulinc.com/ns/doap#os</a>
     */
    private void writeOS( XMLWriter writer, MavenProject project )
    {
        if ( StringUtils.isEmpty( doapOptions.getOs() ) )
        {
            return;
        }

        DoapUtil.writeComment( writer, "Operating system that a project is limited to." );
        String[] oses = StringUtils.split( doapOptions.getOs(), "," );
        for ( String os : oses )
        {
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "os", os.trim() );
        }
    }

    /**
     * Write DOAP screenshots.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#screenshots">http://usefulinc.com/ns/doap#screenshots</a>
     */
    private void writeScreenshots( XMLWriter writer, MavenProject project )
    {
        if ( StringUtils.isEmpty( doapOptions.getScreenshots() ) )
        {
            return;
        }

        String screenshots = doapOptions.getScreenshots().trim();
        try
        {
            new URL( screenshots );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The <doapOptions><screenshots>" + screenshots
                + "</screenshots></doapOptions> parameter is not a valid URL." );
            return;
        }

        DoapUtil.writeComment( writer, "Web page with screenshots of project." );
        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "screenshots", screenshots );
    }

    /**
     * Write DOAP wiki.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#wiki">http://usefulinc.com/ns/doap#wiki</a>
     */
    private void writeWiki( XMLWriter writer, MavenProject project )
    {
        if ( StringUtils.isEmpty( doapOptions.getWiki() ) )
        {
            return;
        }

        String wiki = doapOptions.getWiki().trim();
        try
        {
            new URL( wiki );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The <doapOptions><wiki>" + wiki + "</wiki></doapOptions> parameter is not a valid URL." );
            return;
        }

        DoapUtil.writeComment( writer, "URL of Wiki for collaborative discussion of project." );
        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "wiki", wiki );
    }

    /**
     * Write DOAP licenses.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#license">http://usefulinc.com/ns/doap#license</a>
     */
    private void writeLicenses( XMLWriter writer, MavenProject project )
    {
        if ( project.getLicenses() == null || project.getLicenses().isEmpty() )
        {
            addPomMessage( "licenses" );
            return;
        }

        DoapUtil.writeComment( writer, "The URI of the license the software is distributed under." );
        // TODO: how to map to usefulinc site, or if this is necessary, the OSI page might
        // be more appropriate.
        @SuppressWarnings( "unchecked" )
        List<License> licenses = project.getLicenses();
        for ( License license : licenses )
        {
            if ( StringUtils.isEmpty( license.getUrl() ) )
            {
                continue;
            }

            String licenseUrl = license.getUrl().trim();
            try
            {
                new URL( licenseUrl );

                DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "license", licenseUrl );
            }
            catch ( MalformedURLException e )
            {
                errorMessages.add( "The POM <project><licenses><license><url>" + licenseUrl
                    + "</url></license></licenses></project> value is not a valid URL." );
            }
        }
    }

    /**
     * Write DOAP bug-database.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#bug-database">http://usefulinc.com/ns/doap#bug-database</a>
     */
    private void writeBugDatabase( XMLWriter writer, MavenProject project )
    {
        if ( project.getIssueManagement() == null || StringUtils.isEmpty( project.getIssueManagement().getUrl() ) )
        {
            addPomMessage( "issueManagement" );
            return;
        }

        String issueManagementUrl = project.getIssueManagement().getUrl().trim();
        try
        {
            new URL( issueManagementUrl );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The POM <project><issueManagement><url>" + issueManagementUrl
                + "</url></issueManagement></project> value is not a valid URL." );
            return;
        }

        DoapUtil.writeComment( writer, "Bug database." );
        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "bug-database", issueManagementUrl );
    }

    /**
     * Write DOAP mailing-list.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#mailing-list">http://usefulinc.com/ns/doap#mailing-list</a>
     * @see DoapOptions#getMailingList()
     */
    private void writeMailingList( XMLWriter writer, MavenProject project )
    {
        String ml = interpolate( doapOptions.getMailingList(), project, settings );
        if ( StringUtils.isEmpty( ml ) )
        {
            addDoapMessage( "mailingList" );
            return;
        }

        try
        {
            new URL( ml );

            DoapUtil.writeComment( writer, "Mailing lists." );
            DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "mailing-list", ml );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The <doapOptions><mailingList>" + ml
                + "</mailingList></doapOptions> value is not a valid URL." );
        }
    }

    /**
     * Write all DOAP releases.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @throws MojoExecutionException if any
     * @see <a href="http://usefulinc.com/ns/doap#release">http://usefulinc.com/ns/doap#release</a>
     * @see <a href="http://usefulinc.com/ns/doap#Version">http://usefulinc.com/ns/doap#Version</a>
     */
    private void writeReleases( XMLWriter writer, MavenProject project )
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
                    throw new MojoExecutionException( metadata.extendedToString()
                        + " could not be retrieved from repositories due to an error: " + e.getMessage(), e );
                }
            }
        }

        if ( metadata.getMetadata().getVersioning() == null )
        {
            warnMessages.add( "No versioning was found. Ignored DOAP <release/> tag." );
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
                DoapUtil.writeComment( writer, "Project releases." );
                addComment = true;
            }

            DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "release" );
            DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "Version" );

            DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "name" );
            if ( version.equals( metadata.getMetadata().getVersioning().getRelease() ) )
            {
                writer.writeText( "Latest stable release" );
            }
            else
            {
                writer.writeText( project.getName() + " - " + version );
            }
            writer.endElement(); // name

            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "revision", version );

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
                try
                {
                    DoapUtil.fetchURL( settings, new URL( fileRelease ) );
                }
                catch ( IOException e )
                {
                    getLog().debug( "IOException :" + e.getMessage() );
                    continue;
                }
                DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "file-release", fileRelease );

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
                    DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "created",
                                           DOAP_DATE_FORMAT.format( releaseDate ) );
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
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#Repository">http://usefulinc.com/ns/doap#Repository</a>
     * @see <a href="http://usefulinc.com/ns/doap#CVSRepository">http://usefulinc.com/ns/doap#CVSRepository</a>
     * @see <a href="http://usefulinc.com/ns/doap#SVNRepository">http://usefulinc.com/ns/doap#SVNRepository</a>
     */
    private void writeSourceRepositories( XMLWriter writer, MavenProject project )
    {
        Scm scm = project.getScm();
        if ( scm == null )
        {
            addPomMessage( "scm" );
            return;
        }

        String anonymousConnection = scm.getConnection();
        if ( StringUtils.isNotEmpty( anonymousConnection ) )
        {
            DoapUtil.writeComment( writer, "Anonymous Source Repository" );
            writeSourceRepository( writer, project, anonymousConnection );
        }

        String developerConnection = scm.getDeveloperConnection();
        if ( StringUtils.isNotEmpty( developerConnection ) )
        {
            DoapUtil.writeComment( writer, "Developer Source Repository" );
            writeSourceRepository( writer, project, developerConnection );
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
     * @param project the Maven project, not null
     * @param connection not null
     * @see <a href="http://usefulinc.com/ns/doap#Repository">http://usefulinc.com/ns/doap#Repository</a>
     * @see <a href="http://usefulinc.com/ns/doap#CVSRepository">http://usefulinc.com/ns/doap#CVSRepository</a>
     * @see <a href="http://usefulinc.com/ns/doap#SVNRepository">http://usefulinc.com/ns/doap#SVNRepository</a>
     */
    private void writeSourceRepository( XMLWriter writer, MavenProject project, String connection )
    {
        ScmRepository repository = getScmRepository( connection );

        DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "repository" );

        if ( isScmSystem( repository, "cvs" ) )
        {
            DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "CVSRepository" );

            CvsScmProviderRepository cvsRepo = (CvsScmProviderRepository) repository.getProviderRepository();

            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "anon-root", cvsRepo.getCvsRoot() );
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "module", cvsRepo.getModule() );
        }
        else if ( isScmSystem( repository, "svn" ) )
        {
            DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "SVNRepository" );

            SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

            DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "location", svnRepo.getUrl() );
        }
        else
        {
            /*
             * Supported DOAP repositories actually unsupported by SCM: BitKeeper
             * (http://usefulinc.com/ns/doap#BKRepository) Arch (http://usefulinc.com/ns/doap#ArchRepository) Other SCM
             * repos are unsupported by DOAP.
             */
            DoapUtil.writeStartElement( writer, doapOptions.getXmlnsPrefix(), "Repository" );

            if ( connection.length() < 4 )
            {
                throw new IllegalArgumentException( "The source repository connection is too short." );
            }

            DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "location",
                                              connection.substring( 4 ) );
        }

        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "browse", project.getScm().getUrl() );

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
            // no msg there, only dev are req
            return;
        }

        boolean isDeveloper = Developer.class.isAssignableFrom( contributors.get( 0 ).getClass() );
        if ( isDeveloper )
        {
            DoapUtil.writeComment( writer, "Main committers" );
        }
        else
        {
            DoapUtil.writeComment( writer, "Contributed persons" );
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

        sortContributors( developersOrContributors );

        for ( Contributor developersOrContributor : developersOrContributors )
        {
            writeContributor( writer, developersOrContributor, doapOptions.getXmlnsPrefix(), doapType );
        }
    }

    /**
     * Writer a single developer or contributor
     *
     * @param writer not null
     * @param xmlsPrefix could be null
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
    private void writeContributor( XMLWriter writer, Contributor developerOrContributor, String xmlsPrefix,
                                   String doapType )
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
            errorMessages.add( "The POM <project><developers|contributors><developer|contributor><name/>"
                + "</developer|contributor></developers|contributors></project> value is missing." );
            return;
        }

        if ( !StringUtils.isEmpty( organization ) || !StringUtils.isEmpty( organizationUrl ) )
        {
            DoapUtil.Organization doapOrganization = DoapUtil.addOrganization( organization, organizationUrl );
            nodeId = DoapUtil.getNodeId();
            doapOrganization.addMember( nodeId );
        }

        DoapUtil.writeStartElement( writer, xmlsPrefix, doapType );
        DoapUtil.writeStartElement( writer, "foaf", "Person" );
        if ( StringUtils.isNotEmpty( nodeId ) )
        {
            writer.addAttribute( "rdf:nodeID", nodeId );
        }
        DoapUtil.writeStartElement( writer, "foaf", "name" );
        writer.writeText( name );
        writer.endElement(); // foaf:name
        if ( StringUtils.isNotEmpty( email ) )
        {
            if ( DoapUtil.isValidEmail( email ) )
            {
                DoapUtil.writeRdfResourceElement( writer, "foaf", "mbox", "mailto:" + email );
            }
            else
            {
                warnMessages.add( "The POM <project><developers|contributors><developer|contributor><email>"
                    + email
                    + "</email></developer|contributor></developers|contributors></project> value is not a valid email." );
            }
        }
        if ( StringUtils.isNotEmpty( organization ) && StringUtils.isNotEmpty( organizationUrl ) )
        {
            try
            {
                new URL( organizationUrl );

                DoapUtil.addOrganization( organization, organizationUrl );
            }
            catch ( MalformedURLException e )
            {
                errorMessages.add( "The POM <project><developers|contributors><developer|contributor><organizationUrl>"
                    + organizationUrl
                    + "</organizationUrl></developer|contributor></developers|contributors></project> value is not a valid URL." );
            }
        }
        if ( StringUtils.isNotEmpty( homepage ) )
        {
            try
            {
                new URL( homepage );

                DoapUtil.writeRdfResourceElement( writer, "foaf", "homepage", homepage );
            }
            catch ( MalformedURLException e )
            {
                errorMessages.add( "The POM <project><developers|contributors><developer|contributor><url>" + homepage
                    + "</url></developer|contributor></developers|contributors></project> value is not a valid URL." );
            }
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
     * @param project the Maven project, not null
     * @see <a href="http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext">
     *      http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/asfext</a>
     * @see <a href="http://projects.apache.org/docs/pmc.html">http://projects.apache.org/docs/pmc.html</a>
     */
    private void writeASFext( XMLWriter writer, MavenProject project )
    {
        if ( !asfExtOptions.isIncluded() )
        {
            return;
        }

        DoapUtil.writeComment( writer, "ASF extension" );

        // asfext:pmc
        String pmc = interpolate( asfExtOptions.getPmc(), project, settings );
        if ( StringUtils.isNotEmpty( pmc ) )
        {
            DoapUtil.writeRdfResourceElement( writer, asfExtOptions.getXmlnsPrefix(), "pmc", pmc );
        }
        else
        {
            errorMessages.add( "A <asfExtOptions><pmc/></asfExtOptions> parameter is required by ASF." );
        }

        // asfext:name
        String name = interpolate( asfExtOptions.getName(), project, settings );
        if ( StringUtils.isNotEmpty( name ) )
        {
            if ( !name.toLowerCase( Locale.ENGLISH ).trim().startsWith( "apache" ) )
            {
                name = "Apache " + name;
            }
            DoapUtil.writeElement( writer, asfExtOptions.getXmlnsPrefix(), "name", name );
        }
        else
        {
            errorMessages.add( "A <asfExtOptions><name/></asfExtOptions> parameter is required by ASF." );
        }

        String homepage = interpolate( doapOptions.getHomepage(), project, settings );
        if ( StringUtils.isNotEmpty( homepage ) )
        {
            try
            {
                new URL( homepage );

                DoapUtil.writeRdfResourceElement( writer, "foaf", "homepage", homepage );
            }
            catch ( MalformedURLException e )
            {
                errorMessages.add( "The <doapOptions><homepage>" + homepage
                    + "</homepage></doapOptions> parameter is not a valid URL." );
            }
        }

        // asfext:charter
        if ( StringUtils.isEmpty( asfExtOptions.getCharter() ) )
        {
            errorMessages.add( "A <asfExtOptions><charter/></asfExtOptions> parameter is required by ASF." );
        }
        else
        {
            DoapUtil.writeElement( writer, asfExtOptions.getXmlnsPrefix(), "charter", asfExtOptions.getCharter() );
        }

        // asfext:chair
        @SuppressWarnings( "unchecked" )
        List<Developer> developers = new ArrayList<Developer>( project.getDevelopers() );
        sortContributors( developers );

        if ( StringUtils.isNotEmpty( asfExtOptions.getChair() ) )
        {
            DoapUtil.writeStartElement( writer, asfExtOptions.getXmlnsPrefix(), "chair" );
            DoapUtil.writeStartElement( writer, "foaf", "Person" );
            DoapUtil.writeStartElement( writer, "foaf", "name" );
            writer.writeText( asfExtOptions.getChair() );
            writer.endElement(); // foaf:name
            writer.endElement(); // foaf:Person
            writer.endElement(); // asfext:chair
        }
        else
        {
            Developer chair = ASFExtOptionsUtil.findChair( developers );
            if ( chair != null )
            {
                writeContributor( writer, chair, asfExtOptions.getXmlnsPrefix(), "chair" );
                developers.remove( chair );
            }
            else
            {
                errorMessages.add( "A <asfExtOptions><chair/></asfExtOptions> parameter is required by ASF." );
            }
        }

        // asfext:member
        if ( developers != null && developers.size() > 0 )
        {
            List<Developer> pmcMembers = ASFExtOptionsUtil.findPMCMembers( developers );
            for ( Developer pmcMember : pmcMembers )
            {
                writeContributor( writer, pmcMember, asfExtOptions.getXmlnsPrefix(), "member" );
            }
        }

        writeASFImplements( writer );

        @SuppressWarnings( "unchecked" )
        Map<String, String> map = asfExtOptions.getExtra();
        writeExtra( writer, project, "Extra ASFExt vocabulary.", map, asfExtOptions.getXmlnsPrefix() );
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
            DoapUtil.writeStartElement( writer, asfExtOptions.getXmlnsPrefix(), "implements" );
            DoapUtil.writeStartElement( writer, asfExtOptions.getXmlnsPrefix(), "Standard" );

            if ( StringUtils.isEmpty( standard.getTitle() ) )
            {
                errorMessages.add( "A <asfExtOptions><standards><title/></standards></asfExtOptions> parameter is required by ASF." );
            }
            else
            {
                DoapUtil.writeElement( writer, asfExtOptions.getXmlnsPrefix(), "title", standard.getTitle() );
            }

            if ( StringUtils.isEmpty( standard.getBody() ) )
            {
                errorMessages.add( "A <asfExtOptions><standards><body/></standards></asfExtOptions> parameter is required by ASF." );
            }
            else
            {
                DoapUtil.writeElement( writer, asfExtOptions.getXmlnsPrefix(), "body", standard.getBody() );
            }

            if ( StringUtils.isEmpty( standard.getId() ) )
            {
                errorMessages.add( "A <asfExtOptions><standards><id/></standards></asfExtOptions> parameter is required by ASF." );
            }
            else
            {
                DoapUtil.writeElement( writer, asfExtOptions.getXmlnsPrefix(), "id", standard.getId() );
            }

            if ( StringUtils.isNotEmpty( standard.getUrl() ) )
            {
                try
                {
                    new URL( standard.getUrl() );

                    DoapUtil.writeElement( writer, asfExtOptions.getXmlnsPrefix(), "url", standard.getUrl() );
                }
                catch ( MalformedURLException e )
                {
                    errorMessages.add( "The <asfExtOptions><standards><url>" + standard.getUrl()
                        + "<url></standards></asfExtOptions> parameter is not a valid URL." );
                }
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

        for ( Entry<String, DoapUtil.Organization> organizationEntry : organizations )
        {
            DoapUtil.Organization organization = organizationEntry.getValue();

            DoapUtil.writeStartElement( writer, "foaf", "Organization" );
            if ( !StringUtils.isEmpty( organization.getName() ) )
            {
                DoapUtil.writeElement( writer, "foaf", "name", organization.getName() );
            }
            if ( !StringUtils.isEmpty( organization.getUrl() ) )
            {
                try
                {
                    new URL( organization.getUrl() );

                    DoapUtil.writeRdfResourceElement( writer, "foaf", "homepage", organization.getUrl() );
                }
                catch ( MalformedURLException e )
                {
                    errorMessages.add( "The organization URL " + organization.getUrl() + " is not a valid URL." );
                }
            }
            List<String> members = organization.getMembers();
            for ( String member : members )
            {
                DoapUtil.writeRdfNodeIdElement( writer, "foaf", "member", member );
            }
            writer.endElement(); // foaf:Organization
        }
    }

    /**
     * Write DOAP audience.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#audience">http://usefulinc.com/ns/doap#audience</a>
     * @since 1.1
     */
    private void writeAudience( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getAudience() ) )
        {
            return;
        }

        DoapUtil.writeComment( writer, "Audience." );
        DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "audience", doapOptions.getAudience().trim() );
    }

    /**
     * Write DOAP blog.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#blog">http://usefulinc.com/ns/doap#blog</a>
     * @since 1.1
     */
    private void writeBlog( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getBlog() ) )
        {
            return;
        }

        String blog = doapOptions.getBlog().trim();
        try
        {
            new URL( blog );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The <doapOptions><blog>" + blog + "</blog></doapOptions> parameter is not a valid URL." );
            return;
        }

        DoapUtil.writeComment( writer, "Blog page." );
        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "blog", blog );
    }

    /**
     * Write DOAP plateform.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#plateform">http://usefulinc.com/ns/doap#plateform</a>
     * @since 1.1
     */
    private void writePlateform( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getPlatform() ) )
        {
            return;
        }

        DoapUtil.writeComment( writer, "Plateform." );
        DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "plateform", doapOptions.getPlatform().trim() );
    }

    /**
     * Write DOAP vendor.
     *
     * @param writer not null
     * @param project the Maven project, not null
     * @see <a href="http://usefulinc.com/ns/doap#vendor">http://usefulinc.com/ns/doap#vendor</a>
     * @since 1.1
     */
    private void writeVendor( XMLWriter writer, MavenProject project )
    {
        String vendor = interpolate( doapOptions.getVendor(), project, settings );
        if ( StringUtils.isEmpty( vendor ) )
        {
            return;
        }

        DoapUtil.writeComment( writer, "Vendor." );
        DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "vendor", vendor );
    }

    /**
     * Write DOAP language.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#language">http://usefulinc.com/ns/doap#language</a>
     * @since 1.1
     */
    private void writeLanguage( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getLanguage() ) )
        {
            return;
        }

        boolean addComment = false;
        String[] languages = StringUtils.split( doapOptions.getLanguage(), "," );
        for ( String language : languages )
        {
            language = language.trim();

            if ( Arrays.binarySearch( Locale.getISOLanguages(), language ) < 0 )
            {
                errorMessages.add( "The <doapOptions><languages>" + language
                    + "</languages></doapOptions> parameter is not a valid ISO language." );
                continue;
            }

            if ( !addComment )
            {
                DoapUtil.writeComment( writer, "Language." );
                addComment = true;
            }
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "language", language );
        }
    }

    /**
     * Write DOAP service-endpoint.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#service-endpoint">http://usefulinc.com/ns/doap#service-endpoint</a>
     * @since 1.1
     */
    private void writeServiceEndpoint( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getServiceEndpoint() ) )
        {
            return;
        }

        String serviceEndpoint = doapOptions.getServiceEndpoint().trim();
        try
        {
            new URL( serviceEndpoint );
        }
        catch ( MalformedURLException e )
        {
            errorMessages.add( "The <doapOptions><serviceEndpoint>" + serviceEndpoint
                + "</serviceEndpoint></doapOptions> parameter is not a valid URL." );
            return;
        }

        DoapUtil.writeComment( writer, "Service endpoint." );
        DoapUtil.writeRdfResourceElement( writer, doapOptions.getXmlnsPrefix(), "service-endpoint", serviceEndpoint );
    }

    /**
     * Write DOAP implements.
     *
     * @param writer not null
     * @see <a href="http://usefulinc.com/ns/doap#implements">http://usefulinc.com/ns/doap#implements</a>
     * @since 1.1
     */
    private void writeImplements( XMLWriter writer )
    {
        if ( StringUtils.isEmpty( doapOptions.getImplementations() ) )
        {
            return;
        }

        DoapUtil.writeComment( writer, "Implements." );
        String[] implementations = StringUtils.split( doapOptions.getImplementations(), "," );
        for ( String implementation : implementations )
        {
            DoapUtil.writeElement( writer, doapOptions.getXmlnsPrefix(), "implements", implementation.trim() );
        }
    }

    /**
     * Write extra for DOAP or any extension.
     *
     * @param writer not null
     * @param project not null
     * @param comment not null
     * @param map not null
     * @param xmlnsPrefix not null
     * @since 1.1
     */
    private void writeExtra( XMLWriter writer, MavenProject project, String comment, Map<String, String> map,
                             String xmlnsPrefix )
    {
        if ( map == null || map.isEmpty() )
        {
            return;
        }

        boolean addComment = false;
        for ( Map.Entry<String, String> entry : map.entrySet() )
        {
            String key = entry.getKey();
            String value = entry.getValue();

            if ( value == null )
            {
                continue;
            }

            String interpolatedValue = interpolate( value, project, settings );
            if ( interpolatedValue == null )
            {
                continue;
            }

            if ( !addComment )
            {
                DoapUtil.writeComment( writer, comment );
                addComment = true;
            }

            try
            {
                new URL( interpolatedValue );

                DoapUtil.writeRdfResourceElement( writer, xmlnsPrefix, key, interpolatedValue );
            }
            catch ( MalformedURLException e )
            {
                DoapUtil.writeElement( writer, xmlnsPrefix, key, interpolatedValue );
            }
        }
    }

    /**
     * Write the extra DOAP extensions.
     *
     * @param writer not null
     * @since 1.1
     */
    private void writeExtensions( XMLWriter writer )
    {
        if ( !( extOptions != null && extOptions.length > 0 && !extOptions[0].getExtensions().isEmpty() ) )
        {
            return;
        }

        for ( ExtOptions extOption : extOptions )
        {
            @SuppressWarnings( "unchecked" )
            Map<String, String> map = extOption.getExtensions();
            writeExtra( writer, project, "Other extension vocabulary.", map, extOption.getXmlnsPrefix() );
        }
    }

    /**
     * @param tag not null
     * @since 1.1
     */
    private void addDoapMessage( String tag )
    {
        if ( asfExtOptions.isIncluded() )
        {
            errorMessages.add( "A <doapOptions><" + tag + "/></doapOptions> parameter is required by ASF." );
        }
        else
        {
            warnMessages.add( "No <doapOptions><" + tag
                + "/></doapOptions> parameter defined, it is highly recommended to have one." );
        }
    }

    /**
     * @param tag not null
     * @since 1.1
     */
    private void addPomMessage( String tag )
    {
        if ( asfExtOptions.isIncluded() )
        {
            errorMessages.add( "A POM <project><" + tag + "/></project> value is required by ASF." );
        }
        else
        {
            warnMessages.add( "No POM <project><" + tag
                + "/></project> value defined, it is highly recommended to have one." );
        }
    }

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

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

    /**
     * Interpolate a string with project and settings.
     *
     * @param value not null
     * @param project not null
     * @param settings not null
     * @return the value trimmed and interpolated or null if interpolation doesn't work.
     * @since 1.1
     */
    private static String interpolate( String value, MavenProject project, Settings settings )
    {
        if ( StringUtils.isEmpty( value ) || project == null )
        {
            throw new IllegalArgumentException( "parameters are required" );
        }

        if ( !value.contains( "${" ) )
        {
            return value.trim();
        }

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
        try
        {
            interpolator.addValueSource( new EnvarBasedValueSource() );
        }
        catch ( IOException e )
        {
        }

        interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );
        interpolator.addValueSource( new PropertiesBasedValueSource( project.getProperties() ) );
        interpolator.addValueSource( new PrefixedObjectValueSource( "project", project ) );
        interpolator.addValueSource( new PrefixedObjectValueSource( "pom", project ) );
        if ( settings != null )
        {
            interpolator.addValueSource( new PrefixedObjectValueSource( "settings", settings ) );
        }

        String interpolatedValue = value;
        try
        {
            interpolatedValue = interpolator.interpolate( value );
        }
        catch ( InterpolationException e )
        {
        }

        if ( interpolatedValue.contains( "${" ) )
        {
            return null;
        }

        return interpolatedValue.trim();
    }

    /**
     * Sort Contributor by name or Developer by id.
     *
     * @param contributors not null
     * @since 1.1
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static void sortContributors( List contributors )
    {
        Collections.sort( contributors, new Comparator<Contributor>()
        {
            public int compare( Contributor contributor1, Contributor contributor2 )
            {
                if ( contributor1 == contributor2 )
                {
                    return 0;
                }

                if ( contributor1 == null && contributor2 != null )
                {
                    return -1;
                }
                if ( contributor1 != null && contributor2 == null )
                {
                    return +1;
                }

                if ( Developer.class.isAssignableFrom( contributor1.getClass() )
                    && Developer.class.isAssignableFrom( contributor2.getClass() ) )
                {
                    Developer developer1 = (Developer) contributor1;
                    Developer developer2 = (Developer) contributor2;

                    if ( developer1.getId() == null && developer2.getId() != null )
                    {
                        return -1;
                    }
                    if ( developer1.getId() != null && developer2.getId() == null )
                    {
                        return +1;
                    }

                    return developer1.getId().compareTo( developer2.getId() );
                }

                if ( contributor1.getName() == null && contributor2.getName() != null )
                {
                    return -1;
                }
                if ( contributor1.getName() != null && contributor2.getName() == null )
                {
                    return +1;
                }
                return contributor1.getName().compareTo( contributor2.getName() );
            }
        } );
    }
}
