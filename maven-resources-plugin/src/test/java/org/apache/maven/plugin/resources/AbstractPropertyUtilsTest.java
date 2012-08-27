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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.IOUtil;

/**
 * Base class for propertyutils test case
 */
public abstract class AbstractPropertyUtilsTest
    extends AbstractMojoTestCase
{
    protected File propertyFile;

    protected File validationFile;

    protected Properties validationProp;

    protected abstract File getPropertyFile();

    protected abstract File getValidationFile();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // load data
        propertyFile = getPropertyFile();
        assertNotNull( propertyFile );

        validationFile = getValidationFile();
        assertNotNull( validationFile );

        loadValidationProperties( validationFile );
    }

    protected void tearDown()
    {

    }

    protected boolean validateProperties( Properties prop )
    {
        boolean bRetVal = false;

        Enumeration<?> propKeys = prop.keys();
        String key;

        while ( propKeys.hasMoreElements() )
        {
            key = (String) propKeys.nextElement();
            bRetVal = prop.getProperty( key ).equals( validationProp.getProperty( key ) );
            if ( !bRetVal )
            {
                break;
            }
        }

        return bRetVal;
    }

    /**
     * load the property file for cross checking the
     * values in the processed property file
     *
     * @param validationPropFile
     */
    private void loadValidationProperties( File validationPropFile )
    {
        validationProp = new Properties();
        InputStream in = null;

        try
        {
            in = new FileInputStream( validationPropFile );
            validationProp.load( in );
        }
        catch ( IOException ex )
        {
            // TODO: do error handling
        }
        finally
        {
            IOUtil.close( in );
        }
    }
}
