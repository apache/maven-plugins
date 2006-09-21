package org.apache.maven.plugin.ant;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.StringWriter;

import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import junit.framework.TestCase;

/**
 * Test cases for 'org.apache.maven.plugin.ant.AntBuildWriterUtil'
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntBuildWriterUtilTest
    extends TestCase
{
    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.writeComment(XMLWriter, String)'
     */
    public void testWriteCommentNull()
    {
        StringWriter s = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( s );
        AntBuildWriterUtil.writeComment( writer, null );
        StringBuffer sb = new StringBuffer();
        sb.append( "<!-- null                                                                   -->" ).append( '\n' );
        assertTrue( s.toString().equals( sb.toString() ) );
    }

    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.writeComment(XMLWriter, String)'
     */
    public void testWriteCommentShort()
    {
        StringWriter s = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( s );
        AntBuildWriterUtil.writeComment( writer, "This is a short text");
        StringBuffer sb = new StringBuffer();
        sb.append( "<!-- This is a short text                                                   -->" ).append( '\n' );
        assertTrue( s.toString().equals( sb.toString() ) );
    }

    /**
     * Test method for 'org.apache.maven.plugin.ant.AntBuildWriterUtil.writeComment(XMLWriter, String)'
     */
    public void testWriteCommentLong()
    {
        StringWriter s = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( s );
        AntBuildWriterUtil.writeComment( writer, "Maven is a software project management and comprehension tool. " +
                "Based on the concept of a project object model (POM), Maven can manage a project's build, reporting " +
                "and documentation from a central piece of information." );
        StringBuffer sb = new StringBuffer();
        sb.append( "<!-- Maven is a software project management and comprehension tool. Based   -->" ).append( '\n' );
        sb.append( "<!-- on the concept of a project object model (POM), Maven can manage a     -->" ).append( '\n' );
        sb.append( "<!-- project's build, reporting and documentation from a central piece of   -->" ).append( '\n' );
        sb.append( "<!-- information.                                                           -->" ).append( '\n' );
        assertTrue( s.toString().equals( sb.toString() ) );
    }
}
