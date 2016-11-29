package org.apache.maven.plugins.shade.resource;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.plugins.shade.relocation.Relocator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Test for {@link GroovyResourceTransformer}.
 *
 * @author Julien Viet
 */
public class GroovyResourceTransformerTest
    extends TestCase
{

    private static InputStream stream( Properties props )
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store( baos, null );
        return new ByteArrayInputStream( baos.toByteArray() );

    }

    private static InputStream module( String name, String version, String extensionClasses,
                                       String staticExtensionClasses )
        throws Exception
    {
        Properties desc = new Properties();
        desc.setProperty( "moduleName", name );
        desc.setProperty( "moduleVersion", version );
        if ( extensionClasses != null )
        {
            desc.setProperty( "extensionClasses", extensionClasses );
        }
        if ( staticExtensionClasses != null )
        {
            desc.setProperty( "staticExtensionClasses", staticExtensionClasses );
        }
        return stream( desc );
    }

    private static Properties transform( GroovyResourceTransformer transformer )
        throws Exception
    {
        File tempJar = File.createTempFile( "shade.", ".jar" );
        tempJar.deleteOnExit();
        FileOutputStream fos = new FileOutputStream( tempJar );
        JarOutputStream jaos = new JarOutputStream( fos );
        transformer.modifyOutputStream( jaos );
        jaos.close();
        Properties desc = null;
        JarFile jar = new JarFile( tempJar );
        try
        {
            ZipEntry entry = jar.getEntry( GroovyResourceTransformer.EXT_MODULE_NAME );
            if ( entry != null )
            {
                desc = new Properties();
                desc.load( jar.getInputStream( entry ) );
            }
        }
        finally
        {
            jar.close();
        }
        return desc;
    }

    public void testFilter()
        throws Exception
    {
        GroovyResourceTransformer transformer = new GroovyResourceTransformer();
        assertTrue( transformer.canTransformResource( GroovyResourceTransformer.EXT_MODULE_NAME ) );
        assertFalse( transformer.canTransformResource( "somethingElse" ) );
        assertFalse( transformer.canTransformResource( JarFile.MANIFEST_NAME ) );
    }

    public void testEmpty()
        throws Exception
    {
        GroovyResourceTransformer transformer = new GroovyResourceTransformer();
        assertFalse( transformer.hasTransformedResource() );
        assertNull( transform( transformer ) );
    }

    public void testSpecifyModuleName()
        throws Exception
    {
        GroovyResourceTransformer transformer = new GroovyResourceTransformer();
        transformer.setExtModuleName( "the-module-name" );
        transformer.setExtModuleVersion( "2.0" );
        transformer.processResource( GroovyResourceTransformer.EXT_MODULE_NAME,
                                     module( "mod1", "1.0", "some.ext", "some.staticExt" ),
                                     Collections.<Relocator>emptyList() );
        Properties desc = transform( transformer );
        assertEquals( "the-module-name", desc.getProperty( "moduleName" ) );
        assertEquals( "2.0", desc.getProperty( "moduleVersion" ) );
        assertEquals( "some.ext", desc.getProperty( "extensionClasses" ) );
        assertEquals( "some.staticExt", desc.getProperty( "staticExtensionClasses" ) );
    }

    public void testConcatenation()
        throws Exception
    {
        GroovyResourceTransformer transformer = new GroovyResourceTransformer();
        transformer.processResource( GroovyResourceTransformer.EXT_MODULE_NAME,
                                     module( "mod1", "1.0", "some.ext1", null ), Collections.<Relocator>emptyList() );
        transformer.processResource( GroovyResourceTransformer.EXT_MODULE_NAME,
                                     module( "mod2", "1.0", null, "some.staticExt1" ),
                                     Collections.<Relocator>emptyList() );
        transformer.processResource( GroovyResourceTransformer.EXT_MODULE_NAME, module( "mod3", "1.0", "", "" ),
                                     Collections.<Relocator>emptyList() );
        transformer.processResource( GroovyResourceTransformer.EXT_MODULE_NAME,
                                     module( "mod4", "1.0", "some.ext2", "some.staticExt2" ),
                                     Collections.<Relocator>emptyList() );
        Properties desc = transform( transformer );
        assertEquals( "no-module-name", desc.getProperty( "moduleName" ) );
        assertEquals( "1.0", desc.getProperty( "moduleVersion" ) );
        assertEquals( "some.ext1,some.ext2", desc.getProperty( "extensionClasses" ) );
        assertEquals( "some.staticExt1,some.staticExt2", desc.getProperty( "staticExtensionClasses" ) );
    }
}
