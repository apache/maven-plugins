package org.apache.maven.report.projectinfo;

import java.util.Formatter;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;

/**
 * Generates the Dependency code snippets to be added to build tools.
 *
 * @author <a href="mailto:simonetripodi@apache.org">Simone Tripodi</a>
 * @version $Id$
 * @since 2.4.1
 * @goal dependency-info
 */
public final class DependencyInformationReport
    extends AbstractProjectInfoReport
{

    private static final String DEPENDNECY_INFO = "dependency-info";

    /**
     * @parameter default-value="${project.groupId}"
     * @required
     */
    protected String groupId;

    /**
     * @parameter default-value="${project.artifactId}"
     * @required
     */
    protected String artifactId;

    /**
     * @parameter default-value="${project.version}"
     * @required
     */
    protected String version;

    /**
     * @parameter default-value="${project.packaging}"
     * @required
     */
    protected String packaging;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return DEPENDNECY_INFO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getI18Nsection()
    {
        return DEPENDNECY_INFO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        new DependencyInformationRenderer( getSink(), getI18N( locale ), locale,
                                           groupId, artifactId, version, packaging )
            .render();
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    private static final class  DependencyInformationRenderer
        extends AbstractProjectInfoRenderer
    {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final String packaging;

        public DependencyInformationRenderer( Sink sink, I18N i18n, Locale locale,
                                              String groupId, String artifactId, String version, String packaging )
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
            return DEPENDNECY_INFO;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void renderBody()
        {
            startSection( getTitle() );

            renderDependencyInfo( "Apache Maven", new Formatter()
                                                        .format( "<dependency>%n" )
                                                        .format( "  <groupId>%s</groupId>%n", groupId )
                                                        .format( "  <artifactId>%s</artifactId>%n", artifactId )
                                                        .format( "  <version>%s</version>%n", version )
                                                        .format( "  <packaging>%s</packaging>%n", packaging )
                                                        .format( "</dependency>" ) );

            renderDependencyInfo( "Apache Buildr", new Formatter().format( "'%s:%s:%s:%s'",
                                                                           groupId, artifactId, packaging, version ) );

            renderDependencyInfo( "Apache Ant", new Formatter()
                                                        .format( "<dependency org=\"%s\" name=\"%s\" rev=\"%s\">%n",
                                                                 groupId, artifactId, version )
                                                        .format( "  <artifact name=\"%s\" type=\"%s\" />%n",
                                                                 artifactId, packaging )
                                                        .format( "</dependency>" ) );

            renderDependencyInfo( "Groovy Grape", new Formatter()
                                                        .format( "@Grapes(%n" )
                                                        .format( "@Grab(group='%s', module='%s', version='%s')%n",
                                                                 groupId,
                                                                 artifactId,
                                                                 version )
                                                        .format( ")" ) );

            renderDependencyInfo( "Grails", new Formatter().format( "compile '%s:%s:%s'",
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
