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

import java.io.*;
import java.util.Arrays;
import java.util.jar.*;
import org.codehaus.plexus.util.*;

JarFile jarFile = new JarFile( new File( basedir, "target/mshade-237-1.0.jar" ) );
JarEntry jarEntry = jarFile.getEntry( "META-INF/services/org.apache.maven.shade" );
String service = IOUtil.toString( jarFile.getInputStream( jarEntry ), "UTF-8" );
jarFile.close();

String[] services = service.split( "(\r\n)|(\r)|(\n)" );
String[] expected = { "org.apache.maven.its.shade.One", "shaded.org.apache.maven.its.shade.Two" };

Arrays.sort(services);
Arrays.sort(expected);
if ( !Arrays.equals( services, expected ) )
{
    throw new IllegalStateException( "Different services than expected: " + service );
}
