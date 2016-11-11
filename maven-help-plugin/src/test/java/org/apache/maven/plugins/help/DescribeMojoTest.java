package org.apache.maven.plugins.help;

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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugins.help.DescribeMojo.PluginInfo;
import org.apache.maven.reporting.exec.MavenPluginManagerHelper;
import org.mockito.ArgumentCaptor;

import junit.framework.Assert;
import junit.framework.TestCase;
import junitx.util.PrivateAccessor;
/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DescribeMojoTest
    extends TestCase
{
    /**
     * Test method for {@link org.apache.maven.plugins.help.DescribeMojo#toLines(java.lang.String, int, int, int)}.
     *
     * @throws Exception if any
     */
    public void testGetExpressionsRoot()
        throws Exception
    {
        try
        {
            PrivateAccessor.invoke( DescribeMojo.class, "toLines", new Class[] { String.class, Integer.TYPE,
                Integer.TYPE, Integer.TYPE }, new Object[] { "", 2, 2,
                    80} );
            assertTrue( true );
        }
        catch ( Throwable e )
        {
            Assert.fail( "The API changes" );
        }
    }
    
    public void testValidExpression()
        throws Exception
    {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName( "name" );
        parameter.setExpression( "${valid.expression}" );
        md.addParameter( parameter );
        
        String ls = System.getProperty( "line.separator" );
        
        try
        {
            PrivateAccessor.invoke( new DescribeMojo(), "describeMojoParameters", new Class[] { MojoDescriptor.class,
                StringBuilder.class }, new Object[] { md, sb } );
            
            assertEquals( "  Available parameters:" + ls
            		      + ls +
            		      "    name" + ls +
            		      "      User property: valid.expression" + ls +
            		      "      (no description available)" + ls, sb.toString() );
        }
        catch ( Throwable e )
        {
            fail( e.getMessage() );
        }
    }
    
    public void testInvalidExpression()
        throws Exception
    {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName( "name" );
        parameter.setExpression( "${project.build.directory}/generated-sources/foobar" ); //this is a defaultValue
        md.addParameter( parameter );
        
        String ls = System.getProperty( "line.separator" );
        
        try
        {
            PrivateAccessor.invoke( new DescribeMojo(), "describeMojoParameters", new Class[] { MojoDescriptor.class,
                StringBuilder.class }, new Object[] { md, sb } );
            
            assertEquals( "  Available parameters:" + ls +
                          ls +
                          "    name" + ls +
                          "      Expression: ${project.build.directory}/generated-sources/foobar" + ls +
                          "      (no description available)" + ls, sb.toString() );
        }
        catch ( Throwable e )
        {
            fail( e.getMessage() );
        }
        
    }
    
    public void testParsePluginInfoGAV()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PrivateAccessor.setField( mojo, "groupId", "org.test" );
        PrivateAccessor.setField( mojo, "artifactId", "test" );
        PrivateAccessor.setField( mojo, "version", "1.0" );
        PluginInfo pi = (PluginInfo) PrivateAccessor.invoke( mojo, "parsePluginLookupInfo", null, null );
        assertEquals( pi.getGroupId(), "org.test" );
        assertEquals( pi.getArtifactId(), "test" );
        assertEquals( pi.getVersion(), "1.0" );
        assertNull( pi.getPrefix() );
    }
    
    public void testParsePluginInfoPluginPrefix()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PrivateAccessor.setField( mojo, "plugin", "help" );
        PluginInfo pi = (PluginInfo) PrivateAccessor.invoke( mojo, "parsePluginLookupInfo", null, null );
        assertNull( pi.getGroupId() );
        assertNull( pi.getArtifactId() );
        assertNull( pi.getVersion() );
        assertEquals( "help", pi.getPrefix() );
        
        PrivateAccessor.setField( mojo, "plugin", "help2:::" );
        pi = (PluginInfo) PrivateAccessor.invoke( mojo, "parsePluginLookupInfo", null, null );
        assertEquals( "help2", pi.getPrefix() );
    }
    
    public void testParsePluginInfoPluginGA()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PrivateAccessor.setField( mojo, "plugin", "org.test:test" );
        PluginInfo pi = (PluginInfo) PrivateAccessor.invoke( mojo, "parsePluginLookupInfo", null, null );
        assertEquals( "org.test", pi.getGroupId() );
        assertEquals( "test", pi.getArtifactId() );
        assertNull( pi.getVersion() );
        assertNull( pi.getPrefix() );
    }
    
    public void testParsePluginInfoPluginGAV()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PrivateAccessor.setField( mojo, "plugin", "org.test:test:1.0" );
        PluginInfo pi = (PluginInfo) PrivateAccessor.invoke( mojo, "parsePluginLookupInfo", null, null );
        assertEquals( "org.test", pi.getGroupId() );
        assertEquals( "test", pi.getArtifactId() );
        assertEquals( "1.0", pi.getVersion() );
        assertNull( pi.getPrefix() );
    }
    
    public void testParsePluginInfoPluginIncorrect()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PrivateAccessor.setField( mojo, "plugin", "org.test:test:1.0:invalid" );
        try
        {
            PrivateAccessor.invoke( mojo, "parsePluginLookupInfo", null, null );
            fail();
        }
        catch ( Exception e )
        {
            // expected
        }
    }
    
    public void testLookupPluginDescriptorPrefixWithVersion()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();

        PluginInfo pi = new PluginInfo();
        pi.setPrefix( "help" );
        pi.setVersion( "1.0" );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.test" );
        plugin.setArtifactId( "test" );
        
        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock( MojoDescriptorCreator.class );
        PluginVersionResolver pluginVersionResolver = mock( PluginVersionResolver.class );
        MavenPluginManagerHelper pluginManager = mock( MavenPluginManagerHelper.class );
        MavenSession session = mock( MavenSession.class );
        PrivateAccessor.setField( mojo, "mojoDescriptorCreator", mojoDescriptorCreator );
        PrivateAccessor.setField( mojo, "pluginVersionResolver", pluginVersionResolver );
        PrivateAccessor.setField( mojo, "pluginManager", pluginManager );
        PrivateAccessor.setField( mojo, "session", session );
        when( mojoDescriptorCreator.findPluginForPrefix( "help", session ) ).thenReturn( plugin );
        when( pluginManager.getPluginDescriptor( any( Plugin.class ), eq( session ) ) ).thenReturn( pd );

        PluginDescriptor returned =
            (PluginDescriptor) PrivateAccessor.invoke( mojo, "lookupPluginDescriptor", new Class[] { PluginInfo.class },
                                                       new Object[] { pi } );
        assertEquals( pd, returned );

        verify( mojoDescriptorCreator ).findPluginForPrefix( "help", session );
        verify( pluginVersionResolver, never() ).resolve( any( PluginVersionRequest.class ) );
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass( Plugin.class );
        verify( pluginManager ).getPluginDescriptor( argument.capture(), eq( session ) );
        Plugin capturedPlugin = argument.getValue();
        assertEquals( "org.test", capturedPlugin.getGroupId() );
        assertEquals( "test", capturedPlugin.getArtifactId() );
        assertEquals( "1.0", capturedPlugin.getVersion() );
    }
    
    public void testLookupPluginDescriptorPrefixWithoutVersion()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();

        PluginInfo pi = new PluginInfo();
        pi.setPrefix( "help" );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.test" );
        plugin.setArtifactId( "test" );
        
        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock( MojoDescriptorCreator.class );
        PluginVersionResolver pluginVersionResolver = mock( PluginVersionResolver.class );
        MavenPluginManagerHelper pluginManager = mock( MavenPluginManagerHelper.class );
        PluginVersionResult versionResult = mock( PluginVersionResult.class );
        MavenSession session = mock( MavenSession.class );
        PrivateAccessor.setField( mojo, "mojoDescriptorCreator", mojoDescriptorCreator );
        PrivateAccessor.setField( mojo, "pluginVersionResolver", pluginVersionResolver );
        PrivateAccessor.setField( mojo, "pluginManager", pluginManager );
        PrivateAccessor.setField( mojo, "session", session );
        when( mojoDescriptorCreator.findPluginForPrefix( "help", session ) ).thenReturn( plugin );
        when( pluginVersionResolver.resolve( any( PluginVersionRequest.class ) ) ).thenReturn( versionResult );
        when( versionResult.getVersion() ).thenReturn( "1.0" );
        when( pluginManager.getPluginDescriptor( any( Plugin.class ), eq( session ) ) ).thenReturn( pd );

        PluginDescriptor returned =
            (PluginDescriptor) PrivateAccessor.invoke( mojo, "lookupPluginDescriptor", new Class[] { PluginInfo.class },
                                                       new Object[] { pi } );
        assertEquals( pd, returned );

        verify( mojoDescriptorCreator ).findPluginForPrefix( "help", session );
        ArgumentCaptor<PluginVersionRequest> versionArgument = ArgumentCaptor.forClass( PluginVersionRequest.class );
        verify( pluginVersionResolver ).resolve( versionArgument.capture() );
        assertEquals( "org.test", versionArgument.getValue().getGroupId() );
        assertEquals( "test", versionArgument.getValue().getArtifactId() );
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass( Plugin.class );
        verify( pluginManager ).getPluginDescriptor( argument.capture(), eq( session ) );
        Plugin capturedPlugin = argument.getValue();
        assertEquals( "org.test", capturedPlugin.getGroupId() );
        assertEquals( "test", capturedPlugin.getArtifactId() );
        assertEquals( "1.0", capturedPlugin.getVersion() );
    }
    
    public void testLookupPluginDescriptorGAV()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();

        PluginInfo pi = new PluginInfo();
        pi.setGroupId( "org.test" );
        pi.setArtifactId( "test" );
        pi.setVersion( "1.0" );

        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock( MojoDescriptorCreator.class );
        PluginVersionResolver pluginVersionResolver = mock( PluginVersionResolver.class );
        MavenPluginManagerHelper pluginManager = mock( MavenPluginManagerHelper.class );
        MavenSession session = mock( MavenSession.class );
        PrivateAccessor.setField( mojo, "mojoDescriptorCreator", mojoDescriptorCreator );
        PrivateAccessor.setField( mojo, "pluginVersionResolver", pluginVersionResolver );
        PrivateAccessor.setField( mojo, "pluginManager", pluginManager );
        PrivateAccessor.setField( mojo, "session", session );
        when( pluginManager.getPluginDescriptor( any( Plugin.class ), eq( session ) ) ).thenReturn( pd );

        PluginDescriptor returned =
            (PluginDescriptor) PrivateAccessor.invoke( mojo, "lookupPluginDescriptor", new Class[] { PluginInfo.class },
                                                       new Object[] { pi } );
        assertEquals( pd, returned );

        verify( mojoDescriptorCreator, never() ).findPluginForPrefix( any( String.class ), any( MavenSession.class ) );
        verify( pluginVersionResolver, never() ).resolve( any( PluginVersionRequest.class ) );
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass( Plugin.class );
        verify( pluginManager ).getPluginDescriptor( argument.capture(), eq( session ) );
        Plugin capturedPlugin = argument.getValue();
        assertEquals( "org.test", capturedPlugin.getGroupId() );
        assertEquals( "test", capturedPlugin.getArtifactId() );
        assertEquals( "1.0", capturedPlugin.getVersion() );
    }

    public void testLookupPluginDescriptorGMissingA()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PluginInfo pi = new PluginInfo();
        pi.setGroupId( "org.test" );
        try
        {
            PrivateAccessor.invoke( mojo, "lookupPluginDescriptor", new Class[] { PluginInfo.class },
                                    new Object[] { pi } );
            fail();
        }
        catch ( Exception e )
        {
            assertTrue( e.getMessage().startsWith( "You must specify either" ) );
        }
    }
    
    public void testLookupPluginDescriptorAMissingG()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        PluginInfo pi = new PluginInfo();
        pi.setArtifactId( "test" );
        try
        {
            PrivateAccessor.invoke( mojo, "lookupPluginDescriptor", new Class[] { PluginInfo.class },
                                    new Object[] { pi } );
            fail();
        }
        catch ( Exception e )
        {
            assertTrue( e.getMessage().startsWith( "You must specify either" ) );
        }
    }

}
