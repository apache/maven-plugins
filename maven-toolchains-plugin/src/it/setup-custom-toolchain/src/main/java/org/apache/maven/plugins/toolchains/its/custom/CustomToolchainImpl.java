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

import org.apache.maven.toolchain.DefaultToolchain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;

/**
 * @author Hervé Boutemy
 */
public class CustomToolchainImpl
    extends DefaultToolchain
    implements CustomToolchain
{
    private String toolHome;

    public static final String KEY_TOOLHOME = "toolHome";

    public CustomToolchainImpl( ToolchainModel model, Logger logger )
    {
        super( model, "custom", logger );
    }

    public String getToolHome()
    {
        return toolHome;
    }

    public void setToolHome( String toolHome )
    {
        this.toolHome = toolHome;
    }

    public String toString()
    {
        return "Custom[" + getToolHome() + "]";
    }

    public String findTool( String toolName )
    {
        File tool = findTool( toolName, new File( getToolHome() ) );

        if ( tool != null )
        {
            return tool.getAbsolutePath();
        }

        return null;
    }

    private static File findTool( String toolName, File installFolder )
    {
        File bin = new File( installFolder, "bin" ); //NOI18N

        if ( bin.exists() )
        {
            File tool = new File( bin, toolName + ( Os.isFamily( "windows" ) ? ".exe" : "" ) ); // NOI18N

            if ( tool.exists() )
            {
                return tool;
            }
        }

        return null;
   }
}