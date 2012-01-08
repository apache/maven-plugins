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

import java.io.File;
import java.io.Writer;
import java.util.List;

import org.codehaus.plexus.util.xml.XMLWriter;

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

    public static final String DOCTYPE_3_2 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD J2EE Application 1.3//EN\"\n"
        + "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_3_2.dtd\"";

    public static final String DOCTYPE_4 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD J2EE Application 1.4//EN\"\n"
        + "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_4_0.dtd\"";

    public static final String DOCTYPE_4_2 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD J2EE Application 1.4//EN\"\n"
        + "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_4_2.dtd\"";

    public static final String DOCTYPE_5 = "jboss-app PUBLIC\n" + "\t\"-//JBoss//DTD Java EE Application 5.0//EN\"\n"
        + "\t\"http://www.jboss.org/j2ee/dtd/jboss-app_5_0.dtd\"";

    private static final String JBOSS_APP_ELEMENT = "jboss-app";

    JbossAppXmlWriter( String encoding )
    {
        super( encoding );
    }

    public void write( File destinationFile, JbossConfiguration jbossConfiguration, List<EarModule> earModules )
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
        else if ( jbossConfiguration.isJbossFourDotTwo() )
        {
            writer = initializeXmlWriter( w, DOCTYPE_4_2 );
        }
        else
        {
            writer = initializeXmlWriter( w, DOCTYPE_5 );
        }
        writer.startElement( JBOSS_APP_ELEMENT );

        // Make sure to write the things in the right order so that the DTD validates

        // module-order (only available as from 4.2)
        if ( jbossConfiguration.isJbossFourDotTwoOrHigher() && jbossConfiguration.getModuleOrder() != null )
        {
            writer.startElement( JbossConfiguration.MODULE_ORDER );
            writer.writeText( jbossConfiguration.getModuleOrder() );
            writer.endElement();
        }

        // If JBoss 4, write the jboss4 specific stuff
        if ( jbossConfiguration.isJbossFourOrHigher() )
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
        if ( jbossConfiguration.getLoaderRepository() != null
            || jbossConfiguration.getLoaderRepositoryConfig() != null )
        {
            writer.startElement( JbossConfiguration.LOADER_REPOSITORY );

            // classloader repository class
            if ( jbossConfiguration.getLoaderRepositoryClass() != null )
            {
                writer.addAttribute( JbossConfiguration.LOADER_REPOSITORY_CLASS_ATTRIBUTE,
                                     jbossConfiguration.getLoaderRepositoryClass() );
            }

            // we don't need to write any text if only the loader repo configuration is changed
            if ( jbossConfiguration.getLoaderRepository() != null )
            {
                writer.writeText( jbossConfiguration.getLoaderRepository() );
            }

            // classloader configuration
            if ( jbossConfiguration.getLoaderRepositoryConfig() != null )
            {
                writer.startElement( JbossConfiguration.LOADER_REPOSITORY_CONFIG );

                // classloader configuration parser
                if ( jbossConfiguration.getConfigParserClass() != null )
                {
                    writer.addAttribute( JbossConfiguration.CONFIG_PARSER_CLASS_ATTRIBUTE,
                                         jbossConfiguration.getConfigParserClass() );
                }
                writer.writeText( jbossConfiguration.getLoaderRepositoryConfig() );
                writer.endElement();
            }

            writer.endElement();
        }

        // jmx name
        if ( jbossConfiguration.getJmxName() != null )
        {
            writer.startElement( JbossConfiguration.JMX_NAME );
            writer.writeText( jbossConfiguration.getJmxName() );
            writer.endElement();
        }

        // library-directory (only available as from 4.2)
        if ( jbossConfiguration.isJbossFourDotTwoOrHigher() && jbossConfiguration.getLibraryDirectory() != null )
        {
            writer.startElement( JbossConfiguration.LIBRARY_DIRECTORY );
            writer.writeText( jbossConfiguration.getLibraryDirectory() );
            writer.endElement();
        }

        // Modules

        List<String> dataSources = jbossConfiguration.getDataSources();
        // Write out data source modules first
        if ( dataSources != null )
        {
            for ( String dsPath : dataSources )
            {
                writer.startElement( MODULE_ELEMENT );
                writer.startElement( SERVICE_ELEMENT );
                writer.writeText( dsPath );
                writer.endElement();
                writer.endElement();
            }
        }

        // Write the JBoss specific modules
        for ( EarModule earModule : earModules )
        {
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