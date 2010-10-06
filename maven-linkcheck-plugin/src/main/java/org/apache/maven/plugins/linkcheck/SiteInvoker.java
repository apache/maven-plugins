/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.maven.plugins.linkcheck;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.apache.maven.model.Profile;
import org.apache.maven.model.Reporting;
import org.apache.maven.project.MavenProject;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.cli.CommandLineUtils;


/**
 *
 * @author ltheussl
 * @since 1.1
 */
public class SiteInvoker
{
    private final ArtifactRepository localRepository;
    private final Log log;

    public SiteInvoker( ArtifactRepository localRepository, Log log )
    {
        this.localRepository = localRepository;
        this.log = log;
    }

    /**
     * Invoke Maven for the <code>site</code> phase for a temporary Maven project using
     * <code>tmpReportingOutputDirectory</code> as <code>${project.reporting.outputDirectory}</code>.
     * This is a workaround to be sure that all site files have been correctly generated.
     * <br/>
     * <b>Note 1</b>: the Maven Home should be defined in the <code>maven.home</code> Java system property
     * or defined in <code>M2_HOME</code> system env variables.
     * <b>Note 2</be>: we can't use <code>siteOutputDirectory</code> param from site plugin because some plugins
     * <code>${project.reporting.outputDirectory}</code> in their conf.
     *
     * @param project the MavenProject to invoke the site on. Not null.
     * @param tmpReportingOutputDirectory not null
     * @throws IOException if any
     */
    public void invokeSite( MavenProject project, File tmpReportingOutputDirectory )
        throws IOException
    {
        String mavenHome = getMavenHome();
        if ( StringUtils.isEmpty( mavenHome ) )
        {
            getLog().error( "Could NOT invoke Maven because no Maven Home is defined. "
                + "You need to set the M2_HOME system env variable or a 'maven.home' Java system property." );
            return;
        }

        // invoker site parameters
        List goals = Collections.singletonList( "site" );
        Properties properties = new Properties();
        properties.put( "linkcheck.skip", "true" ); // to stop recursion

        File invokerLog =
            FileUtils.createTempFile( "invoker-site-plugin", ".txt", new File( project.getBuild().getDirectory() ) );

        // clone project and set a new reporting output dir
        MavenProject clone;
        try
        {
            clone = (MavenProject) project.clone();
        }
        catch ( CloneNotSupportedException e )
        {
            IOException ioe = new IOException( "CloneNotSupportedException: " + e.getMessage() );
            ioe.setStackTrace( e.getStackTrace() );
            throw ioe;
        }

        // MLINKCHECK-1
        if ( clone.getOriginalModel().getReporting() == null )
        {
            clone.getOriginalModel().setReporting( new Reporting() );
        }

        clone.getOriginalModel().getReporting().setOutputDirectory( tmpReportingOutputDirectory.getAbsolutePath() );
        List profileIds = getActiveProfileIds( clone );

        // create the original model as tmp pom file for the invoker
        File tmpProjectFile = FileUtils.createTempFile( "pom", ".xml", project.getBasedir() );
        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( tmpProjectFile );
            clone.writeOriginalModel( writer );
        }
        finally
        {
            IOUtil.close( writer );
        }

