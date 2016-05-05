package org.apache.maven.report.projectinfo;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.doxia.site.decoration.Body;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Base class with the things that should be in AbstractMavenReport anyway.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @since 2.0
 */
public abstract class AbstractProjectInfoReport
    extends AbstractMavenReport
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * SiteTool component.
     *
     * @since 2.1
     */
    @Component
    protected SiteTool siteTool;

    /**
     * Doxia Site Renderer component.
     */
    @Component
    protected Renderer siteRenderer;

    /**
     * Artifact Resolver component.
     */
    @Component
    protected ArtifactResolver resolver;

    /**
     * Artifact Factory component.
     */
    @Component
    protected ArtifactFactory factory;

    /**
     * Internationalization component, could support also custom bundle using {@link #customBundle}.
     */
    @Component
    private I18N i18n;

    /**
     * Project builder.
     */
    @Component
    protected MavenProjectBuilder mavenProjectBuilder;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The output directory for the report. Note that this parameter is only evaluated if the goal is run directly from
     * the command line. If the goal is run indirectly as part of a site generation, the output directory configured in
     * the Maven Site Plugin is used instead.
     */
    @Parameter( property = "project.reporting.outputDirectory", required = true )
    protected File outputDirectory;

    /**
     * The Maven Project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * Local Repository.
     */
    @Parameter( property = "localRepository", required = true, readonly = true )
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @since 2.1
     */
    @Parameter( property = "project.remoteArtifactRepositories" )
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * The current user system settings for use in Maven.
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    protected Settings settings;

    /**
     * Path for a custom bundle instead of using the default one. <br>
     * Using this field, you could change the texts in the generated reports.
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "${project.basedir}/src/site/custom/project-info-reports.properties" )
    protected String customBundle;

    /**
     * Skip report.
     *
     * @since 2.8
     */
    @Parameter( property = "mpir.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Skip the project info report generation if a report-specific section of the POM is empty.
     * Defaults to <code>true</code>.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "true" )
    protected boolean skipEmptyReport;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public boolean canGenerateReport()
    {
        return !skip;
    }

    @Override
    public void execute()
        throws MojoExecutionException
    {
        if ( !canGenerateReport() )
        {
            return;
        }

        // TODO: push to a helper? Could still be improved by taking more of the site information from the site plugin
        Writer writer = null;
        try
        {
            String filename = getOutputName() + ".html";

            DecorationModel model = new DecorationModel();
            model.setBody( new Body() );

            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put( "outputEncoding", "UTF-8" );
            attributes.put( "project", project );

            Locale locale = Locale.getDefault();
            Artifact defaultSkin =
                siteTool.getDefaultSkinArtifact( localRepository, project.getRemoteArtifactRepositories() );

            // CHECKSTYLE_OFF: LineLength
            SiteRenderingContext siteContext =
                siteRenderer.createContextForSkin( defaultSkin.getFile(), attributes, model, getName( locale ), locale );
            // CHECKSTYLE_ON: LineLength

            RenderingContext context = new RenderingContext( outputDirectory, filename );

            SiteRendererSink sink = new SiteRendererSink( context );

            generate( sink, null, locale );

            outputDirectory.mkdirs();

            writer = new OutputStreamWriter( new FileOutputStream( new File( outputDirectory, filename ) ), "UTF-8" );

            siteRenderer.generateDocument( writer, sink, siteContext );

            siteRenderer.copyResources( siteContext, new File( project.getBasedir(), "src/site/resources" ),
                                        outputDirectory );

            writer.close();
            writer = null;
        }
        catch ( RendererException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "An error has occurred in " + getName( Locale.ENGLISH )
                + " report generation.", e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    @Override
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /**
     * @param coll The collection to be checked.
     * @return true if coll is empty false otherwise.
     */
    protected boolean isEmpty( Collection<?> coll )
    {
        return coll == null || coll.isEmpty();
    }

    @Override
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    @Override
    public File getReportOutputDirectory()
    {
        return outputDirectory;
    }

    @Override
    public void setReportOutputDirectory( File reportOutputDirectory )
    {
        this.outputDirectory = reportOutputDirectory;
    }

    @Override
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @param pluginId The id of the plugin
     * @return The information about the plugin.
     */
    protected Plugin getPlugin( String pluginId )
    {
        if ( ( getProject().getBuild() == null ) || ( getProject().getBuild().getPluginsAsMap() == null ) )
        {
            return null;
        }

        Plugin plugin = (Plugin) getProject().getBuild().getPluginsAsMap().get( pluginId );

        if ( ( plugin == null ) && ( getProject().getBuild().getPluginManagement() != null )
            && ( getProject().getBuild().getPluginManagement().getPluginsAsMap() != null ) )
        {
            plugin = (Plugin) getProject().getBuild().getPluginManagement().getPluginsAsMap().get( pluginId );
        }

        return plugin;
    }

    /**
     * @param pluginId The pluginId
     * @param param The child which should be checked.
     * @return The value of the dom tree.
     */
    protected String getPluginParameter( String pluginId, String param )
    {
        Plugin plugin = getPlugin( pluginId );
        if ( plugin != null )
        {
            Xpp3Dom xpp3Dom = (Xpp3Dom) plugin.getConfiguration();
            if ( xpp3Dom != null && xpp3Dom.getChild( param ) != null
                && StringUtils.isNotEmpty( xpp3Dom.getChild( param ).getValue() ) )
            {
                return xpp3Dom.getChild( param ).getValue();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @param locale The locale
     * @param key The key to search for
     * @return The text appropriate for the locale.
     */
    protected String getI18nString( Locale locale, String key )
    {
        return getI18N( locale ).getString( "project-info-reports", locale, "report." + getI18Nsection() + '.' + key );
    }

    /**
     * @param locale The local.
     * @return I18N for the locale
     */
    protected I18N getI18N( Locale locale )
    {
        if ( customBundle != null )
        {
            File customBundleFile = new File( customBundle );
            if ( customBundleFile.isFile() && customBundleFile.getName().endsWith( ".properties" ) )
            {
                if ( !i18n.getClass().isAssignableFrom( CustomI18N.class ) )
                {
                    // first load
                    i18n = new CustomI18N( project, settings, customBundleFile, locale, i18n );
                }
                else if ( !i18n.getDefaultLanguage().equals( locale.getLanguage() ) )
                {
                    i18n = new CustomI18N( project, settings, customBundleFile, locale, i18n );
                }
            }
        }

        return i18n;
    }

    /**
     * @return The according string for the section.
     */
    protected abstract String getI18Nsection();

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getI18nString( locale, "name" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getI18nString( locale, "description" );
    }

    private static class CustomI18N
        implements I18N
    {
        private final MavenProject project;

        private final Settings settings;

        private final String bundleName;

        private final Locale locale;

        private final I18N i18nOriginal;

        private ResourceBundle bundle;

        private static final Object[] NO_ARGS = new Object[0];

        public CustomI18N( MavenProject project, Settings settings, File customBundleFile, Locale locale,
                           I18N i18nOriginal )
        {
            super();
            this.project = project;
            this.settings = settings;
            this.locale = locale;
            this.i18nOriginal = i18nOriginal;
            this.bundleName =
                customBundleFile.getName().substring( 0, customBundleFile.getName().indexOf( ".properties" ) );

            URLClassLoader classLoader = null;
            try
            {
                classLoader = new URLClassLoader( new URL[] { customBundleFile.getParentFile().toURI().toURL() } );
            }
            catch ( MalformedURLException e )
            {
                //could not happen.
            }

            this.bundle = ResourceBundle.getBundle( this.bundleName, locale, classLoader );
            if ( !this.bundle.getLocale().getLanguage().equals( locale.getLanguage() ) )
            {
                this.bundle = ResourceBundle.getBundle( this.bundleName, Locale.getDefault(), classLoader );
            }
        }

        /** {@inheritDoc} */
        public String getDefaultLanguage()
        {
            return locale.getLanguage();
        }

        /** {@inheritDoc} */
        public String getDefaultCountry()
        {
            return locale.getCountry();
        }

        /** {@inheritDoc} */
        public String getDefaultBundleName()
        {
            return bundleName;
        }

        /** {@inheritDoc} */
        public String[] getBundleNames()
        {
            return new String[] { bundleName };
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle()
        {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle( String bundleName )
        {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle( String bundleName, String languageHeader )
        {
            return bundle;
        }

        /** {@inheritDoc} */
        public ResourceBundle getBundle( String bundleName, Locale locale )
        {
            return bundle;
        }

        /** {@inheritDoc} */
        public Locale getLocale( String languageHeader )
        {
            return new Locale( languageHeader );
        }

        /** {@inheritDoc} */
        public String getString( String key )
        {
            return getString( bundleName, locale, key );
        }

        /** {@inheritDoc} */
        public String getString( String key, Locale locale )
        {
            return getString( bundleName, locale, key );
        }

        /** {@inheritDoc} */
        public String getString( String bundleName, Locale locale, String key )
        {
            String value;

            if ( locale == null )
            {
                locale = getLocale( null );
            }

            ResourceBundle rb = getBundle( bundleName, locale );
            value = getStringOrNull( rb, key );

            if ( value == null )
            {
                // try to load default
                value = i18nOriginal.getString( bundleName, locale, key );
            }

            if ( !value.contains( "${" ) )
            {
                return value;
            }

            final RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
            try
            {
                interpolator.addValueSource( new EnvarBasedValueSource() );
            }
            catch ( final IOException e )
            {
                //In which cases could this happen? And what should we do?
            }

            interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );
            interpolator.addValueSource( new PropertiesBasedValueSource( project.getProperties() ) );
            interpolator.addValueSource( new PrefixedObjectValueSource( "project", project ) );
            interpolator.addValueSource( new PrefixedObjectValueSource( "pom", project ) );
            interpolator.addValueSource( new PrefixedObjectValueSource( "settings", settings ) );

            try
            {
                value = interpolator.interpolate( value );
            }
            catch ( final InterpolationException e )
            {
                //What does this exception mean?
            }

            return value;
        }

        /** {@inheritDoc} */
        public String format( String key, Object arg1 )
        {
            return format( bundleName, locale, key, new Object[] { arg1 } );
        }

        /** {@inheritDoc} */
        public String format( String key, Object arg1, Object arg2 )
        {
            return format( bundleName, locale, key, new Object[] { arg1, arg2 } );
        }

        /** {@inheritDoc} */
        public String format( String bundleName, Locale locale, String key, Object arg1 )
        {
            return format( bundleName, locale, key, new Object[] { arg1 } );
        }

        /** {@inheritDoc} */
        public String format( String bundleName, Locale locale, String key, Object arg1, Object arg2 )
        {
            return format( bundleName, locale, key, new Object[] { arg1, arg2 } );
        }

        /** {@inheritDoc} */
        public String format( String bundleName, Locale locale, String key, Object[] args )
        {
            if ( locale == null )
            {
                locale = getLocale( null );
            }

            String value = getString( bundleName, locale, key );
            if ( args == null )
            {
                args = NO_ARGS;
            }

            MessageFormat messageFormat = new MessageFormat( "" );
            messageFormat.setLocale( locale );
            messageFormat.applyPattern( value );

            return messageFormat.format( args );
        }

        private String getStringOrNull( ResourceBundle rb, String key )
        {
            if ( rb != null )
            {
                try
                {
                    return rb.getString( key );
                }
                catch ( MissingResourceException ignored )
                {
                    // intentional
                }
            }
            return null;
        }
    }
}
