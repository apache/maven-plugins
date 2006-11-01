package org.apache.maven.plugin.pmd;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.IOException;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Fail the build if there were any CPD violations in the source code.
 *
 * @goal cpd-check
 * @phase verify
 * @execute goal="cpd"
 */
public class CpdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo
{
    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        executeCheck( "cpd.xml", "duplication", "CPD duplication",10 );
    }
    
    /**
     * Formats the failure details and prints them as an INFO message
     * 
     * @param item
     */
    protected void printError( Map item, String severity )
    {
        // TODO Auto-generated method stub
        
    }

    protected Map getErrorDetails( XmlPullParser xpp )
        throws XmlPullParserException, IOException
    {
        // TODO Auto-generated method stub
        return null;
    }
}
