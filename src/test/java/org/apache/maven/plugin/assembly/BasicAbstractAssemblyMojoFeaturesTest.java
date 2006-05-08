package org.apache.maven.plugin.assembly;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.assembly.stubs.ArchiverManagerStub;
import org.apache.maven.plugin.assembly.stubs.ArchiverStub;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
        
        Mojo mojo = run( pluginConfig, "directory-inline" );
        assertFilesAdded( mojo, requiredDependencies );
    }
    
    public void testModuleSetSourceIncludedBinariesNotIncluded() throws Exception
    {
        String pluginConfig = "moduleSetSourceIncludedBinariesNotIncluded-pluginConfig.xml";
        
        Mojo mojo = run( pluginConfig, "attached" );
        
        List required = Collections.singletonList( "sources/module1/src/main/java/org/test/module1/App.java" );
        
        assertFilesAdded( mojo, required );
    }
    
    private Mojo run( String pluginConfig, String mojoName ) throws Exception
    {
        String pluginConfigResource = "basicAbstractAssemblyMojoFeaturesTest/" + pluginConfig;
        
        File pluginConfigFile = new File( getBasedir(), "src/test/plugin-configs/" + pluginConfigResource );
        
        assertTrue( "Cannot find plugin-configuration: \'" + pluginConfigResource + "\' in context-classloader\'s classpath.", pluginConfigFile.exists() );
        
        // TODO: Need to replace this with test-only mojos...
        Mojo mojo = (Mojo) lookupMojo( mojoName, pluginConfigFile.getAbsolutePath() );

        FileLoggingArchiverManagerStub archiverManager = (FileLoggingArchiverManagerStub) getVariableValueFromObject( mojo, "archiverManager" );
        archiverManager.clearArchiver();
        
        mojo.execute();

        return mojo;
    }

    private void assertFilesAdded( Mojo mojo, List requiredDependencies ) throws Exception
    {
        FileLoggingArchiverManagerStub archiverManager = (FileLoggingArchiverManagerStub) getVariableValueFromObject( mojo, "archiverManager" );
        
        FileLoggingArchiverStub archiver = (FileLoggingArchiverStub) archiverManager.getArchiver( (File)null );
        
        Set addedFiles = archiver.getAddedFiles();
        
        System.out.println( "The following files were added to the test assembly:\n" + addedFiles.toString().replace(',', '\n' ) );
        
        for ( Iterator it = requiredDependencies.iterator(); it.hasNext(); )
        {
            String targetPath = (String) it.next();
            
            assertTrue( "Required dependency path missing: \'" + targetPath + "\'", addedFiles.contains( targetPath ) );
        }
    }
    
    public static final class BasedirSettableMavenProjectStub extends MavenProjectStub
    {
        
        File basedir;
        
        public File getBasedir()
        {
            return basedir;
        }
        
        public void setBasedir( File basedir )
        {
            this.basedir = basedir;
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
        
        public UnArchiver getUnArchiver( File arg0 ) throws NoSuchArchiverException
        {
            // TODO Auto-generated method stub
            return null;
        } 
        
        public Archiver getArchiver( File arg0 ) throws NoSuchArchiverException
        {
            // TODO Auto-generated method stub
            return null;
        }         
    }

    public static final class FileLoggingArchiverStub
        implements Archiver
    {

        private Set files = new LinkedHashSet();
        private DirectoryScanner scanner = new DirectoryScanner();

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
        
        public void addDirectory( File dir ) throws ArchiverException
        {
            System.out.println( "Add dir 1" );
            addDirectory_( dir, null, null, null );
        }

        public void addDirectory( File dir, String prefix ) throws ArchiverException
        {
            System.out.println( "Add dir 2" );
            addDirectory_( dir, prefix, null, null );
        }

        public void addDirectory( File dir, String[] includes, String[] excludes ) throws ArchiverException
        {
            System.out.println( "Add dir 3" );
            addDirectory_( dir, null, includes, excludes );
        }

        public void addDirectory( File dir, String prefix, String[] includes, String[] excludes ) throws ArchiverException
        {
            System.out.println( "Add dir 3" );
            addDirectory_( dir, prefix, includes, excludes );
        }
        
        private void addDirectory_( File dir, String prefix, String[] includes, String[] excludes ) throws ArchiverException
        {
            try
            {
                String include = StringUtils.join( includes, "," );
                String exclude = StringUtils.join( excludes, "," );
                
                String prepend = prefix;
                
                if ( prepend != null && !prepend.endsWith( "/" ) )
                {
                    prepend += "/";
                }
                
                System.out.println( "Scanning: " + dir + "\nwith includes: " + include + "\nand excludes: " + exclude + "\nand prepending dir prefix: " + prepend );
                
                List fileNames = FileUtils.getFileNames( dir, include, exclude, false );
                
                for ( Iterator it = fileNames.iterator(); it.hasNext(); )
                {
                    String name = (String) it.next();
                    
                    String fn = prepend + dir.getPath() + "/" + name;
                    fn.replace( '\\', '/' );
                    
                    System.out.println( "Adding: " + fn );
                    
                    files.add( fn );
                }
            }
            catch ( IOException e )
            {
                throw new ArchiverException( "Error scanning for file names.", e );
            }
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
        private File depOneFile;
        
        private String depTwoArtifactId;
        private String depTwoGroupId;
        private String depTwoVersion;
        private String depTwoType = "jar";
        private String depTwoScope = "compile";
        private File depTwoFile;
        private LinkedHashSet artifacts;
        
        public Set getArtifacts()
        {
            artifacts = new LinkedHashSet();

            addArtifact( depOneGroupId, depOneArtifactId, depOneVersion, depOneType, depOneScope, depOneFile );
            addArtifact( depTwoGroupId, depTwoArtifactId, depTwoVersion, depTwoType, depTwoScope, depTwoFile );
            
            return artifacts;
        }
        
        private void addArtifact( String groupId, String artifactId, String version, String type, String scope, File file )
        {
            Artifact artifact = new HandlerEquippedArtifactStub( groupId, artifactId, version, type, scope );
            artifact.setFile( file );
            
            artifacts .add( artifact );
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
    
    public static final class HandlerEquippedArtifactStub implements Artifact
    {

        private String type;
        private String artifactId;
        private String groupId;
        private String version;
        private String classifier;
        private String scope;
        private File file;

        public HandlerEquippedArtifactStub( String groupId, String artifactId, String version, String type, String scope )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.type = type;
            this.scope = scope;
        }
        
        public HandlerEquippedArtifactStub()
        {
        }

        public ArtifactHandler getArtifactHandler()
        {
            ArtifactHandler handler = new ArtifactHandler()
            {

                public String getClassifier()
                {
                    return classifier;
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
                    return "java";
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

        public void addMetadata( ArtifactMetadata arg0 )
        {
            // TODO Auto-generated method stub
            
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public List getAvailableVersions()
        {
            return Collections.singletonList( version );
        }

        public String getBaseVersion()
        {
            return version;
        }

        public String getClassifier()
        {
            return classifier;
        }

        public String getDependencyConflictId()
        {
            return groupId + ":" + artifactId + ":" + type + ":" + version;
        }

        public ArtifactFilter getDependencyFilter()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public List getDependencyTrail()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public String getDownloadUrl()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public File getFile()
        {
            return file;
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getId()
        {
            return getDependencyConflictId() + ":" + scope;
        }

        public Collection getMetadataList()
        {
            return null;
        }

        public ArtifactRepository getRepository()
        {
            return null;
        }

        public String getScope()
        {
            return scope;
        }

        public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException
        {
            return null;
        }

        public String getType()
        {
            return type;
        }

        public String getVersion()
        {
            return version;
        }

        public VersionRange getVersionRange()
        {
            return VersionRange.createFromVersion( version );
        }

        public boolean hasClassifier()
        {
            return classifier != null;
        }

        public boolean isOptional()
        {
            return false;
        }

        public boolean isRelease()
        {
            return false;
        }

        public boolean isResolved()
        {
            return true;
        }

        public boolean isSelectedVersionKnown() throws OverConstrainedVersionException
        {
            return true;
        }

        public boolean isSnapshot()
        {
            return false;
        }

        public void selectVersion( String arg0 )
        {
        }

        public void setArtifactHandler( ArtifactHandler arg0 )
        {
        }

        public void setArtifactId( String artifactId )
        {
            this.artifactId = artifactId;
        }

        public void setAvailableVersions( List arg0 )
        {
        }

        public void setBaseVersion( String version )
        {
            this.version = version;
        }

        public void setDependencyFilter( ArtifactFilter arg0 )
        {
        }

        public void setDependencyTrail( List arg0 )
        {
        }

        public void setDownloadUrl( String arg0 )
        {
        }

        public void setFile( File file )
        {
            this.file = file;
            
            if ( file.exists() )
            {
                Writer writer = null;
                try
                {
                    writer = new FileWriter( file );
                    writer.write( "test artifact" );
                }
                catch ( IOException e )
                {
                    IllegalArgumentException error = new IllegalArgumentException( "Cannot write test file: " + file + ". Reason: " + e.getMessage() );
                    error.initCause( e );
                    
                    throw error;
                }
                finally
                {
                    IOUtil.close( writer );
                }
            }
        }

        public void setGroupId( String groupId )
        {
            this.groupId = groupId;
        }

        public void setRelease( boolean arg0 )
        {
        }

        public void setRepository( ArtifactRepository arg0 )
        {
        }

        public void setResolved( boolean arg0 )
        {
        }

        public void setResolvedVersion( String version )
        {
            this.version = version;
        }

        public void setScope( String scope )
        {
            this.scope = scope;
        }

        public void setVersion( String version )
        {
            this.version = version;
        }

        public void setVersionRange( VersionRange arg0 )
        {
        }

        public void updateVersion( String arg0, ArtifactRepository arg1 )
        {
        }

        public int compareTo( Object o )
        {
            return 0;
        }

    }
}