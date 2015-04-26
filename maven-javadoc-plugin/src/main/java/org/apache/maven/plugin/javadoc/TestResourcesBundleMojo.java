package org.apache.maven.plugin.javadoc;

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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Bundle {@link TestJavadocJar#testJavadocDirectory}, along with javadoc configuration options from
 * {@link AbstractJavadocMojo} such as taglet, doclet, and link information into a deployable
 * artifact. This artifact can then be consumed by the javadoc plugin mojos when used by the
 * <code>includeDependencySources</code> option, to generate javadocs that are somewhat consistent
 * with those generated in the original project itself.
 *
 * @since 2.7
 */
@Mojo( name = "test-resource-bundle", defaultPhase = LifecyclePhase.PACKAGE,
       requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class TestResourcesBundleMojo
    extends ResourcesBundleMojo
{

    /**
     * Specifies the Test Javadoc resources directory to be included in the Javadoc (i.e. package.html, images...).
     */
    @Parameter( alias = "javadocDirectory", defaultValue = "${basedir}/src/test/javadoc" )
    private File testJavadocDirectory;

    @Override
    protected String getAttachmentClassifier()
    {
        return TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER;
    }

    @Override
    protected File getJavadocDirectory()
    {
        return testJavadocDirectory;
    }

    @Override
    protected boolean isTest()
    {
        return true;
    }
}
