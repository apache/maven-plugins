package org.apache.maven.plugins.site;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.util.interpolation.MapBasedValueSource;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Base class for site mojos.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractSiteMojo
    extends AbstractMojo
{
    /**
     * The locale by default for all default bundles
     */
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * A comma separated list of locales supported by Maven. The first valid token will be the default Locale
     * for this instance of the Java Virtual Machine.
     *
     * @parameter expression="${locales}"
     */
    private String locales;

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

    /**
     * Directory containing the site.xml file and the source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    protected File siteDirectory;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}"
     */
    protected ArtifactRepository localRepository;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Project builder
     *
     * @component
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * Specifies the input encoding.
     *
     * @parameter expression="${inputEncoding}" default-value="ISO-8859-1"
     */
    protected String inputEncoding;

    /**
     * Specifies the output encoding.
     *
     * @parameter expression="${outputEncoding}" default-value="ISO-8859-1"
     */
    protected String outputEncoding;

    /**
     * Init the <code>localesList</code> variable.
     * <p>If <code>locales</code> variable is available, the first valid token will be the <code>defaultLocale</code>
     * for this instance of the Java Virtual Machine.</p>
     *
     * @return a list of <code>Locale</code>
     */
    protected List getAvailableLocales()
    {
        List localesList = new ArrayList();
        if ( locales != null )
        {
            String[] localesArray = StringUtils.split( locales, "," );

            for ( int i = 0; i < localesArray.length; i++ )
            {
                Locale locale = codeToLocale( localesArray[i] );

                if ( locale != null )
                {
                    if ( !Arrays.asList( Locale.getAvailableLocales() ).contains( locale ) )
                    {
                        getLog().warn( "The locale parsed defined by '" + locale +
                            "' is not available in this Java Virtual Machine (" + System.getProperty( "java.version" ) +
                            " from " + System.getProperty( "java.vendor" ) + ") - IGNORING" );
                        continue;
                    }

                    // Default bundles are in English
                    if ( !locale.getLanguage().equals( DEFAULT_LOCALE.getLanguage() ) )
                    {
                        if ( !i18n.getBundle( "site-plugin", locale ).getLocale().getLanguage().equals(
                            locale.getLanguage() ) )
                        {
                            StringBuffer sb = new StringBuffer();

                            sb.append( "The locale '" ).append( locale ).append( "' (" );
                            sb.append( locale.getDisplayName( Locale.ENGLISH ) );
                            sb.append( ") is not currently support by Maven - IGNORING. " );
                            sb.append( "\n" );
                            sb.append( "Contribution are welcome and greatly appreciated! " );
                            sb.append( "\n" );
                            sb.append( "If you want to contribute a new translation, please visit " );
                            sb.append( "http://maven.apache.org/plugins/maven-site-plugin/i18n.html " );
                            sb.append( "for detailed instructions." );

                            getLog().warn( sb.toString() );

                            continue;
                        }
                    }

                    localesList.add( locale );
                }
            }
        }

        if ( localesList.isEmpty() )
        {
            localesList = Collections.singletonList( DEFAULT_LOCALE );
        }

        return localesList;
    }

    /**
     * Converts a locale code like "en", "en_US" or "en_US_win" to a <code>java.util.Locale</code>
     * object.
     * <p>If localeCode = <code>default</code>, return the current value of the default locale for this instance
     * of the Java Virtual Machine.</p>
     *
     * @param localeCode the locale code string.
     * @return a java.util.Locale object instancied or null if errors occurred
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/Locale.html">java.util.Locale#getDefault()</a>
     */
    private Locale codeToLocale( String localeCode )
    {
        if ( localeCode == null )
        {
            return null;
        }

        if ( "default".equalsIgnoreCase( localeCode ) )
        {
            return Locale.getDefault();
        }

        String language = "";
        String country = "";
        String variant = "";

        StringTokenizer tokenizer = new StringTokenizer( localeCode, "_" );
        if ( tokenizer.countTokens() > 3 )
        {
            getLog().warn( "Invalid java.util.Locale format for '" + localeCode + "' entry - IGNORING" );
            return null;
        }

        if ( tokenizer.hasMoreTokens() )
        {
            language = tokenizer.nextToken();
            if ( tokenizer.hasMoreTokens() )
            {
                country = tokenizer.nextToken();
                if ( tokenizer.hasMoreTokens() )
                {
                    variant = tokenizer.nextToken();
                }
            }
        }

        return new Locale( language, country, variant );
    }

    /**
     * Get the path of the site descriptor for a given locale.
     *
     * @param basedir the base dir
     * @param locale the locale
     * @return the site descriptor path
     */
    protected File getSiteDescriptorFile( File basedir, Locale locale )
    {
        String relativePath = getRelativePath( siteDirectory.getAbsolutePath(), basedir.getAbsolutePath() );

        File siteDescriptor = new File( relativePath, "site_" + locale.getLanguage() + ".xml" );

        if ( !siteDescriptor.exists() )
        {
            siteDescriptor = new File( relativePath, "site.xml" );
        }
        return siteDescriptor;
    }

    protected void populateModules( DecorationModel decorationModel, Locale locale, boolean keepInheritedRefs )
        throws MojoExecutionException
    {
        Menu menu = decorationModel.getMenuRef( "modules" );

        if ( menu != null )
        {
            if ( !keepInheritedRefs || !menu.isInheritAsRef() )
            {
                // we require child modules and reactors to process module menu
                if ( project.getModules().size() > 0 )
                {
                    List projects = this.reactorProjects;

                    if ( menu.getName() == null )
                    {
                        menu.setName( i18n.getString( "site-plugin", locale, "report.menu.projectmodules" ) );
                    }

                    if ( projects.size() == 1 )
                    {
                        getLog().debug( "Attempting to source module information from local filesystem" );

                        // Not running reactor - search for the projects manually
                        List models = new ArrayList( project.getModules().size() );
                        for ( Iterator i = project.getModules().iterator(); i.hasNext(); )
                        {
                            String module = (String) i.next();
                            Model model;
                            File f = new File( project.getBasedir(), module + "/pom.xml" );
                            if ( f.exists() )
                            {
                                try
                                {
                                    model = mavenProjectBuilder.build( f, localRepository, null ).getModel();
                                }
                                catch ( ProjectBuildingException e )
                                {
                                    throw new MojoExecutionException( "Unable to read local module-POM", e );
                                }
                            }
                            else
                            {
                                getLog().warn( "No filesystem module-POM available" );

                                model = new Model();
                                model.setName( module );
                                model.setUrl( module );
                            }
                            models.add( model );
                        }
                        populateModulesMenuItemsFromModels( models, menu );
                    }
                    else
                    {
                        populateModulesMenuItemsFromReactorProjects( menu );
                    }
                }
                else
                {
                    decorationModel.removeMenuRef( "modules" );
                }
            }
        }
    }

    private void populateModulesMenuItemsFromReactorProjects( Menu menu )
    {
        if ( reactorProjects != null && reactorProjects.size() > 1 )
        {
            Iterator reactorItr = reactorProjects.iterator();

            while ( reactorItr.hasNext() )
            {
                MavenProject reactorProject = (MavenProject) reactorItr.next();

                if ( reactorProject != null && reactorProject.getParent() != null &&
                    project.getArtifactId().equals( reactorProject.getParent().getArtifactId() ) )
                {
                    String reactorUrl = reactorProject.getUrl();
                    String name = reactorProject.getName();

                    appendMenuItem( menu, name, reactorUrl, reactorProject.getArtifactId() );
                }
            }
        }
    }

    private void populateModulesMenuItemsFromModels( List models, Menu menu )
    {
        if ( models != null && models.size() > 1 )
        {
            Iterator reactorItr = models.iterator();

            while ( reactorItr.hasNext() )
            {
                Model model = (Model) reactorItr.next();

                String reactorUrl = model.getUrl();
                String name = model.getName();

                appendMenuItem( menu, name, reactorUrl, model.getArtifactId() );
            }
        }
    }

    private void appendMenuItem( Menu menu, String name, String href, String defaultHref )
    {
        String selectedHref = href;

        if ( selectedHref == null )
        {
            selectedHref = defaultHref;
        }

        MenuItem item = new MenuItem();
        item.setName( name );

        String baseUrl = project.getUrl();
        if ( baseUrl != null )
        {
            selectedHref = getRelativePath( selectedHref, baseUrl );
        }

        if ( selectedHref.endsWith( "/" ) )
        {
            item.setHref( selectedHref + "index.html" );
        }
        else
        {
            item.setHref( selectedHref + "/index.html" );
        }
        menu.addItem( item );
    }

    protected void populateReportsMenu( DecorationModel decorationModel, Locale locale, Map categories )
    {
        Menu menu = decorationModel.getMenuRef( "reports" );

        if ( menu != null )
        {
            if ( menu.getName() == null )
            {
                menu.setName( i18n.getString( "site-plugin", locale, "report.menu.projectdocumentation" ) );
            }

            boolean found = false;
            if ( menu.getItems().isEmpty() )
            {
                List categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_INFORMATION );
                if ( !isEmptyList( categoryReports ) )
                {
                    MenuItem item = createCategoryMenu(
                        i18n.getString( "site-plugin", locale, "report.menu.projectinformation" ), "/project-info.html",
                        categoryReports, locale );
                    menu.getItems().add( item );
                    found = true;
                }

                categoryReports = (List) categories.get( MavenReport.CATEGORY_PROJECT_REPORTS );
                if ( !isEmptyList( categoryReports ) )
                {
                    MenuItem item = createCategoryMenu(
                        i18n.getString( "site-plugin", locale, "report.menu.projectreports" ), "/project-reports.html",
                        categoryReports, locale );
                    menu.getItems().add( item );
                    found = true;
                }
            }
            if ( !found )
            {
                decorationModel.removeMenuRef( "reports" );
            }
        }
    }

    private MenuItem createCategoryMenu( String name, String href, List categoryReports, Locale locale )
    {
        MenuItem item = new MenuItem();
        item.setName( name );
        item.setCollapse( true );
        item.setHref( href );

        Collections.sort( categoryReports, new ReportComparator( locale ) );

        for ( Iterator k = categoryReports.iterator(); k.hasNext(); )
        {
            MavenReport report = (MavenReport) k.next();

            MenuItem subitem = new MenuItem();
            subitem.setName( report.getName( locale ) );
            subitem.setHref( report.getOutputName() + ".html" );
            item.getItems().add( subitem );
        }

        return item;
    }

    protected void populateReportItems( DecorationModel decorationModel, Locale locale, Map reportsByOutputName )
    {
        for ( Iterator i = decorationModel.getMenus().iterator(); i.hasNext(); )
        {
            Menu menu = (Menu) i.next();

            populateItemRefs( menu.getItems(), locale, reportsByOutputName );
        }
    }

    private void populateItemRefs( List items, Locale locale, Map reportsByOutputName )
    {
        for ( Iterator i = items.iterator(); i.hasNext(); )
        {
            MenuItem item = (MenuItem) i.next();

            if ( item.getRef() != null )
            {
                if ( reportsByOutputName.containsKey( item.getRef() ) )
                {
                    MavenReport report = (MavenReport) reportsByOutputName.get( item.getRef() );

                    if ( item.getName() == null )
                    {
                        item.setName( report.getName( locale ) );
                    }

                    if ( item.getHref() == null || item.getHref().length() == 0 )
                    {
                        item.setHref( report.getOutputName() + ".html" );
                    }
                }
                else
                {
                    getLog().warn( "Unrecognised reference: '" + item.getRef() + "'" );
                    i.remove();
                }
            }
            populateItemRefs( item.getItems(), locale, reportsByOutputName );
        }
    }

    private static boolean isEmptyList( List list )
    {
        return list == null || list.isEmpty();
    }

    protected String getRelativePath( String to, String from )
    {
        URL toUrl = null;
        URL fromUrl = null;

        String toPath = to;
        String fromPath = from;

        try
        {
            toUrl = new URL( to );
        }
        catch ( MalformedURLException e )
        {
            try
            {
                toUrl = new File( to ).toURL();
            }
            catch ( MalformedURLException e1 )
            {
                getLog().warn( "Unable to load a URL for '" + to + "': " + e.getMessage() );
            }
        }

        try
        {
            fromUrl = new URL( from );
        }
        catch ( MalformedURLException e )
        {
            try
            {
                toUrl = new File( from ).toURL();
            }
            catch ( MalformedURLException e1 )
            {
                getLog().warn( "Unable to load a URL for '" + from + "': " + e.getMessage() );
            }
        }

        if ( toUrl != null && fromUrl != null )
        {
            // URLs, determine if they share protocol and domain info

            if ( ( toUrl.getProtocol().equalsIgnoreCase( fromUrl.getProtocol() ) ) &&
                ( toUrl.getHost().equalsIgnoreCase( fromUrl.getHost() ) ) && ( toUrl.getPort() == fromUrl.getPort() ) )
            {
                // shared URL domain details, use URI to determine relative path

                toPath = toUrl.getFile();
                fromPath = fromUrl.getFile();
            }
            else
            {
                // dont share basic URL infomation, no relative available

                return to;
            }
        }
        else if ( ( toUrl != null && fromUrl == null ) || ( toUrl == null && fromUrl != null ) )
        {
            // one is a URL and the other isnt, no relative available.

            return to;
        }

        // either the two locations are not URLs or if they are they
        // share the common protocol and domain info and we are left
        // with their URI information

        // normalise the path delimters

        toPath = new File( toPath ).getPath();
        fromPath = new File( fromPath ).getPath();

        // strip any leading slashes if its a windows path
        if ( toPath.matches( "^\\[a-zA-Z]:" ) )
        {
            toPath = toPath.substring( 1 );
        }
        if ( fromPath.matches( "^\\[a-zA-Z]:" ) )
        {
            fromPath = fromPath.substring( 1 );
        }

        // lowercase windows drive letters.

        if ( toPath.startsWith( ":", 1 ) )
        {
            toPath = toPath.substring( 0, 1 ).toLowerCase() + toPath.substring( 1 );
        }
        if ( fromPath.startsWith( ":", 1 ) )
        {
            fromPath = fromPath.substring( 0, 1 ).toLowerCase() + fromPath.substring( 1 );
        }

        // check for the presence of windows drives. No relative way of
        // traversing from one to the other.

        if ( ( toPath.startsWith( ":", 1 ) && fromPath.startsWith( ":", 1 ) ) &&
            ( !toPath.substring( 0, 1 ).equals( fromPath.substring( 0, 1 ) ) ) )
        {
            // they both have drive path element but they dont match, no
            // relative path

            return to;
        }

        if ( ( toPath.startsWith( ":", 1 ) && !fromPath.startsWith( ":", 1 ) ) ||
            ( !toPath.startsWith( ":", 1 ) && fromPath.startsWith( ":", 1 ) ) )
        {

            // one has a drive path element and the other doesnt, no relative
            // path.

            return to;

        }

        // use tokeniser to traverse paths and for lazy checking
        StringTokenizer toTokeniser = new StringTokenizer( toPath, File.separator );
        StringTokenizer fromTokeniser = new StringTokenizer( fromPath, File.separator );

        int count = 0;

        // walk along the to path looking for divergence from the from path
        while ( toTokeniser.hasMoreTokens() && fromTokeniser.hasMoreTokens() )
        {
            if ( File.separatorChar == '\\' )
            {
                if ( !fromTokeniser.nextToken().equalsIgnoreCase( toTokeniser.nextToken() ) )
                {
                    break;
                }
            }
            else
            {
                if ( !fromTokeniser.nextToken().equals( toTokeniser.nextToken() ) )
                {
                    break;
                }
            }

            count++;
        }

        // reinitialise the tokenisers to count positions to retrieve the
        // gobbled token

        toTokeniser = new StringTokenizer( toPath, File.separator );
        fromTokeniser = new StringTokenizer( fromPath, File.separator );

        while ( count-- > 0 )
        {
            fromTokeniser.nextToken();
            toTokeniser.nextToken();
        }

        String relativePath = "";

        // add back refs for the rest of from location.
        while ( fromTokeniser.hasMoreTokens() )
        {
            fromTokeniser.nextToken();

            relativePath += "..";

            if ( fromTokeniser.hasMoreTokens() )
            {
                relativePath += File.separatorChar;
            }
        }

        if ( relativePath.length() != 0 && toTokeniser.hasMoreTokens() )
        {
            relativePath += File.separatorChar;
        }

        // add fwd fills for whatevers left of to.
        while ( toTokeniser.hasMoreTokens() )
        {
            relativePath += toTokeniser.nextToken();

            if ( toTokeniser.hasMoreTokens() )
            {
                relativePath += File.separatorChar;
            }
        }

        if ( !relativePath.equals( to ) )
        {
            getLog().debug( "Mapped url: " + to + " to relative path: " + relativePath );
        }

        return relativePath;
    }

    protected void populateProjectParentMenu( DecorationModel decorationModel, Locale locale,
                                              MavenProject parentProject, boolean keepInheritedRefs )
    {
        Menu menu = decorationModel.getMenuRef( "parent" );

        if ( menu != null )
        {
            if ( !keepInheritedRefs || !menu.isInheritAsRef() )
            {
                String parentUrl = parentProject.getUrl();

                if ( parentUrl != null )
                {
                    if ( parentUrl.endsWith( "/" ) )
                    {
                        parentUrl += "index.html";
                    }
                    else
                    {
                        parentUrl += "/index.html";
                    }

                    parentUrl = getRelativePath( parentUrl, project.getUrl() );

                    if ( menu.getName() == null )
                    {
                        menu.setName( i18n.getString( "site-plugin", locale, "report.menu.parentproject" ) );
                    }

                    MenuItem item = new MenuItem();
                    item.setName( parentProject.getName() );
                    item.setHref( parentUrl );
                    menu.addItem( item );
                }
                else
                {
                    decorationModel.removeMenuRef( "parent" );
                }
            }
        }
    }

    /**
     * Returns the parent POM URL. Attempts to source this value from the reactor env
     * if available (reactor env model attributes are interpolated), or if the
     * reactor is unavailable (-N) resorts to the project.getParent().getUrl() value
     * which will NOT have be interpolated.
     * <p/>
     * TODO: once bug is fixed in Maven proper, remove this
     *
     * @param aProject
     * @return parent project URL.
     */
    protected MavenProject getParentProject( MavenProject aProject )
    {
        MavenProject parentProject = null;

        MavenProject origParent = aProject.getParent();
        if ( origParent != null )
        {
            Iterator reactorItr = reactorProjects.iterator();

            while ( reactorItr.hasNext() )
            {
                MavenProject reactorProject = (MavenProject) reactorItr.next();

                if ( reactorProject.getGroupId().equals( origParent.getGroupId() ) &&
                    reactorProject.getArtifactId().equals( origParent.getArtifactId() ) &&
                    reactorProject.getVersion().equals( origParent.getVersion() ) )
                {
                    parentProject = reactorProject;
                    break;
                }
            }

            if ( parentProject == null && aProject.getBasedir() != null )
            {
                try
                {
                    MavenProject mavenProject = mavenProjectBuilder.build(
                        new File( aProject.getBasedir(), aProject.getModel().getParent().getRelativePath() ),
                        localRepository, null );
                    if ( mavenProject.getGroupId().equals( origParent.getGroupId() ) &&
                        mavenProject.getArtifactId().equals( origParent.getArtifactId() ) &&
                        mavenProject.getVersion().equals( origParent.getVersion() ) )
                    {
                        parentProject = mavenProject;
                    }
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().warn( "Unable to load parent project from a relative path: " + e.getMessage() );
                }
            }

            if ( parentProject == null )
            {
                try
                {
                    parentProject = mavenProjectBuilder.buildFromRepository( aProject.getParentArtifact(),
                                                                             aProject.getRemoteArtifactRepositories(),
                                                                             localRepository );
                }
                catch ( ProjectBuildingException e )
                {
                    getLog().warn( "Unable to load parent project from repository: " + e.getMessage() );
                }
            }

            if ( parentProject == null )
            {
                // fallback to uninterpolated value

                parentProject = origParent;
            }
        }
        return parentProject;
    }

    /**
     * Interpolating several expressions in the site descriptor content. Actually, the expressions could be on
     * the project, the environment variables and the specific properties like <code>encoding</code>.
     * <p/>
     * For instance:
     * <dl>
     * <dt>${project.name}</dt>
     * <dd>The value from the POM of:
     * <p>
     * &lt;project&gt;<br>
     * &lt;name&gt;myProjectName&lt;/name&gt;<br>
     * &lt;/project&gt;
     * </p></dd>
     * <dt>${my.value}</dt>
     * <dd>The value from the POM of:
     * <p>
     * &lt;properties&gt;<br>
     * &lt;my.value&gt;hello&lt;/my.value&gt;<br>
     * &lt;/properties&gt;
     * </p></dd>
     * <dt>${JAVA_HOME}</dt>
     * <dd>The value of JAVA_HOME in the environment variables</dd>
     * </dl>
     *
     * @param props
     * @param aProject
     * @param siteDescriptorContent
     * @return the site descriptor content with interpolate string
     * @throws IOException
     */
    protected String getInterpolatedSiteDescriptorContent( Map props, MavenProject aProject, String siteDescriptorContent )
        throws IOException
    {
        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource( new EnvarBasedValueSource() );
        interpolator.addValueSource( new ObjectBasedValueSource( aProject ) );
        interpolator.addValueSource( new MapBasedValueSource( aProject.getProperties() ) );

        siteDescriptorContent = interpolator.interpolate( siteDescriptorContent, "project" );

        props.put( "inputEncoding", inputEncoding );
        props.put( "outputEncoding", outputEncoding );

        // Legacy for the old ${parentProject} syntax
        props.put( "parentProject", "<menu ref=\"parent\"/>" );

        return StringUtils.interpolate( siteDescriptorContent, props );
    }
}
