package org.apache.maven.plugin.resources;

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

import java.io.File;
import java.util.Properties;

public class AdvancePropertyUtilsTest
    extends AbstractPropertyUtilsTest
{
    final static protected String propFileName = "/target/test-classes/unit/propertiesutils-test/advance.properties";

    final static protected String validationFileName =
        "/target/test-classes/unit/propertiesutils-test/advance_validation.properties";

    protected File getPropertyFile()
    {

        File propFile = new File( getBasedir(), propFileName );

        if ( !propFile.exists() )
        {
            propFile = null;
        }

        return propFile;
    }

    protected File getValidationFile()
    {

        File validationFile = new File( getBasedir(), validationFileName );

        if ( !validationFile.exists() )
        {
            validationFile = null;
        }

        return validationFile;
    }

    /**
     * load property test case can be adjusted by modifying the advance.properties and
     * advance_validation.properties
     *
     * @throws Exception
     */
    public void testAdvanceLoadProperty_FF()
        throws Exception
    {
        Properties prop;
        boolean throwsException = false;

        try
        {
            prop = PropertyUtils.loadPropertyFile( propertyFile, false, false );
        }
        catch ( Exception ex )
        {
            prop = null;
            throwsException = true;
        }

        assertFalse( throwsException );
        assertNotNull( prop );
        assertTrue( validateProperties( prop ) );

    }

    /**
     * load property test case can be adjusted by modifying the advance.properties and
     * advance_validation properties
     *
     * @throws Exception
     */
    public void testAdvanceLoadProperty_TF()
        throws Exception
    {
        Properties prop = PropertyUtils.loadPropertyFile( propertyFile, true, false );

        assertNotNull( prop );
        assertTrue( validateProperties( prop ) );
    }

    /**
     * load property test case can be adjusted by modifying the advance.properties and
     * advance_validation properties
     *
     * @throws Exception
     */
    public void testAdvanceLoadProperty_TT()
        throws Exception
    {
        Properties prop = PropertyUtils.loadPropertyFile( propertyFile, true, true );

        // add system properties to our
        // validation table
        validationProp.putAll( System.getProperties() );
        assertNotNull( prop );
        assertTrue( validateProperties( prop ) );
    }
}
