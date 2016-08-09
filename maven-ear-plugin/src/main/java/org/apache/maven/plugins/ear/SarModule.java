package org.apache.maven.plugins.ear;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * The {@link EarModule} implementation for a JBoss sar module.
 * 
 * @author Stephane Nicoll <snicoll@apache.org>
 * @author $Author: khmarbaise $ (last edit)
 * @version $Revision: 1645331 $
 */
/**
 * @author kama
 *
 */
public class SarModule
    extends AbstractEarModule
    implements JbossEarModule
{
    private static final String SAR_MODULE = "connector";

    /**
     * Create an instance.
     */
    public SarModule()
    {
    }

    /**
     * @param a {@link Artifact}
     */
    public SarModule( Artifact a )
    {
        super( a );
    }

    /**
     * {@inheritDoc}
     */
    public void appendModule( XMLWriter writer, String version, Boolean generateId )
    {
        // If JBoss is not configured, add the module as a connector element
        if ( !earExecutionContext.isJbossConfigured() )
        {
            startModuleElement( writer, generateId );
            writer.startElement( SAR_MODULE );
            writer.writeText( getUri() );
            writer.endElement();
            writer.endElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void appendJbossModule( XMLWriter writer, String version )
    {
        writer.startElement( MODULE_ELEMENT );
        writer.startElement( "service" );
        writer.writeText( getUri() );
        writer.endElement();
        writer.endElement();
    }

    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return "sar";
    }
}
