package org.apache.maven.changes;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
