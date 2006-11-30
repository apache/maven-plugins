package org.apache.maven.plugin.checkstyle;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * <p>
 * A component to work with VelocityTemplates from within plugins.
 * </p>
 *
 * <p/>
 * You will need to reference the velocity component as a parameter
 * in your plugin.  Like this:
 * </p>
 * <pre>
 * /&#042;&#042;
 *  &#042; Velocity Component
 *  &#042; &#064;parameter expression="${component.org.codehaus.plexus.velocity.VelocityComponent}"
 *  &#042; &#064;readonly
 *  &#042;/
 *  private VelocityComponent velocity;
 * </pre>
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 */
public class VelocityTemplate
{
    private String templateDirectory;

    private Log log;

    private VelocityComponent velocity;

    public VelocityTemplate( VelocityComponent velocityComponent, String templateBaseDirectory )
    {
        this.velocity = velocityComponent;
        this.templateDirectory = templateBaseDirectory;
    }

    public String getTemplateDirectory()
    {
        return templateDirectory;
    }

    public VelocityComponent getVelocity()
    {
        return velocity;
    }

    /**
     * Using a specified Velocity Template and provided context, create the outputFilename.
     *
     * @param outputFilename the file to be generated.
     * @param template       the velocity template to use.
     * @param context        the velocity context map.
     * @throws VelocityException if the template was not found or any other Velocity exception.
     * @throws MojoExecutionException
     * @throws IOException
     */
    public void generate( String outputFilename, String template, Context context )
        throws VelocityException, MojoExecutionException, IOException
    {
        File f;

        try
        {
            f = new File( outputFilename );

            if ( !f.getParentFile().exists() )
            {
                f.getParentFile().mkdirs();
            }

            Writer writer = new FileWriter( f );

            getVelocity().getEngine().mergeTemplate( templateDirectory + "/" + template, context, writer );

            writer.flush();
            writer.close();

            getLog().debug( "File " + outputFilename + " created..." );
        }

        catch ( ResourceNotFoundException e )
        {
            throw new ResourceNotFoundException( "Template not found: " + templateDirectory + "/" + template );
        }
        catch ( VelocityException e )
        {
            throw e; // to get past generic catch for Exception.
        }
        catch ( IOException e )
        {
            throw e; // to get past generic catch for Exception.
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    public void setTemplateDirectory( String templateDirectory )
    {
        this.templateDirectory = templateDirectory;
    }

    public void setVelocity( VelocityComponent velocity )
    {
        this.velocity = velocity;
    }

    public Log getLog()
    {
        if ( this.log == null )
        {
            this.log = new SystemStreamLog();
        }
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

}
