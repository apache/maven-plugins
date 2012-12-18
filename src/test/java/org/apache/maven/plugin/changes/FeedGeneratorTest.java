package org.apache.maven.plugin.changes;

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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.maven.plugins.changes.model.Release;

/**
 *
 * @author ltheussl
 */
public class FeedGeneratorTest
    extends TestCase
{
    /**
     * Test of isSupportedFeedType method, of class FeedGenerator.
     */
    public void testIsSupportedFeedType()
    {
        final FeedGenerator generator = new FeedGenerator( Locale.ENGLISH );

        assertTrue( "rss_0.9 not supported?", generator.isSupportedFeedType( "rss_0.9" ) );
        assertTrue( "rss_0.91N not supported?", generator.isSupportedFeedType( "rss_0.91N" ) );
        assertTrue( "rss_0.91U not supported?", generator.isSupportedFeedType( "rss_0.91U" ) );
        assertTrue( "rss_0.92 not supported?", generator.isSupportedFeedType( "rss_0.92" ) );
        assertTrue( "rss_0.93 not supported?", generator.isSupportedFeedType( "rss_0.93" ) );
        assertTrue( "rss_0.94 not supported?", generator.isSupportedFeedType( "rss_0.94" ) );
        assertTrue( "rss_1.0 not supported?", generator.isSupportedFeedType( "rss_1.0" ) );
        assertTrue( "rss_2.0 not supported?", generator.isSupportedFeedType( "rss_2.0" ) );
        assertTrue( "atom_0.3 not supported?", generator.isSupportedFeedType( "atom_0.3" ) );
        assertTrue( "atom_1.0 not supported?", generator.isSupportedFeedType( "atom_1.0" ) );

        assertFalse( generator.isSupportedFeedType( "" ) );
        assertFalse( generator.isSupportedFeedType( null ) );
        assertFalse( generator.isSupportedFeedType( "rss" ) );
    }

    /**
     * Test of export method, of class FeedGenerator.
     *
     * @throws Exception if any.
     */
    public void testExport()
        throws Exception
    {
        final FeedGenerator generator = new FeedGenerator( Locale.ENGLISH );
        generator.setAuthor( "author" );
        generator.setTitle( "title" );
        generator.setLink( "url" );
        generator.setDateFormat( null );

        Release release = new Release();
        release.setVersion( "1.0" );
        List<Release> releases = new ArrayList<Release>( 1 );

        try
        {
            // test with no release: should fail
            generator.export( releases, "rss_0.9", new StringWriter( 512 ) );
            fail( "0 releases not allowed!" );
        }
        catch ( IOException ex )
        {
            assertNotNull( ex );
        }

        releases.add( release );

        for ( String type: generator.getSupportedFeedTypes() )
        {
            Writer writer = new StringWriter( 512 );
            generator.export( releases, type, writer );
            String result = writer.toString(); // TODO: save for inspection?
            assertNotNull( result );
            assertTrue( result.length() > 0 );
            writer.close();
        }
    }
}
