package org.apache.maven.plugin.war.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.codehaus.plexus.interpolation.InterpolationException;

/**
 * Tests the mapping of file names.
 *
 * @author Stephane Nicoll
 */
public class MappingUtilsTest
    extends TestCase
{

    public void testCompleteMapping()
        throws MojoExecutionException, InterpolationException
    {
        TestArtifactStub jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        assertEquals( "maven-test-lib-1.0.jar",
                      MappingUtils.evaluateFileNameMapping( "@{artifactId}@-@{version}@.@{extension}@", jar ) );

    }

    public void testNoVersionMapping()
        throws MojoExecutionException, InterpolationException
    {
        TestArtifactStub jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        assertEquals( "maven-test-lib.jar", MappingUtils.evaluateFileNameMapping( "@{artifactId}@.@{extension}@", jar ) );

    }

    public void testMappingWithGroupId()
        throws MojoExecutionException, InterpolationException
    {
        TestArtifactStub jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        assertEquals( "org.apache.sample-maven-test-lib-1.0.jar",
                      MappingUtils.evaluateFileNameMapping( "@{groupId}@-@{artifactId}@-@{version}@.@{extension}@", jar ) );

    }

    public void testMappingWithClassifier()
        throws MojoExecutionException, InterpolationException
    {
        TestArtifactStub jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        jar.setClassifier( "classifier" );
        assertEquals( "maven-test-lib-1.0-classifier.jar",
                      MappingUtils.evaluateFileNameMapping( MappingUtils.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, jar ) );
    }

    public void testMappingWithNullClassifier()
        throws MojoExecutionException, InterpolationException
    {
        TestArtifactStub jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        jar.setClassifier( null );
        assertEquals( "maven-test-lib-1.0-.jar",
                      MappingUtils.evaluateFileNameMapping( MappingUtils.DEFAULT_FILE_NAME_MAPPING_CLASSIFIER, jar ) );
    }

    /**
     * Test for MWAR-212.
     */
    public void testMappingWithOptionalClassifier()
        throws MojoExecutionException, InterpolationException
    {
        final String MAPPING_WITH_OPTIONAL_CLASSIFIER_1 = "@{artifactId}@-@{version}@@{dashClassifier}@.@{extension}@";
        final String MAPPING_WITH_OPTIONAL_CLASSIFIER_2 = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

        TestArtifactStub jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        assertEquals( "maven-test-lib-1.0.jar",
                      MappingUtils.evaluateFileNameMapping( MAPPING_WITH_OPTIONAL_CLASSIFIER_1, jar ) );
        assertEquals( "maven-test-lib-1.0.jar",
                      MappingUtils.evaluateFileNameMapping( MAPPING_WITH_OPTIONAL_CLASSIFIER_2, jar ) );

        jar = new TestArtifactStub();
        jar.setGroupId( "org.apache.sample" );
        jar.setArtifactId( "maven-test-lib" );
        jar.setVersion( "1.0" );
        jar.setClassifier( "classifier" );
        assertEquals( "maven-test-lib-1.0-classifier.jar",
                      MappingUtils.evaluateFileNameMapping( MAPPING_WITH_OPTIONAL_CLASSIFIER_1, jar ) );
        assertEquals( "maven-test-lib-1.0-classifier.jar",
                      MappingUtils.evaluateFileNameMapping( MAPPING_WITH_OPTIONAL_CLASSIFIER_2, jar ) );
    }

    // A very dumb stub used to test the mappings
    class TestArtifactStub
        extends ArtifactStub
    {
        /**
         * Override in this class because there is no setter in ArtifactStub.
         */
        protected String classifier;

        public TestArtifactStub()
        {
            super();
            setType( "jar" );
        }

        /**
         * Override in this class because ArtifactStub always returns null.
         */
        @Override
        public ArtifactHandler getArtifactHandler()
        {
            return new DefaultArtifactHandler( getType() );
        }

        @Override
        public String getBaseVersion()
        {
          return getVersion();
        }

        @Override
        public String getClassifier()
        {
            return classifier;
        }

        public void setClassifier( String classifier )
        {
            this.classifier = classifier;
        }
    }
}
