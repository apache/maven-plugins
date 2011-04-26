package org.apache.maven.plugin.ear.util;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.InputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class ResourceEntityResolver
    implements EntityResolver
{
    public InputSource resolveEntity( String publicId, String systemId )
    {
        String dtd = "/dtd" + systemId.substring( systemId.lastIndexOf( '/' ) );
        InputStream in = ResourceEntityResolver.class.getResourceAsStream( dtd );
        if ( in == null )
        {
            throw new RuntimeException( "unable to load DTD " + dtd + " for " + systemId );
        }
        else
        {
            return new InputSource( in );
        }
    }
}
