package org.apache.maven.plugin.assembly;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.ArchiverStub;
import org.apache.maven.plugin.assembly.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Test common features of all assembly mojos.
 * 
 * @todo Switch to use test-only mojos, once we can generate descriptors for those...
 */
public class BasicAbstractAssemblyMojoFeaturesTest
    extends AbstractMojoTestCase
{
    
    public void testOutputFileNameMapping() throws Exception
    {
        String pluginConfig = "outputFileNameMapping-pluginConfig.xml";
        
        List requiredDependencies = new ArrayList();
        
        requiredDependencies.add( "dependencies/test.jar" );
        requiredDependencies.add( "dependencies/test2.jar" );
        
        testDependencyMapping( pluginConfig, requiredDependencies );
    }

    private void testDependencyMapping( String pluginConfig, List requiredDependencies ) throws Exception
    {
        String pluginConfigResource = "basicAbstractAssemblyMojoFeaturesTest/" + pluginConfig;
        
        File pluginConfigFile = new File( getBasedir(), "src/test/plugin-configs/" + pluginConfigResource );
        
        assertTrue( "Cannot find plugin-configuration: \'" + pluginConfigResource + "\' in context-classloader\'s classpath.", pluginConfigFile.exists() );
        
        // TODO: Need to replace this with test-only mojos...
        DirectoryMojo mojo = (DirectoryMojo) lookupMojo( "directory", pluginConfigFile.getAbsolutePath() );

        FileLoggingArchiverManagerStub archiverManager = (FileLoggingArchiverManagerStub) getVariableValueFromObject( mojo, "archiverManager" );
        archiverManager.clearArchiver();
        
        mojo.execute();

        FileLoggingArchiverStub archiver = (FileLoggingArchiverStub) archiverManager.getArchiver( null );
        
        Set addedFiles = archiver.getAddedFiles();
        
        System.out.println( "The following files were added to the test assembly:\n" + addedFiles.toString().replace(',', '\n' ) );
        
        for ( Iterator it = requiredDependencies.iterator(); it.hasNext(); )
        {
            String targetPath = (String) it.next();
            
            assertTrue( "Required dependency path missing: \'" + targetPath + "\'", addedFiles.contains( targetPath ) );
        }
    }

    public static final class FileLoggingArchiverManagerStub
        implements ArchiverManager
    {
        private FileLoggingArchiverStub archiverStub;

        public Archiver getArchiver( String string ) throws NoSuchArchiverException
        {
            if ( archiverStub == null )
            {
                archiverStub = new FileLoggingArchiverStub();
            }

            return archiverStub;
        }
        
        void clearArchiver()
        {
            archiverStub = null;
        }

        public UnArchiver getUnArchiver( String arg0 ) throws NoSuchArchiverException
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public static final class FileLoggingArchiverStub
        implements Archiver
    {

        private Set files = new LinkedHashSet();

        public void addFile( File file, String targetPath, int mode ) throws ArchiverException
        {
            files.add( targetPath );
        }

        public void addFile( File file, String targetPath ) throws ArchiverException
        {
            files.add( targetPath );
        }

        public Set getAddedFiles()
        {
            return files;
        }

        public void addDirectory( File arg0 ) throws ArchiverException
        {
            // TODO Auto-generated method stub
            
        }

        public void addDirectory( File arg0, String arg1 ) throws ArchiverException
        {
            // TODO Auto-generated method stub
            
        }

        public void addDirectory( File arg0, String[] arg1, String[] arg2 ) throws ArchiverException
        {
            // TODO Auto-generated method stub
            
        }

        public void addDirectory( File arg0, String arg1, String[] arg2, String[] arg3 ) throws ArchiverException
        {
            // TODO Auto-generated method stub
            
        }

        public void createArchive() throws ArchiverException, IOException
        {
            // TODO Auto-generated method stub
            
        }

        public int getDefaultDirectoryMode()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public int getDefaultFileMode()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public File getDestFile()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public Map getFiles()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean getIncludeEmptyDirs()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public void setDefaultDirectoryMode( int arg0 )
        {
            // TODO Auto-generated method stub
            
        }

        public void setDefaultFileMode( int arg0 )
        {
            // TODO Auto-generated method stub
            
        }

        public void setDestFile( File arg0 )
        {
            // TODO Auto-generated method stub
            
        }

        public void setIncludeEmptyDirs( boolean arg0 )
        {
            // TODO Auto-generated method stub
            
        }

    }

    public static final class TwoDependencyReactorProjectStub
        extends MavenProjectStub
    {
        private String groupId = "org.test.project";
        private String artifactId = "test-project";
        private String version = "1";
        private String packaging = "jar";
        private String scope = "compile";
        
        private String depOneArtifactId;
        private String depOneGroupId;
        private String depOneVersion;
        private String depOneType = "jar";
        private String depOneScope = "compile";
        
        private String depTwoArtifactId;
        private String depTwoGroupId;
        private String depTwoVersion;
        private String depTwoType = "jar";
        private String depTwoScope = "compile";
        
        public Set getArtifacts()
        {
            Set artifacts = new LinkedHashSet();
            
            artifacts.add( new HandlerEquippedArtifactStub( depOneGroupId, depOneArtifactId, depOneVersion, depOneType, depOneScope ) );
            artifacts.add( new HandlerEquippedArtifactStub( depTwoGroupId, depTwoArtifactId, depTwoVersion, depTwoType, depTwoScope ) );
            
            return artifacts;
        }
        
        public Artifact getArtifact()
        {
            return new HandlerEquippedArtifactStub( groupId, artifactId, version, packaging, scope );
        }
        
        public TwoDependencyReactorProjectStub()
        {
            Model model = getModel();
            if(  model == null )
            {
              model = new Model();
              setModel( model );
            }
            
            Properties props = model.getProperties();
            if ( props == null )
            {
                props = new Properties();
                model.setProperties( props );
            }
        }
    }
    
    public static final class HandlerEquippedArtifactStub extends ArtifactStub
    {

        private final String type;

        public HandlerEquippedArtifactStub( String groupId, String artifactId, String version, String type, String scope )
        {
            super( groupId, artifactId, version, type, scope );
            this.type = type;
        }

        public ArtifactHandler getArtifactHandler()
        {
            ArtifactHandler handler = new ArtifactHandler()
            {

                public String getClassifier()
                {
                    return null;
                }

                public String getDirectory()
                {
                    return null;
                }

                public String getExtension()
                {
                    return type;
                }

                public String getLanguage()
                {
                    return null;
                }

                public String getPackaging()
                {
                    return type;
                }

                public boolean isAddedToClasspath()
                {
                    return true;
                }

                public boolean isIncludesDependencies()
                {
                    return true;
                }
                
            };
            
            return handler;
        }
    }
}