package org.apache.maven.plugin.assembly.format;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.testutils.PojoConfigSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.DefaultMavenReaderFilter;
import org.codehaus.plexus.archiver.resources.PlexusIoVirtualFileResource;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.maven.plugin.assembly.format.ReaderFormatter.getFileSetTransformers;
import static org.junit.Assert.assertEquals;


@SuppressWarnings( "ConstantConditions" )
public class ReaderFormatterTest
{
    @Test
    public void lineDosFeed()
        throws IOException, AssemblyFormattingException
    {
        final PojoConfigSource cfg = getPojoConfigSource();
        InputStreamTransformer fileSetTransformers = getFileSetTransformers( cfg, true, "dos" );
        InputStream fud = fileSetTransformers.transform( dummyResource(), payload( "This is a\ntest." ) );
        assertEquals( "This is a\r\ntest.", readResultStream( fud ) );
    }

    @Test
    public void lineUnixFeedWithInterpolation()
        throws IOException, AssemblyFormattingException
    {
        final PojoConfigSource cfg = getPojoConfigSource();
        InputStreamTransformer fileSetTransformers = getFileSetTransformers( cfg, true, "unix" );
        InputStream fud = fileSetTransformers.transform( dummyResource(), payload( "This is a test for project: ${artifactId} @artifactId@.") );
        assertEquals( "This is a test for project: anArtifact anArtifact.", readResultStream( fud ) );
    }


    private MavenProject createBasicMavenProject()
    {
        final Model model = new Model();
        model.setArtifactId( "anArtifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        return new MavenProject( model );
    }


    private String readResultStream( InputStream fud )
        throws IOException
    {
        byte[] actual = new byte[100];
        int read = IOUtils.read( fud, actual );
        return new String( actual, 0, read);
    }

    private ByteArrayInputStream payload( String payload )
    {
        return new ByteArrayInputStream( payload.getBytes() );
    }

    private PojoConfigSource getPojoConfigSource()
    {
        final PojoConfigSource cfg =  new PojoConfigSource();
        cfg.setEncoding( "UTF-8" );
        DefaultMavenReaderFilter mavenReaderFilter = new DefaultMavenReaderFilter();
        mavenReaderFilter.enableLogging( new ConsoleLogger( 2, "fud" ) );
        cfg.setMavenReaderFilter( mavenReaderFilter );
        cfg.setEscapeString( null );
        cfg.setMavenProject( createBasicMavenProject() );

/*        expect( configSource.getFilters()).andReturn( filters );

        expect( configSource.isIncludeProjectBuildFilters()).andReturn( includeProjectBuildFilters );

        expect( configSource.getDelimiters()).andReturn( delimiters );
*/
        return cfg;
    }

    private PlexusIoVirtualFileResource dummyResource()
    {
        return new PlexusIoVirtualFileResource( new File( "fud" ) )
        {
        };
    }
}