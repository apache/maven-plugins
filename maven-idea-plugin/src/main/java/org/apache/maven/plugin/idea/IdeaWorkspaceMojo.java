package org.apache.maven.plugin.idea;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;

/**
 * @author Edwin Punzalan
 * @goal workspace
 * @execute phase="generate-sources"
 */
public class IdeaWorkspaceMojo
    extends AbstractIdeaMojo
{
    /**
     * Create IDEA workspace (.iws) file.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            doDependencyResolution( executedProject, localRepo );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        rewriteWorkspace();
    }

    public void rewriteWorkspace()
        throws MojoExecutionException
    {
        File workspaceFile = new File( executedProject.getBasedir(), executedProject.getArtifactId() + ".iws" );

        try
        {
            Document document = readXmlDocument( workspaceFile, "workspace.xml" );

            Element module = document.getRootElement();

            setProjectScmType( module );

            writeXmlDocument( workspaceFile, document );
        }
        catch ( DocumentException e )
        {
            throw new MojoExecutionException( "Error parsing existing IWS file: " + workspaceFile.getAbsolutePath(),
                                              e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create workspace file", e );
        }
    }

    /**
     * Sets the SCM type of the project
     */
    private void setProjectScmType( Element content )
    {
        String scmType = getScmType();

        if ( scmType != null )
        {
            Element component = findComponent( content, "VcsManagerConfiguration" );

            Element element = findElement( component, "option", "ACTIVE_VCS_NAME" );

            element.addAttribute( "value", scmType );
        }
    }

    /**
     * used to retrieve the SCM Type
     *
     * @return the Scm Type string used to connect to the SCM
     */
    protected String getScmType()
    {
        String scmType;

        if ( executedProject.getScm() == null )
        {
            return null;
        }
        scmType = getScmType( executedProject.getScm().getConnection() );

        if ( scmType != null )
        {
            return scmType;
        }
        scmType = getScmType( executedProject.getScm().getDeveloperConnection() );

        return scmType;
    }

    protected String getScmType( String connection )
    {
        String scmType;

        if ( connection != null )
        {
            if ( connection.length() > 0 )
            {
                int startIndex = connection.indexOf( ":" );

                int endIndex = connection.indexOf( ":", startIndex + 1 );

                if ( startIndex < endIndex )
                {
                    scmType = connection.substring( startIndex + 1, endIndex );

                    return scmType;
                }
            }
        }
        return null;
    }
}
