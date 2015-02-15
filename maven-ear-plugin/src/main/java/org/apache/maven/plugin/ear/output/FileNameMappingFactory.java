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
    static final String STANDARD_FILE_NAME_MAPPING = "standard";

    static final String FULL_FILE_NAME_MAPPING = "full";

    static final String NO_VERSION_FILE_NAME_MAPPING = "no-version";

    static final String NO_VERSION_FOR_EJB_FILE_NAME_MAPPING = "no-version-for-ejb";

    private FileNameMappingFactory()
    {
    }

    /**
     * @return {@link StandardFileNameMapping}
     */
    public static FileNameMapping getDefaultFileNameMapping()
    {
        return new StandardFileNameMapping();
    }

    /**
     * Returns the file name mapping implementation based on a logical name of a fully qualified name of the class.
     * 
     * @param nameOrClass a name of the fqn of the implementation
     * @return the file name mapping implementation
     */
    public static FileNameMapping getFileNameMapping( final String nameOrClass )
    {
        if ( STANDARD_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return getDefaultFileNameMapping();
        }
        if ( FULL_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new FullFileNameMapping();
        }
        if ( NO_VERSION_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new NoVersionFileNameMapping();
        }
        if ( NO_VERSION_FOR_EJB_FILE_NAME_MAPPING.equals( nameOrClass ) )
        {
            return new NoVersionForEjbFileNameMapping();
        }
        try
        {
            final Class<?> c = Class.forName( nameOrClass );
            return (FileNameMapping) c.newInstance();
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalStateException( "File name mapping implementation[" + nameOrClass + "] was not found "
                + e.getMessage() );
        }
        catch ( InstantiationException e )
        {
            throw new IllegalStateException( "Could not instantiate file name mapping implementation[" + nameOrClass
                + "] make sure it has a default public constructor" );
        }
        catch ( IllegalAccessException e )
        {
            throw new IllegalStateException( "Could not access file name mapping implementation[" + nameOrClass
                + "] make sure it has a default public constructor" );
        }
        catch ( ClassCastException e )
        {
            throw new IllegalStateException( "Specified class[" + nameOrClass + "] does not implement["
                + FileNameMapping.class.getName() + "]" );
        }
    }
}
