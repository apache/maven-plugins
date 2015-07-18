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
import java.util.zip.*;

ZipFile zf = new ZipFile( new File( basedir, "assembly/target/assembly-1-bin.zip" ) );

ZipEntry child1InclEntry = zf.getEntry( "child1/org/test/child1/App2.class" );

if ( child1InclEntry == null )
{
    System.out.println( "Included file from child1 not found." );
    return false;
}

ZipEntry child1ExclEntry = zf.getEntry( "child1/org/test/child1/App.class" );

if ( child1ExclEntry != null )
{
    System.out.println( "Non-included file from child1 should not be present, but was found." );
    return false;
}

ZipEntry child2InclEntry = zf.getEntry( "child2/org/test/child2/App.class" );

if ( child2InclEntry == null )
{
    System.out.println( "Included file from child2 not found." );
    return false;
}

ZipEntry child2ExclEntry = zf.getEntry( "child2/org/test/other/App.class" );

if ( child2ExclEntry != null )
{
    System.out.println( "Non-included file from child2 should not be present, but was found." );
    return false;
}

return true;
