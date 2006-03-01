package org.apache.maven.plugin.idea;

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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Collections;

/**
 * Goal for generating IDEA files from a POM.
 * This plug-in provides the ability to generate IDEA project files (.ipr and .iws files) for IDEA
 *
 * @goal idea
 * @execute phase="generate-sources"
 * @todo use dom4j or something. Xpp3Dom can't cope properly with entities and so on
 */
public class IdeaMojo
    extends AbstractMojo
{
    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Maven Project.
     *
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * Specify the name of the registered IDEA JDK to use
     * for the project.
     *
     * @parameter expression="${jdkName}"
     */
    private String jdkName;

    /**
     * Specify the version of the JDK to use for the project for the purpose of enabled assertions and 5.0 language features.
     * The default value is the specification version of the executing JVM.
     *
     * @parameter expression="${jdkLevel}"
     * @todo would be good to use the compilation source if possible
     */
    private String jdkLevel;

    /**
     * Specify the resource pattern in wildcard format, for example "?*.xml;?*.properties".
     * Currently supports 4.x and 5.x.
     * The default value is any file without a java extension ("!?*.java").
     * Because IDEA doesn't distinguish between source and resources directories, this is needed.
     * Please note that the default value includes package.html files as it's not possible to exclude those.
     *
     * @parameter expression="${wildcardResourcePatterns}" default-value="!?*.java"
     */
    private String wildcardResourcePatterns;

    /**
     * Specify the version of idea to use.  This is needed to identify the default formatting of
     * project-jdk-name used by idea.  Currently supports 4.x and 5.x.
     * <p/>
     * This will only be used when parameter jdkName is not set.
     *
     * @parameter expression="${ideaVersion}"
     * default-value="5.x"
     */
    private String ideaVersion;

    /**
     * Whether to update the existing project files or overwrite them.
     *
     * @parameter expression="${overwrite}" default-value="false"
     */
    private boolean overwrite;

    /**
     * Whether to link the reactor projects as dependency modules or as libraries.
     *
     * @parameter expression="${linkModules}" default-value="true"
     */
    private boolean linkModules;

    /**
     * The reactor projects in a multi-module build.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * Whether to use full artifact names when referencing libraries.
     *
     * @parameter expression="${useFullNames}" default-value="false"
     */
    private boolean useFullNames;

    /**
     * Switch to enable or disable the inclusion of sources and javadoc references to the project's library
     *
     * @parameter expression="${useClassifiers}" default-value="false"
     */
    private boolean useClassifiers;

    /**
     * Sets the classifier string attached to an artifact source archive name
     *
     * @parameter expression="${sourceClassifier}" default-value="sources"
     */
    private String sourceClassifier;

    /**
     * Sets the classifier string attached to an artifact javadoc archive name
     *
     * @parameter expression="${javadocClassifier}" default-value="javadoc"
     */
    private String javadocClassifier;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.manager.WagonManager}"
     * @required
     * @readonly
     */
    private WagonManager wagonManager;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepo;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     */
    private ArtifactResolver artifactResolver;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.metadata.ArtifactMetadataSource}"
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * A temporary cache of artifacts that's already been downloaded or
     * attempted to be downloaded. This is to refrain from trying to download a
     * dependency that we have already tried to download.
     *
     * @todo this is nasty! the only reason this is static is to use the same cache between reactor calls
     */
    private static Map attemptedDownloads = new HashMap();

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doDependencyResolution();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        rewriteModule();

        if ( project.isExecutionRoot() )
        {
            rewriteProject();

            rewriteWorkspace();
        }
    }

    private void doDependencyResolution()
        throws InvalidDependencyVersionException, ProjectBuildingException, ArtifactNotFoundException,
        ArtifactResolutionException
    {
        if ( project.getDependencies() != null )
        {
            List missingArtifacts = new ArrayList();

            Map managedVersions = createManagedVersionMap( project.getId(), project.getDependencyManagement() );

            ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.createArtifacts( artifactFactory, Artifact.SCOPE_TEST, null ), project.getArtifact(), managedVersions,
                                                  localRepo, project.getRemoteArtifactRepositories(),
                                                  artifactMetadataSource );

            //project.setArtifacts( project.createArtifacts( artifactFactory, Artifact.SCOPE_TEST, null ) );
            project.setArtifacts( result.getArtifacts() );

            for ( Iterator artifacts = project.getTestArtifacts().iterator(); artifacts.hasNext(); )
            {
                Artifact artifact = (Artifact) artifacts.next();

                artifact.setFile( new File( localRepo.getBasedir(), localRepo.pathOf( artifact ) ) );

                if ( !artifact.getFile().exists() )
                {
                    try
                    {
                        wagonManager.getArtifact( artifact, project.getRemoteArtifactRepositories() );
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        getLog().debug( "Unable to resolve a project dependency: " + artifact.getId(), e  );

                        missingArtifacts.add( artifact );
                    }
                    catch ( TransferFailedException e )
                    {
                        getLog().debug( "Unable to resolve a project dependency: " + artifact.getId(), e );

                        missingArtifacts.add( artifact );
                    }
                }
            }

            if ( missingArtifacts.size() > 0 )
            {
                StringBuffer warnMsg = new StringBuffer();

                warnMsg.append( "The following artifacts failed to resolve\n\n" );

                for( Iterator artifacts = missingArtifacts.iterator(); artifacts.hasNext(); )
                {
                    Artifact artifact = (Artifact) artifacts.next();

                    warnMsg.append( "    " + artifact.getId() + "\n" );
                }

                warnMsg.append( "\nfor the project " + project.getId() + "\n" );

                getLog().warn( warnMsg );
            }
        }
    }

    /**
     * Create IDEA workspace (.iws) file.
     *
     * @throws MojoExecutionException
     */
    private void rewriteWorkspace()
        throws MojoExecutionException
    {
        File workspaceFile = new File( project.getBasedir(), project.getArtifactId() + ".iws" );

        FileWriter writer = null;

        Reader reader = null;

        Xpp3Dom module;

        try
        {
            if ( workspaceFile.exists() && !overwrite )
            {
                reader = new FileReader( workspaceFile );
            }
            else
            {
                reader = new InputStreamReader( getClass().getResourceAsStream( "/templates/default/workspace.xml" ) );
            }
            module = Xpp3DomBuilder.build( reader );

            setProjectScmType( module );

            writer = new FileWriter( workspaceFile );

            Xpp3DomWriter.write( writer, module );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing existing IWS file: " + workspaceFile.getAbsolutePath(),
                                              e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create workspace file", e );
        }
        finally
        {
            IOUtil.close( reader );

            IOUtil.close( writer );
        }
    }

    /**
     * Create IDEA (.ipr) project files.
     *
     * @throws MojoExecutionException
     */
    private void rewriteProject()
        throws MojoExecutionException
    {
        File projectFile = new File( project.getBasedir(), project.getArtifactId() + ".ipr" );
        try
        {
            Reader reader;
            if ( projectFile.exists() && !overwrite )
            {
                reader = new FileReader( projectFile );
            }
            else
            {
                reader = new InputStreamReader( getClass().getResourceAsStream( "/templates/default/project.xml" ) );
            }

            Xpp3Dom module;
            try
            {
                module = Xpp3DomBuilder.build( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            // Set the jdk name if set
            if ( jdkName != null )
            {
                setJdkName( module, jdkName );
            }
            else
            {
                String javaVersion = System.getProperty( "java.version" );
                String defaultJdkName;

                if ( ideaVersion.startsWith( "4" ) )
                {
                    defaultJdkName = "java version &quot;" + javaVersion + "&quot;";
                }
                else
                {
                    defaultJdkName = javaVersion.substring( 0, 3 );
                }
                getLog().info( "jdkName is not set, using [java version" + javaVersion + "] as default." );
                setJdkName( module, defaultJdkName );
            }

            setWildcardResourcePatterns( module, wildcardResourcePatterns );

            Xpp3Dom component = findComponent( module, "ProjectModuleManager" );
            Xpp3Dom modules = findElement( component, "modules" );

            removeOldElements( modules, "module" );

            if ( project.getCollectedProjects().size() > 0 )
            {
                Xpp3Dom m = createElement( modules, "module" );
                String projectPath =
                    new File( project.getBasedir(), project.getArtifactId() + ".iml" ).getAbsolutePath();
                m.setAttribute( "filepath", "$PROJECT_DIR$/" + toRelative( project.getBasedir(), projectPath ) );

                for ( Iterator i = project.getCollectedProjects().iterator(); i.hasNext(); )
                {
                    MavenProject p = (MavenProject) i.next();

                    m = createElement( modules, "module" );
                    String modulePath = new File( p.getBasedir(), p.getArtifactId() + ".iml" ).getAbsolutePath();
                    m.setAttribute( "filepath", "$PROJECT_DIR$/" + toRelative( project.getBasedir(), modulePath ) );
                }
            }
            else
            {
                Xpp3Dom m = createElement( modules, "module" );
                String modulePath =
                    new File( project.getBasedir(), project.getArtifactId() + ".iml" ).getAbsolutePath();
                m.setAttribute( "filepath", "$PROJECT_DIR$/" + toRelative( project.getBasedir(), modulePath ) );
            }

            FileWriter writer = new FileWriter( projectFile );
            try
            {
                Xpp3DomWriter.write( writer, module );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing existing IPR file: " + projectFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error parsing existing IPR file: " + projectFile.getAbsolutePath(), e );
        }
    }

    /**
     * Create IDEA (.iml) project files.
     *
     * @throws MojoExecutionException
     */
    private void rewriteModule()
        throws MojoExecutionException
    {
        File moduleFile = new File( project.getBasedir(), project.getArtifactId() + ".iml" );
        try
        {
            Reader reader;
            if ( moduleFile.exists() && !overwrite )
            {
                reader = new FileReader( moduleFile );
            }
            else
            {
                reader = new InputStreamReader( getClass().getResourceAsStream( "/templates/default/module.xml" ) );
            }

            Xpp3Dom module;
            try
            {
                module = Xpp3DomBuilder.build( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            // TODO: how can we let the WAR/EJBs plugin hook in and provide this?
            // TODO: merge in ejb-module, etc.
            if ( "war".equals( project.getPackaging() ) )
            {
                addWebModule( module );
            }
            else if ( "ejb".equals( project.getPackaging() ) )
            {
                module.setAttribute( "type", "J2EE_EJB_MODULE" );
            }

            Xpp3Dom component = findComponent( module, "NewModuleRootManager" );
            Xpp3Dom output = findElement( component, "output" );
            output.setAttribute( "url", getModuleFileUrl( project.getBuild().getOutputDirectory() ) );

            Xpp3Dom outputTest = findElement( component, "output-test" );
            outputTest.setAttribute( "url", getModuleFileUrl( project.getBuild().getTestOutputDirectory() ) );

            Xpp3Dom content = findElement( component, "content" );

            removeOldElements( content, "sourceFolder" );

            for ( Iterator i = executedProject.getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String directory = (String) i.next();
                addSourceFolder( content, directory, false );
            }
            for ( Iterator i = executedProject.getTestCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String directory = (String) i.next();
                addSourceFolder( content, directory, true );
            }

            List resourceDirectory = new ArrayList();
            for ( Iterator i = project.getBuild().getResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                resourceDirectory.add( resource.getDirectory() );
            }

            for ( Iterator i = project.getBuild().getTestResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                String directory = resource.getDirectory();
                addSourceFolder( content, directory, true );
            }

            removeOldElements( content, "excludeFolder" );

            //For excludeFolder
            File target = new File( project.getBuild().getDirectory() );

            List excludeFolders = new ArrayList();
            excludeFolders.add( new File( project.getBuild().getOutputDirectory() ).getAbsolutePath() );
            excludeFolders.add( new File( project.getBuild().getTestOutputDirectory() ).getAbsolutePath() );

            if ( target.exists() )
            {
                File[] fileNames = target.listFiles();

                for ( int i = 0; i < fileNames.length; i++ )
                {
                    File fileName = fileNames[i];
                    if ( fileName.isDirectory() )
                    {
                        String absolutePath = fileName.getAbsolutePath();
                        if ( !executedProject.getCompileSourceRoots().contains( absolutePath ) )
                        {
                            if ( !excludeFolders.contains( absolutePath ) )
                            {
                                excludeFolders.add( absolutePath );
                            }
                        }
                    }
                }

                for ( Iterator i = excludeFolders.iterator(); i.hasNext(); )
                {
                    addExcludeFolder( content, i.next().toString() );
                }
            }

            removeOldDependencies( component );

            List testClasspathElements = project.getTestArtifacts();
            for ( Iterator i = testClasspathElements.iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();
                Xpp3Dom dep = createElement( component, "orderEntry" );

                boolean found = false;
                if ( reactorProjects != null && linkModules )
                {
                    for ( Iterator j = reactorProjects.iterator(); j.hasNext() && !found; )
                    {
                        MavenProject p = (MavenProject) j.next();
                        if ( p.getGroupId().equals( a.getGroupId() ) && p.getArtifactId().equals( a.getArtifactId() ) )
                        {
                            dep.setAttribute( "type", "module" );
                            dep.setAttribute( "module-name", a.getArtifactId() );
                            found = true;
                        }
                    }
                }

                if ( a.getFile() != null && !found )
                {
                    dep.setAttribute( "type", "module-library" );
                    dep = createElement( dep, "library" );
                    String moduleName;
                    if ( useFullNames )
                    {
                        moduleName =
                            a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getType() + ':' + a.getVersion();
                    }
                    else
                    {
                        moduleName = a.getArtifactId();
                    }
                    dep.setAttribute( "name", moduleName );

                    Xpp3Dom el = createElement( dep, "CLASSES" );
                    el = createElement( el, "root" );
                    File file = a.getFile();
                    el.setAttribute( "url", "jar://" + file.getAbsolutePath().replace( '\\', '/' ) + "!/" );

                    if ( useClassifiers )
                    {
                        resolveClassifier( createElement( dep, "JAVADOC" ), a, javadocClassifier );
                        resolveClassifier( createElement( dep, "SOURCES" ), a, sourceClassifier );
                    }
                }
            }

            for ( Iterator resourceDirs = resourceDirectory.iterator(); resourceDirs.hasNext(); )
            {
                String resourceDir = (String) resourceDirs.next();

                getLog().info( "Adding resource directory: " + resourceDir );

                addResources( component, resourceDir );
            }

            FileWriter writer = new FileWriter( moduleFile );
            try
            {
                Xpp3DomWriter.write( writer, module );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file " + moduleFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file " + moduleFile.getAbsolutePath(), e );
        }
    }

    private void addResources( Xpp3Dom component, String directory )
    {
        Xpp3Dom dep = createElement( component, "orderEntry" );
        dep.setAttribute( "type", "module-library" );
        dep = createElement( dep, "library" );
        dep.setAttribute( "name", "resources" );

        Xpp3Dom el = createElement( dep, "CLASSES" );
        el = createElement( el, "root" );
        el.setAttribute( "url", getModuleFileUrl( directory ) );

        createElement( dep, "JAVADOC" );
        createElement( dep, "SOURCES" );
    }

    private void resolveClassifier( Xpp3Dom element, Artifact a, String classifier )
    {
        String id = a.getId() + '-' + classifier;

        String path;
        if ( attemptedDownloads.containsKey( id ) )
        {
            getLog().debug( id + " was already downloaded." );
            path = (String) attemptedDownloads.get( id );
        }
        else
        {
            getLog().debug( id + " was not attempted to be downloaded yet: trying..." );
            path = resolveClassifiedArtifact( a, classifier );
            attemptedDownloads.put( id, path );
        }

        if ( path != null )
        {
            String jarPath = "jar://" + path + "!/";
            getLog().debug( "Setting " + classifier + " for " + id + " to " + jarPath );
            createElement( element, "root" ).setAttribute( "url", jarPath );
        }
    }

    private String resolveClassifiedArtifact( Artifact artifact, String classifier )
    {
        String basePath = artifact.getFile().getAbsolutePath().replace( '\\', '/' );
        int delIndex = basePath.indexOf( ".jar" );
        if ( delIndex < 0 )
        {
            return null;
        }

        List remoteRepos = project.getRemoteArtifactRepositories();
        try
        {
            Artifact classifiedArtifact = artifactFactory.createArtifactWithClassifier( artifact.getGroupId(),
                                                                                        artifact.getArtifactId(),
                                                                                        artifact.getVersion(),
                                                                                        artifact.getType(),
                                                                                        classifier );
            String dstFilename = basePath.substring( 0, delIndex ) + '-' + classifier + ".jar";
            File dstFile = new File( dstFilename );
            classifiedArtifact.setFile( dstFile );
            //this check is here because wagonManager does not seem to check if the remote file is newer
            //    or such feature is not working
            if ( !dstFile.exists() )
            {
                wagonManager.getArtifact( classifiedArtifact, remoteRepos );
            }
            return dstFile.getAbsolutePath().replace( '\\', '/' );
        }
        catch ( TransferFailedException e )
        {
            getLog().debug( e );
            return null;
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLog().debug( e );
            return null;
        }
    }

    /**
     * Adds the Web module to the (.iml) project file.
     *
     * @param module Xpp3Dom element
     */
    private void addWebModule( Xpp3Dom module )
    {
        // TODO: this is bad - reproducing war plugin defaults, etc!
        //   --> this is where the OGNL out of a plugin would be helpful as we could run package first and
        //       grab stuff from the mojo

/*
Can't run this anyway as Xpp3Dom is in both classloaders...
                Xpp3Dom configuration = project.getGoalConfiguration( "maven-war-plugin", "war" );
                String warWebapp = configuration.getChild( "webappDirectory" ).getValue();
                if ( warWebapp == null )
                {
                    warWebapp = project.getBuild().getDirectory() + "/" + project.getArtifactId();
                }
                String warSrc = configuration.getChild( "warSrc" ).getValue();
                if ( warSrc == null )
                {
                    warSrc = "src/main/webapp";
                }
                String webXml = configuration.getChild( "webXml" ).getValue();
                if ( webXml == null )
                {
                    webXml = warSrc + "/WEB-INF/web.xml";
                }
*/
        String warWebapp = project.getBuild().getDirectory() + "/" + project.getArtifactId();
        String warSrc = "src/main/webapp";
        String webXml = warSrc + "/WEB-INF/web.xml";

        module.setAttribute( "type", "J2EE_WEB_MODULE" );

        Xpp3Dom component = findComponent( module, "WebModuleBuildComponent" );
        Xpp3Dom setting = findSetting( component, "EXPLODED_URL" );
        setting.setAttribute( "value", getModuleFileUrl( warWebapp ) );

        component = findComponent( module, "WebModuleProperties" );

        removeOldElements( component, "containerElement" );
        List artifacts = project.getTestArtifacts();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( artifact.getScope().equals( "compile" ) || artifact.getScope().equals( "runtime" ) )
            {
                Xpp3Dom containerElement = createElement( component, "containerElement" );

                boolean linkAsModule = false;
                if ( reactorProjects != null && linkModules )
                {
                    for ( Iterator j = reactorProjects.iterator(); j.hasNext() && !linkAsModule; )
                    {
                        MavenProject p = (MavenProject) j.next();
                        if ( p.getGroupId().equals( artifact.getGroupId() ) &&
                            p.getArtifactId().equals( artifact.getArtifactId() ) )
                        {
                            linkAsModule = true;
                        }
                    }
                }

                if ( linkAsModule )
                {
                    containerElement.setAttribute( "type", "module" );
                    containerElement.setAttribute( "name", artifact.getArtifactId() );
                    Xpp3Dom methodAttribute = createElement( containerElement, "attribute" );
                    methodAttribute.setAttribute( "name", "method" );
                    methodAttribute.setAttribute( "value", "1" );
                    Xpp3Dom uriAttribute = createElement( containerElement, "attribute" );
                    uriAttribute.setAttribute( "name", "URI" );
                    uriAttribute.setAttribute( "value", "/WEB-INF/classes" );
                }
                else if ( artifact.getFile() != null )
                {
                    containerElement.setAttribute( "type", "library" );
                    containerElement.setAttribute( "level", "module" );
                    containerElement.setAttribute( "name", artifact.getArtifactId() );
                    Xpp3Dom methodAttribute = createElement( containerElement, "attribute" );
                    methodAttribute.setAttribute( "name", "method" );
                    methodAttribute.setAttribute( "value", "1" ); // IntelliJ 5.0.2 is bugged and doesn't read it
                    Xpp3Dom uriAttribute = createElement( containerElement, "attribute" );
                    uriAttribute.setAttribute( "name", "URI" );
                    uriAttribute.setAttribute( "value", "/WEB-INF/lib/" + artifact.getFile().getName() );
                }
            }
        }

        Xpp3Dom element = findElement( component, "deploymentDescriptor" );
        if ( element.getAttribute( "version" ) == null )
        {
            // TODO: should derive from web.xml - does IDEA do this if omitted?
//                    element.setAttribute( "version", "2.3" );
        }
        if ( element.getAttribute( "name" ) == null )
        {
            element.setAttribute( "name", "web.xml" );
        }

        element.setAttribute( "url", getModuleFileUrl( webXml ) );

        element = findElement( component, "webroots" );
        removeOldElements( element, "root" );

        element = createElement( element, "root" );
        element.setAttribute( "relative", "/" );
        element.setAttribute( "url", getModuleFileUrl( warSrc ) );
    }

    /**
     * Sets the name of the JDK to use.
     *
     * @param content Xpp3Dom element.
     * @param jdkName Name of the JDK to use.
     */
    private void setJdkName( Xpp3Dom content, String jdkName )
    {
        Xpp3Dom component = findComponent( content, "ProjectRootManager" );
        component.setAttribute( "project-jdk-name", jdkName );

        String jdkLevel = this.jdkLevel;
        if ( jdkLevel == null )
        {
            jdkLevel = System.getProperty( "java.specification.version" );
        }

        if ( jdkLevel.startsWith( "1.4" ) )
        {
            component.setAttribute( "assert-keyword", "true" );
            component.setAttribute( "jdk-15", "false" );
        }
        else if ( jdkLevel.compareTo( "1.5" ) >= 0 )
        {
            component.setAttribute( "assert-keyword", "true" );
            component.setAttribute( "jdk-15", "true" );
        }
        else
        {
            component.setAttribute( "assert-keyword", "false" );
        }
    }

    /**
     * Sets the wilcard resource patterns.
     *
     * @param content                  Xpp3Dom element.
     * @param wildcardResourcePatterns The wilcard resource patterns.
     */
    private void setWildcardResourcePatterns( Xpp3Dom content, String wildcardResourcePatterns )
    {
        Xpp3Dom compilerConfigurationElement = findComponent( content, "CompilerConfiguration" );
        if ( !StringUtils.isEmpty( wildcardResourcePatterns ) )
        {
            removeOldElements( compilerConfigurationElement, "wildcardResourcePatterns" );
            Xpp3Dom wildcardResourcePatternsElement =
                createElement( compilerConfigurationElement, "wildcardResourcePatterns" );
            StringTokenizer wildcardResourcePatternsTokenizer = new StringTokenizer( wildcardResourcePatterns, ";" );
            while ( wildcardResourcePatternsTokenizer.hasMoreTokens() )
            {
                String wildcardResourcePattern = wildcardResourcePatternsTokenizer.nextToken();
                Xpp3Dom entryElement = createElement( wildcardResourcePatternsElement, "entry" );
                entryElement.setAttribute( "name", wildcardResourcePattern );
            }
        }
    }

    /**
     * Adds a sourceFolder element to IDEA (.iml) project file
     *
     * @param content   Xpp3Dom element
     * @param directory Directory to set as url.
     * @param isTest    True if directory isTestSource.
     */
    private void addSourceFolder( Xpp3Dom content, String directory, boolean isTest )
    {
        if ( !StringUtils.isEmpty( directory ) && new File( directory ).isDirectory() )
        {
            Xpp3Dom sourceFolder = createElement( content, "sourceFolder" );
            sourceFolder.setAttribute( "url", getModuleFileUrl( directory ) );
            sourceFolder.setAttribute( "isTestSource", Boolean.toString( isTest ) );
        }
    }

    private void addExcludeFolder( Xpp3Dom content, String directory )
    {
        if ( !StringUtils.isEmpty( directory ) && new File( directory ).isDirectory() )
        {
            Xpp3Dom excludeFolder = createElement( content, "excludeFolder" );
            excludeFolder.setAttribute( "url", getModuleFileUrl( directory ) );
        }
    }

    //  TODO: to FileUtils

    /**
     * Translate the absolutePath into its relative path.
     *
     * @param basedir      The basedir of the project.
     * @param absolutePath The absolute path that must be translated to relative path.
     * @return relative  Relative path of the parameter absolute path.
     */
    private static String toRelative( File basedir, String absolutePath )
    {
        String relative;

        if ( absolutePath.startsWith( basedir.getAbsolutePath() ) )
        {
            relative = absolutePath.substring( basedir.getAbsolutePath().length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, "\\", "/" );

        return relative;
    }

    /**
     * Translate the relative path of the file into module path
     *
     * @param file File to translate to ModuleFileUrl
     * @return moduleFileUrl Translated Module File URL
     */
    private String getModuleFileUrl( String file )
    {
        return "file://$MODULE_DIR$/" + toRelative( project.getBasedir(), file );
    }

    // TODO: some xpath may actually be more appropriate here

    /**
     * Remove elements from content (Xpp3Dom).
     *
     * @param content Xpp3Dom element
     * @param name    Name of the element to be removed
     */
    private void removeOldElements( Xpp3Dom content, String name )
    {
        Xpp3Dom[] children = content.getChildren();
        for ( int i = children.length - 1; i >= 0; i-- )
        {
            Xpp3Dom child = children[i];
            if ( child.getName().equals( name ) )
            {
                content.removeChild( i );
            }
        }
    }

    /**
     * Removes dependencies from Xpp3Dom component.
     *
     * @param component Xpp3Dom element
     */
    private void removeOldDependencies( Xpp3Dom component )
    {
        Xpp3Dom[] children = component.getChildren();
        for ( int i = children.length - 1; i >= 0; i-- )
        {
            Xpp3Dom child = children[i];
            if ( "orderEntry".equals( child.getName() ) && "module-library".equals( child.getAttribute( "type" ) ) )
            {
                component.removeChild( i );
            }
        }
    }

    /**
     * Finds element from the module element.
     *
     * @param module Xpp3Dom element
     * @param name   Name attribute to find
     * @return component  Returns the Xpp3Dom element found.
     */
    private Xpp3Dom findComponent( Xpp3Dom module, String name )
    {
        Xpp3Dom[] components = module.getChildren( "component" );
        for ( int i = 0; i < components.length; i++ )
        {
            if ( name.equals( components[i].getAttribute( "name" ) ) )
            {
                return components[i];
            }
        }

        Xpp3Dom component = createElement( module, "component" );
        component.setAttribute( "name", name );
        return component;
    }

    /**
     * Returns a an Xpp3Dom element (setting).
     *
     * @param component Xpp3Dom element
     * @param name      Setting attribute to find
     * @return setting Xpp3Dom element
     */
    private Xpp3Dom findSetting( Xpp3Dom component, String name )
    {
        Xpp3Dom[] settings = component.getChildren( "setting" );
        for ( int i = 0; i < settings.length; i++ )
        {
            if ( name.equals( settings[i].getAttribute( "name" ) ) )
            {
                return settings[i];
            }
        }

        Xpp3Dom setting = createElement( component, "setting" );
        setting.setAttribute( "name", name );
        return setting;
    }

    /**
     * Returns a an Xpp3Dom element with (child) tag name and (name) attribute name.
     *
     * @param component Xpp3Dom element
     * @param name      Setting attribute to find
     * @return option Xpp3Dom element
     */
    private Xpp3Dom findElementName( Xpp3Dom component, String child, String name )
    {
        Xpp3Dom[] elements = component.getChildren( child );
        for ( int i = 0; i < elements.length; i++ )
        {
            if ( name.equals( elements[i].getAttribute( "name" ) ) )
            {
                return elements[i];
            }
        }

        Xpp3Dom element = createElement( component, child );
        element.setAttribute( "name", name );
        return element;
    }

    /**
     * Creates an Xpp3Dom element.
     *
     * @param module Xpp3Dom element
     * @param name   Name of the element
     * @return component Xpp3Dom element
     */
    private static Xpp3Dom createElement( Xpp3Dom module, String name )
    {
        Xpp3Dom component = new Xpp3Dom( name );
        module.addChild( component );
        return component;
    }

    /**
     * Finds an element from Xpp3Dom component.
     *
     * @param component Xpp3Dom component
     * @param name      Name of the element to find.
     * @return the element
     */
    private Xpp3Dom findElement( Xpp3Dom component, String name )
    {
        Xpp3Dom element = component.getChild( name );

        if ( element == null )
        {
            element = createElement( component, name );
        }
        return element;
    }

    /**
     * Sets the SCM type of the project
     */
    private void setProjectScmType( Xpp3Dom content )
    {
        String scmType;

        scmType = getScmType();

        if ( scmType != null )
        {
            Xpp3Dom component = findComponent( content, "VcsManagerConfiguration" );

            Xpp3Dom element = findElementName( component, "option", "ACTIVE_VCS_NAME" );

            element.setAttribute( "value", scmType );
        }
    }

    /**
     * used to retrieve the SCM Type
     *
     * @return the Scm Type string used to connect to the SCM
     */
    protected String getScmType()
    {
        String scmType;

        if ( project.getScm() == null )
        {
            return null;
        }
        scmType = getScmType( project.getScm().getConnection() );

        if ( scmType != null )
        {
            return scmType;
        }
        scmType = getScmType( project.getScm().getDeveloperConnection() );

        return scmType;
    }

    protected String getScmType( String connection )
    {
        String scmType;

        if ( connection != null )
        {
            if ( connection.length() > 0 )
            {
                int startIndex = connection.indexOf( ":" );

                int endIndex = connection.indexOf( ":", startIndex + 1 );

                if ( startIndex < endIndex )
                {
                    scmType = connection.substring( startIndex + 1, endIndex );

                    return scmType;
                }
            }
        }
        return null;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    private Map createManagedVersionMap( String projectId, DependencyManagement dependencyManagement )
        throws ProjectBuildingException
    {
        Map map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap();
            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                                  versionRange, d.getType(),
                                                                                  d.getClassifier(), d.getScope(),
                                                                                  d.isOptional() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new ProjectBuildingException( projectId, "Unable to parse version '" + d.getVersion() +
                        "' for dependency '" + d.getManagementKey() + "': " + e.getMessage(), e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }
}
