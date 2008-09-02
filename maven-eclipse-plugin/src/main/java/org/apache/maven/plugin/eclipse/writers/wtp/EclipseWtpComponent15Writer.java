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
package org.apache.maven.plugin.eclipse.writers.wtp;

import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Component writer for WTP 1.5. File name has changed in WTP 1.5rc2 and the <code>project-version</code> attribute has
 * been added. These ones are the only differences
 * 
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EclipseWtpComponent15Writer
    extends EclipseWtpComponentWriter
{

    /**
     * File name where the WTP component settings will be stored for our Eclipse Project.
     * 
     * @return <code>org.eclipse.wst.common.component</code>
     */
    protected String getComponentFileName()
    {
        return "org.eclipse.wst.common.component"; //$NON-NLS-1$
    }

    /**
     * Version number added to component configuration.
     * 
     * @return <code>1.0</code>
     */
    protected String getProjectVersion()
    {
        if ( this.config.getWtpVersion() < 2.0f )
        {
            return "1.5.0"; //$NON-NLS-1$
        }
        else
        {
            return "2.0"; //$NON-NLS-1$
        }
    }

    /**
     * @param writer
     */
    protected void writeContextRoot( XMLWriter writer )
    {
        writer.startElement( ELT_PROPERTY );
        writer.addAttribute( ATTR_NAME, ATTR_CONTEXT_ROOT );
        writer.addAttribute( ATTR_VALUE, config.getContextName() );
        writer.endElement(); // property
    }

}
