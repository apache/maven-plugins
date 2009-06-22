package org.apache.maven.plugins.pdf.stubs;

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

import java.io.File;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author ltheussl
 * @version $Id$
 */
public class ModelBuilderMavenProjectStub
    extends MavenProjectStub
{
    /**
     * Stub to test the DocumentModelBuilder.
     */
    public ModelBuilderMavenProjectStub()
    {
        try
        {
            Model model = new MavenXpp3Reader().read(
                    new FileReader( new File( getBasedir() + "/pom_model_builder.xml" ) ) );
            setModel( model );

            setGroupId( model.getGroupId() );
            setArtifactId( model.getArtifactId() );
            setVersion( model.getVersion() );
            setName( model.getName() );
            setDescription( model.getDescription() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    /** {@inheritDoc}
     * @return the test base dir: "/target/test-classes/unit/pdf/".
     */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/target/test-classes/unit/pdf/" );
    }
}
