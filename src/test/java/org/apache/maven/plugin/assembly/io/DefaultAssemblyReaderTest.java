package org.apache.maven.plugin.assembly.io;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Component;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.apache.maven.plugin.assembly.model.io.xpp3.ComponentXpp3Writer;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.easymock.MockControl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class DefaultAssemblyReaderTest
    extends TestCase
{

    private TestFileManager fileManager;

    private MockManager mockManager;

    private MockControl configSourceControl;

    private AssemblerConfigurationSource configSource;

    public void setUp()
    {
        fileManager = new TestFileManager( "assembly-reader.test.", ".xml" );
        mockManager = new MockManager();

        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();

        MockControl localRepoControl = MockControl.createControl( ArtifactRepository.class );
        ArtifactRepository localRepo = (ArtifactRepository) localRepoControl.getMock();

        mockManager.add( localRepoControl );

        configSource.getLocalRepository();
        configSourceControl.setReturnValue( localRepo, MockControl.ZERO_OR_MORE );

        configSource.getRemoteRepositories();
        configSourceControl.setReturnValue( Collections.EMPTY_LIST, MockControl.ZERO_OR_MORE );
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testIncludeSiteInAssembly_ShouldFailIfSiteDirectoryNonExistent()
        throws IOException
    {
        File siteDir = File.createTempFile( "assembly-reader.", ".test" );
        siteDir.delete();

        configSource.getSiteDirectory();
        configSourceControl.setReturnValue( siteDir, MockControl.ZERO_OR_MORE );

        Assembly assembly = new Assembly();

        mockManager.replayAll();

        try
        {
            new DefaultAssemblyReader().includeSiteInAssembly( assembly, configSource );

            fail( "Should fail when site directory is non-existent." );
        }
        catch ( InvalidAssemblerConfigurationException e )
        {
            // this should happen.
        }

        mockManager.verifyAll();
    }

    public void testIncludeSiteInAssembly_ShouldAddSiteDirFileSetWhenDirExists()
        throws IOException, InvalidAssemblerConfigurationException
    {
        File siteDir = fileManager.createTempDir();

        configSource.getSiteDirectory();
        configSourceControl.setReturnValue( siteDir, MockControl.ZERO_OR_MORE );

        Assembly assembly = new Assembly();

        mockManager.replayAll();

        new DefaultAssemblyReader().includeSiteInAssembly( assembly, configSource );

        List fileSets = assembly.getFileSets();

        assertNotNull( fileSets );
        assertEquals( 1, fileSets.size() );

        FileSet fs = (FileSet) fileSets.get( 0 );

        assertEquals( siteDir.getPath(), fs.getDirectory() );

        mockManager.verifyAll();
    }

//    public void testReadComponent_ShouldReadComponentFromXml()
//        throws IOException, AssemblyReadException
//    {
//        Component component = new Component();
//
//        FileSet fileSet = new FileSet();
//        fileSet.setDirectory( "/dir" );
//
//        component.addFileSet( fileSet );
//
//        StringWriter sw = new StringWriter();
//
//        ComponentXpp3Writer componentWriter = new ComponentXpp3Writer();
//
//        componentWriter.write( sw, component );
//
//        Component result = new DefaultAssemblyReader().readComponent( new StringReader( sw.toString() ) );
//
//        List fileSets = result.getFileSets();
//
//        assertNotNull( fileSets );
//        assertEquals( 1, fileSets.size() );
//
//        FileSet fs = (FileSet) fileSets.get( 0 );
//
//        assertEquals( "/dir", fs.getDirectory() );
//    }
//
//    public void testGetComponentFromFile_ShouldReadComponent()
//        throws IOException, AssemblyReadException
//    {
//        Component component = new Component();
//
//        FileSet fileSet = new FileSet();
//        fileSet.setDirectory( "/dir" );
//
//        component.addFileSet( fileSet );
//
//        File componentFile = fileManager.createTempFile();
//
//        FileWriter writer = null;
//
//        try
//        {
//            writer = new FileWriter( componentFile );
//
//            ComponentXpp3Writer componentWriter = new ComponentXpp3Writer();
//
//            componentWriter.write( writer, component );
//        }
//        finally
//        {
//            IOUtil.close( writer );
//        }
//
//        File basedir = componentFile.getParentFile();
//        String filename = componentFile.getName();
//
//        configSource.getBasedir();
//        configSourceControl.setReturnValue( basedir );
//
//        mockManager.replayAll();
//
//        Component result = new DefaultAssemblyReader().getComponentFromFile( filename, configSource );
//
//        List fileSets = result.getFileSets();
//
//        assertNotNull( fileSets );
//        assertEquals( 1, fileSets.size() );
//
//        FileSet fs = (FileSet) fileSets.get( 0 );
//
//        assertEquals( "/dir", fs.getDirectory() );
//
//        mockManager.verifyAll();
//    }

    public void testMergeComponentWithAssembly_ShouldAddOneFileSetToExistingListOfTwo()
    {
        Assembly assembly = new Assembly();

        FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        assembly.addFileSet( fs );

        fs = new FileSet();
        fs.setDirectory( "/other-dir" );
        assembly.addFileSet( fs );

        fs = new FileSet();
        fs.setDirectory( "/third-dir" );

        Component component = new Component();

        component.addFileSet( fs );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        List fileSets = assembly.getFileSets();

        assertNotNull( fileSets );
        assertEquals( 3, fileSets.size() );

        FileSet rfs1 = (FileSet) fileSets.get( 0 );
        assertEquals( "/dir", rfs1.getDirectory() );

        FileSet rfs2 = (FileSet) fileSets.get( 1 );
        assertEquals( "/other-dir", rfs2.getDirectory() );

        FileSet rfs3 = (FileSet) fileSets.get( 2 );
        assertEquals( "/third-dir", rfs3.getDirectory() );

    }

    public void testMergeComponentWithAssembly_ShouldAddOneFileItemToExistingListOfTwo()
    {
        Assembly assembly = new Assembly();

        FileItem fi = new FileItem();
        fi.setSource( "file" );

        assembly.addFile( fi );

        fi = new FileItem();
        fi.setSource( "file2" );

        assembly.addFile( fi );

        fi = new FileItem();
        fi.setSource( "file3" );

        Component component = new Component();

        component.addFile( fi );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        List fileItems = assembly.getFiles();

        assertNotNull( fileItems );
        assertEquals( 3, fileItems.size() );

        FileItem rf1 = (FileItem) fileItems.get( 0 );
        assertEquals( "file", rf1.getSource() );

        FileItem rf2 = (FileItem) fileItems.get( 1 );
        assertEquals( "file2", rf2.getSource() );

        FileItem rf3 = (FileItem) fileItems.get( 2 );
        assertEquals( "file3", rf3.getSource() );

    }

    public void testMergeComponentWithAssembly_ShouldAddOneDependencySetToExistingListOfTwo()
    {
        Assembly assembly = new Assembly();

        DependencySet ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_RUNTIME );

        assembly.addDependencySet( ds );

        ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_COMPILE );

        assembly.addDependencySet( ds );

        Component component = new Component();

        ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_SYSTEM );

        component.addDependencySet( ds );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        List depSets = assembly.getDependencySets();

        assertNotNull( depSets );
        assertEquals( 3, depSets.size() );

        assertEquals( Artifact.SCOPE_RUNTIME, ( (DependencySet) depSets.get( 0 ) ).getScope() );
        assertEquals( Artifact.SCOPE_COMPILE, ( (DependencySet) depSets.get( 1 ) ).getScope() );
        assertEquals( Artifact.SCOPE_SYSTEM, ( (DependencySet) depSets.get( 2 ) ).getScope() );
    }

    public void testMergeComponentWithAssembly_ShouldAddOneRepositoryToExistingListOfTwo()
    {
        Assembly assembly = new Assembly();

        Repository repo = new Repository();
        repo.setScope( Artifact.SCOPE_RUNTIME );

        assembly.addRepository( repo );

        repo = new Repository();
        repo.setScope( Artifact.SCOPE_COMPILE );

        assembly.addRepository( repo );

        Component component = new Component();

        repo = new Repository();
        repo.setScope( Artifact.SCOPE_SYSTEM );

        component.addRepository( repo );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        List depSets = assembly.getRepositories();

        assertNotNull( depSets );
        assertEquals( 3, depSets.size() );

        assertEquals( Artifact.SCOPE_RUNTIME, ( (Repository) depSets.get( 0 ) ).getScope() );
        assertEquals( Artifact.SCOPE_COMPILE, ( (Repository) depSets.get( 1 ) ).getScope() );
        assertEquals( Artifact.SCOPE_SYSTEM, ( (Repository) depSets.get( 2 ) ).getScope() );
    }

    public void testMergeComponentWithAssembly_ShouldAddOneContainerDescriptorHandlerToExistingListOfTwo()
    {
        Assembly assembly = new Assembly();

        ContainerDescriptorHandlerConfig cfg = new ContainerDescriptorHandlerConfig();
        cfg.setHandlerName( "one" );

        assembly.addContainerDescriptorHandler( cfg );

        cfg = new ContainerDescriptorHandlerConfig();
        cfg.setHandlerName( "two" );

        assembly.addContainerDescriptorHandler( cfg );

        Component component = new Component();

        cfg = new ContainerDescriptorHandlerConfig();
        cfg.setHandlerName( "three" );

        component.addContainerDescriptorHandler( cfg );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        List result = assembly.getContainerDescriptorHandlers();

        assertNotNull( result );
        assertEquals( 3, result.size() );

        Iterator it = result.iterator();
        assertEquals( "one", ( (ContainerDescriptorHandlerConfig) it.next() ).getHandlerName() );
        assertEquals( "two", ( (ContainerDescriptorHandlerConfig) it.next() ).getHandlerName() );
        assertEquals( "three", ( (ContainerDescriptorHandlerConfig) it.next() ).getHandlerName() );
    }

    // FIXME: Deep merging should take place...
    // public void
    // testMergeComponentWithAssembly_ShouldMergeOneFileSetToOneOfExistingTwo()
    // {
    // Assembly assembly = new Assembly();
    //
    // FileSet fs = new FileSet();
    // fs.setDirectory( "/dir" );
    // fs.addInclude( "**/test.txt" );
    //
    // assembly.addFileSet( fs );
    //
    // fs = new FileSet();
    // fs.setDirectory( "/other-dir" );
    // assembly.addFileSet( fs );
    //
    // fs = new FileSet();
    // fs.setDirectory( "/dir" );
    // fs.addInclude( "**/components.txt" );
    //
    // Component component = new Component();
    //
    // component.addFileSet( fs );
    //
    // new DefaultAssemblyReader().mergeComponentWithAssembly( component,
    // assembly );
    //
    // List fileSets = assembly.getFileSets();
    //
    // assertNotNull( fileSets );
    // assertEquals( 2, fileSets.size() );
    //
    // FileSet rfs1 = (FileSet) fileSets.get( 0 );
    // assertEquals( "/dir", rfs1.getDirectory() );
    //
    // List includes = rfs1.getIncludes();
    //
    // assertNotNull( includes );
    // assertEquals( 2, includes.size() );
    // assertTrue( includes.contains( "**/test.txt" ) );
    // assertTrue( includes.contains( "**/components.txt" ) );
    //
    // FileSet rfs2 = (FileSet) fileSets.get( 1 );
    // assertEquals( "/other-dir", rfs2.getDirectory() );
    //
    // }

    public void testMergeComponentsWithMainAssembly_ShouldAddOneFileSetToAssembly()
        throws IOException, AssemblyReadException
    {
        Component component = new Component();

        FileSet fileSet = new FileSet();
        fileSet.setDirectory( "/dir" );

        component.addFileSet( fileSet );

        File componentFile = fileManager.createTempFile();

        Writer writer = null;

        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( componentFile ), "UTF-8" );

            ComponentXpp3Writer componentWriter = new ComponentXpp3Writer();

            componentWriter.write( writer, component );
            writer.flush();
        }
        finally
        {
            IOUtil.close( writer );
        }

        String filename = componentFile.getName();

        Assembly assembly = new Assembly();
        assembly.addComponentDescriptor( filename );

        File basedir = componentFile.getParentFile();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        new DefaultAssemblyReader().mergeComponentsWithMainAssembly( assembly, configSource );

        List fileSets = assembly.getFileSets();

        assertNotNull( fileSets );
        assertEquals( 1, fileSets.size() );

        FileSet fs = (FileSet) fileSets.get( 0 );

        assertEquals( "/dir", fs.getDirectory() );

        mockManager.verifyAll();
    }

    public void testReadAssembly_ShouldReadAssemblyWithoutComponentsInterpolationOrSiteDirInclusion()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        StringWriter sw = new StringWriter();
        AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        StringReader sr = new StringReader( sw.toString() );

        File basedir = fileManager.createTempDir();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().readAssembly( sr,
                                                                    "testLocation",
                                                                    configSource );

        assertEquals( assembly.getId(), result.getId() );

        mockManager.verifyAll();
    }

    public void testReadAssembly_ShouldReadAssemblyWithSiteDirInclusionFromAssemblyWithoutComponentsOrInterpolation()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        assembly.setIncludeSiteDirectory( true );

        StringWriter sw = new StringWriter();
        AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        StringReader sr = new StringReader( sw.toString() );

        File siteDir = fileManager.createTempDir();

        configSource.getSiteDirectory();
        configSourceControl.setReturnValue( siteDir, MockControl.ZERO_OR_MORE );

        File basedir = fileManager.createTempDir();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().readAssembly( sr,
                                                                    "testLocation",
                                                                    configSource );

        assertEquals( assembly.getId(), result.getId() );

        List fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "/site", ( (FileSet) fileSets.get( 0 ) ).getOutputDirectory() );

        mockManager.verifyAll();
    }

    public void testReadAssembly_ShouldReadAssemblyWithSiteDirInclusionFromConfigWithoutComponentsOrInterpolation()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        StringWriter sw = new StringWriter();
        AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        StringReader sr = new StringReader( sw.toString() );

        File siteDir = fileManager.createTempDir();

        configSource.getSiteDirectory();
        configSourceControl.setReturnValue( siteDir, MockControl.ZERO_OR_MORE );

        File basedir = fileManager.createTempDir();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( true, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().readAssembly( sr,
                                                                    "testLocation",
                                                                    configSource );

        assertEquals( assembly.getId(), result.getId() );

        List fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "/site", ( (FileSet) fileSets.get( 0 ) ).getOutputDirectory() );

        mockManager.verifyAll();
    }

    public void testReadAssembly_ShouldReadAssemblyWithComponentWithoutSiteDirInclusionOrInterpolation()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        File componentsFile = fileManager.createTempFile();

        File basedir = componentsFile.getParentFile();
        String componentsFilename = componentsFile.getName();

        Component component = new Component();

        FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        component.addFileSet( fs );

        Writer fw = null;

        try
        {
            fw = new OutputStreamWriter( new FileOutputStream( componentsFile ), "UTF-8" );
            new ComponentXpp3Writer().write( fw, component );
        }
        finally
        {
            IOUtil.close( fw );
        }

        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        assembly.addComponentDescriptor( componentsFilename );

        StringWriter sw = new StringWriter();
        AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        StringReader sr = new StringReader( sw.toString() );

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().readAssembly( sr,
                                                                    "testLocation",
                                                                    configSource );

        assertEquals( assembly.getId(), result.getId() );

        List fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "/dir", ( (FileSet) fileSets.get( 0 ) ).getDirectory() );

        mockManager.verifyAll();
    }

    public void testReadAssembly_ShouldReadAssemblyWithComponentInterpolationWithoutSiteDirInclusionOrAssemblyInterpolation()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        File componentsFile = fileManager.createTempFile();

        File basedir = componentsFile.getParentFile();
        String componentsFilename = componentsFile.getName();

        Component component = new Component();

        FileSet fs = new FileSet();
        fs.setDirectory( "${groupId}-dir" );

        component.addFileSet( fs );

        Writer fw = null;

        try
        {
            fw = new OutputStreamWriter( new FileOutputStream( componentsFile ), "UTF-8" );
            new ComponentXpp3Writer().write( fw, component );
        }
        finally
        {
            IOUtil.close( fw );
        }

        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        assembly.addComponentDescriptor( componentsFilename );

        StringWriter sw = new StringWriter();
        AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        StringReader sr = new StringReader( sw.toString() );

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ONE_OR_MORE );

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().readAssembly( sr,
                                                                    "testLocation",
                                                                    configSource );

        assertEquals( assembly.getId(), result.getId() );

        List fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "group-dir", ( (FileSet) fileSets.get( 0 ) ).getDirectory() );

        mockManager.verifyAll();
    }

    public void testReadAssembly_ShouldReadAssemblyWithInterpolationWithoutComponentsOrSiteDirInclusion()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setId( "${groupId}-assembly" );

        StringWriter sw = new StringWriter();
        AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        StringReader sr = new StringReader( sw.toString() );

        File basedir = fileManager.createTempDir();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().readAssembly( sr,
                                                                    "testLocation",
                                                                    configSource );

        assertEquals( "group-assembly", result.getId() );

        mockManager.verifyAll();
    }

    public void testGetAssemblyFromDescriptorFile_ShouldReadAssembly()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        assembly.addFileSet( fs );

        File assemblyFile = fileManager.createTempFile();

        File basedir = assemblyFile.getParentFile();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        configSource.getProject();
        configSourceControl.setReturnValue( new MavenProject( new Model() ),
                                            MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( assemblyFile ), "UTF-8" );
            new AssemblyXpp3Writer().write( writer, assembly );
        }
        finally
        {
            IOUtil.close( writer );
        }

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().getAssemblyFromDescriptorFile( assemblyFile,
                                                                                     configSource );

        assertEquals( assembly.getId(), result.getId() );

        mockManager.verifyAll();
    }

    public void testGetAssemblyForDescriptorReference_ShouldReadBinaryAssemblyRef()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        File basedir = fileManager.createTempDir();

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        configSource.getProject();
        configSourceControl.setReturnValue( new MavenProject( new Model() ),
                                            MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        Assembly result = new DefaultAssemblyReader().getAssemblyForDescriptorReference( "bin",
                                                                                         configSource );

        assertEquals( "bin", result.getId() );

        mockManager.verifyAll();
    }

    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromSingleFile()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        assembly.setId( "test" );

        FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        assembly.addFileSet( fs );

        File basedir = fileManager.createTempDir();

        List files = writeAssembliesToFile( Collections.singletonList( assembly ), basedir );

        String assemblyFile = (String) files.get( 0 );

        List assemblies = performReadAssemblies( basedir, assemblyFile, null, null, null, null );

        assertNotNull( assemblies );
        assertEquals( 1, assemblies.size() );

        Assembly result = (Assembly) assemblies.get( 0 );

        assertEquals( assembly.getId(), result.getId() );
    }

    public void testReadAssemblies_ShouldFailWhenSingleDescriptorFileMissing()
        throws IOException, InvalidAssemblerConfigurationException
    {
        File basedir = fileManager.createTempDir();

        File assemblyFile = new File( basedir, "test.xml" );
        assemblyFile.delete();

        try
        {
            performReadAssemblies( basedir,
                                   assemblyFile.getAbsolutePath(),
                                   null,
                                   null,
                                   null,
                                   null,
                                   false );

            fail( "Should fail when descriptor file is missing and ignoreDescriptors == false" );
        }
        catch ( AssemblyReadException e )
        {
            // expected.
        }
    }

    public void testReadAssemblies_ShouldIgnoreMissingSingleDescriptorFileWhenIgnoreIsConfigured()
        throws IOException, InvalidAssemblerConfigurationException
    {
        File basedir = fileManager.createTempDir();

        File assemblyFile = new File( basedir, "test.xml" );
        assemblyFile.delete();

        try
        {
            performReadAssemblies( basedir,
                                   assemblyFile.getAbsolutePath(),
                                   null,
                                   null,
                                   null,
                                   null,
                                   true );
        }
        catch ( AssemblyReadException e )
        {
            e.printStackTrace();
            fail( "Setting ignoreMissingDescriptor == true (true flag in performReadAssemblies, above) should NOT produce an exception." );
        }
    }

    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromSingleRef()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        File basedir = fileManager.createTempDir();

        List assemblies = performReadAssemblies( basedir, null, "bin", null, null, null );

        assertNotNull( assemblies );
        assertEquals( 1, assemblies.size() );

        Assembly result = (Assembly) assemblies.get( 0 );

        assertEquals( "bin", result.getId() );
    }

    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromFileArray()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly1 = new Assembly();
        assembly1.setId( "test" );

        Assembly assembly2 = new Assembly();
        assembly2.setId( "test2" );

        List assemblies = new ArrayList();
        assemblies.add( assembly1 );
        assemblies.add( assembly2 );

        File basedir = fileManager.createTempDir();

        List files = writeAssembliesToFile( assemblies, basedir );

        List results = performReadAssemblies( basedir,
                                              null,
                                              null,
                                              (String[]) files.toArray( new String[0] ),
                                              null,
                                              null );

        assertNotNull( results );
        assertEquals( 2, results.size() );

        Assembly result1 = (Assembly) assemblies.get( 0 );

        assertEquals( assembly1.getId(), result1.getId() );

        Assembly result2 = (Assembly) assemblies.get( 1 );

        assertEquals( assembly2.getId(), result2.getId() );
    }

    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromMultipleRefs()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        File basedir = fileManager.createTempDir();

        List assemblies = performReadAssemblies( basedir, null, null, null, new String[] {
            "bin",
            "src"
        }, null );

        assertNotNull( assemblies );
        assertEquals( 2, assemblies.size() );

        Assembly result = (Assembly) assemblies.get( 0 );

        assertEquals( "bin", result.getId() );

        Assembly result2 = (Assembly) assemblies.get( 1 );

        assertEquals( "src", result2.getId() );
    }

    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromDirectory()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly1 = new Assembly();
        assembly1.setId( "test" );

        Assembly assembly2 = new Assembly();
        assembly2.setId( "test2" );

        List assemblies = new ArrayList();
        assemblies.add( assembly1 );
        assemblies.add( assembly2 );

        File basedir = fileManager.createTempDir();

        writeAssembliesToFile( assemblies, basedir );

        List results = performReadAssemblies( basedir, null, null, null, null, basedir );

        assertNotNull( results );
        assertEquals( 2, results.size() );

        Assembly result1 = (Assembly) assemblies.get( 0 );

        assertEquals( assembly1.getId(), result1.getId() );

        Assembly result2 = (Assembly) assemblies.get( 1 );

        assertEquals( assembly2.getId(), result2.getId() );
    }

    public void testReadAssemblies_ShouldGetTwoAssemblyDescriptorsFromDirectoryWithThreeFiles()
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly1 = new Assembly();
        assembly1.setId( "test" );

        Assembly assembly2 = new Assembly();
        assembly2.setId( "test2" );

        List assemblies = new ArrayList();
        assemblies.add( assembly1 );
        assemblies.add( assembly2 );

        File basedir = fileManager.createTempDir();

        writeAssembliesToFile( assemblies, basedir );

        fileManager.createFile( basedir,
                                "readme.txt",
                                "This is just a readme file, not a descriptor." );

        List results = performReadAssemblies( basedir, null, null, null, null, basedir );

        assertNotNull( results );
        assertEquals( 2, results.size() );

        Assembly result1 = (Assembly) assemblies.get( 0 );

        assertEquals( assembly1.getId(), result1.getId() );

        Assembly result2 = (Assembly) assemblies.get( 1 );

        assertEquals( assembly2.getId(), result2.getId() );
    }

    private List writeAssembliesToFile( List assemblies,
                                        File dir )
        throws IOException
    {
        List files = new ArrayList();

        for ( Iterator it = assemblies.iterator(); it.hasNext(); )
        {
            Assembly assembly = (Assembly) it.next();

            File assemblyFile = new File( dir, assembly.getId() + ".xml" );

            Writer writer = null;
            try
            {
                writer = new OutputStreamWriter( new FileOutputStream( assemblyFile ), "UTF-8" );
                new AssemblyXpp3Writer().write( writer, assembly );
            }
            finally
            {
                IOUtil.close( writer );
            }

            files.add( assemblyFile.getAbsolutePath() );
        }

        return files;
    }

    private List performReadAssemblies( File basedir,
                                        String descriptor,
                                        String descriptorRef,
                                        String[] descriptors,
                                        String[] descriptorRefs,
                                        File descriptorDir )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        return performReadAssemblies( basedir,
                                      descriptor,
                                      descriptorRef,
                                      descriptors,
                                      descriptorRefs,
                                      descriptorDir,
                                      false );
    }

    private List performReadAssemblies( File basedir,
                                        String descriptor,
                                        String descriptorRef,
                                        String[] descriptors,
                                        String[] descriptorRefs,
                                        File descriptorDir,
                                        boolean ignoreMissing )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        configSource.getDescriptor();
        configSourceControl.setReturnValue( descriptor );

        configSource.getDescriptorId();
        configSourceControl.setReturnValue( descriptorRef );

        configSource.getDescriptorReferences();
        configSourceControl.setReturnValue( descriptorRefs );

        configSource.getDescriptors();
        configSourceControl.setReturnValue( descriptors );

        configSource.getDescriptorSourceDirectory();
        configSourceControl.setReturnValue( descriptorDir );

        configSource.getBasedir();
        configSourceControl.setReturnValue( basedir, MockControl.ZERO_OR_MORE );

        configSource.getProject();
        configSourceControl.setReturnValue( new MavenProject( new Model() ),
                                            MockControl.ZERO_OR_MORE );

        configSource.isSiteIncluded();
        configSourceControl.setReturnValue( false, MockControl.ZERO_OR_MORE );

        configSource.isIgnoreMissingDescriptor();
        configSourceControl.setReturnValue( ignoreMissing, MockControl.ZERO_OR_MORE );

        mockManager.replayAll();

        List assemblies = new DefaultAssemblyReader().readAssemblies( configSource );

        mockManager.verifyAll();

        return assemblies;
    }

}
