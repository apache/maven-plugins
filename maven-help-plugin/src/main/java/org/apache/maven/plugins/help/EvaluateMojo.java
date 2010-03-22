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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.lang.ClassUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.collections.PropertiesConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Evaluates Maven expressions given by the user in an interactive mode.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 * @goal evaluate
 * @requiresProject false
 */
public class EvaluateMojo
    extends AbstractMojo
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven Artifact Factory component.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Input handler, needed for command line handling.
     *
     * @component
     */
    private InputHandler inputHandler;

    /**
     * Maven Project Builder component.
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * @component
     */
    private PathTranslator pathTranslator;

    /**
     * Artifact Resolver component.
     *
     * @component
     */
    private ArtifactResolver resolver;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * An artifact for evaluating Maven expressions.
     * <br/>
     * <b>Note</b>: Should respect the Maven format, i.e. <code>groupId:artifactId[:version][:classifier]</code>.
     *
     * @parameter expression="${artifact}"
     */
    private String artifact;

    /**
     * An expression to evaluate instead of prompting. Note that this <i>must not</i> include the surrounding ${...}.
     *
     * @parameter expression="${expression}"
     */
    private String expression;

    /**
     * Local Repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * The current Maven project or the super pom.
     *
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * Remote repositories used for the project.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List remoteRepositories;

    /**
     * The system settings for Maven.
     *
     * @parameter expression="${settings}"
     * @readonly
     * @required
     */
    protected Settings settings;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    // ----------------------------------------------------------------------
    // Instance variables
    // ----------------------------------------------------------------------

    /** lazy loading evaluator variable */
    private PluginParameterExpressionEvaluator evaluator;

    /** lazy loading xstream variable */
    private XStream xstream;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( expression == null && !settings.isInteractiveMode() )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "Maven is configured to NOT interact with the user for input. " );
            msg.append( "This Mojo requires that 'interactiveMode' in your settings file is flag to 'true'." );

            getLog().error( msg.toString() );
            return;
        }

        validateParameters();

        if ( StringUtils.isNotEmpty( artifact ) )
        {
            Artifact artifactObj = getArtifact( artifact );

            try
            {
                project = getMavenProject( artifactObj );
            }
            catch ( ProjectBuildingException e )
            {
                throw new MojoExecutionException( "Unable to get the POM for the artifact '" + artifact
                    + "'. Verify the artifact parameter." );
            }
        }

        if ( expression == null )
        {
            while ( true )
            {
                getLog().info( "Enter the Maven expression i.e. ${project.groupId} or 0 to exit?:" );

                try
                {
                    String userExpression = inputHandler.readLine();
                    if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equals( "0" ) )
                    {
                        break;
                    }

                    handleResponse( userExpression );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to read from standard input.", e );
                }
            }
        }
        else
        {
            handleResponse( "${" + expression + "}" );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Validate Mojo parameters.
     */
    private void validateParameters()
    {
        if ( artifact == null )
        {
            // using project if found or super-pom
            getLog().info( "No artifact parameter specified, using '" + project.getId() + "' as project." );
        }
    }

    /**
     * @param artifactString should respect the format <code>groupId:artifactId[:version][:classifier]</code>
     * @return the <code>Artifact</code> object for the <code>artifactString</code> parameter.
     * @throws MojoExecutionException if the <code>artifactString</code> doesn't respect the format.
     */
    private Artifact getArtifact( String artifactString )
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( artifactString ) )
        {
            throw new IllegalArgumentException( "artifact parameter could not be empty" );
        }

        String groupId = null; // required
        String artifactId = null; // required
        String version = null; // optional
        String classifier = null; // optional

        String[] artifactParts = artifactString.split( ":" );

        switch ( artifactParts.length )
        {
            case ( 2 ):
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = Artifact.LATEST_VERSION;
                break;
            case ( 3 ):
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = artifactParts[2];
                break;
            case ( 4 ):
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = artifactParts[2];
                classifier = artifactParts[3];
                break;
            default:
                throw new MojoExecutionException( "The artifact parameter '" + artifactString
                    + "' should be conform to: " + "'groupId:artifactId[:version][:classifier]'." );
        }

        if ( StringUtils.isNotEmpty( classifier ) )
        {
            return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, "jar", classifier );
        }

        return artifactFactory.createArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar" );
    }

    /**
     * @param artifactObj not null
     * @return the POM for the given artifact.
     * @throws MojoExecutionException if the artifact has a system scope.
     * @throws ProjectBuildingException when building pom.
     */
    private MavenProject getMavenProject( Artifact artifactObj )
        throws MojoExecutionException, ProjectBuildingException
    {
        if ( Artifact.SCOPE_SYSTEM.equals( artifactObj.getScope() ) )
        {
            throw new MojoExecutionException( "System artifact is not be handled." );
        }

        Artifact copyArtifact = ArtifactUtils.copyArtifact( artifactObj );
        if ( !"pom".equals( copyArtifact.getType() ) )
        {
            copyArtifact =
                artifactFactory.createProjectArtifact( copyArtifact.getGroupId(), copyArtifact.getArtifactId(),
                                                       copyArtifact.getVersion(), copyArtifact.getScope() );
        }

        return mavenProjectBuilder.buildFromRepository( copyArtifact, remoteRepositories, localRepository );
    }

    /**
     * @return a lazy loading evaluator object.
     * @throws MojoExecutionException if any
     * @throws MojoFailureException if any reflection exceptions occur or missing components.
     * @see #getMojoDescriptor(String, MavenSession, MavenProject, String, boolean, boolean)
     */
    private PluginParameterExpressionEvaluator getEvaluator()
        throws MojoExecutionException, MojoFailureException
    {
        if ( evaluator == null )
        {
            MojoDescriptor mojoDescriptor =
                HelpUtil.getMojoDescriptor( "help:evaluate", session, project, "help:evaluate", true, false );
            MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
            evaluator =
                new PluginParameterExpressionEvaluator( session, mojoExecution, pathTranslator,
                                                        session.getContainer().getLogger(), project,
                                                        session.getExecutionProperties() );
        }

        return evaluator;
    }

    /**
     * @param expression the user expression asked.
     * @throws MojoExecutionException if any
     * @throws MojoFailureException if any reflection exceptions occur or missing components.
     */
    private void handleResponse( String expression )
        throws MojoExecutionException, MojoFailureException
    {
        StringBuffer response = new StringBuffer();

        Object obj;
        try
        {
            obj = getEvaluator().evaluate( expression );
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new MojoExecutionException( "Error when evaluating the Maven expression", e );
        }

        if ( obj != null && expression.equals( obj.toString() ) )
        {
            getLog().warn( "The Maven expression was invalid. Please use a valid expression." );
            return;
        }

        // handle null
        if ( obj == null )
        {
            response.append( "null object or invalid expression" );
        }
        // handle primitives objects
        else if ( obj instanceof String )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Boolean )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Byte )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Character )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Double )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Float )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Integer )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Long )
        {
            response.append( obj.toString() );
        }
        else if ( obj instanceof Short )
        {
            response.append( obj.toString() );
        }
        // handle specific objects
        else if ( obj instanceof File )
        {
            File f = (File) obj;
            response.append( f.getAbsolutePath() );
        }
        // handle Maven pom object
        else if ( obj instanceof MavenProject )
        {
            MavenProject projectAsked = (MavenProject) obj;
            StringWriter sWriter = new StringWriter();
            MavenXpp3Writer pomWriter = new MavenXpp3Writer();
            try
            {
                pomWriter.write( sWriter, projectAsked.getModel() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error when writing pom", e );
            }

            response.append( sWriter.toString() );
        }
        // handle Maven Settings object
        else if ( obj instanceof Settings )
        {
            Settings settingsAsked = (Settings) obj;
            StringWriter sWriter = new StringWriter();
            SettingsXpp3Writer settingsWriter = new SettingsXpp3Writer();
            try
            {
                settingsWriter.write( sWriter, settingsAsked );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error when writing settings", e );
            }

            response.append( sWriter.toString() );
        }
        else
        {
            // others Maven objects
            response.append( toXML( expression, obj ) );
        }

        getLog().info( "\n" + response.toString() );
    }

    /**
     * @param expression the user expression.
     * @param obj a not null.
     * @return the XML for the given object.
     */
    private String toXML( String expression, Object obj )
    {
        XStream currentXStream = getXStream();

        // beautify list
        if ( obj instanceof List )
        {
            List list = (List) obj;
            if ( list.size() > 0 )
            {
                Object elt = list.iterator().next();

                String name = StringUtils.lowercaseFirstLetter( ClassUtils.getShortClassName( elt.getClass() ) );
                currentXStream.alias( pluralize( name ), List.class );
            }
            else
            {
                // try to detect the alias from question
                if ( expression.indexOf( "." ) != -1 )
                {
                    String name = expression.substring( expression.indexOf( "." ) + 1, expression.indexOf( "}" ) );
                    currentXStream.alias( name, List.class );
                }
            }
        }

        return currentXStream.toXML( obj );
    }

    /**
     * @return lazy loading xstream object.
     */
    private XStream getXStream()
    {
        if ( xstream == null )
        {
            xstream = new XStream();
            addAlias( xstream );

            // handle Properties a la Maven
            xstream.registerConverter( new PropertiesConverter()
            {
                /** {@inheritDoc} */
                public boolean canConvert( Class type )
                {
                    return Properties.class == type;
                }

                /** {@inheritDoc} */
                public void marshal( Object source, HierarchicalStreamWriter writer, MarshallingContext context )
                {
                    Properties properties = (Properties) source;
                    Map map = new TreeMap( properties ); // sort
                    for ( Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); )
                    {
                        Map.Entry entry = (Map.Entry) iterator.next();

                        writer.startNode( entry.getKey().toString() );
                        writer.setValue( entry.getValue().toString() );
                        writer.endNode();
                    }
                }
            } );
        }

        return xstream;
    }

    /**
     * @param xstreamObject not null
     */
    private void addAlias( XStream xstreamObject )
    {
        try
        {
            addAlias( xstreamObject, getMavenModelJarFile(), "org.apache.maven.model" );
            addAlias( xstreamObject, getMavenSettingsJarFile(), "org.apache.maven.settings" );
        }
        catch ( MojoExecutionException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "MojoExecutionException: " + e.getMessage(), e );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "ArtifactResolutionException: " + e.getMessage(), e );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "ArtifactNotFoundException: " + e.getMessage(), e );
            }
        }
        catch ( ProjectBuildingException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "ProjectBuildingException: " + e.getMessage(), e );
            }
        }

        // TODO need to handle specific Maven objects like DefaultArtifact?
    }

    /**
     * @param xstreamObject not null
     * @param jarFile not null
     * @param packageFilter a package name to filter.
     */
    private void addAlias( XStream xstreamObject, File jarFile, String packageFilter )
    {
        JarInputStream jarStream = null;
        try
        {
            jarStream = new JarInputStream( new FileInputStream( jarFile ) );
            JarEntry jarEntry = jarStream.getNextJarEntry();
            while ( jarEntry != null )
            {
                if ( jarEntry == null )
                {
                    break;
                }

                if ( jarEntry.getName().toLowerCase( Locale.ENGLISH ).endsWith( ".class" ) )
                {
                    String name = jarEntry.getName().substring( 0, jarEntry.getName().indexOf( "." ) );
                    name = name.replaceAll( "/", "\\." );

                    if ( name.indexOf( packageFilter ) != -1 )
                    {
                        try
                        {
                            Class clazz = ClassUtils.getClass( name );
                            String alias = StringUtils.lowercaseFirstLetter( ClassUtils.getShortClassName( clazz ) );
                            xstreamObject.alias( alias, clazz );
                            if ( !clazz.equals( Model.class ) )
                            {
                                xstreamObject.omitField( clazz, "modelEncoding" ); // unnecessary field
                            }
                        }
                        catch ( ClassNotFoundException e )
                        {
                            e.printStackTrace();
                        }
                    }
                }

                jarStream.closeEntry();
                jarEntry = jarStream.getNextJarEntry();
            }
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "IOException: " + e.getMessage(), e );
            }
        }
        finally
        {
            IOUtil.close( jarStream );
        }
    }

    /**
     * @return the <code>org.apache.maven:maven-model</code> artifact jar file in the local repository.
     * @throws MojoExecutionException if any
     * @throws ProjectBuildingException if any
     * @throws ArtifactResolutionException if any
     * @throws ArtifactNotFoundException if any
     */
    private File getMavenModelJarFile()
        throws MojoExecutionException, ProjectBuildingException, ArtifactResolutionException,
        ArtifactNotFoundException
    {
        return getArtifactFile( true );
    }

    /**
     * @return the <code>org.apache.maven:maven-settings</code> artifact jar file in the local repository.
     * @throws MojoExecutionException if any
     * @throws ProjectBuildingException if any
     * @throws ArtifactResolutionException if any
     * @throws ArtifactNotFoundException if any
     */
    private File getMavenSettingsJarFile()
        throws MojoExecutionException, ProjectBuildingException, ArtifactResolutionException,
        ArtifactNotFoundException
    {
        return getArtifactFile( false );
    }

    /**
     *
     * @param isPom <code>true</code> to lookup the <code>maven-model</code> artifact jar, <code>false</code> to
     * lookup the <code>maven-settings</code> artifact jar.
     * @return the <code>org.apache.maven:maven-model|maven-settings</code> artifact jar file for this current
     * HelpPlugin pom.
     * @throws MojoExecutionException if any
     * @throws ProjectBuildingException if any
     * @throws ArtifactResolutionException if any
     * @throws ArtifactNotFoundException if any
     */
    private File getArtifactFile( boolean isPom )
        throws MojoExecutionException, ProjectBuildingException, ArtifactResolutionException,
        ArtifactNotFoundException
    {
        for ( Iterator it = getHelpPluginPom().getDependencies().iterator(); it.hasNext(); )
        {
            Dependency depependency = (Dependency) it.next();

            if ( !( depependency.getGroupId().equals( "org.apache.maven" ) ) )
            {
                continue;
            }

            if ( isPom )
            {
                if ( !( depependency.getArtifactId().equals( "maven-model" ) ) )
                {
                    continue;
                }
            }
            else
            {
                if ( !( depependency.getArtifactId().equals( "maven-settings" ) ) )
                {
                    continue;
                }
            }

            Artifact mavenArtifact =
                getArtifact( depependency.getGroupId() + ":" + depependency.getArtifactId() + ":"
                    + depependency.getVersion() );
            resolver.resolveAlways( mavenArtifact, remoteRepositories, localRepository );

            return mavenArtifact.getFile();
        }

        throw new MojoExecutionException( "Unable to find the 'org.apache.maven:"
            + ( isPom ? "maven-model" : "maven-settings" ) + "' artifact" );
    }

    /**
     * @return the Maven POM for the current help plugin
     * @throws MojoExecutionException if any
     * @throws ProjectBuildingException if any
     */
    private MavenProject getHelpPluginPom()
        throws MojoExecutionException, ProjectBuildingException
    {
        String resource = "META-INF/maven/org.apache.maven.plugins/maven-help-plugin/pom.properties";

        InputStream resourceAsStream = EvaluateMojo.class.getClassLoader().getResourceAsStream( resource );
        Artifact helpPluginArtifact = null;
        if ( resourceAsStream != null )
        {
            Properties properties = new Properties();
            try
            {
                properties.load( resourceAsStream );
            }
            catch ( IOException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "IOException: " + e.getMessage(), e );
                }
            }

            String artifactString =
                properties.getProperty( "groupId", "unknown" ) + ":"
                    + properties.getProperty( "artifactId", "unknown" ) + ":"
                    + properties.getProperty( "version", "unknown" );

            helpPluginArtifact = getArtifact( artifactString );
        }

        if ( helpPluginArtifact == null )
        {
            throw new MojoExecutionException( "The help plugin artifact was not found." );
        }

        return getMavenProject( helpPluginArtifact );
    }

    /**
     * @param name not null
     * @return the plural of the name
     */
    private static String pluralize( String name )
    {
        if ( StringUtils.isEmpty( name ) )
        {
            throw new IllegalArgumentException( "name is required" );
        }

        if ( name.endsWith( "y" ) )
        {
            return name.substring( 0, name.length() - 1 ) + "ies";
        }
        else if ( name.endsWith( "s" ) )
        {
            return name;
        }
        else
        {
            return name + "s";
        }
    }
}
