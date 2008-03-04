package org.apache.maven.plugin.javadoc;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Set of utilities methods for Javadoc.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.4
 */
public class JavadocUtil
{
    /**
     * Method that removes the invalid directories in the specified directories
     *
     * @param dirs the list of directories that will be validated
     * @return a List of valid directories
     */
    protected static List pruneDirs( List dirs )
    {
        List pruned = new ArrayList( dirs.size() );
        for ( Iterator i = dirs.iterator(); i.hasNext(); )
        {
            String dir = (String) i.next();

            if ( dir == null )
            {
                continue;
            }

            File directory = new File( dir );
            if ( directory.exists() && directory.isDirectory() )
            {
                if ( !pruned.contains( dir ) )
                {
                    pruned.add( dir );
                }
            }
        }

        return pruned;
    }

    /**
     * Method that removes the invalid files in the specified files
     *
     * @param files the list of files that will be validated
     * @return a List of valid files
     */
    protected static List pruneFiles( List files )
    {
        List pruned = new ArrayList( files.size() );
        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            String f = (String) i.next();

            if ( f == null )
            {
                continue;
            }

            File file = new File( f );
            if ( file.exists() && file.isFile() )
            {
                if ( !pruned.contains( f ) )
                {
                    pruned.add( f );
                }
            }
        }

