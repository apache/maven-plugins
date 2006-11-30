/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.maven.plugin.dependency.testUtils.stubs;
/*
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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * very simple stub of maven project, going to take a lot of work to make it
 * useful as a stub though
 */
public class DependencyProjectStub
    extends MavenProject
{
    private String groupId;

    private String artifactId;

    private String name;

    private Model model;

    private MavenProject parent;

    private List dependencies;
    
    private File file;

    private List collectedProjects;

    private List attachedArtifacts;

    private List compileSourceRoots;

    private List testCompileSourceRoots;

    private List scriptSourceRoots;

    private List pluginArtifactRepositories;

    private ArtifactRepository releaseArtifactRepository;

    private ArtifactRepository snapshotArtifactRepository;

    private List activeProfiles;

    private Set dependencyArtifacts;

    private DependencyManagement dependencyManagement;
    
    private Artifact artifact;

    private Map artifactMap;

    private Model originalModel;

    private Map pluginArtifactMap;

    private Map reportArtifactMap;

    private Map extensionArtifactMap;

    private Map projectReferences;

    private Build buildOverlay;

    private boolean executionRoot;

    private List compileArtifacts;

    private List compileDependencies;

    private List systemDependencies;

    private List testClasspathElements;

    private List testDependencies;

    private List systemClasspathElements;

    private List systemArtifacts;

    private List testArtifacts;

    private List runtimeArtifacts;

    private List runtimeDependencies;

    private List runtimeClasspathElements;

    private String modelVersion;

    private String packaging;

    private String inceptionYear;

    private String url;

    private String description;

    private String version;

    private String defaultGoal;

    private Set artifacts;

    public DependencyProjectStub()
    {
        super( (Model) null );
    }

    // kinda dangerous...
    public DependencyProjectStub( Model model )
    {
        // super(model);
        super( (Model) null );
    }

    // kinda dangerous...
    public DependencyProjectStub( MavenProject project )
    {
        // super(project);
        super( (Model) null );
    }

    public String getModulePathAdjustment( MavenProject mavenProject )
        throws IOException
    {
        return "";
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public Model getModel()
    {
        return model;
    }

    public MavenProject getParent()
    {
        return parent;
    }

    public void setParent( MavenProject mavenProject )
    {
        this.parent = mavenProject;
    }

    public void setRemoteArtifactRepositories( List list )
    {

    }

    public List getRemoteArtifactRepositories()
    {
        return Collections.singletonList( "" );
    }

    public boolean hasParent()
    {
        if ( parent != null )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public File getBasedir()
    {
        return new File( PlexusTestCase.getBasedir() );
    }

    public void setDependencies( List list )
    {
        dependencies = list;
    }

    public List getDependencies()
    {
        if (dependencies == null)
        {
            dependencies = Collections.EMPTY_LIST;
        }
        return dependencies;
    }

    public DependencyManagement getDependencyManagement()
    {
        if (dependencyManagement == null)
        {
            dependencyManagement = new DependencyManagement();
        }
        
        return dependencyManagement;
    }

    public void addCompileSourceRoot( String string )
    {
        if ( compileSourceRoots == null )
        {
            compileSourceRoots = Collections.singletonList( string );
        }
        else
        {
            compileSourceRoots.add( string );
        }
    }

    public void addScriptSourceRoot( String string )
    {
        if ( scriptSourceRoots == null )
        {
            scriptSourceRoots = Collections.singletonList( string );
        }
        else
        {
            scriptSourceRoots.add( string );
        }
    }

    public void addTestCompileSourceRoot( String string )
    {
        if ( testCompileSourceRoots == null )
        {
            testCompileSourceRoots = Collections.singletonList( string );
        }
        else
        {
            testCompileSourceRoots.add( string );
        }
    }

    public List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    public List getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    public List getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    public List getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return compileSourceRoots;
    }

    public void setCompileArtifacts( List compileArtifacts )
    {
        this.compileArtifacts = compileArtifacts;
    }

    public List getCompileArtifacts()
    {
        return compileArtifacts;
    }

    public List getCompileDependencies()
    {
        return compileDependencies;
    }

    public List getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return testClasspathElements;
    }

    public List getTestArtifacts()
    {
        return testArtifacts;
    }

    public List getTestDependencies()
    {
        return testDependencies;
    }

    public List getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return runtimeClasspathElements;
    }

    public List getRuntimeArtifacts()
    {
        return runtimeArtifacts;
    }

    public List getRuntimeDependencies()
    {
        return runtimeDependencies;
    }

    public List getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return systemClasspathElements;
    }

    public List getSystemArtifacts()
    {
        return systemArtifacts;
    }

    public void setRuntimeClasspathElements( List runtimeClasspathElements )
    {
        this.runtimeClasspathElements = runtimeClasspathElements;
    }

    public void setAttachedArtifacts( List attachedArtifacts )
    {
        this.attachedArtifacts = attachedArtifacts;
    }

    public void setCompileSourceRoots( List compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    public void setTestCompileSourceRoots( List testCompileSourceRoots )
    {
        this.testCompileSourceRoots = testCompileSourceRoots;
    }

    public void setScriptSourceRoots( List scriptSourceRoots )
    {
        this.scriptSourceRoots = scriptSourceRoots;
    }

    public void setArtifactMap( Map artifactMap )
    {
        this.artifactMap = artifactMap;
    }

    public void setPluginArtifactMap( Map pluginArtifactMap )
    {
        this.pluginArtifactMap = pluginArtifactMap;
    }

    public void setReportArtifactMap( Map reportArtifactMap )
    {
        this.reportArtifactMap = reportArtifactMap;
    }

    public void setExtensionArtifactMap( Map extensionArtifactMap )
    {
        this.extensionArtifactMap = extensionArtifactMap;
    }

    public void setProjectReferences( Map projectReferences )
    {
        this.projectReferences = projectReferences;
    }

    public void setBuildOverlay( Build buildOverlay )
    {
        this.buildOverlay = buildOverlay;
    }

    public void setCompileDependencies( List compileDependencies )
    {
        this.compileDependencies = compileDependencies;
    }

    public void setSystemDependencies( List systemDependencies )
    {
        this.systemDependencies = systemDependencies;
    }

    public void setTestClasspathElements( List testClasspathElements )
    {
        this.testClasspathElements = testClasspathElements;
    }

    public void setTestDependencies( List testDependencies )
    {
        this.testDependencies = testDependencies;
    }

    public void setSystemClasspathElements( List systemClasspathElements )
    {
        this.systemClasspathElements = systemClasspathElements;
    }

    public void setSystemArtifacts( List systemArtifacts )
    {
        this.systemArtifacts = systemArtifacts;
    }

    public void setTestArtifacts( List testArtifacts )
    {
        this.testArtifacts = testArtifacts;
    }

    public void setRuntimeArtifacts( List runtimeArtifacts )
    {
        this.runtimeArtifacts = runtimeArtifacts;
    }

    public void setRuntimeDependencies( List runtimeDependencies )
    {
        this.runtimeDependencies = runtimeDependencies;
    }

    public void setModel( Model model )
    {
        this.model = model;
    }

    public List getSystemDependencies()
    {
        return systemDependencies;
    }

    public void setModelVersion( String string )
    {
        this.modelVersion = string;
    }

    public String getModelVersion()
    {
        return modelVersion;
    }

    public String getId()
    {
        return "";
    }

    public void setGroupId( String string )
    {
        this.groupId = string;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setArtifactId( String string )
    {
        this.artifactId = string;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setName( String string )
    {
        this.name = string;
    }

    public String getName()
    {
        return name;
    }

    public void setVersion( String string )
    {
        this.version = string;
    }

    public String getVersion()
    {
        return version;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String string )
    {
        this.packaging = string;
    }

    public void setInceptionYear( String string )
    {
        this.inceptionYear = string;
    }

    public String getInceptionYear()
    {
        return inceptionYear;
    }

    public void setUrl( String string )
    {
        this.url = string;
    }

    public String getUrl()
    {
        return url;
    }

    public Prerequisites getPrerequisites()
    {
        return null;
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {

    }

    public CiManagement getCiManagement()
    {
        return null;
    }

    public void setCiManagement( CiManagement ciManagement )
    {

    }

    public IssueManagement getIssueManagement()
    {
        return null;
    }

    public void setDistributionManagement( DistributionManagement distributionManagement )
    {

    }

    public DistributionManagement getDistributionManagement()
    {
        return null;
    }

    public void setDescription( String string )
    {
        this.description = string;
    }

    public String getDescription()
    {
        return description;
    }

    public void setOrganization( Organization organization )
    {

    }

    public Organization getOrganization()
    {
        return null;
    }

    public void setScm( Scm scm )
    {

    }

    public Scm getScm()
    {
        return null;
    }

    public void setMailingLists( List list )
    {

    }

    public List getMailingLists()
    {
        return Collections.singletonList( "" );
    }

    public void addMailingList( MailingList mailingList )
    {

    }

    public void setDevelopers( List list )
    {

    }

    public List getDevelopers()
    {
        return Collections.singletonList( "" );
    }

    public void addDeveloper( Developer developer )
    {

    }

    public void setContributors( List list )
    {

    }

    public List getContributors()
    {
        return Collections.singletonList( "" );
    }

    public void addContributor( Contributor contributor )
    {

    }

    public void setBuild( Build build )
    {

    }

    public Build getBuild()
    {
        return null;
    }

    public List getResources()
    {
        return Collections.singletonList( "" );
    }

    public List getTestResources()
    {
        return Collections.singletonList( "" );
    }

    public void addResource( Resource resource )
    {

    }

    public void addTestResource( Resource resource )
    {

    }

    public void setReporting( Reporting reporting )
    {

    }

    public Reporting getReporting()
    {
        return null;
    }

    public void setLicenses( List list )
    {

    }

    public List getLicenses()
    {
        return Collections.singletonList( "" );
    }

    public void addLicense( License license )
    {

    }

    public void setArtifacts( Set set )
    {
        this.artifacts = set;
    }

    public Set getArtifacts()
    {
        if ( artifacts == null )
        {
            return Collections.EMPTY_SET;
        }
        else
        {
            return artifacts;
        }
    }

    public Map getArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    public void setPluginArtifacts( Set set )
    {

    }

    public Set getPluginArtifacts()
    {
        return Collections.singleton( "" );
    }

    public Map getPluginArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    public void setReportArtifacts( Set set )
    {

    }

    public Set getReportArtifacts()
    {
        return Collections.singleton( "" );
    }

    public Map getReportArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    public void setExtensionArtifacts( Set set )
    {

    }

    public Set getExtensionArtifacts()
    {
        return Collections.singleton( "" );
    }

    public Map getExtensionArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    public void setParentArtifact( Artifact artifact )
    {

    }

    public Artifact getParentArtifact()
    {
        return null;
    }

    public List getRepositories()
    {
        return Collections.singletonList( "" );
    }

    public List getReportPlugins()
    {
        return Collections.singletonList( "" );
    }

    public List getBuildPlugins()
    {
        return Collections.singletonList( "" );
    }

    public List getModules()
    {
        return Collections.singletonList( "" );
    }

    public PluginManagement getPluginManagement()
    {
        return null;
    }

    public void addPlugin( Plugin plugin )
    {

    }

    public void injectPluginManagementInfo( Plugin plugin )
    {

    }

    public List getCollectedProjects()
    {
        return collectedProjects;
    }

    public void setCollectedProjects( List list )
    {
        this.collectedProjects = list;
    }

    public void setPluginArtifactRepositories( List list )
    {
        this.pluginArtifactRepositories = list;
    }

    public List getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return null;
    }

    public List getPluginRepositories()
    {
        return Collections.singletonList( "" );
    }

    public void setActiveProfiles( List list )
    {
        activeProfiles = list;
    }

    public List getActiveProfiles()
    {
        return activeProfiles;
    }

    public void addAttachedArtifact( Artifact theArtifact )
    {
        if ( attachedArtifacts == null )
        {
            this.attachedArtifacts = Collections.singletonList( theArtifact );
        }
        else
        {
            attachedArtifacts.add( theArtifact );
        }
    }

    public List getAttachedArtifacts()
    {
        return attachedArtifacts;
    }

    public Xpp3Dom getGoalConfiguration( String string, String string1, String string2, String string3 )
    {
        return null;
    }

    public Xpp3Dom getReportConfiguration( String string, String string1, String string2 )
    {
        return null;
    }

    public MavenProject getExecutionProject()
    {
        return null;
    }

    public void setExecutionProject( MavenProject mavenProject )
    {

    }

    public void writeModel( Writer writer )
        throws IOException
    {

    }

    public void writeOriginalModel( Writer writer )
        throws IOException
    {

    }

    public Set getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    public void setDependencyArtifacts( Set set )
    {
        this.dependencyArtifacts = set;
    }

    public void setReleaseArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.releaseArtifactRepository = artifactRepository;
    }

    public void setSnapshotArtifactRepository( ArtifactRepository artifactRepository )
    {
        this.snapshotArtifactRepository = artifactRepository;
    }

    public void setOriginalModel( Model model )
    {
        this.originalModel = model;
    }

    public Model getOriginalModel()
    {
        return originalModel;
    }

    public List getBuildExtensions()
    {
        return Collections.singletonList( "" );
    }

    public Set createArtifacts( ArtifactFactory artifactFactory, String string, ArtifactFilter artifactFilter )
        throws InvalidDependencyVersionException
    {
        return Collections.EMPTY_SET;
    }

    public void addProjectReference( MavenProject mavenProject )
    {

    }

    public void attachArtifact( String string, String string1, File theFile )
    {

    }

    public Properties getProperties()
    {
        return new Properties();
    }

    public List getFilters()
    {
        return Collections.singletonList( "" );
    }

    public Map getProjectReferences()
    {
        return Collections.singletonMap( "", "" );
    }

    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    public void setExecutionRoot( boolean b )
    {
        this.executionRoot = b;
    }

    public String getDefaultGoal()
    {
        return defaultGoal;
    }

    public Artifact replaceWithActiveArtifact( Artifact artifact )
    {
        return null;
    }
}
