/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com)
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 *
 */
public class ClassifierTypeTranslator
    implements ArtifactTranslator
{

    private String classifier;

    private String type;

    private ArtifactFactory factory;

    public ClassifierTypeTranslator( String classifier, String type, ArtifactFactory factory )
    {
        this.classifier = classifier;
        this.type = type;
        this.factory = factory;
    }

    /* (non-Javadoc)
     * @see org.apache.mojo.dependency.utils.translators.ArtifactTranslator#translate(java.util.Set, org.apache.maven.plugin.logging.Log)
     */
    public Set translate( Set artifacts, Log log )
    {
        Set results = artifacts;

        log.debug( "Translating Artifacts using Classifier: " + this.classifier + " and Type: " + this.type );
        results = new HashSet();
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            // Create a new artifact
            Artifact newArtifact = factory.createArtifactWithClassifier( artifact.getGroupId(), artifact
                .getArtifactId(), artifact.getVersion(), this.type, this.classifier );

            results.add( newArtifact );
        }

        return results;
    }

    /**
     * @return Returns the classifier.
     */
    public String getClassifier()
    {
        return this.classifier;
    }

    /**
     * @param classifier The classifier to set.
     */
    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    /**
     * @return Returns the type.
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type The type to set.
     */
    public void setType( String type )
    {
        this.type = type;
    }

}
