package org.apache.maven.plugin.ear;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * The {@link EarModule} implementation for an EJB module.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class EjbModule
    extends AbstractEarModule
{
    protected static final String EJB_MODULE = "ejb";

    public EjbModule()
    {
    }

    public EjbModule( Artifact a )
    {
        super( a );
    }

    public void appendModule( XMLWriter writer, String version )
    {
        writer.startElement( MODULE_ELEMENT );
        writer.startElement( EJB_MODULE );
        writer.writeText( getUri() );
        writer.endElement();
        writer.endElement();
    }

    public String getType()
    {
        return "ejb";
    }
}
