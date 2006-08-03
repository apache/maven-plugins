package org.apache.maven.plugin.assembly.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import junit.framework.TestCase;

public class FileFormatterTest
    extends TestCase
{

    private Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

    private TestFileManager fileManager = new TestFileManager( "fileFormatterTest.", "" );

    private MockManager mockManager = new MockManager();

    private AssemblerConfigurationSource configSource;

    private MockControl configSourceControl;

    public void setUp()
    {
        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldNotTransformOneFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        //        Model model = new Model();
        //        model.setArtifactId( "artifact" );
        //        model.setGroupId( "group" );
        //        model.setVersion( "version" );
        //        
        //        MavenProject project = new MavenProject( model );
        //        
        //        configSource.getProject();
        //        configSourceControl.setReturnValue( project );

        File file = fileManager.createFile( basedir, "one.txt", "This is a test." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, null );

        assertEquals( file, result );

        mockManager.verifyAll();
    }

    // TODO: Should not be appending line-ending at the end if there is none in the source.
    public void testShouldConvertCRLFLineEndingsInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        File file = fileManager.createFile( basedir, "one.txt", "This is a\ntest." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, "dos" );

        assertEquals( "This is a\r\ntest.\r\n", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    // TODO: Should not be appending line-ending at the end if there is none in the source.
    public void testShouldConvertLFLineEndingsInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        File file = fileManager.createFile( basedir, "one.txt", "This is a\r\ntest." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, false, "unix" );

        assertEquals( "This is a\ntest.\n", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterProjectExpressionInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        enableBasicFilteringConfiguration( basedir, Collections.EMPTY_LIST );

        File file = fileManager.createFile( basedir, "one.txt", "This is a test for project: ${artifactId}." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "This is a test for project: artifact.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionFromFiltersFileInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );

        enableBasicFilteringConfiguration( basedir, Collections.singletonList( filterProps.getCanonicalPath() ) );

        File file = fileManager.createFile( basedir, "one.txt", "This is a test for project: ${property}." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "This is a test for project: Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldFilterExpressionsFromTwoFiltersFilesInFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );
        File filterProps2 = fileManager.createFile( basedir, "filter2.properties", "otherProperty=OtherValue" );

        List filters = new ArrayList();
        filters.add( filterProps.getCanonicalPath() );
        filters.add( filterProps2.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        File file = fileManager.createFile( basedir, "one.txt",
                                            "property: ${property}  otherProperty: ${otherProperty}." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "property: Test  otherProperty: OtherValue.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldOverrideOneFilterValueWithAnotherAndFilterFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "property=Test" );
        File filterProps2 = fileManager.createFile( basedir, "filter2.properties", "property=OtherValue" );

        List filters = new ArrayList();
        filters.add( filterProps.getCanonicalPath() );
        filters.add( filterProps2.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        File file = fileManager.createFile( basedir, "one.txt", "property: ${property}." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "property: OtherValue.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    public void testShouldOverrideProjectValueWithFilterValueAndFilterFile()
        throws IOException, AssemblyFormattingException
    {
        File basedir = fileManager.createTempDir();

        File filterProps = fileManager.createFile( basedir, "filter.properties", "artifactId=Test" );

        List filters = new ArrayList();
        filters.add( filterProps.getCanonicalPath() );

        enableBasicFilteringConfiguration( basedir, filters );

        File file = fileManager.createFile( basedir, "one.txt", "project artifact-id: ${artifactId}." );

        mockManager.replayAll();

        File result = new FileFormatter( configSource, logger ).format( file, true, null );

        assertEquals( "project artifact-id: Test.", fileManager.getFileContents( result ) );

        mockManager.verifyAll();
    }

    private void enableBasicFilteringConfiguration( File basedir, List filterFilenames )
    {
        configSource.getTemporaryRootDirectory();
        configSourceControl.setReturnValue( basedir );

        Model model = new Model();
        model.setArtifactId( "artifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        MavenProject project = new MavenProject( model );

        configSource.getProject();
        configSourceControl.setReturnValue( project, MockControl.ONE_OR_MORE );

        // list of filenames that contain filter definitions.
        configSource.getFilters();
        configSourceControl.setReturnValue( filterFilenames );
    }

}
