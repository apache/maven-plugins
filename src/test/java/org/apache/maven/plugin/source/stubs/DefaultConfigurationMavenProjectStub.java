package org.apache.maven.plugin.source.stubs;

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
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class DefaultConfigurationMavenProjectStub
    extends MavenProjectStub
{

    private Build build;

    private List resources;

    private List testResources;

    private Artifact parentArtifact;

    private List attachedArtifacts;

    public DefaultConfigurationMavenProjectStub()
    {
        setGroupId( "default.configuration" );
        setArtifactId( "default-configuration" );
        setVersion( "1.0-SNAPSHOT" );
        setName( "Maven Source Plugin Default Configuration" );
        setPackaging( "jar" );
        setUrl( "http://maven.apache.org" );
        setInceptionYear( "2006" );
        setDescription( "Test Project that contains the default configuration." );

        Build build = new Build();
        build.setFinalName( "default-configuration" );
        build.setDirectory( getBasedir() + "/target/test/unit/default-configuration/target" );

        setBuild( build );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( basedir + "/src/test/resources/unit/default-configuration/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        List resources = new ArrayList();
        Resource resource = new Resource();
        resource.setDirectory( basedir + "/src/test/resources/unit/default-configuration/src/main/resources" );
        resources.add( resource );
        setResources( resources );

        List testSourceRoots = new ArrayList();
        testSourceRoots.add( basedir + "/src/test/resources/unit/default-configuration/src/test/java" );
        setTestCompileSourceRoots( testSourceRoots );

        List testResources = new ArrayList();
        resource = new Resource();
        resource.setDirectory( basedir + "/src/test/resources/unit/default-configuration/src/test/resources" );
        testResources.add( resource );
        setTestResources( testResources );

        Artifact artifact = new SourcePluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.hasClassifier();

        setArtifact( artifact );
    }

    public Build getBuild()
    {
        return build;
    }

    public void setBuild( Build build )
    {
        this.build = build;
    }

    public List getResources()
    {
        return resources;
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

    public List getTestResources()
    {
        return testResources;
    }

    public void setTestResources( List testResources )
    {
        this.testResources = testResources;
    }

    public void setParentArtifact( Artifact parentArtifact )
    {
        this.parentArtifact = parentArtifact;
    }

    public Artifact getParentArtifact()
    {
        return parentArtifact;
    }

    public void addAttachedArtifact( Artifact artifact )
    {
        if ( attachedArtifacts == null )
        {
            attachedArtifacts = new ArrayList();
        }
        attachedArtifacts.add( artifact );
    }

    public List getAttachedArtifacts()
    {
        return attachedArtifacts;
    }
}
