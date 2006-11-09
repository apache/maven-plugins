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
package org.apache.maven.plugin.eclipse;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 */
public abstract class AbstractEclipsePluginTestCase
    extends PlexusTestCase
{

    private static Invoker mavenInvoker = new DefaultInvoker();

    /**
     * The embedder.
     */
    //    protected MavenEmbedder maven;
    private ArtifactRepository localRepository;

    private MavenProjectBuilder projectBuilder;

    /**
     * Test repository directory.
     */
    protected static final File LOCAL_REPO_DIR = getTestFile( "target/test-classes/m2repo" );

    /**
     * Group-Id for running test builds.
     */
    protected static final String GROUP_ID = "org.apache.maven.plugins";

    /**
     * Artifact-Id for running test builds.
     */
    protected static final String ARTIFACT_ID = "maven-eclipse-plugin";

    /**
     * Version under which the plugin was installed to the test-time local repository for running 
     * test builds.
     */
    protected static final String VERSION = "test";
    
    private static String mvnHome;

    private static boolean installed = false;

    /**
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        synchronized( AbstractEclipsePluginTestCase.class )
        {
            if ( mvnHome == null )
            {
                mvnHome = System.getProperty( "maven.home" );
                
                if ( mvnHome == null )
                {
                    Properties envVars = CommandLineUtils.getSystemEnvVars();
                    
                    mvnHome = envVars.getProperty( "M2_HOME" );
                }
            }
            
            if ( mvnHome != null )
            {
                mavenInvoker.setMavenHome( new File( mvnHome ) );
            }
        }
        
        //        this.maven = new MavenEmbedder();
        //        this.maven.setClassLoader( Thread.currentThread().getContextClassLoader() );
        //        this.maven.setLogger( new MavenEmbedderConsoleLogger() );
        //        this.maven.setLocalRepositoryDirectory( LOCAL_REPO_DIR );
        //        this.maven.setOffline( true );
        //        this.maven.setInteractiveMode( false );
        //        this.maven.start();

        super.setUp();

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        createLocalArtifactRepositoryInstance();

        // We need to call super.setup() first, to ensure that we can use the PlexusContainer 
        // initialized in the parent class.
        installPluginInTestLocalRepository();
    }

    protected void createLocalArtifactRepositoryInstance()
        throws Exception
    {
        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        ArtifactRepositoryLayout defaultLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                    "default" );

        localRepository = repoFactory.createArtifactRepository( "local", LOCAL_REPO_DIR.toURL().toExternalForm(),
                                                                defaultLayout, null, null );

    }

    protected void installPluginInTestLocalRepository()
        throws Exception
    {
        // synchronizing just in case we try to parallelize later...
        synchronized ( AbstractEclipsePluginTestCase.class )
        {
            if ( !installed )
            {
                System.out.println( "\n\n\n\n*** Installing test-version of the Eclipse plugin to: " + LOCAL_REPO_DIR + "***\n\n\n\n" );

                ArtifactInstaller installer = (ArtifactInstaller) lookup( ArtifactInstaller.ROLE );
                ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

                Artifact artifact = factory.createArtifact( GROUP_ID, ARTIFACT_ID, VERSION, null, "maven-plugin" );

                File pomFile = manglePom();
                File artifactFile = new File( "target/" + ARTIFACT_ID + "-" + VERSION + ".jar" );
                
                artifact.addMetadata( new ProjectArtifactMetadata( artifact, pomFile ) );

                Properties properties = new Properties();
//                properties.setProperty( "maven.test.skip", "true" );
                
                List goals = new ArrayList();
                goals.add( "package" );
                
                executeMaven( pomFile, properties, goals, false );
                
                artifact.setFile( artifactFile );

                String localPath = localRepository.pathOf( artifact );

                File destination = new File( localRepository.getBasedir(), localPath );
                if ( !destination.getParentFile().exists() )
                {
                    destination.getParentFile().mkdirs();
                }

                System.out.println( "Installing " + artifactFile.getPath() + " to " + destination );

                installer.install( artifactFile, artifact, localRepository );

                installLocalParentSnapshotPoms( pomFile, installer, factory, localRepository );

                installed = true;
            }
        }
    }

    private File manglePom() throws IOException, XmlPullParserException
    {
        File input = new File( "pom.xml" );
        
        File output = new File( "pom-test.xml" );
        output.deleteOnExit();
        
        FileReader reader = null;
        FileWriter writer = null;
        
        try
        {
            reader = new FileReader( input );
            writer = new FileWriter( output );
            
            Model model = new MavenXpp3Reader().read( reader );
            
            model.setVersion( "test" );
            
            Build build = model.getBuild();
            if ( build == null )
            {
                build = new Build();
                model.setBuild( build );
            }
            
            List plugins = build.getPlugins();
            Plugin plugin = null;
            for ( Iterator iter = plugins.iterator(); iter.hasNext(); )
            {
                Plugin plug = (Plugin) iter.next();
                
                if ( "maven-surefire-plugin".equals( plug.getArtifactId() ) )
                {
                    plugin = plug;
                    break;
                }
            }
            
            if ( plugin == null )
            {
                plugin = new Plugin();
                plugin.setArtifactId( "maven-surefire-plugin" );
                build.addPlugin( plugin );
            }
            
            Xpp3Dom configDom = (Xpp3Dom) plugin.getConfiguration();
            if ( configDom == null )
            {
                configDom = new Xpp3Dom( "configuration" );
                plugin.setConfiguration( configDom );
            }
            
            Xpp3Dom skipDom = new Xpp3Dom( "skip" );
            skipDom.setValue( "true" );
            
            configDom.addChild( skipDom );
            
            new MavenXpp3Writer().write( writer, model );
        }
        finally
        {
            IOUtil.close( reader );
            IOUtil.close( writer );
        }
        
        return output;
    }

    private void installLocalParentSnapshotPoms( File pomFile, ArtifactInstaller installer, ArtifactFactory factory,
                                                 ArtifactRepository localRepo )
        throws IOException, XmlPullParserException, ArtifactInstallationException
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();

        File pom = pomFile;

        boolean firstPass = true;

        while ( pom != null )
        {

            if ( !pom.exists() )
            {
                pom = null;
                break;
            }

            String pomGroupId = null;
            String pomArtifactId = null;
            String pomVersion = null;

            FileReader reader = null;

            File currentPom = pom;

            try
            {
                reader = new FileReader( pom );

                Model model = pomReader.read( reader );

                pomGroupId = model.getGroupId();
                pomArtifactId = model.getArtifactId();
                pomVersion = model.getVersion();

                Parent parent = model.getParent();
                if ( parent != null )
                {
                    pom = new File( pom.getParentFile(), parent.getRelativePath() );
                }
                else
                {
                    pom = null;
                }
            }
            finally
            {
                IOUtil.close( reader );
            }

            if ( !firstPass )
            {
                Artifact pomArtifact = factory.createProjectArtifact( pomGroupId, pomArtifactId, pomVersion );
                pomArtifact.addMetadata( new ProjectArtifactMetadata( pomArtifact, currentPom ) );

                installer.install( currentPom, pomArtifact, localRepo );
            }
            else
            {
                firstPass = false;
            }
        }
    }

    /**
     * @see org.codehaus.plexus.PlexusTestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        //        maven.stop();
        //        super.tearDown();
        //
        //        Field embedderField = maven.getClass().getDeclaredField( "embedder" );
        //        embedderField.setAccessible( true );
        //        Embedder embedder = (Embedder) embedderField.get( maven );

        List containers = new ArrayList();

        //        containers.add( embedder.getContainer() );
        containers.add( getContainer() );

        for ( Iterator iter = containers.iterator(); iter.hasNext(); )
        {
            PlexusContainer container = (PlexusContainer) iter.next();

            if ( container != null )
            {
                container.dispose();

                ClassRealm realm = container.getContainerRealm();

                if ( realm != null )
                {
                    realm.getWorld().disposeRealm( realm.getId() );
                }
            }
        }
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName )
        throws Exception
    {
        testProject( projectName, new Properties(), "clean", "eclipse" );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @param properties additional properties
     * @throws Exception any exception generated during test
     * @deprecated Use {@link #testProject(String,Properties,String,String)} instead
     */
    protected void testProject( String projectName, Properties properties )
        throws Exception
    {
        testProject( projectName, properties, "clean", "eclipse" );
    }

    /**
     * Execute the eclipse:eclipse goal on a test project and verify generated files.
     * @param projectName project directory
     * @param properties additional properties
     * @param cleanGoal TODO
     * @param genGoal TODO
     * @throws Exception any exception generated during test
     */
    protected void testProject( String projectName, Properties properties, String cleanGoal, String genGoal )
        throws Exception
    {
        File basedir = getTestFile( "target/test-classes/projects/" + projectName );

        File pom = new File( basedir, "pom.xml" );
        
        String pluginSpec = getPluginCLISpecification();

        List goals = new ArrayList();

        goals.add( pluginSpec + cleanGoal );
        goals.add( pluginSpec + genGoal );
        
        executeMaven( pom, properties, goals );

        MavenProject project = readProject( pom );

        String outputDirPath = IdeUtils.getPluginSetting( project, "maven-eclipse-plugin", "outputDir", null );
        File outputDir;
        File projectOutputDir = basedir;

        if ( outputDirPath == null )
        {
            outputDir = basedir;
        }
        else
        {
            outputDir = new File( basedir, outputDirPath );
            outputDir.mkdirs();
            projectOutputDir = new File( outputDir, project.getArtifactId() );
        }

        compareDirectoryContent( basedir, projectOutputDir, "" );
        compareDirectoryContent( basedir, projectOutputDir, ".settings/" );
        compareDirectoryContent( basedir, projectOutputDir, "META-INF/" );

    }

    protected void executeMaven( File pom, Properties properties, List goals )
        throws MavenInvocationException
    {
        executeMaven( pom, properties, goals, true );
    }
    
    protected void executeMaven( File pom, Properties properties, List goals, boolean switchLocalRepo )
        throws MavenInvocationException
    {
        InvocationRequest request = new DefaultInvocationRequest();
        
//        request.setDebug( true );
        
        if ( switchLocalRepo )
        {
            request.setLocalRepositoryDirectory( LOCAL_REPO_DIR );
        }
        
        request.setPomFile( pom );

        request.setGoals( goals );

        request.setProperties( properties );

        mavenInvoker.execute( request );
    }

    protected MavenProject readProject( File pom )
        throws ProjectBuildingException
    {
        return projectBuilder.build( pom, localRepository, null );
    }

    protected String getPluginCLISpecification()
    {
        String pluginSpec = GROUP_ID + ":" + ARTIFACT_ID + ":";

        //        String pluginVersion = System.getProperty( "pluginVersion" );
        //        
        //        if ( pluginVersion != null )
        //        {
        //            pluginSpec += pluginVersion + ":";
        //        }
        //
        //        System.out.println( "\n\nUsing Eclipse plugin version: " + pluginVersion + "\n\n" );

        // try using the test-version installed during setUp()
        pluginSpec += VERSION + ":";

        return pluginSpec;
    }

    /**
     * @param basedir
     * @param projectOutputDir
     * @throws IOException
     */
    private void compareDirectoryContent( File basedir, File projectOutputDir, String additionalDir )
        throws IOException
    {
        File expectedConfigDir = new File( basedir, "expected/" + additionalDir );

        if ( expectedConfigDir.isDirectory() )
        {
            File[] files = expectedConfigDir.listFiles( new FileFilter()
            {
                public boolean accept( File file )
                {
                    return !file.isDirectory();
                }
            } );

            for ( int j = 0; j < files.length; j++ )
            {
                File file = files[j];

                assertFileEquals( LOCAL_REPO_DIR.getCanonicalPath(), file, new File( projectOutputDir, additionalDir
                    + file.getName() ) );

            }
        }
    }

    protected void assertFileEquals( String mavenRepo, File expectedFile, File actualFile )
        throws IOException
    {
        List expectedLines = getLines( mavenRepo, expectedFile );
        List actualLines = getLines( mavenRepo, actualFile );
        String filename = actualFile.getName();

        String basedir = new File( getBasedir() ).getCanonicalPath().replace( '\\', '/' );

        for ( int i = 0; i < expectedLines.size(); i++ )
        {
            String expected = expectedLines.get( i ).toString();

            // replace some vars in the expected line, to account
            // for absolute paths that are different on each installation.
            expected = StringUtils.replace( expected, "${basedir}", basedir );
            expected = StringUtils.replace( expected, "${M2_REPO}", LOCAL_REPO_DIR.getCanonicalPath().replace( '\\',
                                                                                                               '/' ) );

            if ( actualLines.size() <= i )
            {
                fail( "Too few lines in the actual file. Was " + actualLines.size() + ", expected: "
                    + expectedLines.size() );
            }

            String actual = actualLines.get( i ).toString();

            if ( expected.startsWith( "#" ) && actual.startsWith( "#" ) )
            {
                //ignore comments, for settings file
                continue;
            }

            assertEquals( "Checking " + filename + ", line #" + ( i + 1 ), expected, actual );
        }

        assertTrue( "Unequal number of lines.", expectedLines.size() == actualLines.size() );
    }

    protected void assertContains( String message, String full, String substring )
    {
        if ( full == null || full.indexOf( substring ) == -1 )
        {
            StringBuffer buf = new StringBuffer();
            if ( message != null )
            {
                buf.append( message );
            }
            buf.append( ". " );
            buf.append( "Expected \"" );
            buf.append( substring );
            buf.append( "\" not found" );
            fail( buf.toString() );
        }
    }

    protected void assertDoesNotContain( String message, String full, String substring )
    {
        if ( full == null || full.indexOf( substring ) != -1 )
        {
            StringBuffer buf = new StringBuffer();
            if ( message != null )
            {
                buf.append( message );
            }
            buf.append( ". " );
            buf.append( "Unexpected \"" );
            buf.append( substring );
            buf.append( "\" found" );
            fail( buf.toString() );
        }
    }

    private List getLines( String mavenRepo, File file )
        throws IOException
    {
        List lines = new ArrayList();

        BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( file ), "UTF-8" ) );

        String line;

        while ( ( line = reader.readLine() ) != null )
        {
            lines.add( line );
        }

        IOUtil.close( reader );

        return lines;
    }
}
