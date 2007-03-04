package org.apache.maven.plugin.jxr.stubs;

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
import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:dennisl@apache.org">Dennis Lundberg</a>
 */
public class IncludeConfigurationMavenProjectStub extends MavenProjectStub
{
    List reportPlugins = new ArrayList();

    public IncludeConfigurationMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( getBasedir() +
                "/src/test/resources/unit/include-configuration/include-configuration-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {

        }

        setArtifactId( model.getArtifactId() );
        setGroupId( model.getGroupId() );
        setVersion( model.getVersion() );
        setPackaging( model.getPackaging() );
        setInceptionYear( model.getInceptionYear() );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add(
            basedir + "/src/test/resources/unit/include-configuration/include/configuration" );
        setCompileSourceRoots( compileSourceRoots );

        // set the report plugins
        List reportPlugins = new ArrayList();
        for ( Iterator iter = model.getReporting().getPlugins().iterator(); iter.hasNext(); )
        {
            ReportPlugin plugin = (ReportPlugin) iter.next();
            reportPlugins.add( plugin );
        }
        setReportPlugins( reportPlugins );

        Artifact artifact = new JxrPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        setArtifact( artifact );

    }

    public void setReportPlugins( List plugins )
    {
        this.reportPlugins = plugins;
    }

    public List getReportPlugins()
    {
        return reportPlugins;
    }
}
