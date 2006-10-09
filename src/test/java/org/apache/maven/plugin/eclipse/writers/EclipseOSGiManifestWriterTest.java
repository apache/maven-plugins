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
package org.apache.maven.plugin.eclipse.writers;

import junit.framework.TestCase;

/**
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EclipseOSGiManifestWriterTest
    extends TestCase
{
    /**
     * Test normalization of a 2 digits snapshot version
     */
    public void testGetNormalizedVersionTwoDigitsSnapshot()
    {
        assertEquals( "2.1.0.SNAPSHOT", EclipseOSGiManifestWriter.getNormalizedVersion( "2.1-SNAPSHOT" ) );
    }

    /**
     * Test normalization of a 3 digits snapshot version
     */
    public void testGetNormalizedVersionThreeDigitsSnapshot()
    {
        assertEquals( "2.1.3.SNAPSHOT", EclipseOSGiManifestWriter.getNormalizedVersion( "2.1.3-SNAPSHOT" ) );
    }

    /**
     * Test normalization of a 4 digits snapshot version
     */
    public void testGetNormalizedVersionFourDigitsSnapshot()
    {
        assertEquals( "2.1.3.0.SNAPSHOT", EclipseOSGiManifestWriter.getNormalizedVersion( "2.1.3.0-SNAPSHOT" ) );
    }
}
