package org.apache.maven.plugin.ear.util;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import junit.framework.TestCase;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.apache.maven.plugin.ear.util.ArtifactTypeMappingService;
import org.apache.maven.plugin.ear.EarModuleFactory;
import org.apache.maven.plugin.ear.UnknownArtifactTypeException;
import org.apache.maven.plugin.ear.EarPluginException;

import java.util.Iterator;

/**
 * Tests for the {@link ArtifactTypeMappingService}
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: AbstractEarModule.java 314956 2005-10-12 18:27:15 +0200 (Wed, 12 Oct 2005) brett $
 */
public class ArtifactTypeMappingServiceTest
    extends TestCase
{

    public void testDefaultConfiguration()
    {
        ArtifactTypeMappingService service = getDefaultService();
        final Iterator it = EarModuleFactory.getStandardArtifactTypes().iterator();
        while ( it.hasNext() )
        {
            String type = (String) it.next();
            assertTrue( "Standard type could not be found", service.isMappedToType( type, type ) );
        }
    }

    public void testIsMappedToTypeForUnknownType()
    {
        ArtifactTypeMappingService service = getDefaultService();
        assertFalse( service.isMappedToType( "rar", "MyKoolCustomType" ) );
    }

    public void testIsMappedToTypeForKnownType()
    {
        ArtifactTypeMappingService service = getServiceWithRarMappingToMyRar();
        assertTrue( service.isMappedToType( "rar", "MyRar" ) );
    }

    public void testGetStandardTypeForUknonwnType()
    {
        try
        {
            ArtifactTypeMappingService service = getDefaultService();
            service.getStandardType( "MyKoolCustomType" );
            fail( "Should have failed to retrieve a unknwon custom type" );
        }
        catch ( UnknownArtifactTypeException e )
        {
            // That's good
        }
    }

    public void testGetStandardTypeForKnownType()
    {
        try
        {
            ArtifactTypeMappingService service = getServiceWithRarMappingToMyRar();
            assertEquals( "rar", service.getStandardType( "MyRar" ) );
        }
        catch ( UnknownArtifactTypeException e )
        {
            fail( "Should not have failed to retrieve a knwon custom type " + e.getMessage() );
        }
    }

    public void testConfigWithSameCustomType()
    {
        try
        {
            XmlPlexusConfiguration rootConfig = new XmlPlexusConfiguration( "dummy" );
            XmlPlexusConfiguration childConfig =
                new XmlPlexusConfiguration( ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT );
            childConfig.setAttribute( "type", "generic" );
            childConfig.setAttribute( "mapping", "rar" );
            XmlPlexusConfiguration childConfig2 =
                new XmlPlexusConfiguration( ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT );
            childConfig.setAttribute( "type", "generic" );
            childConfig.setAttribute( "mapping", "ejb" );

            rootConfig.addChild( childConfig );
            rootConfig.addChild( childConfig2 );
            ArtifactTypeMappingService service = ArtifactTypeMappingService.getInstance();
            service.configure( rootConfig );
            fail( "Should have failed" );
        }
        catch ( EarPluginException e )
        {
            //OK
        }
        catch ( PlexusConfigurationException e )
        {
            e.printStackTrace();
            fail( "Unexpected " + e.getMessage() );
        }
    }

    public void testConfigWithUnknownStandardType()
    {
        try
        {
            XmlPlexusConfiguration rootConfig = new XmlPlexusConfiguration( "dummy" );
            XmlPlexusConfiguration childConfig =
                new XmlPlexusConfiguration( ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT );
            childConfig.setAttribute( "type", "generic" );
            childConfig.setAttribute( "mapping", "notAStandardType" );

            rootConfig.addChild( childConfig );
            ArtifactTypeMappingService service = ArtifactTypeMappingService.getInstance();
            service.configure( rootConfig );
            fail( "Should have failed" );
        }
        catch ( EarPluginException e )
        {
            //OK
        }
        catch ( PlexusConfigurationException e )
        {
            e.printStackTrace();
            fail( "Unexpected " + e.getMessage() );
        }
    }

    public void testConfigWithNoType()
    {
        try
        {
            XmlPlexusConfiguration rootConfig = new XmlPlexusConfiguration( "dummy" );
            XmlPlexusConfiguration childConfig =
                new XmlPlexusConfiguration( ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT );
            childConfig.setAttribute( "mapping", "ejb" );

            rootConfig.addChild( childConfig );
            ArtifactTypeMappingService service = ArtifactTypeMappingService.getInstance();
            service.configure( rootConfig );
            fail( "Should have failed" );
        }
        catch ( EarPluginException e )
        {
            //OK
        }
        catch ( PlexusConfigurationException e )
        {
            e.printStackTrace();
            fail( "Unexpected " + e.getMessage() );
        }
    }

    public void testConfigWithNoMapping()
    {
        try
        {
            XmlPlexusConfiguration rootConfig = new XmlPlexusConfiguration( "dummy" );
            XmlPlexusConfiguration childConfig =
                new XmlPlexusConfiguration( ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT );
            childConfig.setAttribute( "type", "generic" );

            rootConfig.addChild( childConfig );
            ArtifactTypeMappingService service = ArtifactTypeMappingService.getInstance();
            service.configure( rootConfig );
            fail( "Should have failed" );
        }
        catch ( EarPluginException e )
        {
            //OK
        }
        catch ( PlexusConfigurationException e )
        {
            e.printStackTrace();
            fail( "Unexpected " + e.getMessage() );
        }
    }

    // Utilities

    protected ArtifactTypeMappingService getServiceWithRarMappingToMyRar()
    {
        try
        {
            XmlPlexusConfiguration rootConfig = new XmlPlexusConfiguration( "artifact-type-mappings" );
            XmlPlexusConfiguration childConfig =
                new XmlPlexusConfiguration( ArtifactTypeMappingService.ARTIFACT_TYPE_MAPPING_ELEMENT );
            childConfig.setAttribute( "type", "MyRar" );
            childConfig.setAttribute( "mapping", "rar" );
            rootConfig.addChild( childConfig );
            ArtifactTypeMappingService service = ArtifactTypeMappingService.getInstance();
            service.configure( rootConfig );

            return service;
        }
        catch ( EarPluginException e )
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        catch ( PlexusConfigurationException e )
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        // Won't occur
        return null;


    }

    protected ArtifactTypeMappingService getDefaultService()
    {
        try
        {
            ArtifactTypeMappingService service = ArtifactTypeMappingService.getInstance();
            service.configure( new XmlPlexusConfiguration( "dummy" ) );

            return service;
        }
        catch ( EarPluginException e )
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        catch ( PlexusConfigurationException e )
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        // Won't occur
        return null;
    }
}
