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

package org.apache.maven.plugins.shade.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;

/**
 * Prevents duplicate copies of the license
 *
 */
public class ApacheLicenseResourceTransformer
    implements ResourceTransformer
{
    Set entries = new HashSet();
    
    public boolean canTransformResource( String resource )
    {
        String s = resource.toLowerCase();

        if ( s.startsWith( "meta-inf/license.txt" ) || s.equals( "meta-inf/license" ))
        {
            return true;
        }

        return false;
    }

    public void processResource( InputStream is )
        throws IOException
    {
       
    }

    public boolean hasTransformedResource()
    {
        return false;
    }

    public void modifyOutputStream( JarOutputStream os )
        throws IOException
    {
    }
}
