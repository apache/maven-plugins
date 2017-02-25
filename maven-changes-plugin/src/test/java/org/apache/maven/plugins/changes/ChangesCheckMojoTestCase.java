package org.apache.maven.plugins.changes;

import org.apache.maven.plugins.changes.ChangesCheckMojo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

/**
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class ChangesCheckMojoTestCase
    extends TestCase
{
    public void testIsValidDate()
        throws Exception
    {
        String pattern;

        // null pattern
        pattern = null;
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "2010-12-16", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern ) );

        // empty pattern
        pattern = "";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "2010-12-16", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern ) );

        // valid pattern
        pattern = "yyyy-MM-dd";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "2010-DD-MM", pattern ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-16", pattern ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern ) );
    }

    public void testIsValidateWithLocale()
        throws Exception
    {
        String pattern, locale = null;

        // null locale
        pattern = "yyyy-MM-dd";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // unknown locale specified, should use default locale
        locale = "ab_CD";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // pattern with months as number
        pattern = "yyyy-MM-dd";

        // Czech locale
        locale = "cs_CZ";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // English locale
        locale = "en_US";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "2010-12-06", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // pattern with months as text
        pattern = "dd MMM yyyy";

        // Czech locale
        locale = "cs_CZ";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "06 XII 2010", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );

        // English locale
        locale = "en_US";
        assertFalse( ChangesCheckMojo.isValidDate( null, pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "", pattern, locale ) );
        assertTrue( ChangesCheckMojo.isValidDate( "06 Dec 2010", pattern, locale ) );
        assertFalse( ChangesCheckMojo.isValidDate( "pending", pattern, locale ) );
    }
}
