package org.apache.maven.plugin.assembly.format;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.ConfigSourceStub;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReaderFormatterTest
{

    @Test
    @Ignore
    public void testGetFileSetTransformers()
        throws Exception
    {
        final AssemblerConfigurationSource assemblerConfigurationSource =  new ConfigSourceStub();
        ReaderFormatter.getFileSetTransformers( assemblerConfigurationSource, true, "\r\n" );
    }

}