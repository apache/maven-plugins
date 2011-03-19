package org.apache.maven.plugins.site;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.site.SimpleDavServerHandler.HttpRequest;
import org.apache.maven.plugins.site.stubs.SiteMavenProjectStub;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @version $Id$
 */
@RunWith(JUnit4.class)
public abstract class AbstractSiteDeployWebDavTest
    extends AbstractMojoTestCase
{
    
    File siteTargetPath = new File(getBasedir() + File.separator + "target" +  File.separator + "siteTargetDeploy");
    
    private Logger log = LoggerFactory.getLogger( getClass() );
    
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        if (!siteTargetPath.exists())
        {
            siteTargetPath.mkdirs();
            FileUtils.cleanDirectory( siteTargetPath );
        }
    }
    
    abstract String getMojoName();
    
    abstract AbstractMojo getMojo( File pluginXmlFile ) throws Exception;
    
    @Test
    public void noAuthzDavDeploy() throws Exception
    {
        FileUtils.cleanDirectory( siteTargetPath );
        SimpleDavServerHandler simpleDavServerHandler = new SimpleDavServerHandler( siteTargetPath );
        try
        {
            File pluginXmlFile = getTestFile( "src/test/resources/unit/deploy-dav/pom.xml" );
            AbstractMojo mojo = getMojo( pluginXmlFile );
            assertNotNull( mojo );
            SiteMavenProjectStub siteMavenProjectStub = new SiteMavenProjectStub("src/test/resources/unit/deploy-dav/pom.xml"); 
            
            siteMavenProjectStub.getDistributionManagement().getSite()
                .setUrl( "dav:http://localhost:" + simpleDavServerHandler.getPort() + "/site/" );
            
            setVariableValueToObject( mojo, "project", siteMavenProjectStub );
            Settings settings = new Settings();
            setVariableValueToObject( mojo, "settings", settings );
            File inputDirectory = new File("src/test/resources/unit/deploy-dav/target/site");
            
            setVariableValueToObject( mojo, "inputDirectory", inputDirectory );
            mojo.execute();
            
            assertContentInFiles();
            assertFalse( requestsContainsProxyUse( simpleDavServerHandler.httpRequests ) );
        }
        finally
        {
            simpleDavServerHandler.stop();
        }
    }
    
    @Test
    public void davDeployThruProxyWithoutAuthzInProxy()
        throws Exception
    {
        
        FileUtils.cleanDirectory( siteTargetPath );
        SimpleDavServerHandler simpleDavServerHandler = new SimpleDavServerHandler( siteTargetPath );
        try
        {
            File pluginXmlFile = getTestFile( "src/test/resources/unit/deploy-dav/pom.xml" );
            AbstractMojo mojo = getMojo( pluginXmlFile );
            assertNotNull( mojo );
            SiteMavenProjectStub siteMavenProjectStub = new SiteMavenProjectStub("src/test/resources/unit/deploy-dav/pom.xml");
            // olamy, Note : toto is something like foo or bar for french folks :-)
            String siteUrl = "dav:http://toto.com/site/";
            siteMavenProjectStub.getDistributionManagement().getSite().setUrl( siteUrl );
            
            setVariableValueToObject( mojo, "project", siteMavenProjectStub );
            Settings settings = new Settings();
            Proxy proxy = new Proxy();

            //dummy proxy
            proxy.setActive( true );
            proxy.setHost( "localhost" );
            proxy.setPort( simpleDavServerHandler.getPort() );
            proxy.setProtocol( "http" );
            proxy.setNonProxyHosts( "www.google.com|*.somewhere.com" );
            settings.addProxy( proxy );
            
            setVariableValueToObject( mojo, "settings", settings );
            
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            request.setProxies( Arrays.asList( proxy ) );
            MavenSession mavenSession = new MavenSession( getContainer(), null, request, null );
            
            setVariableValueToObject( mojo, "mavenSession", mavenSession );
            
            File inputDirectory = new File("src/test/resources/unit/deploy-dav/target/site");
            
            setVariableValueToObject( mojo, "inputDirectory", inputDirectory );
            mojo.execute();
            
            assertContentInFiles();
            
            assertTrue( requestsContainsProxyUse( simpleDavServerHandler.httpRequests ) );
            
            for (HttpRequest rq : simpleDavServerHandler.httpRequests)
            {
                log.info( rq.toString() );
            }
            
        }
        finally
        {
            simpleDavServerHandler.stop();
        }        
        
    }
    
    @Test
    public void davDeployThruProxyWitAuthzInProxy() throws Exception
    {

        FileUtils.cleanDirectory( siteTargetPath );
        //SimpleDavServerHandler simpleDavServerHandler = new SimpleDavServerHandler( siteTargetPath );
        
        Map<String, String> authentications = new HashMap<String, String>();
        authentications.put( "foo", "titi" );
        
        AuthAsyncProxyServlet servlet = new  AuthAsyncProxyServlet(authentications, siteTargetPath);

        SimpleDavServerHandler simpleDavServerHandler = new SimpleDavServerHandler( servlet );        
        try
        {
            File pluginXmlFile = getTestFile( "src/test/resources/unit/deploy-dav/pom.xml" );
            AbstractMojo mojo = getMojo( pluginXmlFile );
            assertNotNull( mojo );
            SiteMavenProjectStub siteMavenProjectStub = new SiteMavenProjectStub("src/test/resources/unit/deploy-dav/pom.xml"); 
            
            siteMavenProjectStub.getDistributionManagement().getSite()
                .setUrl( "dav:http://toto.com/site/" );
            
            setVariableValueToObject( mojo, "project", siteMavenProjectStub );
            Settings settings = new Settings();
            Proxy proxy = new Proxy();

            //dummy proxy
            proxy.setActive( true );
            proxy.setHost( "localhost" );
            proxy.setPort( simpleDavServerHandler.getPort() );
            proxy.setProtocol( "dav" );
            proxy.setUsername( "foo" );
            proxy.setPassword( "titi" );
            proxy.setNonProxyHosts( "www.google.com|*.somewhere.com" );
            settings.addProxy( proxy );
            
            setVariableValueToObject( mojo, "settings", settings );
            
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            request.setProxies( Arrays.asList( proxy ) );
            MavenSession mavenSession = new MavenSession( getContainer(), null, request, null );
            
            setVariableValueToObject( mojo, "mavenSession", mavenSession );
            
            File inputDirectory = new File("src/test/resources/unit/deploy-dav/target/site");
            
            // test which mojo we are using
            if (ReflectionUtils.getFieldByNameIncludingSuperclasses( "inputDirectory", mojo.getClass() ) != null)
            {
                setVariableValueToObject( mojo, "inputDirectory", inputDirectory );
            }
            else
            {
                ArtifactRepositoryFactory artifactRepositoryFactory = getContainer().lookup( ArtifactRepositoryFactory.class );
                
                setVariableValueToObject( mojo, "stagingDirectory", inputDirectory );
                setVariableValueToObject( mojo, "reactorProjects", Collections.emptyList() );
                setVariableValueToObject( mojo, "localRepository",
                                          artifactRepositoryFactory.createArtifactRepository( "local", "foo", "default",
                                                                                              null, null ) );
                setVariableValueToObject( mojo, "siteTool", getContainer().lookup( SiteTool.class ) );
                setVariableValueToObject( mojo, "siteDirectory", new File("foo") );
                setVariableValueToObject( mojo, "repositories", Collections.emptyList() );
            }
            mojo.execute();
            
            assertContentInFiles();
            assertTrue( requestsContainsProxyUse( servlet.httpRequests ) );
            assertAtLeastOneRequestContainsHeader(servlet.httpRequests, "Proxy-Authorization");
            for (HttpRequest rq : servlet.httpRequests)
            {
                log.info( rq.toString() );
            }
        }
        finally
        {
            simpleDavServerHandler.stop();
        }  
    }        
    
    private void assertContentInFiles()
        throws Exception
    {
        File fileToTest = new File( siteTargetPath, "site" + File.separator + "index.html" );
        assertTrue( fileToTest.exists() );
        String fileContent = FileUtils.readFileToString( fileToTest );
        assertTrue( fileContent.contains( "Welcome to Apache Maven" ) );

        fileToTest = new File( siteTargetPath, "site" + File.separator + "css" + File.separator + "maven-base.css" );
        assertTrue( fileToTest.exists() );
        fileContent = FileUtils.readFileToString( fileToTest );
        assertTrue( fileContent.contains( "background-image: url(../images/collapsed.gif);" ) );
    }
    
    /**
     * @param requests
     * @return true if at least on request use proxy http header Proxy-Connection : Keep-Alive
     */
    private boolean requestsContainsProxyUse( List<HttpRequest> requests )
    {
        return assertAtLeastOneRequestContainsHeader(requests, "Proxy-Connection");
    }
    
    private boolean assertAtLeastOneRequestContainsHeader(List<HttpRequest> requests , String headerName)
    {
        for ( HttpRequest rq : requests )
        {
            boolean containsProxyHeader = rq.headers.containsKey( headerName );
            if ( containsProxyHeader )
            {
                return true;
            }
        }
        return false;
    }
}
