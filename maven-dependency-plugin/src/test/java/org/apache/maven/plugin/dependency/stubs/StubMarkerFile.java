/*
 * Copyright Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.plugin.dependency.stubs;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class StubMarkerFile
    extends File
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public StubMarkerFile( String pathname )
    {
        super( pathname );
        // TODO Auto-generated constructor stub
    }

    public StubMarkerFile( URI uri )
    {
        super( uri );
        // TODO Auto-generated constructor stub
    }

    public StubMarkerFile( File parent, String child )
    {
        super( parent, child );
        // TODO Auto-generated constructor stub
    }

    public StubMarkerFile( String parent, String child )
    {
        super( parent, child );
        // TODO Auto-generated constructor stub
    }

    public boolean createNewFile()
        throws IOException
    {
        throw new IOException( "Intended Error" );
    }
}
