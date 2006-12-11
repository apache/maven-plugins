package org.apache.maven.plugin.ear.output;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to {@link FileNameMapping} implementations.
 * <p/>
 * Two basic implementations are provided by default:
 * <ul>
 * <li>standard: the default implementation</li>
 * <li>full: an implementation that maps to a 'full' file name, i.e. containing the groupId</li>
 * </ul>
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class FileNameMappingFactory
{
    public static final String STANDARD_FILE_NAME_MAPPING = "standard";

    public static final String FULL_FILE_NAME_MAPPING = "full";

    public static final FileNameMappingFactory INSTANCE = new FileNameMappingFactory();

    private final Map mappings;

    private FileNameMappingFactory()
    {
        mappings = new HashMap();
        mappings.put( STANDARD_FILE_NAME_MAPPING, new StandardFileNameMapping() );
        mappings.put( FULL_FILE_NAME_MAPPING, new FullFileNameMapping() );
    }

    public FileNameMapping getDefaultFileNameMapping()
    {
        return getFileNameMapping( STANDARD_FILE_NAME_MAPPING );
    }

    /**
     * Returns the file name mapping implementation based on a logical name
     * of a fully qualified name of the class.
     *
     * @param nameOrClass a name of the fqn of the implementation
     * @return the file name mapping implementation
     * @throws IllegalStateException if the implementation is not found
     */
    public FileNameMapping getFileNameMapping( final String nameOrClass )
        throws IllegalStateException
    {
        // Check if it's there yet
        if ( mappings.containsKey( nameOrClass ) )
        {
            return (FileNameMapping) mappings.get( nameOrClass );
        }
        else
        {
            try
            {
                final Class c = Class.forName( nameOrClass );
                final FileNameMapping fnm = (FileNameMapping) c.newInstance();
                mappings.put( nameOrClass, fnm );
                return fnm;
            }
            catch ( ClassNotFoundException e )
            {
                throw new IllegalStateException(
                    "File name mapping implementation[" + nameOrClass + "] was not found " + e.getMessage() );
            }
            catch ( InstantiationException e )
            {
                throw new IllegalStateException( "Could not instanciate file name mapping implementation[" +
                    nameOrClass + "] make sure it has a default public constructor" );
            }
            catch ( IllegalAccessException e )
            {
                throw new IllegalStateException( "Could not access file name mapping implementation[" + nameOrClass +
                    "] make sure it has a default public constructor" );
            }
            catch ( ClassCastException e )
            {
                throw new IllegalStateException( "Specified class[" + nameOrClass + "] does not implement[" +
                    FileNameMapping.class.getName() + "]" );
            }
        }
    }
}
