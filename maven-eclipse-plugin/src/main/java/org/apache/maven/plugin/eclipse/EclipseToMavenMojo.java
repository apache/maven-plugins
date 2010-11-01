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
package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.eclipse.osgiplugin.EclipseOsgiPlugin;
import org.apache.maven.plugin.eclipse.osgiplugin.ExplodedPlugin;
import org.apache.maven.plugin.eclipse.osgiplugin.PackagedPlugin;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import aQute.lib.osgi.Analyzer;

/**
 * Add eclipse artifacts from an eclipse installation to the local repo. This mojo automatically analize the eclipse
 * directory, copy plugins jars to the local maven repo, and generates appropriate poms. This is the official central
 * repository builder for Eclipse plugins, so it has the necessary default values. For customized repositories see
 * {@link MakeArtifactsMojo} Typical usage:
 * <code>mvn eclipse:to-maven -DdeployTo=maven.org::default::scpexe://repo1.maven.org/home/maven/repository-staging/to-ibiblio/eclipse-staging -DeclipseDir=.</code>
 * 
 * @author Fabrizio Giustina
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @goal to-maven
 * @requiresProject false
 */
public class EclipseToMavenMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * A pattern the <code>deployTo</code> param should match.
     */
    private static final Pattern DEPLOYTO_PATTERN = Pattern.compile( "(.+)::(.+)::(.+)" ); //$NON-NLS-1$

    /**
     * A pattern for a 4 digit osgi version number.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile( "(([0-9]+\\.)+[0-9]+)" ); //$NON-NLS-1$

    /**
     * Plexus container, needed to manually lookup components for deploy of artifacts.
     */
    private PlexusContainer container;

    /**
     * Local maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * ArtifactRepositoryFactory component.
     * 
     * @component
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * ArtifactFactory component.
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * ArtifactInstaller component.
     * 
     * @component
     */
    protected ArtifactInstaller installer;

    /**
     * ArtifactDeployer component.
     * 
     * @component
     */
    private ArtifactDeployer deployer;

    /**
     * Eclipse installation dir. If not set, a value for this parameter will be asked on the command line.
     * 
     * @parameter expression="${eclipseDir}"
     */
    private File eclipseDir;

    /**
     * Input handler, needed for comand line handling.
     * 
     * @component
     */
    protected InputHandler inputHandler;

    /**
     * Strip qualifier (fourth token) from the plugin version. Qualifiers are for eclipse plugin the equivalent of
     * timestamped snapshot versions for Maven, but the date is maintained also for released version (e.g. a jar for the
     * release <code>3.2</code> can be named <code>org.eclipse.core.filesystem_1.0.0.v20060603.jar</code>. It's usually
     * handy to not to include this qualifier when generating maven artifacts for major releases, while it's needed when
     * working with eclipse integration/nightly builds.
     * 
     * @parameter expression="${stripQualifier}" default-value="false"
     */
    private boolean stripQualifier;

    /**
     * Specifies a remote repository to which generated artifacts should be deployed to. If this property is specified,
     * artifacts are also deployed to the remote repo. The format for this parameter is <code>id::layout::url</code>
     * 
     * @parameter expression="${deployTo}"
     */
    private String deployTo;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( eclipseDir == null )
        {
            getLog().info( Messages.getString( "EclipseToMavenMojo.eclipseDirectoryPrompt" ) ); //$NON-NLS-1$

            String eclipseDirString;
            try
            {
                eclipseDirString = inputHandler.readLine();
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( Messages.getString( "EclipseToMavenMojo.errorreadingfromstandardinput" ) ); //$NON-NLS-1$
            }
            eclipseDir = new File( eclipseDirString );
        }

        if ( !eclipseDir.isDirectory() )
        {
            throw new MojoFailureException(
                                            Messages.getString(
                                                                "EclipseToMavenMojo.directoydoesnotexist", eclipseDir.getAbsolutePath() ) ); //$NON-NLS-1$
        }

        File pluginDir = new File( eclipseDir, "plugins" ); //$NON-NLS-1$

        if ( !pluginDir.isDirectory() )
        {
            throw new MojoFailureException(
                                            Messages.getString(
                                                                "EclipseToMavenMojo.plugindirectorydoesnotexist", pluginDir.getAbsolutePath() ) ); //$NON-NLS-1$
        }

        File[] files = pluginDir.listFiles();

        ArtifactRepository remoteRepo = resolveRemoteRepo();

        if ( remoteRepo != null )
        {
            getLog().info( Messages.getString( "EclipseToMavenMojo.remoterepositorydeployto", deployTo ) ); //$NON-NLS-1$
        }

        Map plugins = new HashMap();
        Map models = new HashMap();

        for ( int j = 0; j < files.length; j++ )
        {
            File file = files[j];

            getLog().info( Messages.getString( "EclipseToMavenMojo.processingfile", file.getAbsolutePath() ) ); //$NON-NLS-1$

            processFile( file, plugins, models );
        }

        int i = 1;
        for ( Iterator it = plugins.keySet().iterator(); it.hasNext(); )
        {
            getLog().info(
                           Messages.getString(
                                               "EclipseToMavenMojo.processingplugin", new Object[] { new Integer( i++ ), new Integer( plugins.keySet().size() ) } ) ); //$NON-NLS-1$
            String key = (String) it.next();
            EclipseOsgiPlugin plugin = (EclipseOsgiPlugin) plugins.get( key );
            Model model = (Model) models.get( key );
            writeArtifact( plugin, model, remoteRepo );
        }
    }

    protected void processFile( File file, Map plugins, Map models )
        throws MojoExecutionException, MojoFailureException
    {
        EclipseOsgiPlugin plugin = getEclipsePlugin( file );

        if ( plugin == null )
        {
            getLog().warn( Messages.getString( "EclipseToMavenMojo.skippingfile", file.getAbsolutePath() ) ); //$NON-NLS-1$
            return;
        }

        Model model = createModel( plugin );

        if ( model == null )
        {
            return;
        }

        processPlugin( plugin, model, plugins, models );
    }

    protected void processPlugin( EclipseOsgiPlugin plugin, Model model, Map plugins, Map models )
        throws MojoExecutionException, MojoFailureException
    {
        plugins.put( getKey( model ), plugin );
        models.put( getKey( model ), model );
    }

    protected String getKey( Model model )
    {
        return model.getGroupId() + "." + model.getArtifactId(); //$NON-NLS-1$
    }

    private String getKey( Dependency dependency )
    {
        return dependency.getGroupId() + "." + dependency.getArtifactId(); //$NON-NLS-1$
    }

    /**
     * Resolve version ranges in the model provided, overriding version ranges with versions from the dependency in the
     * provided map of models. TODO doesn't check if the version is in range, it just overwrites it
     * 
     * @param model
     * @param models
     * @throws MojoFailureException
     */
    protected void resolveVersionRanges( Model model, Map models )
        throws MojoFailureException
    {
        for ( Iterator it = model.getDependencies().iterator(); it.hasNext(); )
        {
            Dependency dep = (Dependency) it.next();
            if ( dep.getVersion().indexOf( "[" ) > -1 || dep.getVersion().indexOf( "(" ) > -1 ) //$NON-NLS-1$ //$NON-NLS-2$
            {
                String key = getKey( model );
                Model dependencyModel = (Model) models.get( getKey( dep ) );
                if ( dependencyModel != null )
                {
                    dep.setVersion( dependencyModel.getVersion() );
                }
                else
                {
                    throw new MojoFailureException(
                                                    Messages.getString(
                                                                        "EclipseToMavenMojo.unabletoresolveversionrange", new Object[] { dep //$NON-NLS-1$
                                                                            , key } ) ); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Get a {@link EclipseOsgiPlugin} object from a plugin jar/dir found in the target dir.
     * 
     * @param file plugin jar or dir
     * @throws MojoExecutionException if anything bad happens while parsing files
     */
    private EclipseOsgiPlugin getEclipsePlugin( File file )
        throws MojoExecutionException
    {
        if ( file.isDirectory() )
        {
            return new ExplodedPlugin( file );
        }
        else if ( file.getName().endsWith( ".jar" ) ) //$NON-NLS-1$
        {
            try
            {
                return new PackagedPlugin( file );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                                                  Messages.getString(
                                                                      "EclipseToMavenMojo.unabletoaccessjar", file.getAbsolutePath() ), e ); //$NON-NLS-1$
            }
        }

        return null;
    }

    /**
     * Create the {@link Model} from a plugin manifest
     * 
     * @param plugin Eclipse plugin jar or dir
     * @throws MojoExecutionException if anything bad happens while parsing files
     */
    private Model createModel( EclipseOsgiPlugin plugin )
        throws MojoExecutionException
    {

        String name, bundleName, version, groupId, artifactId, requireBundle;

        try
        {
            if ( !plugin.hasManifest() )
            {
                getLog().warn( Messages.getString( "EclipseToMavenMojo.plugindoesnothavemanifest", plugin ) ); //$NON-NLS-1$
                return null;
            }

            Analyzer analyzer = new Analyzer();

            Map bundleSymbolicNameHeader =
                analyzer.parseHeader( plugin.getManifestAttribute( Analyzer.BUNDLE_SYMBOLICNAME ) );
            bundleName = (String) bundleSymbolicNameHeader.keySet().iterator().next();
            version = plugin.getManifestAttribute( Analyzer.BUNDLE_VERSION );

            if ( bundleName == null || version == null )
            {
                getLog().error( Messages.getString( "EclipseToMavenMojo.unabletoreadbundlefrommanifest" ) ); //$NON-NLS-1$
                return null;
            }

            version = osgiVersionToMavenVersion( version );

            name = plugin.getManifestAttribute( Analyzer.BUNDLE_NAME );

            requireBundle = plugin.getManifestAttribute( Analyzer.REQUIRE_BUNDLE );

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                                              Messages.getString( "EclipseToMavenMojo.errorprocessingplugin", plugin ), e ); //$NON-NLS-1$
        }

        Dependency[] deps = parseDependencies( requireBundle );

        groupId = createGroupId( bundleName );
        artifactId = createArtifactId( bundleName );

        Model model = new Model();
        model.setModelVersion( "4.0.0" ); //$NON-NLS-1$
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setName( name );
        model.setVersion( version );

        model.setProperties( plugin.getPomProperties() );

        if ( groupId.startsWith( "org.eclipse" ) ) //$NON-NLS-1$
        {
            // why do we need a parent?

            // Parent parent = new Parent();
            // parent.setGroupId( "org.eclipse" );
            // parent.setArtifactId( "eclipse" );
            // parent.setVersion( "1" );
            // model.setParent( parent );

            // infer license for know projects, everything at eclipse is licensed under EPL
            // maybe too simplicistic, but better than nothing
            License license = new License();
            license.setName( "Eclipse Public License - v 1.0" ); //$NON-NLS-1$
            license.setUrl( "http://www.eclipse.org/org/documents/epl-v10.html" ); //$NON-NLS-1$
            model.addLicense( license );
        }

        if ( deps.length > 0 )
        {
            for ( int k = 0; k < deps.length; k++ )
            {
                model.getDependencies().add( deps[k] );
            }

        }

        return model;
    }

    /**
     * Writes the artifact to the repo
     * 
     * @param model
     * @param remoteRepo remote repository (if set)
     * @throws MojoExecutionException
     */
    private void writeArtifact( EclipseOsgiPlugin plugin, Model model, ArtifactRepository remoteRepo )
        throws MojoExecutionException
    {
        Writer fw = null;
        ArtifactMetadata metadata = null;
        File pomFile = null;
        Artifact pomArtifact =
            artifactFactory.createArtifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), null, "pom" ); //$NON-NLS-1$
        Artifact artifact =
            artifactFactory.createArtifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), null,
                                            Constants.PROJECT_PACKAGING_JAR );
        try
        {
            pomFile = File.createTempFile( "pom-", ".xml" ); //$NON-NLS-1$ //$NON-NLS-2$

            // TODO use WriterFactory.newXmlWriter() when plexus-utils is upgraded to 1.4.5+
            fw = new OutputStreamWriter( new FileOutputStream( pomFile ), "UTF-8" ); //$NON-NLS-1$
            model.setModelEncoding( "UTF-8" ); // to be removed when encoding is detected instead of forced to UTF-8 //$NON-NLS-1$
            pomFile.deleteOnExit();
            new MavenXpp3Writer().write( fw, model );
            metadata = new ProjectArtifactMetadata( pomArtifact, pomFile );
            pomArtifact.addMetadata( metadata );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                                              Messages.getString(
                                                                  "EclipseToMavenMojo.errorwritingtemporarypom", e.getMessage() ), e ); //$NON-NLS-1$
        }
        finally
        {
            IOUtil.close( fw );
        }

        try
        {
            File jarFile = plugin.getJarFile();

            if ( remoteRepo != null )
            {
                deployer.deploy( pomFile, pomArtifact, remoteRepo, localRepository );
                deployer.deploy( jarFile, artifact, remoteRepo, localRepository );
            }
            else
            {
                installer.install( pomFile, pomArtifact, localRepository );
                installer.install( jarFile, artifact, localRepository );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            throw new MojoExecutionException(
                                              Messages.getString( "EclipseToMavenMojo.errordeployartifacttorepository" ), e ); //$NON-NLS-1$
        }
        catch ( ArtifactInstallationException e )
        {
            throw new MojoExecutionException(
                                              Messages.getString( "EclipseToMavenMojo.errorinstallartifacttorepository" ), e ); //$NON-NLS-1$
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                                              Messages.getString(
                                                                  "EclipseToMavenMojo.errorgettingjarfileforplugin", plugin ), e ); //$NON-NLS-1$
        }
        finally
        {
            pomFile.delete();
        }

    }

    protected String osgiVersionToMavenVersion( String version )
    {
        return osgiVersionToMavenVersion( version, null, stripQualifier );
    }

    /**
     * The 4th (build) token MUST be separed with "-" and not with "." in maven. A version with 4 dots is not parsed,
     * and the whole string is considered a qualifier. See tests in DefaultArtifactVersion for reference.
     * 
     * @param version initial version
     * @param forcedQualifier build number
     * @param stripQualifier always remove 4th token in version
     * @return converted version
     */
    protected String osgiVersionToMavenVersion( String version, String forcedQualifier, boolean stripQualifier )
    {
        if ( stripQualifier && StringUtils.countMatches( version, "." ) > 2 ) //$NON-NLS-1$
        {
            version = StringUtils.substring( version, 0, version.lastIndexOf( "." ) ); //$NON-NLS-1$
        }
        else if ( StringUtils.countMatches( version, "." ) > 2 ) //$NON-NLS-1$
        {
            int lastDot = version.lastIndexOf( "." ); //$NON-NLS-1$
            if ( StringUtils.isNotEmpty( forcedQualifier ) )
            {
                version = StringUtils.substring( version, 0, lastDot ) + "-" + forcedQualifier; //$NON-NLS-1$
            }
            else
            {
                version = StringUtils.substring( version, 0, lastDot ) + "-" //$NON-NLS-1$
                    + StringUtils.substring( version, lastDot + 1, version.length() );
            }
        }
        return version;
    }

    /**
     * Resolves the deploy<code>deployTo</code> parameter to an <code>ArtifactRepository</code> instance (if set).
     * 
     * @throws MojoFailureException
     * @throws MojoExecutionException
     * @return ArtifactRepository instance of null if <code>deployTo</code> is not set.
     */
    private ArtifactRepository resolveRemoteRepo()
        throws MojoFailureException, MojoExecutionException
    {
        if ( deployTo != null )
        {
            Matcher matcher = DEPLOYTO_PATTERN.matcher( deployTo );

            if ( !matcher.matches() )
            {
                throw new MojoFailureException( deployTo,
                                                Messages.getString( "EclipseToMavenMojo.invalidsyntaxforrepository" ), //$NON-NLS-1$
                                                Messages.getString( "EclipseToMavenMojo.invalidremoterepositorysyntax" ) ); //$NON-NLS-1$
            }
            else
            {
                String id = matcher.group( 1 ).trim();
                String layout = matcher.group( 2 ).trim();
                String url = matcher.group( 3 ).trim();

                ArtifactRepositoryLayout repoLayout;
                try
                {
                    repoLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE, layout );
                }
                catch ( ComponentLookupException e )
                {
                    throw new MojoExecutionException(
                                                      Messages.getString(
                                                                          "EclipseToMavenMojo.cannotfindrepositorylayout", layout ), e ); //$NON-NLS-1$
                }

                return artifactRepositoryFactory.createDeploymentArtifactRepository( id, url, repoLayout, true );
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Get the group id as the tokens until last dot e.g. <code>org.eclipse.jdt</code> -> <code>org.eclipse</code>
     * 
     * @param bundleName bundle name
     * @return group id
     */
    protected String createGroupId( String bundleName )
    {
        int i = bundleName.lastIndexOf( "." ); //$NON-NLS-1$
        if ( i > 0 )
        {
            return bundleName.substring( 0, i );
        }
        else
            return bundleName;
    }

    /**
     * Get the artifact id as the tokens after last dot e.g. <code>org.eclipse.jdt</code> -> <code>jdt</code>
     * 
     * @param bundleName bundle name
     * @return artifact id
     */
    protected String createArtifactId( String bundleName )
    {
        int i = bundleName.lastIndexOf( "." ); //$NON-NLS-1$
        if ( i > 0 )
        {
            return bundleName.substring( i + 1 );
        }
        else
            return bundleName;
    }

    /**
     * Parses the "Require-Bundle" and convert it to a list of dependencies.
     * 
     * @param requireBundle "Require-Bundle" entry
     * @return an array of <code>Dependency</code>
     */
    protected Dependency[] parseDependencies( String requireBundle )
    {
        if ( requireBundle == null )
        {
            return new Dependency[0];
        }

        List dependencies = new ArrayList();

        Analyzer analyzer = new Analyzer();

        Map requireBundleHeader = analyzer.parseHeader( requireBundle );

        // now iterates on bundles and extract dependencies
        for ( Iterator iter = requireBundleHeader.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            String bundleName = (String) entry.getKey();
            Map attributes = (Map) entry.getValue();

            String version = (String) attributes.get( Analyzer.BUNDLE_VERSION.toLowerCase() );
            boolean optional = "optional".equals( attributes.get( "resolution:" ) ); //$NON-NLS-1$ //$NON-NLS-2$

            if ( version == null )
            {
                getLog().info( Messages.getString( "EclipseToMavenMojo.missingversionforbundle", bundleName ) ); //$NON-NLS-1$
                version = "[0,)"; //$NON-NLS-1$
            }

            version = fixBuildNumberSeparator( version );

            Dependency dep = new Dependency();
            dep.setGroupId( createGroupId( bundleName ) );
            dep.setArtifactId( createArtifactId( bundleName ) );
            dep.setVersion( version );
            dep.setOptional( optional );

            dependencies.add( dep );

        }

        return (Dependency[]) dependencies.toArray( new Dependency[dependencies.size()] );

    }

    /**
     * Fix the separator for the 4th token in a versions. In maven this must be "-", in OSGI it's "."
     * 
     * @param versionRange input range
     * @return modified version range
     */
    protected String fixBuildNumberSeparator( String versionRange )
    {
        // should not be called with a null versionRange, but a check doesn't hurt...
        if ( versionRange == null )
        {
            return null;
        }

        StringBuffer newVersionRange = new StringBuffer();

        Matcher matcher = VERSION_PATTERN.matcher( versionRange );

        while ( matcher.find() )
        {
            String group = matcher.group();

            if ( StringUtils.countMatches( group, "." ) > 2 ) //$NON-NLS-1$
            {
                // build number found, fix it
                int lastDot = group.lastIndexOf( "." ); //$NON-NLS-1$
                group = StringUtils.substring( group, 0, lastDot ) + "-" //$NON-NLS-1$
                    + StringUtils.substring( group, lastDot + 1, group.length() );
            }
            matcher.appendReplacement( newVersionRange, group );
        }

        matcher.appendTail( newVersionRange );

        return newVersionRange.toString();
    }

}
