package org.apache.maven.plugin.idea;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Edwin Punzalan
 * @goal module
 * @execute phase="generate-sources"
 */
public class IdeaModuleMojo
    extends AbstractIdeaMojo
{
    /**
     * The Maven Project.
     *
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * The reactor projects in a multi-module build.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.manager.WagonManager}"
     * @required
     * @readonly
     */
    private WagonManager wagonManager;

    /**
     * Whether to link the reactor projects as dependency modules or as libraries.
     *
     * @parameter expression="${linkModules}" default-value="true"
     */
    private boolean linkModules;

    /**
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
     * Enables/disables the downloading of source attachments. Defaults to false.
     *
     * @parameter expression="${downloadSources}" default-value="false"
     */
    private boolean downloadSources;

    /**
     * Enables/disables the downloading of javadoc attachements. Defaults to false.
     *
     * @parameter expression="${downloadJavadocs}" default-value="false"
     */
    private boolean downloadJavadocs;

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
     * @parameter default-value="true"
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

    private Set macros;

    public void initParam( MavenProject project, ArtifactFactory artifactFactory, ArtifactRepository localRepo,
                           ArtifactResolver artifactResolver, ArtifactMetadataSource artifactMetadataSource, Log log,
                           boolean overwrite, MavenProject executedProject, List reactorProjects,
                           WagonManager wagonManager, boolean linkModules, boolean useFullNames,
                           boolean downloadSources, String sourceClassifier, boolean downloadJavadocs,
                           String javadocClassifier, Library[] libraries, Set macros, String exclude,
                           boolean useShortDependencyNames )
    {
        super.initParam( project, artifactFactory, localRepo, artifactResolver, artifactMetadataSource, log,
                         overwrite );

        this.executedProject = executedProject;

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
            doDependencyResolution( project, localRepo );
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
        File moduleFile = new File( project.getBasedir(), project.getArtifactId() + ".iml" );
        try
        {
            Document document = readXmlDocument( moduleFile, "module.xml" );

            Element module = document.getRootElement();

            // TODO: how can we let the WAR/EJBs plugin hook in and provide this?
            // TODO: merge in ejb-module, etc.
            if ( "war".equals( project.getPackaging() ) )
            {
                addWebModule( module );
            }
            else if ( "ejb".equals( project.getPackaging() ) )
            {
                addEjbModule( module );
            }
            else if ( "ear".equals( project.getPackaging() ) )
            {
                addEarModule( module );
            }

            Element component = findComponent( module, "NewModuleRootManager" );
            Element output = findElement( component, "output" );
            output.addAttribute( "url", getModuleFileUrl( project.getBuild().getOutputDirectory() ) );

            Element outputTest = findElement( component, "output-test" );
            outputTest.addAttribute( "url", getModuleFileUrl( project.getBuild().getTestOutputDirectory() ) );

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
            File classes = new File( project.getBuild().getOutputDirectory() );
            File testClasses = new File( project.getBuild().getTestOutputDirectory() );

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
                    File excludedDir = new File( project.getBasedir(), dirs[i] );
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
                boolean addExclude = true;
                for ( Iterator iterator = actuallyExcluded.iterator(); iterator.hasNext(); )
                {
                    String dir = (String) iterator.next();
                    if ( dirToExclude.startsWith( dir ) )
                    {
                        addExclude = false;
                        break;
                    }
                }

                if ( addExclude )
                {
                    actuallyExcluded.add( dirToExclude );
                    addExcludeFolder( content, dirToExclude );
                }
            }

            removeOldDependencies( component );

            List testClasspathElements = project.getTestArtifacts();
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

                Element dep = null;

                for ( Iterator children = component.elementIterator( "orderEntry" ); children.hasNext(); )
                {
                    Element orderEntry = (Element) children.next();

                    if ( orderEntry.attributeValue( "type" ).equals( "module" ) )
                    {
                        if ( orderEntry.attributeValue( "module-name" ).equals( moduleName ) )
                        {
                            dep = orderEntry;
                            break;
                        }
                    }
                    else if ( orderEntry.attributeValue( "type" ).equals( "module-library" ) )
                    {
                        Element lib = orderEntry.element( "library" );
                        String name = lib.attributeValue( "name" );
                        if ( name != null )
                        {
                            if ( name.equals( moduleName ) )
                            {
                                dep = orderEntry;
                                break;
                            }
                        }
                        else
                        {
                            Element classesChild = lib.element( "CLASSES" );
                            if ( classesChild != null )
                            {
                                Element rootChild = classesChild.element( "root" );
                                if ( rootChild != null )
                                {
                                    String url = getLibraryUrl( a );
                                    if ( url.equals( rootChild.getText() ) )
                                    {
                                        dep = orderEntry;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if ( dep == null )
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
                    removeOldElements( dep, "library" );
                    dep = createElement( dep, "library" );

                    if ( dependenciesAsLibraries )
                    {
                        dep.addAttribute( "name", moduleName );
                    }

                    Element el = createElement( dep, "CLASSES" );
                    if ( library != null && library.getSplitClasses().length > 0 )
                    {
                        dep.addAttribute( "name", moduleName );
                        String[] libraryClasses = library.getSplitClasses();
                        for ( int k = 0; k < libraryClasses.length; k++ )
                        {
                            String classpath = libraryClasses[k];
                            extractMacro( classpath );
                            Element classEl = createElement( el, "root" );
                            classEl.addAttribute( "url", classpath );
                        }
                    }
                    else
                    {
                        createElement( el, "root" ).addAttribute( "url", getLibraryUrl( a ) );
                    }

                    boolean usedSources = false;
                    if ( library != null && library.getSplitSources().length > 0 )
                    {
                        Element sourcesElement = createElement( dep, "SOURCES" );
                        usedSources = true;
                        String[] sources = library.getSplitSources();
                        for ( int k = 0; k < sources.length; k++ )
                        {
                            String source = sources[k];
                            extractMacro( source );
                            Element sourceEl = createElement( sourcesElement, "root" );
                            sourceEl.addAttribute( "url", source );
                        }
                    }

                    if ( !usedSources && downloadSources )
                    {
                        resolveClassifier( createElement( dep, "SOURCES" ), a, sourceClassifier );
                    }

                    if ( downloadJavadocs )
                    {
                        resolveClassifier( createElement( dep, "JAVADOC" ), a, javadocClassifier );
                    }
                }
            }

            for ( Iterator resourceDirs = resourceDirectory.iterator(); resourceDirs.hasNext(); )
            {
                String resourceDir = (String) resourceDirs.next();

                getLog().info( "Adding resource directory: " + resourceDir );

                addResources( component, resourceDir );
            }

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

    private void addEarModule( Element module )
    {
        module.addAttribute( "type", "J2EE_APPLICATION_MODULE" );
        Element component = findComponent( module, "ApplicationModuleProperties" );
        addDeploymentDescriptor( component, "application.xml", "1.3",
                                 project.getBuild().getDirectory() + "/application.xml" );
    }

    private void addEjbModule( Element module )
    {
        module.addAttribute( "type", "J2EE_EJB_MODULE" );

        String explodedDir = project.getBuild().getDirectory() + "/" + project.getArtifactId();

        Element component = findComponent( module, "EjbModuleBuildComponent" );

        Element setting = findSetting( component, "EXPLODED_URL" );
        setting.addAttribute( "value", getModuleFileUrl( explodedDir ) );

        component = findComponent( module, "EjbModuleProperties" );
        addDeploymentDescriptor( component, "ejb-jar.xml", "2.x", "src/main/resources/META-INF/ejb-jar.xml" );

        removeOldElements( component, "containerElement" );
        List artifacts = project.getTestArtifacts();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            Element containerElement = createElement( component, "containerElement" );

            boolean linkAsModule = false;
            if ( linkModules )
            {
                linkAsModule = isReactorProject( artifact.getGroupId(), artifact.getArtifactId() );
            }

            if ( linkAsModule )
            {
                containerElement.addAttribute( "type", "module" );
                containerElement.addAttribute( "name", artifact.getArtifactId() );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "6" );
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/WEB-INF/classes" );
            }
            else if ( artifact.getFile() != null )
            {
                containerElement.addAttribute( "type", "library" );
                containerElement.addAttribute( "level", "module" );
                containerElement.addAttribute( "name", artifact.getArtifactId() );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "2" );
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/WEB-INF/lib/" + artifact.getFile().getName() );
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

        module.addAttribute( "type", "J2EE_WEB_MODULE" );

        Element component = findComponent( module, "WebModuleBuildComponent" );
        Element setting = findSetting( component, "EXPLODED_URL" );
        setting.addAttribute( "value", getModuleFileUrl( warWebapp ) );

        component = findComponent( module, "WebModuleProperties" );

        removeOldElements( component, "containerElement" );
        List artifacts = project.getTestArtifacts();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            Element containerElement = createElement( component, "containerElement" );

            boolean linkAsModule = false;
            if ( linkModules )
            {
                linkAsModule = isReactorProject( artifact.getGroupId(), artifact.getArtifactId() );
            }

            if ( linkAsModule )
            {
                containerElement.addAttribute( "type", "module" );
                containerElement.addAttribute( "name", artifact.getArtifactId() );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "5" );
                Element uriAttribute = createElement( containerElement, "attribute" );
                uriAttribute.addAttribute( "name", "URI" );
                uriAttribute.addAttribute( "value", "/WEB-INF/classes" );
            }
            else if ( artifact.getFile() != null )
            {
                containerElement.addAttribute( "type", "library" );
                containerElement.addAttribute( "level", "module" );
                Element methodAttribute = createElement( containerElement, "attribute" );
                methodAttribute.addAttribute( "name", "method" );
                methodAttribute.addAttribute( "value", "1" ); // IntelliJ 5.0.2 is bugged and doesn't read it
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
        return getModuleFileUrl( project.getBasedir(), file );
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

    /**
     * Removes dependencies from Xpp3Dom component.
     *
     * @param component Xpp3Dom element
     */
    private void removeOldDependencies( Element component )
    {
        for ( Iterator children = component.elementIterator(); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if ( "orderEntry".equals( child.getName() ) && "module-library".equals( child.attributeValue( "type" ) ) )
            {
                component.remove( child );
            }
        }
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

    private void addResources( Element component, String directory )
    {
        Element dep = createElement( component, "orderEntry" );
        dep.addAttribute( "type", "module-library" );
        dep = createElement( dep, "library" );
        dep.addAttribute( "name", "resources" );

        Element el = createElement( dep, "CLASSES" );
        el = createElement( el, "root" );
        el.addAttribute( "url", getModuleFileUrl( directory ) );
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
