package org.apache.maven.plugin.assembly.mojos;

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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.io.AssemblyReadException;
import org.apache.maven.plugin.assembly.io.AssemblyReader;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractAssemblyMojo
    extends AbstractMojo
    implements AssemblerConfigurationSource
{

    /**
     * Local Maven repository where artifacts are cached during the build process.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteRepositories;

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter default-value="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * The output directory of the assembled distribution file.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The filename of the assembled distribution file.
     *
     * @parameter default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Directory to unpack JARs into if needed
     *
     * @parameter default-value="${project.build.directory}/assembly/work"
     * @required
     */
    private File workDirectory;

    /**
     * This is the artifact classifier to be used for the resultant assembly artifact. Normally, you would use the
     * assembly-id instead of specifying this here.
     *
     * @parameter expression="${classifier}"
     * @deprecated Please use the Assembly's id for classifier instead
     */
    private String classifier;

    /**
     * A list of descriptor files to generate from.
     *
     * @parameter
     */
    private String[] descriptors;

    /**
     * A list of built-in descriptor references to generate from. You can select from <code>bin</code>,
     * <code>jar-with-dependencies</code>, or <code>src</code>.
     *
     * @parameter
     */
    private String[] descriptorRefs;

    /**
     * directory to scan for descriptor files in
     *
     * @parameter
     */
    private File descriptorSourceDirectory;

    /**
     * This is the base directory from which archive files are created.
     * This base directory pre-pended to any <code>&lt;directory&gt;</code>
     * specifications in the assembly descriptor.  This is an optional
     * parameter
     *
     * @parameter
     */
    private File archiveBaseDirectory;

    /**
     * Predefined Assembly Descriptor Id's.  You can select bin, jar-with-dependencies, or src.
     *
     * @parameter expression="${descriptorId}"
     * @deprecated Please use descriptorRefs instead
     */
    protected String descriptorId;

    /**
     * Assembly XML Descriptor file.  This must be the path to your customized descriptor file.
     *
     * @parameter expression="${descriptor}"
     * @deprecated Please use descriptors instead
     */
    protected String descriptor;

    /**
     * Sets the TarArchiver behavior on file paths with more than 100 characters length.
     * Valid values are: "warn" (default), "fail", "truncate", "gnu", or "omit".
     *
     * @parameter expression="${tarLongFileMode}" default-value="warn"
     */
    private String tarLongFileMode;

    /**
     * Base directory of the project.
     *
     * @parameter default-value="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * Maven ProjectHelper
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Temporary directory that contain the files to be assembled.
     *
     * @parameter default-value="${project.build.directory}/archive-tmp"
     * @required
     * @readonly
     */
    private File tempRoot;

    /**
     * Directory for site generated.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     * @readonly
     */
    private File siteDirectory;

    /**
     * Set to true to include the site generated by site:site goal.
     *
     * @parameter expression="${includeSite}" default-value="false"
     * @deprecated Please set this variable in the assembly descriptor instead
     */
    private boolean includeSite;

    /**
     * Set to false to exclude the assembly id from the assembly final name.
     *
     * @parameter expression="${appendAssemblyId}" default-value="true"
     */
    protected boolean appendAssemblyId;

    /**
     * This is a set of instructions to the archive builder, especially for building .jar files. It enables you to
     * specify a Manifest file for the jar, in addition to other options.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive;

    /**
     * @parameter
     */
    protected List filters;

    /**
     * @parameter expression="${attach}" default-value="true"
     */
    private boolean attach;

    /**
     * @component
     */
    private AssemblyArchiver assemblyArchiver;

    /**
     * @component
     */
    private AssemblyReader assemblyReader;

    /**
     * Create the binary distribution.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        List assemblies;
        try
        {
            assemblies = assemblyReader.readAssemblies( this );
        }
        catch ( AssemblyReadException e )
        {
            throw new MojoExecutionException( "Error reading assemblies: " + e.getMessage(), e );
        }
        catch ( InvalidAssemblerConfigurationException e )
        {
            throw new MojoFailureException( assemblyReader, e.getMessage(), "Mojo configuration is invalid: " + e.getMessage() );
        }

        // TODO: include dependencies marked for distribution under certain formats
        // TODO: how, might we plug this into an installer, such as NSIS?

        for ( Iterator assemblyIterator = assemblies.iterator(); assemblyIterator.hasNext(); )
        {
            Assembly assembly = (Assembly) assemblyIterator.next();
            try
            {
                String fullName = AssemblyFormatUtils.getDistributionName( assembly, this );

                for ( Iterator formatIterator = assembly.getFormats().iterator(); formatIterator.hasNext(); )
                {
                    String format = (String) formatIterator.next();

                    File destFile = assemblyArchiver.createArchive( assembly, fullName, format, this );

                    MavenProject project = getProject();
                    String classifier = getClassifier();

                    if ( attach && destFile.isFile() )
                    {
                        if ( isAssemblyIdAppended() )
                        {
                            projectHelper.attachArtifact( project, format, assembly.getId(), destFile );
                        }
                        else if ( classifier != null )
                        {
                            projectHelper.attachArtifact( project, format, classifier, destFile );
                        }
                        else
                        {
                            StringBuffer message = new StringBuffer();

                            message.append( "Configuration options: 'appendAssemblyId' is set to false, and 'classifier' is missing." );
                            message.append( "\nInstead of attaching the assembly file: " ).append( destFile ).append( ", it will become the file for main project artifact." );
                            message.append( "\nNOTE: If multiple descriptors or descriptor-formats are provided for this project, the value of this file will be non-deterministic!" );

                            getLog().warn( message );
                            project.getArtifact().setFile( destFile );
                        }
                    }
                    else
                    {
                        getLog().warn( "Assembly file: " + destFile + " is not a regular file (it may be a directory). It cannot be attached to the project build for installation or deployment." );
                    }
                }
            }
            catch ( ArchiveCreationException e )
            {
                throw new MojoExecutionException( "Failed to create assembly: " + e.getMessage(), e );
            }
            catch ( AssemblyFormattingException e )
            {
                throw new MojoExecutionException( "Failed to create assembly: " + e.getMessage(), e );
            }
            catch ( InvalidAssemblerConfigurationException e )
            {
                throw new MojoFailureException( assembly, "Assembly is incorrectly configured: " + assembly.getId(), "Assembly: "
                                + assembly.getId() + " is not configured correctly: " + e.getMessage() );
            }
        }
    }

    protected AssemblyArchiver getAssemblyArchiver()
    {
        return assemblyArchiver;
    }

    protected AssemblyReader getAssemblyReader()
    {
        return assemblyReader;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public String getDescriptor()
    {
        return descriptor;
    }

    public String getDescriptorId()
    {
        return descriptorId;
    }

    public String[] getDescriptorReferences()
    {
        return descriptorRefs;
    }

    public File getDescriptorSourceDirectory()
    {
        return descriptorSourceDirectory;
    }

    public String[] getDescriptors()
    {
        return descriptors;
    }

    public abstract MavenProject getProject();

    public File getSiteDirectory()
    {
        return siteDirectory;
    }

    public boolean isSiteIncluded()
    {
        return includeSite;
    }

    public String getFinalName()
    {
        return finalName;
    }

    public boolean isAssemblyIdAppended()
    {
        return appendAssemblyId;
    }

    public String getTarLongFileMode()
    {
        return tarLongFileMode;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public MavenArchiveConfiguration getJarArchiveConfiguration()
    {
        return archive;
    }

    public File getWorkingDirectory()
    {
        return workDirectory;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public File getTemporaryRootDirectory()
    {
        return tempRoot;
    }

    public File getArchiveBaseDirectory()
    {
        return archiveBaseDirectory;
    }

    public List getFilters()
    {
        if ( filters == null )
        {
            filters = getProject().getBuild().getFilters();
        }
        return filters;
    }

    public List getReactorProjects()
    {
        return reactorProjects;
    }

    public String getClassifier()
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected MavenProjectHelper getProjectHelper()
    {
        return projectHelper;
    }

    public void setAppendAssemblyId( boolean appendAssemblyId )
    {
        this.appendAssemblyId = appendAssemblyId;
    }

    public void setArchive( MavenArchiveConfiguration archive )
    {
        this.archive = archive;
    }

    public void setArchiveBaseDirectory( File archiveBaseDirectory )
    {
        this.archiveBaseDirectory = archiveBaseDirectory;
    }

    public void setAssemblyArchiver( AssemblyArchiver assemblyArchiver )
    {
        this.assemblyArchiver = assemblyArchiver;
    }

    public void setAssemblyReader( AssemblyReader assemblyReader )
    {
        this.assemblyReader = assemblyReader;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public void setDescriptor( String descriptor )
    {
        this.descriptor = descriptor;
    }

    public void setDescriptorId( String descriptorId )
    {
        this.descriptorId = descriptorId;
    }

    public void setDescriptorRefs( String[] descriptorRefs )
    {
        this.descriptorRefs = descriptorRefs;
    }

    public void setDescriptors( String[] descriptors )
    {
        this.descriptors = descriptors;
    }

    public void setDescriptorSourceDirectory( File descriptorSourceDirectory )
    {
        this.descriptorSourceDirectory = descriptorSourceDirectory;
    }

    public void setFilters( List filters )
    {
        this.filters = filters;
    }

    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    }

    public void setIncludeSite( boolean includeSite )
    {
        this.includeSite = includeSite;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setProjectHelper( MavenProjectHelper projectHelper )
    {
        this.projectHelper = projectHelper;
    }

    public void setReactorProjects( List reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    public void setSiteDirectory( File siteDirectory )
    {
        this.siteDirectory = siteDirectory;
    }

    public void setTarLongFileMode( String tarLongFileMode )
    {
        this.tarLongFileMode = tarLongFileMode;
    }

    public void setTempRoot( File tempRoot )
    {
        this.tempRoot = tempRoot;
    }

    public void setWorkDirectory( File workDirectory )
    {
        this.workDirectory = workDirectory;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

}
