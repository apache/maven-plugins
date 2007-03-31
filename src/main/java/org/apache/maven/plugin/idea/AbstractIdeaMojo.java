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
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

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

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractIdeaMojo
    extends AbstractMojo
{
    /**
     * The Maven Project.
     *
     * @parameter expression="${executedProject}"
     * @required
     * @readonly
     */
    protected MavenProject executedProject;

    /* holder for the log object only */
    protected Log log;

    /**
     * Whether to update the existing project files or overwrite them.
     *
     * @parameter expression="${overwrite}" default-value="false"
     */
    protected boolean overwrite;

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepo;

    /**
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @component role="org.apache.maven.artifact.metadata.ArtifactMetadataSource" hint="maven"
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    public void initParam( MavenProject project, ArtifactFactory artifactFactory, ArtifactRepository localRepo,
                           ArtifactResolver artifactResolver, ArtifactMetadataSource artifactMetadataSource, Log log,
                           boolean overwrite )
    {
        this.executedProject = project;

        this.log = log;

        this.artifactFactory = artifactFactory;

        this.localRepo = localRepo;

        this.artifactResolver = artifactResolver;

        this.artifactMetadataSource = artifactMetadataSource;

        this.overwrite = overwrite;
    }

    protected Document readXmlDocument( File file, String altFilename )
        throws DocumentException
    {
        SAXReader reader = new SAXReader();
        if ( file.exists() && !overwrite )
        {
            return reader.read( file );
        }
        else
        {
            File altFile = new File( executedProject.getBasedir(), "src/main/idea/" + altFilename );
            if ( altFile.exists() )
            {
                return reader.read( altFile );
            }
            else
            {
                return reader.read( getClass().getResourceAsStream( "/templates/default/" + altFilename ) );
            }
        }
    }

    protected void writeXmlDocument( File file, Document document )
        throws IOException
    {
        XMLWriter writer = new IdeaXmlWriter( file );
        writer.write( document );
        writer.close();
    }

    /**
     * Finds element from the module element.
     *
     * @param module Xpp3Dom element
     * @param name   Name attribute to find
     * @return component  Returns the Xpp3Dom element found.
     */
    protected Element findComponent( Element module, String name )
    {
        return findElement( module, "component", name );
    }

    protected Element findElement( Element element, String elementName, String attributeName )
    {
        for ( Iterator children = element.elementIterator( elementName ); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if ( attributeName.equals( child.attributeValue( "name" ) ) )
            {
                return child;
            }
        }
        return createElement( element, elementName ).addAttribute( "name", attributeName );
    }

    protected Element findElement( Element component, String name )
    {
        Element element = component.element( name );
        if ( element == null )
        {
            element = createElement( component, name );
        }
        return element;
    }

    /**
     * Creates an Xpp3Dom element.
     *
     * @param module Xpp3Dom element
     * @param name   Name of the element
     * @return component Xpp3Dom element
     */
    protected Element createElement( Element module, String name )
    {
        return module.addElement( name );
    }

    /**
     * Translate the absolutePath into its relative path.
     *
     * @param basedir      The basedir of the project.
     * @param absolutePath The absolute path that must be translated to relative path.
     * @return relative  Relative path of the parameter absolute path.
     */
    protected String toRelative( File basedir, String absolutePath )
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
     * Remove elements from content (Xpp3Dom).
     *
     * @param content Xpp3Dom element
     * @param name    Name of the element to be removed
     */
    protected void removeOldElements( Element content, String name )
    {
        for ( Iterator children = content.elementIterator(); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if ( name.equals( child.getName() ) )
            {
                content.remove( child );
            }
        }
    }

    protected void doDependencyResolution( MavenProject project, ArtifactRepository localRepo )
        throws InvalidDependencyVersionException, ProjectBuildingException, InvalidVersionSpecificationException
    {
        Map managedVersions =
            createManagedVersionMap( artifactFactory, project.getId(), project.getDependencyManagement() );

        try
        {
            ArtifactResolutionResult result = artifactResolver.resolveTransitively( getProjectArtifacts(),
                                                                                    project.getArtifact(),
                                                                                    managedVersions, localRepo,
                                                                                    project.getRemoteArtifactRepositories(),
                                                                                    artifactMetadataSource );

            project.setArtifacts( result.getArtifacts() );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLog().debug( e.getMessage(), e );

            StringBuffer msg = new StringBuffer();
            msg.append( "An error occurred during dependency resolution.\n\n" );
            msg.append( "    Failed to retrieve " + e.getDownloadUrl() + "\n" );
            msg.append( "from the following repositories:" );
            for ( Iterator repositories = e.getRemoteRepositories().iterator(); repositories.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) repositories.next();
                msg.append( "\n    " + repository.getId() + "(" + repository.getUrl() + ")" );
            }
            msg.append( "\nCaused by: " + e.getMessage() );

            getLog().warn( msg );
        }
        catch ( ArtifactResolutionException e )
        {
            getLog().debug( e.getMessage(), e );

            StringBuffer msg = new StringBuffer();
            msg.append( "An error occurred during dependency resolution of the following artifact:\n\n" );
            msg.append( "    " + e.getGroupId() + ":" + e.getArtifactId() + e.getVersion() + "\n\n" );
            msg.append( "Caused by: " + e.getMessage() );

            getLog().warn( msg );
        }
    }

    /*
    * @todo we need a more permanent feature that does this properly
    */
    protected String getPluginSetting( String artifactId, String optionName, String defaultValue )
    {
        for ( Iterator it = executedProject.getBuildPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();
            if ( plugin.getArtifactId().equals( artifactId ) )
            {
                Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();
                if ( o != null && o.getChild( optionName ) != null )
                {
                    return o.getChild( optionName ).getValue();
                }
            }
        }
        return defaultValue;
    }

    private Set getProjectArtifacts()
        throws InvalidVersionSpecificationException
    {
        Set artifacts = new HashSet();

        for ( Iterator dependencies = executedProject.getDependencies().iterator(); dependencies.hasNext(); )
        {
            Dependency dep = (Dependency) dependencies.next();

            String groupId = dep.getGroupId();
            String artifactId = dep.getArtifactId();
            VersionRange versionRange = VersionRange.createFromVersionSpec( dep.getVersion() );
            String type = dep.getType();
            if ( type == null )
            {
                type = "jar";
            }
            String classifier = dep.getClassifier();
            boolean optional = dep.isOptional();
            String scope = dep.getScope();
            if ( scope == null )
            {
                scope = Artifact.SCOPE_COMPILE;
            }

            Artifact artifact = artifactFactory.createDependencyArtifact( groupId, artifactId, versionRange, type,
                                                                          classifier, scope, optional );

            if ( scope.equalsIgnoreCase( Artifact.SCOPE_SYSTEM ) )
            {
                artifact.setFile( new File( dep.getSystemPath() ) );
            }

            List exclusions = new ArrayList();
            for ( Iterator j = dep.getExclusions().iterator(); j.hasNext(); )
            {
                Exclusion e = (Exclusion) j.next();
                exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
            }

            ArtifactFilter newFilter = new ExcludesArtifactFilter( exclusions );

            artifact.setDependencyFilter( newFilter );

            artifacts.add( artifact );
        }

        return artifacts;
    }

    private Map createManagedVersionMap( ArtifactFactory artifactFactory, String projectId,
                                         DependencyManagement dependencyManagement )
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

    public Log getLog()
    {
        if ( log == null )
        {
            log = super.getLog();
        }

        return log;
    }
}
