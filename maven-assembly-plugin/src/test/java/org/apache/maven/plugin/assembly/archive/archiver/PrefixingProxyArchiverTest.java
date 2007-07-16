package org.apache.maven.plugin.assembly.archive.archiver;

import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class PrefixingProxyArchiverTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "massembly-proxyArchiver", "" );

    public void tearDown()
        throws Exception
    {
        fileManager.cleanUp();
    }

    public void testAddFile_NoPerms_CallAcceptFilesOnlyOnce()
        throws IOException, ArchiverException
    {
        MockControl delegateControl = MockControl.createControl( Archiver.class );
        Archiver delegate = (Archiver) delegateControl.getMock();

        delegate.addFile( null, null );
        delegateControl.setMatcher( MockControl.ALWAYS_MATCHER );
        delegateControl.setVoidCallable();

        CounterSelector counter = new CounterSelector( true );
        List selectors = Collections.singletonList( counter );

        delegateControl.replay();

        PrefixingProxyArchiver archiver = new PrefixingProxyArchiver( "", delegate, Collections.EMPTY_LIST, selectors,
                                                                      Collections.EMPTY_LIST );

        File inputFile = fileManager.createTempFile();

        archiver.addFile( inputFile, "file.txt" );

        assertEquals( 1, counter.getCount() );

        delegateControl.verify();
    }

    public void testAddDirectory_NoPerms_CallAcceptFilesOnlyOnce()
        throws IOException, ArchiverException
    {
        Archiver delegate = new JarArchiver();

        File output = fileManager.createTempFile();
        delegate.setDestFile( output );

        CounterSelector counter = new CounterSelector( false );
        List selectors = Collections.singletonList( counter );

        PrefixingProxyArchiver archiver = new PrefixingProxyArchiver( "", delegate, Collections.EMPTY_LIST, selectors,
                                                                      Collections.EMPTY_LIST );

        File dir = fileManager.createTempDir();
        fileManager.createFile( dir, "file.txt", "This is a test." );

        archiver.addDirectory( dir );

        assertEquals( 1, counter.getCount() );
    }

    private static final class CounterSelector
        implements FileSelector
    {

        private int count = 0;

        private boolean answer = false;

        public CounterSelector( boolean answer )
        {
            this.answer = answer;
        }

        public int getCount()
        {
            return count;
        }

        public boolean isSelected( FileInfo fileInfo )
            throws IOException
        {
            if ( fileInfo.isFile() )
            {
                count++;
            }

            return answer;
        }

    }

}
