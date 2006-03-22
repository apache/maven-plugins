

package org.apache.maven.jira;

import junit.framework.*;


public class JiraIssueTest extends TestCase {
    
    JiraIssue issue;
    
    public JiraIssueTest(String testName) 
    {
        super(testName);
    }

    protected void setUp() throws Exception 
    {
        issue = new JiraIssue();
    }

    protected void tearDown() throws Exception 
    {
    }

    public static Test suite() 
    {
        TestSuite suite = new TestSuite(JiraIssueTest.class);
        
        return suite;
    }

    public void testGetSetKey() 
    {
        issue.setKey("key");
        
        assertEquals("key",issue.getKey());
    }

    public void testGetSetSummary() 
    {
        issue.setSummary("summary");
        
        assertEquals("summary",issue.getSummary());
    }

    public void testGetSetStatus() 
    {
        issue.setStatus("status");
        
        assertEquals("status",issue.getStatus());
    }

    public void testGetSetResolution() 
    {
        issue.setResolution("resolution");
        
        assertEquals("resolution",issue.getResolution());
    }

    public void testGetSetAssignee() 
    {
        issue.setAssignee("assignee");
        
        assertEquals("assignee",issue.getAssignee());
    }
    
}
