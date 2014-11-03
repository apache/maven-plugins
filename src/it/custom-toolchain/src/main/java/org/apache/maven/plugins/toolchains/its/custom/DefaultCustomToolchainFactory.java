package org.apache.maven.plugins.toolchains.its.custom;

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

import java.io.File;

import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.RequirementMatcherFactory;
import org.apache.maven.toolchain.ToolchainFactory;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 *
 * @author Hervé Boutemy
 */
@Component( role = ToolchainFactory.class, hint = "custom" )
public class DefaultCustomToolchainFactory
    implements ToolchainFactory
{

    @Requirement
    private Logger logger;

    public DefaultCustomToolchainFactory()
    {
    }

    public ToolchainPrivate createToolchain( ToolchainModel model )
        throws MisconfiguredToolchainException
    {
        if ( model == null )
        {
            return null;
        }
        DefaultCustomToolchain customToolchain = new DefaultCustomToolchain( model, logger );
        Xpp3Dom dom = (Xpp3Dom) model.getConfiguration();
        Xpp3Dom toolHome = dom.getChild( DefaultCustomToolchain.KEY_TOOLHOME );
        if ( toolHome == null )
        {
            throw new MisconfiguredToolchainException( "Custom toolchain without the "
                + DefaultCustomToolchain.KEY_TOOLHOME + " configuration element." );
        }
        File normal = new File( FileUtils.normalize( toolHome.getValue() ) );
        if ( normal.exists() )
        {
            customToolchain.setToolHome( FileUtils.normalize( toolHome.getValue() ) );
        }
        else
        {
            // for this IT, don't really check the toolHome directory exists...
            // throw new MisconfiguredToolchainException( "Non-existing tool home configuration at "
            //    + normal.getAbsolutePath() );
            customToolchain.setToolHome( FileUtils.normalize( toolHome.getValue() ) );
        }

        // now populate the provides section.
        dom = (Xpp3Dom) model.getProvides();
        Xpp3Dom[] provides = dom.getChildren();
        for ( Xpp3Dom provide : provides )
        {
            String key = provide.getName();
            String value = provide.getValue();
            if ( value == null )
            {
                throw new MisconfiguredToolchainException(
                    "Provides token '" + key + "' doesn't have any value configured." );
            }
            if ( "version".equals( key ) )
            {
                customToolchain.addProvideToken( key, RequirementMatcherFactory.createVersionMatcher( value ) );
            }
            else
            {
                customToolchain.addProvideToken( key, RequirementMatcherFactory.createExactMatcher( value ) );
            }
        }
        return customToolchain;
    }

    public ToolchainPrivate createDefaultToolchain()
    {
        // not sure it's necessary to provide a default toolchain here.
        return null;
    }

    protected Logger getLogger()
    {
        return logger;
    }

}
