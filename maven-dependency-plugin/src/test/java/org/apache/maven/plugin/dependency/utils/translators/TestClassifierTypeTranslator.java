/*
 * Copyright Apache Software Foundation
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
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.translators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class TestClassifierTypeTranslator
    extends AbstractDependencyMojoTestCase
{
    Set artifacts = new HashSet();

    ArtifactFactory artifactFactory;

    Log log = new SilentLog();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactHandlerManager manager = new DefaultArtifactHandlerManager();
        this.setVariableValueToObject( manager, "artifactHandlers", new HashMap() );

        artifactFactory = new DefaultArtifactFactory();
        this.setVariableValueToObject( artifactFactory, "artifactHandlerManager", manager );

        ArtifactStubFactory factory = new ArtifactStubFactory( null, false );
        artifacts = factory.getMixedArtifacts();
    }

    public void testNullClassifier()
    {
        doTestNullEmptyClassifier( null );
    }

    public void testEmptyClassifier()
    {
        doTestNullEmptyClassifier( "" );
    }

    public void doTestNullEmptyClassifier( String classifier )
    {
        String type = "zip";

        ArtifactTranslator at = new ClassifierTypeTranslator( classifier, type, artifactFactory );
        Set results = at.translate( artifacts, log );

        Iterator iter = artifacts.iterator();

        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            Iterator resultIter = results.iterator();
            boolean found = false;
            while ( !found && resultIter.hasNext() )
            {
                Artifact translatedArtifact = (Artifact) resultIter.next();
                if ( artifact.getArtifactId() == translatedArtifact.getArtifactId()
                    && artifact.getGroupId() == translatedArtifact.getGroupId()
                    && artifact.getScope() == translatedArtifact.getScope() )
                {
                    // classifier is null, should be the same as the artifact
                    assertEquals( artifact.getClassifier(), translatedArtifact.getClassifier() );
                    assertEquals( type, translatedArtifact.getType() );

                    found = true;
                    break;
                }
            }
            assertTrue( found );
        }
    }

    public void testNullType()
    {
        doTestNullEmptyType( null );
    }

    public void testEmptyType()
    {
        doTestNullEmptyType( "" );
    }

    public void doTestNullEmptyType( String type )
    {
        String classifier = "jdk5";

        ArtifactTranslator at = new ClassifierTypeTranslator( classifier, type, artifactFactory );
        Set results = at.translate( artifacts, log );

        Iterator iter = artifacts.iterator();

        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            Iterator resultIter = results.iterator();
            boolean found = false;
            while ( !found && resultIter.hasNext() )
            {
                Artifact translatedArtifact = (Artifact) resultIter.next();
                if ( artifact.getArtifactId() == translatedArtifact.getArtifactId()
                    && artifact.getGroupId() == translatedArtifact.getGroupId()
                    && artifact.getScope() == translatedArtifact.getScope() )
                {
                    // classifier is null, should be the same as the artifact
                    assertEquals( classifier, translatedArtifact.getClassifier() );
                    assertEquals( artifact.getType(), translatedArtifact.getType() );

                    found = true;
                    break;
                }
            }
            assertTrue( found );
        }
    }

    public void testClassifierAndType()
    {
        String classifier = "jdk14";
        String type = "sources";
        ArtifactTranslator at = new ClassifierTypeTranslator( classifier, type, artifactFactory );
        Set results = at.translate( artifacts, log );

        Iterator iter = artifacts.iterator();

        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            Iterator resultIter = results.iterator();
            boolean found = false;
            while ( !found && resultIter.hasNext() )
            {
                Artifact translatedArtifact = (Artifact) resultIter.next();
                if ( artifact.getArtifactId() == translatedArtifact.getArtifactId()
                    && artifact.getGroupId() == translatedArtifact.getGroupId()
                    && artifact.getScope() == translatedArtifact.getScope() )
                {
                    assertEquals( translatedArtifact.getClassifier(), classifier );
                    assertEquals( translatedArtifact.getType(), type );

                    found = true;
                    break;
                }
            }
            assertTrue( found );
        }
    }

    public void testGetterSetter()
    {
        String classifier = "class";
        String type = "type";
        ClassifierTypeTranslator at = new ClassifierTypeTranslator( classifier, type, artifactFactory );

        assertEquals( classifier, at.getClassifier() );
        assertEquals( type, at.getType() );

        at.setClassifier( type );
        at.setType( classifier );

        assertEquals( type, at.getClassifier() );
        assertEquals( classifier, at.getType() );

    }
}
