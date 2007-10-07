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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

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
     * Method that removes the invalid directories in the specified source directories
     *
     * @param sourceDirs the list of source directories that will be validated
     * @return a List of valid source directories
     */
    // TODO: could be better aligned with JXR, including getFiles() vs hasSources that finds java files.
    protected static List pruneSourceDirs( List sourceDirs )
    {
        List pruned = new ArrayList( sourceDirs.size() );
        for ( Iterator i = sourceDirs.iterator(); i.hasNext(); )
        {
            String dir = (String) i.next();
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
            List docFiles = FileUtils.getDirectoryNames( javadocDir, "**/doc-files", null, false, true );
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
     * Call the javadoc tool to have its version
     *
     * @param javadocExe
     * @return the javadoc version as float
     * @throws IOException if any
     * @throws CommandLineException if any
     */
    protected static float getJavadocVersion( File javadocExe )
        throws IOException, CommandLineException
    {
        if ( !javadocExe.exists() || !javadocExe.isFile() )
        {
            throw new IOException( "The javadoc executable '" + javadocExe + "' doesn't exist or is not a file. " );
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable( javadocExe.getAbsolutePath() );
        cmd.setWorkingDirectory( javadocExe.getParentFile() );
        cmd.createArgument().setValue( "-J-fullversion" );

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

        /*
         * Exemple: java full version "1.5.0_11-b03"
         *
         * @see com.sun.tools.javac.main.JavaCompiler#fullVersion()
         */
        StringTokenizer token = new StringTokenizer( err.getOutput(), "\"" );
        token.nextToken();

        String version = token.nextToken();
        String str = version.substring( 0, 3 );
        if ( version.length() >= 5 )
        {
            str = str + version.substring( 4, 5 );
        }

        return Float.parseFloat( str );
    }

    /**
     * Fetch an URL
     *
     * @param settings the user settings used to fetch the url like a proxy
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

        if ( settings != null )
        {
            String scheme = url.getProtocol();
            if ( !"file".equals( scheme ) )
            {
                Proxy proxy = settings.getActiveProxy();
                if ( proxy != null )
                {
                    if ( "http".equals( scheme ) || "https".equals( scheme ) )
                    {
                        scheme = "http.";
                    }
                    else if ( "ftp".equals( scheme ) )
                    {
                        scheme = "ftp.";
                    }
                    else
                    {
                        scheme = "";
                    }

                    String host = proxy.getHost();
                    if ( !StringUtils.isEmpty( host ) )
                    {
                        Properties p = System.getProperties();
                        p.setProperty( scheme + "proxySet", "true" );
                        p.setProperty( scheme + "proxyHost", host );
                        p.setProperty( scheme + "proxyPort", String.valueOf( proxy.getPort() ) );
                        if ( !StringUtils.isEmpty( proxy.getNonProxyHosts() ) )
                        {
                            p.setProperty( scheme + "nonProxyHosts", proxy.getNonProxyHosts() );
                        }

                        final String userName = proxy.getUsername();
                        if ( !StringUtils.isEmpty( userName ) )
                        {
                            final String pwd = StringUtils.isEmpty( proxy.getPassword() ) ? "" : proxy.getPassword();
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
        }
    }
}
