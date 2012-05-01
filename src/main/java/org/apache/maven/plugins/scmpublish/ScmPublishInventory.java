package org.apache.maven.plugins.scmpublish;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.MappingJsonFactory;

/**
 * A class designed for json serialization to store the existing inventory, if any. In this version, there's no attempt
 * to account for directories as managed items.
 */
public class ScmPublishInventory
{
    private static class DotFilter
        implements IOFileFilter
    {

        public boolean accept( File file )
        {
            return !file.getName().startsWith( "." );
        }

        public boolean accept( File dir, String name )
        {
            return !name.startsWith( "." );
        }

    }

    public static List<String> listInventory( File basedir )
    {
        return Arrays.asList( basedir.list( new DotFilter() ) );
    }

    public static List<File> listInventoryFiles( File basedir )
    {
        List<File> inventory = new ArrayList<File>();
        inventory.addAll( FileUtils.listFiles( basedir, new DotFilter(), new DotFilter() ) );
        Collections.sort( inventory );
        return inventory;
    }

    /**
     * Create a list of all the files in the checkout (which we will presently remove). For now, duck anything that
     * starts with a ., since the site plugin won't make any and it will dodge metadata I'm familiar with. None if this
     * is really good enough for safe usage with exotics like clearcase. Perhaps protest if anything other than svn or
     * git? Or use http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/AbstractScanner.html#DEFAULTEXCLUDES?
     * @throws MojoFailureException 
     */
    public static List<File> writeInventory( List<File> inventory, File inventoryFile )
        throws MojoFailureException
    {
        Set<String> paths = new HashSet<String>();

        /*
         * It might be cleverer to store paths relative to the checkoutDirectory, but this really should work.
         */
        for ( File f : inventory )
        {
            // See below. We only bother about files.
            if ( f.isFile() )
            {
                paths.add( f.getAbsolutePath() );
            }
        }
        try
        {
            MappingJsonFactory factory = new MappingJsonFactory();
            JsonGenerator gen = factory.createJsonGenerator( inventoryFile, JsonEncoding.UTF8 );
            gen.writeObject( paths );
            gen.close();
            return inventory;
        }
        catch ( JsonProcessingException e )
        {
            throw new MojoFailureException( "Failed to write inventory to " + inventoryFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to write inventory to " + inventoryFile.getAbsolutePath(), e );
        }
    }

    public static List<File> readInventory( File inventoryFile )
        throws MojoFailureException
    {
        try
        {
            MappingJsonFactory factory = new MappingJsonFactory();
            JsonParser parser = factory.createJsonParser( inventoryFile );
            @SuppressWarnings( "unchecked" )
            Set<String> storedInventory = parser.readValueAs( HashSet.class );
            List<File> inventory = new ArrayList<File>();
            for ( String p : storedInventory )
            {
                inventory.add( new File( p ) );
            }
            parser.close();
            return inventory;
        }
        catch ( JsonProcessingException e )
        {
            throw new MojoFailureException( "Failed to write inventory to " + inventoryFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to write inventory to " + inventoryFile.getAbsolutePath(), e );
        }  
    }
}
