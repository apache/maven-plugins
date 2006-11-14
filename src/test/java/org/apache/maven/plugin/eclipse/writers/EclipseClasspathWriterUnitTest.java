package org.apache.maven.plugin.eclipse.writers;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.writers.testutils.TestEclipseWriterConfig;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class EclipseClasspathWriterUnitTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "EclipseClasspathWriter.unitTest.", "" );

    protected void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testWrite_ShouldMaskOutputDirsNestedWithinAnExistingOutputDir()
        throws MojoExecutionException, JDOMException, IOException
    {
        TestEclipseWriterConfig config = new TestEclipseWriterConfig();

        File basedir = fileManager.createTempDir();

        config.setProjectBaseDir( basedir );
        config.setEclipseProjectDirectory( basedir );

        String baseOutputDir = "target/classes";
        String maskedOutputDir = "target/classes/main-resources";

        File buildOutputDir = new File( basedir, baseOutputDir );
        buildOutputDir.mkdirs();

        config.setBuildOutputDirectory( buildOutputDir );

        new File( basedir, maskedOutputDir ).mkdirs();

        EclipseSourceDir dir = new EclipseSourceDir( "src/main/resources", "target/classes", true, false, null, null,
                                                     false );
        EclipseSourceDir testDir = new EclipseSourceDir( "src/test/resources", "target/classes/test-resources", true,
                                                         true, null, null, false );

        EclipseSourceDir[] dirs = { dir, testDir };

        config.setSourceDirs( dirs );
        
        config.setEclipseProjectName( "test-project" );

        TestLog log = new TestLog();

        EclipseClasspathWriter classpathWriter = new EclipseClasspathWriter();
        classpathWriter.init( log, config );
        classpathWriter.write();
        
        SAXBuilder builder = new SAXBuilder( false );

        Document doc = builder.build( new File( basedir, ".classpath" ) );

        XPath resourcePath = XPath.newInstance( "//classpathentry[@path='src/main/resources']" );
        
        assertTrue( "resources classpath entry not found.", resourcePath.selectSingleNode( doc ) != null );
        
        XPath testResourcePath = XPath.newInstance( "//classpathentry[@path='src/test/resources']" );
        
        assertTrue( "test resources (minus custom output dir) classpath entry not found.", testResourcePath.selectSingleNode( doc ) != null );
        
        XPath stdOutputPath = XPath.newInstance( "//classpathentry[@kind='output' && @path='target/classes']" );
        
        assertTrue( "standard output classpath entry not found.", stdOutputPath.selectSingleNode( doc ) != null );

    }
    
    private static final class TestLog extends SystemStreamLog
    {
        public boolean isDebugEnabled()
        {
            return true;
        }
    }

}
