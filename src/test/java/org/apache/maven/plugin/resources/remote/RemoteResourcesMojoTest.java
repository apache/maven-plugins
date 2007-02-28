package org.apache.maven.plugin.resources.remote;

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

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.resources.remote.stub.MavenProjectResourcesStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;


/**
 * RemoteResources plugin Test Case
 */

public class RemoteResourcesMojoTest
    extends AbstractMojoTestCase
{
    static final String DEFAULT_BUNDLE_POM_PATH = "target/test-classes/unit/rrmojotest/bundle-plugin-config.xml";
    static final String DEFAULT_PROCESS_POM_PATH = "target/test-classes/unit/rrmojotest/process-plugin-config.xml";

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {

    }

    /**
     * check test environment
     *
     * @throws Exception if any exception occurs
     */
    public void testTestEnvironment()
        throws Exception
    {
        // Perform lookup on the Mojo to make sure everything is ok
        lookupProcessMojo();
    }


    public void testNoBundles()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "default-nobundles" );
        final ProcessRemoteResourcesMojo mojo = lookupProcessMojoWithDefaultSettings( project );

        setupDefaultProject( project );

        mojo.execute();
    }

    public void testCreateBundle()
        throws Exception
    {
        buildResourceBundle( "default-createbundle",
                            new String[] { "SIMPLE.txt" },
                            null);
    }
    
    public void testSimpleBundles()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "default-simplebundles" );
        final ProcessRemoteResourcesMojo mojo = lookupProcessMojoWithSettings( project ,
                                                                        new String[] {
                                                                            "test:test:1.0"
                                                                        });

        setupDefaultProject( project );
        
        ArtifactRepository repo = (ArtifactRepository) getVariableValueFromObject( mojo, "localRepository" );
        String path = repo.pathOf(new DefaultArtifact( "test",
                                                        "test",
                                                        VersionRange.createFromVersion("1.0"),
                                                        null,
                                                        "jar",
                                                        "",
                                                        new DefaultArtifactHandler() ));
        
        File file = new File(repo.getBasedir() + "/" + path + ".jar");
        file.getParentFile().mkdirs();
        buildResourceBundle( "default-simplebundles-create",
                             new String[] { "SIMPLE.txt" },
                             file );
                             
        
        mojo.execute();
        
        file = (File) getVariableValueFromObject( mojo, "outputDirectory" );
        file = new File(file, "SIMPLE.txt"); 
        assertTrue(file.exists());
    }
        
    public void testFilteredBundles()
        throws Exception
    {
        final MavenProjectResourcesStub project = createTestProject( "default-filterbundles" );
        final ProcessRemoteResourcesMojo mojo = lookupProcessMojoWithSettings( project ,
                                                                        new String[] {
                                                                            "test:test:1.1"
                                                                        });
    
        setupDefaultProject( project );
        
        ArtifactRepository repo = (ArtifactRepository) getVariableValueFromObject( mojo, "localRepository" );
        String path = repo.pathOf(new DefaultArtifact( "test",
                                                        "test",
                                                        VersionRange.createFromVersion("1.1"),
                                                        null,
                                                        "jar",
                                                        "",
                                                        new DefaultArtifactHandler() ));
        
        File file = new File(repo.getBasedir() + "/" + path + ".jar");
        file.getParentFile().mkdirs();
        buildResourceBundle( "default-filterbundles-create",
                             new String[] { "FILTER.txt.vm" },
                             file );
                             
        
        mojo.execute();
        // executing a second time (example: forked lifecycle) should still work
        mojo.execute();
        
        file = (File) getVariableValueFromObject( mojo, "outputDirectory" );
        file = new File(file, "FILTER.txt"); 
        assertTrue(file.exists());
        
        String data = FileUtils.fileRead(file);
        assertTrue(data.indexOf("2007") != -1);
        assertTrue(data.indexOf("default-filterbundles") != -1);
    }

    protected void buildResourceBundle(String id,
                                       String resourceNames[],
                                       File jarName)
    throws Exception 
    {
        final MavenProjectResourcesStub project = createTestProject( id );

        final File resourceDir = new File(project.getBasedir() + "/src/main/resources");
        final BundleRemoteResourcesMojo mojo = lookupBundleMojoWithSettings( project , resourceDir );
        
        setupDefaultProject( project );
        
        for (int x = 0; x < resourceNames.length; x++)
        {
            File resource = new File(resourceDir, resourceNames[x]);
            URL source = getClass().getResource("/" + resourceNames[x]);
        
            FileUtils.copyURLToFile(source, resource);
        }
        
        mojo.execute();
        
        File xmlFile = new File(project.getBasedir() + "/target/classes/META-INF/maven/remote-resources.xml");
        assertTrue(xmlFile.exists());
        
        String data = FileUtils.fileRead(xmlFile);
        for (int x = 0; x < resourceNames.length; x++)
        {
            assertTrue(data.indexOf(resourceNames[x]) != -1);
        }
        
        if ( null != jarName)
        {
            JarOutputStream jar = new JarOutputStream( new FileOutputStream(jarName) );
            jar.putNextEntry(new ZipEntry("META-INF/maven/remote-resources.xml"));
            jar.write(data.getBytes());
            jar.closeEntry();

            for (int x = 0; x < resourceNames.length; x++)
            {
                File resource = new File(resourceDir, resourceNames[x]);
                data = FileUtils.fileRead(resource);
                jar.putNextEntry(new ZipEntry(resourceNames[x]));
                jar.write(data.getBytes());
                jar.closeEntry();
            }
            jar.close();
        }
    }
    
    

    protected MavenProjectResourcesStub createTestProject( final String testName )
    throws Exception
    {
        // this will automatically create the isolated
        // test environment
        return new MavenProjectResourcesStub( testName );
    }
    protected void setupDefaultProject( final MavenProjectResourcesStub project )
    throws Exception
    {
        // put this on the root dir
        project.addFile( "pom.xml", MavenProjectResourcesStub.ROOT_FILE );
        project.setInceptionYear("2007");
        // start creating the environment
        project.setupBuildEnvironment();
    }

    
    protected BundleRemoteResourcesMojo lookupBundleMojo()
    throws Exception
    {
        File pomFile = new File( getBasedir(), DEFAULT_BUNDLE_POM_PATH );
        BundleRemoteResourcesMojo mojo = (BundleRemoteResourcesMojo) lookupMojo( "bundle", pomFile );

        assertNotNull( mojo );
        
        return mojo;
    }
    protected BundleRemoteResourcesMojo lookupBundleMojoWithDefaultSettings( final MavenProject project )
        throws Exception
    {
        File resourceDir = new File(project.getBasedir() + "/src/main/resources");
        return lookupBundleMojoWithSettings(project, resourceDir);
    }
    protected BundleRemoteResourcesMojo lookupBundleMojoWithSettings( final MavenProject project,
                                                                      File resourceDir)
    throws Exception
    {
        final BundleRemoteResourcesMojo mojo = lookupBundleMojo();
    
        setVariableValueToObject( mojo, "resourcesDirectory", resourceDir );
        setVariableValueToObject( mojo, "outputDirectory", new File(project.getBuild().getOutputDirectory()) );
        return mojo;
    }

    protected ProcessRemoteResourcesMojo lookupProcessMojo()
        throws Exception
    {
        File pomFile = new File( getBasedir(), DEFAULT_PROCESS_POM_PATH );
        ProcessRemoteResourcesMojo mojo = (ProcessRemoteResourcesMojo) lookupMojo( "process", pomFile );

        assertNotNull( mojo );

        return mojo;
    }


    protected ProcessRemoteResourcesMojo lookupProcessMojoWithSettings( final MavenProject project,
                                                                 String bundles[])
        throws Exception
    {
        return lookupProcessMojoWithSettings( project, new ArrayList(Arrays.asList(bundles)));
    }
    
    protected ProcessRemoteResourcesMojo lookupProcessMojoWithSettings( final MavenProject project,
                                                                 ArrayList bundles)
        throws Exception
    {
        final ProcessRemoteResourcesMojo mojo = lookupProcessMojo();
        
        MavenSession session = new MavenSession( container,
                                    null, //Settings settings,
                                    null, //ArtifactRepository localRepository,
                                    null, //EventDispatcher eventDispatcher,
                                    null, //ReactorManager reactorManager, 
                                    Arrays.asList(new String[] {"install"}),
                                    project.getBasedir().toString(),
                                    new Properties(),
                                    Calendar.getInstance().getTime());

        
        setVariableValueToObject( mojo, "project", project );
        setVariableValueToObject( mojo, "outputDirectory", new File(project.getBuild().getOutputDirectory()) );
        setVariableValueToObject( mojo, "resourceBundles", bundles );
        setVariableValueToObject( mojo, "mavenSession", session );
        setVariableValueToObject( mojo, "remoteRepositories", project.getRemoteArtifactRepositories());
        setVariableValueToObject( mojo, "resources", project.getResources());
        return mojo;
    }

    protected ProcessRemoteResourcesMojo lookupProcessMojoWithDefaultSettings( final MavenProject project )
        throws Exception
    {
        return lookupProcessMojoWithSettings( project, new ArrayList() );
    }
}
