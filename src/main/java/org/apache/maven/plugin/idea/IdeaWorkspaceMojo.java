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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @goal workspace
 * @execute phase="generate-sources"
 * @todo use dom4j or something. Xpp3Dom can't cope properly with entities and so on
 *
 * @author Edwin Punzalan
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
            doDependencyResolution( project, artifactFactory, artifactResolver, localRepo, artifactMetadataSource );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to build project dependencies.", e );
        }

        File workspaceFile = new File( project.getBasedir(), project.getArtifactId() + ".iws" );

        FileWriter writer = null;

        Reader reader = null;

        Xpp3Dom module;

        try
        {
            if ( workspaceFile.exists() && !overwrite )
            {
                reader = new FileReader( workspaceFile );
            }
            else
            {
                reader = new InputStreamReader( getClass().getResourceAsStream( "/templates/default/workspace.xml" ) );
            }
            module = Xpp3DomBuilder.build( reader );

            setProjectScmType( module );

            writer = new FileWriter( workspaceFile );

            Xpp3DomWriter.write( writer, module );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing existing IWS file: " + workspaceFile.getAbsolutePath(),
                                              e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create workspace file", e );
        }
        finally
        {
            IOUtil.close( reader );

            IOUtil.close( writer );
        }
    }

    /**
     * Sets the SCM type of the project
     */
    private void setProjectScmType( Xpp3Dom content )
    {
        String scmType;

        scmType = getScmType();

        if ( scmType != null )
        {
            Xpp3Dom component = findComponent( content, "VcsManagerConfiguration" );

            Xpp3Dom element = findElementName( component, "option", "ACTIVE_VCS_NAME" );

            element.setAttribute( "value", scmType );
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

        if ( project.getScm() == null )
        {
            return null;
        }
        scmType = getScmType( project.getScm().getConnection() );

        if ( scmType != null )
        {
            return scmType;
        }
        scmType = getScmType( project.getScm().getDeveloperConnection() );

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

    /**
     * Returns a an Xpp3Dom element with (child) tag name and (name) attribute name.
     *
     * @param component Xpp3Dom element
     * @param name      Setting attribute to find
     * @return option Xpp3Dom element
     */
    private Xpp3Dom findElementName( Xpp3Dom component, String child, String name )
    {
        Xpp3Dom[] elements = component.getChildren( child );
        for ( int i = 0; i < elements.length; i++ )
        {
            if ( name.equals( elements[i].getAttribute( "name" ) ) )
            {
                return elements[i];
            }
        }

        Xpp3Dom element = createElement( component, child );
        element.setAttribute( "name", name );
        return element;
    }
}
