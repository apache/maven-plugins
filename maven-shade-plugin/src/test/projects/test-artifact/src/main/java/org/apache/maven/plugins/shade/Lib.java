package org.apache.maven.plugins.shade;

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


/**
 * Hello world!
 *
 */
public class Lib
{
    // simulate the type of call for static loggers that currently fails
    private static final String name = new String( Lib.class.getName() );

    public static final String CONSTANT = "foo.bar/baz";

    // constant shouldn't be changed if "org.codehaus.plexus.util.xml.pull.*" is excluded from relocation.
    public static final String CLASS_REALM_PACKAGE_IMPORT = "org.codehaus.plexus.util.xml.pull";

    public static String getClassRealmPackageImport()
    {
        // argument shouldn't be changed if "org.codehaus.plexus.util.xml.pull.*" is excluded from relocation.
        return importFrom( "org.codehaus.plexus.util.xml.pull" );
    }

    private static String importFrom( String packageName )
    {
        return packageName;
    }
}
