package org.apache.maven.plugin.dependency.testUtils;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

/**
 * 
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.dependency.utils.filters.AbstractArtifactFeatureFilter;
import org.apache.maven.plugin.logging.Log;

/**
 * Abstract test case for subclasses of AbstractArtifactFeatureFilter
 * 
 * @author clove
 * @see junit.framework.TestCase
 * @see org.apache.maven.plugin.dependency.utils.filters.AbstractArtifactFeatureFilter
 * @since 2.0
 */
public abstract class AbstractArtifactFeatureFilterTestCase
    extends TestCase
{
    protected Set artifacts = new HashSet();

    Log log = new SilentLog();

    protected Class filterClass;

    protected void setUp()
        throws Exception
    {
        super.setUp();

    }

    private Object createObjectViaReflection( Class clazz, Object[] conArgs )
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Class[] argslist = new Class[2];
        argslist[0] = String.class;
        argslist[1] = String.class;
        Constructor ct = clazz.getConstructor( argslist );
        return ct.newInstance( conArgs );
    }

    public abstract void testParsing()
        throws Exception;

    public void parsing()
        throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Object[] conArgs = new Object[] { "one,two", "three,four," };

        AbstractArtifactFeatureFilter filter = (AbstractArtifactFeatureFilter) createObjectViaReflection( filterClass,
                                                                                                          conArgs );
        List includes = filter.getIncludes();
        List excludes = filter.getExcludes();

        assertEquals( 2, includes.size() );
        assertEquals( 2, excludes.size() );
        assertEquals( "one", includes.get( 0 ).toString() );
        assertEquals( "two", includes.get( 1 ).toString() );
        assertEquals( "three", excludes.get( 0 ).toString() );
        assertEquals( "four", excludes.get( 1 ).toString() );
    }

    public abstract void testFiltering()
        throws Exception;

    public Set filtering()
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Object[] conArgs = new Object[] { "one,two", "one,three," };
        AbstractArtifactFeatureFilter filter = (AbstractArtifactFeatureFilter) createObjectViaReflection( filterClass,
                                                                                                          conArgs );
        Set result = filter.filter( artifacts, log );
        assertEquals( 2, result.size() );
        return result;
    }

    public abstract void testFiltering2()
        throws Exception;

    public Set filtering2()
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Object[] conArgs = new Object[] { null, "one,three," };
        AbstractArtifactFeatureFilter filter = (AbstractArtifactFeatureFilter) createObjectViaReflection( filterClass,
                                                                                                          conArgs );
        Set result = filter.filter( artifacts, log );
        assertEquals( 2, result.size() );
        return result;

    }

    public abstract void testFiltering3()
        throws Exception;

    public void filtering3()
        throws SecurityException, IllegalArgumentException, NoSuchMethodException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Object[] conArgs = new Object[] { null, null };
        AbstractArtifactFeatureFilter filter = (AbstractArtifactFeatureFilter) createObjectViaReflection( filterClass,
                                                                                                          conArgs );
        Set result = filter.filter( artifacts, log );
        assertEquals( 4, result.size() );
    }
}