        return pruned;
    }

    /**
     * Method that gets all the source files to be excluded from the javadoc on the given
     * source paths.
     *
     * @param sourcePaths      the path to the source files
     * @param subpackagesList  list of subpackages to be included in the javadoc
     * @param excludedPackages the package names to be excluded in the javadoc
     * @return a List of the source files to be excluded in the generated javadoc
     */
    protected static List getExcludedNames( List sourcePaths, String[] subpackagesList, String[] excludedPackages )
    {
        List excludedNames = new ArrayList();
        for ( Iterator i = sourcePaths.iterator(); i.hasNext(); )
        {
            String path = (String) i.next();
            for ( int j = 0; j < subpackagesList.length; j++ )
            {
                List excludes = getExcludedPackages( path, excludedPackages );
                excludedNames.addAll( excludes );
            }
        }

        return excludedNames;
    }

    /**
     * Copy from {@link MavenProject#getCompileArtifacts()}
     * @param artifacts
     * @return list of compile artifacts
     */
    protected static List getCompileArtifacts( Set artifacts )
    {
        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }

        return list;
    }

    /**
     * Convenience method to wrap an argument value in single quotes (i.e. <code>'</code>). Intended for values
     * which may contain whitespaces.
     * <br/>
     * To prevent javadoc error, the line separator (i.e. <code>\n</code>) are skipped.
     *
     * @param value the argument value.
     * @return argument with quote
     */
    protected static String quotedArgument( String value )
    {
        String arg = value;

        if ( StringUtils.isNotEmpty( arg ) )
        {
            if ( arg.indexOf( "'" ) != -1 )
            {
                arg = StringUtils.replace( arg, "'", "\\'" );
            }
            arg = "'" + arg + "'";

            // To prevent javadoc error
            arg = StringUtils.replace( arg, "\n", " " );
        }

        return arg;
    }

    /**
     * Convenience method to format a path argument so that it is properly interpreted by the javadoc tool. Intended
     * for path values which may contain whitespaces.
     *
     * @param value the argument value.
     * @return path argument with quote
     */
    protected static String quotedPathArgument( String value )
    {
        String path = value;

        if ( StringUtils.isNotEmpty( path ) )
        {
            path = path.replace( '\\', '/' );
            if ( path.indexOf( "\'" ) != -1 )
            {
                String split[] = path.split( "\'" );
                path = "";

                for ( int i = 0; i < split.length; i++ )
                {
                    if ( i != split.length - 1 )
                    {
                        path = path + split[i] + "\\'";
                    }
                    else
                    {
                        path = path + split[i];
                    }
                }
            }
            path = "'" + path + "'";
        }

        return path;
    }

    /**
     * Convenience method that copy all <code>doc-files</code> directories from <code>javadocDir</code>
     * to the <code>outputDirectory</code>.
     *
     * @param outputDirectory the output directory
     * @param javadocDir the javadoc directory
     * @throws java.io.IOException if any
     */
    protected static void copyJavadocResources( File outputDirectory, File javadocDir )
        throws IOException
    {
        if ( javadocDir.exists() && javadocDir.isDirectory() )
        {
            List docFiles = FileUtils.getDirectoryNames( javadocDir, "**/doc-files", StringUtils.join( FileUtils
                .getDefaultExcludes(), "," ), false, true );
            for ( Iterator it = docFiles.iterator(); it.hasNext(); )
            {
                String docFile = (String) it.next();

                File docFileOutput = new File( outputDirectory, docFile );
                FileUtils.mkdir( docFileOutput.getAbsolutePath() );
                FileUtils.copyDirectory( new File( javadocDir, docFile ), docFileOutput );
            }
        }
    }

    /**
     * Method that gets the files or classes that would be included in the javadocs using the subpackages
     * parameter.
     *
     * @param sourceDirectory the directory where the source files are located
     * @param fileList        the list of all files found in the sourceDirectory
     * @param excludePackages package names to be excluded in the javadoc
     * @return a StringBuffer that contains the appended file names of the files to be included in the javadoc
     */
    protected static List getIncludedFiles( File sourceDirectory, String[] fileList, String[] excludePackages )
    {
        List files = new ArrayList();

        for ( int j = 0; j < fileList.length; j++ )
        {
            boolean include = true;
            for ( int k = 0; k < excludePackages.length && include; k++ )
            {
                // handle wildcards (*) in the excludePackageNames
                String[] excludeName = excludePackages[k].split( "[*]" );

                if ( excludeName.length > 1 )
                {
                    int u = 0;
                    while ( include && u < excludeName.length )
                    {
                        if ( !"".equals( excludeName[u].trim() ) && fileList[j].indexOf( excludeName[u] ) != -1 )
                        {
                            include = false;
                        }
                        u++;
                    }
                }
                else
                {
                    if ( fileList[j].startsWith( sourceDirectory.toString() + File.separatorChar + excludeName[0] ) )
                    {
                        if ( excludeName[0].endsWith( String.valueOf( File.separatorChar ) ) )
                        {
                            int i = fileList[j].lastIndexOf( File.separatorChar );
                            String packageName = fileList[j].substring( 0, i + 1 );
                            File currentPackage = new File( packageName );
                            File excludedPackage = new File( sourceDirectory, excludeName[0] );
                            if ( currentPackage.equals( excludedPackage )
                                && fileList[j].substring( i ).indexOf( ".java" ) != -1 )
                            {
                                include = true;
                            }
                            else
                            {
                                include = false;
                            }
                        }
                        else
                        {
                            include = false;
                        }
                    }
                }
            }

            if ( include )
            {
                files.add( quotedPathArgument( fileList[j] ) );
            }
        }

        return files;
    }

    /**
     * Method that gets the complete package names (including subpackages) of the packages that were defined
     * in the excludePackageNames parameter.
     *
     * @param sourceDirectory     the directory where the source files are located
     * @param excludePackagenames package names to be excluded in the javadoc
     * @return a List of the packagenames to be excluded
     */
    protected static List getExcludedPackages( String sourceDirectory, String[] excludePackagenames )
    {
        List files = new ArrayList();
        for ( int i = 0; i < excludePackagenames.length; i++ )
        {
            String[] fileList = FileUtils.getFilesFromExtension( sourceDirectory, new String[] { "java" } );
            for ( int j = 0; j < fileList.length; j++ )
            {
                String[] excludeName = excludePackagenames[i].split( "[*]" );
                int u = 0;
                while ( u < excludeName.length )
                {
                    if ( !"".equals( excludeName[u].trim() ) && fileList[j].indexOf( excludeName[u] ) != -1
                        && sourceDirectory.indexOf( excludeName[u] ) == -1 )
                    {
                        files.add( fileList[j] );
                    }
                    u++;
                }
            }
        }

        List excluded = new ArrayList();
        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            String file = (String) it.next();
            int idx = file.lastIndexOf( File.separatorChar );
            String tmpStr = file.substring( 0, idx );
            tmpStr = tmpStr.replace( '\\', '/' );
            String[] srcSplit = tmpStr.split( sourceDirectory.replace( '\\', '/' ) + '/' );
            String excludedPackage = srcSplit[1].replace( '/', '.' );

            if ( !excluded.contains( excludedPackage ) )
            {
                excluded.add( excludedPackage );
            }
        }

        return excluded;
    }

    /**
     * Convenience method that gets the files to be included in the javadoc.
     *
     * @param sourceDirectory the directory where the source files are located
     * @param files           the variable that contains the appended filenames of the files to be included in the javadoc
     * @param excludePackages the packages to be excluded in the javadocs
     */
    protected static void addFilesFromSource( List files, File sourceDirectory, String[] excludePackages )
    {
        String[] fileList = FileUtils.getFilesFromExtension( sourceDirectory.getPath(), new String[] { "java" } );
        if ( fileList != null && fileList.length != 0 )
        {
            List tmpFiles = getIncludedFiles( sourceDirectory, fileList, excludePackages );
            files.addAll( tmpFiles );
        }
    }

    /**
     * Call the Javadoc tool and parse its output to find its version, i.e.:
     * <pre>
     * javadoc.exe(or .sh) -J-version
     * </pre>
     *
     * @param javadocExe not null file
     * @return the javadoc version as float
     * @throws IOException if javadocExe is null, doesn't exist or is not a file
     * @throws CommandLineException if any
     * @throws PatternSyntaxException if the output contains a syntax error in the regular-expression pattern.
     * @throws IllegalArgumentException if no output was found in the command line
     * @see #parseJavadocVersion(String)
     */
    protected static float getJavadocVersion( File javadocExe )
        throws IOException, CommandLineException, IllegalArgumentException, PatternSyntaxException
    {
        if ( ( javadocExe == null ) || ( !javadocExe.exists() ) || ( !javadocExe.isFile() ) )
        {
            throw new IOException( "The javadoc executable '" + javadocExe + "' doesn't exist or is not a file. " );
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable( javadocExe.getAbsolutePath() );
        cmd.setWorkingDirectory( javadocExe.getParentFile() );
        cmd.createArgument().setValue( "-J-version" );

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        int exitCode = CommandLineUtils.executeCommandLine( cmd, out, err );

        if ( exitCode != 0 )
        {
            StringBuffer msg = new StringBuffer( "Exit code: " + exitCode + " - " + err.getOutput() );
            msg.append( '\n' );
            msg.append( "Command line was:" + Commandline.toString( cmd.getCommandline() ) );
            throw new CommandLineException( msg.toString() );
        }

        if ( StringUtils.isNotEmpty( err.getOutput() ) )
        {
            return parseJavadocVersion( err.getOutput() );
        }
        else if ( StringUtils.isNotEmpty( out.getOutput() ) )
        {
            return parseJavadocVersion( out.getOutput() );
        }

        throw new IllegalArgumentException( "No output found from the command line 'javadoc -J-version'" );
    }

    /**
     * Parse the output for 'javadoc -J-version' and return the javadoc version recognized.
     * <br/>
     * Here are some output for 'javadoc -J-version' depending the JDK used:
     * <table>
     * <tr>
     *   <th>JDK</th>
     *   <th>Output for 'javadoc -J-version'</th>
     * </tr>
     * <tr>
     *   <td>Sun 1.4</td>
     *   <td>java full version "1.4.2_12-b03"</td>
     * </tr>
     * <tr>
     *   <td>Sun 1.5</td>
     *   <td>java full version "1.5.0_07-164"</td>
     * </tr>
     * <tr>
     *   <td>IBM 1.4</td>
     *   <td>javadoc full version "J2RE 1.4.2 IBM Windows 32 build cn1420-20040626"</td>
     * </tr>
     * <tr>
     *   <td>IBM 1.5 (French JVM)</td>
     *   <td>javadoc version complète de "J2RE 1.5.0 IBM Windows 32 build pwi32pdev-20070426a"</td>
     * </tr>
     * <tr>
     *   <td>FreeBSD 1.5</td>
     *   <td>java full version "diablo-1.5.0-b01"</td>
     * </tr>
     * <tr>
     *   <td>BEA jrockit 1.5</td>
     *   <td>java full version "1.5.0_11-b03"</td>
     * </tr>
     * </table>
     *
     * @param output for 'javadoc -J-version'
     * @return the version of the javadoc for the output.
     * @throws PatternSyntaxException if the output doesn't match with the output pattern <tt>(?s).*?([0-9]+\\.[0-9]+)(\\.([0-9]+))?.*</tt>.
     * @throws IllegalArgumentException if the output is null
     */
    protected static float parseJavadocVersion( String output )
        throws IllegalArgumentException, PatternSyntaxException
    {
        if ( StringUtils.isEmpty( output ) )
        {
            throw new IllegalArgumentException( "The output could not be null." );
        }

        Pattern pattern = Pattern.compile( "(?s).*?([0-9]+\\.[0-9]+)(\\.([0-9]+))?.*" );

        Matcher matcher = pattern.matcher( output );
        if ( !matcher.matches() )
        {
            throw new PatternSyntaxException( "Unrecognized version of Javadoc: '" + output + "'", pattern.pattern(),
                                              pattern.toString().length() - 1 );
        }

        String version = matcher.group( 3 );
        if ( version == null )
        {
            version = matcher.group( 1 );
        }
        else
        {
            version = matcher.group( 1 ) + version;
        }

        return Float.parseFloat( version );
    }

    /**
     * Parse a memory string which be used in the JVM arguments <code>-Xms</code> or <code>-Xmx</code>.
     * <br/>
     * Here are some supported memory string depending the JDK used:
     * <table>
     * <tr>
     *   <th>JDK</th>
     *   <th>Memory argument support for <code>-Xms</code> or <code>-Xmx</code></th>
     * </tr>
     * <tr>
     *   <td>SUN</td>
     *   <td>1024k | 128m | 1g | 1t</td>
     * </tr>
     * <tr>
     *   <td>IBM</td>
     *   <td>1024k | 1024b | 128m | 128mb | 1g | 1gb</td>
     * </tr>
     * <tr>
     *   <td>BEA</td>
     *   <td>1024k | 1024kb | 128m | 128mb | 1g | 1gb</td>
     * </tr>
     * </table>
     *
     * @param memory the memory to be parsed, not null.
     * @return the memory parsed with a supported unit. If no unit specified in the <code>memory</code> parameter,
     * the default unit is <code>m</code>. The units <code>g | gb</code> or <code>t | tb</code> will be converted
     * in <code>m</code>.
     * @throws IllegalArgumentException if the <code>memory</code> parameter is null or doesn't match any pattern.
     */
    protected static String parseJavadocMemory( String memory )
        throws IllegalArgumentException
    {
        if ( StringUtils.isEmpty( memory ) )
        {
            throw new IllegalArgumentException( "The memory could not be null." );
        }

        Pattern p = Pattern.compile( "^\\s*(\\d+)\\s*?\\s*$" );
        Matcher m = p.matcher( memory );
        if ( m.matches() )
        {
            return m.group( 1 ) + "m";
        }

        p = Pattern.compile( "^\\s*(\\d+)\\s*k(b)?\\s*$", Pattern.CASE_INSENSITIVE );
        m = p.matcher( memory );
        if ( m.matches() )
        {
            return m.group( 1 ) + "k";
        }

        p = Pattern.compile( "^\\s*(\\d+)\\s*m(b)?\\s*$", Pattern.CASE_INSENSITIVE );
        m = p.matcher( memory );
        if ( m.matches() )
        {
            return m.group( 1 ) + "m";
        }

        p = Pattern.compile( "^\\s*(\\d+)\\s*g(b)?\\s*$", Pattern.CASE_INSENSITIVE );
        m = p.matcher( memory );
        if ( m.matches() )
        {
            return ( Integer.parseInt( m.group( 1 ) ) * 1024 ) + "m";
        }

        p = Pattern.compile( "^\\s*(\\d+)\\s*t(b)?\\s*$", Pattern.CASE_INSENSITIVE );
        m = p.matcher( memory );
        if ( m.matches() )
        {
            return ( Integer.parseInt( m.group( 1 ) ) * 1024 * 1024 ) + "m";
        }

        throw new IllegalArgumentException( "Could convert not to a memory size: " + memory );
    }

    /**
     * Fetch an URL
     *
     * @param settings the user settings used to fetch the url with an active proxy, if defined.
     * @param url the url to fetch
     * @throws IOException if any
     */
    protected static void fetchURL( Settings settings, URL url )
        throws IOException
    {
        if ( url == null )
        {
            throw new IOException( "The url is null" );
        }

        Properties oldSystemProperties = new Properties();
        oldSystemProperties.putAll( System.getProperties() );

        if ( settings != null )
        {
            String scheme = url.getProtocol();

            if ( !"file".equals( scheme ) )
            {
                Proxy activeProxy = settings.getActiveProxy();
                if ( activeProxy != null )
                {
                    if ( "http".equals( scheme ) || "https".equals( scheme ) || "ftp".equals( scheme ) )
                    {
                        scheme += ".";
                    }
                    else
                    {
                        scheme = "";
                    }

                    if ( StringUtils.isNotEmpty( activeProxy.getHost() ) )
                    {
                        Properties systemProperties = System.getProperties();
                        systemProperties.setProperty( scheme + "proxySet", "true" );
                        systemProperties.setProperty( scheme + "proxyHost", activeProxy.getHost() );

                        if ( activeProxy.getPort() > 0 )
                        {
                            systemProperties
                                .setProperty( scheme + "proxyPort", String.valueOf( activeProxy.getPort() ) );
                        }

                        if ( StringUtils.isNotEmpty( activeProxy.getNonProxyHosts() ) )
                        {
                            systemProperties.setProperty( scheme + "nonProxyHosts", activeProxy.getNonProxyHosts() );
                        }

                        final String userName = activeProxy.getUsername();
                        if ( StringUtils.isNotEmpty( userName ) )
                        {
                            final String pwd = StringUtils.isEmpty( activeProxy.getPassword() ) ? "" : activeProxy
                                .getPassword();
                            Authenticator.setDefault( new Authenticator()
                            {
                                protected PasswordAuthentication getPasswordAuthentication()
                                {
                                    return new PasswordAuthentication( userName, pwd.toCharArray() );
                                }
                            } );
                        }
                    }
                }
            }
        }

        InputStream in = null;
        try
        {
            in = url.openStream();
        }
        finally
        {
            IOUtil.close( in );

            // Reset system properties
            if ( ( settings != null ) && ( !"file".equals( url.getProtocol() ) )
                && ( settings.getActiveProxy() != null )
                && ( StringUtils.isNotEmpty( settings.getActiveProxy().getHost() ) ) )
            {
                System.setProperties( oldSystemProperties );
                Authenticator.setDefault( null );
            }
        }
    }

    /**
     * Validate if a charset is supported on this platform.
     *
     * @param charsetName the charsetName to be check.
     */
    protected static boolean validateEncoding( String charsetName )
    {
        if ( StringUtils.isEmpty( charsetName ) )
        {
            return false;
        }

        OutputStream ost = new ByteArrayOutputStream();
        OutputStreamWriter osw = null;
        try
        {
            osw = new OutputStreamWriter( ost, charsetName );
        }
        catch ( UnsupportedEncodingException exc )
        {
            return false;
        }
        finally
        {
            try
            {
                if ( osw != null )
                {
                    osw.close();
                }
            }
            catch ( IOException exc )
            {
                //nop
            }
        }
        return true;
    }

    /**
     * For security reasons, if an active proxy is defined and needs an authentication by
     * username/password, hide the proxy password in the command line.
     *
     * @param cmdLine a command line, not null
     * @param settings the user settings
     * @return the cmdline with '*' for the http.proxyPassword JVM property
     */
    protected static String hideProxyPassword( String cmdLine, Settings settings )
    {
        if ( cmdLine == null )
        {
            throw new IllegalArgumentException( "cmdLine could not be null" );
        }

        if ( settings == null )
        {
            return cmdLine;
        }

        Proxy activeProxy = settings.getActiveProxy();
        if ( activeProxy != null )
        {
            if ( ( StringUtils.isNotEmpty( activeProxy.getHost() ) )
                && ( StringUtils.isNotEmpty( activeProxy.getUsername() ) )
                && ( StringUtils.isNotEmpty( activeProxy.getPassword() ) ) )
            {
                String pass = "-J-Dhttp.proxyPassword=\"" + activeProxy.getPassword() + "\"";
                String hidepass = "-J-Dhttp.proxyPassword=\""
                    + StringUtils.repeat( "*", activeProxy.getPassword().length() ) + "\"";

                return StringUtils.replace( cmdLine, pass, hidepass );
            }
        }

        return cmdLine;
    }
}
