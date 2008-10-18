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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.jdom.Content;

/**
 * Resources transformer that appends entries in META-INF/services resources into
 * a single resource. For example, if there are several META-INF/services/org.apache.maven.project.ProjectBuilder
 * resources spread across many JARs the individual entries will all be concatenated into a single
 * META-INF/services/org.apache.maven.project.ProjectBuilder resource packaged into the resultant JAR produced
 * by the shading process.
 *
 * @author jvanzyl
 */
public class ServicesResourceTransformer
    implements ResourceTransformer
{
    private static final String SERVICES_PATH = "META-INF/services";

    private ByteArrayOutputStream data;

    private Map serviceEntries = new HashMap();

    public boolean canTransformResource( String resource )
    {
        if ( resource.startsWith( SERVICES_PATH ) )
        {
            data = (ByteArrayOutputStream) serviceEntries.get( resource );

            if ( data == null )
            {
                data = new ByteArrayOutputStream();
                serviceEntries.put( resource, data );
            }

            return true;
        }

        return false;
    }

    public void processResource( InputStream is )
        throws IOException
    {
        IOUtil.copy( is, data );
        is.close();
    }

    public boolean hasTransformedResource()
    {
        return serviceEntries.size() > 0;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        for ( Iterator i = serviceEntries.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            ByteArrayOutputStream data = (ByteArrayOutputStream) serviceEntries.get( key );
            jos.putNextEntry( new JarEntry( key ) );
            IOUtil.copy( new ByteArrayInputStream( data.toByteArray() ), jos );
            data.reset();
        }
    }
}