        // invoke it
        try
        {
            invoke( tmpProjectFile, invokerLog, mavenHome, goals, profileIds, properties );
        }
        finally
        {
            if ( !getLog().isDebugEnabled() )
            {
                tmpProjectFile.delete();
            }
        }
    }

    private static List getActiveProfileIds( MavenProject clone )
    {
        List profileIds = new ArrayList();

        for ( Iterator it = clone.getActiveProfiles().iterator(); it.hasNext(); )
        {
            profileIds.add( ( (Profile) it.next() ).getId() );
        }

        return profileIds;
    }

    /**
     * @param projectFile not null, should be in the ${project.basedir}
     * @param invokerLog not null
     * @param mavenHome not null
     * @param goals the list of goals
     * @param properties the properties for the invoker
     */
    private void invoke( File projectFile, File invokerLog, String mavenHome,
        List goals, List activeProfiles, Properties properties )
    {
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome( new File( mavenHome ) );
        File localRepoDir = new File( localRepository.getBasedir() );
        invoker.setLocalRepositoryDirectory( localRepoDir );

        InvocationRequest request = new DefaultInvocationRequest();
        request.setLocalRepositoryDirectory( localRepoDir );
        //request.setUserSettingsFile( settingsFile );
        request.setInteractive( false );
        request.setShowErrors( getLog().isErrorEnabled() );
        request.setDebug( getLog().isDebugEnabled() );
        //request.setShowVersion( false );
        request.setBaseDirectory( projectFile.getParentFile() );
        request.setPomFile( projectFile );
        request.setGoals( goals );
        request.setProperties( properties );
        request.setProfiles( activeProfiles );

        File javaHome = getJavaHome();
        if ( javaHome != null )
        {
            request.setJavaHome( javaHome );
        }

        InvocationResult invocationResult;
        try
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Invoking Maven for the goals: " + goals + " with properties=" + properties );
            }
            invocationResult = invoke( invoker, request, invokerLog, goals, properties, null );
        }
        catch ( MavenInvocationException e )
        {
            getLog().error( "Error when invoking Maven, consult the invoker log." );
            getLog().debug( e );
            return;
        }

        String invokerLogContent = null;
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newReader( invokerLog, "UTF-8" );
            invokerLogContent = IOUtil.toString( reader );
        }
        catch ( IOException e )
        {
            getLog().error( "IOException: " + e.getMessage() );
            getLog().debug( e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( invokerLogContent != null
            && invokerLogContent.indexOf( "Error occurred during initialization of VM" ) != -1 )
        {
            getLog().info( "Error occurred during initialization of VM, try to use an empty MAVEN_OPTS." );

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Reinvoking Maven for the goals: " + goals + " with an empty MAVEN_OPTS" );
            }

            try
            {
                invocationResult = invoke( invoker, request, invokerLog, goals, properties, "" );
            }
            catch ( MavenInvocationException e )
            {
                getLog().error( "Error when reinvoking Maven, consult the invoker log." );
                getLog().debug( e );
                return;
            }
        }

        if ( invocationResult.getExitCode() != 0 )
        {
            if ( getLog().isErrorEnabled() )
            {
                getLog().error( "Error when invoking Maven, consult the invoker log file: "
                                    + invokerLog.getAbsolutePath() );
            }
        }
    }

    /**
     * @param invoker not null
     * @param request not null
     * @param invokerLog not null
     * @param goals the list of goals
     * @param properties the properties for the invoker
     * @param mavenOpts could be null
     * @return the invocation result
     * @throws MavenInvocationException if any
     */
    private InvocationResult invoke( Invoker invoker, InvocationRequest request, File invokerLog, List goals,
                                     Properties properties, String mavenOpts )
        throws MavenInvocationException
    {
        PrintStream ps;
        OutputStream os = null;
        if ( invokerLog != null )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Using " + invokerLog.getAbsolutePath() + " to log the invoker" );
            }

            try
            {
                if ( !invokerLog.exists() )
                {
                    invokerLog.getParentFile().mkdirs();
                }
                os = new FileOutputStream( invokerLog );
                ps = new PrintStream( os, true, "UTF-8" );
            }
            catch ( FileNotFoundException e )
            {
                if ( getLog().isErrorEnabled() )
                {
                    getLog().error( "FileNotFoundException: " + e.getMessage()
                                        + ". Using System.out to log the invoker." );
                }
                ps = System.out;
            }
            catch ( UnsupportedEncodingException e )
            {
                if ( getLog().isErrorEnabled() )
                {
                    getLog().error( "UnsupportedEncodingException: " + e.getMessage()
                                        + ". Using System.out to log the invoker." );
                }
                ps = System.out;
            }
        }
        else
        {
            getLog().debug( "Using System.out to log the invoker." );

            ps = System.out;
        }

        if ( mavenOpts != null )
        {
            request.setMavenOpts( mavenOpts );
        }

        InvocationOutputHandler outputHandler = new PrintStreamHandler( ps, false );
        request.setOutputHandler( outputHandler );
        request.setErrorHandler( outputHandler );

        outputHandler.consumeLine( "Invoking Maven for the goals: " + goals + " with properties=" + properties );
        outputHandler.consumeLine( "" );
        outputHandler.consumeLine( "M2_HOME=" + getMavenHome() );
        outputHandler.consumeLine( "MAVEN_OPTS=" + getMavenOpts() );
        outputHandler.consumeLine( "JAVA_HOME=" + getJavaHome() );
        outputHandler.consumeLine( "JAVA_OPTS=" + getJavaOpts() );
        outputHandler.consumeLine( "" );

        try
        {
            return invoker.execute( request );
        }
        finally
        {
            IOUtil.close( os );
            ps = null;
        }
    }

    /**
     * @return the Maven home defined in the <code>maven.home</code> system property or defined
     * in <code>M2_HOME</code> system env variables or null if never setted.
     * @see #invoke(Invoker, InvocationRequest, File, List, Properties, String)
     */
    private String getMavenHome()
    {
        String mavenHome = System.getProperty( "maven.home" );
        if ( mavenHome == null )
        {
            try
            {
                mavenHome = CommandLineUtils.getSystemEnvVars().getProperty( "M2_HOME" );
            }
            catch ( IOException e )
            {
                getLog().error( "IOException: " + e.getMessage() );
                getLog().debug( e );
            }
        }

        File m2Home = new File( mavenHome );
        if ( !m2Home.exists() )
        {
            getLog().error( "Cannot find Maven application directory. Either specify \'maven.home\' "
                + "system property, or M2_HOME environment variable." );
        }

        return mavenHome;
    }

    /**
     * @return the <code>MAVEN_OPTS</code> env variable value or null if not setted.
     * @see #invoke(Invoker, InvocationRequest, File, List, Properties, String)
     */
    private String getMavenOpts()
    {
        String mavenOpts = null;
        try
        {
            mavenOpts = CommandLineUtils.getSystemEnvVars().getProperty( "MAVEN_OPTS" );
        }
        catch ( IOException e )
        {
            getLog().error( "IOException: " + e.getMessage() );
            getLog().debug( e );
        }

        return mavenOpts;
    }

    /**
     * @return the <code>JAVA_HOME</code> from System.getProperty( "java.home" )
     * By default, <code>System.getProperty( "java.home" ) = JRE_HOME</code> and <code>JRE_HOME</code>
     * should be in the <code>JDK_HOME</code> or null if not setted.
     * @see #invoke(Invoker, InvocationRequest, File, List, Properties, String)
     */
    private File getJavaHome()
    {
        File javaHome;
        if ( SystemUtils.IS_OS_MAC_OSX )
        {
            javaHome = SystemUtils.getJavaHome();
        }
        else
        {
            javaHome = new File( SystemUtils.getJavaHome(), ".." );
        }

        if ( javaHome == null || !javaHome.exists() )
        {
            try
            {
                javaHome = new File( CommandLineUtils.getSystemEnvVars().getProperty( "JAVA_HOME" ) );
            }
            catch ( IOException e )
            {
                getLog().error( "IOException: " + e.getMessage() );
                getLog().debug( e );
            }
        }

        if ( javaHome == null || !javaHome.exists() )
        {
            getLog().error( "Cannot find Java application directory. Either specify \'java.home\' "
                + "system property, or JAVA_HOME environment variable." );
        }

        return javaHome;
    }

    /**
     * @return the <code>JAVA_OPTS</code> env variable value or null if not setted.
     * @see #invoke(Invoker, InvocationRequest, File, List, Properties, String)
     */
    private String getJavaOpts()
    {
        String javaOpts = null;
        try
        {
            javaOpts = CommandLineUtils.getSystemEnvVars().getProperty( "JAVA_OPTS" );
        }
        catch ( IOException e )
        {
            getLog().error( "IOException: " + e.getMessage() );
            getLog().debug( e );
        }

        return javaOpts;
    }

    private Log getLog()
    {
        return log;
    }
}
