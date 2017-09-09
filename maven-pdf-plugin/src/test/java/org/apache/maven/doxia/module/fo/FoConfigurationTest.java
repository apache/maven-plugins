package org.apache.maven.doxia.module.fo;

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

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

import junit.framework.TestCase;

/**
 * FoConfiguration tests.
 *
 * @version $Id: FoConfigurationTest.java 1125262 2011-05-20 07:23:00Z ltheussl $
 */
public class FoConfigurationTest
    extends TestCase
{

    /** Tests the getAttributeString( String ) method. */
    public void testGetAttributeString()
    {
        FoConfiguration config = new FoConfiguration();

        assertEquals( "Null attribute ID should return empty string!", "", config.getAttributeString( null ) );

        assertEquals( "Non existent attribute ID should return empty string!", "",
                      config.getAttributeString( "a.dummy.attribute" ) );

        assertEquals( "Wrong attributes returned for italic!", " font-style=\"italic\"",
                      config.getAttributeString( "italic" ) );
    }

    /** Tests the getAttributeSet( String ) method. */
    public void testGetAttributeSet()
    {
        FoConfiguration config = new FoConfiguration();

        assertNull( "Null attribute ID should return null AttributeSet!", config.getAttributeSet( null ) );

        assertNull( "Empty attribute ID should return null AttributeSet!", config.getAttributeSet( "" ) );

        assertNull( "Non existent attribute ID should return null AttributeSet!",
                    config.getAttributeSet( "a.dummy.attribute" ) );

        MutableAttributeSet expected = new SimpleAttributeSet();
        expected.addAttribute( "font-style", "italic" );
        MutableAttributeSet actual = config.getAttributeSet( "italic" );

        assertTrue( "Wrong AttributeSet returned for italic!", expected.isEqual( actual ) );
    }

}
