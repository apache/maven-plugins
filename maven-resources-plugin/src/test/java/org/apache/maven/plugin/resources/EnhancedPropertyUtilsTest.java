package org.apache.maven.plugin.resources;

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
import java.io.IOException;
import java.util.Properties;

/**
 * Tests {@link PropertyUtils#loadPropertyFile(File, Properties)}.
 *
 * @author William Ferguson
 */
public class EnhancedPropertyUtilsTest
    extends AbstractPropertyUtilsTest
{
    private static final String validationFileName =
        "/target/test-classes/unit/propertiesutils-test/enhanced_validation.properties";

    private static final String propFileName = "/target/test-classes/unit/propertiesutils-test/enhanced.properties";

    private final Properties baseProps = new Properties();

    protected void setUp()
        throws Exception
    {
        super.setUp();
        this.baseProps.setProperty( "prop1", "valueOfProperty1" );
    }

    protected File getPropertyFile()
    {
        final File propFile = new File( getBasedir(), propFileName );

        if ( !propFile.exists() )
        {
            return null;
        }

        return propFile;
    }

    protected File getValidationFile()
    {
        final File file = new File( getBasedir(), validationFileName );

        if ( !file.exists() )
        {
            return null;
        }

        return file;
    }

    /**
     * Load property test case can be adjusted by modifying the enhanced.properties and enhanced_validation properties.
     *
     * @throws Exception
     */
    public void testBasicLoadProperty()
        throws Exception
    {
        final Properties props = PropertyUtils.loadPropertyFile( this.propertyFile, this.baseProps );

        assertNotNull( props );
        assertTrue( validateProperties( props ) );
    }

    /**
     * Load property test case can be adjusted by modifying the enhanced.properties and enhanced_validation properties.
     *
     * @throws Exception
     */
    public void testNonExistentProperty()
        throws Exception
    {
        final Properties props = PropertyUtils.loadPropertyFile( this.propertyFile, this.baseProps );

        assertNotNull( props );
        assertNull( props.getProperty( "does_not_exist" ) );
    }

    /**
     * Load property test case can be adjusted by modifying the enhanced.properties and enhanced_validation properties.
     *
     * @throws Exception
     */
    public void testException()
        throws Exception
    {
        try
        {
            PropertyUtils.loadPropertyFile( new File( "NON_EXISTENT_FILE" ), this.baseProps );
            fail( "Should not have been able to load properties from a non-existent file" );
        }
        catch ( IOException e )
        {
            // as expected.
        }
    }
}
