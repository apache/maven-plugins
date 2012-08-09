package org.apache.maven.report.projectinfo;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;

import java.util.Formatter;
import java.util.Locale;

/**
 * Generates the Dependency code snippets to be added to build tools.
 *
 * @author <a href="mailto:simonetripodi@apache.org">Simone Tripodi</a>
 * @version $Id$
 * @since 2.5
 */
@Mojo( name = "dependency-info" )
public final class DependencyInformationReport
    extends AbstractProjectInfoReport
{

    private static final String DEPENDENCY_INFO = "dependency-info";

    private static final String JAR_PACKAGING = "jar";

    /**
     */
    @Parameter( defaultValue = "${project.groupId}", required = true )
    protected String groupId;

    /**
     */
    @Parameter( defaultValue = "${project.artifactId}", required = true )
    protected String artifactId;

    /**
     */
    @Parameter( defaultValue = "${project.version}", required = true )
    protected String version;

    /**
     */
    @Parameter( defaultValue = "${project.packaging}", required = true )
    protected String packaging;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return DEPENDENCY_INFO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getI18Nsection()
    {
        return DEPENDENCY_INFO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        new DependencyInformationRenderer( getSink(), getI18N( locale ), locale, groupId, artifactId, version,
                                           packaging ).render();
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    private static final class DependencyInformationRenderer
        extends AbstractProjectInfoRenderer
    {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String packaging;

        public DependencyInformationRenderer( Sink sink, I18N i18n, Locale locale, String groupId, String artifactId,
                                              String version, String packaging )
        {
            super( sink, i18n, locale );
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.packaging = packaging;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getI18Nsection()
        {
            return DEPENDENCY_INFO;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void renderBody()
        {
            startSection( getTitle() );

            Formatter mavenDependency =
                new Formatter().format( "<dependency>%n" ).format( "  <groupId>%s</groupId>%n", groupId ).format(
                    "  <artifactId>%s</artifactId>%n", artifactId ).format( "  <version>%s</version>%n", version );

            if ( !JAR_PACKAGING.equals( packaging ) )
            {
                mavenDependency = mavenDependency.format( "  <type>%s</type>%n", packaging );
            }

            renderDependencyInfo( "Apache Maven", mavenDependency.format( "</dependency>" ) );

            renderDependencyInfo( "Apache Buildr",
                                  new Formatter().format( "'%s:%s:%s:%s'", groupId, artifactId, packaging, version ) );

            renderDependencyInfo( "Apache Ant",
                                  new Formatter().format( "<dependency org=\"%s\" name=\"%s\" rev=\"%s\">%n", groupId,
                                                          artifactId, version ).format(
                                      "  <artifact name=\"%s\" type=\"%s\" />%n", artifactId, packaging ).format(
                                      "</dependency>" ) );

            renderDependencyInfo( "Groovy Grape", new Formatter().format( "@Grapes(%n" ).format(
                "@Grab(group='%s', module='%s', version='%s')%n", groupId, artifactId, version ).format( ")" ) );

            renderDependencyInfo( "Grails",
                                  new Formatter().format( "compile '%s:%s:%s'", groupId, artifactId, version ) );

            // Leiningen

            Formatter leiningenDependency = new Formatter().format( "[%s", groupId );

            if ( !groupId.equals( artifactId ) )
            {
                leiningenDependency.format( "/%s", artifactId );
            }

            leiningenDependency.format( " \"%s\"]", version );

            renderDependencyInfo( "Leiningen", leiningenDependency );

            // sbt

            renderDependencyInfo( "SBT", new Formatter().format( "libraryDependencies += \"%s\" %%%% \"%s\" %% \"%s\"",
                                                                 groupId, artifactId, version ) );

            endSection();
        }

        private void renderDependencyInfo( String name, Formatter formatter )
        {
            startSection( name );
            verbatimText( formatter.toString() );
            endSection();
        }

    }

}
