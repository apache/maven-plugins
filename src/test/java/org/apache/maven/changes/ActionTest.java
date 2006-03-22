
package org.apache.maven.changes;

import junit.framework.*;

public class ActionTest extends TestCase {
    
    Action action = new Action();
    
    public ActionTest(String testName) 
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
        TestSuite suite = new TestSuite(ActionTest.class);
        
        return suite;
    }
    
    public void testGetSetAction() 
    {
        action.setAction("action");
        
        assertEquals("action", action.getAction());
    }

    public void testGetSetDev() 
    {
        action.setDev("developer");
        
        assertEquals("developer",action.getDev());
    }

    public void testGetSetType() 
    {
        action.setType("type");
        
        assertEquals("type",action.getType());
    }

    public void testGetSetIssue() 
    {
        action.setIssue("issue");
        
        assertEquals("issue",action.getIssue());
    }

    public void testGetSetDueTo() 
    {
        action.setDueTo("due-to");
        
        assertEquals("due-to",action.getDueTo());
    }

    public void testGetSetDueToEmail() 
    {
        action.setDueToEmail("due-to-mail");
        
        assertEquals("due-to-mail",action.getDueToEmail());
    }
}
