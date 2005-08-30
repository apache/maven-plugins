package org.apache.maven.plugin.assembly;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assemble an application bundle or distribution.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal assembly
 * @requiresDependencyResolution test
 * @requiresDirectInvocation
 * @execute phase="package"
 */
public class AssemblyMojo
    extends AbstractUnpackingMojo
{

    /**
     * @parameter expression="${maven.assembly.descriptorId}"
     */
    protected String descriptorId;

    /**
     * @parameter expression="${maven.assembly.descriptor}"
     */
    protected File descriptor;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private String basedir;
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}"
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;


    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doExecute();
        }
        catch ( Exception e )
        {
            // TODO: don't catch exception
            throw new MojoExecutionException( "Error creating assembly", e );
        }
    }

    private void doExecute()
        throws ArchiverException, IOException, MojoExecutionException, XmlPullParserException
    {
        Reader r = null;
    
        if ( descriptor != null )
        {
            r = new FileReader( descriptor );
        }
        else if ( descriptorId != null )
        {
            InputStream resourceAsStream = getClass().getResourceAsStream( "/assemblies/" + descriptorId + ".xml" );
            if ( resourceAsStream == null )
            {
                throw new MojoExecutionException( "Descriptor with ID '" + descriptorId + "' not found" );
            }
            r = new InputStreamReader( resourceAsStream );
        }
        else
        {
            // TODO: better exception
            throw new MojoExecutionException( "You must specify descriptor or descriptorId" );
        }
    
        try
        {
            AssemblyXpp3Reader reader = new AssemblyXpp3Reader();
            Assembly assembly = reader.read( r );
    
            // TODO: include dependencies marked for distribution under certain formats
            // TODO: how, might we plug this into an installer, such as NSIS?
            // TODO: allow file mode specifications?
    
            String fullName = finalName + "-" + assembly.getId();
    
            for ( Iterator i = assembly.getFormats().iterator(); i.hasNext(); )
            {
                String format = (String) i.next();
    
                String filename = fullName + "." + format;
    
                // TODO: use component roles? Can we do that in a mojo?
                Archiver archiver = createArchiver( format );
    
                processFileSets( archiver, assembly.getFileSets(), assembly.isIncludeBaseDirectory() );
                processDependencySets( archiver, assembly.getDependencySets(), assembly.isIncludeBaseDirectory() );
    
                File destFile = new File( outputDirectory, filename );
                archiver.setDestFile( destFile );
                archiver.createArchive();
                
                projectHelper.attachArtifact(project, format, format + "-assembly", destFile );
            }
        }
        finally
        {
            IOUtil.close( r );
        }
    }

    private void processDependencySets( Archiver archiver, List dependencySets, boolean includeBaseDirectory )
        throws ArchiverException, IOException, MojoExecutionException
    {
        for ( Iterator i = dependencySets.iterator(); i.hasNext(); )
        {
            DependencySet dependencySet = (DependencySet) i.next();
            String output = dependencySet.getOutputDirectory();
            output = getOutputDirectory( output, includeBaseDirectory );

            archiver.setDefaultDirectoryMode( Integer.parseInt( 
                    dependencySet.getDirectoryMode(), 8 ) );

            archiver.setDefaultFileMode( Integer.parseInt( 
                    dependencySet.getFileMode(), 8 ) );

            getLog().debug("DependencySet["+output+"]" +
                " dir perms: " + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) +
                " file perms: " + Integer.toString( archiver.getDefaultFileMode(), 8 ) );

            AndArtifactFilter filter = new AndArtifactFilter();
            filter.add( new ScopeArtifactFilter( dependencySet.getScope() ) );

            if ( !dependencySet.getIncludes().isEmpty() )
            {
                filter.add( new IncludesArtifactFilter( dependencySet.getIncludes() ) );
            }
            if ( !dependencySet.getExcludes().isEmpty() )
            {
                filter.add( new ExcludesArtifactFilter( dependencySet.getExcludes() ) );
            }

            // TODO: includes and excludes
            for ( Iterator j = dependencies.iterator(); j.hasNext(); )
            {
                Artifact artifact = (Artifact) j.next();

                if ( filter.include( artifact ) )
                {
                    String name = artifact.getFile().getName();
                    
                    if ( dependencySet.isUnpack() )
                    {
                        // TODO: something like zipfileset in plexus-archiver
//                        archiver.addJar(  )

                        File tempLocation = new File( workDirectory, name.substring( 0, name.length() - 4 ) );
                        boolean process = false;
                        if ( !tempLocation.exists() )
                        {
                            tempLocation.mkdirs();
                            process = true;
                        }
                        else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
                        {
                            process = true;
                        }

                        if ( process )
                        {
                            unpack( artifact.getFile(), tempLocation );
                        }
                        archiver.addDirectory( tempLocation, null,
                                               (String[]) getJarExcludes().toArray( EMPTY_STRING_ARRAY ) );
                    }
                    else
                    {
                        archiver.addFile( artifact.getFile(), output +
                            evaluateFileNameMapping( dependencySet.getOutputFileNameMapping(), artifact ) );
                    }
                }
            }
        } 
    }


    private void processFileSets( Archiver archiver, List fileSets, boolean includeBaseDirecetory )
        throws ArchiverException
    {
        for ( Iterator i = fileSets.iterator(); i.hasNext(); )
        {
            FileSet fileSet = (FileSet) i.next();
            String directory = fileSet.getDirectory();
            String output = fileSet.getOutputDirectory();

            archiver.setDefaultDirectoryMode( Integer.parseInt( 
                    fileSet.getDirectoryMode(), 8 ) );

            archiver.setDefaultFileMode( Integer.parseInt( 
                    fileSet.getFileMode(), 8 ) );
            
            getLog().debug("FileSet["+output+"]" +
                " dir perms: " + Integer.toString( archiver.getDefaultDirectoryMode(), 8 ) +
                " file perms: " + Integer.toString( archiver.getDefaultFileMode(), 8 ) );

            if ( directory == null )
            {
                directory = basedir;
                if ( output == null )
                {
                    output = "";
                }
            }
            else
            {
                if ( output == null )
                {
                    output = directory;
                }
            }
            output = getOutputDirectory( output, includeBaseDirecetory );
    
            String[] includes = (String[]) fileSet.getIncludes().toArray( EMPTY_STRING_ARRAY );
            if ( includes.length == 0 )
            {
                includes = null;
            }
    
            List excludesList = fileSet.getExcludes();
            excludesList.addAll( getDefaultExcludes() );
            String[] excludes = (String[]) excludesList.toArray( EMPTY_STRING_ARRAY );
    
            // TODO: default excludes should be in the archiver?
            archiver.addDirectory( new File( directory ), output, includes, excludes );
        }
    }
    
    private static String evaluateFileNameMapping( String expression, Artifact artifact )
        throws MojoExecutionException
    {
        // this matches the last ${...} string
        Pattern pat = Pattern.compile( "^(.*)\\$\\{([^\\}]+)\\}(.*)$" );
        Matcher mat = pat.matcher( expression );
    
        String left,right;
        Object middle;
    
        if ( mat.matches() )
        {
            left = evaluateFileNameMapping( mat.group( 1 ), artifact );
            try
            {
                middle = ReflectionValueExtractor.evaluate( "dep." + mat.group( 2 ), artifact );
            }
            catch (Exception e)
            {
                throw new MojoExecutionException("Cannot evaluate filenameMapping", e);
            }
            right = mat.group( 3 );
    
            if ( middle == null )
            {
                // TODO: There should be a more generic way dealing with that. Having magic words is not good at all.
                // probe for magic word
                if ( mat.group( 2 ).trim().equals( "extension" ) )
                {
                    ArtifactHandler artifactHandler = artifact.getArtifactHandler();
                    middle = artifactHandler.getExtension();
                }
                else
                {
                    middle = "${" + mat.group( 2 ) + "}";
                }
            }
    
            return left + middle + right;
        }
    
        return expression;
    }
    
    private static List getJarExcludes()
    {
        List l = new ArrayList( getDefaultExcludes() );
        l.add( "META-INF/**" );
        return l;
    }
    
    private String getOutputDirectory( String output, boolean includeBaseDirectory )
    {
        if ( output == null )
        {
            output = "";
        }
        if ( !output.endsWith( "/" ) && !output.endsWith( "\\" ) )
        {
            // TODO: shouldn't archiver do this?
            output += '/';
        }
    
        if ( includeBaseDirectory )
        {
            if ( output.startsWith( "/" ) )
            {
                output = finalName + output;
            }
            else
            {
                output = finalName + "/" + output;
            }
        }
        else
        {
            if ( output.startsWith( "/" ) )
            {
                output = output.substring( 1 );
            }
        }
        return output;
    }
    
    private static Archiver createArchiver( String format )
        throws ArchiverException
    {
        Archiver archiver;
        if ( format.startsWith( "tar" ) )
        {
            TarArchiver tarArchiver = new TarArchiver();
            archiver = tarArchiver;
            int index = format.indexOf( '.' );
            if ( index >= 0 )
            {
                // TODO: this needs a cleanup in plexus archiver - use a real typesafe enum
                TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
                // TODO: this should accept gz and bz2 as well so we can skip over the switch
                String compression = format.substring( index + 1 );
                if ( compression.equals( "gz" ) )
                {
                    tarCompressionMethod.setValue( "gzip" );
                }
                else if ( compression.equals( "bz2" ) )
                {
                    tarCompressionMethod.setValue( "bzip2" );
                }
                else
                {
                    // TODO: better handling
                    throw new IllegalArgumentException( "Unknown compression format: " + compression );
                }
                tarArchiver.setCompression( tarCompressionMethod );
            }
        }
        else if ( format.startsWith( "zip" ) )
        {
            archiver = new ZipArchiver();
        }
        else if ( format.startsWith( "jar" ) )
        {
            // TODO: use MavenArchiver for manifest?
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setCompress( true );
            archiver = jarArchiver;
        }
        else
        {
            // TODO: better handling
            throw new IllegalArgumentException( "Unknown format: " + format );
        }
        return archiver;
    }
    
    public static List getDefaultExcludes()
    {
        List defaultExcludes = new ArrayList();
        defaultExcludes.add( "**/*~" );
        defaultExcludes.add( "**/#*#" );
        defaultExcludes.add( "**/.#*" );
        defaultExcludes.add( "**/%*%" );
        defaultExcludes.add( "**/._*" );
    
        // CVS
        defaultExcludes.add( "**/CVS" );
        defaultExcludes.add( "**/CVS/**" );
        defaultExcludes.add( "**/.cvsignore" );
    
        // SCCS
        defaultExcludes.add( "**/SCCS" );
        defaultExcludes.add( "**/SCCS/**" );
    
        // Visual SourceSafe
        defaultExcludes.add( "**/vssver.scc" );
    
        // Subversion
        defaultExcludes.add( "**/.svn" );
        defaultExcludes.add( "**/.svn/**" );
    
        // Mac
        defaultExcludes.add( "**/.DS_Store" );
    
        return defaultExcludes;
    }
    
}
