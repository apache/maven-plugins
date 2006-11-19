package org.apache.maven.plugin.ear.output;

import junit.framework.TestCase;

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
/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class FileNameMappingFactoryTest
    extends TestCase
{

    public void testDefaultFileNameMapping()
    {
        final FileNameMapping actual = FileNameMappingFactory.INSTANCE.getDefaultFileNameMapping();
        assertNotNull( actual );
        assertEquals( StandardFileNameMapping.class, actual.getClass() );
    }

    public void testGetFileNameMappingByName()
    {
        final FileNameMapping actual =
            FileNameMappingFactory.INSTANCE.getFileNameMapping( FileNameMappingFactory.STANDARD_FILE_NAME_MAPPING );
        assertNotNull( actual );
        assertEquals( StandardFileNameMapping.class, actual.getClass() );
    }

    public void testGetFileNameMappingByName2()
    {
        final FileNameMapping actual =
            FileNameMappingFactory.INSTANCE.getFileNameMapping( FileNameMappingFactory.FULL_FILE_NAME_MAPPING );
        assertNotNull( actual );
        assertEquals( FullFileNameMapping.class, actual.getClass() );
    }

    public void testGetFileNameMappingByClass() {
        final FileNameMapping actual = FileNameMappingFactory.INSTANCE.getFileNameMapping(StandardFileNameMapping.class.getName());
        assertNotNull( actual);
        assertEquals( StandardFileNameMapping.class, actual.getClass());
    }

    public void testGetFileNameMappingByClass2()
    {
        final FileNameMapping actual =
            FileNameMappingFactory.INSTANCE.getFileNameMapping( FullFileNameMapping.class.getName() );
        assertNotNull( actual );
        assertEquals( FullFileNameMapping.class, actual.getClass() );
    }

    public void testGetFileNameMappingByUnknownClass()
    {
        try
        {
            FileNameMappingFactory.INSTANCE.getFileNameMapping( "com.foo.bar" );
            fail("Should have failed");
        }
        catch ( IllegalStateException e )
        {
            // OK
        }
    }
}
