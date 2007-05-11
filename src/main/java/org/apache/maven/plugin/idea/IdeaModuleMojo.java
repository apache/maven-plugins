package org.apache.maven.plugin.idea;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates the module files (*.iml) for IntelliJ IDEA.
 *
 * @author Edwin Punzalan
 * @goal module
 * @execute phase="generate-sources"
 */
public class IdeaModuleMojo
    extends AbstractIdeaMojo
{
    /**
     * The reactor projects in a multi-module build.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @component
     */
    private WagonManager wagonManager;

    /**
     * Whether to link the reactor projects as dependency modules or as libraries.
     *
     * @parameter expression="${linkModules}" default-value="true"
     */
    private boolean linkModules;

    /**
     * Specify the location of the deployment descriptor file, if one is provided.
     *
     * @parameter expression="${deploymentDescriptorFile}"
     */
    private String deploymentDescriptorFile;

    /**
     * Whether to use full artifact names when referencing libraries.
     *
     * @parameter expression="${useFullNames}" default-value="false"
     */
    private boolean useFullNames;

    /**
     * Enables/disables the downloading of source attachments.
     *
     * @parameter expression="${downloadSources}" default-value="false"
     */
    private boolean downloadSources;

    /**
     * Enables/disables the downloading of javadoc attachments.
     *
     * @parameter expression="${downloadJavadocs}" default-value="false"
     */
    private boolean downloadJavadocs;

    /**
     * Sets the classifier string attached to an artifact source archive name.
     *
     * @parameter expression="${sourceClassifier}" default-value="sources"
     */
    private String sourceClassifier;

    /**
     * Sets the classifier string attached to an artifact javadoc archive name.
     *
     * @parameter expression="${javadocClassifier}" default-value="javadoc"
     */
    private String javadocClassifier;

    /**
     * An optional set of Library objects that allow you to specify a comma separated list of source dirs, class dirs,
     * or to indicate that the library should be excluded from the module. For example:
     * <p/>
     * <pre>
     * &lt;libraries&gt;
     *  &lt;library&gt;
     *      &lt;name&gt;webwork&lt;/name&gt;
     *      &lt;sources&gt;file://$webwork$/src/java&lt;/sources&gt;
     *      &lt;!--
     *      &lt;classes&gt;...&lt;/classes&gt;
     *      &lt;exclude&gt;true&lt;/exclude&gt;
     *      --&gt;
     *  &lt;/library&gt;
     * &lt;/libraries&gt;
     * </pre>
     *
     * @parameter
     */
    private Library[] libraries;

    /**
     * A comma-separated list of directories that should be excluded. These directories are in addition to those
     * already excluded, such as target.
     *
     * @parameter
     */
    private String exclude;

    /**
     * Causes the module libraries to use a short name for all dependencies. This is very convenient but has been
     * reported to cause problems with IDEA.
     *
     * @parameter default-value="false"
     */
    private boolean dependenciesAsLibraries;

    /**
     * A temporary cache of artifacts that's already been downloaded or
     * attempted to be downloaded. This is to refrain from trying to download a
     * dependency that we have already tried to download.
     *
     * @todo this is nasty! the only reason this is static is to use the same cache between reactor calls
     */
    private static Map attemptedDownloads = new HashMap();

    /**
     * Tell IntelliJ IDEA that this module is an IntelliJ IDEA Plugin.
     *
     * @parameter default-value="false"
     */
    private boolean ideaPlugin;

    /**
     * Specify the version of IDEA to target.  This is needed to identify the default formatting of
     * project-jdk-name used by IDEA.  Currently supports 4.x and 5.x.
     * <p/>
     * This will only be used when parameter jdkName is not set.
     *
     * @parameter expression="${ideaVersion}" default-value="5.x"
     */
    private String ideaVersion;

    private Set macros;

    public void initParam( MavenProject project, ArtifactFactory artifactFactory, ArtifactRepository localRepo,
                           ArtifactResolver artifactResolver, ArtifactMetadataSource artifactMetadataSource, Log log,
                           boolean overwrite, MavenProject executedProject, List reactorProjects,
                           WagonManager wagonManager, boolean linkModules, boolean useFullNames,
                           boolean downloadSources, String sourceClassifier, boolean downloadJavadocs,
                           String javadocClassifier, Library[] libraries, Set macros, String exclude,
                           boolean useShortDependencyNames, String deploymentDescriptorFile, boolean ideaPlugin,
                           String ideaVersion )
    {
        super.initParam( project, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, log,
                         overwrite );

        this.reactorProjects = reactorProjects;

        this.wagonManager = wagonManager;

        this.linkModules = linkModules;

        this.useFullNames = useFullNames;

        this.downloadSources = downloadSources;

        this.sourceClassifier = sourceClassifier;

        this.downloadJavadocs = downloadJavadocs;

        this.javadocClassifier = javadocClassifier;

        this.libraries = libraries;

        this.macros = macros;

        this.exclude = exclude;

        this.dependenciesAsLibraries = useShortDependencyNames;

        this.deploymentDescriptorFile = deploymentDescriptorFile;

        this.ideaPlugin = ideaPlugin;

        this.ideaVersion = ideaVersion;
    }

    /**
     * Create IDEA (.iml) project files.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doDependencyResolution( executedProject, localRepo );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        rewriteModule();
    }

    public void rewriteModule()
        throws MojoExecutionException
    {
        File moduleFile = new File( executedProject.getBasedir(), executedProject.getArtifactId() + ".iml" );
        try
        {
            Document document = readXmlDocument( moduleFile, "module.xml" );

            Element module = document.getRootElement();

            // TODO: how can we let the WAR/EJBs plugin hook in and provide this?
            // TODO: merge in ejb-module, etc.
            if ( "war".equals( executedProject.getPackaging() ) )
            {
                addWebModule( module );
            }
            else if ( "ejb".equals( executedProject.getPackaging() ) )
            {
                addEjbModule( module );
            }
            else if ( "ear".equals( executedProject.getPackaging() ) )
            {
                addEarModule( module );
            }
            else if ( ideaPlugin )
            {
                addPluginModule( module );
            }

            Element component = findComponent( module, "NewModuleRootManager" );
            Element output = findElement( component, "output" );
            output.addAttribute( "url", getModuleFileUrl( executedProject.getBuild().getOutputDirectory() ) );

            Element outputTest = findElement( component, "output-test" );
            outputTest.addAttribute( "url", getModuleFileUrl( executedProject.getBuild().getTestOutputDirectory() ) );

            Element content = findElement( component, "content" );

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

            for ( Iterator i = executedProject.getBuild().getResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                String directory = resource.getDirectory();
                if ( resource.getTargetPath() == null && !resource.isFiltering() )
                {
                    addSourceFolder( content, directory, false );
                }
                else
                {
                    getLog().info(
                        "Not adding resource directory as it has an incompatible target path or filtering: "
                            + directory );
                }
            }

            for ( Iterator i = executedProject.getBuild().getTestResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                String directory = resource.getDirectory();
                if ( resource.getTargetPath() == null && !resource.isFiltering() )
                {
                    addSourceFolder( content, directory, true );
                }
                else
                {
                    getLog().info(
                        "Not adding test resource directory as it has an incompatible target path or filtering: "
                            + directory );
                }
            }

            removeOldElements( content, "excludeFolder" );

            //For excludeFolder
            File target = new File( executedProject.getBuild().getDirectory() );
            File classes = new File( executedProject.getBuild().getOutputDirectory() );
            File testClasses = new File( executedProject.getBuild().getTestOutputDirectory() );

            List sourceFolders = content.elements( "sourceFolder" );

            List filteredExcludes = new ArrayList();
            filteredExcludes.addAll( getExcludedDirectories( target, filteredExcludes, sourceFolders ) );
            filteredExcludes.addAll( getExcludedDirectories( classes, filteredExcludes, sourceFolders ) );
            filteredExcludes.addAll( getExcludedDirectories( testClasses, filteredExcludes, sourceFolders ) );

            if ( exclude != null )
            {
                String[] dirs = exclude.split( "[,\\s]+" );
                for ( int i = 0; i < dirs.length; i++ )
                {
                    File excludedDir = new File( executedProject.getBasedir(), dirs[i] );
                    filteredExcludes.addAll( getExcludedDirectories( excludedDir, filteredExcludes, sourceFolders ) );
                }
            }

            // even though we just ran all the directories in the filteredExcludes List through the intelligent
            // getExcludedDirectories method, we never actually were guaranteed the order that they were added was
            // in the order required to make the most optimized exclude list. In addition, the smart logic from
            // that method is entirely skipped if the directory doesn't currently exist. A simple string matching
            // will do pretty much the same thing and make the list more concise.
            ArrayList actuallyExcluded = new ArrayList();
            Collections.sort( filteredExcludes );
            for ( Iterator i = filteredExcludes.iterator(); i.hasNext(); )
            {
                String dirToExclude = i.next().toString();
                String dirToExcludeTemp = dirToExclude.replace( '\\', '/' );
                boolean addExclude = true;
                for ( Iterator iterator = actuallyExcluded.iterator(); iterator.hasNext(); )
                {
                    String dir = iterator.next().toString();
                    String dirTemp = dir.replace( '\\', '/' );
                    if ( dirToExcludeTemp.startsWith( dirTemp + "/" ) )
                    {
                        addExclude = false;
                        break;
                    }
                    else if ( dir.startsWith( dirToExcludeTemp + "/" ) )
                    {
                        actuallyExcluded.remove( dir );
                    }
                }

                if ( addExclude )
                {
                    actuallyExcluded.add( dirToExclude );
                    addExcludeFolder( content, dirToExclude );
                }
            }

            //Remove default exclusion for output dirs if there are sources in it
            String outputModuleUrl = getModuleFileUrl( executedProject.getBuild().getOutputDirectory() );
            String testOutputModuleUrl = getModuleFileUrl( executedProject.getBuild().getTestOutputDirectory() );
            for ( Iterator i = content.elements( "sourceFolder" ).iterator(); i.hasNext(); )
            {
                Element sourceFolder = (Element) i.next();
                String sourceUrl = sourceFolder.attributeValue( "url" ).replace( '\\', '/' );
                if ( sourceUrl.startsWith( outputModuleUrl + "/" ) || sourceUrl.startsWith( testOutputModuleUrl ) )
                {
                    component.remove( component.element( "exclude-output" ) );
                    break;
                }
            }

            rewriteDependencies( component );

            writeXmlDocument( moduleFile, document );
        }
        catch ( DocumentException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file " + moduleFile.getAbsolutePath(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file " + moduleFile.getAbsolutePath(), e );
        }
    }

    private void rewriteDependencies( Element component )
    {
        Map modulesByName = new HashMap();
        Map modulesByUrl = new HashMap();
        Set unusedModules = new HashSet();
        for ( Iterator children = component.elementIterator( "orderEntry" ); children.hasNext(); )
        {
            Element orderEntry = (Element) children.next();

            String type = orderEntry.attributeValue( "type" );
            if ( "module".equals( type ) )
            {
                modulesByName.put( orderEntry.attributeValue( "module-name" ), orderEntry );
            }
            else if ( "module-library".equals( type ) )
            {
                // keep track for later so we know what is left
                unusedModules.add( orderEntry );

                Element lib = orderEntry.element( "library" );
                String name = lib.attributeValue( "name" );
                if ( name != null )
                {
                    modulesByName.put( name, orderEntry );
                }
                else
                {
                    Element classesChild = lib.element( "CLASSES" );
                    if ( classesChild != null )
                    {
                        Element rootChild = classesChild.element( "root" );
                        if ( rootChild != null )
                        {
                            String url = rootChild.attributeValue( "url" );
                            if ( url != null )
                            {
                                // Need to ignore case because of Windows drive letters
                                modulesByUrl.put( url.toLowerCase(), orderEntry );
                            }
                        }
                    }
                }
            }
        }

        List testClasspathElements = executedProject.getTestArtifacts();
        for ( Iterator i = testClasspathElements.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            Library library = findLibrary( a );
            if ( library != null && library.isExclude() )
            {
                continue;
            }

            String moduleName;
            if ( useFullNames )
            {
                moduleName = a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getType() + ':' + a.getVersion();
            }
            else
            {
                moduleName = a.getArtifactId();
            }

            Element dep = (Element) modulesByName.get( moduleName );

            if ( dep == null )
            {
                // Need to ignore case because of Windows drive letters
                dep = (Element) modulesByUrl.get( getLibraryUrl( a ).toLowerCase() );
            }

            if ( dep != null )
            {
                unusedModules.remove( dep );
            }
            else
            {
                dep = createElement( component, "orderEntry" );
            }

            boolean isIdeaModule = false;
            if ( linkModules )
            {
                isIdeaModule = isReactorProject( a.getGroupId(), a.getArtifactId() );

                if ( isIdeaModule )
                {
                    dep.addAttribute( "type", "module" );
                    dep.addAttribute( "module-name", moduleName );
                }
            }

            if ( a.getFile() != null && !isIdeaModule )
            {
                dep.addAttribute( "type", "module-library" );

                Element lib = dep.element( "library" );

                if ( lib == null )
                {
                    lib = createElement( dep, "library" );
                }

                if ( dependenciesAsLibraries )
                {
                    lib.addAttribute( "name", moduleName );
                }

                // replace classes
                removeOldElements( lib, "CLASSES" );
                Element classes = createElement( lib, "CLASSES" );
                if ( library != null && library.getSplitClasses().length > 0 )
                {
                    lib.addAttribute( "name", moduleName );
                    String[] libraryClasses = library.getSplitClasses();
                    for ( int k = 0; k < libraryClasses.length; k++ )
                    {
                        String classpath = libraryClasses[k];
                        extractMacro( classpath );
                        Element classEl = createElement( classes, "root" );
                        classEl.addAttribute( "url", classpath );
                    }
                }
                else
                {
                    createElement( classes, "root" ).addAttribute( "url", getLibraryUrl( a ) );
                }

                if ( library != null && library.getSplitSources().length > 0 )
                {
                    removeOldElements( lib, "SOURCES" );
                    Element sourcesElement = createElement( lib, "SOURCES" );
                    String[] sources = library.getSplitSources();
                    for ( int k = 0; k < sources.length; k++ )
                    {
                        String source = sources[k];
                        extractMacro( source );
                        Element sourceEl = createElement( sourcesElement, "root" );
                        sourceEl.addAttribute( "url", source );
                    }
                }
                else if ( downloadSources )
                {
                    resolveClassifier( createOrGetElement( lib, "SOURCES" ), a, sourceClassifier );
                }

                if ( downloadJavadocs )
                {
                    resolveClassifier( createOrGetElement( lib, "JAVADOC" ), a, javadocClassifier );
                }
            }
        }

        for ( Iterator i = unusedModules.iterator(); i.hasNext(); )
        {
            Element orderEntry = (Element) i.next();

            component.remove( orderEntry );
        }
    }

    private Element createOrGetElement( Element lib, String name )
    {
        Element el = lib.element( "name" );

        if ( el == null )
        {
            el = createElement( lib, name );
        }
        return el;
    }

    private void addEarModule( Element module )
    {
        module.addAttribute( "type", "J2EE_APPLICATION_MODULE" );
        Element component = findComponent( module, "ApplicationModuleProperties" );
        addDeploymentDescriptor( component, "application.xml", "1.3",
                                 executedProject.getBuild().getDirectory() + "/application.xml" );
    }

    private void addEjbModule( Element module )
    {
        String ejbVersion = getPluginSetting( "maven-ejb-plugin", "ejbVersion", "2.x" );

        module.addAttribute( "type", "J2EE_EJB_MODULE" );

        String explodedDir = executedProject.getBuild().getDirectory() + "/" + executedProject.getArtifactId();

        Element component = findComponent( module, "EjbModuleBuildComponent" );

        Element setting = findSetting( component, "EXPLODED_URL" );
        setting.addAttribute( "value", getModuleFileUrl( explodedDir ) );

        component = findComponent( module, "EjbModuleProperties" );
        Element deployDescElement =
            addDeploymentDescriptor( component, "ejb-jar.xml", ejbVersion, "src/main/resources/META-INF/ejb-jar.xml" );
        deployDescElement.addAttribute( "optional", ejbVersion.startsWith( "3" ) + "" );

        removeOldElements( component, "containerElement" );
        List artifacts = executedProject.getTestArtifacts();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            Element containerElement = createElement( component, "containerElement" );

            if ( linkModules && isReactorProject( artifact.getGroupId(), artifact.getArtifactId() ) )
            {
                containerElement.addAttribute( "type", "module" );
                containerElement.addAttribute( "name", artifact.getArtifactId() );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "6" );
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/lib/" + artifact.getArtifactId() + ".jar" );
            }
            else if ( artifact.getFile() != null )
            {
                containerElement.addAttribute( "type", "library" );
                containerElement.addAttribute( "level", "module" );

                //no longer needed in IntelliJ 6
                if ( StringUtils.isEmpty( ideaVersion ) || !ideaVersion.startsWith( "6" ) )
                {
                    containerElement.addAttribute( "name", artifact.getArtifactId() );
                }

                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "2" );
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/lib/" + artifact.getFile().getName() );
                Element urlElement = createElement( containerElement, "url" );
                urlElement.setText( getLibraryUrl( artifact ) );
            }
        }
    }

    private void extractMacro( String path )
    {
        if ( macros != null )
        {
            Pattern p = Pattern.compile( ".*\\$([^\\$]+)\\$.*" );
            Matcher matcher = p.matcher( path );
            while ( matcher.find() )
            {
                String macro = matcher.group( 1 );
                macros.add( macro );
            }
        }
    }

    private Library findLibrary( Artifact a )
    {
        if ( libraries != null )
        {
            for ( int j = 0; j < libraries.length; j++ )
            {
                Library library = libraries[j];
                if ( a.getArtifactId().equals( library.getName() ) )
                {
                    return library;
                }
            }
        }

        return null;
    }

    private List getExcludedDirectories( File target, List excludeList, List sourceFolders )
    {
        List foundFolders = new ArrayList();

        int totalDirs = 0, excludedDirs = 0;

        if ( target.exists() && !excludeList.contains( target.getAbsolutePath() ) )
        {
            File[] files = target.listFiles();

            for ( int i = 0; i < files.length; i++ )
            {
                File file = files[i];
                if ( file.isDirectory() && !excludeList.contains( file.getAbsolutePath() ) )
                {
                    totalDirs++;

                    String absolutePath = file.getAbsolutePath();
                    String url = getModuleFileUrl( absolutePath );

                    boolean addToExclude = true;
                    for ( Iterator sources = sourceFolders.iterator(); sources.hasNext(); )
                    {
                        String source = ( (Element) sources.next() ).attributeValue( "url" );
                        if ( source.equals( url ) )
                        {
                            addToExclude = false;
                            break;
                        }
                        else if ( source.indexOf( url ) == 0 )
                        {
                            foundFolders.addAll(
                                getExcludedDirectories( new File( absolutePath ), excludeList, sourceFolders ) );
                            addToExclude = false;
                            break;
                        }
                    }
                    if ( addToExclude )
                    {
                        excludedDirs++;
                        foundFolders.add( absolutePath );
                    }
                }
            }

            //if all directories are excluded, then just exclude the parent directory
            if ( totalDirs > 0 && totalDirs == excludedDirs )
            {
                foundFolders.clear();

                foundFolders.add( target.getAbsolutePath() );
            }
        }
        else if ( !target.exists() )
        {
            //might as well exclude a non-existent dir so that it won't show when it suddenly appears
            foundFolders.add( target.getAbsolutePath() );
        }

        return foundFolders;
    }

    /**
     * Adds the Web module to the (.iml) project file.
     *
     * @param module Xpp3Dom element
     */
    private void addWebModule( Element module )
    {
        // TODO: this is bad - reproducing war plugin defaults, etc!
        //   --> this is where the OGNL out of a plugin would be helpful as we could run package first and
        //       grab stuff from the mojo

        String warWebapp = executedProject.getBuild().getDirectory() + "/" + executedProject.getArtifactId();
        String warSrc = getPluginSetting( "maven-war-plugin", "warSourceDirectory", "src/main/webapp" );
        String webXml = warSrc + "/WEB-INF/web.xml";

        module.addAttribute( "type", "J2EE_WEB_MODULE" );

        Element component = findComponent( module, "WebModuleBuildComponent" );
        Element setting = findSetting( component, "EXPLODED_URL" );
        setting.addAttribute( "value", getModuleFileUrl( warWebapp ) );

        component = findComponent( module, "WebModuleProperties" );

        removeOldElements( component, "containerElement" );
        List artifacts = executedProject.getTestArtifacts();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            Element containerElement = createElement( component, "containerElement" );

            if ( linkModules && isReactorProject( artifact.getGroupId(), artifact.getArtifactId() ) )
            {
                containerElement.addAttribute( "type", "module" );
                containerElement.addAttribute( "name", artifact.getArtifactId() );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "5" );
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/WEB-INF/lib/" + artifact.getArtifactId() + "-"
                    + artifact.getVersion() + ".jar" );
            }
            else if ( artifact.getFile() != null )
            {
                containerElement.addAttribute( "type", "library" );
                containerElement.addAttribute( "level", "module" );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                if ( Artifact.SCOPE_PROVIDED.equalsIgnoreCase( artifact.getScope() )
                    || Artifact.SCOPE_SYSTEM.equalsIgnoreCase( artifact.getScope() )
                    || Artifact.SCOPE_TEST.equalsIgnoreCase( artifact.getScope() ) )
                {
                    // If scope is provided, system or test - do not package.
                    methodAttribute.addAttribute( "value", "0" );
                }
                else
                {
                    methodAttribute.addAttribute( "value", "1" ); // IntelliJ 5.0.2 is bugged and doesn't read it
                }
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/WEB-INF/lib/" + artifact.getFile().getName() );
                Element url = createElement( containerElement, "url" );
                url.setText( getLibraryUrl( artifact ) );
            }
        }

        addDeploymentDescriptor( component, "web.xml", "2.3", webXml );

        Element element = findElement( component, "webroots" );
        removeOldElements( element, "root" );

        element = createElement( element, "root" );
        element.addAttribute( "relative", "/" );
        element.addAttribute( "url", getModuleFileUrl( warSrc ) );
    }

    private void addPluginModule( Element module )
    {
        module.addAttribute( "type", "PLUGIN_MODULE" );

        // this is where the META-INF/plugin.xml file is located
        Element pluginDevElement = createElement( module, "component" );
        pluginDevElement.addAttribute( "name", "DevKit.ModuleBuildProperties" );
        pluginDevElement.addAttribute( "url", getModuleFileUrl( "src/main/resources/META-INF/plugin.xml" ) );
    }

    /**
     * Translate the relative path of the file into module path
     *
     * @param basedir File to use as basedir
     * @param path    Absolute path string to translate to ModuleFileUrl
     * @return moduleFileUrl Translated Module File URL
     */
    private String getModuleFileUrl( File basedir, String path )
    {
        return "file://$MODULE_DIR$/" + toRelative( basedir, path );
    }

    private String getModuleFileUrl( String file )
    {
        return getModuleFileUrl( executedProject.getBasedir(), file );
    }

    /**
     * Adds a sourceFolder element to IDEA (.iml) project file
     *
     * @param content   Xpp3Dom element
     * @param directory Directory to set as url.
     * @param isTest    True if directory isTestSource.
     */
    private void addSourceFolder( Element content, String directory, boolean isTest )
    {
        if ( !StringUtils.isEmpty( directory ) && new File( directory ).isDirectory() )
        {
            Element sourceFolder = createElement( content, "sourceFolder" );
            sourceFolder.addAttribute( "url", getModuleFileUrl( directory ) );
            sourceFolder.addAttribute( "isTestSource", Boolean.toString( isTest ) );
        }
    }

    private void addExcludeFolder( Element content, String directory )
    {
        Element excludeFolder = createElement( content, "excludeFolder" );
        excludeFolder.addAttribute( "url", getModuleFileUrl( directory ) );
    }

    private boolean isReactorProject( String groupId, String artifactId )
    {
        if ( reactorProjects != null )
        {
            for ( Iterator j = reactorProjects.iterator(); j.hasNext(); )
            {
                MavenProject p = (MavenProject) j.next();
                if ( p.getGroupId().equals( groupId ) && p.getArtifactId().equals( artifactId ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void resolveClassifier( Element element, Artifact a, String classifier )
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
            removeOldElements( element, "root" );
            createElement( element, "root" ).addAttribute( "url", jarPath );
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

        List remoteRepos = executedProject.getRemoteArtifactRepositories();
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
     * Returns a an Xpp3Dom element (setting).
     *
     * @param component Xpp3Dom element
     * @param name      Setting attribute to find
     * @return setting Xpp3Dom element
     */
    private Element findSetting( Element component, String name )
    {
        return findElement( component, "setting", name );
    }

    private String getLibraryUrl( Artifact artifact )
    {
        return "jar://" + artifact.getFile().getAbsolutePath().replace( '\\', '/' ) + "!/";
    }

    private Element addDeploymentDescriptor( Element component, String name, String version, String file )
    {
        Element deploymentDescriptor = findElement( component, "deploymentDescriptor" );

        if ( deploymentDescriptor.attributeValue( "version" ) == null )
        {
            deploymentDescriptor.addAttribute( "version", version );
        }

        if ( deploymentDescriptor.attributeValue( "name" ) == null )
        {
            deploymentDescriptor.addAttribute( "name", name );
        }

        deploymentDescriptor.addAttribute( "optional", "false" );

        if ( deploymentDescriptorFile == null )
        {
            deploymentDescriptorFile = file;
        }

        deploymentDescriptor.addAttribute( "url", getModuleFileUrl( deploymentDescriptorFile ) );

        return deploymentDescriptor;
    }
}
