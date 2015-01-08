
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
import org.codehaus.plexus.util.*;

boolean result = true;

try
{
    File manifest = new File( basedir, "target/MWAR-167-1.0-SNAPSHOT/META-INF/MANIFEST.MF" );
    if ( !manifest.exists() || !manifest.isFile() )
    {
        System.err.println( "Manifest is missing!");
        return false;
    }
    

    FileInputStream fis = new FileInputStream ( manifest );
    String manifestContent = IOUtil.toString ( fis );
    
    int indexOf = manifestContent.indexOf("Manifest-Version: 1.0" );
    if ( indexOf < 0)
    {
    	System.err.println( "Manifest-Version header not found" );
    	return false;
    }

	indexOf = manifestContent.indexOf("Bundle-Name: Dummy Bundle" );
    if ( indexOf < 0)
    {
    	System.err.println( "Bundle-Name header not found" );
    	return false;
    }

	indexOf = manifestContent.indexOf("Bundle-SymbolicName: dummy.bundle" );
    if ( indexOf < 0)
    {
    	System.err.println( "Bundle-SymbolicName: 2" );
    	return false;
    }

	indexOf = manifestContent.indexOf("Bundle-Version: 1.0.0.SNAPSHOT" );
    if ( indexOf < 0)
    {
    	System.err.println( "Bundle-Version header not found" );
    	return false;
    }
    
}
catch( Throwable e )
{
    e.printStackTrace();
    result = false;
}

return result;
