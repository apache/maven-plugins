package org.apache.maven.changes;

import java.util.ArrayList;
import junit.framework.*;
import java.util.List;


public class ReleaseTest extends TestCase {
    
    Release release = new Release();
    
    public ReleaseTest(String testName) 
    {
        super(testName);
    }

    protected void setUp() throws Exception 
    {
    }

    protected void tearDown() throws Exception 
    {
    }

    public static Test suite() 
    {
        TestSuite suite = new TestSuite(ReleaseTest.class);
        
        return suite;
    }

    public void testGetSetVersion() 
    {
         release.setVersion("version");   

         assertEquals("version",release.getVersion());
    }

    public void testGetSetDateRelease() 
    {
        release.setDateRelease("12-09-1979");
        
        assertEquals("12-09-1979",release.getDateRelease());
    }

    public void testGetSetAction() 
    {
        List actionList = new ArrayList();
        
        release.setAction(actionList);
        
        assertEquals(actionList,  release.getAction());
    }
    
}
