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
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.cvslib.repository.CvsScmProviderRepository;
import org.apache.maven.scm.provider.perforce.repository.PerforceScmProviderRepository;
import org.apache.maven.scm.provider.starteam.repository.StarteamScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

/**
 * Generates the Project Source Code Management report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @goal scm
 */
public class ScmReport
    extends AbstractProjectInfoReport
{
    /**
     * Maven SCM Manager.
     *
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
     */
    protected ScmManager scmManager;

    /**
     * The directory name to checkout right after the scm url
     *
     * @parameter expression="${project.artifactId}"
     * @required
     */
    private String checkoutDirectoryName;

    /**
     * The scm anonymous connection url.
     *
     * @parameter default-value="${project.scm.connection}"
     * @since 2.1
     */
    private String anonymousConnection;

    /**
     * The scm developer connection url.
     *
     * @parameter default-value="${project.scm.developerConnection}"
     * @since 2.1
     */
    private String developerConnection;

    /**
     * The scm web access url.
     *
     * @parameter default-value="${project.scm.url}"
     * @since 2.1
     */
    private String webAccessUrl;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.scm.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.scm.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
    {
        ScmRenderer r =
            new ScmRenderer( scmManager, getSink(), getProject().getModel(), i18n, locale, checkoutDirectoryName,
                    webAccessUrl, anonymousConnection, developerConnection );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "source-repository";
    }

    private static class ScmRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private I18N i18n;

        private Locale locale;

        private ScmManager scmManager;

        /**
         * To support more SCM
         */
        private String anonymousConnection;

        private String devConnection;

        private String checkoutDirectoryName;

        private String webAccessUrl;

        ScmRenderer( ScmManager scmManager, Sink sink, Model model, I18N i18n, Locale locale, String checkoutDirName,
                     String webAccessUrl, String anonymousConnection, String devConnection )
        {
            super( sink );

            this.scmManager = scmManager;

            this.model = model;

            this.i18n = i18n;

            this.locale = locale;

            this.checkoutDirectoryName = checkoutDirName;

            this.webAccessUrl = webAccessUrl;

            this.anonymousConnection = anonymousConnection;

            this.devConnection = devConnection;

        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.scm.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            Scm scm = model.getScm();
            if ( scm == null )
            {
                startSection( getTitle() );

                paragraph( i18n.getString( "project-info-report", locale, "report.scm.noscm" ) );

                endSection();

                return;
            }

            if ( StringUtils.isEmpty( anonymousConnection ) && StringUtils.isEmpty( devConnection ) &&
                StringUtils.isEmpty( scm.getUrl() ) )
            {
                startSection( getTitle() );

                paragraph( i18n.getString( "project-info-report", locale, "report.scm.noscm" ) );

                endSection();

                return;
            }

            ScmRepository anonymousRepository = getScmRepository( anonymousConnection );
            ScmRepository devRepository = getScmRepository( devConnection );

            // Overview section
            renderOverViewSection( anonymousRepository );

            // Web access section
            renderWebAccesSection( webAccessUrl );

            // Anonymous access section if needed
            renderAnonymousAccessSection( anonymousRepository );

            // Developer access section
            renderDeveloperAccessSection( devRepository );

            // Access from behind a firewall section if needed
            renderAccessBehindFirewallSection( devRepository );

            // Access through a proxy section if needed
            renderAccessThroughProxySection( anonymousRepository, devRepository );
        }

        /**
         * Render the overview section
         *
         * @param anonymousRepository the anonymous repository
         */
        private void renderOverViewSection( ScmRepository anonymousRepository )
        {
            startSection( i18n.getString( "project-info-report", locale, "report.scm.overview.title" ) );

            if ( isScmSystem( anonymousRepository, "clearcase" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.scm.clearcase.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "cvs" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.scm.cvs.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "perforce" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.scm.perforce.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "starteam" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.scm.starteam.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "svn" ) )
            {
                linkPatternedText( i18n.getString( "project-info-report", locale, "report.scm.svn.intro" ) );
            }
            else
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.scm.general.intro" ) );
            }

            endSection();
        }

        /**
         * Render the web access section
         *
         * @param scmUrl The URL to the project's browsable repository.
         */
        private void renderWebAccesSection( String scmUrl )
        {
            startSection( i18n.getString( "project-info-report", locale, "report.scm.webaccess.title" ) );

            if ( StringUtils.isEmpty( scmUrl ) )
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.scm.webaccess.nourl" ) );
            }
            else
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.scm.webaccess.url" ) );

                verbatimLink( scmUrl, scmUrl );
            }

            endSection();
        }

        /**
         * Render the anonymous access section depending the repository.
         * <p>Note: ClearCase, Starteam et Perforce seems to have no anonymous access.</>
         *
         * @param anonymousRepository the anonymous repository
         */
        private void renderAnonymousAccessSection( ScmRepository anonymousRepository )
        {
            if ( isScmSystem( anonymousRepository, "clearcase" ) || isScmSystem( anonymousRepository, "perforce" ) ||
                isScmSystem( anonymousRepository, "starteam" ) || StringUtils.isEmpty( anonymousConnection ) )
            {
                return;
            }

            startSection( i18n.getString( "project-info-report", locale, "report.scm.anonymousaccess.title" ) );

            if ( anonymousRepository != null && isScmSystem( anonymousRepository, "cvs" ) )
            {
                CvsScmProviderRepository cvsRepo =
                    (CvsScmProviderRepository) anonymousRepository.getProviderRepository();

                anonymousAccessCVS( cvsRepo );
            }
            else if ( anonymousRepository != null && isScmSystem( anonymousRepository, "svn" ) )
            {
                SvnScmProviderRepository svnRepo =
                    (SvnScmProviderRepository) anonymousRepository.getProviderRepository();

                anonymousAccessSVN( svnRepo );
            }
            else
            {
                paragraph(
                    i18n.getString( "project-info-report", locale, "report.scm.anonymousaccess.general.intro" ) );

                if ( anonymousConnection.length() < 4 )
                {
                    throw new IllegalArgumentException( "The source repository connection is too short." );
                }

                verbatimText( anonymousConnection.substring( 4 ) );
            }

            endSection();
        }

        /**
         * Render the developer access section
         *
         * @param devRepository the dev repository
         */
        private void renderDeveloperAccessSection( ScmRepository devRepository )
        {
            if ( StringUtils.isEmpty( devConnection ) )
            {
                return;
            }

            startSection( i18n.getString( "project-info-report", locale, "report.scm.devaccess.title" ) );

            if ( devRepository != null && isScmSystem( devRepository, "clearcase" ) )
            {
                developerAccessClearCase();
            }
            else if ( devRepository != null && isScmSystem( devRepository, "cvs" ) )
            {
                CvsScmProviderRepository cvsRepo = (CvsScmProviderRepository) devRepository.getProviderRepository();

                developerAccessCVS( cvsRepo );
            }
            else if ( devRepository != null && isScmSystem( devRepository, "perforce" ) )
            {
                PerforceScmProviderRepository perforceRepo =
                    (PerforceScmProviderRepository) devRepository.getProviderRepository();

                developerAccessPerforce( perforceRepo );
            }
            else if ( devRepository != null && isScmSystem( devRepository, "starteam" ) )
            {
                StarteamScmProviderRepository starteamRepo =
                    (StarteamScmProviderRepository) devRepository.getProviderRepository();

                developerAccessStarteam( starteamRepo );
            }
            else if ( devRepository != null && isScmSystem( devRepository, "svn" ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) devRepository.getProviderRepository();

                developerAccessSVN( svnRepo );
            }
            else
            {
                paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.general.intro" ) );

                if ( devConnection.length() < 4 )
                {
                    throw new IllegalArgumentException( "The source repository connection is too short." );
                }

                verbatimText( devConnection.substring( 4 ) );
            }

            endSection();
        }

        /**
         * Render the access from behind a firewall section
         *
         * @param devRepository the dev repository
         */
        private void renderAccessBehindFirewallSection( ScmRepository devRepository )
        {
            startSection( i18n.getString( "project-info-report", locale, "report.scm.accessbehindfirewall.title" ) );

            if ( devRepository != null && isScmSystem( devRepository, "svn" ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) devRepository.getProviderRepository();

                paragraph(
                    i18n.getString( "project-info-report", locale, "report.scm.accessbehindfirewall.svn.intro" ) );

                StringBuffer sb = new StringBuffer();
                sb.append( "$ svn checkout " ).append( svnRepo.getUrl() ).append( " " ).append( checkoutDirectoryName );
                verbatimText( sb.toString() );
            }
            else if ( devRepository != null && isScmSystem( devRepository, "cvs" ) )
            {
                linkPatternedText(
                    i18n.getString( "project-info-report", locale, "report.scm.accessbehindfirewall.cvs.intro" ) );
            }
            else
            {
                paragraph(
                    i18n.getString( "project-info-report", locale, "report.scm.accessbehindfirewall.general.intro" ) );
            }

            endSection();
        }

        /**
         * Render the access from behind a firewall section
         *
         * @param anonymousRepository the anonymous repository
         * @param devRepository       the dev repository
         */
        private void renderAccessThroughProxySection( ScmRepository anonymousRepository, ScmRepository devRepository )
        {
            if ( isScmSystem( anonymousRepository, "svn" ) || isScmSystem( devRepository, "svn" ) )
            {
                startSection( i18n.getString( "project-info-report", locale, "report.scm.accessthroughtproxy.title" ) );

                paragraph(
                    i18n.getString( "project-info-report", locale, "report.scm.accessthroughtproxy.svn.intro1" ) );
                paragraph(
                    i18n.getString( "project-info-report", locale, "report.scm.accessthroughtproxy.svn.intro2" ) );
                paragraph(
                    i18n.getString( "project-info-report", locale, "report.scm.accessthroughtproxy.svn.intro3" ) );

                StringBuffer sb = new StringBuffer();
                sb.append( "[global]" );
                sb.append( "\n" );
                sb.append( "http-proxy-host = your.proxy.name" ).append( "\n" );
                sb.append( "http-proxy-port = 3128" ).append( "\n" );
                verbatimText( sb.toString() );

                endSection();
            }
        }

        // Clearcase

        /**
         * Create the documentation to provide an developer access with a <code>Clearcase</code> SCM.
         * For example, generate the following command line:
         * <p>cleartool checkout module</p>
         */
        private void developerAccessClearCase()
        {
            paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.clearcase.intro" ) );

            StringBuffer command = new StringBuffer();
            command.append( "$ cleartool checkout " );

            verbatimText( command.toString() );
        }

        // CVS

        /**
         * Create the documentation to provide an anonymous access with a <code>CVS</code> SCM.
         * For example, generate the following command line:
         * <p>cvs -d :pserver:anoncvs@cvs.apache.org:/home/cvspublic login</p>
         * <p>cvs -z3 -d :pserver:anoncvs@cvs.apache.org:/home/cvspublic co maven-plugins/dist</p>
         *
         * @param cvsRepo
         * @see <a href="https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115">https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115</a>
         */
        private void anonymousAccessCVS( CvsScmProviderRepository cvsRepo )
        {
            paragraph( i18n.getString( "project-info-report", locale, "report.scm.anonymousaccess.cvs.intro" ) );

            StringBuffer command = new StringBuffer();
            command.append( "$ cvs -d " ).append( cvsRepo.getCvsRoot() ).append( " login" );
            command.append( "\n" );
            command.append( "$ cvs -z3 -d " ).append( cvsRepo.getCvsRoot() );
            command.append( " co " ).append( cvsRepo.getModule() );

            verbatimText( command.toString() );
        }

        /**
         * Create the documentation to provide an developer access with a <code>CVS</code> SCM.
         * For example, generate the following command line:
         * <p>cvs -d :pserver:username@cvs.apache.org:/home/cvs login</p>
         * <p>cvs -z3 -d :ext:username@cvs.apache.org:/home/cvs co maven-plugins/dist</p>
         *
         * @param cvsRepo
         * @see <a href="https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115">https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115</a>
         */
        private void developerAccessCVS( CvsScmProviderRepository cvsRepo )
        {
            paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.cvs.intro" ) );

            // Safety: remove the username if present
            String cvsRoot = StringUtils.replace( cvsRepo.getCvsRoot(), cvsRepo.getUser(), "username" );

            StringBuffer command = new StringBuffer();
            command.append( "$ cvs -d " ).append( cvsRoot ).append( " login" );
            command.append( "\n" );
            command.append( "$ cvs -z3 -d " ).append( cvsRoot ).append( " co " ).append( cvsRepo.getModule() );

            verbatimText( command.toString() );
        }

        // Perforce

        /**
         * Create the documentation to provide an developer access with a <code>Perforce</code> SCM.
         * For example, generate the following command line:
         * <p>p4 -H hostname -p port -u username -P password path</p>
         * <p>p4 -H hostname -p port -u username -P password path submit -c changement</p>
         *
         * @param perforceRepo
         * @see <a href="http://www.perforce.com/perforce/doc.051/manuals/cmdref/index.html">http://www.perforce.com/perforce/doc.051/manuals/cmdref/index.html</>
         */
        private void developerAccessPerforce( PerforceScmProviderRepository perforceRepo )
        {
            paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.perforce.intro" ) );

            StringBuffer command = new StringBuffer();
            command.append( "$ p4" );
            if ( !StringUtils.isEmpty( perforceRepo.getHost() ) )
            {
                command.append( " -H " ).append( perforceRepo.getHost() );
            }
            if ( perforceRepo.getPort() > 0 )
            {
                command.append( " -p " ).append( perforceRepo.getPort() );
            }
            command.append( " -u username" );
            command.append( " -P password" );
            command.append( " " );
            command.append( perforceRepo.getPath() );
            command.append( "\n" );
            command.append( "$ p4 submit -c \"A comment\"" );

            verbatimText( command.toString() );
        }

        // Starteam

        /**
         * Create the documentation to provide an developer access with a <code>Starteam</code> SCM.
         * For example, generate the following command line:
         * <p>stcmd co -x -nologo -stop -p myusername:mypassword@myhost:1234/projecturl -is</p>
         * <p>stcmd ci -x -nologo -stop -p myusername:mypassword@myhost:1234/projecturl -f NCI -is</p>
         *
         * @param starteamRepo
         */
        private void developerAccessStarteam( StarteamScmProviderRepository starteamRepo )
        {
            paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.starteam.intro" ) );

            StringBuffer command = new StringBuffer();

            // Safety: remove the username/password if present
            String fullUrl = StringUtils.replace( starteamRepo.getFullUrl(), starteamRepo.getUser(), "username" );
            fullUrl = StringUtils.replace( fullUrl, starteamRepo.getPassword(), "password" );

            command.append( "$ stcmd co -x -nologo -stop -p " );
            command.append( fullUrl );
            command.append( " -is" );
            command.append( "\n" );
            command.append( "$ stcmd ci -x -nologo -stop -p " );
            command.append( fullUrl );
            command.append( " -f NCI -is" );

            verbatimText( command.toString() );
        }

        // SVN

        /**
         * Create the documentation to provide an anonymous access with a <code>SVN</code> SCM.
         * For example, generate the following command line:
         * <p>svn checkout http://svn.apache.org/repos/asf/maven/components/trunk maven</p>
         *
         * @param svnRepo
         * @see <a href="http://svnbook.red-bean.com/">http://svnbook.red-bean.com/</a>
         */
        private void anonymousAccessSVN( SvnScmProviderRepository svnRepo )
        {
            paragraph( i18n.getString( "project-info-report", locale, "report.scm.anonymousaccess.svn.intro" ) );

            StringBuffer sb = new StringBuffer();
            sb.append( "$ svn checkout " ).append( svnRepo.getUrl() ).append( " " ).append( checkoutDirectoryName );

            verbatimText( sb.toString() );
        }

        /**
         * Create the documentation to provide an developer access with a <code>SVN</code> SCM.
         * For example, generate the following command line:
         * <p>svn checkout https://svn.apache.org/repos/asf/maven/components/trunk maven</p>
         * <p>svn commit --username your-username -m "A message"</p>
         *
         * @param svnRepo
         * @see <a href="http://svnbook.red-bean.com/">http://svnbook.red-bean.com/</a>
         */
        private void developerAccessSVN( SvnScmProviderRepository svnRepo )
        {
            if ( svnRepo.getUrl() != null )
            {
                if ( svnRepo.getUrl().startsWith( "https://" ) )
                {
                    paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.svn.intro1.https" ) );
                }
                else if ( svnRepo.getUrl().startsWith( "svn://" ) )
                {
                    paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.svn.intro1.svn" ) );
                }
                else if ( svnRepo.getUrl().startsWith( "svn+ssh://" ) )
                {
                    paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.svn.intro1.svnssh" ) );
                }
                else
                {
                    paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.svn.intro1.other" ) );
                }
            }

            StringBuffer sb = new StringBuffer();

            sb.append( "$ svn checkout " ).append( svnRepo.getUrl() ).append( " " ).append( checkoutDirectoryName );

            verbatimText( sb.toString() );

            paragraph( i18n.getString( "project-info-report", locale, "report.scm.devaccess.svn.intro2" ) );

            sb = new StringBuffer();
            sb.append( "$ svn commit --username your-username -m \"A message\"" );

            verbatimText( sb.toString() );
        }

        /**
         * Return a <code>SCM repository</code> defined by a given url
         *
         * @param scmUrl an SCM URL
         * @return a valid SCM repository or null
         */
        public ScmRepository getScmRepository( String scmUrl )
        {
            ScmRepository repo = null;
            if ( !StringUtils.isEmpty( scmUrl ) )
            {
                try
                {
                    repo = scmManager.makeScmRepository( scmUrl );
                }
                catch ( NoSuchScmProviderException e )
                {
                    // ignore
                }
                catch ( ScmRepositoryException e )
                {
                    // ignore
                }
            }
            return repo;
        }

        /**
         * Convenience method that return true is the defined <code>SCM repository</code> is a known provider.
         * <p>Actually, we fully support Clearcase, CVS, Perforce, Starteam, SVN by the maven-scm-providers component.</p>
         *
         * @param scmRepository a SCM repository
         * @param scmProvider   a SCM provider name
         * @return true if the provider of the given SCM repository is equal to the given scm provider.
         * @see <a href="http://svn.apache.org/repos/asf/maven/scm/trunk/maven-scm-providers/">maven-scm-providers</a>
         */
        private static boolean isScmSystem( ScmRepository scmRepository, String scmProvider )
        {
            if ( StringUtils.isEmpty( scmProvider ) )
            {
                return false;
            }

            if ( scmRepository != null && scmProvider.equalsIgnoreCase( scmRepository.getProvider() ) )
            {
                return true;
            }

            return false;
        }
    }
}
