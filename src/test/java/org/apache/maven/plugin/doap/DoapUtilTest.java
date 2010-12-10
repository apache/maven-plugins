package org.apache.maven.plugin.doap;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Developer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Test {@link DoapUtil} class.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DoapUtilTest
    extends PlexusTestCase
{
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Test method for {@link DoapUtil#writeElement(XMLWriter, String, String, String).
     *
     * @throws Exception if any
     */
    public void testWriteElementXMLWriterStringStringString()
        throws Exception
    {
        StringWriter w = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( w );
        DoapUtil.writeElement( writer, "name", "value" );
        w.close();
        assertEquals( w.toString(), "<name>value</name>" );

        w = new StringWriter();
        writer = new PrettyPrintXMLWriter( w );
        try
        {
            DoapUtil.writeElement( writer, null, null );
            assertTrue( "Null not catched", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( "IllegalArgumentException catched", true );
        }
        finally
        {
            w.close();
        }
    }

    /**
     * Test method for {@link DoapUtil#writeRdfResourceElement(XMLWriter, String, String).
     *
     * @throws Exception if any
     */
    public void testWriteRdfResourceElement()
        throws Exception
    {
        StringWriter w = new StringWriter();
        XMLWriter writer = new PrettyPrintXMLWriter( w );
        DoapUtil.writeRdfResourceElement( writer, "name", "value" );
        w.close();
        assertEquals( w.toString(), "<name " + DoapUtil.RDF_RESOURCE + "=\"value\"/>" );

        w = new StringWriter();
        writer = new PrettyPrintXMLWriter( w );
        try
        {
            DoapUtil.writeRdfResourceElement( writer, null, null );
            assertTrue( "Null not catched", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( "IllegalArgumentException catched", true );
        }
        finally
        {
            w.close();
        }
    }

    /**
     * Test method for:
     * {@link DoapUtil#getDevelopersOrContributorsWithDeveloperRole(I18N, List)}
     * {@link DoapUtil#getDevelopersOrContributorsWithDocumenterRole(I18N, List)}
     * {@link DoapUtil#getDevelopersOrContributorsWithHelperRole(I18N, List)}
     * {@link DoapUtil#getDevelopersOrContributorsWithMaintainerRole(I18N, List)}
     * {@link DoapUtil#getDevelopersOrContributorsWithTesterRole(I18N, List)}
     * {@link DoapUtil#getDevelopersOrContributorsWithTranslatorRole(I18N, List)}
     * {@link DoapUtil#getDevelopersOrContributorsWithUnknownRole(I18N, List)}
     *
     * @throws Exception if any
     */
    public void testDevelopersOrContributorsByDoapRoles()
        throws Exception
    {
        I18N i18n = (I18N) getContainer().lookup( I18N.ROLE );
        assertNotNull( i18n );
        assertNotNull( i18n.getBundle() );

        List developersOrContributors = new ArrayList();

        // One role
        Developer dev = new Developer();
        dev.setId( "dev1" );
        dev.addRole( "maintainer" );

        developersOrContributors.add( dev );

        assertTrue( DoapUtil.getDevelopersOrContributorsWithDeveloperRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithDocumenterRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithHelperRole( i18n, developersOrContributors ).isEmpty() );
        assertFalse( DoapUtil.getDevelopersOrContributorsWithMaintainerRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithTesterRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithTranslatorRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithUnknownRole( i18n, developersOrContributors ).isEmpty() );

        // Several roles
        developersOrContributors.clear();

        dev = new Developer();
        dev.setId( "dev1" );
        dev.addRole( " MAINTAINER" );
        dev.addRole( "tesTER " );
        dev.addRole( "blabla" );
        dev.addRole( "translato r" );

        developersOrContributors.add( dev );

        assertTrue( DoapUtil.getDevelopersOrContributorsWithDeveloperRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithDocumenterRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithHelperRole( i18n, developersOrContributors ).isEmpty() );
        assertFalse( DoapUtil.getDevelopersOrContributorsWithMaintainerRole( i18n, developersOrContributors ).isEmpty() );
        assertFalse( DoapUtil.getDevelopersOrContributorsWithTesterRole( i18n, developersOrContributors ).isEmpty() );
        assertTrue( DoapUtil.getDevelopersOrContributorsWithTranslatorRole( i18n, developersOrContributors ).isEmpty() );
        assertFalse( DoapUtil.getDevelopersOrContributorsWithUnknownRole( i18n, developersOrContributors ).isEmpty() );

        // Skip emeritus role
        developersOrContributors.clear();

        dev = new Developer();
        dev.setId( "dev1" );
        dev.addRole( "maintainer" );
        dev.addRole( "unknown" );

        developersOrContributors.add( dev );

        int sizeBeforeEmeritus = DoapUtil.getDevelopersOrContributorsWithUnknownRole( i18n, developersOrContributors).size();
        dev.addRole( " Emeritus" );

        assertTrue( DoapUtil.getDevelopersOrContributorsWithUnknownRole( i18n, developersOrContributors).size() == sizeBeforeEmeritus );

    }

    /**
     * Test method for:
     * {@link DoapUtil#validate(java.io.File)}
     *
     * @throws Exception if any
     */
    public void testValidate()
        throws Exception
    {
        File doapFile = new File( getBasedir(), "src/test/resources/generated-doap-1.0.rdf" );
        assertFalse( DoapUtil.validate( doapFile ).isEmpty() );
    }
}
