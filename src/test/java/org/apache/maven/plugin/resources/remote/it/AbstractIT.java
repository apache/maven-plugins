package org.apache.maven.plugin.resources.remote.it;

import org.apache.maven.plugin.resources.remote.it.support.BootstrapInstaller;

import junit.framework.TestCase;

public abstract class AbstractIT
    extends TestCase
{
    public void setUp()
        throws Exception
    {
        BootstrapInstaller.install();
        
        super.setUp();
    }
}
