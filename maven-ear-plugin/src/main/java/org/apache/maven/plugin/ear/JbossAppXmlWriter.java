package org.apache.maven.plugin.ear;

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

import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/**
 * An <tt>XmlWriter</tt> based implementation used to generate a
 * <tt>jboss-app.xml</tt> file
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
final class JbossAppXmlWriter
    extends AbstractXmlWriter
{

    public static final String DOCTYPE_3_2 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD J2EE Application 1.3//EN\"\n" +
        "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_3_2.dtd\"";

    public static final String DOCTYPE_4 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD J2EE Application 1.4//EN\"\n" +
        "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_4_0.dtd\"";

    public static final String DOCTYPE_4_2 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD J2EE Application 1.4//EN\"\n" +
        "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_4_2.dtd\"";

    private static final String JBOSS_APP_ELEMENT = "jboss-app";

    JbossAppXmlWriter( String encoding )
    {
        super( encoding );
    }

    public void write( File destinationFile, JbossConfiguration jbossConfiguration, List earModules )
        throws EarPluginException
    {
        final Writer w = initializeWriter( destinationFile );

        XMLWriter writer;
        if ( jbossConfiguration.isJbossThreeDotTwo() )
        {
            writer = initializeXmlWriter( w, DOCTYPE_3_2 );
        }
        else if ( jbossConfiguration.isJbossFour() )
        {
            writer = initializeXmlWriter( w, DOCTYPE_4 );
        }
        else
        {
            writer = initializeXmlWriter( w, DOCTYPE_4_2 );
        }
        writer.startElement( JBOSS_APP_ELEMENT );

        // If JBoss 4.2, write the jboss4.2 specific stuff
        if ( jbossConfiguration.isJbossFourDotTwo() )
        {
            // module-order
            if ( jbossConfiguration.getModuleOrder() != null )
            {
                writer.startElement( JbossConfiguration.MODULE_ORDER );
                writer.writeText( jbossConfiguration.getModuleOrder() );
                writer.endElement();
            }
        }

        // If JBoss 4, write the jboss4 specific stuff
        if ( jbossConfiguration.isJbossFour() || jbossConfiguration.isJbossFourDotTwo() )
        {
            if ( jbossConfiguration.getSecurityDomain() != null )
            {
                writer.startElement( JbossConfiguration.SECURITY_DOMAIN );
                writer.writeText( jbossConfiguration.getSecurityDomain() );
                writer.endElement();
            }
            if ( jbossConfiguration.getUnauthenticatedPrincipal() != null )
            {
                writer.startElement( JbossConfiguration.UNAUHTHENTICTED_PRINCIPAL );
                writer.writeText( jbossConfiguration.getUnauthenticatedPrincipal() );
                writer.endElement();
            }
        }

        // classloader repository
        if ( jbossConfiguration.getLoaderRepository() != null )
        {
            writer.startElement( JbossConfiguration.LOADER_REPOSITORY );
            writer.writeText( jbossConfiguration.getLoaderRepository() );
            writer.endElement();
        }

        // jmx name
        if ( jbossConfiguration.getJmxName() != null )
        {
            writer.startElement( JbossConfiguration.JMX_NAME );
            writer.writeText( jbossConfiguration.getJmxName() );
            writer.endElement();
        }

        List dataSources = jbossConfiguration.getDataSources();
        // Write out data source modules first
        if ( dataSources != null )
        {
            final Iterator it = dataSources.iterator();
            while ( it.hasNext() )
            {
                String dsPath = (String) it.next();
                writer.startElement( MODULE_ELEMENT );
                writer.startElement( SERVICE_ELEMENT );
                writer.writeText( dsPath );
                writer.endElement();
                writer.endElement();
            }
        }

        // Write the JBoss specific modules
        final Iterator it = earModules.iterator();
        while ( it.hasNext() )
        {
            EarModule earModule = (EarModule) it.next();
            if ( JbossEarModule.class.isInstance( earModule ) )
            {
                JbossEarModule jbossEarModule = (JbossEarModule) earModule;
                jbossEarModule.appendJbossModule( writer, jbossConfiguration.getVersion() );
            }
        }
        writer.endElement();

        close( w );
    }
}
